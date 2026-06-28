package com.huangbo.adhd.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskCreateRequest(@NotBlank(message = "不能为空") String name) {
}
