/*******************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The initial set of unit tests below were heavily derived from
 *   https://github.com/eclipse/microprofile-reactive/blob/master/streams/tck/src/main/java/org/eclipse/microprofile/reactive/streams/tck/spi/CollectStageVerification.java
 * by James Roper.
 ******************************************************************************/

package com.ibm.ws.microprofile.reactive.streams.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.eclipse.microprofile.reactive.streams.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.spi.ReactiveStreamsEngine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.microprofile.reactive.streams.WASReactiveStreamsEngineImpl;

public class CollectTest {

    @Before
    public void before() {
        //Engine Start?
    }

    @After
    public void after() {
        //Engine Stop?
    }

    @Test
    public void toListStageShouldReturnAList() {
        assertEquals(await(ReactiveStreams.of(1, 2, 3).toList().run(getEngine())), Arrays.asList(1, 2, 3));
    }

    @Test
    public void toListStageShouldReturnEmpty() {
        assertEquals(await(ReactiveStreams.of().toList().run(getEngine())), Collections.emptyList());
    }

    @Test
    public void collectShouldAccumulateResult() {
        assertEquals(await(ReactiveStreams.of(1, 2, 3).collect(
                                                               () -> new AtomicInteger(0),
                                                               AtomicInteger::addAndGet).run(getEngine())).get(),
                     6);
    }

    @Test
    public void collectShouldSupportEmptyStreams() {
        assertEquals(await(ReactiveStreams.<Integer> empty().collect(
                                                                     () -> new AtomicInteger(42),
                                                                     AtomicInteger::addAndGet).run(getEngine())).get(),
                     42);
    }

    @Test(expected = RuntimeException.class)
    public void collectShouldPropagateErrors() {
        await(ReactiveStreams.<Integer> failed(new RuntimeException("failed")).collect(
                                                                                       () -> new AtomicInteger(0),
                                                                                       AtomicInteger::addAndGet).run(getEngine()));
    }

    @Test
    public void finisherFunctionShouldBeInvoked() {
        assertEquals(await(ReactiveStreams.of("1", "2", "3").collect(Collectors.joining(", ")).run(getEngine())), "1, 2, 3");
    }

    @Test(expected = RuntimeException.class)
    public void toListStageShouldPropagateErrors() {
        await(ReactiveStreams.failed(new RuntimeException("failed")).toList().run(getEngine()));
    }

    /**
     * @return
     */
    private ReactiveStreamsEngine getEngine() {
        return WASReactiveStreamsEngineImpl.getEngine();
    }

    <T> T await(CompletionStage<T> future) {
        try {
            return future.toCompletableFuture().get(getTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new RuntimeException(e.getCause());
            }
        } catch (TimeoutException e) {
            throw new RuntimeException("Future timed out after " + getTimeout() + "ms", e);
        }
    }

    /**
     * @return
     */
    private long getTimeout() {
        return 1000;
    }
}
