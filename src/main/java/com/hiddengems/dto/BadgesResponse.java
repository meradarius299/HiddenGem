package com.hiddengems.dto;

import java.util.List;

/** The player's key count plus every badge, so the page can show progress towards the locked ones. */
public record BadgesResponse(long keys, List<BadgeResponse> badges) {
}