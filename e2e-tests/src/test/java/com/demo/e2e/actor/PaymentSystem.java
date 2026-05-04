package com.demo.e2e.actor;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.rest.abilities.CallAnApi;

public class PaymentSystem {

    public static Actor named(String name, String paymentServiceBaseUrl) {
        return Actor.named(name)
                .whoCan(CallAnApi.at(paymentServiceBaseUrl));
    }
}
