/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.tck;

import java.util.concurrent.CompletableFuture;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;

import com.ibm.ws.microprofile.reactive.messaging.kafka.KafkaInput;
import com.ibm.ws.microprofile.reactive.messaging.kafka.PartitionTrackerFactory;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaAdapterFactory;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaConsumer;

import io.openliberty.microprofile.reactive.messaging.internal.interfaces.RMAsyncProvider;

public class TestKafkaInput<K, V> extends KafkaInput<K, V> {

    public TestKafkaInput(KafkaAdapterFactory kafkaAdapterFactory, PartitionTrackerFactory partitionTrackerFactory, KafkaConsumer<K, V> kafkaConsumer,
                          RMAsyncProvider asyncProvider, String topic, int unackedLimit, boolean fastAck) {
        super(kafkaAdapterFactory, partitionTrackerFactory, kafkaConsumer, asyncProvider, topic, unackedLimit, fastAck);
    }

    @Override
    protected void countDownAppStartedLatch() {
        // Nothing to do
    }

    @Override
    protected boolean isAppStarted() {
        return true;
    }

    @Override
    protected boolean waitForAppStart(CompletableFuture<PublisherBuilder<Message<V>>> result) {
        return true;
    }

}
