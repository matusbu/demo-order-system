package com.demo.e2e.interactions;

import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.rest.interactions.Post;

public class CreateOrder implements Interaction {

    private final String customerName;
    private final String productName;
    private final int quantity;

    private CreateOrder(String customerName, String productName, int quantity) {
        this.customerName = customerName;
        this.productName = productName;
        this.quantity = quantity;
    }

    public static CreateOrder with(String customerName, String productName, int quantity) {
        return new CreateOrder(customerName, productName, quantity);
    }

    @Override
    @Step("{0} posts an order for #quantity x #productName")
    public <T extends Actor> void performAs(T actor) {
        String body = String.format(
                "{\"customerName\":\"%s\",\"productName\":\"%s\",\"quantity\":%d}",
                customerName, productName, quantity);
        actor.attemptsTo(
                Post.to("/orders")
                        .with(request -> request
                                .header("Content-Type", "application/json")
                                .body(body))
        );
    }
}
