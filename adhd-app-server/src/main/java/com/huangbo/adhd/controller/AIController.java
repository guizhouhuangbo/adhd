package com.huangbo.adhd.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huangbo.adhd.common.Result;
import com.huangbo.adhd.config.AuthContext;
import com.huangbo.adhd.dto.ChatHistoryItem;
import com.huangbo.adhd.dto.ChatRequest;
import com.huangbo.adhd.dto.ChatResponse;
import com.huangbo.adhd.service.ChatService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    public AIController(ChatService chatService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
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
    public ResponseEntity<StreamingResponseBody> chatStream(@Valid @RequestBody ChatRequest request) {
        Long userId = AuthContext.getUserId();

        StreamingResponseBody body = outputStream -> {
            writeEvent(outputStream, "open", "[OPEN]");
            chatService.chatStream(userId, request.message(), delta -> writeChunk(outputStream, delta));
            writeEvent(outputStream, "done", "[DONE]");
        };

        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .header("Cache-Control", "no-cache")
            .header("X-Accel-Buffering", "no")
            .body(body);
    }

    private void writeChunk(OutputStream outputStream, String delta) {
        try {
            writeEvent(outputStream, null, objectMapper.writeValueAsString(Map.of("delta", delta)));
        } catch (IOException ex) {
            throw new IllegalStateException("流式响应写出失败", ex);
        }
    }

    private void writeEvent(OutputStream outputStream, String eventName, String data) throws IOException {
        StringBuilder builder = new StringBuilder();
        if (eventName != null && !eventName.isBlank()) {
            builder.append("event: ").append(eventName).append("\n");
        }
        builder.append("data: ").append(data).append("\n\n");
        outputStream.write(builder.toString().getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }
}
