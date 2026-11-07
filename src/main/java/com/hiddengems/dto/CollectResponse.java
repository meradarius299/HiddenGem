package com.hiddengems.dto;

public record CollectResponse(Long gemId, boolean alreadyCollected, long totalCollected) {
}