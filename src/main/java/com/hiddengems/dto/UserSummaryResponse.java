package com.hiddengems.dto;

import com.hiddengems.entity.User;

public record UserSummaryResponse(Long id, String username) {

    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(user.getId(), user.getUsername());
    }
}
