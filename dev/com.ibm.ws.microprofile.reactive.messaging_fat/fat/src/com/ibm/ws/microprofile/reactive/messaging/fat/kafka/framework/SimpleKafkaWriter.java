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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

/**
 * A simple interface for writing String messages to a kafka topic.
 * <p>
 * This writer is very basic, it only writes String messages.
 */
public class SimpleKafkaWriter<T> implements AutoCloseable {

    private final KafkaProducer<String, T> kafkaProducer;
    private final String topic;
    private static final Duration DEFAULT_SEND_TIMEOUT = Duration.ofSeconds(10);

    /**
     * @param kafkaProducer the configured KafkaProducer to use
     * @param topic         the topic name to write to
     */
    public SimpleKafkaWriter(KafkaProducer<String, T> kafkaProducer, String topic) {
        super();
        this.kafkaProducer = kafkaProducer;
        this.topic = topic;
    }

    public RecordMetadata sendMessage(T message) {
        return sendMessage(message, DEFAULT_SEND_TIMEOUT);
    }

    public RecordMetadata sendMessage(T message, Duration timeout) {
        try {
            ProducerRecord<String, T> record = new ProducerRecord<>(topic, message);
            Future<RecordMetadata> ack = kafkaProducer.send(record);
            return ack.get(timeout.toMillis(), MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Error sending Kafka message", e);
        }
    }

    public RecordMetadata sendMessage(T message, int partition, Duration timeout) {
        try {
            ProducerRecord<String, T> record = new ProducerRecord<>(topic, partition, null, message);
            Future<RecordMetadata> ack = kafkaProducer.send(record);
            return ack.get(timeout.toMillis(), MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Error sending Kafka message", e);
        }
    }

    @Override
    public void close() throws Exception {
        kafkaProducer.close();
    }

}
