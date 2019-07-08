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
package com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.impl;

import java.time.Duration;
import java.util.Map;

import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.Callback;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaProducer;

/**
 *
 */
public class KafkaProducerImpl<K, V> extends AbstractKafkaAdapter<org.apache.kafka.clients.producer.KafkaProducer<K, V>> implements KafkaProducer<K, V> {

    public KafkaProducerImpl(Map<String, Object> producerConfig) {
        super(new org.apache.kafka.clients.producer.KafkaProducer<K, V>(producerConfig));
    }

    /** {@inheritDoc} */
    @Override
    public void send(String topic, V value, Callback callback) {
        org.apache.kafka.clients.producer.ProducerRecord<K, V> delegateRecord = new org.apache.kafka.clients.producer.ProducerRecord<>(topic, value);

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
