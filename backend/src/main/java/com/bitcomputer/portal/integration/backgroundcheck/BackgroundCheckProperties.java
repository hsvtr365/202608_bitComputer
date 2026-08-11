package com.bitcomputer.portal.integration.backgroundcheck;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.background-check")
public record BackgroundCheckProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {}
