/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * The initial set of unit test material was heavily derived from
 * tests at https://github.com/eclipse/microprofile-reactive
 * by James Roper.
 ******************************************************************************/

package io.openliberty.microprofile.reactive.streams.test;

import static junit.framework.Assert.assertNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collector;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.Test;

import io.openliberty.microprofile.reactive.streams.test.utils.TestException;

/**
 * Unit test exceptional results from running streams
 */
public class ExceptionalCompletionTest extends AbstractReactiveUnitTest {

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
    @Test(expected = TestException.class)
    public void collectShouldPropagateUpstreamErrors2() {
        await(ReactiveStreams.<Integer> failed(new TestException("failed")).collect(() -> new AtomicInteger(0),
                                                                                            AtomicInteger::addAndGet)
                        .run(getEngine()));
    }

    /**
     * Check an exception escapes a toList typed stream
     */
    @Test(expected = TestException.class)
    public void toListStageShouldPropagateUpstreamErrors2() {
        await(ReactiveStreams.failed(new TestException("failed")).toList().run(getEngine()));
    }

    /**
     * Check exceptional termination
     */
    @Test(expected = TestException.class)
    public void collectStageShouldPropagateErrorsFromSupplierThroughCompletionStage() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        CompletionStage<Integer> result = null;
        try {
            result = infiniteStream().onTerminate(() -> cancelled.complete(null))
                            .collect(Collector.<Integer, Integer, Integer> of(() -> {
                                throw new TestException("failed");
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
    @Test(expected = TestException.class)
    public void collectStageShouldPropagateErrorsFromAccumulator() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        CompletionStage<String> result = infiniteStream().onTerminate(() -> cancelled.complete(null))
                        .collect(Collector.of(() -> "", (a, b) -> {
                            throw new TestException("failed");
                        }, (a, b) -> a + b,
                                              Function.identity()))
                        .run(getEngine());
        await(cancelled);
        await(result);
    }

    /**
     *
     */
    @Test(expected = TestException.class)
    public void collectStageShouldPropagateErrorsFromFinisher() {
        CompletionStage<Integer> result = ReactiveStreams.of(1, 2, 3)
                        .collect(Collector.<Integer, Integer, Integer> of(() -> 0, (a, b) -> {
                        }, (a, b) -> a + b, r -> {
                            throw new TestException("failed");
                        })).run(getEngine());
        await(result);
    }

}
