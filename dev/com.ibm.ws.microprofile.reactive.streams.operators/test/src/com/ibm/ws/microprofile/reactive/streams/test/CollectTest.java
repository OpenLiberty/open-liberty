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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.eclipse.microprofile.reactive.streams.operators.CompletionRunner;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;
import org.junit.Test;

/**
 * This is the ground-zero test set that is run most commonly during development
 * to be able to look at a Stream all at once, you need to collect it.
 */
public class CollectTest extends WASReactiveUT {

    /**
     * This simple test is broken down into small stages so that we can tell which
     * part is broken
     */
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

    /**
     * Simple, no member, stream testing
     */
    @Test
    public void toListStageShouldReturnEmpty() {
        assertEquals(await(
                ReactiveStreams.of().toList().run(getEngine())), Collections.emptyList());
    }

    /**
     * Are all elements sent down a simple stream
     */
    @Test
    public void collectShouldAccumulateResult() {
        assertEquals(await(
                ReactiveStreams.of(1, 2, 3).collect(() -> new AtomicInteger(0),
                        AtomicInteger::addAndGet).run(getEngine())).get(),
                6);
    }

    /**
     * Can we collect a stream with no elements
     */
    @Test
    public void collectShouldSupportEmptyStreams() {
        assertEquals(await(
                ReactiveStreams.<Integer>empty().collect(() -> new AtomicInteger(42),
                        AtomicInteger::addAndGet).run(getEngine())).get(),
                42);
    }

    /**
     * Do failed streams terminate
     */
    @Test(expected = RuntimeException.class)
    public void collectShouldPropagateErrors() {
        await(ReactiveStreams.<Integer>failed(new RuntimeException("failed")).collect(() -> new AtomicInteger(0),
                AtomicInteger::addAndGet).run(getEngine()));
    }

    /**
     * Can we collect but, inside that, operate on each element
     */
    @Test
    public void finisherFunctionShouldBeInvoked() {
        assertEquals(await(
                ReactiveStreams.of("1", "2", "3")
                        .collect(Collectors.joining(", ")).run(getEngine())),
                "1, 2, 3");
    }

    /**
     * A SubscriberBuilder should be reusable
     */
    @Test
    public void collectStageBuilderShouldBeReusable() {
        SubscriberBuilder<Integer, List<Integer>> reusedBuilder = ReactiveStreams.<Integer>builder().toList();
        assertEquals(await(
                ReactiveStreams.of(1, 2, 3).to(reusedBuilder).run(getEngine())),
                Arrays.asList(1, 2,
                        3));
        assertEquals(await(ReactiveStreams.of(4, 5,
                6).to(reusedBuilder).run(getEngine())), Arrays.asList(4, 5, 6));
    }

}
