package com.demo.e2e.abilities;

import com.demo.stockservice.grpc.StockServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import net.serenitybdd.screenplay.Ability;

public class CallStockServiceGrpc implements Ability {

    private final ManagedChannel channel;
    private final StockServiceGrpc.StockServiceBlockingStub stub;

    private CallStockServiceGrpc(ManagedChannel channel) {
        this.channel = channel;
        this.stub = StockServiceGrpc.newBlockingStub(channel);
    }

    public static CallStockServiceGrpc at(String host, int port) {
        ManagedChannel channel = NettyChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        return new CallStockServiceGrpc(channel);
    }

    public StockServiceGrpc.StockServiceBlockingStub getStub() {
        return stub;
    }

    public void shutdown() {
        channel.shutdownNow();
    }
}
