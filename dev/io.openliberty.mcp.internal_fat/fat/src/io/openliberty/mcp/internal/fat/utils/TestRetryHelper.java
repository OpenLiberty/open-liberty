/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for retrying test actions that are sensitive to timing, JIT warmup,
 * or temporary CI delays.
 */
public class TestRetryHelper {

    private static final Logger LOG = Logger.getLogger(TestRetryHelper.class.getName());
    public static final int DEFAULT_MAX_ATTEMPTS = 3;

    @FunctionalInterface
    public interface RetryableAction {
        void run() throws Throwable;
    }

    @FunctionalInterface
    public interface ResetAction {
        void run() throws Throwable;
    }

    /**
     * Executes the given action, retrying up to {@value #DEFAULT_MAX_ATTEMPTS} times
     * if an AssertionError or Exception is thrown.
     *
     * @param action the test action to execute
     * @throws Exception if all retry attempts fail
     */
    public static void retry(RetryableAction action) throws Exception {
        retry(DEFAULT_MAX_ATTEMPTS, action, null);
    }

    /**
     * Executes the given action, retrying up to maxAttempts times if an AssertionError
     * or Exception is thrown. Invokes resetState before each retry attempt.
     *
     * @param maxAttempts the maximum number of attempts
     * @param action the test action to execute
     * @param resetState an optional runnable to reset state before retrying (may be null)
     * @throws Exception if all retry attempts fail
     */
    public static void retry(int maxAttempts, RetryableAction action, ResetAction resetState) throws Exception {
        Throwable lastThrowable = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (attempt > 1 && resetState != null) {
                    try {
                        resetState.run();
                    } catch (Throwable resetFailure) {
                        // If we can't reset state, don't retry
                        if (lastThrowable != null) {
                            resetFailure.addSuppressed(lastThrowable);
                        }
                        if (resetFailure instanceof Exception e) {
                            throw e;
                        } else if (resetFailure instanceof Error err) {
                            throw err;
                        } else {
                            throw new RuntimeException("Failed to reset state before retry attempt " + attempt, resetFailure);
                        }
                    }
                }
                action.run();
                return; // Succeeded
            } catch (Throwable t) {
                lastThrowable = t;
                LOG.log(Level.WARNING, "Attempt " + attempt + " of " + maxAttempts + " failed: " + t.getMessage(), t);
                if (attempt == maxAttempts) {
                    break;
                }
            }
        }

        if (lastThrowable instanceof Exception e) {
            throw e;
        } else if (lastThrowable instanceof Error err) {
            throw err;
        } else {
            throw new RuntimeException("Test failed after " + maxAttempts + " attempts", lastThrowable);
        }
    }
}
