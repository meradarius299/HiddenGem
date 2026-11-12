package com.hiddengems.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MapConfigController {
    private final String apiKey;
    private final String style;

    public MapConfigController(
            @Value("${MAPTILER_API_KEY:}") String apiKey,
            @Value("${MAPTILER_MAP_ID:streets-v4}") String style) {
        this.apiKey = apiKey.trim();
        this.style = style.trim();
    }

    // Browser-facing map key: restrict its allowed origins in MapTiler.
    @GetMapping("/api/config/map")
    public ResponseEntity<MapConfig> mapConfig() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new MapConfig(apiKey, style));
    }

    public record MapConfig(String apiKey, String style) {}
}
