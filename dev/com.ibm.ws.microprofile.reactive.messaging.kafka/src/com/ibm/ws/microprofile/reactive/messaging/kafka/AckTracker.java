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
package com.ibm.ws.microprofile.reactive.messaging.kafka;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.eclipse.microprofile.reactive.messaging.Message;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.ConsumerRebalanceListener;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.ConsumerRecord;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaAdapterFactory;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.OffsetAndMetadata;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.TopicPartition;

/**
 * Tracks which messages have been acknowledged and generates commit events when
 * there are messages which can be committed
 */
public class AckTracker implements ConsumerRebalanceListener {

    private static final TraceComponent tc = Tr.register(AckTracker.class);

    private final Map<TopicPartition, PartitionAckTracker> partitionTrackers;
    private final ScheduledExecutorService executor;
    private final int ackThreshold;
    private final AtomicInteger outstandingAcks;

    private CommitAction commitAction = (p, o) -> CompletableFuture.completedFuture(null);
    private CompletableFuture<Void> ackThresholdStage = CompletableFuture.completedFuture(null);
    private final KafkaAdapterFactory kafkaAdapterFactory;

    public AckTracker(KafkaAdapterFactory kafkaAdapterFactory, ScheduledExecutorService executor, int ackThreshold) {
        this.kafkaAdapterFactory = kafkaAdapterFactory;
        this.executor = executor;
        this.ackThreshold = ackThreshold;
        this.partitionTrackers = Collections.synchronizedMap(new HashMap<>());
        this.outstandingAcks = new AtomicInteger(0);
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, partitions.size() + " removed partitions", partitions);
        }

        for (TopicPartition partition : partitions) {
            PartitionAckTracker tracker = this.partitionTrackers.remove(partition);
            if (tracker != null) {
                tracker.close();
            }
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, partitions.size() + " new partitions", partitions);
        }

        for (TopicPartition partition : partitions) {
            if (this.partitionTrackers.get(partition) == null) {
                this.partitionTrackers.put(partition, new PartitionAckTracker(partition));
            }
        }
    }

    /**
     * Track the acknowledgement of the given record
     * <p>
     * The Supplier returned from this message should be passed as the second
     * argument to {@link Message#of(Object, Supplier)}
     *
     * @param record the kafka record to track
     * @return the ack function to be used when creating the message for this record
     */
    public Supplier<CompletionStage<Void>> trackRecord(ConsumerRecord<?, ?> record) {
        TopicPartition partition = this.kafkaAdapterFactory.newTopicPartition(record.topic(), record.partition());
        PartitionAckTracker partitionTracker = this.partitionTrackers.get(partition);
        if (partitionTracker == null) {
            CompletableFuture<Void> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(this.kafkaAdapterFactory.newCommitFailedException());
            return () -> failedFuture;
        } else {
            MessageAckData ackData = partitionTracker.recordSent(record);
            return () -> partitionTracker.recordAck(ackData);
        }
    }

    /**
     * Set the action which should be run to commit record reads
     *
     * @param commitAction the action
     */
    public void setCommitAction(CommitAction commitAction) {
        this.commitAction = commitAction;
    }

    /**
     * Returns a completion stage which completes when the number of unacked
     * messages is less than the threshold value
     * <p>
     * If the number of unacked messages is already below the threshold value then a
     * completed completion stage is returned
     *
     * @return
     */
    public CompletionStage<Void> waitForAckThreshold() {
        return this.ackThresholdStage;
    }

    /**
     * Shutdown the ack tracker
     * <p>
     * This will cause all incomplete acks() to fail and ensure that we don't try to commit any more messages.
     */
    public void shutdown() {
        synchronized (partitionTrackers) {
            Iterator<PartitionAckTracker> i = partitionTrackers.values().iterator();
            while (i.hasNext()) {
                PartitionAckTracker tracker = i.next();
                i.remove();
                try {
                    tracker.close();
                } catch (Exception e) {
                    // Ensures we try to close all trackers
                    // and that we get an FFDC on error
                }
            }
        }
    }

    private void incrementOutstandingAcks() {
        synchronized (this.outstandingAcks) {
            int outstanding = this.outstandingAcks.incrementAndGet();
            if (outstanding == this.ackThreshold) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Too many outstanding unacked messages, stopping polling kafka. Current count: " + outstanding);
                }

                this.ackThresholdStage = new CompletableFuture<>();
            }
        }
    }

    private void decrementOutstandingAcks() {
        CompletableFuture<Void> thresholdStage;
        int outstanding;
        synchronized (this.outstandingAcks) {
            outstanding = this.outstandingAcks.getAndDecrement();
            thresholdStage = this.ackThresholdStage;
        }
        if (outstanding == this.ackThreshold) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Outstanding unacked message count dropped below threshold, resuming polling kafka. Current count: " + outstanding);
            }
            thresholdStage.complete(null);
        }
    }

    @FunctionalInterface
    public interface CommitAction {
        CompletionStage<Void> doCommit(TopicPartition partition, OffsetAndMetadata offset);
    }

    private class PartitionAckTracker {

        private final List<MessageAckData> unackedMessages;
        private final Batcher<MessageAckData> messageBatcher = Batcher.create(MessageAckData.class).withMaxBatchSize(2000).withMaxBatchTime(Duration.ofMillis(500)).withProcessBatchAction(this::commitAckBatch).withExecutor(AckTracker.this.executor).build();
        private final TopicPartition partition;

        private PartitionAckTracker(TopicPartition partition) {
            this.partition = partition;
            this.unackedMessages = new LinkedList<>();
        }

        /**
         * Record a message that is sent into the stream
         * <p>
         * This must be called once for every message emitted from the connector
         *
         * @param record the record being emitted
         * @return the MessageAckData for that message
         */
        public MessageAckData recordSent(ConsumerRecord<?, ?> record) {
            MessageAckData ackCompletion = new MessageAckData(record.offset(), record.leaderEpoch());
            synchronized (this.unackedMessages) {
                this.unackedMessages.add(ackCompletion);
            }
            incrementOutstandingAcks();
            return ackCompletion;
        }

        /**
         * Process the acknowledgement of a message
         * <p>
         * This must be called as part of the ack callback for every message emitted
         * from the container
         *
         * @param messageAckData the MessageAckData for that message
         * @return a CompletionStage to indicate when the ack has been processed
         */
        public CompletionStage<Void> recordAck(MessageAckData messageAckData) {
            CompletableFuture<Void> result = new CompletableFuture<>();
            try {
                if (AckTracker.this.partitionTrackers.get(this.partition) == null) {
                    result.completeExceptionally(AckTracker.this.kafkaAdapterFactory.newCommitFailedException());
                } else {
                    synchronized (this.unackedMessages) {
                        messageAckData.setCompletion(result);
                        batchPendingAcks();
                    }
                    decrementOutstandingAcks();
                }
            } catch (Throwable t) {
                result.completeExceptionally(t);
                throw t;
            }
//            return result;
            return CompletableFuture.completedFuture(null); // TODO: wait for acks here?
        }

        /**
         * Check for a contiguous block of acknowledged messages and batch them to be
         * committed
         */
        private void batchPendingAcks() {

            synchronized (this.unackedMessages) {
                Iterator<MessageAckData> i = this.unackedMessages.iterator();
                while (i.hasNext()) {
                    MessageAckData ackData = i.next();
                    if (ackData.getCompletion() == null) {
                        break;
                    } else {
                        this.messageBatcher.addToBatch(ackData);
                        i.remove();
                    }
                }
            }
        }

        /**
         * Commit the current batch of acknowledged messages
         */
        private void commitAckBatch(List<MessageAckData> toCommit) {
            if (toCommit.isEmpty()) {
                return;
            }

            MessageAckData lastAcked = toCommit.get(toCommit.size() - 1);
            long commitOffset = lastAcked.offset + 1;
            OffsetAndMetadata offset = AckTracker.this.kafkaAdapterFactory.newOffsetAndMetadata(commitOffset, lastAcked.getLeaderEpoch(), null);
            CompletionStage<Void> commitResult = AckTracker.this.commitAction.doCommit(this.partition, offset);

            commitResult.handleAsync((r, t) -> {
                if (t == null) {
                    for (MessageAckData ackData : toCommit) {
                        ackData.getCompletion().complete(null);
                    }
                } else {
                    for (MessageAckData ackData : toCommit) {
                        ackData.getCompletion().completeExceptionally(t);
                    }
                }
                return null;
            }, AckTracker.this.executor);

        }

        /**
         * Called when we are no longer tracking acks for the partition
         */
        private void close() {
            messageBatcher.close();
            synchronized (this.unackedMessages) {
                for (MessageAckData ackData : this.unackedMessages) {
                    if (ackData.getCompletion() != null) {
                        ackData.getCompletion().completeExceptionally(AckTracker.this.kafkaAdapterFactory.newCommitFailedException());
                    }
                }
            }
        }
    }

    private static class MessageAckData {
        private final long offset;
        private CompletableFuture<Void> completion;
        private final Optional<Integer> leaderEpoch;

        public MessageAckData(long offset, Optional<Integer> leaderEpoch) {
            super();
            this.offset = offset;
            this.leaderEpoch = leaderEpoch;
            this.completion = null;
        }

        public void setCompletion(CompletableFuture<Void> completion) {
            this.completion = completion;
        }

        public CompletableFuture<Void> getCompletion() {
            return this.completion;
        }

        public Optional<Integer> getLeaderEpoch() {
            return this.leaderEpoch;
        }

    }
}
