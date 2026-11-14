package com.hiddengems.dto;

import com.hiddengems.entity.CollectedGem;
import java.time.LocalDateTime;

public record FriendDiscoveryResponse(Long gemId, String gemName, String discoverer, LocalDateTime discoveredAt) {
    public static FriendDiscoveryResponse from(CollectedGem collected) {
        return new FriendDiscoveryResponse(collected.getGem().getId(), collected.getGem().getName(),
                collected.getUser().getUsername(), collected.getCollectedAt());
    }
}
