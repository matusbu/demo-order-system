package com.demo.e2e.support;

import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.Duration;

@Testcontainers
public abstract class E2EBase {

    @Container
    static final ComposeContainer STACK = new ComposeContainer(
            new File("docker-compose-e2e.yml"))
            .withExposedService("order-engine", 8080,
                    Wait.forHttp("/orders?customerName=probe")
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofMinutes(2)))
            .withExposedService("payment-service", 8081,
                    Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
            .withExposedService("stock-service", 8082,
                    Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
            .withLocalCompose(true);

    protected static String orderEngineUrl() {
        return "http://" + STACK.getServiceHost("order-engine", 8080)
                + ":" + STACK.getServicePort("order-engine", 8080);
    }

    protected static String paymentServiceUrl() {
        return "http://" + STACK.getServiceHost("payment-service", 8081)
                + ":" + STACK.getServicePort("payment-service", 8081);
    }

    protected static String stockServiceUrl() {
        return "http://" + STACK.getServiceHost("stock-service", 8082)
                + ":" + STACK.getServicePort("stock-service", 8082);
    }
}
