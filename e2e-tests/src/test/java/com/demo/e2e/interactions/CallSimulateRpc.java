package com.demo.e2e.interactions;

import com.demo.e2e.abilities.CallStockServiceGrpc;
import com.demo.stockservice.grpc.OrderIdRequest;
import com.demo.stockservice.grpc.StockReply;
import com.demo.stockservice.grpc.StockServiceGrpc;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;

import java.util.function.BiFunction;

public class CallSimulateRpc implements Interaction {

    private final String orderId;
    private final BiFunction<StockServiceGrpc.StockServiceBlockingStub, OrderIdRequest, StockReply> rpcCall;
    private final String description;

    private CallSimulateRpc(String orderId,
                             BiFunction<StockServiceGrpc.StockServiceBlockingStub, OrderIdRequest, StockReply> rpcCall,
                             String description) {
        this.orderId = orderId;
        this.rpcCall = rpcCall;
        this.description = description;
    }

    public static CallSimulateRpc stockAvailable(String orderId) {
        return new CallSimulateRpc(orderId, StockServiceGrpc.StockServiceBlockingStub::simulateStockAvailable, "stock available");
    }

    public static CallSimulateRpc stockSoldOut(String orderId) {
        return new CallSimulateRpc(orderId, StockServiceGrpc.StockServiceBlockingStub::simulateStockSoldOut, "stock sold out");
    }

    public static CallSimulateRpc shipped(String orderId) {
        return new CallSimulateRpc(orderId, StockServiceGrpc.StockServiceBlockingStub::simulateShipped, "shipped");
    }

    public static CallSimulateRpc deliveryConfirmed(String orderId) {
        return new CallSimulateRpc(orderId, StockServiceGrpc.StockServiceBlockingStub::simulateDeliveryConfirmed, "delivery confirmed");
    }

    @Override
    @Step("{0} simulates #description for order #orderId via gRPC")
    public <T extends Actor> void performAs(T actor) {
        CallStockServiceGrpc ability = actor.abilityTo(CallStockServiceGrpc.class);
        OrderIdRequest request = OrderIdRequest.newBuilder().setOrderId(orderId).build();
        rpcCall.apply(ability.getStub(), request);
    }
}
