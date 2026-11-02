package com.hiddengems.security;

public interface GoogleTokenVerifier {

    GoogleUser verify(String idToken);

    record GoogleUser(String subject, String email, String name, boolean emailVerified) {
    }
}
