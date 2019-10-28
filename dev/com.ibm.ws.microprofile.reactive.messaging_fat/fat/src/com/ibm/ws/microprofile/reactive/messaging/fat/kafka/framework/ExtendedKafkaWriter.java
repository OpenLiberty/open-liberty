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
 * An interface for writing messages to a kafka topic. The key and message types are dependent on the KafkaProducer which is passed in.
 */
public class ExtendedKafkaWriter<K, V> implements AutoCloseable {

    private final KafkaProducer<K, V> kafkaProducer;
    private final String topic;
    private static final Duration DEFAULT_SEND_TIMEOUT = Duration.ofSeconds(10);

    /**
     * @param kafkaProducer the configured KafkaProducer to use
     * @param topic         the topic name to write to
     */
    public ExtendedKafkaWriter(KafkaProducer<K, V> kafkaProducer, String topic) {
        super();
        this.kafkaProducer = kafkaProducer;
        this.topic = topic;
    }

    public RecordMetadata sendMessage(K key, V message) {
        return sendMessage(key, message, DEFAULT_SEND_TIMEOUT);
    }

    public RecordMetadata sendMessage(K key, V message, Duration timeout) {
        try {
            ProducerRecord<K, V> record = new ProducerRecord<>(topic, key, message);
            Future<RecordMetadata> ack = kafkaProducer.send(record);
            return ack.get(timeout.toMillis(), MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Error sending Kafka message", e);
        }
    }

    public RecordMetadata sendMessage(K key, V message, int partition, Duration timeout) {
        try {
            ProducerRecord<K, V> record = new ProducerRecord<>(topic, partition, key, message);
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
