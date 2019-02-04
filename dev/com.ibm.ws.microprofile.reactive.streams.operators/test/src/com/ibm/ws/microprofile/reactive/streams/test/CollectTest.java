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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.eclipse.microprofile.reactive.streams.operators.CompletionRunner;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;
import org.junit.Test;

public class CollectTest extends WASReactiveUT {

    @Test
    public void toListStageShouldReturnAList() {

        // This test is broken down so it can be used
        // to investigate which stage is causing problems
        // when there is a basic plumbing issue
        List<Integer> list = Arrays.asList(1, 2, 3);
        assertNotNull("list is null", list);

        ReactiveStreamsEngine engine = getEngine();
        assertNotNull("Engine is null", engine);

        PublisherBuilder<Integer> streamOf = ReactiveStreams.of(1, 2, 3);
        assertNotNull("streamOf is null", streamOf);

        CompletionRunner<List<Integer>> listOfStream = streamOf.toList();
        assertNotNull("listOfStream is null", listOfStream);

        CompletionStage<List<Integer>> composed = listOfStream.run(engine);
        assertNotNull("composed is null", composed);

        List<Integer> awaitedComposed = await(
                composed);
        assertNotNull("awaitedComposed is null", awaitedComposed);

        assertEquals(awaitedComposed,
                list);
    }

    @Test
    public void toListStageShouldReturnEmpty() {
        assertEquals(await(
                ReactiveStreams.of().toList().run(getEngine())), Collections.emptyList());
    }

    @Test
    public void collectShouldAccumulateResult() {
        assertEquals(await(
                ReactiveStreams.of(1, 2, 3).collect(() -> new AtomicInteger(0),
                        AtomicInteger::addAndGet).run(getEngine())).get(),
                6);
    }

    @Test
    public void collectShouldSupportEmptyStreams() {
        assertEquals(await(
                ReactiveStreams.<Integer>empty().collect(() -> new AtomicInteger(42),
                        AtomicInteger::addAndGet).run(getEngine())).get(),
                42);
    }

    @Test(expected = RuntimeException.class)
    public void collectShouldPropagateErrors() {
        await(ReactiveStreams.<Integer>failed(new RuntimeException("failed")).collect(() -> new AtomicInteger(0),
                AtomicInteger::addAndGet).run(getEngine()));
    }

    @Test
    public void finisherFunctionShouldBeInvoked() {
        assertEquals(await(
                ReactiveStreams.of("1", "2", "3")
                        .collect(Collectors.joining(", ")).run(getEngine())),
                "1, 2, 3");
    }

    @Test(expected = RuntimeException.class)
    public void toListStageShouldPropagateErrors() {
        await(ReactiveStreams.failed(new RuntimeException("failed")).toList().run(getEngine()));
    }

    @Test(expected = QuietRuntimeException.class)
    public void collectShouldPropagateUpstreamErrors2() {
        await(ReactiveStreams.<Integer>failed(new QuietRuntimeException("failed")).collect(() -> new AtomicInteger(0),
                AtomicInteger::addAndGet).run(getEngine()));
    }

    @Test(expected = QuietRuntimeException.class)
    public void toListStageShouldPropagateUpstreamErrors2() {
        await(ReactiveStreams.failed(new QuietRuntimeException("failed")).toList().run(getEngine()));
    }

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

    @Test(expected = QuietRuntimeException.class)
    public void collectStageShouldPropagateErrorsFromFinisher() {
        CompletionStage<Integer> result = ReactiveStreams.of(1, 2, 3)
                .collect(Collector.<Integer, Integer, Integer>of(() -> 0, (a, b) -> {
                }, (a, b) -> a + b, r -> {
                    throw new QuietRuntimeException("failed");
                })).run(getEngine());
        await(result);
    }

    @Test
    public void collectStageBuilderShouldBeReusable() {
        SubscriberBuilder<Integer, List<Integer>> toList = ReactiveStreams.<Integer>builder().toList();
        assertEquals(await(
                ReactiveStreams.of(1, 2, 3).to(toList).run(getEngine())),
                Arrays.asList(1, 2,
                        3));
        assertEquals(await(ReactiveStreams.of(4, 5,
                6).to(toList).run(getEngine())), Arrays.asList(4, 5, 6));
    }

}
