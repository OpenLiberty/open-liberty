/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.kafka;

import java.time.Duration;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaProducer;

public class KafkaOutput<K, V> {

    private static final TraceComponent tc = Tr.register(KafkaOutput.class);
    private final KafkaProducer<K, V> kafkaProducer;
    private final String topic;
    private volatile boolean running = true;

    public KafkaOutput(String topic, KafkaProducer<K, V> kafkaProducer) {
        this.topic = topic;
        this.kafkaProducer = kafkaProducer;
    }

    public SubscriberBuilder<Message<V>, Void> getSubscriber() {
        return ReactiveStreams.<Message<V>> builder().takeWhile(m -> running).onError(this::logError).forEach(this::sendMessage);
    }

    public void shutdown(Duration timeout) {
        running = false;
        kafkaProducer.close(timeout);
    }

    private void logError(Throwable t) {
        Tr.error(tc, "internal.kafka.connector.error.CWMRX1000E", t);
    }

    private void sendMessage(Message<V> message) {
        try {
            this.kafkaProducer.send(this.topic, message.getPayload(), (r, e) -> {
                if (e == null) {
                    message.ack();
                } else {
                    reportSendException(e);
                }
            });
        } catch (Exception e) {
            reportSendException(e);
            throw e;
        }
    }

    private static void reportSendException(Throwable t) {
        Tr.error(tc, "kafka.send.error.CWMRX1003E", t);
    }

}
