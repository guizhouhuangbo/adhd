package com.huangbo.adhd.dto;

public record CheckInResponse(Long taskId, Integer earnedStars, Integer totalStars, String message) {
}
