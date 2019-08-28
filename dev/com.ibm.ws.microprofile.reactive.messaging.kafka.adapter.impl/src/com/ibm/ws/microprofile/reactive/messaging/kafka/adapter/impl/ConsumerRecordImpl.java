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

import java.util.Optional;

import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.ConsumerRecord;

/**
 *
 */
public class ConsumerRecordImpl<K, V> extends AbstractKafkaAdapter<org.apache.kafka.clients.consumer.ConsumerRecord<K, V>> implements ConsumerRecord<K, V> {

    /**
     * @param delegate
     */
    public ConsumerRecordImpl(org.apache.kafka.clients.consumer.ConsumerRecord<K, V> delegate) {
        super(delegate);
    }

    /**
     * @return
     */
    @Override
    public String topic() {
        return getDelegate().topic();
    }

    /**
     * @return
     */
    @Override
    public int partition() {
        return getDelegate().partition();
    }

    /**
     * @return
     */
    @Override
    public long offset() {
        return getDelegate().offset();
    }

    /**
     * @return
     */
    @Override
    public Optional<Integer> leaderEpoch() {
        return getDelegate().leaderEpoch();
    }

    /** {@inheritDoc} */
    @Override
    public V value() {
        return getDelegate().value();
    }

}
