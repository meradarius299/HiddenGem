package com.hiddengems.service;

import com.hiddengems.dto.BadgesResponse;
import com.hiddengems.entity.Badge;
import com.hiddengems.entity.User;

import java.util.List;

public interface BadgeService {

    /** Awards every badge the user now qualifies for and notifies them. Returns only the badges earned just now. */
    List<Badge> awardEarned(User user, long totalKeys);

    /** All badges with the user's progress. */
    BadgesResponse badgesOf(String username);

    /** Removes the user's badges (part of the "reset progress" testing tool). */
    void reset(String username);
}