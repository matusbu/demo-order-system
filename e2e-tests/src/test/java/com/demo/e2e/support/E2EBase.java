package com.demo.e2e.support;

import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.time.Duration;

@ExtendWith(SerenityJUnit5Extension.class)
public abstract class E2EBase {

    protected static ComposeContainer environment;
    protected static int orderEnginePort;
    protected static int paymentServicePort;
    protected static int stockServicePort;

    @BeforeAll
    static void startEnvironment() {
        environment = new ComposeContainer(new File("../docker-compose.yml"))
                .withExposedService("order-engine", 8080,
                        Wait.forHttp("/orders?customerName=probe")
                                .forStatusCode(200)
                                .withStartupTimeout(Duration.ofSeconds(120)))
                .withExposedService("payment-service", 8081,
                        Wait.forListeningPort()
                                .withStartupTimeout(Duration.ofSeconds(60)))
                .withExposedService("stock-service", 8082,
                        Wait.forListeningPort()
                                .withStartupTimeout(Duration.ofSeconds(60)))
                .withLocalCompose(true);

        environment.start();

        orderEnginePort    = environment.getServicePort("order-engine",    8080);
        paymentServicePort = environment.getServicePort("payment-service", 8081);
        stockServicePort   = environment.getServicePort("stock-service",   8082);
    }

    @AfterAll
    static void stopEnvironment() {
        if (environment != null) {
            environment.stop();
        }
    }
}
