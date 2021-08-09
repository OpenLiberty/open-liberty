/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;

/**
 * An interface for reading ConsumerRecords from a Kafka topic. The key and message types are dependent on the KafkaConsumer which is passed in.
 * <p>
 * This reader doesn't commit any offsets so each new reader will start reading from the start of the topic.
 */
public class KafkaReader<K, V> implements AutoCloseable {

    private final KafkaConsumer<K, V> kafkaConsumer;

    /**
     * @param kafkaConsumer
     * @param topic
     */
    public KafkaReader(KafkaConsumer<K, V> kafkaConsumer, String topic) {
        super();
        this.kafkaConsumer = kafkaConsumer;
        kafkaConsumer.subscribe(Collections.singleton(topic));
    }

    /**
     * Poll Kafka until the desired number of messages is received
     * <p>
     * Will throw an AssertionError if expected number of messages is not available on the topic
     *
     * @param count   the number of records expected
     * @param timeout the amount of time to wait for the expected number of records to be received
     * @return the list of messages received
     */
    public List<V> assertReadMessages(int count, Duration timeout) {
        List<ConsumerRecord<K, V>> records = assertReadRecords(count, timeout);
        List<V> result = records.stream().map((r) -> r.value()).collect(Collectors.toList());
        return result;
    }

    /**
     * Poll Kafka until the desired number of records is received
     * <p>
     * Will throw an AssertionError if expected number of records is not available on the topic
     *
     * @param count   the number of records expected
     * @param timeout the amount of time to wait for the expected number of records to be received
     * @return the list of records received
     */
    public List<ConsumerRecord<K, V>> assertReadRecords(int count, Duration timeout) {
        List<ConsumerRecord<K, V>> records = readRecords(count, timeout);
        assertThat("Wrong number of records fetched from kafka", records, hasSize(count));
        return records;
    }

    /**
     * Poll Kafka until either the maximum number of records is received or the timeout is reached
     *
     * @param maxRecords the maximum number of records to read
     * @param timeout    the amount of time to wait for records to be received
     * @return the list of records received
     */
    public List<ConsumerRecord<K, V>> readRecords(int maxRecords, Duration timeout) {
        ArrayList<ConsumerRecord<K, V>> result = new ArrayList<>();
        Duration remaining = timeout;
        long startTime = System.nanoTime();
        while (!remaining.isNegative() && result.size() < maxRecords) {
            for (ConsumerRecord<K, V> record : kafkaConsumer.poll(remaining)) {
                result.add(record);
            }
            Duration elapsed = Duration.ofNanos(System.nanoTime() - startTime);
            remaining = timeout.minus(elapsed);
        }
        return result;
    }

    /**
     * Get the underlying Kafka Consumer
     *
     * @return the consumer
     */
    public KafkaConsumer<K, V> getConsumer() {
        return kafkaConsumer;
    }

    @Override
    public void close() throws Exception {
        kafkaConsumer.close();
    }

}
