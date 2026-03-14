package com.demo.stockservice.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integrations")
public record IntegrationProperties(ServiceConfig orderEngine) {
    public record ServiceConfig(String url) {}
}
