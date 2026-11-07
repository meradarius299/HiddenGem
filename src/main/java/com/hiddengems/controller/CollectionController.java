package com.hiddengems.controller;

import com.hiddengems.dto.CollectRequest;
import com.hiddengems.dto.CollectResponse;
import com.hiddengems.service.CollectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @GetMapping("/me/collected")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<Long> collected(Authentication auth) {
        return collectionService.collectedGemIds(auth.getName());
    }

    @PostMapping("/gems/{id}/collect")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public CollectResponse collect(@PathVariable Long id,
                                   @Valid @RequestBody CollectRequest request,
                                   Authentication auth) {
        return collectionService.collect(id, request.latitude(), request.longitude(), auth.getName());
    }

    /** Testing tool: forget all of the current user's collected keys. */
    @DeleteMapping("/me/collected")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> reset(Authentication auth) {
        collectionService.reset(auth.getName());
        return ResponseEntity.noContent().build();
    }
}