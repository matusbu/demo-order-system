package com.demo.orderengine.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integrations")
public record IntegrationProperties(
        ServiceConfig paymentService,
        ServiceConfig stockService
) {
    public record ServiceConfig(String url) {}
}
