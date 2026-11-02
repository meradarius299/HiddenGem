package com.hiddengems.dto;

import com.hiddengems.entity.Notification;

import java.time.LocalDateTime;

/** "type" permite aplicatiei Flutter sa aleaga iconita si unde duce apasarea (loc / profil). */
public record NotificationResponse(
        Long id,
        String type,
        String message,
        Long actorId,
        String actorUsername,
        Long gemId,
        boolean read,
        LocalDateTime createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType().name(),
                n.getMessage(),
                n.getActor() == null ? null : n.getActor().getId(),
                n.getActor() == null ? null : n.getActor().getUsername(),
                n.getGem() == null ? null : n.getGem().getId(),
                n.isSeen(),
                n.getCreatedAt());
    }
}
