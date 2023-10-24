/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.reactive.messaging.Message;

import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.ConsumerRecord;

/**
 *
 */
public class IncomingKafkaMessage<T> implements Message<T> {

    private final ConsumerRecord<?, T> consumerRecord;
    private final Supplier<CompletionStage<Void>> ack;
    private final Function<Throwable, CompletionStage<Void>> nack;

    public IncomingKafkaMessage(ConsumerRecord<?, T> consumerRecord,
                                Supplier<CompletionStage<Void>> ack,
                                Function<Throwable, CompletionStage<Void>> nack) {
        this.consumerRecord = consumerRecord;
        if (ack != null) {
            this.ack = ack;
        } else {
            this.ack = () -> CompletableFuture.completedFuture(null);
        }
        if (nack != null) {
            this.nack = nack;
        } else {
            this.nack = (t) -> CompletableFuture.completedFuture(null);
        }
    }

    /** {@inheritDoc} */
    @Override
    public T getPayload() {
        return this.consumerRecord.value();
    }

    /** {@inheritDoc} */
    @Override
    public CompletionStage<Void> ack() {
        return this.ack.get();
    }

    // RM 3.0 methods:
    public Supplier<CompletionStage<Void>> getAck() {
        return ack;
    }

    public Function<Throwable, CompletionStage<Void>> getNack() {
        return nack;
    }

    @Override
    public <C> C unwrap(Class<C> unwrapType) {
        if (unwrapType == null) {
            throw new IllegalArgumentException("The target class must not be `null`");
        }
        if (org.apache.kafka.clients.consumer.ConsumerRecord.class.equals(unwrapType)) {
            return unwrapType.cast(this.consumerRecord.getDelegate());
        }

        try {
            return unwrapType.cast(this);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Cannot unwrap an instance of " + this.getClass().getName()
                                               + " to " + unwrapType.getName(), e);
        }

    }
}
