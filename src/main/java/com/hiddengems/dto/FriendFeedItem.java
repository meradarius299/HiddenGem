package com.hiddengems.dto;

import com.hiddengems.entity.CollectedGem;
import com.hiddengems.entity.Gem;

import java.time.LocalDateTime;

public record FriendFeedItem(
        Long id,
        String name,
        String category,
        String actor,
        String activity,
        LocalDateTime createdAt) {

    public static FriendFeedItem added(Gem gem) {
        return new FriendFeedItem(gem.getId(), gem.getName(), gem.getCategory(),
                gem.getOwner().getUsername(), "ADDED", gem.getCreatedAt());
    }

    public static FriendFeedItem discovered(CollectedGem collected) {
        Gem gem = collected.getGem();
        return new FriendFeedItem(gem.getId(), gem.getName(), gem.getCategory(),
                collected.getUser().getUsername(), "DISCOVERED", collected.getCollectedAt());
    }
}
