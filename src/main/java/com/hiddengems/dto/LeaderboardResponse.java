package com.hiddengems.dto;

import java.util.List;

/** The top of the board plus the current player's own place, even when they are outside the top. */
public record LeaderboardResponse(String scope, List<LeaderboardEntry> top, LeaderboardEntry me) {
}