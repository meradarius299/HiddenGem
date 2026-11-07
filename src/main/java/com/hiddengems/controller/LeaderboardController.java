package com.hiddengems.controller;

import com.hiddengems.dto.LeaderboardResponse;
import com.hiddengems.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/leaderboard")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public LeaderboardResponse leaderboard(@RequestParam(defaultValue = "global") String scope,
                                           Authentication auth) {
        return leaderboardService.leaderboard(auth.getName(), scope);
    }
}