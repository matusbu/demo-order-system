package com.demo.e2e.tasks;

import com.demo.e2e.interactions.CreateOrder;
import com.demo.e2e.questions.TheLastOrderId;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;
import net.serenitybdd.screenplay.Task;

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
        actor.attemptsTo(CreateOrder.with(customerName, productName, quantity));
        actor.remember("orderId", TheLastOrderId.fromTheLastResponse().answeredBy(actor));
    }
}
