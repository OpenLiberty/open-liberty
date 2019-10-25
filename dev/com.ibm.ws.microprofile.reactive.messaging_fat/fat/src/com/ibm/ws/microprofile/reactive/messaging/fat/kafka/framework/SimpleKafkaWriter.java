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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework;

import java.time.Duration;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;

/**
 * A simple interface for writing String messages to a kafka topic.
 * <p>
 * This writer is very basic, it only writes String messages.
 */
public class SimpleKafkaWriter<T> extends ExtendedKafkaWriter<String, T> implements AutoCloseable {

    /**
     * @param kafkaProducer the configured KafkaProducer to use
     * @param topic         the topic name to write to
     */
    public SimpleKafkaWriter(KafkaProducer<String, T> kafkaProducer, String topic) {
        super(kafkaProducer, topic);
    }

    public RecordMetadata sendMessage(T message) {
        return sendMessage(null, message);
    }

    public RecordMetadata sendMessage(T message, Duration timeout) {
        return sendMessage(null, message, timeout);
    }

    public RecordMetadata sendMessage(T message, int partition, Duration timeout) {
        return sendMessage(null, message, partition, timeout);
    }
}
