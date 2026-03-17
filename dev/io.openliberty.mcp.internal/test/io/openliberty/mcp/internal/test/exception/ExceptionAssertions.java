/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test.exception;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

/**
 * Methods and matcher for asserting that some code throws a particular exception
 * <p>
 * Example
 *
 * <pre>{@code
 * assertThrows(() -> methodThatShouldThrowException(),
 *              exception().ofType(MyException.class)
 *                         .messageIncludes("The error was foo"));
 * }</pre>
 */
public class ExceptionAssertions {

    /**
     * Assert that {@code code} throws an exception accepted by {@code matcher}.
     * <p>
     * In most cases {@link #exception()} will be used to create the matcher.
     *
     * @param code the code to run which should throw the exception
     * @param matcher the matcher to use to evaluate the exception thrown
     */
    public static void assertThrows(ThrowingRunnable code, Matcher<? super Throwable> matcher) {
        try {
            code.run();
            fail("No exception thrown");
        } catch (Throwable t) {
            System.out.println("Exception thrown: ");
            t.printStackTrace();
            assertThat(t, matcher);
        }
    }

    @FunctionalInterface
    public static interface ThrowingRunnable {
        public void run() throws Throwable;
    }

    /**
     * Create a configurable matcher for asserting exception objects
     *
     * @return an exception matcher
     */
    public static ExceptionMatcher exception() {
        return new ExceptionMatcher();
    }

    /**
     * A matcher for exceptions. Obtain with {@link ExceptionAssertions#exception()}
     */
    public static class ExceptionMatcher extends TypeSafeDiagnosingMatcher<Throwable> {

        protected Class<? extends Throwable> type;
        protected final List<String> messageIncludes = new ArrayList<>();
        protected Matcher<Throwable> causeMatcher;

        private ExceptionMatcher() {
            super(Throwable.class);
        }

        /**
         * Assert the exception is a subclass of the given type
         *
         * @param type the expected type
         * @return {@code this}
         */
        public ExceptionMatcher ofType(Class<? extends Throwable> type) {
            this.type = type;
            return this;
        }

        /**
         * Assert the exception message includes a string
         *
         * @param message the string to check for
         * @return {@code this}
         */
        public ExceptionMatcher messageIncludes(String message) {
            this.messageIncludes.add(message);
            return this;
        }

        /**
         * Assert something about the cause of the exception
         *
         * @param causeMatcher a matcher to test against the cause of the exception
         * @return {@code this}
         */
        public ExceptionMatcher withCause(Matcher<Throwable> causeMatcher) {
            this.causeMatcher = causeMatcher;
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public void describeTo(Description desc) {
            desc.appendText("An exception");
            if (type != null) {
                desc.appendText(" with type ").appendValue(type);
            }
            if (!messageIncludes.isEmpty()) {
                desc.appendText(" with message including ").appendValueList("", ", ", "", messageIncludes);
            }
            if (causeMatcher != null) {
                desc.appendText(" with cause ").appendDescriptionOf(causeMatcher);
            }
        }

        /** {@inheritDoc} */
        @Override
        protected boolean matchesSafely(Throwable t, Description desc) {
            boolean result = true;
            if (type != null) {
                if (!type.isInstance(t)) {
                    result = false;
                    desc.appendText("type was ").appendValue(t.getClass()).appendText("\n");
                }
            }

            for (String messagePart : messageIncludes) {
                if (!t.getMessage().contains(messagePart)) {
                    result = false;
                    desc.appendText("message was").appendValue(t.getMessage()).appendText("\n");
                    break;
                }
            }

            if (causeMatcher != null) {
                if (!causeMatcher.matches(t.getCause())) {
                    result = false;
                    desc.appendText("cause was ");
                    causeMatcher.describeMismatch(t.getCause(), desc);
                    desc.appendText("\n");
                }
            }
            return result;
        }
    }
}
