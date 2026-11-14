package com.hiddengems.service.impl;

import com.hiddengems.dto.FriendFeedItem;
import com.hiddengems.dto.FriendDiscoveryResponse;
import com.hiddengems.entity.FriendshipStatus;
import com.hiddengems.entity.User;
import com.hiddengems.repository.FriendshipRepository;
import com.hiddengems.repository.CollectedGemRepository;
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
import java.util.Comparator;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final GemRepository gemRepository;
    private final CollectedGemRepository collectedGemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FriendFeedItem> friendsFeed(String username) {
        User me = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilizator inexistent"));

        List<Long> friendIds = friendshipRepository.findFriendIds(me.getId(), FriendshipStatus.ACCEPTED);
        if (friendIds.isEmpty()) {
            return List.of();
        }
        var page = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt"));
        Stream<FriendFeedItem> added = gemRepository
                .findByOwnerIdIn(friendIds, page)
                .stream()
                .map(FriendFeedItem::added);
        var collectionPage = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "collectedAt"));
        Stream<FriendFeedItem> discovered = collectedGemRepository
                .findByUserIdIn(friendIds, collectionPage)
                .stream()
                .map(FriendFeedItem::discovered);
        return Stream.concat(added, discovered)
                .sorted(Comparator.comparing(FriendFeedItem::createdAt).reversed())
                .limit(50)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendDiscoveryResponse> friendsDiscoveries(String username) {
        User me = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilizator inexistent"));
        List<Long> friendIds = friendshipRepository.findFriendIds(me.getId(), FriendshipStatus.ACCEPTED);
        if (friendIds.isEmpty()) return List.of();
        return collectedGemRepository.findByUserIdIn(friendIds).stream()
                .map(FriendDiscoveryResponse::from)
                .toList();
    }
}
