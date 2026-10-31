package com.hiddengems.service;

import com.hiddengems.dto.AuthResponse;
import com.hiddengems.dto.GoogleLoginRequest;
import com.hiddengems.dto.LoginRequest;
import com.hiddengems.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse loginWithGoogle(GoogleLoginRequest request);
}
