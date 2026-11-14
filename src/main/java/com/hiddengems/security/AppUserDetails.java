package com.hiddengems.security;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public final class AppUserDetails extends org.springframework.security.core.userdetails.User {

    private final int tokenVersion;

    public AppUserDetails(String username, String password, boolean enabled,
                          Collection<? extends GrantedAuthority> authorities, int tokenVersion) {
        super(username, password, enabled, true, true, true, authorities);
        this.tokenVersion = tokenVersion;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }
}
