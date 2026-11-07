package com.hiddengems.controller;

import com.hiddengems.dto.BadgesResponse;
import com.hiddengems.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    /** The current player's badges, earned and locked, with their key count. */
    @GetMapping("/me/badges")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public BadgesResponse myBadges(Authentication auth) {
        return badgeService.badgesOf(auth.getName());
    }
}