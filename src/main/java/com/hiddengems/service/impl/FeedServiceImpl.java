package com.hiddengems.service.impl;

import com.hiddengems.dto.GemResponse;
import com.hiddengems.entity.FriendshipStatus;
import com.hiddengems.entity.User;
import com.hiddengems.repository.FriendshipRepository;
import com.hiddengems.repository.GemRepository;
import com.hiddengems.repository.UserRepository;
import com.hiddengems.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final GemRepository gemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GemResponse> friendsFeed(String username) {
        User me = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilizator inexistent"));

        List<Long> friendIds = friendshipRepository.findFriendIds(me.getId(), FriendshipStatus.ACCEPTED);
        if (friendIds.isEmpty()) {
            return List.of();
        }
        return gemRepository
                .findByOwnerIdIn(friendIds, PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(GemResponse::from)
                .toList();
    }
}
