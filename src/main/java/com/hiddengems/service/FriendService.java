package com.hiddengems.service;

import com.hiddengems.dto.FriendshipResponse;
import com.hiddengems.dto.FriendProfileResponse;
import com.hiddengems.dto.UserSummaryResponse;

import java.util.List;

public interface FriendService {

    FriendshipResponse sendRequest(String username, String targetUsername);

    List<FriendshipResponse> incomingRequests(String username);

    FriendshipResponse accept(String username, Long requestId);

    void reject(String username, Long requestId);

    List<UserSummaryResponse> friends(String username);
    FriendProfileResponse profile(String username, Long friendId);
    List<UserSummaryResponse> blockedUsers(String username);
    void block(String username, Long userId);
    void unblock(String username, Long userId);

    void removeFriend(String username, Long friendUserId);

    List<UserSummaryResponse> searchUsers(String username, String fragment);
}
