package com.huangbo.adhd.controller;

import com.huangbo.adhd.common.Result;
import com.huangbo.adhd.config.AuthContext;
import com.huangbo.adhd.dto.ChatHistoryItem;
import com.huangbo.adhd.dto.ChatRequest;
import com.huangbo.adhd.dto.ChatResponse;
import com.huangbo.adhd.service.ChatService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final ChatService chatService;

    public AIController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/chat/history")
    public Result<List<ChatHistoryItem>> history() {
        return Result.success(chatService.getHistory(AuthContext.getUserId()));
    }

    @PostMapping("/chat")
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return Result.success(chatService.chat(AuthContext.getUserId(), request.message()));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        Long userId = AuthContext.getUserId();

        CompletableFuture.runAsync(() -> {
            try {
                emitter.send(SseEmitter.event().name("open").data("[OPEN]"));
                chatService.chatStream(userId, request.message(), delta -> {
                    try {
                        emitter.send(SseEmitter.event().data(Map.of("delta", delta)));
                    } catch (Exception ex) {
                        throw new IllegalStateException("SSE 写出失败", ex);
                    }
                });
                try {
                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        });

        return emitter;
    }
}
