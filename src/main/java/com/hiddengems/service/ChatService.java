package com.hiddengems.service;

import com.hiddengems.dto.ChatMessageResponse;

import java.util.List;
import java.util.Map;

public interface ChatService {

    /** Oldest first. Also marks the friend's messages as read. */
    List<ChatMessageResponse> messages(String username, Long friendId, long afterId);

    ChatMessageResponse send(String username, Long friendId, String content);

    /** Friend id -> number of unread messages from that friend. */
    Map<Long, Long> unreadByFriend(String username);
}