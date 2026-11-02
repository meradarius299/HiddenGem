package com.hiddengems.dto;

import jakarta.validation.constraints.NotBlank;

/** Corpul cererii POST /api/friends/requests. */
public record FriendRequestCreate(@NotBlank String username) {
}
