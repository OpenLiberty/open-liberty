/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.kafka;

import java.time.Duration;

import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaAdapterFactory;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.TopicPartition;

import io.openliberty.microprofile.reactive.messaging.internal.interfaces.RMAsyncProvider;

/**
 *
 */
public class PartitionTrackerFactory {

    private KafkaAdapterFactory adapterFactory = null;
    private int commitBatchMaxElements = 500;
    private Duration commitBatchMaxInterval = Duration.ofMillis(500);
    private boolean autoCommitEnabled = false;
    private RMAsyncProvider asyncProvider = null;

    public void setAdapterFactory(KafkaAdapterFactory adapterFactory) {
        this.adapterFactory = adapterFactory;
    }

    public void setAsyncProvider(RMAsyncProvider asyncProvider) {
        this.asyncProvider = asyncProvider;
    }

    public void setCommitBatchMaxElements(int commitBatchMaxElements) {
        this.commitBatchMaxElements = commitBatchMaxElements;
    }

    public void setCommitBatchMaxInterval(Duration commitBatchMaxInterval) {
        this.commitBatchMaxInterval = commitBatchMaxInterval;
    }

    public void setAutoCommitEnabled(boolean autoCommitEnabled) {
        this.autoCommitEnabled = autoCommitEnabled;
    }

    /**
     * Creates a partition tracker for the given partition
     *
     * @param partition the partition
     * @return the tracker
     */
    public PartitionTracker create(KafkaInput<?, ?> kafkaInput, TopicPartition partition, long initialCommittedOffset) {
        if (autoCommitEnabled) {
            return new PartitionTracker(partition);
        } else {
            return new CommittingPartitionTracker(partition, adapterFactory, kafkaInput, initialCommittedOffset, asyncProvider, commitBatchMaxElements, commitBatchMaxInterval);
        }
    }

}
