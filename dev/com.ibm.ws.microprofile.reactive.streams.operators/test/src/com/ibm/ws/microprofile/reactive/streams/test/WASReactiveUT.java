/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.streams.test;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.eclipse.microprofile.reactive.streams.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.spi.ReactiveStreamsEngine;

import com.ibm.ws.microprofile.reactive.streams.spi.impl.WASReactiveStreamsEngineImpl;

/**
 *
 */
public class WASReactiveUT {

    /**
     * Getter for the main class
     *
     * @return the ReactiveStreamEngine implementation
     */
    protected ReactiveStreamsEngine getEngine() {
        return WASReactiveStreamsEngineImpl.getEngine();
    }

    /**
     * Waits for things to complete (with a timeout) and returns the actual result
     *
     * @param future
     * @return the stage's CompletableFuture.get()
     */
    protected <T> T await(CompletionStage<T> future) {
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
     * Just an initial unit test timeout of 10 seconds
     *
     * @return 10000
     */
    private long getTimeout() {
        return 10000;
    }

    /**
     * An infinite stream of integers starting from one.
     */
    protected PublisherBuilder<Integer> infiniteStream() {
        return ReactiveStreams.fromIterable(() -> {
            AtomicInteger value = new AtomicInteger();
            return IntStream.generate(value::incrementAndGet).boxed().iterator();
        });
    }

}
