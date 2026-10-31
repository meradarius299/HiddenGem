package com.hiddengems.controller;

import com.hiddengems.dto.GemRequest;
import com.hiddengems.dto.GemResponse;
import com.hiddengems.service.GemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gems")
@RequiredArgsConstructor
public class GemController {

    private final GemService gemService;

    // Orice utilizator autentificat poate vedea locurile
    @GetMapping
    public List<GemResponse> getAll() {
        return gemService.findAll();
    }

    @GetMapping("/{id}")
    public GemResponse getOne(@PathVariable Long id) {
        return gemService.findById(id);
    }

    @GetMapping("/nearby")
    public List<GemResponse> nearby(@RequestParam double lat,
                                    @RequestParam double lng,
                                    @RequestParam(defaultValue = "5") double radiusKm) {
        return gemService.findNearby(lat, lng, radiusKm);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<GemResponse> create(@Valid @RequestBody GemRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gemService.create(request, auth.getName()));
    }

    // Doar administratorul poate sterge un loc
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        gemService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Doar administratorul poate marca un loc ca verificat
    @PatchMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public GemResponse verify(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean verified) {
        return gemService.setVerified(id, verified);
    }
}
