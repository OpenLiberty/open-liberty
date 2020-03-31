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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A counter which can return a CompletionStage that completes when the counter value drops below a threshold value
 */
public interface ThresholdCounter {

    /**
     * Increment the counter
     */
    void increment();

    /**
     * Decrement the counter
     */
    void decrement();

    /**
     * Returns a completion stage which completes when the counter is less than the threshold value
     * <p>
     * If the counter is already below the threshold value then a completed completion stage is returned
     *
     * @return completion stage which completes when the counter is less than the threshold value
     */
    CompletionStage<Void> waitForBelowThreshold();

    ThresholdCounter UNLIMITED = new ThresholdCounter() {

        @Override
        public CompletionStage<Void> waitForBelowThreshold() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void increment() {
            // Do nothing
        }

        @Override
        public void decrement() {
            // Do nothing
        }
    };

}