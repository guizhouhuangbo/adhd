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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ModelClient {

    private final ModelProperties modelProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ModelClient(ModelProperties modelProperties, ObjectMapper objectMapper) {
        this.modelProperties = modelProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
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
                Map.of("role", "user", "content", userPrompt)
            ),
            "temperature", 0.7
        );

        Map<String, Object> response = webClient.post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        if (response == null) {
            throw new IllegalStateException("模型服务无响应");
        }

        Object choicesObj = response.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            throw new IllegalStateException("模型返回为空");
        }

        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) {
            throw new IllegalStateException("模型返回格式异常");
        }

        Object messageObj = firstMap.get("message");
        if (!(messageObj instanceof Map<?, ?> messageMap) || messageMap.get("content") == null) {
            throw new IllegalStateException("模型未返回内容");
        }

        return String.valueOf(messageMap.get("content"));
    }

    public void chatStream(String systemPrompt, String userPrompt, Consumer<String> onDelta) {
        try {
            Map<String, Object> request = Map.of(
                "model", modelProperties.getModel(),
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.7,
                "stream", true,
                "enable_thinking", false
            );

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(modelProperties.getBaseUrl() + "/chat/completions"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + modelProperties.getApiKey())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request), StandardCharsets.UTF_8))
                .build();

            HttpResponse<InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("模型服务调用失败: " + response.statusCode());
            }

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
                        return;
                    }

                    JsonNode root = objectMapper.readTree(data);
                    JsonNode contentNode = root.path("choices").path(0).path("delta").path("content");
                    if (!contentNode.isMissingNode() && !contentNode.isNull()) {
                        String content = contentNode.asText();
                        if (!content.isEmpty()) {
                            onDelta.accept(content);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("模型流式响应失败", ex);
        }
    }
}
