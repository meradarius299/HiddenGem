package com.hiddengems.service.impl;

import com.hiddengems.dto.FriendshipResponse;
import com.hiddengems.dto.UserSummaryResponse;
import com.hiddengems.entity.Friendship;
import com.hiddengems.entity.FriendshipStatus;
import com.hiddengems.entity.User;
import com.hiddengems.repository.FriendshipRepository;
import com.hiddengems.repository.UserRepository;
import com.hiddengems.service.FriendService;
import com.hiddengems.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

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
            // REJECTED: refolosim randul (perechea e unica in baza de date) si retrimitem cererea
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
                .map(UserSummaryResponse::from)
                .toList();
    }

    /** Cererea trebuie sa existe, sa fie adresata utilizatorului curent si sa fie inca PENDING. */
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
