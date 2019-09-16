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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Batches up elements
 *
 * @param <T>
 *            the element type
 */
public class Batcher<T> {

    private static final TraceComponent tc = Tr.register(Batcher.class);

    private final int maxBatchSize;
    private final Duration maxBatchTime;
    private final ProcessBatchAction<T> processBatchAction;
    private final ScheduledExecutorService executor;

    private boolean closed;

    private List<T> batchList;
    private Future<?> pendingBatchAction;

    public static <T> BatcherBuilder<T> create(Class<T> type) {
        return new BatcherBuilder<>();
    }

    private Batcher(int maxBatchSize,
                    Duration maxBatchTime,
                    ProcessBatchAction<T> processBatchAction,
                    ScheduledExecutorService executor) {
        this.maxBatchSize = maxBatchSize;
        this.maxBatchTime = maxBatchTime;
        this.processBatchAction = processBatchAction;
        this.executor = executor;
        this.closed = false;

        this.batchList = new ArrayList<>();
        this.pendingBatchAction = null;
    }

    public void addToBatch(T item) {
        List<T> completeBatch = null;
        synchronized (this) {
            if (closed) {
                return;
            }
            this.batchList.add(item);
            if ((this.maxBatchSize != -1) && (this.batchList.size() >= this.maxBatchSize)) {
                completeBatch = startNewBatch();
            } else if ((this.maxBatchTime != null) && (this.pendingBatchAction == null)) {
                this.pendingBatchAction = this.executor.schedule(this::processBatch,
                                                                 this.maxBatchTime.toNanos(),
                                                                 TimeUnit.NANOSECONDS);
            }
        }

        if (completeBatch != null) {
            this.processBatchAction.processBatch(completeBatch);
        }
    }

    public void close() {
        synchronized (this) {
            closed = true;
            if (pendingBatchAction != null) {
                // If we're closing, we don't want to run any remaining actions as the app context may have gone away
                pendingBatchAction.cancel(false);
            }
        }
    }

    private List<T> startNewBatch() {
        synchronized (this) {
            if (!this.batchList.isEmpty()) {
                if (this.pendingBatchAction != null) {
                    this.pendingBatchAction.cancel(false);
                    this.pendingBatchAction = null;
                }
                List<T> oldBatch = this.batchList;
                this.batchList = new ArrayList<>();
                return oldBatch;
            }
            return null;
        }
    }

    private void processBatch() {
        List<T> oldBatch = null;
        synchronized (this) {
            oldBatch = startNewBatch();
        }
        if (oldBatch != null) {
            this.processBatchAction.processBatch(oldBatch);
        }
    }

    @FunctionalInterface
    public interface ProcessBatchAction<T> {
        void processBatch(List<T> items);
    }

    public static class BatcherBuilder<T> {
        private int maxBatchSize = -1;
        private Duration maxBatchTime;
        private ProcessBatchAction<T> processBatchAction;
        private ScheduledExecutorService executor;

        private BatcherBuilder() {
            // No public constructor, use Batcher.create()
        }

        public BatcherBuilder<T> withMaxBatchSize(int size) {
            this.maxBatchSize = size;
            return this;
        }

        public BatcherBuilder<T> withMaxBatchTime(Duration duration) {
            this.maxBatchTime = duration;
            return this;
        }

        public BatcherBuilder<T> withProcessBatchAction(ProcessBatchAction<T> processBatchAction) {
            this.processBatchAction = processBatchAction;
            return this;
        }

        public BatcherBuilder<T> withExecutor(ScheduledExecutorService executor) {
            this.executor = executor;
            return this;
        }

        public Batcher<T> build() {
            if ((this.maxBatchTime != null) && (this.executor == null)) {
                String msg = Tr.formatMessage(tc, "internal.kafka.connector.error.CWMRX1000E", "Executor must be set if maxBatchTime is set");
                throw new IllegalStateException(msg);
            }

            if ((this.maxBatchSize == -1) && (this.maxBatchTime == null)) {
                String msg = Tr.formatMessage(tc, "internal.kafka.connector.error.CWMRX1000E", "Either maxBatchSize or maxBatchTime must be set");
                throw new IllegalStateException(msg);
            }

            if (this.processBatchAction == null) {
                String msg = Tr.formatMessage(tc, "internal.kafka.connector.error.CWMRX1000E", "processBatchAction must be set");
                throw new IllegalStateException(msg);
            }

            return new Batcher<>(this.maxBatchSize, this.maxBatchTime, this.processBatchAction, this.executor);
        }

    }
}