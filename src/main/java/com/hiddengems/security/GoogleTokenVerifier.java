package com.hiddengems.security;

/** Verifica un ID token emis de Google si returneaza datele utilizatorului. */
public interface GoogleTokenVerifier {

    GoogleUser verify(String idToken);

    record GoogleUser(String subject, String email, String name, boolean emailVerified) {
    }
}
