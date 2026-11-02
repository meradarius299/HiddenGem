package com.hiddengems.service;

import com.hiddengems.dto.FriendshipResponse;
import com.hiddengems.dto.UserSummaryResponse;

import java.util.List;

public interface FriendService {

    FriendshipResponse sendRequest(String username, String targetUsername);

    List<FriendshipResponse> incomingRequests(String username);

    FriendshipResponse accept(String username, Long requestId);

    void reject(String username, Long requestId);

    List<UserSummaryResponse> friends(String username);

    void removeFriend(String username, Long friendUserId);

    List<UserSummaryResponse> searchUsers(String username, String fragment);
}
