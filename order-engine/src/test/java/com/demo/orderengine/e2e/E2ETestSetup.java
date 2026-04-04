package com.demo.orderengine.e2e;

import com.demo.orderengine.e2e.actors.Client;
import com.demo.orderengine.e2e.actors.PaymentSystem;
import com.demo.orderengine.e2e.actors.WarehouseSystem;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.time.Duration;

/**
 * Starts the full service stack once for the entire E2E test suite and wires
 * the three test actors to their respective services.
 *
 * Uses DockerComposeContainer with the local docker-compose binary so that
 * relative build contexts (e.g. build: ./order-engine) resolve correctly from
 * the repo root where docker-compose.e2e.yml lives.
 *
 * Container lifecycle: started once in the static block (singleton pattern),
 * stopped automatically via TestContainers' JVM shutdown hook.
 */
public abstract class E2ETestSetup {

    private static final DockerComposeContainer<?> COMPOSE;

    protected static final Client client;
    protected static final PaymentSystem paymentSystem;
    protected static final WarehouseSystem warehouseSystem;

    static {
        // Path is relative to the Maven module working directory (order-engine/).
        // ../docker-compose.e2e.yml resolves to the repo root.
        COMPOSE = new DockerComposeContainer<>(new File("../docker-compose.e2e.yml"))
                .withLocalCompose(true)
                .withExposedService("order-engine", 8080,
                        Wait.forHttp("/orders?customerName=ping")
                                .forStatusCode(200)
                                .withStartupTimeout(Duration.ofMinutes(5)))
                .withExposedService("payment-service", 8081,
                        Wait.forListeningPort()
                                .withStartupTimeout(Duration.ofMinutes(3)))
                .withExposedService("stock-service", 8082,
                        Wait.forListeningPort()
                                .withStartupTimeout(Duration.ofMinutes(3)));

        COMPOSE.start();

        String orderEngineUrl = "http://"
                + COMPOSE.getServiceHost("order-engine", 8080) + ":"
                + COMPOSE.getServicePort("order-engine", 8080);
        String paymentUrl = "http://"
                + COMPOSE.getServiceHost("payment-service", 8081) + ":"
                + COMPOSE.getServicePort("payment-service", 8081);
        String stockUrl = "http://"
                + COMPOSE.getServiceHost("stock-service", 8082) + ":"
                + COMPOSE.getServicePort("stock-service", 8082);

        client = new Client(orderEngineUrl);
        paymentSystem = new PaymentSystem(paymentUrl);
        warehouseSystem = new WarehouseSystem(stockUrl);
    }
}
