package com.demo.e2e.actor;

import com.demo.e2e.support.OrderStatus;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Represents a customer who places and cancels orders and observes status updates
 * via WebSocket.
 */
public class Customer implements AutoCloseable {

    private final String name;
    private final String orderEngineBaseUrl;
    private final LinkedBlockingQueue<StatusFrame> receivedFrames = new LinkedBlockingQueue<>();

    private StompSession stompSession;
    private UUID lastOrderId;

    public Customer(String name, String orderEngineBaseUrl) {
        this.name = name;
        this.orderEngineBaseUrl = orderEngineBaseUrl;
        connect();
    }

    private void connect() {
        List<Transport> transports = List.of(
                new WebSocketTransport(new StandardWebSocketClient()),
                new RestTemplateXhrTransport()
        );
        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(transports));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        CountDownLatch connected = new CountDownLatch(1);

        stompClient.connectAsync(orderEngineBaseUrl + "/ws/orders", new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                stompSession = session;
                session.subscribe("/topic/orders/" + name, new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return Map.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) payload;
                        receivedFrames.add(new StatusFrame(
                                UUID.fromString((String) map.get("orderId")),
                                (String) map.get("status")));
                    }
                });
                connected.countDown();
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                connected.countDown();
            }
        });

        try {
            if (!connected.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("WebSocket connection to order-engine timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while connecting to WebSocket");
        }
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    public void placesAnOrderFor(String productName, int quantity) {
        Map<?, ?> response = given()
                .baseUri(orderEngineBaseUrl)
                .contentType("application/json")
                .body(Map.of("customerName", name, "productName", productName, "quantity", quantity))
                .post("/orders")
                .then()
                .statusCode(201)
                .extract().as(Map.class);
        lastOrderId = UUID.fromString((String) response.get("id"));
    }

    public void cancelsOrder() {
        given()
                .baseUri(orderEngineBaseUrl)
                .delete("/orders/{id}/cancel", lastOrderId)
                .then()
                .statusCode(200);
    }

    // ── Verifications ─────────────────────────────────────────────────────────

    public void mustSeeOrderInStatus(OrderStatus expected) {
        try {
            StatusFrame frame = receivedFrames.poll(20, TimeUnit.SECONDS);
            assertThat(frame)
                    .as("Expected WebSocket frame with status %s but none arrived within 20 s", expected)
                    .isNotNull();
            assertThat(frame.status())
                    .as("WebSocket frame status")
                    .isEqualTo(expected.name());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting for status update: " + expected);
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public UUID lastOrderId() {
        return lastOrderId;
    }

    @Override
    public void close() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
    }

    private record StatusFrame(UUID orderId, String status) {}
}
