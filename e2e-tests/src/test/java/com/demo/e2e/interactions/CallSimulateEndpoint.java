package com.demo.e2e.interactions;

import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.rest.interactions.Post;

public class CallSimulateEndpoint implements Interaction {

    private final String path;
    private String orderId;
    private String extraFields = "";

    private CallSimulateEndpoint(String path) {
        this.path = path;
    }

    public static CallSimulateEndpoint at(String path) {
        return new CallSimulateEndpoint(path);
    }

    public CallSimulateEndpoint withOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public CallSimulateEndpoint andExtraFields(String extraFields) {
        this.extraFields = extraFields;
        return this;
    }

    @Override
    @Step("{0} calls simulate endpoint #path")
    public <T extends Actor> void performAs(T actor) {
        String body = String.format("{\"orderId\":\"%s\"%s}", orderId, extraFields);
        actor.attemptsTo(
                Post.to(path)
                        .with(request -> request
                                .header("Content-Type", "application/json")
                                .body(body))
        );
    }
}
