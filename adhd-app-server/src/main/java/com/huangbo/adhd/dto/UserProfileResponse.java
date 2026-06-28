package com.huangbo.adhd.dto;

public record UserProfileResponse(Long id, String nickname, String avatarUrl, String childName, Integer childAge, Integer totalStars) {
}
