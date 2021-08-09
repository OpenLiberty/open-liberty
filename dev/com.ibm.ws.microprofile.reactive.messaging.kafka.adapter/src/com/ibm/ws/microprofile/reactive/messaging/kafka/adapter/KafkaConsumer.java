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
package com.ibm.ws.microprofile.reactive.messaging.kafka.adapter;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

/**
 *
 */
public interface KafkaConsumer<K, V> extends KafkaAdapter {

    /**
     *
     */
    void wakeup();

    /**
     *
     */
    void close();

    /**
     * @param topics
     */
    void subscribe(Collection<String> topics);

    /**
     * @param topics
     * @param ackTracker
     */
    void subscribe(Collection<String> topics, ConsumerRebalanceListener listener);

    /**
     * @param duration
     * @return
     */
    ConsumerRecords<K, V> poll(Duration duration);

    /**
     * @param offsets
     * @param object
     */
    void commitAsync(Map<TopicPartition, OffsetAndMetadata> offsets, OffsetCommitCallback callback);

    long position(TopicPartition partition);

}
