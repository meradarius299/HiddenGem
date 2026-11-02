package com.hiddengems.controller;

import com.hiddengems.dto.FriendRequestCreate;
import com.hiddengems.dto.FriendshipResponse;
import com.hiddengems.dto.UserSummaryResponse;
import com.hiddengems.service.FriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    /** Cauta utilizatori dupa username, ca sa le trimiti cerere de prietenie. */
    @GetMapping("/users/search")
    public List<UserSummaryResponse> search(@RequestParam("q") String query, Authentication auth) {
        return friendService.searchUsers(auth.getName(), query);
    }

    @GetMapping("/friends")
    public List<UserSummaryResponse> friends(Authentication auth) {
        return friendService.friends(auth.getName());
    }

    @PostMapping("/friends/requests")
    public ResponseEntity<FriendshipResponse> sendRequest(@Valid @RequestBody FriendRequestCreate body,
                                                          Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(friendService.sendRequest(auth.getName(), body.username()));
    }

    @GetMapping("/friends/requests/incoming")
    public List<FriendshipResponse> incoming(Authentication auth) {
        return friendService.incomingRequests(auth.getName());
    }

    @PostMapping("/friends/requests/{id}/accept")
    public FriendshipResponse accept(@PathVariable Long id, Authentication auth) {
        return friendService.accept(auth.getName(), id);
    }

    @PostMapping("/friends/requests/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id, Authentication auth) {
        friendService.reject(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    /** Sterge un prieten (id-ul utilizatorului, nu al cererii). */
    @DeleteMapping("/friends/{userId}")
    public ResponseEntity<Void> remove(@PathVariable Long userId, Authentication auth) {
        friendService.removeFriend(auth.getName(), userId);
        return ResponseEntity.noContent().build();
    }
}
