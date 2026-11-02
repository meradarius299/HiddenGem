package com.hiddengems.dto;

import com.hiddengems.entity.Friendship;

import java.time.LocalDateTime;

public record FriendshipResponse(
        Long id,
        Long requesterId,
        String requesterUsername,
        Long addresseeId,
        String addresseeUsername,
        String status,
        LocalDateTime createdAt) {

    public static FriendshipResponse from(Friendship f) {
        return new FriendshipResponse(
                f.getId(),
                f.getRequester().getId(),
                f.getRequester().getUsername(),
                f.getAddressee().getId(),
                f.getAddressee().getUsername(),
                f.getStatus().name(),
                f.getCreatedAt());
    }
}
