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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.microprofile.reactive.messaging.Message;

import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.TopicPartition;

/**
 * Tracks a particular assignment of a partition to this consumer
 * <p>
 * This class has two important jobs:
 * <ul>
 * <li>{@link #recordDone(long, Optional)} can be used to create the result of {@link Message#ack()}</li>
 * <li>tracks whether this partition has been revoked (available from {@link #isClosed()})</li>
 * </ul>
 * <p>
 * If a partition is revoked and then later reassigned to this consumer, a new {@code PartitionTracker} instance is created to track that assignment.
 */
public class PartitionTracker {

    protected AtomicBoolean isClosed;
    protected final TopicPartition topicPartition;

    /**
     * @param topicPartition the partition to track
     */
    public PartitionTracker(TopicPartition topicPartition) {
        this.topicPartition = topicPartition;
        this.isClosed = new AtomicBoolean(false);
    }

    /**
     * Records that the assigned partition has been revoked
     */
    public void close() {
        isClosed.set(true);
    }

    /**
     * Returns whether the assigned partition has been revoked
     *
     * @return {@code true} if the assigned partition has been revoked
     */
    public boolean isClosed() {
        return isClosed.get();
    }

    /**
     * Record that processing of a record has been completed.
     * <p>
     * The result of this method can be used as the return value from {@link Message#ack()}
     *
     * @param offset the record offset
     * @param leaderEpoch the record leaderEpoch
     * @return a CompletionStage which completes when any associated processing has been completed (e.g. when the message offset has been committed)
     */
    public CompletionStage<Void> recordDone(long offset, Optional<Integer> leaderEpoch) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public String toString() {
        return "PartitionTracker [isClosed=" + isClosed + ", topicPartition=" + topicPartition + "]";
    }

}
