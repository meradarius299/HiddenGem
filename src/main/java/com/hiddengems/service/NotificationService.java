package com.hiddengems.service;

import com.hiddengems.dto.NotificationResponse;
import com.hiddengems.entity.Gem;
import com.hiddengems.entity.User;
import com.hiddengems.entity.Badge;

import java.util.List;

public interface NotificationService {

    /** Creeaza o notificare "Uite ce a descoperit ..." pentru fiecare prieten al lui actor. */
    void notifyFriendsNewGem(User actor, Gem gem);

    void notifyFriendRequest(User from, User to);

    void notifyBadgeEarned(User user, Badge badge);

    void notifyFriendAccepted(User accepter, User requester);

    List<NotificationResponse> list(String username);

    long unreadCount(String username);

    void markRead(String username, Long notificationId);

    void markAllRead(String username);
}
