package com.demo.orderengine.integration;

import com.demo.stockservice.grpc.OrderIdRequest;
import com.demo.stockservice.grpc.ReserveRequest;
import com.demo.stockservice.grpc.StockReply;
import com.demo.stockservice.grpc.StockServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@GrpcService
public class StockServiceTestDouble extends StockServiceGrpc.StockServiceImplBase {

    public final List<ReserveRequest> reserveRequests = new CopyOnWriteArrayList<>();
    public final List<String> cancelReservationOrderIds = new CopyOnWriteArrayList<>();
    public final List<String> shipOrderIds = new CopyOnWriteArrayList<>();

    @Override
    public void reserve(ReserveRequest request, StreamObserver<StockReply> responseObserver) {
        reserveRequests.add(request);
        reply(responseObserver);
    }

    @Override
    public void cancelReservation(OrderIdRequest request, StreamObserver<StockReply> responseObserver) {
        cancelReservationOrderIds.add(request.getOrderId());
        reply(responseObserver);
    }

    @Override
    public void ship(OrderIdRequest request, StreamObserver<StockReply> responseObserver) {
        shipOrderIds.add(request.getOrderId());
        reply(responseObserver);
    }

    public void reset() {
        reserveRequests.clear();
        cancelReservationOrderIds.clear();
        shipOrderIds.clear();
    }

    private void reply(StreamObserver<StockReply> responseObserver) {
        responseObserver.onNext(StockReply.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
