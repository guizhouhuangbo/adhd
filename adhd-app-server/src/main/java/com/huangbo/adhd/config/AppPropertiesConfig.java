package com.huangbo.adhd.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ModelProperties.class, WeChatProperties.class})
public class AppPropertiesConfig {
}
