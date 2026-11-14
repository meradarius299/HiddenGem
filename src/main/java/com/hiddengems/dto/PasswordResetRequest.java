package com.hiddengems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank @Size(max = 128) String token,
        @NotBlank @Size(min = 6, max = 100) String password) {
}
