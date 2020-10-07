/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.kafka;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

/**
 * Matcher for {@link CompletionStage}
 * <p>
 * Can test for done-ness, and assert the value or the exception
 */
public class CompletionStageMatcher<T> extends TypeSafeDiagnosingMatcher<CompletionStage<? extends T>> {

    private enum State {
        INCOMPLETE,
        SUCCEEDED,
        FAILED
    }

    private enum ExpectedState {
        INCOMPLETE(State.INCOMPLETE),
        SUCCEEDED(State.SUCCEEDED),
        FAILED(State.FAILED),
        COMPLETE(State.SUCCEEDED, State.FAILED),
        ANY(State.INCOMPLETE, State.SUCCEEDED, State.FAILED);

        private List<State> validStates;

        private ExpectedState(State... validStates) {
            this.validStates = Arrays.asList(validStates);
        }

        private boolean isValid(State state) {
            return validStates.contains(state);
        }
    }

    private final ExpectedState expectedState;
    private final Matcher<? extends T> valueMatcher;
    private final Matcher<? extends Throwable> exceptionMatcher;

    private CompletionStageMatcher(ExpectedState expectedState, Matcher<? extends T> valueMatcher, Matcher<? extends Throwable> exceptionMatcher) {
        this.expectedState = expectedState;
        this.valueMatcher = valueMatcher;
        this.exceptionMatcher = exceptionMatcher;
    }

    @Override
    public void describeTo(Description desc) {
        desc.appendText("CompletionStage in state ");
        desc.appendValue(expectedState);

        if (expectedState.isValid(State.SUCCEEDED) && (valueMatcher != null)) {
            desc.appendText(" with value ");
            desc.appendDescriptionOf(valueMatcher);
        }

        if (expectedState.isValid(State.FAILED) && (exceptionMatcher != null)) {
            desc.appendText(" with exception ");
            desc.appendDescriptionOf(exceptionMatcher);
        }
    }

    @Override
    protected boolean matchesSafely(CompletionStage<? extends T> stage, Description desc) {
        CompletableFuture<? extends T> future = stage.toCompletableFuture();
        State actualState;
        Throwable exception = null;
        T value = null;

        if (future.isDone() == false) {
            actualState = State.INCOMPLETE;
        } else {
            try {
                value = future.get();
                actualState = State.SUCCEEDED;
            } catch (ExecutionException e) {
                exception = e.getCause();
                actualState = State.FAILED;
            } catch (InterruptedException e) {
                throw new IllegalStateException("Interrupted checking CompletionStage which was complete");
            }
        }

        boolean result = true;

        if (!expectedState.isValid(actualState)) {
            result = false;
        }

        if ((actualState == State.FAILED) && (exceptionMatcher != null)) {
            if (!exceptionMatcher.matches(exception)) {
                result = false;
            }
        }

        if ((actualState == State.SUCCEEDED) && (valueMatcher != null)) {
            if (!valueMatcher.matches(value)) {
                result = false;
            }
        }

        desc.appendText("was CompletionStage in state ");
        desc.appendValue(actualState);

        if (actualState == State.SUCCEEDED) {
            desc.appendText(" with value ");
            desc.appendValue(value);
        }

        if (actualState == State.FAILED) {
            desc.appendText(" with exception ");
            desc.appendValue(exception);
        }

        return result;
    }

    public static <T> CompletionStageMatcher<T> completed() {
        return new CompletionStageMatcher<>(ExpectedState.COMPLETE, null, null);
    }

    public static <T> CompletionStageMatcher<T> incomplete() {
        return new CompletionStageMatcher<>(ExpectedState.INCOMPLETE, null, null);
    }

    public static <T> CompletionStageMatcher<T> succeeded() {
        return new CompletionStageMatcher<>(ExpectedState.SUCCEEDED, null, null);
    }

    public static <T> CompletionStageMatcher<T> succeededWith(Matcher<? extends T> valueMatcher) {
        return new CompletionStageMatcher<>(ExpectedState.SUCCEEDED, valueMatcher, null);
    }

    public static <T> CompletionStageMatcher<T> failed() {
        return new CompletionStageMatcher<>(ExpectedState.FAILED, null, null);
    }

    public static <T> CompletionStageMatcher<T> failedWith(Matcher<? extends Throwable> exceptionMatcher) {
        return new CompletionStageMatcher<>(ExpectedState.FAILED, null, exceptionMatcher);
    }

}
