package com.demo.e2e.tasks;

import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.rest.interactions.Post;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class PlaceAnOrder implements Task {

    private final String customerName;
    private final String productName;
    private final int quantity;

    public PlaceAnOrder(String customerName, String productName, int quantity) {
        this.customerName = customerName;
        this.productName  = productName;
        this.quantity     = quantity;
    }

    public static Performable with(String customerName, String productName, int quantity) {
        return instrumented(PlaceAnOrder.class, customerName, productName, quantity);
    }

    @Override
    @Step("{0} places an order for #quantity x #productName")
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Post.to("/orders")
                        .with(request -> request
                                .header("Content-Type", "application/json")
                                .body(String.format(
                                        "{\"customerName\":\"%s\",\"productName\":\"%s\",\"quantity\":%d}",
                                        customerName, productName, quantity)))
        );
        String orderId = SerenityRest.lastResponse().jsonPath().getString("id");
        actor.remember("orderId", orderId);
    }
}
