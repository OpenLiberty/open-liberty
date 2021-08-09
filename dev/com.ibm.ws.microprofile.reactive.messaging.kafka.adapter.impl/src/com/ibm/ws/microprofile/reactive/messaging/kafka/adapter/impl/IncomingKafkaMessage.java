/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import org.eclipse.microprofile.reactive.messaging.Message;

import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.ConsumerRecord;

/**
 *
 */
public class IncomingKafkaMessage<T> implements Message<T> {

    private final ConsumerRecord<?, T> consumerRecord;
    private final Supplier<CompletionStage<Void>> ack;

    public IncomingKafkaMessage(ConsumerRecord<?, T> consumerRecord, Supplier<CompletionStage<Void>> ack) {
        this.consumerRecord = consumerRecord;
        this.ack = ack;
    }

    /** {@inheritDoc} */
    @Override
    public T getPayload() {
        return this.consumerRecord.value();
    }

    /** {@inheritDoc} */
    @Override
    public CompletionStage<Void> ack() {
        if (this.ack != null) {
            return this.ack.get();
        } else {
            return CompletableFuture.completedFuture(null);
        }
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
