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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaAdapterFactory;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.OffsetAndMetadata;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.TopicPartition;

/**
 * Tracks the assignment of a partition to this consumer and commits message offsets to that partition
 * <p>
 * In addition to the function of {@link PartitionTracker}, this class additionally commits message offsets back to the kafka broker in response to a call to
 * {@link #recordDone(long, Optional)}.
 * <p>
 * For performance, each completed record isn't committed immediately. Instead, this class tries to batch up completed records and commit them together. There are several
 * parameters that control how often the message offset is committed:
 * <ul>
 * <li>{@code maxCommitBatchSize}: sets the maximum number of records to wait for before committing the offset</li>
 * <li>{@code maxCommitBatchInterval}: sets the maximum time to wait after a {@link #recordDone(long, Optional)} is called before committing the offset</li>
 * </ul>
 */
public class CommittingPartitionTracker extends PartitionTracker {

    private static final TraceComponent tc = Tr.register(CommittingPartitionTracker.class);

    private final KafkaAdapterFactory factory;
    private final ScheduledExecutorService executor;
    private final KafkaInput<?, ?> kafkaInput;
    private final int maxCommitBatchSize;
    private final Duration maxCommitBatchInterval;

    /**
     * Set of CompletedWork which has either not been committed or has been committed but the commit has not completed yet.
     * <p>
     * Any access to this must be synchronized on {@link #completedWork}
     */
    private final SortedSet<CompletedWork> completedWork = new TreeSet<>();

    /**
     * The count of CompletedWork for which a commit has not been started
     * <p>
     * Any access to this must be synchronized on {@link #completedWork}
     */
    private int outstandingUncommittedWork = 0;

    /**
     * The last offset for which a commit was started
     * <p>
     * Any access to this must be synchronized on {@link #completedWork}
     */
    private long committedOffset;

    /**
     * The currently scheduled task which will attempt to commit completed work or {@code null} if no task has been scheduled.
     * <p>
     * Any access to this must be synchronized on {@link #completedWork}
     */
    private Future<?> pendingCommitTask = null;

    /**
     *
     * @param topicPartition the partition to track
     * @param factory the KafkaAdaptorFactory
     * @param kafkaInput the KafkaInput
     * @param initialCommittedOffset the position of the reader when the partition was assigned
     * @param executor a ScheduledExecutorService
     * @param maxCommitBatchSize the maximum number of records to wait for before committing the offset, or zero or less to indicate no maximum
     * @param maxCommitBatchInterval the maximum time to wait after a {@link #recordDone(long, Optional)} is called before committing the offset, or zero or less to indicate that
     *            the offset should not be committed at a regular interval
     */
    public CommittingPartitionTracker(TopicPartition topicPartition,
                                      KafkaAdapterFactory factory,
                                      KafkaInput<?, ?> kafkaInput,
                                      long initialCommittedOffset,
                                      ScheduledExecutorService executor,
                                      int maxCommitBatchSize,
                                      Duration maxCommitBatchInterval) {
        super(topicPartition);
        this.factory = factory;
        this.kafkaInput = kafkaInput;
        this.executor = executor;
        this.committedOffset = initialCommittedOffset;
        this.maxCommitBatchSize = maxCommitBatchSize;
        this.maxCommitBatchInterval = maxCommitBatchInterval;
    }

    @Override
    public CompletionStage<Void> recordDone(long offset, Optional<Integer> leaderEpoch) {

        CompletableFuture<Void> result = new CompletableFuture<>();
        try {
            synchronized (completedWork) {
                // Protect against this method being called twice for the same message
                boolean isNewOffset;
                if (offset < committedOffset) {
                    isNewOffset = false;
                } else {
                    isNewOffset = completedWork.add(new CompletedWork(offset, leaderEpoch, result));
                }

                if (isNewOffset) {
                    outstandingUncommittedWork++;
                    requestCommit();
                } else {
                    result.completeExceptionally(new IllegalStateException("recordDone called more than once for offset " + offset));
                }
            }
        } catch (Throwable t) {
            // Log any unexpected exceptions and return them in the completion stage
            Tr.error(tc, "internal.kafka.connector.error.CWMRX1000E", t);
            result.completeExceptionally(new KafkaConnectorException(Tr.formatMessage(tc, "internal.kafka.connector.error.CWMRX1000E"), t));
        }

        return result;
    }

    /**
     * Request that done but uncommitted work is committed, either now or in the future
     * <p>
     * This method will either commit the partition offset now, or schedule it to be done in the future, depending on the values of {@link #maxCommitBatchInterval} and
     * {@link #maxCommitBatchSize}.
     * <p>
     * Calls to this method must be synchronized on {@link #completedWork}
     */
    private void requestCommit() {
        if ((maxCommitBatchSize > 0) && (outstandingUncommittedWork > maxCommitBatchSize)) {
            if (pendingCommitTask != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Cancelling scheduled commit task because we're committing right now", this);
                }
                pendingCommitTask.cancel(true);
            }
            // commit now
            commitCompletedWork();
        } else {
            if ((pendingCommitTask == null) && !maxCommitBatchInterval.isZero()) {
                // commit later
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Scheduling deferred commit task", this);
                }
                pendingCommitTask = executor.schedule(this::commitCompletedWork, maxCommitBatchInterval.toNanos(), TimeUnit.NANOSECONDS);
            }
        }
    }

    /**
     * Attempts to commit the latest block of completed but uncommitted work
     * <p>
     * We can only commit the offset for a record if all prior records are complete. This method looks through the committed work to see which messages can be committed without
     * leaving a gap and then commits them.
     */
    private void commitCompletedWork() {
        synchronized (completedWork) {
            if (Thread.interrupted()) {
                // Don't do anything if we were cancelled
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Commit task running but has been cancelled", this);
                }
                return;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Checking for new work to commit, last committed offset is " + committedOffset, this);
                Tr.debug(this, tc, "Current completed work", completedWork);
            }

            long newCommitOffset = committedOffset;
            CompletedWork newestWork = null;
            for (CompletedWork work : completedWork) {
                if (work.offset < newCommitOffset) {
                    // Work that we've already asked to commit, ignore it
                } else if (work.offset == newCommitOffset) {
                    // Work that should be committed now
                    newCommitOffset++;
                    newestWork = work;
                } else {
                    // We've reached the end of a continuous block of completed work
                    // Can't commit any further work until everything before it has been committed
                    break;
                }
            }

            if (newestWork != null) {
                // commit
                long originalCommitOffset = committedOffset;
                long finalCommitOffset = newCommitOffset;

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Committing from " + originalCommitOffset + " to " + finalCommitOffset, this);
                }
                commitUpTo(newestWork).whenCompleteAsync((r, t) -> processCommittedWork(originalCommitOffset, finalCommitOffset, t), executor);
            }

            outstandingUncommittedWork -= newCommitOffset - committedOffset;
            pendingCommitTask = null;
            committedOffset = newCommitOffset;
        }
    }

    /**
     * Commit the offset up to the offset of the given work
     *
     * @param work
     * @return completion stage which completes with the result of the commit, or completes exceptionally if the commit failed
     */
    private CompletionStage<Void> commitUpTo(CompletedWork work) {
        if (isClosed()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Rejecting commit attempt because partition is closed", this);
            }

            CompletableFuture<Void> result = new CompletableFuture<>();
            result.completeExceptionally(new Exception("Partition is closed"));
            return result;
        }

        // Note work.offset + 1
        // In general the committed offset for a partition is the first message which should be received by a new consumer
        // which starts consuming from that partition.
        // Therefore, the offset which we are about to commit must be the offset _after_ the last message we have processed.
        OffsetAndMetadata offsetAndMetadata = factory.newOffsetAndMetadata(work.offset + 1, work.leaderEpoch, null);
        return kafkaInput.commitOffsets(topicPartition, offsetAndMetadata);
    }

    /**
     * Complete the CompletionStage associated with work which has been committed
     * <p>
     * This is called as callback after the partition offset has been committed asynchronously
     *
     * @param originalOffset the committed offset before this commit
     * @param committedOffset the new committed offset
     * @param exception the exception which caused the commit to fail, or {@code null} if it was successful
     */
    private void processCommittedWork(long originalOffset, long committedOffset, Throwable exception) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (exception == null) {
                Tr.debug(this, tc, "Commit from " + originalOffset + " to " + committedOffset + " completed successfully", this);
            } else {
                Tr.debug(this, tc, "Commit from " + originalOffset + " to " + committedOffset + " failed", this, exception);
            }
        }

        // Note: Pull out the list of completed work inside the synchronized block
        //       but complete the CompletionStage outside the synchronized block
        List<CompletedWork> committedWork = new ArrayList<>();
        synchronized (completedWork) {
            for (Iterator<CompletedWork> i = completedWork.iterator(); i.hasNext();) {
                CompletedWork work = i.next();
                if (work.offset < originalOffset) {
                    continue;
                }
                if (work.offset >= committedOffset) {
                    break;
                }
                committedWork.add(work);
                i.remove();
            }
        }

        if (exception == null) {
            for (CompletedWork work : committedWork) {
                work.completion.complete(null);
            }
        } else {
            for (CompletedWork work : committedWork) {
                work.completion.completeExceptionally(exception);
            }
        }
    }

    @Override
    public void close() {
        synchronized (completedWork) {
            try {
                if (!completedWork.isEmpty()) {
                    // Attempt a final commit before we relinquish the partition
                    commitCompletedWork();
                }
            } finally {
                super.close();
            }
        }
    }

    /**
     * Represents a record which the application has finished processing, but which may not yet have been committed
     */
    public static class CompletedWork implements Comparable<CompletedWork> {
        private final long offset;
        private final CompletableFuture<Void> completion;
        private final Optional<Integer> leaderEpoch;

        public CompletedWork(long offset, Optional<Integer> leaderEpoch, CompletableFuture<Void> completion) {
            super();
            this.offset = offset;
            this.leaderEpoch = leaderEpoch;
            this.completion = completion;
        }

        public CompletableFuture<Void> getCompletion() {
            return this.completion;
        }

        public Optional<Integer> getLeaderEpoch() {
            return this.leaderEpoch;
        }

        @Override
        public int compareTo(CompletedWork o) {
            return Long.compare(offset, o.offset);
        }
    }

}
