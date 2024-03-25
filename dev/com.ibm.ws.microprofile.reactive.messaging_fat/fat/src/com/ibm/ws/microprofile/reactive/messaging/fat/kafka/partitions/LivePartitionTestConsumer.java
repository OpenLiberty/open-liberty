/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.partitions;

import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.partitions.LivePartitionTestBean.FINAL_MESSAGE_NUMBER;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

/**
 * Consumer which joins the topic, consumes and commits five messages and then closes
 */
public class LivePartitionTestConsumer implements Runnable {

    private static final long TIMEOUT_NS = DEFAULT_KAFKA_TIMEOUT.toNanos();
    private static final int MESSAGES_TO_CONSUME = 5;

    private final Map<String, Object> config;
    private final String topic;

    private final List<String> messagesReceived;

    public LivePartitionTestConsumer(Map<String, Object> config, String topic) {
        this.config = new HashMap<>(config);
        this.config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        this.topic = topic;
        this.messagesReceived = new ArrayList<>();
    }

    @Override
    public void run() {
        System.out.println("Test consumer starting");
        try (KafkaConsumer<?, String> consumer = new KafkaConsumer<>(config)) {
            consumer.subscribe(Collections.singleton(topic));
            ConsumerRecord<?, String> lastRecord = null;

            int messages = 0;
            long startTime = System.nanoTime();
            readloop: while (messages < MESSAGES_TO_CONSUME && System.nanoTime() - startTime < TIMEOUT_NS) {
                ConsumerRecords<?, String> records = consumer.poll(Duration.ofSeconds(5));
                System.out.println("Test consumer polled " + records.count() + " records");
                for (ConsumerRecord<?, String> record : records) {
                    if (record.value().endsWith(Integer.toString(FINAL_MESSAGE_NUMBER))) {
                        // Never consume the final record, the test bean looks for it
                        break readloop;
                    }
                    messages++;
                    messagesReceived.add(record.value());
                    lastRecord = record;
                    if (messages >= MESSAGES_TO_CONSUME) {
                        break;
                    }
                }
            }

            if (lastRecord != null) {
                TopicPartition topicPartition = new TopicPartition(lastRecord.topic(), lastRecord.partition());
                OffsetAndMetadata offset = new OffsetAndMetadata(lastRecord.offset() + 1);
                System.out.println("Test consumer committing offset: " + offset);
                consumer.commitSync(Collections.singletonMap(topicPartition, offset));
            }
            System.out.println("Test consumer consumed " + messages + " messages.");
        }
    }

    public List<String> getMessages() {
        return messagesReceived;
    }
}
