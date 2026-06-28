package com.huangbo.adhd.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank(message = "不能为空") String message) {
}
