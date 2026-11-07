package com.hiddengems.dto;

import java.time.LocalDateTime;

/** One badge as shown to the player: earned (with a date) or still locked. */
public record BadgeResponse(
        String code,
        String title,
        String description,
        int keysRequired,
        boolean earned,
        LocalDateTime earnedAt) {
}