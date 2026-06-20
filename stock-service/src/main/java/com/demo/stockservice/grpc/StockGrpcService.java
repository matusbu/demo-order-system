package com.demo.stockservice.grpc;

import com.demo.stockservice.service.StockService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
public class StockGrpcService extends StockServiceGrpc.StockServiceImplBase {

    private final StockService stockService;

    public StockGrpcService(StockService stockService) {
        this.stockService = stockService;
    }

    @Override
    public void reserve(ReserveRequest request, StreamObserver<StockReply> responseObserver) {
        handle(responseObserver, () -> {
            UUID orderId = requireOrderId(request.getOrderId());
            if (request.getProductName().isBlank()) {
                throw invalidArgument("productName must not be blank");
            }
            if (request.getQuantity() <= 0) {
                throw invalidArgument("quantity must be positive");
            }
            stockService.reserve(orderId, request.getProductName(), request.getQuantity());
        });
    }

    @Override
    public void cancelReservation(OrderIdRequest request, StreamObserver<StockReply> responseObserver) {
        handle(responseObserver, () -> stockService.cancelReservation(requireOrderId(request.getOrderId())));
    }

    @Override
    public void ship(OrderIdRequest request, StreamObserver<StockReply> responseObserver) {
        handle(responseObserver, () -> stockService.ship(requireOrderId(request.getOrderId())));
    }

    @Override
    public void simulateStockAvailable(OrderIdRequest request, StreamObserver<StockReply> responseObserver) {
        handle(responseObserver, () -> stockService.simulateStockAvailable(requireOrderId(request.getOrderId())));
    }

    @Override
    public void simulateStockSoldOut(OrderIdRequest request, StreamObserver<StockReply> responseObserver) {
        handle(responseObserver, () -> stockService.simulateStockSoldOut(requireOrderId(request.getOrderId())));
    }

    @Override
    public void simulateShipped(OrderIdRequest request, StreamObserver<StockReply> responseObserver) {
        handle(responseObserver, () -> stockService.simulateShipped(requireOrderId(request.getOrderId())));
    }

    @Override
    public void simulateDeliveryConfirmed(OrderIdRequest request, StreamObserver<StockReply> responseObserver) {
        handle(responseObserver, () -> stockService.simulateDeliveryConfirmed(requireOrderId(request.getOrderId())));
    }

    private void handle(StreamObserver<StockReply> responseObserver, Runnable action) {
        try {
            action.run();
            reply(responseObserver);
        } catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        }
    }

    private UUID requireOrderId(String rawOrderId) {
        if (rawOrderId == null || rawOrderId.isBlank()) {
            throw invalidArgument("orderId must not be blank");
        }
        try {
            return UUID.fromString(rawOrderId);
        } catch (IllegalArgumentException e) {
            throw invalidArgument("orderId must be a valid UUID");
        }
    }

    private StatusRuntimeException invalidArgument(String description) {
        return Status.INVALID_ARGUMENT.withDescription(description).asRuntimeException();
    }

    private void reply(StreamObserver<StockReply> responseObserver) {
        responseObserver.onNext(StockReply.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
