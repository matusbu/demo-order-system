package com.demo.stockservice.grpc;

import com.demo.stockservice.service.StockService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class StockGrpcServiceTest {

    @Mock
    private StockService stockService;

    private Server server;
    private ManagedChannel channel;
    private StockServiceGrpc.StockServiceBlockingStub stub;

    @BeforeEach
    void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(new StockGrpcService(stockService))
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build();
        stub = StockServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() throws Exception {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    // --- Reserve ---

    @Test
    void reserve_validRequest_delegatesToService() {
        UUID orderId = UUID.randomUUID();

        stub.reserve(ReserveRequest.newBuilder()
                .setOrderId(orderId.toString())
                .setProductName("iPhone 15")
                .setQuantity(2)
                .build());

        verify(stockService).reserve(orderId, "iPhone 15", 2);
    }

    @Test
    void reserve_missingOrderId_throwsInvalidArgument() {
        assertInvalidArgument(() -> stub.reserve(ReserveRequest.newBuilder()
                .setProductName("iPhone 15")
                .setQuantity(2)
                .build()));
    }

    @Test
    void reserve_blankProductName_throwsInvalidArgument() {
        assertInvalidArgument(() -> stub.reserve(ReserveRequest.newBuilder()
                .setOrderId(UUID.randomUUID().toString())
                .setProductName("")
                .setQuantity(2)
                .build()));
    }

    @Test
    void reserve_zeroQuantity_throwsInvalidArgument() {
        assertInvalidArgument(() -> stub.reserve(ReserveRequest.newBuilder()
                .setOrderId(UUID.randomUUID().toString())
                .setProductName("iPhone 15")
                .setQuantity(0)
                .build()));
    }

    // --- CancelReservation ---

    @Test
    void cancelReservation_validRequest_delegatesToService() {
        UUID orderId = UUID.randomUUID();

        stub.cancelReservation(OrderIdRequest.newBuilder().setOrderId(orderId.toString()).build());

        verify(stockService).cancelReservation(orderId);
    }

    // --- Ship ---

    @Test
    void ship_validRequest_delegatesToService() {
        UUID orderId = UUID.randomUUID();

        stub.ship(OrderIdRequest.newBuilder().setOrderId(orderId.toString()).build());

        verify(stockService).ship(orderId);
    }

    // --- SimulateStockAvailable ---

    @Test
    void simulateStockAvailable_validRequest_delegatesToService() {
        UUID orderId = UUID.randomUUID();

        stub.simulateStockAvailable(OrderIdRequest.newBuilder().setOrderId(orderId.toString()).build());

        verify(stockService).simulateStockAvailable(orderId);
    }

    @Test
    void simulateStockAvailable_missingOrderId_throwsInvalidArgument() {
        assertInvalidArgument(() -> stub.simulateStockAvailable(OrderIdRequest.newBuilder().build()));
    }

    // --- SimulateStockSoldOut ---

    @Test
    void simulateStockSoldOut_validRequest_delegatesToService() {
        UUID orderId = UUID.randomUUID();

        stub.simulateStockSoldOut(OrderIdRequest.newBuilder().setOrderId(orderId.toString()).build());

        verify(stockService).simulateStockSoldOut(orderId);
    }

    @Test
    void simulateStockSoldOut_missingOrderId_throwsInvalidArgument() {
        assertInvalidArgument(() -> stub.simulateStockSoldOut(OrderIdRequest.newBuilder().build()));
    }

    // --- SimulateShipped ---

    @Test
    void simulateShipped_validRequest_delegatesToService() {
        UUID orderId = UUID.randomUUID();

        stub.simulateShipped(OrderIdRequest.newBuilder().setOrderId(orderId.toString()).build());

        verify(stockService).simulateShipped(orderId);
    }

    @Test
    void simulateShipped_missingOrderId_throwsInvalidArgument() {
        assertInvalidArgument(() -> stub.simulateShipped(OrderIdRequest.newBuilder().build()));
    }

    // --- SimulateDeliveryConfirmed ---

    @Test
    void simulateDeliveryConfirmed_validRequest_delegatesToService() {
        UUID orderId = UUID.randomUUID();

        stub.simulateDeliveryConfirmed(OrderIdRequest.newBuilder().setOrderId(orderId.toString()).build());

        verify(stockService).simulateDeliveryConfirmed(orderId);
    }

    @Test
    void simulateDeliveryConfirmed_missingOrderId_throwsInvalidArgument() {
        assertInvalidArgument(() -> stub.simulateDeliveryConfirmed(OrderIdRequest.newBuilder().build()));
    }

    // --- helpers ---

    private void assertInvalidArgument(ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(e -> assertThat(((StatusRuntimeException) e).getStatus().getCode())
                        .isEqualTo(Status.INVALID_ARGUMENT.getCode()));
    }
}
