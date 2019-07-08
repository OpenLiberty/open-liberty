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

import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.TopicPartition;

/**
 *
 */
public class TopicPartitionImpl extends AbstractKafkaAdapter<org.apache.kafka.common.TopicPartition> implements TopicPartition {

    /**
     * @param topic
     * @param partition
     */
    public TopicPartitionImpl(String topic, int partition) {
        this(new org.apache.kafka.common.TopicPartition(topic, partition));
    }

    /**
     * @param partition
     */
    public TopicPartitionImpl(org.apache.kafka.common.TopicPartition delegatePartition) {
        super(delegatePartition);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TopicPartitionImpl) {
            TopicPartitionImpl other = (TopicPartitionImpl) o;
            return getDelegate().equals(other.getDelegate());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getDelegate().hashCode();
    }

}
