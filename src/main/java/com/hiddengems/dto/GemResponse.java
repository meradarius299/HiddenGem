package com.hiddengems.dto;

import com.hiddengems.entity.Gem;

import java.time.LocalDateTime;

public record GemResponse(
        Long id,
        String name,
        String description,
        double latitude,
        double longitude,
        String category,
        boolean verified,
        String owner,
        LocalDateTime createdAt) {

    public static GemResponse from(Gem gem) {
        return new GemResponse(
                gem.getId(),
                gem.getName(),
                gem.getDescription(),
                gem.getLatitude(),
                gem.getLongitude(),
                gem.getCategory(),
                gem.isVerified(),
                gem.getOwner().getUsername(),
                gem.getCreatedAt());
    }
}
