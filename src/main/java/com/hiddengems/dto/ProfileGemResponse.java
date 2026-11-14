package com.hiddengems.dto;

import java.time.LocalDateTime;

public record ProfileGemResponse(Long id, String name, LocalDateTime collectedAt) {
}
