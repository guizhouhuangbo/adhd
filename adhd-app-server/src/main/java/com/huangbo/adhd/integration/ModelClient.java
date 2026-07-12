package com.huangbo.adhd.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huangbo.adhd.config.ModelProperties;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ModelClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final int STREAM_DECISION_CHARS = 160;
    private static final int MAX_TOKENS = 1200;

    private final ModelProperties modelProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ModelClient(ModelProperties modelProperties, ObjectMapper objectMapper) {
        this.modelProperties = modelProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
    }

    public String chat(String systemPrompt, String userPrompt) {
        WebClient webClient = WebClient.builder()
            .baseUrl(modelProperties.getBaseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + modelProperties.getApiKey())
            .build();

        Map<String, Object> request = Map.of(
            "model", modelProperties.getModel(),
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", "/no_think\n" + userPrompt)
            ),
            "temperature", 0.7,
            "max_tokens", MAX_TOKENS,
            "enable_thinking", false,
            "chat_template_kwargs", Map.of("enable_thinking", false)
        );

        String responseBody = webClient.post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(String.class)
            .block(REQUEST_TIMEOUT);

        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("模型服务无响应");
        }

        JsonNode root = readJson(responseBody);
        JsonNode firstChoice = root.path("choices").path(0);
        if (firstChoice.isMissingNode()) {
            throw new IllegalStateException("模型返回为空: " + abbreviate(responseBody));
        }

        String content = extractChoiceContent(firstChoice);
        if (content == null) {
            throw new IllegalStateException("模型未返回内容: " + summarizeChoice(firstChoice));
        }

        return sanitizeFinalAnswer(content);
    }

    public void chatStream(String systemPrompt, String userPrompt, Consumer<String> onDelta) {
        try {
            Map<String, Object> request = Map.of(
                "model", modelProperties.getModel(),
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", "/no_think\n" + userPrompt)
                ),
                "temperature", 0.7,
                "max_tokens", MAX_TOKENS,
                "stream", true,
                "enable_thinking", false,
                "chat_template_kwargs", Map.of("enable_thinking", false)
            );

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(modelProperties.getBaseUrl() + "/chat/completions"))
                .timeout(REQUEST_TIMEOUT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + modelProperties.getApiKey())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request), StandardCharsets.UTF_8))
                .build();

            HttpResponse<InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("模型服务调用失败: " + response.statusCode());
            }

            boolean hasContent = false;
            boolean streamingToUser = false;
            StringBuilder pendingContent = new StringBuilder();
            StringBuilder ignoredReasoning = new StringBuilder();
            try (
                InputStream body = response.body();
                BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) {
                        continue;
                    }

                    String data = line.substring(5).trim();
                    if (data.isEmpty()) {
                        continue;
                    }
                    if ("[DONE]".equals(data)) {
                        break;
                    }

                    JsonNode root = objectMapper.readTree(data);
                    JsonNode firstChoice = root.path("choices").path(0);
                    appendIfPresent(ignoredReasoning, firstNonBlank(
                        textValue(firstChoice.path("delta").path("reasoning_content")),
                        textValue(firstChoice.path("message").path("reasoning_content")),
                        textValue(firstChoice.path("reasoning_content"))
                    ));

                    String content = extractChoiceContent(firstChoice);
                    if (content != null) {
                        hasContent = true;
                        if (streamingToUser) {
                            onDelta.accept(content);
                            continue;
                        }

                        pendingContent.append(content);
                        String finalAnswer = extractFinalAnswer(pendingContent.toString());
                        if (finalAnswer != null) {
                            streamingToUser = true;
                            onDelta.accept(finalAnswer);
                            pendingContent.setLength(0);
                            continue;
                        }

                        if (pendingContent.length() >= STREAM_DECISION_CHARS) {
                            if (looksLikeThinkingProcess(pendingContent.toString())) {
                                throw new IllegalStateException("模型返回了思考过程而不是最终答复");
                            }
                            streamingToUser = true;
                            onDelta.accept(pendingContent.toString());
                            pendingContent.setLength(0);
                        }
                    }
                }
            }
            if (!hasContent) {
                throw new IllegalStateException("模型流式响应未返回内容"
                    + (ignoredReasoning.length() > 0 ? "，仅返回了 reasoning_content" : ""));
            }
            if (!streamingToUser && pendingContent.length() > 0) {
                String finalAnswer = sanitizeFinalAnswer(pendingContent.toString());
                onDelta.accept(finalAnswer);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("模型流式响应失败", ex);
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String extractChoiceContent(JsonNode firstChoice) {
        return firstNonBlank(
            textValue(firstChoice.path("delta").path("content")),
            textValue(firstChoice.path("message").path("content")),
            textValue(firstChoice.path("content")),
            textValue(firstChoice.path("text"))
        );
    }

    private String textValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isArray()) {
            String joined = StreamSupport.stream(node.spliterator(), false)
                .map(this::contentPartText)
                .filter(value -> value != null && !value.isBlank())
                .reduce("", (left, right) -> left + right);
            return joined.isBlank() ? null : joined;
        }
        return node.asText(null);
    }

    private String contentPartText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        return firstNonBlank(
            node.path("text").asText(null),
            node.path("content").asText(null)
        );
    }

    private JsonNode readJson(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception ex) {
            throw new IllegalStateException("模型返回不是合法 JSON: " + abbreviate(body), ex);
        }
    }

    private void appendIfPresent(StringBuilder builder, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(value);
        }
    }

    private String summarizeChoice(JsonNode choice) {
        return abbreviate(choice.toString());
    }

    private String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() <= 600 ? compact : compact.substring(0, 600) + "...";
    }

    private String sanitizeFinalAnswer(String content) {
        String finalAnswer = extractFinalAnswer(content);
        if (finalAnswer != null) {
            return finalAnswer;
        }
        if (looksLikeThinkingProcess(content)) {
            throw new IllegalStateException("模型返回了思考过程而不是最终答复");
        }
        return content;
    }

    private String extractFinalAnswer(String content) {
        String[] markers = {
            "Final Answer:",
            "Final answer:",
            "Answer:",
            "最终答复：",
            "最终答复:",
            "最终回答：",
            "最终回答:",
            "给用户的答复：",
            "给用户的答复:"
        };
        for (String marker : markers) {
            int index = content.indexOf(marker);
            if (index >= 0) {
                String answer = content.substring(index + marker.length()).trim();
                return answer.isBlank() ? null : answer;
            }
        }
        return null;
    }

    private boolean looksLikeThinkingProcess(String content) {
        String normalized = content.stripLeading().toLowerCase();
        return normalized.startsWith("thinking process")
            || normalized.startsWith("thought process")
            || normalized.startsWith("analysis")
            || normalized.startsWith("reasoning")
            || normalized.contains("analyze the request")
            || normalized.contains("constraints:")
            || normalized.contains("default language: simplified chinese");
    }
}
