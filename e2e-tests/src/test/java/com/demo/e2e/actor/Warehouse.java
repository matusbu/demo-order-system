package com.demo.e2e.actor;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.rest.abilities.CallAnApi;

public class Warehouse {

    public static Actor named(String name, String stockServiceBaseUrl) {
        return Actor.named(name)
                .whoCan(CallAnApi.at(stockServiceBaseUrl));
    }
}
