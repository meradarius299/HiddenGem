package com.hiddengems.service;

import com.hiddengems.dto.CollectResponse;

import java.util.List;

public interface CollectionService {

    /** Ids of the gems this user has collected. */
    List<Long> collectedGemIds(String username);

    /** Collects the gem's key if the player is close enough. Safe to call twice. */
    CollectResponse collect(Long gemId, double lat, double lng, String username);

    /** Removes all collected keys of this user (testing tool). */
    void reset(String username);
}