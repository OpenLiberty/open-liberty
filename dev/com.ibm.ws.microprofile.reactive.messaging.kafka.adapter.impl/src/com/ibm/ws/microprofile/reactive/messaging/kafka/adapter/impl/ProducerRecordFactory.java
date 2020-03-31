/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ProducerRecordFactory {

    private static final String CLAZZ = ProducerRecordFactory.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLAZZ);

    /**
     * Get a delegate (Kafka Client API) Producer Record
     *
     * @param <V> the type of the payload value
     * @param configuredTopic The configured topic, may be null
     * @param channelName The channel name
     * @param value The payload value
     * @return the delegate ProducerRecord
     */
    public static <V> org.apache.kafka.clients.producer.ProducerRecord<?, ?> getDelegateProducerRecord(String configuredTopic, String channelName, V value) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLAZZ, "newDelegateProducerRecord", "Configured Topic: {0}, Channel Name: {1}, Value: {2}",
                        new String[] { configuredTopic, channelName, value.toString() });
        }

        org.apache.kafka.clients.producer.ProducerRecord<?, ?> delegateRecord;

        if (value instanceof org.apache.kafka.clients.producer.ProducerRecord) {
            org.apache.kafka.clients.producer.ProducerRecord<?, ?> userProducerRecord = (org.apache.kafka.clients.producer.ProducerRecord<?, ?>) value;

            delegateRecord = extractUserProducerRecord(configuredTopic, userProducerRecord);

            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.logp(Level.FINEST, CLAZZ, "newDelegateProducerRecord", "User provider ProducerRecord. Topic: {0}, Value: {1}",
                            new String[] { delegateRecord.topic(), delegateRecord.value().toString() });
            }
        } else {
            //if a topic was not configured, default to the channel name
            String topic = configuredTopic == null ? channelName : configuredTopic;

            delegateRecord = new org.apache.kafka.clients.producer.ProducerRecord<String, V>(topic, value);

            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.logp(Level.FINEST, CLAZZ, "newDelegateProducerRecord", "Internal ProducerRecord. Topic: {0}, Value: {1}",
                            new String[] { delegateRecord.topic(), delegateRecord.value().toString() });
            }
        }

        return delegateRecord;
    }

    /**
     * Extract a suitable delegate ProducerRecord from a user provided one.
     * It will be a brand new ProducerRecord if the configuredTopic does not match the one in the user ProducerRecord.
     * If the topic is the same then this method will just return the user's ProducerRecord as-is
     *
     * @param <K> key type
     * @param <V> value type
     * @param configuredTopic The configured topic
     * @param userProducerRecord The user's producer record
     * @return
     */
    public static <K, V> org.apache.kafka.clients.producer.ProducerRecord<K, V> extractUserProducerRecord(String configuredTopic,
                                                                                                          org.apache.kafka.clients.producer.ProducerRecord<K, V> userProducerRecord) {

        org.apache.kafka.clients.producer.ProducerRecord<K, V> delegateRecord;

        String userTopic = userProducerRecord.topic();

        //if a topic was not configured, use the one in the user provided ProducerRecord
        String topic = configuredTopic == null ? userTopic : configuredTopic;
        if (topic.contentEquals(userTopic)) {
            delegateRecord = userProducerRecord;
        } else {
            org.apache.kafka.common.header.Headers headers = userProducerRecord.headers();
            K key = userProducerRecord.key();
            Integer partition = userProducerRecord.partition();
            Long timestamp = userProducerRecord.timestamp();
            V userValue = userProducerRecord.value();

            delegateRecord = new org.apache.kafka.clients.producer.ProducerRecord<>(topic, partition, timestamp, key, userValue, headers);
        }

        return delegateRecord;
    }
}
