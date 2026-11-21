package com.hiddengems.controller;

import com.hiddengems.dto.ChatMessageRequest;
import com.hiddengems.dto.ChatMessageResponse;
import com.hiddengems.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/unread")
    public Map<Long, Long> unread(Authentication auth) {
        return chatService.unreadByFriend(auth.getName());
    }

    @GetMapping("/{friendId}")
    public List<ChatMessageResponse> messages(@PathVariable Long friendId,
                                              @RequestParam(defaultValue = "0") long afterId,
                                              Authentication auth) {
        return chatService.messages(auth.getName(), friendId, afterId);
    }

    @PostMapping("/{friendId}")
    public ChatMessageResponse send(@PathVariable Long friendId,
                                    @Valid @RequestBody ChatMessageRequest request,
                                    Authentication auth) {
        return chatService.send(auth.getName(), friendId, request.content());
    }
}