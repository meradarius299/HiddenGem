package com.hiddengems.dto;

import java.util.List;

public record CollectResponse(Long gemId, boolean alreadyCollected, long totalCollected,
                              /** Titles of the badges earned by this key (empty most of the time). */
                              List<String> newBadges) {
}