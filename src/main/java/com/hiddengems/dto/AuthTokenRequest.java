package com.hiddengems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthTokenRequest(@NotBlank @Size(max = 128) String token) {
}
