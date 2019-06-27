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
import java.util.concurrent.TimeUnit;

import javax.enterprise.concurrent.ManagedScheduledExecutorService;

/**
 * Batches up elements
 *
 * @param <T>
 *            the element type
 */
public class Batcher<T> {

    private final int maxBatchSize;
    private final Duration maxBatchTime;
    private final ProcessBatchAction<T> processBatchAction;
    private final ManagedScheduledExecutorService executor;

    private List<T> batchList;
    private Future<?> pendingBatchAction;

    public static <T> BatcherBuilder<T> create(Class<T> type) {
        return new BatcherBuilder<>();
    }

    private Batcher(int maxBatchSize,
                    Duration maxBatchTime,
                    ProcessBatchAction<T> processBatchAction,
                    ManagedScheduledExecutorService executor) {
        this.maxBatchSize = maxBatchSize;
        this.maxBatchTime = maxBatchTime;
        this.processBatchAction = processBatchAction;
        this.executor = executor;

        this.batchList = new ArrayList<>();
        this.pendingBatchAction = null;
    }

    public void addToBatch(T item) {
        List<T> completeBatch = null;
        synchronized (this) {
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
        private ManagedScheduledExecutorService executor;

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

        public BatcherBuilder<T> withExecutor(ManagedScheduledExecutorService executor) {
            this.executor = executor;
            return this;
        }

        public Batcher<T> build() {
            if ((this.maxBatchTime != null) && (this.executor == null)) {
                throw new IllegalStateException("Executor must be set if maxBatchTime is set");
            }

            if ((this.maxBatchSize == -1) && (this.maxBatchTime == null)) {
                throw new IllegalStateException("Either maxBatchSize or maxBatchTime must be set");
            }

            if (this.processBatchAction == null) {
                throw new IllegalStateException("processBatchAction must be set");
            }

            return new Batcher<>(this.maxBatchSize, this.maxBatchTime, this.processBatchAction, this.executor);
        }

    }
}