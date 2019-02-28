/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The initial set of unit test material was heavily derived from
 * tests at https://github.com/eclipse/microprofile-reactive
 * by James Roper.
 ******************************************************************************/

package com.ibm.ws.microprofile.reactive.streams.test;

import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.Test;

import io.reactivex.subscribers.TestSubscriber;

/**
 * A class that unit tests errors, recovery handling and fallback
 */
public class ErrorRecoveryTest extends WASReactiveUT {

    /**
     * A user's Processor throws an exception
     *
     * @throws InterruptedException
     */
    @Test
    public void testError() throws InterruptedException {

        PublisherBuilder<Integer> data = ReactiveStreams.of(1, 2, 3, 4, 5);
        ProcessorBuilder<Integer, Integer> errorInjector = ReactiveStreams.<Integer>builder().

                map(element -> {
                    if (element == 3) {
                        throw new QuietRuntimeException("Processor exception");
                    }
                    return element;
                });

        TestSubscriber<Integer> rxSub = new TestSubscriber<Integer>();
        data.via(errorInjector).to(rxSub).run(getEngine());
        rxSub.await()
                .assertNotComplete()
                .assertError(QuietRuntimeException.class)
                .assertValueAt(0, 1)
                .assertValueAt(1, 2);
    }

    /**
     * Tests catching exceptions with onErrorResume operator
     *
     * @throws InterruptedException
     */
    @Test
    public void testErrorRecovery() throws InterruptedException {

        PublisherBuilder<String> data = ReactiveStreams.of("tick", "tick", "boom", "tick");
        ProcessorBuilder<String, String> errorInjector = ReactiveStreams.<String>builder().

                map(element -> {
                    if (element.equals("boom")) {
                        throw new QuietRuntimeException("BOOM!");
                    }
                    return element;
                })

                .onErrorResume(err -> {
                    return "defused";
                })

        ;

        TestSubscriber<String> rxSub = new TestSubscriber<String>();
        data.via(errorInjector).to(rxSub).run(getEngine());
        TestSubscriber<String> o = rxSub.await();
        o.assertComplete();
        o.assertNoErrors();
        o.assertResult("tick", "tick", "defused");

    }

    /**
     * Tests catching exceptions with onErrorResume operator plus splicing on a
     * continuation of the stream with concat
     *
     * @throws InterruptedException
     */
    @Test
    public void testConcatFallback() throws InterruptedException {

        PublisherBuilder<String> data1 = ReactiveStreams.of("tick", "tick", "boom", "tick");
        PublisherBuilder<String> fallback = ReactiveStreams.of("TICK");

        ProcessorBuilder<String, String> errorInjector = ReactiveStreams.<String>builder().

                map(element -> {
                    if (element.equals("boom")) {
                        throw new QuietRuntimeException("BOOM!");
                    }
                    return element;
                })

                .onErrorResume(err -> {
                    return "defused";
                })

        ;

        TestSubscriber<String> rxSub = new TestSubscriber<String>();

        ReactiveStreams.concat(data1.via(errorInjector), fallback).to(rxSub).run(getEngine());
        TestSubscriber<String> o = rxSub.await();
        o.assertComplete();
        o.assertNoErrors();
        o.assertResult("tick", "tick", "defused", "TICK");

    }

}
