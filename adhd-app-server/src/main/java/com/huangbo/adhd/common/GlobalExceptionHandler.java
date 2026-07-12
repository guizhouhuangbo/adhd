package com.huangbo.adhd.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + " " + error.getDefaultMessage())
            .orElse("参数校验失败");
        return ResponseEntity.badRequest().body(Result.failure(message));
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Result<Void>> handleBadRequest(Exception ex) {
        log.warn("Request failed: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Result.failure(ex.getMessage() == null ? "请求失败" : ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Result<Void>> handleIllegalState(IllegalStateException ex) {
        log.warn("Request failed: {}", ex.getMessage());
        HttpStatus status = ex.getMessage() != null && ex.getMessage().contains("不存在")
            ? HttpStatus.NOT_FOUND
            : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(Result.failure(ex.getMessage() == null ? "请求失败" : ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleGeneric(Exception ex) {
        log.error("Request failed", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Result.failure(ex.getMessage() == null ? "系统繁忙，请稍后再试" : ex.getMessage()));
    }
}
