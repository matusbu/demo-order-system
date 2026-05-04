package com.demo.e2e.actor;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.rest.abilities.CallAnApi;

public class Customer {

    public static Actor named(String name, String orderEngineBaseUrl) {
        return Actor.named(name)
                .whoCan(CallAnApi.at(orderEngineBaseUrl));
    }
}
