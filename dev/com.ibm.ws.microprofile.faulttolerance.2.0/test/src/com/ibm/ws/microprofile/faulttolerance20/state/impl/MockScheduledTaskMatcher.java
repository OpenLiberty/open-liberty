/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.state.impl;

import java.time.Duration;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

/**
 * Matcher for MockScheduledTasks to make tests simpler
 */
public class MockScheduledTaskMatcher extends TypeSafeDiagnosingMatcher<MockScheduledTask<?>> {

    private final boolean isCancelled;
    private final Duration expectedDelay;

    /**
     * Match a non-cancelled task scheduled with the expected delay
     */
    public static MockScheduledTaskMatcher taskWithDelay(Duration expectedDelay) {
        return new MockScheduledTaskMatcher(false, expectedDelay);
    }

    /**
     * Match a cancelled task
     */
    public static MockScheduledTaskMatcher cancelledTask() {
        return new MockScheduledTaskMatcher(true, null);
    }

    public MockScheduledTaskMatcher(boolean isCancelled, Duration expectedDelay) {
        super();
        this.isCancelled = isCancelled;
        this.expectedDelay = expectedDelay;
    }

    @Override
    public void describeTo(Description desc) {
        desc.appendText("Task which is ").appendText(isCancelled ? "cancelled" : "not cancelled").appendText(" scheduled for ").appendValue(expectedDelay);
    }

    @Override
    protected boolean matchesSafely(MockScheduledTask<?> task, Description desc) {
        boolean matches = true;

        if (isCancelled != task.getFuture().isCancelled()) {
            desc.appendText("isCancelled: ");
            desc.appendText("expected: ").appendValue(isCancelled);
            desc.appendText(", actual: ").appendValue(task.getFuture().isCancelled());
            desc.appendText("\n");
            matches = false;
        }

        if (expectedDelay != null && !expectedDelay.equals(task.getDelay())) {
            desc.appendText("Delay: ");
            desc.appendText("expected: ").appendValue(expectedDelay);
            desc.appendText(", actual: ").appendValue(task.getDelay());
            desc.appendText("\n");
            matches = false;
        }

        return matches;
    }

}