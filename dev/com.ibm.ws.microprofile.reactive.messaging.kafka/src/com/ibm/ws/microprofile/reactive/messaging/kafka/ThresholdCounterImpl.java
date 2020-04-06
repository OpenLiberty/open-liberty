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
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public class ThresholdCounterImpl implements ThresholdCounter {

    private static final TraceComponent tc = Tr.register(ThresholdCounterImpl.class);

    private final int threshold;
    private final AtomicInteger counter;

    private CompletableFuture<Void> counterThresholdStage;

    /**
     * @param threshold the counter threshold value
     */
    public ThresholdCounterImpl(int threshold) {
        this.threshold = threshold;
        this.counter = new AtomicInteger(0);
        counterThresholdStage = CompletableFuture.completedFuture(null);
    }

    /** {@inheritDoc} */
    @Override
    public void increment() {
        synchronized (this.counter) {
            int count = this.counter.incrementAndGet();
            if (count == this.threshold) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Too many outstanding unacked messages, stopping polling kafka. Current count: " + count);
                }

                this.counterThresholdStage = new CompletableFuture<>();
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public void decrement() {
        CompletableFuture<Void> thresholdStage;
        int count;
        synchronized (this.counter) {
            count = this.counter.getAndDecrement();
            thresholdStage = this.counterThresholdStage;
        }
        if (count == this.threshold) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Outstanding unacked message count dropped below threshold, resuming polling kafka. Current count: " + count);
            }
            thresholdStage.complete(null);
        }

    }

    /** {@inheritDoc} */
    @Override
    public CompletionStage<Void> waitForBelowThreshold() {
        return this.counterThresholdStage;
    }

}
