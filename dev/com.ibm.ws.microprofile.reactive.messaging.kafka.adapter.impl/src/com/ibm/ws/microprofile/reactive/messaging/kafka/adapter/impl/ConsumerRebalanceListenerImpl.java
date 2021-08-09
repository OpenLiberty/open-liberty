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

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.ConsumerRebalanceListener;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.TopicPartition;

/**
 *
 */
public class ConsumerRebalanceListenerImpl implements org.apache.kafka.clients.consumer.ConsumerRebalanceListener {

    private static final String CLAZZ = ConsumerRebalanceListenerImpl.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLAZZ);

    private final ConsumerRebalanceListener listener;

    /**
     * @param listener
     */
    public ConsumerRebalanceListenerImpl(ConsumerRebalanceListener listener) {
        this.listener = listener;
    }

    /** {@inheritDoc} */
    @Override
    public void onPartitionsAssigned(Collection<org.apache.kafka.common.TopicPartition> delegatePartitions) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLAZZ, "onPartitionsAssigned", "TopicPartitions: {0}", delegatePartitions);
        }
        Collection<TopicPartition> partitions = wrap(delegatePartitions);
        this.listener.onPartitionsAssigned(partitions);
    }

    /** {@inheritDoc} */
    @Override
    public void onPartitionsRevoked(Collection<org.apache.kafka.common.TopicPartition> delegatePartitions) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLAZZ, "onPartitionsRevoked", "TopicPartitions: {0}", delegatePartitions);
        }
        Collection<TopicPartition> partitions = wrap(delegatePartitions);
        this.listener.onPartitionsRevoked(partitions);
    }

    private Collection<TopicPartition> wrap(Collection<org.apache.kafka.common.TopicPartition> delegatePartitions) {
        Collection<TopicPartition> partitions = new ArrayList<>();

        for (org.apache.kafka.common.TopicPartition delegatePartition : delegatePartitions) {
            TopicPartitionImpl partition = new TopicPartitionImpl(delegatePartition);
            partitions.add(partition);
        }
        return partitions;
    }

}
