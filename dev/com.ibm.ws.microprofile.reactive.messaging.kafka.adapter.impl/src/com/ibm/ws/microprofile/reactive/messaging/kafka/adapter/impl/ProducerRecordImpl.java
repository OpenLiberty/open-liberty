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

import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.ProducerRecord;

/**
 *
 */
public class ProducerRecordImpl<K, V> extends AbstractKafkaAdapter<org.apache.kafka.clients.producer.ProducerRecord<K, V>> implements ProducerRecord<K, V> {

    @SuppressWarnings("unchecked")
    public ProducerRecordImpl(String configuredTopic, String channelName, V value) {
        super((org.apache.kafka.clients.producer.ProducerRecord<K, V>) ProducerRecordFactory.getDelegateProducerRecord(configuredTopic, channelName, value));
    }

}
