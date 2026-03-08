package com.demo.orderengine.config;

import com.demo.orderengine.integration.IntegrationClient;
import com.demo.orderengine.integration.IntegrationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(IntegrationProperties.class)
public class RestClientConfig {

    @Bean
    public IntegrationClient integrationClient(RestClient.Builder builder, IntegrationProperties props) {
        RestClient paymentClient = builder.clone()
                .baseUrl(props.paymentService().url())
                .build();
        RestClient stockClient = builder.clone()
                .baseUrl(props.stockService().url())
                .build();
        return new IntegrationClient(paymentClient, stockClient);
    }
}
