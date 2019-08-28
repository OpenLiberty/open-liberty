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

import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;
import static java.time.Duration.ZERO;
import static org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams.fromIterable;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.ConsumerRecord;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.ConsumerRecords;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaAdapterFactory;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaConsumer;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.OffsetAndMetadata;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.TopicPartition;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.WakeupException;

/**
 * Connects a reactive stream to a Kafka topic
 *
 * @param K
 *            the key type
 * @param V
 *            the value type
 */
public class KafkaInput<K, V> {

    private static final TraceComponent tc = Tr.register(KafkaInput.class);
    private static final Duration FOREVER = Duration.ofMillis(Long.MAX_VALUE);

    private final KafkaConsumer<K, V> kafkaConsumer;
    private final ExecutorService executor;
    private final Collection<String> topics;

    private PublisherBuilder<Message<V>> publisher;
    private boolean subscribed = false;
    private volatile boolean running = true;
    private final ConcurrentLinkedQueue<KafkaConsumerAction> tasks;
    private final AckTracker ackTracker;

    /**
     * Lock to synchronize access to the kafkaConsumer instance
     */
    private final ReentrantLock lock = new ReentrantLock();

    public KafkaInput(KafkaAdapterFactory kafkaAdapterFactory, KafkaConsumer<K, V> kafkaConsumer, ExecutorService executor, String topic, AckTracker ackTracker) {
        super();
        this.kafkaConsumer = kafkaConsumer;
        this.executor = executor;
        this.topics = Collections.singleton(topic);
        this.tasks = new ConcurrentLinkedQueue<>();
        this.ackTracker = ackTracker;
        if (ackTracker != null) {
            this.ackTracker.setCommitAction(this::commitOffsets);
        }
    }

    public PublisherBuilder<Message<V>> getPublisher() {
        if (this.publisher == null) {
            this.publisher = createPublisher();
        }

        return this.publisher;
    }

    private PublisherBuilder<Message<V>> createPublisher() {
        PublisherBuilder<Message<V>> kafkaStream;
        if (ackTracker != null) {
            kafkaStream = ReactiveStreams.generate(() -> 0)
                                         .flatMapCompletionStage(x -> this.ackTracker.waitForAckThreshold().thenCompose(y -> pollKafkaAsync()))
                                         .flatMap(Function.identity())
                                         .map(this::wrapInMessage)
                                         .takeWhile((record) -> this.running);
        } else {
            kafkaStream = ReactiveStreams.generate(() -> 0)
                                         .flatMapCompletionStage(x -> pollKafkaAsync())
                                         .flatMap(Function.identity())
                                         .map(r -> Message.of(r.value()))
                                         .takeWhile((record) -> this.running);
        }
        return kafkaStream;
    }

    private CompletionStage<Void> commitOffsets(TopicPartition partition, OffsetAndMetadata offset) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        try {
            Map<TopicPartition, OffsetAndMetadata> offsets = Collections.singletonMap(partition, offset);
            runAction((c) -> {
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

    private Message<V> wrapInMessage(ConsumerRecord<K, V> record) {
        try {
            return Message.of(record.value(), this.ackTracker.trackRecord(record));
        } catch (Throwable t) {
            Tr.error(tc, "internal.kafka.connector.error.CWMRX1000E", t);
            throw t;
        }
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

        if (ackTracker != null) {
            ackTracker.shutdown();
        }
    }

    @FFDCIgnore(WakeupException.class)
    private CompletionStage<PublisherBuilder<ConsumerRecord<K, V>>> pollKafkaAsync() {
        if (!this.subscribed) {
            this.lock.lock();
            try {
                if (ackTracker != null) {
                    this.kafkaConsumer.subscribe(this.topics, this.ackTracker);
                } else {
                    this.kafkaConsumer.subscribe(this.topics);
                }
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

        CompletableFuture<PublisherBuilder<ConsumerRecord<K, V>>> result = new CompletableFuture<>();
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
            result.complete(fromIterable(records));
        } else {
            this.executor.submit(() -> {
                executePollActions(result);
            });
        }

        return result;
    }

    /**
     * Run any pending actions and then poll Kafka for messages.
     */
    @FFDCIgnore(WakeupException.class)
    private void executePollActions(CompletableFuture<PublisherBuilder<ConsumerRecord<K, V>>> result) {
        this.lock.lock();
        try {
            while (this.running) {
                try {
                    runPendingActions();
                    ConsumerRecords<K, V> asyncRecords = this.kafkaConsumer.poll(FOREVER);
                    result.complete(fromIterable(asyncRecords));
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
     */
    private void runPendingActions() {
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

}
