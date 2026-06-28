package com.huangbo.adhd.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huangbo.adhd.config.WeChatProperties;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class WeChatAuthClient {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final WeChatProperties weChatProperties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public WeChatAuthClient(WeChatProperties weChatProperties, ObjectMapper objectMapper) {
        this.weChatProperties = weChatProperties;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder().baseUrl("https://api.weixin.qq.com").build();
    }

    public String exchangeOpenId(String code) {
        if (weChatProperties.getAppId() == null || weChatProperties.getAppId().isBlank()) {
            throw new IllegalStateException("WECHAT_APP_ID 未配置");
        }
        if (weChatProperties.getAppSecret() == null || weChatProperties.getAppSecret().isBlank()) {
            throw new IllegalStateException("WECHAT_APP_SECRET 未配置");
        }
        if (weChatProperties.getAppSecret().contains("你的真实Secret")) {
            throw new IllegalStateException("WECHAT_APP_SECRET 还是占位符，请换成微信后台的真实 AppSecret");
        }

        String responseBody = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/sns/jscode2session")
                .queryParam("appid", weChatProperties.getAppId())
                .queryParam("secret", weChatProperties.getAppSecret())
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .build())
            .retrieve()
            .bodyToMono(String.class)
            .block();

        Map<String, Object> response;
        try {
            response = objectMapper.readValue(responseBody, MAP_TYPE);
        } catch (Exception ex) {
            String message = responseBody == null || responseBody.isBlank() ? "空响应" : responseBody;
            throw new IllegalStateException("微信登录失败，微信返回了非 JSON 内容: " + message, ex);
        }

        if (response == null || response.get("openid") == null) {
            Object errorCode = response == null ? "unknown" : response.getOrDefault("errcode", "unknown");
            Object errorMessage = response == null ? "微信登录失败" : response.getOrDefault("errmsg", "微信登录失败");
            throw new IllegalStateException("微信登录失败: " + errorCode + " " + errorMessage);
        }
        return String.valueOf(response.get("openid"));
    }
}
