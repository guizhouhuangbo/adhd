package com.huangbo.adhd.dto;

public record LoginResponse(String token, UserProfileResponse profile) {
}
