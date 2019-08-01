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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;

/**
 * A simple interface for reading String messages from a Kafka topic
 * <p>
 * This reader is very basic, it only reads string messages and it doesn't commit any offsets so each new reader will start reading from the start of the topic.
 */
public class SimpleKafkaReader<T> implements AutoCloseable {

    private final KafkaConsumer<String, T> kafkaConsumer;

    /**
     * @param kafkaConsumer
     * @param topic
     */
    public SimpleKafkaReader(KafkaConsumer<String, T> kafkaConsumer, String topic) {
        super();
        this.kafkaConsumer = kafkaConsumer;
        kafkaConsumer.subscribe(Collections.singleton(topic));
    }

    /**
     * Poll Kafka until the desired number of records is received
     * <p>
     * May throw an error if there are more than the expected number of records on the topic
     *
     * @param count   the number of records expected
     * @param timeout the amount of time to wait for the expected number of records to be received
     * @return the list of records received
     */
    public List<T> waitForMessages(int count, Duration timeout) {
        ArrayList<T> result = new ArrayList<>();
        Duration remaining = timeout;
        long startTime = System.nanoTime();
        while (!remaining.isNegative() && result.size() < count) {
            for (ConsumerRecord<String, T> record : kafkaConsumer.poll(remaining)) {
                result.add(record.value());
            }
            Duration elapsed = Duration.ofNanos(System.nanoTime() - startTime);
            remaining = timeout.minus(elapsed);
        }
        assertThat("Wrong number of records fetched from kafka", result, hasSize(count));
        return result;
    }

    /**
     * Get the underlying Kafka Consumer
     *
     * @return the consumer
     */
    public KafkaConsumer<String, T> getConsumer() {
        return kafkaConsumer;
    }

    @Override
    public void close() throws Exception {
        kafkaConsumer.close();
    }

}
