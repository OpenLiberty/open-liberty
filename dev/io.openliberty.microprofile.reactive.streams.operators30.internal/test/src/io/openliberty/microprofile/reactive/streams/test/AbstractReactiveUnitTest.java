/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.streams.test;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.core.ReactiveStreamsEngineResolver;
import org.eclipse.microprofile.reactive.streams.operators.core.ReactiveStreamsFactoryImpl;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsFactoryResolver;
import org.junit.After;
import org.junit.Before;

import io.openliberty.microprofile.reactive.streams.test.utils.TestReactiveStreamsEngine;

public class AbstractReactiveUnitTest {

    private static final long TIMEOUT = 10000; //timeout in ms
    private TestReactiveStreamsEngine engine;

    @Before
    public void activateEngine() {
        engine = new TestReactiveStreamsEngine();
        ReactiveStreamsEngineResolver.setInstance(engine);
        ReactiveStreamsFactoryResolver.setInstance(new ReactiveStreamsFactoryImpl());
    }

    @After
    public void deactivateEngine() {
        if (engine != null) {
            ReactiveStreamsFactoryResolver.setInstance(null);
            ReactiveStreamsEngineResolver.setInstance(null);
            engine = null;
        }
    }

    public TestReactiveStreamsEngine getEngine() {
        return engine;
    }

    /**
     * Waits for things to complete (with a timeout) and returns the actual result
     *
     * @param future
     * @return the stage's CompletableFuture.get()
     */
    protected <T> T await(CompletionStage<T> future) {
        try {
            return future.toCompletableFuture().get(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new RuntimeException(e.getCause());
            }
        } catch (TimeoutException e) {
            throw new RuntimeException("Future timed out after " + TIMEOUT + "ms", e);
        }
    }

    /**
     * An infinite stream of integers starting from one.
     */
    protected PublisherBuilder<Integer> infiniteStream() {
        return ReactiveStreams.iterate(1, i -> i + 1);
    }

}
