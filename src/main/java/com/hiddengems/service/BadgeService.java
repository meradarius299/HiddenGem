package com.hiddengems.service;

import com.hiddengems.dto.BadgesResponse;
import com.hiddengems.entity.Badge;
import com.hiddengems.entity.User;

import java.util.List;

public interface BadgeService {

    List<Badge> awardEarned(User user, long totalKeys);

    BadgesResponse badgesOf(String username);
    void reset(String username);
}