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

import static junit.framework.Assert.assertNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collector;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.Test;

/**
 * Unit test exceptional results from running streams
 */
public class ExceptionalCompletionTest extends WASReactiveUT {

    /**
     * Check an exception escapes a stream with no elements
     */
    @Test(expected = RuntimeException.class)
    public void toListStageShouldPropagateErrors() {
        await(ReactiveStreams.failed(new RuntimeException("failed")).toList().run(getEngine()));
    }

    /**
     * Check an exception escapes a collected typed stream
     */
    @Test(expected = QuietRuntimeException.class)
    public void collectShouldPropagateUpstreamErrors2() {
        await(ReactiveStreams.<Integer>failed(new QuietRuntimeException("failed")).collect(() -> new AtomicInteger(0),
                AtomicInteger::addAndGet).run(getEngine()));
    }

    /**
     * Check an exception escapes a toList typed stream
     */
    @Test(expected = QuietRuntimeException.class)
    public void toListStageShouldPropagateUpstreamErrors2() {
        await(ReactiveStreams.failed(new QuietRuntimeException("failed")).toList().run(getEngine()));
    }

    /**
     * Check exceptional termination
     */
    @Test(expected = QuietRuntimeException.class)
    public void collectStageShouldPropagateErrorsFromSupplierThroughCompletionStage() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        CompletionStage<Integer> result = null;
        try {
            result = infiniteStream().onTerminate(() -> cancelled.complete(null))
                    .collect(Collector.<Integer, Integer, Integer>of(() -> {
                        throw new QuietRuntimeException("failed");
                    }, (a, b) -> {
                    }, (a, b) -> a + b,
                            Function.identity()))
                    .run(getEngine());
        } catch (Exception e) {
            assertNull(
                    "Exception thrown directly from stream, it should have been captured by the returned CompletionStage",
                    e);
        }
        await(cancelled);
        await(result);
    }

    /**
     *
     */
    @Test(expected = QuietRuntimeException.class)
    public void collectStageShouldPropagateErrorsFromAccumulator() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        CompletionStage<String> result = infiniteStream().onTerminate(() -> cancelled.complete(null))
                .collect(Collector.of(() -> "", (a, b) -> {
                    throw new QuietRuntimeException("failed");
                }, (a, b) -> a + b,
                        Function.identity()))
                .run(getEngine());
        await(cancelled);
        await(result);
    }

    /**
     *
     */
    @Test(expected = QuietRuntimeException.class)
    public void collectStageShouldPropagateErrorsFromFinisher() {
        CompletionStage<Integer> result = ReactiveStreams.of(1, 2, 3)
                .collect(Collector.<Integer, Integer, Integer>of(() -> 0, (a, b) -> {
                }, (a, b) -> a + b, r -> {
                    throw new QuietRuntimeException("failed");
                })).run(getEngine());
        await(result);
    }

}
