package com.hiddengems.repository;

/** One line of the leaderboard query: a user and how many keys they hold. */
public interface LeaderboardRow {

    Long getUserId();

    String getUsername();

    Long getTotal();
}