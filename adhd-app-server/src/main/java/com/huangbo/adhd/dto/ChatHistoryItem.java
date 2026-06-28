package com.huangbo.adhd.dto;

import java.time.LocalDateTime;

public record ChatHistoryItem(String role, String content, LocalDateTime createdAt) {
}
