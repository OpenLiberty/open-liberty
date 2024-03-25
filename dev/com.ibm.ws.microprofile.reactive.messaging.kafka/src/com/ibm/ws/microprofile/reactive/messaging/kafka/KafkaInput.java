/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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

import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;
import static java.time.Duration.ZERO;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.ConsumerRebalanceListener;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.ConsumerRecord;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.ConsumerRecords;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaAdapterFactory;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaConsumer;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.OffsetAndMetadata;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.TopicPartition;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.WakeupException;

import io.openliberty.microprofile.reactive.messaging.internal.interfaces.RMAsyncProvider;
import io.openliberty.microprofile.reactive.messaging.internal.interfaces.RMContext;

/**
 * Connects a reactive stream to a Kafka topic
 *
 * @param K
 *            the key type
 * @param V
 *            the value type
 */
public class KafkaInput<K, V> implements ConsumerRebalanceListener {

    private static final TraceComponent tc = Tr.register(KafkaInput.class);
    private static final Duration FOREVER = Duration.ofMillis(Long.MAX_VALUE);

    private final KafkaConsumer<K, V> kafkaConsumer;
    private final RMAsyncProvider asyncProvider;
    private final Collection<String> topics;

    private PublisherBuilder<Message<V>> publisher;
    private boolean subscribed = false;
    private volatile boolean running = true;
    /**
     * indicates whether an error has occurred which should fail the stream
     */
    private volatile Throwable error = null;
    private final ConcurrentLinkedQueue<KafkaConsumerAction> tasks;
    private final KafkaAdapterFactory kafkaAdapterFactory;
    private final ThresholdCounter unackedMessageCounter;
    private final PartitionTrackerFactory partitionTrackerFactory;
    /**
     * If true, acknowledgement is reported complete immediately, otherwise it's not reported complete until the partition offset is committed which may fail.
     */
    private final boolean fastAck;

    /**
     * The current collection of partition trackers
     * <p>
     * When modifying this map it's very important that the map is <i>replaced</i> rather than <i>updated</i> as parts of the code rely on being able to take an immutable copy.
     */
    private volatile Map<TopicPartition, PartitionTracker> partitionTrackers = Collections.emptyMap();

    /**
     * Partitions we have been given to add but haven't yet processed
     * <p>
     * This should be {@code null} unless onPartitionsAssigned() has been interrupted by a WakeupException
     */
    private volatile Collection<TopicPartition> newPartitionsPending = null;

    /**
     * Lock to synchronize access to the kafkaConsumer instance
     */
    private final ReentrantLock lock = new ReentrantLock();

    public KafkaInput(KafkaAdapterFactory kafkaAdapterFactory, PartitionTrackerFactory partitionTrackerFactory,
                      KafkaConsumer<K, V> kafkaConsumer, RMAsyncProvider asyncProvider,
                      String topic, int unackedLimit, boolean fastAck) {
        super();
        this.kafkaConsumer = kafkaConsumer;
        this.asyncProvider = asyncProvider;
        this.topics = Collections.singleton(topic);
        this.tasks = new ConcurrentLinkedQueue<>();
        this.kafkaAdapterFactory = kafkaAdapterFactory;
        this.partitionTrackerFactory = partitionTrackerFactory;
        if (unackedLimit > 0) {
            this.unackedMessageCounter = new ThresholdCounterImpl(unackedLimit);
        } else {
            this.unackedMessageCounter = ThresholdCounter.UNLIMITED;
        }
        this.fastAck = fastAck;
    }

    public PublisherBuilder<Message<V>> getPublisher() {
        if (this.publisher == null) {
            this.publisher = createPublisher();
        }

        return this.publisher;
    }

    private PublisherBuilder<Message<V>> createPublisher() {
        PublisherBuilder<Message<V>> kafkaStream;
        kafkaStream = ReactiveStreams.generate(() -> 0)
                                     .flatMapCompletionStage(x -> unackedMessageCounter.waitForBelowThreshold().thenCompose(y -> pollKafkaAsync()))
                                     .flatMap(Function.identity())
                                     .peek(x -> unackedMessageCounter.increment())
                                     .takeWhile((record) -> this.running)
                                     .flatMap(this::errorMapper)
                                     .onError(e -> this.shutdown());
        return kafkaStream;
    }

    /**
     * Maps a message to an error if an error has occurred, or to itself otherwise
     *
     * @param message the message
     * @return a stream consisting of {@code message} or failing with the reported {@link #error}
     */
    private PublisherBuilder<Message<V>> errorMapper(Message<V> message) {
        if (error != null) {
            return ReactiveStreams.failed(error);
        } else {
            return ReactiveStreams.of(message);
        }
    }

    public CompletionStage<Void> commitOffsets(PartitionTracker partitionTracker, OffsetAndMetadata offset) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        try {
            Map<TopicPartition, OffsetAndMetadata> offsets = Collections.singletonMap(partitionTracker.getTopicPartition(), offset);
            runAction((c) -> {
                if (partitionTracker.isClosed()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Rejecting commit attempt because partition is closed", this);
                    }
                    result.completeExceptionally(new Exception("Partition is closed"));
                    return;
                }

                c.commitAsync(offsets, (o, e) -> {
                    if (e != null) {
                        Tr.warning(tc, "kafka.read.offsets.commit.warning.CWMRX1001W", e);
                        result.completeExceptionally(e);
                    } else {
                        if (isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Committed offsets successfully", o);
                        }
                        result.complete(null);
                    }
                });
            });
            return result;
        } catch (Throwable t) {
            Tr.warning(tc, "kafka.read.offsets.commit.warning.CWMRX1001W", t);
            result.completeExceptionally(t);
            return result;
        }
    }

    private static <T> Void logPollFailure(T result, Throwable t) {
        if (t != null) {
            Tr.error(tc, "kafka.poll.error.CWMRX1002E", t);
        }
        return null;
    }

    public void shutdown() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Shutting down Kafka connection");
        }

        this.running = false;
        this.kafkaConsumer.wakeup();
        this.lock.lock();
        try {
            this.kafkaConsumer.close();
        } finally {
            this.lock.unlock();
        }

        for (PartitionTracker tracker : partitionTrackers.values()) {
            tracker.close();
        }
    }

    @FFDCIgnore({ WakeupException.class, RejectedExecutionException.class })
    private CompletionStage<PublisherBuilder<Message<V>>> pollKafkaAsync() {
        if (!this.subscribed) {
            this.lock.lock();
            try {
                this.kafkaConsumer.subscribe(this.topics, this);
                this.subscribed = true;
            } finally {
                this.lock.unlock();
            }
        }

        if (!this.running) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Not running, returning incomplete future");
            }
            return new CompletableFuture<>();
        }

        CompletableFuture<PublisherBuilder<Message<V>>> result = new CompletableFuture<>();
        result.handle(KafkaInput::logPollFailure);

        ConsumerRecords<K, V> records = null;

        while (this.lock.tryLock()) {
            try {
                records = this.kafkaConsumer.poll(ZERO);
                break;
            } catch (WakeupException e) {
                // Asked to stop polling, probably means there are pending actions to process
            } finally {
                this.lock.unlock();
            }
            runPendingActions();
        }

        if ((records != null) && !records.isEmpty()) {
            result.complete(wrapInMessageStream(records));
        } else {
            try {
                RMContext context = asyncProvider.captureContext();
                this.asyncProvider.getExecutorService().submit(() -> {
                    context.execute(() -> {
                        executePollActions(result);
                    });
                });
            } catch (RejectedExecutionException e) {
                //by far the most likely reason for this exception is the server is being shutdown
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Asynchronous execution rejected, returning incomplete future");
                }
                return new CompletableFuture<>();
            }
        }

        return result;
    }

    /**
     * Run any pending actions and then poll Kafka for messages.
     */
    @FFDCIgnore(WakeupException.class)
    private void executePollActions(CompletableFuture<PublisherBuilder<Message<V>>> result) {
        this.lock.lock();
        try {
            while (this.running) {
                try {
                    runPendingActions();
                    ConsumerRecords<K, V> asyncRecords = this.kafkaConsumer.poll(FOREVER);
                    result.complete(wrapInMessageStream(asyncRecords));
                    break;
                } catch (WakeupException e) {
                    // We were asked to stop polling, probably means there are pending actions to
                    // process
                } catch (Throwable t) {
                    result.completeExceptionally(t);
                    break;
                }
            }
        } finally {
            this.lock.unlock();
        }
        runPendingActions();
    }

    private PublisherBuilder<Message<V>> wrapInMessageStream(ConsumerRecords<K, V> records) {
        Map<TopicPartition, PartitionTracker> trackers = partitionTrackers;
        return ReactiveStreams.fromIterable(records)
                              .map(r -> {
                                  try {
                                      TopicPartition partition = kafkaAdapterFactory.newTopicPartition(r.topic(), r.partition());
                                      PartitionTracker tracker = trackers.get(partition);
                                      Message<V> message = this.kafkaAdapterFactory.newIncomingKafkaMessage(r,
                                                                                                            () -> {
                                                                                                                unackedMessageCounter.decrement();
                                                                                                                if (fastAck) {
                                                                                                                    tracker.recordDone(r.offset(),
                                                                                                                                       r.leaderEpoch(),
                                                                                                                                       RMContext.NOOP);
                                                                                                                    return CompletableFuture.completedFuture(null);
                                                                                                                } else {
                                                                                                                    RMContext context = asyncProvider.captureContext();
                                                                                                                    CompletionStage<Void> ackResult = tracker.recordDone(r.offset(),
                                                                                                                                                                         r.leaderEpoch(),
                                                                                                                                                                         context);
                                                                                                                    return ackResult;
                                                                                                                }

                                                                                                            },
                                                                                                            (t) -> {
                                                                                                                logNackedMessage(r, t);
                                                                                                                error = t;
                                                                                                                unackedMessageCounter.decrement();
                                                                                                                return CompletableFuture.completedFuture(null);
                                                                                                            });
                                      return new TrackedMessage<>(message, tracker);
                                  } catch (Throwable t) {
                                      Tr.error(tc, "internal.kafka.connector.error.CWMRX1000E", t);
                                      throw t;
                                  }
                              })
                              .filter(m -> !m.tracker.isClosed())
                              .map(m -> m.message);
    }

    private static void logNackedMessage(ConsumerRecord<?, ?> record, Throwable exception) {
        Tr.error(tc, "kafka.input.message.nacked.CWMRX1011E", record, exception);
        FFDCFilter.processException(exception, KafkaInput.class.getName(), "message-nack");
    }

    private static class TrackedMessage<V> {
        private final Message<V> message;
        private final PartitionTracker tracker;

        public TrackedMessage(Message<V> message, PartitionTracker tracker) {
            super();
            this.message = message;
            this.tracker = tracker;
        }

    }

    /**
     * Submit an action to be run using the kafkaConsumer object
     * <p>
     * The action may or may not have run by the time this method returns.
     * <p>
     * This method ensures that the {@link KafkaConsumer} is called in a thread-safe
     * way.
     *
     * @param action
     *            the action to run
     */
    public void runAction(KafkaConsumerAction action) {
        this.tasks.add(action);
        this.kafkaConsumer.wakeup();
        runPendingActions();
    }

    /**
     * Run pending actions if no other threads are running actions or polling the
     * broker
     * <p>
     * This may also be called by external classes to flush any pending actions during
     * a callback.
     */
    public void runPendingActions() {
        while (!this.tasks.isEmpty() && this.lock.tryLock()) {
            try {
                if (!this.running) {
                    break;
                }

                KafkaConsumerAction task = null;
                while ((task = this.tasks.poll()) != null) {
                    try {
                        task.run(this.kafkaConsumer);
                    } catch (Throwable t) {
                        Tr.error(tc, "internal.kafka.connector.error.CWMRX1000E", t);
                        throw t;
                    }
                }
            } finally {
                this.lock.unlock();
            }
        }
    }

    @FunctionalInterface
    public interface KafkaConsumerAction {
        void run(KafkaConsumer<?, ?> consumer);
    }

    /** {@inheritDoc} */
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (newPartitionsPending != null) {
                Tr.debug(this, tc, "onPartitionsRevoked called while new partitions pending:", newPartitionsPending);
            }
        }
        Map<TopicPartition, PartitionTracker> newMap = new HashMap<>(partitionTrackers);
        for (TopicPartition partition : partitions) {
            PartitionTracker tracker = newMap.get(partition);
            if (tracker != null) {
                tracker.close();
                newMap.remove(partition);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Partition revoked that we didn't know we were assigned", partition);
                }
            }
        }
        partitionTrackers = newMap;
    }

    /** {@inheritDoc} */
    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> newPartitions) {

        // If this method ends up throwing a WakeupException and doesn't complete, kafka will call it again but won't pass in the new partitions the second time
        // Therefore we must immediately store the list of new partitions we've been assigned when we receive them the first time
        if (newPartitionsPending != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "onPartitionsAssigned called with pending partitions", newPartitionsPending);
            }
            newPartitionsPending.addAll(newPartitions);
        } else {
            newPartitionsPending = new HashSet<>(newPartitions);
        }

        Map<TopicPartition, PartitionTracker> newMap = new HashMap<>(partitionTrackers);
        for (TopicPartition partition : newPartitionsPending) {
            newMap.put(partition, partitionTrackerFactory.create(this, partition, kafkaConsumer.position(partition)));
        }

        newPartitionsPending = null;
        partitionTrackers = newMap;
    }

}
