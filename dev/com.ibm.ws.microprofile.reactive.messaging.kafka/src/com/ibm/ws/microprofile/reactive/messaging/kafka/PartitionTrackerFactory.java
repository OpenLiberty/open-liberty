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
package com.ibm.ws.microprofile.reactive.messaging.kafka;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;

import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaAdapterFactory;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.TopicPartition;

/**
 *
 */
public class PartitionTrackerFactory {

    private KafkaAdapterFactory adapterFactory = null;
    private ScheduledExecutorService executor = null;
    private int commitBatchMaxElements = 500;
    private Duration commitBatchMaxInterval = Duration.ofMillis(500);
    private boolean autoCommitEnabled = false;

    public void setAdapterFactory(KafkaAdapterFactory adapterFactory) {
        this.adapterFactory = adapterFactory;
    }

    public void setExecutor(ScheduledExecutorService executor) {
        this.executor = executor;
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
            return new CommittingPartitionTracker(partition, adapterFactory, kafkaInput, initialCommittedOffset, executor, commitBatchMaxElements, commitBatchMaxInterval);
        }
    }

}
