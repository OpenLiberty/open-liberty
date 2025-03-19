/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.test.utils;

import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import io.openliberty.microprofile.openapi20.internal.services.OASValidationResult;
import io.openliberty.microprofile.openapi20.internal.services.OASValidationResult.ValidationEvent;
import io.openliberty.microprofile.openapi20.internal.services.OASValidationResult.ValidationEvent.Severity;

/**
 * Checks whether an {@link OASValidationResult} contains the expected validation events
 * <p>
 * Examples:
 *
 * <pre>
 * <code>
 * assertThat(result, successful());
 * assertThat(result, hasError("The foo reference is not valid");
 * assertThat(result, failedWithEvents().error("some error")
 *                                      .warning(containsString("foo"));
 * </code>
 * </pre>
 */
public class ValidationResultMatcher extends TypeSafeDiagnosingMatcher<OASValidationResult> {

    private final List<ExpectedEvent> expectedEvents;
    private final boolean requireAll;

    public ValidationResultMatcher(boolean requireAll) {
        this.expectedEvents = new ArrayList<>();
        this.requireAll = true;
    }

    public ValidationResultMatcher(List<ExpectedEvent> expectedEvents) {
        this.expectedEvents = expectedEvents;
        this.requireAll = true;
    }

    @Override
    public void describeTo(Description desc) {
        desc.appendText("Validation result");
        if (expectedEvents.isEmpty()) {
            desc.appendText(" with no events");
        } else {
            desc.appendText(" with events");
            for (ExpectedEvent e : expectedEvents) {
                desc.appendText("\n   ");
                describeExpectedEvent(desc, e);
            }
        }
    }

    @Override
    protected boolean matchesSafely(OASValidationResult value, Description desc) {
        List<ValidationEvent> events = new ArrayList<>(value.getEvents());
        List<ExpectedEvent> unmetExpectations = new ArrayList<>();
        for (ExpectedEvent expected : expectedEvents) {
            boolean expectationMatched = false;
            for (ValidationEvent e : events) {
                if (expected.expectedSeverity != null && e.severity != expected.expectedSeverity) {
                    continue;
                }
                if (expected.locationMatcher != null && !expected.locationMatcher.matches(e.location)) {
                    continue;
                }
                if (expected.messageMatcher != null && !expected.messageMatcher.matches(e.message)) {
                    continue;
                }
                expectationMatched = true;
                events.remove(e);
                break;
            }
            if (!expectationMatched) {
                unmetExpectations.add(expected);
            }
        }

        for (ExpectedEvent event : unmetExpectations) {
            desc.appendText("Expected error not found: ");
            describeExpectedEvent(desc, event);
            desc.appendText("\n");
        }

        if (requireAll) {
            for (ValidationEvent event : events) {
                desc.appendText("Unexpected error: ");
                describeValidationEvent(desc, event);
                desc.appendText("\n");
            }
        }

        if (requireAll) {
            return unmetExpectations.isEmpty() && events.isEmpty();
        } else {
            return unmetExpectations.isEmpty();
        }
    }

    private void describeExpectedEvent(Description desc, ExpectedEvent e) {
        if (e.expectedSeverity != null) {
            desc.appendText("severity = ").appendValue(e.expectedSeverity).appendText(", ");
        }
        if (e.messageMatcher != null) {
            desc.appendText("message ").appendDescriptionOf(e.messageMatcher).appendText(", ");
        }
        if (e.locationMatcher != null) {
            desc.appendText("location ").appendDescriptionOf(e.locationMatcher).appendText(", ");
        }
    }

    private void describeValidationEvent(Description desc, ValidationEvent event) {
        desc.appendText("severity = ").appendValue(event.severity).appendText(", ");
        desc.appendText("message = ").appendValue(event.message).appendText(", ");
        desc.appendText("location = ").appendValue(event.location);
    }

    private static class ExpectedEvent {
        Severity expectedSeverity;
        Matcher<String> locationMatcher;
        Matcher<String> messageMatcher;

        public ExpectedEvent(Severity expectedSeverity, Matcher<String> locationMatcher, Matcher<String> messageMatcher) {
            super();
            this.expectedSeverity = expectedSeverity;
            this.locationMatcher = locationMatcher;
            this.messageMatcher = messageMatcher;
        }
    }

    /**
     * Add an expected warning to this matcher
     *
     * @param message the expected warning message
     * @return {@code this}
     */
    public ValidationResultMatcher warning(String message) {
        return error(equalTo(message));
    }

    /**
     * Add an expected warning to this matcher
     *
     * @param messageMatcher a matcher which must match the warning message
     * @return {@code this}
     */
    public ValidationResultMatcher warning(Matcher<String> messageMatcher) {
        return event(Severity.WARNING, messageMatcher, null);
    }

    /**
     * Add an expected error to this matcher
     *
     * @param message the expected error message
     * @return {@code this}
     */
    public ValidationResultMatcher error(String message) {
        return error(equalTo(message));
    }

    /**
     * Add an expected error to this matcher
     *
     * @param messageMatcher a matcher which must match the error message
     * @return {@code this}
     */
    public ValidationResultMatcher error(Matcher<String> messageMatcher) {
        return event(Severity.ERROR, messageMatcher, null);
    }

    /**
     * Add an expected event to this matcher
     *
     * @param severity the expected severity, or {@code null} to not assert the severity
     * @param messageMatcher a matcher that must match the event message, or {@code null} to not assert the message
     * @param locationMatcher a matcher that must match the event location, or {@code null} to not assert the location
     * @return {@code this}
     */
    public ValidationResultMatcher event(Severity severity, Matcher<String> messageMatcher, Matcher<String> locationMatcher) {
        expectedEvents.add(new ExpectedEvent(severity, locationMatcher, messageMatcher));
        return this;
    }

    /**
     * Creates a matcher that asserts there are no validation events
     *
     * @return the new matcher
     */
    public static ValidationResultMatcher successful() {
        return new ValidationResultMatcher(Collections.emptyList());
    }

    /**
     * Creates a matcher which will assert that exactly the expected events are contained within the result.
     * <p>
     * If the result contains any unexpected events, the assertion will fail, even if all the expected events are found.
     * <p>
     * Expected events should be added before using the matcher.
     *
     * @return the new matcher
     */
    public static ValidationResultMatcher failedWithEvents() {
        return new ValidationResultMatcher(true);
    }

    /**
     * Creates a matcher which will assert that at least the expected events are contained within the result.
     * <p>
     * If the result contains any unexpected events, the assertion can still succeed as long as all the expected events are found.
     * <p>
     * Expected events should be added before using the matcher.
     *
     * @return the new matcher
     */
    public static ValidationResultMatcher failedIncludingEvents() {
        return new ValidationResultMatcher(false);
    }

    /**
     * Creates a matcher that asserts that the result contains one error with the given message
     *
     * @param message the expected error message
     * @return the new matcher
     */
    public static ValidationResultMatcher hasError(String message) {
        return hasError(equalTo(message));
    }

    /**
     * Creates a matcher that asserts that the result contains one error which is accepted by the given matcher
     *
     * @param messageMatcher the matcher for the expected error message
     * @return the new matcher
     */
    public static ValidationResultMatcher hasError(Matcher<String> messageMatcher) {
        return failedWithEvents().error(messageMatcher);
    }

}
