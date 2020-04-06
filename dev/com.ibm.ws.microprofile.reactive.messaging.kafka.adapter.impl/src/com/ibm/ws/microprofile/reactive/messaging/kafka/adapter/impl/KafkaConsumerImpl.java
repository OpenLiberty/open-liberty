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
package com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.impl;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.ConsumerRebalanceListener;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.ConsumerRecords;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaConsumer;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.OffsetAndMetadata;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.OffsetCommitCallback;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.TopicPartition;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.WakeupException;

/**
 *
 */
public class KafkaConsumerImpl<K, V> extends AbstractKafkaAdapter<org.apache.kafka.clients.consumer.KafkaConsumer<K, V>> implements KafkaConsumer<K, V> {

    private static final String CLAZZ = KafkaConsumerImpl.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLAZZ);

    public KafkaConsumerImpl(Map<String, Object> consumerConfig) {
        super(new org.apache.kafka.clients.consumer.KafkaConsumer<>(consumerConfig));
    }

    /**
     *
     */
    @Override
    public void wakeup() {
        this.getDelegate().wakeup();
    }

    /**
     *
     */
    @Override
    public void close() {
        this.getDelegate().close();
    }

    /**
     * @param topics
     */
    @Override
    public void subscribe(Collection<String> topics) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLAZZ, "subscribe", "Topics: {0}", topics);
        }
        this.getDelegate().subscribe(topics);
    }

    /**
     * @param topics
     * @param ackTracker
     */
    @Override
    public void subscribe(Collection<String> topics, ConsumerRebalanceListener listener) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLAZZ, "subscribe", "Topics: {0}", topics);
        }
        org.apache.kafka.clients.consumer.ConsumerRebalanceListener delegateListener = new ConsumerRebalanceListenerImpl(listener);
        this.getDelegate().subscribe(topics, delegateListener);
    }

    /**
     * @param duration
     * @return
     */
    @Override
    public ConsumerRecords<K, V> poll(Duration duration) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLAZZ, "poll", "Duration: {0}", duration);
        }
        org.apache.kafka.clients.consumer.ConsumerRecords<K, V> delegateRecords;
        try {
            delegateRecords = this.getDelegate().poll(duration);
        } catch (org.apache.kafka.common.errors.WakeupException dwe) {
            throw new WakeupException(dwe);
        }
        ConsumerRecords<K, V> records = new ConsumerRecordsImpl<>(delegateRecords);
        return records;
    }

    /**
     * @param offsets
     * @param object
     */
    @Override
    public void commitAsync(Map<TopicPartition, OffsetAndMetadata> offsets, OffsetCommitCallback callback) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLAZZ, "commitAsync", "Offsets: {0}", offsets);
        }
        Map<org.apache.kafka.common.TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> delegateOffsets = unwrap(offsets);

        org.apache.kafka.clients.consumer.OffsetCommitCallback delegateOffsetCommitCallback = (o, e) -> {

            Map<org.apache.kafka.common.TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> delOffsets = o;
            Map<TopicPartition, OffsetAndMetadata> offsetsx = wrap(delOffsets);

            callback.onComplete(offsetsx, e);
        };

        this.getDelegate().commitAsync(delegateOffsets, delegateOffsetCommitCallback);
    }

    private Map<org.apache.kafka.common.TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> unwrap(Map<TopicPartition, OffsetAndMetadata> offsets) {
        Map<org.apache.kafka.common.TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> delegateOffsets = new HashMap<>();
        for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : offsets.entrySet()) {

            TopicPartitionImpl topicPartition = (TopicPartitionImpl) entry.getKey();
            org.apache.kafka.common.TopicPartition delegateTopicPartition = topicPartition.getDelegate();

            OffsetAndMetadataImpl offsetAndMetadata = (OffsetAndMetadataImpl) entry.getValue();
            org.apache.kafka.clients.consumer.OffsetAndMetadata delegateOffsetAndMetadata = offsetAndMetadata.getDelegate();

            delegateOffsets.put(delegateTopicPartition, delegateOffsetAndMetadata);
        }
        return delegateOffsets;
    }

    private Map<TopicPartition, OffsetAndMetadata> wrap(Map<org.apache.kafka.common.TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> delegateOffsets) {
        Map<TopicPartition, OffsetAndMetadata> offsetsx = new HashMap<>();
        for (Map.Entry<org.apache.kafka.common.TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> entry : delegateOffsets.entrySet()) {

            org.apache.kafka.common.TopicPartition delegateTopicPartition = entry.getKey();
            TopicPartition topicPartitionx = new TopicPartitionImpl(delegateTopicPartition);

            org.apache.kafka.clients.consumer.OffsetAndMetadata delegateOffsetAndMetadata = entry.getValue();
            OffsetAndMetadata offsetAndMetadatax = new OffsetAndMetadataImpl(delegateOffsetAndMetadata);

            offsetsx.put(topicPartitionx, offsetAndMetadatax);
        }
        return offsetsx;
    }

    @Override
    public long position(TopicPartition partition) {
        TopicPartitionImpl partitionImpl = (TopicPartitionImpl) partition;
        return this.getDelegate().position(partitionImpl.getDelegate());
    }

}
