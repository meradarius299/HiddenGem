package com.hiddengems.dto;

import jakarta.validation.constraints.NotBlank;

/** idToken primit in aplicatia Flutter de la Google Sign-In. */
public record GoogleLoginRequest(@NotBlank String idToken) {
}
