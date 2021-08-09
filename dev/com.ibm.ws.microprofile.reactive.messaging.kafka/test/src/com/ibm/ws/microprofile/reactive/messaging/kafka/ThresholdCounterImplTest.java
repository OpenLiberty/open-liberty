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

import static com.ibm.ws.microprofile.reactive.messaging.kafka.CompletionStageMatcher.incomplete;
import static com.ibm.ws.microprofile.reactive.messaging.kafka.CompletionStageMatcher.succeeded;
import static com.ibm.ws.microprofile.reactive.messaging.kafka.CompletionStageMatcher.succeededWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public class ThresholdCounterImplTest {

    @Test
    public void testReadyWhenBelowThreshold() {
        ThresholdCounterImpl counter = new ThresholdCounterImpl(1);
        assertThat(counter.waitForBelowThreshold(), succeededWith(nullValue()));
    }

    @Test
    public void testNotReadyAboveThreshold() {
        ThresholdCounterImpl counter = new ThresholdCounterImpl(1);
        counter.increment();
        assertThat(counter.waitForBelowThreshold(), incomplete());
    }

    @Test
    public void testBecomesReadyWhenDroppingBelowThreshold() {
        ThresholdCounterImpl counter = new ThresholdCounterImpl(1);
        counter.increment();

        AtomicBoolean counterReady = new AtomicBoolean(false);
        counter.waitForBelowThreshold().thenRun(() -> counterReady.set(true));

        assertFalse("Counter ready before decremented", counterReady.get());
        assertThat(counter.waitForBelowThreshold(), incomplete());

        counter.decrement();
        assertTrue("Counter not ready after decremented", counterReady.get());
        assertThat(counter.waitForBelowThreshold(), succeeded());
    }

    @Test
    public void testCrossingThresholdMultipleTimes() {
        ThresholdCounterImpl counter = new ThresholdCounterImpl(3);
        counter.increment(); // 1
        counter.increment(); // 2
        counter.increment(); // 3
        counter.increment(); // 4

        CompletionStage<Void> cs1 = counter.waitForBelowThreshold();
        assertThat(cs1, incomplete());
        assertThat(counter.waitForBelowThreshold(), incomplete());

        counter.decrement(); // 3
        assertThat(cs1, incomplete());
        assertThat(counter.waitForBelowThreshold(), incomplete());

        counter.decrement(); // 2
        assertThat(cs1, succeeded());
        assertThat(counter.waitForBelowThreshold(), succeeded());

        counter.increment(); // 3
        CompletionStage<Void> cs2 = counter.waitForBelowThreshold();
        assertThat(cs2, incomplete());
        assertThat(counter.waitForBelowThreshold(), incomplete());
        assertThat(cs1, succeeded()); // A completed stage does not become incomplete again

        counter.decrement(); // 2
        assertThat(cs2, succeeded());
        assertThat(counter.waitForBelowThreshold(), succeeded());
    }

}
