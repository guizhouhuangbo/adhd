package com.huangbo.adhd.common;

import java.time.Instant;

public record Result<T>(int code, String msg, T data, long timestamp) {

    public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data, Instant.now().toEpochMilli());
    }

    public static Result<Void> success() {
        return success(null);
    }

    public static Result<Void> failure(String msg) {
        return new Result<>(1, msg, null, Instant.now().toEpochMilli());
    }
}
