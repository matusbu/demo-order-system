package com.demo.paymentservice.config;

import com.demo.paymentservice.integration.IntegrationProperties;
import com.demo.paymentservice.integration.OrderEngineClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableConfigurationProperties(IntegrationProperties.class)
public class AppConfig {

    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newScheduledThreadPool(4);
    }

    @Bean
    public OrderEngineClient orderEngineClient(RestClient.Builder builder, IntegrationProperties props) {
        RestClient restClient = builder.baseUrl(props.orderEngine().url()).build();
        return new OrderEngineClient(restClient);
    }
}
