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

import static com.ibm.ws.microprofile.faulttolerance20.state.impl.CircuitBreakerStateImpl.CircuitBreakerResult.FAILURE;

import java.util.BitSet;

/**
 * Implements the rolling window for a Fault Tolerance Circuit Breaker
 * <p>
 * This class is not thread safe, the caller must ensure all access is synchronized.
 * <p>
 * The rolling window has a set size but starts empty. As results are recorded, it fills up. Once it is full, recording a new result will result in the oldest result being removed
 * from the window.
 */
public class CircuitBreakerRollingWindow {

    /**
     * The size of the rolling window
     */
    private final int size;

    /**
     * The failure threshold of the rolling window
     * <p>
     * If this number of failures are in a full rolling window, it's over the threshold
     */
    private final int threshold;

    /**
     * Circular buffer of results in the rolling window
     * <p>
     * A set bit indicates a failure, an unset bit represents a success.
     * <p>
     * If {@code resultCount < size}, then only the first {@code resultCount} bits are meaningful.
     */
    private final BitSet results;

    /**
     * Index indicating where in {@link #results} the next result should be stored
     * <p>
     * If the window is full (i.e. {@code resultCount == size}), this also points to the oldest result in the window
     */
    private int nextResultIndex;

    /**
     * Running total of the number of failures in the rolling window
     */
    private int failures;

    /**
     * Count of the number of results in the rolling window
     * <p>
     * When the window is first created or is cleared, this is zero because the window is empty.
     * <p>
     * If {@code resultCount == size} then the window is full and recording a new result will result in the oldest result being pushed out of the window.
     */
    private int resultCount;

    /**
     * @param size         the size of the rolling window
     * @param failureRatio the ratio of failures required to be over the threshold
     */
    public CircuitBreakerRollingWindow(int size, double failureRatio) {
        this.size = size;
        this.threshold = (int) Math.ceil(size * failureRatio);
        results = new BitSet(size);
        nextResultIndex = 0;
        failures = 0;
        resultCount = 0;
    }

    /**
     * Record a result in the rolling window
     *
     * @param result the result to record
     */
    public void record(CircuitBreakerStateImpl.CircuitBreakerResult result) {
        boolean isFailure = (result == FAILURE);
        if (resultCount < size) {
            // Window is not yet full
            resultCount++;
        } else {
            // Window is full, roll off the oldest result
            boolean oldestResultIsFailure = results.get(nextResultIndex);
            if (oldestResultIsFailure) {
                failures--;
            }
        }

        results.set(nextResultIndex, isFailure);
        if (isFailure) {
            failures++;
        }

        nextResultIndex++;
        if (nextResultIndex >= size) {
            nextResultIndex = 0;
        }
    }

    /**
     * Whether the number of failures is over the configured threshold
     * <p>
     * The rolling window is only over the threshold if
     * <ul>
     * <li>the window has been filled</li>
     * <li>the proportion of failures in the rolling window is greater than the {@code failureRatio} passed in the constructor</li>
     * </ul>
     *
     * @return {@code true} if the number failures in the rolling window is over the configured threshold
     */
    public boolean isOverThreshold() {
        return (size == resultCount) && (failures >= threshold);
    }

    /**
     * Clear all the results in the rolling window
     * <p>
     * After calling this, {@link #isOverThreshold()} will not return {@code false} until enough new results have come in to fill the window.
     */
    public void clear() {
        results.clear();
        nextResultIndex = 0;
        failures = 0;
        resultCount = 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append("[Rolling window:");
        sb.append(" current failures: (").append(failures).append('/').append(resultCount).append(')');
        sb.append(" limit (").append(threshold).append('/').append(size).append(')');
        sb.append(']');
        return sb.toString();
    }
}