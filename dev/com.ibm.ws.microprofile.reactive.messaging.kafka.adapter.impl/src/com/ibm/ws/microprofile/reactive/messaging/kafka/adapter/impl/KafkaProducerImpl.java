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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.Callback;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaProducer;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.ProducerRecord;

/**
 *
 */
public class KafkaProducerImpl<K, V> extends AbstractKafkaAdapter<org.apache.kafka.clients.producer.KafkaProducer<K, V>> implements KafkaProducer<K, V> {

    private static final String CLAZZ = KafkaProducerImpl.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLAZZ);

    public KafkaProducerImpl(Map<String, Object> producerConfig) {
        super(new org.apache.kafka.clients.producer.KafkaProducer<K, V>(producerConfig));
    }

    /** {@inheritDoc} */
    @Override
    public void send(ProducerRecord<K, V> producerRecord, Callback callback) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLAZZ, "send", "ProducerRecord: {0}",
                        new String[] { producerRecord.toString() });
        }

        org.apache.kafka.clients.producer.ProducerRecord<K, V> delegateRecord = (org.apache.kafka.clients.producer.ProducerRecord<K, V>) producerRecord.getDelegate();

        org.apache.kafka.clients.producer.Callback delegateCallback = (m, e) -> {
            org.apache.kafka.clients.producer.RecordMetadata delegateRecordMetadata = m;
            callback.onComplete(new RecordMetadataImpl(delegateRecordMetadata), e);
        };

        getDelegate().send(delegateRecord, delegateCallback);
    }

    @Override
    public void close(Duration timeout) {
        getDelegate().close(timeout);
    }

}
