package com.huangbo.adhd.dto;

import jakarta.validation.constraints.NotNull;

public record CheckInRequest(
    @NotNull(message = "taskId 不能为空") Long taskId,
    String note
) {
}
