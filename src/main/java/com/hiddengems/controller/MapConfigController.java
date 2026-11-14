package com.hiddengems.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MapConfigController {
    private final String googleApiKey;
    private final String googleClientId;

    public MapConfigController(
            @Value("${GOOGLE_MAPS_API_KEY:}") String googleApiKey,
            @Value("${app.google.client-ids:}") String googleClientIds) {
        this.googleApiKey = googleApiKey.trim();
        this.googleClientId = googleClientIds.lines()
                .flatMap(line -> java.util.Arrays.stream(line.split(",")))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .findFirst()
                .orElse("");
    }

    // Browser-facing map keys must be restricted to the site's allowed origins in the provider dashboard.
    @GetMapping("/api/config/map")
    public ResponseEntity<MapConfig> mapConfig() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new MapConfig(googleApiKey));
    }

    @GetMapping("/api/config/auth")
    public ResponseEntity<AuthConfig> authConfig() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new AuthConfig(googleClientId));
    }

    public record MapConfig(String googleApiKey) {}
    public record AuthConfig(String googleClientId) {}
}
