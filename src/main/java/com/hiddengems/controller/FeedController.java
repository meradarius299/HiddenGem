package com.hiddengems.controller;

import com.hiddengems.dto.GemResponse;
import com.hiddengems.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    /** "Ce au facut prietenii mei": ultimele locuri adaugate de ei. */
    @GetMapping
    public List<GemResponse> feed(Authentication auth) {
        return feedService.friendsFeed(auth.getName());
    }
}
