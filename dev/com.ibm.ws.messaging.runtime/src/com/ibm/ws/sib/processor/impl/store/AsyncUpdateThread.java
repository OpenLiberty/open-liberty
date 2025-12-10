/*
 * Copyright (c) 2012, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.ws.sib.processor.impl.store;

import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;
import static com.ibm.ws.sib.processor.SIMPConstants.MP_TRACE_GROUP;
import static com.ibm.ws.sib.processor.SIMPConstants.RESOURCE_BUNDLE;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Queue;

import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.exceptions.ClosedException;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class executed operations on the Message Store asynchronously.
 * Operations are encapsulated as AsyncUpdates and enqueued on this object.
 * Periodically, this class will take all the enqueued AsyncUpdates,
 * get a thread, and execute each update on the thread in its own
 * local transaction. The AsyncUpdates are informed if the transaction
 * commits or rolls back.
 * The conditions that trigger the execution of AsyncUpdates are:
 * (1) The previous thread executing AsyncUpdates is finished (either has committed or
 *     rolledback), so this class behaves in a single threaded manner.
 *   AND
 * (2) either (a), (b), or (c) is true
 *    (a) the number of enqueued AsyncUpdates has exceeded the batchThreshold.
 *    (b) an interval greater than maxCommitInterval has elapsed since the start
 *        of the previous transaction.
 *    (c) the AsyncUpdateThread has been closed.
 */
public class AsyncUpdateThread {
    private static final TraceComponent tc =
            SibTr.register(AsyncUpdateThread.class, MP_TRACE_GROUP, RESOURCE_BUNDLE);

    private final SIMPTransactionManager tranManager;
    private final MessageProcessor mp;

    /** AsyncUpdates that have been enqueued */
    private final Queue<AsyncUpdate> enqueuedItems = new LinkedList<AsyncUpdate>();
    private int enqueuedCount = 0;
    private boolean executing = false;
    private boolean executeSinceExpiry = false;
    private CloseCallerInfo closeCallerInfo = null;

    private final int batchThreshold;


    public static AsyncUpdateThread create(MessageProcessor mp, SIMPTransactionManager tm, int batchThreshold, long maxCommitInterval) {
        final AsyncUpdateThread asyncUpdateThread = new AsyncUpdateThread(mp, tm, consideredThreshold(batchThreshold, maxCommitInterval));
        asyncUpdateThread.startTimer(consideredMaxInterval(maxCommitInterval, batchThreshold));
        return asyncUpdateThread;
    }

    private static int consideredThreshold(int threshold, long maxCommitInterval) {
        if (threshold < 1) return 0;
        if (maxCommitInterval > 0L) return threshold;
        SibTr.warning(tc, "IGNORING_BATCH_SIZE_CWSIP0351", new Object[] { threshold, maxCommitInterval });
        return 0;
    }

    private static long consideredMaxInterval(long maxCommitInterval, int threshold) {
        if (maxCommitInterval < 1L) return 0L;
        if (threshold > 0) return maxCommitInterval;
        // As the threshold is at (or below) zero, there is no need (or point) in using the timer, so return zero to disable it.
        return 0L;
    }

    /**
     * Constructor
     * @param mp The MessageProcessor, used to get an execution thread.
     * @param tranManager The transaction manager, to get a local transaction
     * @param batchThreshold When the number of enqueued AsyncUpdates exceeds this threshold,
     *        their execution is scheduled
     */
    private AsyncUpdateThread(MessageProcessor mp, SIMPTransactionManager tranManager, int batchThreshold) {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "AsyncUpdateThread.<init>",
                    new Object[] { mp, tranManager, batchThreshold });
        this.mp = mp;
        this.tranManager = tranManager;
        this.batchThreshold = batchThreshold;

        if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "AsyncUpdateThread.<init>", this);
    }

    private void startTimer(long maxCommitInterval) {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "startTimer", maxCommitInterval);
        try {
            if (0L == maxCommitInterval) return;
            new Timer(maxCommitInterval).schedule();
        } finally {
            if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "startTimer");
        }
    }

    private final class Timer implements AlarmListener {
        private final long maxCommitInterval;

        Timer(long maxCommitInterval) {
            this.maxCommitInterval = maxCommitInterval;
        }

        public void alarm(Object ignored) {
            if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "alarm", mp.getMessagingEngineUuid());

            considerScheduleExecution(false);
            schedule();

            if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "alarm");
        }

        void schedule() {
            if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "schedule", maxCommitInterval);
            mp.getAlarmManager().create(maxCommitInterval, this);
            if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "schedule");
        }
    }

    /**
     * Enqueue an AsyncUpdate
     * @param item the AsyncUpdate
     */
    public void enqueueWork(AsyncUpdate item) throws ClosedException {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "enqueueWork", item);
        if (null == item) {
            if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "enqueueWork", "No work given");
            return;
        }

        synchronized (this) {
            if (isClosed()) {
                ClosedException e = new ClosedException(closeCallerInfo);
                if (tc.isEventEnabled()) SibTr.exception(this, tc, e);
                if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "enqueueWork", "already closed - throwing ClosedException");
                throw e;
            }

            if (isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Enqueueing update: " + item);

            addItem(item);
            if (executing) {
                if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "enqueueWork", "AsyncUpdateThread executing");
                return;
            }

            considerScheduleExecution(true);
        }

        if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "enqueueWork");
    }

    private synchronized void addItem(AsyncUpdate item) {
        enqueuedItems.add(item);
        enqueuedCount++;
    }

    private synchronized Iterable<AsyncUpdate> drainItems() {
        final Queue<AsyncUpdate> copy = new LinkedList<AsyncUpdate>();
        for (AsyncUpdate item = enqueuedItems.poll(); null != item; item = enqueuedItems.poll()) copy.add(item);
        enqueuedCount = enqueuedItems.size();
        return copy;
    }

    private synchronized void considerScheduleExecution(boolean batch) {
        if (isClosed() || executing) return;
        if (!batch && executeSinceExpiry) {
            executeSinceExpiry = false;
            return;
        }
        final int threshold = batch ? batchThreshold : 0;
        if (enqueuedCount <= threshold) return;
        try {
            executing = true;
            mp.startNewSystemThread(new ExecutionThread(threshold));
        } catch (InterruptedException e) {
            FFDCFilter.processException(e,
                    this.getClass().getName() + ".considerScheduleExecution", "1:211:1.30", this);
            SibTr.exception(this, tc, e);
            close(e);
        }
    }

    private synchronized Iterable<AsyncUpdate> getNextBatch() {
        return getNextBatch(batchThreshold);
    }

    private synchronized Iterable<AsyncUpdate> getNextBatch(int batchSize) {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getNextBatch", batchSize);
        final int threshold = isClosed() ? 0 : batchSize;
        Iterable<AsyncUpdate> nextBatch = null;
        try {
            if (enqueuedCount > threshold) {
                nextBatch = drainItems();
            } else {
                executing = false;
                if (0 < batchThreshold) executeSinceExpiry = true;
                notifyAll();
            }
            return nextBatch;
        } finally {
            if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getNextBatch", nextBatch);
        }
    }

    private enum AsyncUpdateProcessing {
        ;

        private static final class CommitFailedException extends Exception {
            private static final long serialVersionUID = 1L;
            CommitFailedException(Throwable t) {
                super("", t);
            }
        }

        static void processItems(Iterable<AsyncUpdate> items, SIMPTransactionManager tranMgr) {
            for (AsyncUpdate item: items) {
                try {
                    processItem(item, tranMgr);
                } catch (Throwable t) {
                    FFDCFilter.processException(
                            t,
                            AsyncUpdateProcessing.class.getName() + ".processItems",
                            "1:250:1.30",
                            AsyncUpdateProcessing.class,
                            new Object[] {items});
                    SibTr.exception(tc, t);
                }
            }
        }

        private static void processItem(AsyncUpdate item, SIMPTransactionManager tranMgr) throws Throwable {
            LocalTransaction tran = tranMgr.createLocalTransaction(false);
            try {
                processItemTran(item, tran);
            } catch (Throwable t) {
                notifyRollbackThenThrow(item, t);
            }
            if (isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Calling committed on " + item);
            item.committed();
        }

        private static void processItemTran(AsyncUpdate item, LocalTransaction tran) throws Throwable {
            try {
                executeThenCommit(item, tran);
            } catch (CommitFailedException cfe) {
                throw cfe.getCause();
            } catch (Throwable t) {
                rollbackThenThrow(tran, t);
            }
        }

        private static void notifyRollbackThenThrow(AsyncUpdate item, Throwable t) throws Throwable {
            try {
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Calling rolledback on " + item);
                item.rolledback(t);
            } catch (Throwable t2) {
                t.addSuppressed(t2);
            }
            throw t;
        }

        private static void rollbackThenThrow(LocalTransaction tran, Throwable t) throws Throwable {
            try {
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Calling rollback on " + tran);
                tran.rollback();
            } catch (Throwable t2) {
                t.addSuppressed(t2);
            }
            throw t;
        }

        private static void executeThenCommit(AsyncUpdate item, LocalTransaction tran) throws Throwable, CommitFailedException {
            if (isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Calling execute on " + item + " with tran " + tran);
            item.execute(tran);
            try {
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Calling commit on " + tran);
                tran.commit();
            } catch (Throwable t) {
                throw new CommitFailedException(t);
            }
        }
    }

    private void processItems(int initialBatchSize) {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "processItems", initialBatchSize);
        for (Iterable<AsyncUpdate> items = getNextBatch(initialBatchSize); null != items; items = getNextBatch()) AsyncUpdateProcessing.processItems(items, tranManager);
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "processItems");
    }

    private class ExecutionThread implements Runnable {
        final int initialBatchSize;
        ExecutionThread(int initialBatchSize) {
            this.initialBatchSize = initialBatchSize;
        }
        public void run() {
            processItems(initialBatchSize);
        }
    }

    private synchronized boolean isClosed() {
        return (null != closeCallerInfo);
    }

    private static final class CloseCallerInfo extends Throwable {
        private static final long serialVersionUID = 1L;
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yy.MM.dd_HH.mm.ss.SSS_Z");

        CloseCallerInfo(Throwable reason) {
            super("Close called at " + DATE_FORMATTER.format(OffsetDateTime.now()), reason);
        }
    }

    /**
     * Close this. Note that committed(),rolledback(),execute() callbacks can occur after this is closed.
     */
    public void close() {
        close(null);
    }

    public void close(Throwable reason) {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "close", reason);
        try {
            synchronized (this) {
                if (isClosed()) return;
                closeCallerInfo = new CloseCallerInfo(reason);
                if (executing) return;
            }
            processItems(0);
        } finally {
            if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "close");
        }
    }

    /**
     * This method blocks till there are 0 enqueued updates and 0 executing updates.
     * Useful for unit testing.
     */
    public void waitTillAllUpdatesExecuted() throws InterruptedException {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "waitTillAllUpdatesExecuted");
        synchronized (this) {
            while (!enqueuedItems.isEmpty() || executing) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // No FFDC code needed
                    if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "waitTillAllUpdatesExecuted", e);
                    throw e;
                }
            }
        }
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "waitTillAllUpdatesExecuted");
    }
}
