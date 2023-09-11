/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.reactive.messaging.Message;

/**
 * A Message implementation compatible with both RM 1.0 and 3.0
 * <p>
 * Allows a nack function to be provided which will be called by the 3.0 implementation
 */
public class TestMessageImpl<T> implements Message<T> {

    private final T payload;
    private final Supplier<CompletionStage<Void>> ackFunction;
    private final Function<Throwable, CompletionStage<Void>> nackFunction;

    private TestMessageImpl(T payload, Supplier<CompletionStage<Void>> ackFunction, Function<Throwable, CompletionStage<Void>> nackFunction) {
        this.payload = payload;
        this.ackFunction = ackFunction;
        this.nackFunction = nackFunction;
    }

    /** {@inheritDoc} */
    @Override
    public T getPayload() {
        return payload;
    }

    /** {@inheritDoc} */
    @Override
    public CompletionStage<Void> ack() {
        return ackFunction.get();
    }

    /**
     * Returns the function to be called when {@code ack} is called
     *
     * @return the ack function
     */
    public Supplier<CompletionStage<Void>> getAck() {
        return ackFunction;
    }

    /**
     * Returns the function to be called when {@code nack} is called
     * <p>
     * Note that {@code nack()} itself does not need to be implemented because the default implementation calls this method
     *
     * @return the nack function
     */
    public Function<Throwable, CompletionStage<Void>> getNack() {
        return nackFunction;
    }

    /**
     * Create a test message with no ack or nack functions
     *
     * @param <T>     payload type
     * @param payload the payload
     * @return the message
     */
    public static <T> TestMessageImpl<T> of(T payload) {
        return new TestMessageImpl<T>(payload, () -> CompletableFuture.completedFuture(null), t -> CompletableFuture.completedFuture(null));
    }

    /**
     * Create a test message with ack and nack functions
     *
     * @param <T>     payload type
     * @param payload the payload
     * @param ack     the function to be called when the message is acked
     * @param nack    the function to be called when the message is nacked
     * @return the message
     */
    public static <T> TestMessageImpl<T> of(T payload, Supplier<CompletionStage<Void>> ack, Function<Throwable, CompletionStage<Void>> nack) {
        return new TestMessageImpl<T>(payload, ack, nack);
    }

}
