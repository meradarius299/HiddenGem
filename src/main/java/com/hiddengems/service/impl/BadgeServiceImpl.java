package com.hiddengems.service.impl;

import com.hiddengems.dto.BadgeResponse;
import com.hiddengems.dto.BadgesResponse;
import com.hiddengems.entity.Badge;
import com.hiddengems.entity.User;
import com.hiddengems.entity.UserBadge;
import com.hiddengems.repository.CollectedGemRepository;
import com.hiddengems.repository.UserBadgeRepository;
import com.hiddengems.repository.UserRepository;
import com.hiddengems.service.BadgeService;
import com.hiddengems.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BadgeServiceImpl implements BadgeService {

    private final UserBadgeRepository userBadgeRepository;
    private final CollectedGemRepository collectedGemRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // Not @Transactional on purpose (same reason as CollectionServiceImpl.collect): if two requests award the
    // same badge at once, the database rejects the second insert and we can catch it without breaking a transaction.
    @Override
    public List<Badge> awardEarned(User user, long totalKeys) {
        Set<String> owned = new HashSet<>();
        userBadgeRepository.findByUserId(user.getId()).forEach(b -> owned.add(b.getBadgeCode()));

        List<Badge> awarded = new ArrayList<>();
        for (Badge badge : Badge.values()) {
            if (totalKeys < badge.getKeysRequired() || owned.contains(badge.name())) {
                continue;
            }
            UserBadge earned = new UserBadge();
            earned.setUser(user);
            earned.setBadgeCode(badge.name());
            try {
                userBadgeRepository.save(earned);
            } catch (DataIntegrityViolationException e) {
                continue; // a parallel request awarded it first
            }
            notificationService.notifyBadgeEarned(user, badge);
            awarded.add(badge);
        }
        return awarded;
    }

    @Override
    public BadgesResponse badgesOf(String username) {
        User user = getUser(username);
        long keys = collectedGemRepository.countByUserId(user.getId());
        // Catches up players who collected keys before badges existed.
        awardEarned(user, keys);

        Map<String, LocalDateTime> earnedAt = userBadgeRepository.findByUserId(user.getId()).stream()
                .collect(Collectors.toMap(UserBadge::getBadgeCode, UserBadge::getEarnedAt));
        List<BadgeResponse> badges = Arrays.stream(Badge.values())
                .map(b -> new BadgeResponse(b.name(), b.getTitle(), b.getDescription(), b.getKeysRequired(),
                        earnedAt.containsKey(b.name()), earnedAt.get(b.name())))
                .toList();
        return new BadgesResponse(keys, badges);
    }

    @Override
    @Transactional
    public void reset(String username) {
        userBadgeRepository.deleteByUserId(getUser(username).getId());
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    }
}