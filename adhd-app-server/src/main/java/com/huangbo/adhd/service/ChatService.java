package com.huangbo.adhd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huangbo.adhd.dto.ChatHistoryItem;
import com.huangbo.adhd.dto.ChatResponse;
import com.huangbo.adhd.entity.ChatLog;
import com.huangbo.adhd.integration.ModelClient;
import com.huangbo.adhd.mapper.ChatLogMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final String SYSTEM_PROMPT = "你是 ADHD 家长助手。直接给用户最终答复，第一句必须是简体中文。严禁输出 Thinking Process、Analyze the Request、Role、Constraints、analysis、reasoning、英文草稿或任何思考过程。默认并优先使用简体中文回答，除非用户明确要求其他语言；不要中英混杂。风格要共情、具体、温和，先安抚情绪，再给出今晚就能执行的一步建议，不要使用医疗诊断语气。适合用简洁的小标题、短列表和 markdown 强调重点。";

    private final ChatLogMapper chatLogMapper;
    private final ModelClient modelClient;

    public ChatService(ChatLogMapper chatLogMapper, ModelClient modelClient) {
        this.chatLogMapper = chatLogMapper;
        this.modelClient = modelClient;
    }

    public ChatResponse chat(Long userId, String message) {
        save(userId, "user", message);
        String reply = generateReply(message);
        save(userId, "assistant", reply);
        return new ChatResponse(reply);
    }

    public List<ChatHistoryItem> getHistory(Long userId) {
        return chatLogMapper.selectList(
            new LambdaQueryWrapper<ChatLog>()
                .eq(ChatLog::getUserId, userId)
                .orderByAsc(ChatLog::getCreatedAt, ChatLog::getId)
        ).stream().map(log -> new ChatHistoryItem(
            log.getRole(),
            log.getContent(),
            log.getCreatedAt()
        )).toList();
    }

    public void chatStream(Long userId, String message, Consumer<String> onDelta) {
        save(userId, "user", message);

        StringBuilder reply = new StringBuilder();
        try {
            modelClient.chatStream(SYSTEM_PROMPT, message, delta -> {
                reply.append(delta);
                onDelta.accept(delta);
            });
        } catch (Exception ex) {
            logModelFallback("stream", ex);
            String fallback = fallbackReply();
            reply.append(fallback);
            streamText(fallback, onDelta);
        }

        save(userId, "assistant", reply.toString());
    }

    private String generateReply(String message) {
        try {
            return modelClient.chat(SYSTEM_PROMPT, message);
        } catch (Exception ex) {
            logModelFallback("normal", ex);
            return fallbackReply();
        }
    }

    private void logModelFallback(String mode, Exception ex) {
        String message = rootMessage(ex);
        if (message.contains("仅返回了 reasoning_content") || message.contains("思考过程而不是最终答复")) {
            log.info("Model {} chat returned reasoning only, using fallback reply: {}", mode, message);
            return;
        }
        log.warn("Model {} chat failed, using fallback reply: {}", mode, message);
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    private String fallbackReply() {
        return "## 先别急着否定自己\n\n我听到你现在已经很累了，这不代表你做得不够，而是今天真的消耗很大。\n\n### 今晚只做一小步\n\n- 把眼前任务缩成 **10 分钟**\n- 开始前先说一句：**你已经很努力了，我们先试一小步**\n- 10 分钟结束后，不管完成多少，都先停下来给一点肯定\n\n如果你愿意，我可以继续帮你把下一步拆得更细。";
    }

    private void streamText(String text, Consumer<String> onDelta) {
        int chunkSize = 8;
        for (int start = 0; start < text.length(); start += chunkSize) {
            int end = Math.min(text.length(), start + chunkSize);
            onDelta.accept(text.substring(start, end));
            try {
                Thread.sleep(25);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void save(Long userId, String role, String content) {
        ChatLog log = new ChatLog();
        log.setUserId(userId);
        log.setRole(role);
        log.setContent(content);
        log.setCreatedAt(LocalDateTime.now());
        chatLogMapper.insert(log);
    }
}
