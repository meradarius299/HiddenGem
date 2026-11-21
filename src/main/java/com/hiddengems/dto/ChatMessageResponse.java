package com.hiddengems.dto;

import com.hiddengems.entity.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageResponse(Long id, Long senderId, String content, LocalDateTime createdAt, boolean mine) {

    public static ChatMessageResponse from(ChatMessage message, Long meId) {
        Long senderId = message.getSender().getId();
        return new ChatMessageResponse(message.getId(), senderId, message.getContent(),
                message.getCreatedAt(), senderId.equals(meId));
    }
}