package com.hiddengems.service.impl;

import com.hiddengems.dto.FriendshipResponse;
import com.hiddengems.dto.BadgeResponse;
import com.hiddengems.dto.FriendProfileResponse;
import com.hiddengems.dto.ProfileGemResponse;
import com.hiddengems.dto.UserSummaryResponse;
import com.hiddengems.entity.Badge;
import com.hiddengems.entity.CollectedGem;
import com.hiddengems.entity.Friendship;
import com.hiddengems.entity.FriendshipStatus;
import com.hiddengems.entity.User;
import com.hiddengems.repository.FriendshipRepository;
import com.hiddengems.repository.CollectedGemRepository;
import com.hiddengems.repository.UserBadgeRepository;
import com.hiddengems.repository.UserRepository;
import com.hiddengems.service.FriendService;
import com.hiddengems.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final CollectedGemRepository collectedGemRepository;
    private final UserBadgeRepository userBadgeRepository;

    @Override
    @Transactional
    public FriendshipResponse sendRequest(String username, String targetUsername) {
        User me = getUser(username);
        User target = userRepository.findByUsername(targetUsername)
                .filter(User::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizatorul nu exista"));

        if (me.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nu iti poti trimite singur o cerere");
        }

        Friendship friendship = friendshipRepository.findBetween(me.getId(), target.getId()).orElse(null);
        if (friendship != null && friendship.getStatus() == FriendshipStatus.BLOCKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unblock this user before sending a friend request");
        }
        if (friendship == null) {
            friendship = new Friendship();
            friendship.setRequester(me);
            friendship.setAddressee(target);
            friendship.setStatus(FriendshipStatus.PENDING);
            friendship = friendshipRepository.save(friendship);
        } else if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sunteti deja prieteni");
        } else if (friendship.getStatus() == FriendshipStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Exista deja o cerere in asteptare");
        } else {
            friendship.setRequester(me);
            friendship.setAddressee(target);
            friendship.setStatus(FriendshipStatus.PENDING);
        }

        notificationService.notifyFriendRequest(me, target);
        return FriendshipResponse.from(friendship);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendshipResponse> incomingRequests(String username) {
        User me = getUser(username);
        return friendshipRepository.findIncoming(me.getId(), FriendshipStatus.PENDING).stream()
                .map(FriendshipResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public FriendshipResponse accept(String username, Long requestId) {
        User me = getUser(username);
        Friendship friendship = getPendingRequestFor(me, requestId);
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        notificationService.notifyFriendAccepted(me, friendship.getRequester());
        return FriendshipResponse.from(friendship);
    }

    @Override
    @Transactional
    public void reject(String username, Long requestId) {
        User me = getUser(username);
        Friendship friendship = getPendingRequestFor(me, requestId);
        friendship.setStatus(FriendshipStatus.REJECTED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> friends(String username) {
        User me = getUser(username);
        return friendshipRepository.findByUserAndStatus(me.getId(), FriendshipStatus.ACCEPTED).stream()
                .map(f -> f.getRequester().getId().equals(me.getId()) ? f.getAddressee() : f.getRequester())
                .map(UserSummaryResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FriendProfileResponse profile(String username, Long friendId) {
        User me = getUser(username);
        User friend = userRepository.findById(friendId)
                .filter(User::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        friendshipRepository.findBetween(me.getId(), friendId)
                .filter(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view a friend's profile"));

        List<CollectedGem> collected = collectedGemRepository.findByUserIdOrderByCollectedAtDesc(friendId);
        List<Long> friendIds = friendshipRepository.findFriendIds(friendId, FriendshipStatus.ACCEPTED);
        List<UserSummaryResponse> friendList = userRepository.findAllById(friendIds).stream()
                .map(UserSummaryResponse::from).toList();
        Map<String, java.time.LocalDateTime> earnedAt = userBadgeRepository.findByUserId(friendId).stream()
                .collect(Collectors.toMap(b -> b.getBadgeCode(), b -> b.getEarnedAt()));
        List<BadgeResponse> medals = Arrays.stream(Badge.values())
                .map(b -> new BadgeResponse(b.name(), b.getTitle(), b.getDescription(), b.getKeysRequired(),
                        earnedAt.containsKey(b.name()), earnedAt.get(b.name())))
                .toList();
        long stars = collected.size();
        long score = stars * 100 + earnedAt.size() * 250L;
        List<ProfileGemResponse> gems = collected.stream()
                .map(c -> new ProfileGemResponse(c.getGem().getId(), c.getGem().getName(), c.getCollectedAt()))
                .toList();
        return new FriendProfileResponse(friend.getId(), friend.getUsername(), score, stars, collected.size(),
                friendIds.size(), friend.getCreatedAt(), medals, gems, friendList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> blockedUsers(String username) {
        User me = getUser(username);
        return friendshipRepository.findByUserAndStatus(me.getId(), FriendshipStatus.BLOCKED).stream()
                .filter(f -> f.getRequester().getId().equals(me.getId()))
                .map(Friendship::getAddressee)
                .map(UserSummaryResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void block(String username, Long userId) {
        User me = getUser(username);
        User target = userRepository.findById(userId).filter(User::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (me.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot block yourself");
        }
        Friendship relation = friendshipRepository.findBetween(me.getId(), userId).orElseGet(Friendship::new);
        if (relation.getStatus() == FriendshipStatus.BLOCKED) {
            if (relation.getRequester().getId().equals(me.getId())) return;
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This user has blocked you");
        }
        relation.setRequester(me);
        relation.setAddressee(target);
        relation.setStatus(FriendshipStatus.BLOCKED);
        friendshipRepository.save(relation);
    }

    @Override
    @Transactional
    public void unblock(String username, Long userId) {
        User me = getUser(username);
        Friendship relation = friendshipRepository.findBetween(me.getId(), userId)
                .filter(f -> f.getStatus() == FriendshipStatus.BLOCKED && f.getRequester().getId().equals(me.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blocked user not found"));
        friendshipRepository.delete(relation);
    }

    @Override
    @Transactional
    public void removeFriend(String username, Long friendUserId) {
        User me = getUser(username);
        Friendship friendship = friendshipRepository.findBetween(me.getId(), friendUserId)
                .filter(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nu sunteti prieteni"));
        friendshipRepository.delete(friendship);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> searchUsers(String username, String fragment) {
        if (fragment == null || fragment.trim().length() < 2) {
            return List.of();
        }
        User me = getUser(username);
        return userRepository
                .findTop20ByUsernameContainingIgnoreCaseAndIdNotAndEnabledTrue(fragment.trim(), me.getId())
                .stream()
                .filter(user -> friendshipRepository.findBetween(me.getId(), user.getId())
                        .map(f -> f.getStatus() != FriendshipStatus.BLOCKED).orElse(true))
                .map(UserSummaryResponse::from)
                .toList();
    }
    private Friendship getPendingRequestFor(User me, Long requestId) {
        Friendship friendship = friendshipRepository.findById(requestId)
                .filter(f -> f.getAddressee().getId().equals(me.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cererea nu exista"));
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cererea a fost deja procesata");
        }
        return friendship;
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilizator inexistent"));
    }
}
