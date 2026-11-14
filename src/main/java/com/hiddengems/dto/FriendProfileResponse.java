package com.hiddengems.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FriendProfileResponse(
        Long id,
        String username,
        long score,
        long stars,
        long collectedGems,
        int friendsCount,
        LocalDateTime joinedAt,
        List<BadgeResponse> medals,
        List<ProfileGemResponse> gems,
        List<UserSummaryResponse> friends) {
}
