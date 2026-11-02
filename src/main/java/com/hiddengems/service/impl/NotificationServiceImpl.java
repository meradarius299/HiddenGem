package com.hiddengems.service.impl;

import com.hiddengems.dto.NotificationResponse;
import com.hiddengems.entity.FriendshipStatus;
import com.hiddengems.entity.Gem;
import com.hiddengems.entity.Notification;
import com.hiddengems.entity.NotificationType;
import com.hiddengems.entity.User;
import com.hiddengems.repository.FriendshipRepository;
import com.hiddengems.repository.NotificationRepository;
import com.hiddengems.repository.UserRepository;
import com.hiddengems.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_MESSAGE = 300;
    private static final Pageable LATEST_50 = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt"));

    private final NotificationRepository notificationRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void notifyFriendsNewGem(User actor, Gem gem) {
        List<Long> friendIds = friendshipRepository.findFriendIds(actor.getId(), FriendshipStatus.ACCEPTED);
        if (friendIds.isEmpty()) {
            return;
        }
        String message = "Look what %s discovered: %s. Have you been there?"
                .formatted(actor.getUsername(), gem.getName());

        List<Notification> notifications = friendIds.stream()
                .map(id -> build(userRepository.getReferenceById(id), actor, NotificationType.NEW_GEM, gem, message))
                .toList();
        notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional
    public void notifyFriendRequest(User from, User to) {
        notificationRepository.save(build(to, from, NotificationType.FRIEND_REQUEST, null,
                "%s sent you a friend request".formatted(from.getUsername())));
    }

    @Override
    @Transactional
    public void notifyFriendAccepted(User accepter, User requester) {
        notificationRepository.save(build(requester, accepter, NotificationType.FRIEND_REQUEST, null,
                "%s accepted your friend request".formatted(accepter.getUsername())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> list(String username) {
        User me = getUser(username);
        return notificationRepository.findByRecipientId(me.getId(), LATEST_50).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(String username) {
        return notificationRepository.countByRecipientIdAndSeenFalse(getUser(username).getId());
    }

    @Override
    @Transactional
    public void markRead(String username, Long notificationId) {
        int updated = notificationRepository.markRead(notificationId, getUser(username).getId());
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
        }
    }

    @Override
    @Transactional
    public void markAllRead(String username) {
        notificationRepository.markAllRead(getUser(username).getId());
    }

    private Notification build(User recipient, User actor, NotificationType type, Gem gem, String message) {
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setActor(actor);
        n.setType(type);
        n.setGem(gem);
        n.setMessage(message.length() > MAX_MESSAGE ? message.substring(0, MAX_MESSAGE) : message);
        return n;
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}