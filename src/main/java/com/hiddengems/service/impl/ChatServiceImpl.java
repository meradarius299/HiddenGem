package com.hiddengems.service.impl;

import com.hiddengems.dto.ChatMessageResponse;
import com.hiddengems.entity.ChatMessage;
import com.hiddengems.entity.FriendshipStatus;
import com.hiddengems.entity.User;
import com.hiddengems.repository.ChatMessageRepository;
import com.hiddengems.repository.FriendshipRepository;
import com.hiddengems.repository.UserRepository;
import com.hiddengems.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int PAGE_SIZE = 50;
    private static final int MAX_LENGTH = 1000;

    private final ChatMessageRepository chatMessageRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public List<ChatMessageResponse> messages(String username, Long friendId, long afterId) {
        User me = getUser(username);
        requireFriend(me, friendId);
        List<ChatMessageResponse> result = new ArrayList<>(
                chatMessageRepository.findConversation(me.getId(), friendId, afterId, PageRequest.of(0, PAGE_SIZE))
                        .stream()
                        .map(m -> ChatMessageResponse.from(m, me.getId()))
                        .toList());
        Collections.reverse(result);
        chatMessageRepository.markConversationRead(me.getId(), friendId);
        return result;
    }

    @Override
    @Transactional
    public ChatMessageResponse send(String username, Long friendId, String content) {
        User me = getUser(username);
        User friend = requireFriend(me, friendId);
        String text = content == null ? "" : content.trim();
        if (text.isEmpty() || text.length() > MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message must be 1 to 1000 characters");
        }
        ChatMessage message = new ChatMessage();
        message.setSender(me);
        message.setRecipient(friend);
        message.setContent(text);
        return ChatMessageResponse.from(chatMessageRepository.save(message), me.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> unreadByFriend(String username) {
        User me = getUser(username);
        Set<Long> friends = new HashSet<>(friendshipRepository.findFriendIds(me.getId(), FriendshipStatus.ACCEPTED));
        Map<Long, Long> result = new HashMap<>();
        chatMessageRepository.unreadBySender(me.getId()).forEach(row -> {
            if (friends.contains(row.getSenderId())) {
                result.put(row.getSenderId(), row.getTotal());
            }
        });
        return result;
    }

    private User requireFriend(User me, Long friendId) {
        User friend = userRepository.findById(friendId)
                .filter(User::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        friendshipRepository.findBetween(me.getId(), friendId)
                .filter(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only chat with friends"));
        return friend;
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    }
}