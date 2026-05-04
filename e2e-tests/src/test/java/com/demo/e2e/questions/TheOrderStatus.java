package com.demo.e2e.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.rest.interactions.Get;

public class TheOrderStatus implements Question<String> {

    private final String orderId;

    private TheOrderStatus(String orderId) {
        this.orderId = orderId;
    }

    public static TheOrderStatus forOrderWithId(String orderId) {
        return new TheOrderStatus(orderId);
    }

    @Override
    public String answeredBy(Actor actor) {
        actor.attemptsTo(Get.resource("/orders/" + orderId));
        return SerenityRest.lastResponse().jsonPath().getString("status");
    }
}
