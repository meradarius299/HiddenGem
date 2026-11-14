package com.hiddengems.service;

import com.hiddengems.dto.FriendFeedItem;
import com.hiddengems.dto.FriendDiscoveryResponse;

import java.util.List;

public interface FeedService {

    /** Recent gems added or discovered by the user's friends. */
    List<FriendFeedItem> friendsFeed(String username);

    List<FriendDiscoveryResponse> friendsDiscoveries(String username);
}
