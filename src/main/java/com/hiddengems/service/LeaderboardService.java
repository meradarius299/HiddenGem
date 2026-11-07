package com.hiddengems.service;

import com.hiddengems.dto.LeaderboardResponse;

public interface LeaderboardService {

    /** Top players by collected keys. Scope is "global" (everyone) or "friends" (the player and their friends). */
    LeaderboardResponse leaderboard(String username, String scope);
}