package com.bitcomputer.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.seed")
public record SeedProperties(boolean enabled, String adminEmail, String adminPassword,
                             String employeeEmail, String employeePassword,
                             String terminatedEmail, String terminatedPassword) {}
