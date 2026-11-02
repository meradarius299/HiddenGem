package com.hiddengems.service;

import com.hiddengems.dto.GemResponse;

import java.util.List;

public interface FeedService {

    /** Ultimele locuri adaugate de prietenii utilizatorului. */
    List<GemResponse> friendsFeed(String username);
}
