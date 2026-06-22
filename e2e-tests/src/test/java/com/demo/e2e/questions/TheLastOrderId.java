package com.demo.e2e.questions;

import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;

public class TheLastOrderId implements Question<String> {

    private TheLastOrderId() {
    }

    public static TheLastOrderId fromTheLastResponse() {
        return new TheLastOrderId();
    }

    @Override
    public String answeredBy(Actor actor) {
        return SerenityRest.lastResponse().jsonPath().getString("id");
    }
}
