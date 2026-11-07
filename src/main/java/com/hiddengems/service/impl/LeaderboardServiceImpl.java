package com.hiddengems.service.impl;

import com.hiddengems.dto.LeaderboardEntry;
import com.hiddengems.dto.LeaderboardResponse;
import com.hiddengems.entity.FriendshipStatus;
import com.hiddengems.entity.User;
import com.hiddengems.repository.CollectedGemRepository;
import com.hiddengems.repository.FriendshipRepository;
import com.hiddengems.repository.LeaderboardRow;
import com.hiddengems.repository.UserRepository;
import com.hiddengems.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {

    static final int TOP = 10;

    private final CollectedGemRepository collectedGemRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public LeaderboardResponse leaderboard(String username, String scope) {
        User me = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));

        List<LeaderboardRow> rows;
        if ("global".equals(scope)) {
            rows = collectedGemRepository.leaderboard();
        } else if ("friends".equals(scope)) {
            Set<Long> ids = new HashSet<>(friendshipRepository.findFriendIds(me.getId(), FriendshipStatus.ACCEPTED));
            ids.add(me.getId());
            rows = collectedGemRepository.leaderboardAmong(ids);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scope must be global or friends");
        }

        // The query is already sorted: most keys first, and on a tie whoever got there first.
        // So the rank is simply the position in the list.
        List<LeaderboardEntry> ranked = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            LeaderboardRow row = rows.get(i);
            ranked.add(new LeaderboardEntry(i + 1, row.getUserId(), row.getUsername(), row.getTotal()));
        }

        LeaderboardEntry mine = ranked.stream()
                .filter(e -> e.userId().equals(me.getId()))
                .findFirst()
                .orElse(new LeaderboardEntry(null, me.getId(), me.getUsername(), 0));

        return new LeaderboardResponse(scope, ranked.stream().limit(TOP).toList(), mine);
    }
}