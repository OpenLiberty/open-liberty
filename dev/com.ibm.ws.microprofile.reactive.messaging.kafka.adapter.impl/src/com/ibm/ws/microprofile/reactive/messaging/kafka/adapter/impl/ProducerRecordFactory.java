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

    public static <K, V> org.apache.kafka.clients.producer.ProducerRecord<K, V> newDelegateProducerRecord(String configuredTopic, String channelName, V value,
                                                                                                          boolean allowKafkaProducerRecord) {//TODO remove beta guard before GA
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLAZZ, "newDelegateProducerRecord", "Configured Topic: {0}, Channel Name: {1}, Value: {2}",
                        new String[] { configuredTopic, channelName, value.toString() });
        }

        org.apache.kafka.clients.producer.ProducerRecord<K, V> delegateRecord;

        //TODO remove beta guard before GA
        if (allowKafkaProducerRecord && (value instanceof org.apache.kafka.clients.producer.ProducerRecord)) {
            org.apache.kafka.clients.producer.ProducerRecord<K, V> userProducerRecord = (org.apache.kafka.clients.producer.ProducerRecord<K, V>) value;
            org.apache.kafka.common.header.Headers headers = userProducerRecord.headers();
            K key = userProducerRecord.key();
            Integer partition = userProducerRecord.partition();
            Long timestamp = userProducerRecord.timestamp();
            String userTopic = userProducerRecord.topic();
            V userValue = userProducerRecord.value();

            //if a topic was not configured, use the one in the user provided ProducerRecord
            String topic = configuredTopic == null ? userTopic : configuredTopic;

            delegateRecord = new org.apache.kafka.clients.producer.ProducerRecord<>(topic, partition, timestamp, key, userValue, headers);

            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.logp(Level.FINEST, CLAZZ, "newDelegateProducerRecord", "User provider ProducerRecord. Topic: {0}, Value: {1}",
                            new String[] { delegateRecord.topic(), delegateRecord.value().toString() });
            }
        } else {
            //if a topic was not configured, default to the channel name
            String topic = configuredTopic == null ? channelName : configuredTopic;

            delegateRecord = new org.apache.kafka.clients.producer.ProducerRecord<>(topic, value);

            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.logp(Level.FINEST, CLAZZ, "newDelegateProducerRecord", "Internal ProducerRecord. Topic: {0}, Value: {1}",
                            new String[] { delegateRecord.topic(), delegateRecord.value().toString() });
            }
        }

        return delegateRecord;
    }
}
