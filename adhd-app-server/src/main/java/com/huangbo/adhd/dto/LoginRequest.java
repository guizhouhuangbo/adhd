package com.huangbo.adhd.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "code 不能为空") String code,
    String nickname,
    String avatarUrl
) {
}
