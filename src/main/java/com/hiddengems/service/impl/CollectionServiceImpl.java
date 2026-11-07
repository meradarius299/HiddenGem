package com.hiddengems.service.impl;

import com.hiddengems.dto.CollectResponse;
import com.hiddengems.entity.CollectedGem;
import com.hiddengems.entity.Gem;
import com.hiddengems.entity.User;
import com.hiddengems.repository.CollectedGemRepository;
import com.hiddengems.repository.GemRepository;
import com.hiddengems.repository.UserRepository;
import com.hiddengems.service.CollectionService;
import com.hiddengems.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.hiddengems.entity.Badge;
import com.hiddengems.service.BadgeService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectionServiceImpl implements CollectionService {

    /** The app offers the unlock at 75 m. The server allows more because GPS drifts. */
    static final double MAX_DISTANCE_M = 150;

    private final CollectedGemRepository collectedGemRepository;
    private final GemRepository gemRepository;
    private final UserRepository userRepository;
    private final BadgeService badgeService;

    @Override
    public List<Long> collectedGemIds(String username) {
        return collectedGemRepository.findGemIdsByUserId(getUser(username).getId());
    }

    // Not @Transactional on purpose: if two requests collect the same gem at the same time,
    // the database rejects the second insert and we can catch it without breaking a transaction.
    @Override
    public CollectResponse collect(Long gemId, double lat, double lng, String username) {
        User user = getUser(username);
        Gem gem = gemRepository.findById(gemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gem not found"));

        double distance = GeoUtils.distanceMeters(lat, lng, gem.getLatitude(), gem.getLongitude());
        if (distance > MAX_DISTANCE_M) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are too far from this gem");
        }

        boolean already = collectedGemRepository.existsByUserIdAndGemId(user.getId(), gemId);
        if (!already) {
            CollectedGem collected = new CollectedGem();
            collected.setUser(user);
            collected.setGem(gem);
            try {
                collectedGemRepository.save(collected);
            } catch (DataIntegrityViolationException e) {
                already = true; // a parallel request saved it first
            }
        }
        long total = collectedGemRepository.countByUserId(user.getId());
        List<String> newBadges = already
                ? List.of()
                : badgeService.awardEarned(user, total).stream().map(Badge::getTitle).toList();
        return new CollectResponse(gemId, already, total, newBadges);
    }

    @Override
    @Transactional
    public void reset(String username) {
        collectedGemRepository.deleteByUserId(getUser(username).getId());
        badgeService.reset(username);
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    }
}