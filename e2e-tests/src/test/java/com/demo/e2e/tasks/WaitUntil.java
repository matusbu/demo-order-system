package com.demo.e2e.tasks;

import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.Task;

import java.time.Duration;

import static net.serenitybdd.screenplay.Tasks.instrumented;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class WaitUntil<T> implements Task {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final Question<T> question;
    private final T expectedValue;

    public WaitUntil(Question<T> question, T expectedValue) {
        this.question = question;
        this.expectedValue = expectedValue;
    }

    public static <T> ValueExpectation<T> the(Question<T> question) {
        return new ValueExpectation<>(question);
    }

    @Override
    @Step("{0} waits until answer is #expectedValue")
    public <A extends Actor> void performAs(A actor) {
        await().atMost(TIMEOUT).untilAsserted(() ->
                assertThat(question.answeredBy(actor)).isEqualTo(expectedValue)
        );
    }

    public static class ValueExpectation<T> {
        private final Question<T> question;

        ValueExpectation(Question<T> question) {
            this.question = question;
        }

        @SuppressWarnings("unchecked")
        public Performable isEqualTo(T expectedValue) {
            return instrumented(WaitUntil.class, question, expectedValue);
        }
    }
}
