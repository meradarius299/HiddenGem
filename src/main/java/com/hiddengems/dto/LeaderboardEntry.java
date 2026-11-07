package com.hiddengems.dto;

/** A place on the leaderboard. {@code rank} is null for a player who has no keys yet. */
public record LeaderboardEntry(Integer rank, Long userId, String username, long keys) {
}