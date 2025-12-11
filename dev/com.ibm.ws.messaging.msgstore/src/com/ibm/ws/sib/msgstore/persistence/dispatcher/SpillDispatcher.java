/* ==============================================================================
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
 * ==============================================================================
 */
package com.ibm.ws.sib.msgstore.persistence.dispatcher;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.PersistentDataEncodingException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.SeverePersistenceException;
import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.persistence.BatchingContext;
import com.ibm.ws.sib.msgstore.persistence.BatchingContextFactory;
import com.ibm.ws.sib.msgstore.persistence.dispatcher.StateUtils.UpdateCallback;
import com.ibm.ws.sib.msgstore.persistence.impl.Tuple;
import com.ibm.ws.sib.msgstore.task.Task;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.TransactionState;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;

import static com.ibm.ws.sib.msgstore.persistence.dispatcher.DispatcherState.getUpdaterForStopRequested;
import static com.ibm.ws.sib.msgstore.persistence.dispatcher.DispatcherState.updaterForErrorCleared;
import static com.ibm.ws.sib.msgstore.persistence.dispatcher.DispatcherState.updaterForErrorOccurred;
import static com.ibm.ws.sib.msgstore.persistence.dispatcher.DispatcherState.updaterForStart;
import static com.ibm.ws.sib.msgstore.persistence.dispatcher.DispatcherState.updaterForStopped;
import static com.ibm.ws.sib.msgstore.persistence.dispatcher.StateUtils.updateState;

/**
 * Instances of this class orchestrate asynchronous writing of data being
 * spilled to the persistence layer. The purpose of spilling <tt>STORE_MAYBE</tt>
 * data is to use the persistence layer as backing store for item streams
 * which are not being serviced efficiently.<p>
 * 
 * Spilled data is written respecting the order in which it was dispatched,
 * but not the transaction boundaries. Similarly, we do not care about
 * referential integrity (such as orphaned items whose streams have been
 * deleted) because the data is not read after restart. This makes it simple
 * to coordinate multiple concurrent threads in the dispatcher by partitioning
 * the data across the threads by unique id, each with its own dispatch queue
 * to reduce the contention on the queues.<p>
 * 
 * The policy with regard to errors from the persistence layer is as follows.
 * Following an error with a batch, the batch is re-tried one <tt>Task</tt>
 * at a time in case the act of batching was the cause of the failure to
 * process the batch. If an error still occurs, the dispatcher will be unable
 * to accept new work and will start to reject threads trying to give work
 * to the dispatcher.
 */
public class SpillDispatcher extends DispatcherBase
{
    private static TraceComponent tc = SibTr.register(SpillDispatcher.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);

    // Number of bytes required before a batch is dispatched or written.
    private long _minBytesPerBatch;

    // Number of bytes beyond which it is not worth building batches.
    // Tasks beyond this size are written individually. No more tasks are added
    // to a batch once this size is reached.
    private long _maxBytesPerBatch;

    // Maximum tasks written in each batch. Beyond this point, the benefits of
    // batching start to diminish at the risk of creating enormous transactions
    // for the persistence code to handle.
    private int _maxTasksPerBatch;

    // Maximum number of bytes that can be accepted onto each thread's
    // dispatch queue without waiting
    private long _maxDispatchedBytesPerThread;

    // The number of successful writes required to reset the error state
    public int _writesToResetErrorState;

    // Background threads which actually do the database writes
    private int _maxThreads;    

    // The number of threads in existence
    private int _numThreads;

    // The Message Store instance
    private MessageStoreImpl _msi;

    // The background threads which actually do the database writes    
    private Thread[] _threads;
    private SpillDispatcherThread[] _workers;

    // Batching context factory - allows testing of dispatcher independently
    private BatchingContextFactory _bcfactory;

    private final AtomicReference<DispatcherState> stateRef = new AtomicReference<>(new DispatcherState());

    // Defect 560281.1
    // Use an inner class specific to this class for locking.
    private final static class DispatchingLock {}

    /**
     * Constructs a <code>SpillDispatcher</code>.
     */
    public SpillDispatcher(MessageStoreImpl msi, BatchingContextFactory bcfactory)
    {
        this(msi,
             bcfactory,
             obtainIntConfigParameter(msi,
                                      MessageStoreConstants.PROP_JDBC_SPILL_THREADS,
                                      MessageStoreConstants.PROP_JDBC_SPILL_THREADS_DEFAULT, 1, 32)
            );
    }

    /**
     * Constructs a <code>SpillDispatcher</code>.
     */
    public SpillDispatcher(MessageStoreImpl msi, BatchingContextFactory bcfactory, int maxThreads)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[] {msi, bcfactory, Integer.valueOf(maxThreads)});

        _msi = msi;

        _bcfactory = bcfactory;

        // Set the values of all of the configuration parameters
        _maxThreads = maxThreads;
        _minBytesPerBatch = obtainLongConfigParameter(_msi,
                                                      MessageStoreConstants.PROP_JDBC_SPILL_MIN_BYTES_PER_BATCH,
                                                      MessageStoreConstants.PROP_JDBC_SPILL_MIN_BYTES_PER_BATCH_DEFAULT,
                                                      10000L, 100000000L);
        _maxBytesPerBatch = obtainLongConfigParameter(_msi,
                                                      MessageStoreConstants.PROP_JDBC_SPILL_MAX_BYTES_PER_BATCH,
                                                      MessageStoreConstants.PROP_JDBC_SPILL_MAX_BYTES_PER_BATCH_DEFAULT,
                                                      100000L, 100000000L);
        _maxTasksPerBatch = obtainIntConfigParameter(_msi,
                                                     MessageStoreConstants.PROP_JDBC_SPILL_MAX_TASKS_PER_BATCH,
                                                     MessageStoreConstants.PROP_JDBC_SPILL_MAX_TASKS_PER_BATCH_DEFAULT,
                                                     1, 10000);
        _maxDispatchedBytesPerThread = obtainLongConfigParameter(_msi,
                                                                 MessageStoreConstants.PROP_JDBC_SPILL_MAX_DISPATCHED_BYTES_PER_THREAD,
                                                                 MessageStoreConstants.PROP_JDBC_SPILL_MAX_DISPATCHED_BYTES_PER_THREAD_DEFAULT,
                                                                 100000L, 1000000000L);

        if (_minBytesPerBatch > _maxDispatchedBytesPerThread)
        {
            _minBytesPerBatch = _maxDispatchedBytesPerThread;
        }

        if (_minBytesPerBatch > _maxBytesPerBatch)
        {
            _minBytesPerBatch = _maxBytesPerBatch;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
    }

    /**
     * Used as a quick way to check the health of a dispatcher before giving it work
     * in situations in which the work cannot be rejected. For example, for a transaction
     * which requires both synchronous and asynchronous persistence, once we've done the
     * synchronous persistence, a transient persistence problem from a dispatcher will
     * not be reported from the dispatching method because we cannot guarantee to roll back the
     * synchronous work.
     * 
     * @return <tt>true</tt> if the dispatcher is experiencing no problems, else <tt>false</tt>
     */
    public synchronized boolean isHealthy()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isHealthy");

        boolean retval = stateRef.get().isHealthy();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isHealthy", Boolean.valueOf(retval));
        return retval;
    }

    // SIB0112d.ms.2

    /**
     * Dispatches a list of <tt>Tasks</tt> for asynchronous persistence.
     * The <tt>Tasks</tt> are added to a queue processed by a background
     * thread. In order to ensure that the thread does not become swamped
     * by more work than it can handle, this method may wait a while before
     * returning.<p>
     *
     * Each Persistable is called back on
     * {@link com.ibm.ws.sib.msgstore.persistence.impl.Tuple#persistableOperationBegun()}.
     * 
     * @param tasks The collection of <tt>Tasks</tt> to dispatch
     * @param tran  The transaction associated with the <tt>Tasks</tt>. Ignored by this dispatcher.
     * @param canReject <tt>true</tt> if the call can be rejected, <tt>false</tt> otherwise.
     * The SpillDispatcher never rejects work, although it does discard it when it is told to stop.
     * 
     * @throws PersistenceException dispatch rejected due to an ailing dispatcher
     * @throws SevereMessageStoreException 
     */
    public void dispatch(Collection tasks, PersistentTransaction tran, boolean canReject) throws PersistenceException, SevereMessageStoreException
    {
        // tran is not traced because it is ignored
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dispatch", new Object[] {tasks, Boolean.valueOf(canReject)});

        int taskCount = 0;

        long dispatchTime = System.currentTimeMillis();

        if (tasks != null)
        {
            boolean mustWait = false;

            taskCount = tasks.size();

            // We need to provide an object upon which we will wait for
            // notification that our work has been dispatched.
            DispatchNotifier waitHere = new DispatchNotifier(taskCount, canReject);

            // Array of flags for threads which need to be notified
            boolean[] mustNotifyThreads = new boolean[_maxThreads];

            // Array of flags for threads for which we need to wait for dispatch
            boolean[] mustWaitThreads = new boolean[_maxThreads];

            // Add the tasks to the appropriate queues
            Iterator it = tasks.iterator();
            while (it.hasNext())
            {
                Task task = (Task)it.next();

                // Ensure that the task is a standalone logical entity so the
                // data written is transactionally clean (i.e. committed)
                task.copyDataIfVulnerable();

                // Ensure that the persistable is not discarded prematurely
                ((Tuple)task.getPersistable()).persistableOperationBegun();

                // Tasks are distributed amongst the various threads' queues
                int threadnum = (int)(task.getPersistable().getUniqueId() % _maxThreads);

                // Dispatch to a thread
                mustWaitThreads[threadnum] = _workers[threadnum].addTask(task, dispatchTime, waitHere);
                mustNotifyThreads[threadnum] = true;
            } // end while

            // Notify the threads affected by the dispatch request that there's
            // work to do            
            for (int i = 0; i < _maxThreads; i++)
            {
                if (mustNotifyThreads[i])
                {
                    _workers[i].notifyDispatchArrived();
                }

                mustWait |= mustWaitThreads[i];
            }

            // Wait for our work to be dispatched
            if (mustWait)
            {
                try
                {
                    waitHere.waitForDispatch();
                }
                catch (PersistenceException pe)
                {
                    // Since the SpillDispatcher never rejects work, this case should not occur
                	com.ibm.ws.ffdc.FFDCFilter.processException(pe, "com.ibm.ws.sib.msgstore.persistence.impl.SpillDispatcher.writeBatch", "1:326:1.48", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception persisting batch", pe);
                } // end try-catch
            }
        }


        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dispatch");
    }

    public void start()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "start");

        int priorityDelta = obtainIntConfigParameter(_msi,
                                                     MessageStoreConstants.PROP_JDBC_SPILL_THREAD_PRIORITY_DELTA,
                                                     MessageStoreConstants.PROP_JDBC_SPILL_THREAD_PRIORITY_DELTA_DEFAULT,
                                                     Thread.MIN_PRIORITY - Thread.NORM_PRIORITY,
                                                     Thread.MAX_PRIORITY - Thread.NORM_PRIORITY);

        _threads = new Thread[_maxThreads];
        _workers = new SpillDispatcherThread[_maxThreads];

        synchronized(this)
        {
            updateState(stateRef, updaterForStart);
        }

        // Get the ME_UUID so that we can tell which ME our
        // threads belong to.
        String meUUID = "";
        if (_msi != null)
        {
            JsMessagingEngine me = _msi._getMessagingEngine();
            if (me != null)
            {
                meUUID = me.getUuid().toString()+"-";
            }
        }

        for (int i = 0; i < _maxThreads; i++)
        {
            String threadName = "sib.SpillDispatcher-"+meUUID+i;
            _workers[i] = new SpillDispatcherThread(i, threadName);
            _threads[i] = new Thread(_workers[i], threadName);
            _threads[i].setDaemon(true);
            _threads[i].setPriority(Thread.NORM_PRIORITY + priorityDelta);
            _threads[i].start();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
    }

    public void stop(int mode, Throwable reason)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "stop", Integer.valueOf(mode));

        boolean performingTheStop;

        // First change the state of the dispatcher
        synchronized(this)
        {
            performingTheStop = updateState(stateRef, getUpdaterForStopRequested(reason));
        } // end synchronized


        // Are we performing the stop or is it stopping already?
        if (performingTheStop)
        {
            for (int i = 0; i < _maxThreads; i++)
            {
                _workers[i].cleanup();
            }

            // Now wait for the dispatcher threads to stop.
            // They should stop quickly unless they're stuck inside JDBC so
            // we only allow 60 seconds in total to join all of the threads
            long limitStopMillis = System.currentTimeMillis() + 60000;

            for (int i = 0; i < _maxThreads; i++)
            {
                long remainingStopDelay = limitStopMillis - System.currentTimeMillis();
                if (remainingStopDelay <= 0)
                {
                    remainingStopDelay = 1;
                }

                try
                {
                    if (_threads[i] != null)
                    {
                        _threads[i].join(remainingStopDelay);
                        if (_threads[i].isAlive())
                        {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Cannot join dispatcher thread " + _workers[i]);
                        }
                    }
                }
                catch (InterruptedException ie)
                {
                    //No FFDC Code Needed.
                }
            }
        } // end if (performingTheStop)


        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "stop");
    }

    public synchronized void threadWriteErrorOccurred(int threadNum)
    {
        final UpdateCallback<DispatcherState> callback = (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) ?
                new UpdateCallback<DispatcherState>() {
            public void updated(DispatcherState newState) {
                SibTr.event(SpillDispatcher.this, tc, String.format("threadWriteErrorOccurred: threadNum = %d, new writeErrorCount = %d",
                        threadNum, newState.threadWriteErrors));
            }
        } : null;

        updateState(stateRef, updaterForErrorOccurred, callback);
    }

    public synchronized void threadWriteErrorCleared(int threadNum)
    {
        final UpdateCallback<DispatcherState> callback = (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) ?
                new UpdateCallback<DispatcherState>() {
            public void updated(DispatcherState newState) {
                SibTr.event(SpillDispatcher.this, tc, String.format("threadWriteErrorCleared: threadNum = %d, new writeErrorCount = %d",
                        threadNum, newState.threadWriteErrors));
            }
        } : null;
        updateState(stateRef, updaterForErrorCleared, callback);
    }

    // SIB0112d.ms.2

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    public String toString()
    {
        return super.toString() + stateRef.get().desc();
    }

    /**
     * A worker thread for the SpillDispatcher.
     */
    private class SpillDispatcherThread implements Runnable
    {
        // Number of this thread
        private int _threadNum;

        // The name of this thread
        private String _threadName;

        // Queue of tasks waiting to be added to the dispatch queue - size is unbounded
        // but if the dispatching queue is full and a thread adds to this queue, it
        // must wait to be notified that its dispatch was accepted
        private LinkedList _waitingQueue;

        // Defect 496154
        // Queue of add tasks being dispatched - size is bounded
        // These tasks can be cancelled if a matching remove is dispatched
        // before they are written to disk. To minimize the size of the 
        // list traversed at cancellation time the adds and updates are 
        // stored in seperate lists to the removes.
        private LinkedList _dispatchAddingQueue;

        // Defect 496154
        // Queue of update tasks being dispatched - size is bounded
        // These tasks can be cancelled if a matching remove is dispatched
        // before they are written to disk. To minimize the size of the 
        // list traversed at cancellation time the updates are stored
        // in a seperate list to the adds. This allows us to stop the 
        // search of the add list if we find a match. Previously we would
        // have had to search to the end of the list in order to find 
        // (possibly) numerous updates that match.
        private LinkedList _dispatchUpdateQueue;

        // Defect 496154
        // Queue of remove tasks being dispatched - size is bounded
        // This list will store any remove tasks that cannot be cancelled
        // at dispatch time. Keeping them seperate should reduce the size 
        // of the list that is traversed at cancellation time and will
        // also allow us to prioritise the writing of un-cancellable 
        // removes to disk which should give the add/update queue more 
        // chance to be trimmed via cancellation.
        private LinkedList _dispatchRemoveQueue;

        // Object used for synchronization purposes during dispatching
        private final DispatchingLock _dispatchingLock = new DispatchingLock();

        // Number of bytes associated with the tasks on the dispatch queue
        private long _dispatchedBytes;

        // Flag set on receipt of a write error from the persistence layer
        private boolean _writeErrorOccurred = false;

        // SIB0112d.ms.2

        // Number of tasks written successfully since the last error
        private int _goodWritesSinceLastError = 0;

        // Number of consecutive write errors
        private int _consecutiveWriteErrors = 0;

        // Indicates whether this thread is contributing to the thread write error
        // count for the dispatcher. A single write error does not contribute to
        // the count. A second consuective write error does. This in turn causes
        // work to be rejected by the dispatcher
        private boolean _isContributingToThreadWriteErrors = false;

        // SIB0112d.ms.2

        // The number of milliseconds to wait before retrying after a write error
        private long _writeErrorRetryDelay;

        // Indicates whether the thread cen be safely interrupted
        private boolean _interruptible = false;

        // Whether the thread is actively working    
        private boolean _threadActive;

        // Whether there is an unanswered notify() outstanding. If so, don't trigger another
        private boolean _notifyOutstanding = false;


        /**
         * Constructs a new SpillDispatcherThread.
         * @param threadNum the number of the thread being constructed
         */
        SpillDispatcherThread(int threadNum, String threadName)
        {
            _threadNum = threadNum;
            _threadName = threadName;
            _numThreads++;
            _waitingQueue = new LinkedList();
            _threadActive = true;

            // Defect 496154
            _dispatchAddingQueue = new LinkedList();
            _dispatchUpdateQueue = new LinkedList();
            _dispatchRemoveQueue = new LinkedList();
        }


        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run()
        {
            // Begin the ME's scope for trace on this thread
            if ((_msi != null) && (_msi._getMessagingEngine() != null)) SibTr.push(_msi._getMessagingEngine());

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "run");

            LinkedList batch = null;
            boolean stopNow = false;

            // Continue looping until the Message Store is stopped
            try
            {
                while (!stopNow)
                {
                    boolean dataToWrite = false;

                    // Changed due to FINDBUGS to make
                    // changes/checks of _stopRequested
                    // consistently synchronized
                    synchronized(SpillDispatcher.this)
                    {
                        stopNow = stateRef.get().isStopRequested;
                    }

                    while (!dataToWrite && !stopNow)
                    {
                        // Synchronize on the dispatching lock so we get a clear picture of the
                        // work to be performed
                        synchronized(_dispatchingLock)
                        {
                            // Promote any eligible dispatch units from the waiting queue to the
                            // dispatch queue to give the maximum scope for batching
                            promoteWaiters();

                            // We need to start building a batch right now
                            batch = buildBatch();
                            dataToWrite = !batch.isEmpty();

                            if (!dataToWrite)
                            {
                                _threadActive = false;

                                try
                                {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Spill dispatcher started indefinite wait", this);

                                    _dispatchingLock.wait(0);

                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Spill dispatcher completed wait", this);
                                }
                                catch (InterruptedException ie)
                                {
                                    //No FFDC Code Needed.
                                }
                                finally
                                {
                                    _threadActive = true;
                                    _notifyOutstanding = false;
                                }
                            }
                        } // end synchronized

                        // Changed due to FINDBUGS to make
                        // changes/checks of _stopRequested
                        // consistently synchronized
                        synchronized(SpillDispatcher.this)
                        {
                            stopNow = stateRef.get().isStopRequested;
                        }
                    } // end while (!dataToWrite && !stopNow)


                    // If we have had a write error and multiple consecutive write errors,
                    // we must wait to ensure that we don't go into a busy retry loop.
                    if (!stopNow && _writeErrorOccurred && (_consecutiveWriteErrors > 1))
                    {
                        synchronized(_dispatchingLock)
                        {
                            _interruptible = true;
                        }

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Dispatcher started retry wait of " + _writeErrorRetryDelay + " ms", this);

                        try
                        {
                            Thread.sleep(_writeErrorRetryDelay);

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Dispatcher completed wait", this);
                        }
                        catch (InterruptedException ie)
                        {
                            //No FFDC Code Needed.

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Dispatcher interrupted during wait", this);
                        }
                        finally
                        {
                            synchronized(_dispatchingLock)
                            {
                                _interruptible = false;
                            }
                        }
                    }


                    // If anything is due to be written, do it now
                    if (!stopNow && dataToWrite)
                    {
                        boolean normalCompletion = false;

                        try
                        {
                            if (writeBatch(batch))
                            {
                                // Defect 496154
                                // Confirm the completion of the writing back to the Persistables
                                batchCompleted(batch);

                                dataToWrite = false;

                                // If we've recovered from the write errors, reset the
                                // error status
                                if (_writeErrorOccurred)
                                {
                                    _consecutiveWriteErrors = 0;
                                    _goodWritesSinceLastError++;

                                    if (_goodWritesSinceLastError > _writesToResetErrorState)
                                    {
                                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Good write threshold passed. Re-enabling batch mode.");

                                        _writeErrorOccurred = false;

                                        if (_isContributingToThreadWriteErrors)
                                        {
                                            _isContributingToThreadWriteErrors = false;
                                            threadWriteErrorCleared(_threadNum);
                                        }

                                        // SIB0112d.ms.2
                                    }
                                }
                            }

                            normalCompletion = true;
                        }
                        finally
                        {
                            // Do we have cleanup to do? This is in a finally block to ensure that
                            // exceptions release waiting threads
                            if (dataToWrite)
                            {
                                // Defect 496154
                                // Confirm the failure of the writing back to the Persistables
                                int numOfCancelled = batchCancelled(batch);

                                // Handle the write error. Following a write error,
                                // the batch size is reduced to one in case excessive
                                // batching was the cause of the error
                                handleWriteError(!normalCompletion, batch.size() - numOfCancelled);

                                // SIB0112d.ms.2
                            }
                        }
                    } // end if (dataToWrite)
                } // end while (!stopNow)
            }
            catch (Throwable t)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.sib.msgstore.persistence.dispatcher.SpillDispatcherThread.run", "1:769:1.48", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unexpected exception caught in SpillDispatcher thread!", t);
            }
            finally
            {
                synchronized(SpillDispatcher.this)
                {
                    _numThreads--;

                    // Defect 258476 - reordered logic slightly for better state management

                    // Was this a planned termination?
                    if (false == stateRef.get().isStopRequested)
                    {
                        // This is a thread termination which occurred for some
                        // reason other than a stop request. Bounce the server
                        if (_msi != null)
                        {
                            _msi.reportLocalError();
                        }
                    }

                    // If we're last out, turn off the lights
                    if (_numThreads == 0)
                    {
                        updateState(stateRef, updaterForStopped);
                    }
                } // end synchronized(SpillDispatcher.this)
            } // end try-finally

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "run");

            // End the ME's scope for trace on this thread            
            if ((_msi != null) && (_msi._getMessagingEngine() != null)) SibTr.pop();
        }


        /**
         * Notifies all waiting threads without writing their data.
         * This draconian act is done during stop of the dispatcher
         * since this is only spill after all and we needn't write
         * any more data.
         */
        private void cleanup()
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "cleanup");

            synchronized(_dispatchingLock)        
            {
                // Run down the list of dispatch queue elements, notifying those
                // which have waiting dispatching threads
                Iterator it = _waitingQueue.iterator();
                while (it.hasNext())
                {
                    QueueElement qe = (QueueElement)it.next();
                    qe.notifyDispatch();
                }

                // Now throw them all away so they are not inadvertently dispatched
                _waitingQueue.clear();

                // If the thread is interrupible and thus perhaps in a sleep from
                // which it is safe to wake it, interrupt it now
                if (isInterruptible())
                {
                    _threads[_threadNum].interrupt();
                }

                // Now wake up the dispatcher thread to give it a chance to see that
                // we're stopping
                _dispatchingLock.notify();
            } // end synchronized

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "cleanup");
        }


        /**
         * Adds a task to the waiting queue.
         * 
         * @param newTask
         * @param dispatchTime
         * @param dn
         * 
         * @return whether the caller must wait for dispatch of this task
         * @throws SevereMessageStoreException 
         */
        private boolean addTask(Task newTask, long dispatchTime, DispatchNotifier dn) throws SevereMessageStoreException
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "addTask", new Object[] {newTask, Long.valueOf(dispatchTime), dn});

            boolean callerMustWait = false;

            QueueElement newElement = new QueueElement(newTask, dn);

            int bytesForTask = newElement.getDataSize();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "bytesForTask=" + bytesForTask);

            int skipCount = 0;
            int skipBytes = 0;

            //
            //
            // 1. We only attempt cancellation on DELETES. An add cannot 
            //    by definition trigger any cancellation and updates would
            //    require an update of the data in the existing add before
            //    the add was written which could be problematic.
            // 
            // 2. Ordering of eventual writes to disk are as follows:
            // 
            //    a) Un-cancelled DELETES - These deletes already have an add
            //                              that has made it to disk and so 
            //                              need to be deleted. Doing so first 
            //                              gives any adds/updates more time
            //                              to be cancelled before making it to disk.
            //    b) Adds    - Need to be left as long as possible to allow 
            //                 cancellation but need to be written before any
            //                 matching updates.
            //    c) Updates - If written then must be written last to ensure
            //                 they have an add on disk to update.
            // 
            // 3. If an add has made it to disk we CAN still cancel any
            //    outstanding updates before writing the deletes to disk as 
            //    if we are cancelling then we are already in the process of
            //    deleting the item from the in-memory cache. In fact due to
            //    the ordering stated in 2. above we MUST cancel any updates
            //    associated with a delete else they would be written after the
            //    delete was and would therefore have no row/object to update.
            // 
            synchronized(_dispatchingLock)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                {
                    SibTr.debug(this, tc, "_dispatchAddingQueue.size()=" + _dispatchAddingQueue.size());
                    SibTr.debug(this, tc, "_dispatchUpdateQueue.size()=" + _dispatchUpdateQueue.size());
                    SibTr.debug(this, tc, "_dispatchRemoveQueue.size()=" + _dispatchRemoveQueue.size());
                    SibTr.debug(this, tc, "_waitingQueue.size()=" + _waitingQueue.size());
                    SibTr.debug(this, tc, "_dispatchedBytes=" + _dispatchedBytes);
                    SibTr.debug(this, tc, "_maxDispatchedBytesPerThread=" + _maxDispatchedBytesPerThread);
                }

                boolean cancelled = false;

                // Defect 496154
                // If the new task is NOT a delete then we won't do any cancellation.
                // If the new task IS a delete then we can try to cancel it if a
                // matching add (and any updates) can be found.
                if (!newElement.isDelete())
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "New task is NOT a delete. SKIPPING cancellation.");
                }
                else
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "New task IS a delete. ATTEMPTING cancellation.");

                    // Only if we find a matching add that hasn't yet been
                    // batched/made it to disk can we cancel this delete.
                    boolean foundMatchingAdd = false;

                    // Defect 496154
                    // Do we have any outstanding persistable operations
                    // on our new task? If so we can try and cancel them
                    // if not we can stop looking.
                    boolean outstandingOperationsToCancel = true;

                    // Are there any other operations outstanding on our
                    // new task?
                    if (((Tuple)newTask.getPersistable()).persistableOperationsOutstanding() == 1)
                    {
                        // The only operation outstanding on this task is
                        // the delete we are currently trying to cancel.
                        outstandingOperationsToCancel = false;

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "No outstanding operations. STOPPING cancellation.");
                    }

                    // Do we need to check for an add to cancel? Only neccessary if 
                    // this task has not got a representation on disk yet.
                    if (outstandingOperationsToCancel &&                                          // We have operations to cancel
                        !((Tuple)newTask.getPersistable()).persistableRepresentationWasCreated()) // AND The add for this task hasn't made it to disk yet.
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Searching the ADDING queue.");
    
                        boolean addAlreadyBatched = false;
    
                        Iterator it = _dispatchAddingQueue.iterator();
    
                        while (!addAlreadyBatched &&  // A matching add is not already in a batch
                               !foundMatchingAdd &&   // AND we haven't matched an add in this search
                               it.hasNext())          // AND we have more adds to check
                        {
                            QueueElement  currentElement = (QueueElement)it.next();
                            Task             currentTask = currentElement.getTask();
                            AbstractItemLink currentLink = currentTask.getLink();
    
                            // SIB0112d.ms.2
                            // If this element is in a batch then it is in the process of being written
                            // to disk and so cannot be cancelled out at this point in time.
                            if (!currentElement.isBatched())
                            {
                                // Defect 463614
                                // To make sure we avoid cancelling a task just as it is being added 
                                // to a batch for persisting we need to double check the isBatched()
                                // flag inside a synchronized block.
                                synchronized(currentElement)
                                {
                                    if (!currentElement.isBatched())
                                    {
                                        if ((currentLink != null) &&                                                      // Task does not have a null link
                                            currentLink == newTask.getLink() &&                                           // AND Task's link matches that of the one being added
                                            currentLink.isRemoving() &&                                                   // AND Task's Item is in the middle of being removed
                                            !((Tuple)currentTask.getPersistable()).persistableRepresentationWasCreated()) // AND The create has not yet made it to disk
                                        {
                                            // Remove the task from the queue
                                            it.remove();
    
                                            // Decrement the dispatchedBytes count
                                            int currentBytes = currentElement.getDataSize();
                                            _dispatchedBytes -= currentBytes;
    
                                            // Inform the task that it is in a consistent state
                                            ((Tuple)currentTask.getPersistable()).persistableOperationCancelled();
    
                                            // Are there any other operations outstanding on our
                                            // new task?
                                            if (((Tuple)newTask.getPersistable()).persistableOperationsOutstanding() == 1)
                                            {
                                                // The only operation outstanding on this task is
                                                // the delete we are currently trying to cancel.
                                                outstandingOperationsToCancel = false;
                                            }
    
                                            // Cancel the element so that it cannot be included
                                            // in a new batch being built.
                                            currentElement.setCancelled();
    
                                            // Update flag to track that we have found the matching
                                            // add for our delete and can stop searching.
                                            foundMatchingAdd = true;
    
                                            // Update PMI data.
                                            skipCount++;
                                            skipBytes += currentBytes;
    
                                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Task matched and removed from ADDING queue: New Task: "+newTask+", Cancelled Task: "+currentTask);
                                        }
                                    }
                                    else
                                    {
                                        // If the element is batched then we need to check if it matches our
                                        // new task. If it does then we can no longer cancel this delete but
                                        // we can still remove any matching updates.
                                        if (newTask.getLink() == currentLink)
                                        {
                                            addAlreadyBatched = true;
                                        }
                                    }
                                } // end synchronized
                            }
                            else // currentElement.isBatched() == true
                            {
                                // If the element is batched then we need to check if it matches our
                                // new task. If it does then we can no longer cancel this delete but
                                // we can still remove any matching updates.
                                if (newTask.getLink() == currentLink)
                                {
                                    addAlreadyBatched = true;
                                }
                            }
                        } // end while (!addAlreadyBatched && !foundMatchingAdd)
                    } // end if (outstandingOperationToCancel && !PersistentRepresentationCreated)
    
    
                    // For the updates we need to check if we have found a
                    // matching add or not: 
                    // 
                    //   1. If we have found an add then we need to cancel any updates
                    //      as they will have no data on disk to update. 
                    // 
                    //   2. If we have not found an add then it has made it to disk
                    //      but doing an update then a delete is pointless so we can
                    //      cancel the update and keep the delete.
                    if (outstandingOperationsToCancel &&  // We still have more operations to cancel for this task
                        !_dispatchUpdateQueue.isEmpty())  // AND the dispatch queue for updates is not empty
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Searching the UPDATE queue.");
    
                        Iterator it = _dispatchUpdateQueue.iterator();
    
                        while (outstandingOperationsToCancel &&  // We still have some cancellation to do
                               it.hasNext())                     // AND we have more of this queue to check
                        {
                            QueueElement  currentElement = (QueueElement)it.next();
                            Task             currentTask = currentElement.getTask();
                            AbstractItemLink currentLink = currentTask.getLink();
    
                            // SIB0112d.ms.2
                            // If this element is in a batch then it is in the process of being written
                            // to disk and so cannot be cancelled out at this point in time.
                            if (!currentElement.isBatched())
                            {
                                // Defect 463614
                                // To make sure we avoid cancelling a task just as it is being added 
                                // to a batch for persisting we need to double check the isBatched()
                                // flag inside a synchronized block.
                                synchronized(currentElement)
                                {
                                    if (!currentElement.isBatched())
                                    {
                                        // Defect 530772
                                        // Need to remove updates even if the add has 
                                        // made it to disk.
                                        if ((currentLink != null) &&            // Task does not have a null link
                                            currentLink == newTask.getLink() && // AND Task's link matches that of the one being added
                                            currentLink.isRemoving())           // AND Task's Item is in the middle of being removed
                                        {
                                            // Remove the task from the queue
                                            it.remove();
    
                                            // Decrement the dispatchedBytes count
                                            int currentBytes = currentElement.getDataSize();
                                            _dispatchedBytes -= currentBytes;
    
                                            // Inform the task that it is in a consistent state
                                            ((Tuple)currentTask.getPersistable()).persistableOperationCancelled();
    
                                            if (((Tuple)newTask.getPersistable()).persistableOperationsOutstanding() == 1)
                                            {
                                                // The only operation outstanding on this task is
                                                // the delete we are currently trying to cancel.
                                                outstandingOperationsToCancel = false;
                                            }
    
                                            // Cancel the element so that it cannot be included
                                            // in a new batch being built.
                                            currentElement.setCancelled();
    
                                            // Update PMI data.
                                            skipCount++;
                                            skipBytes += currentBytes;
    
                                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Task matched and removed from UPDATE queue: New Task: "+newTask+", Cancelled Task: "+currentTask);
                                        }
                                    }
                                    else
                                    {
                                        // If the element is batched then we need to check if 
                                        // it matches our new task. 
                                        if (newTask.getLink() == currentLink)
                                        {
                                            if (foundMatchingAdd)
                                            {
                                                // We have cancelled an add but an update is now batched.
                                                // This should NEVER happen as we check updates at buildBatch()
                                                // time to see if they are in removing state. If they are then 
                                                // that means cancellation could be under way and the update 
                                                // is not batched. If it is not in removing state then the add
                                                // cannot have been cancelled and will either be in the same 
                                                // batch as the update or will have already made it to disk.
                                                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Update task batched after cancellation of add!");
                                                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exit(this, tc, "addTask");
                                                throw new SevereMessageStoreException("Update task batched after cancellation of add!");
                                            }
                                        }
                                    }
                                } // end synchronized
                            }
                            else // currentElement.isBatched() == true
                            {
                                // If the element is batched then we need to check if 
                                // it matches our new task. 
                                if (newTask.getLink() == currentLink)
                                {
                                    if (foundMatchingAdd)
                                    {
                                        // We have cancelled an add but an update is now batched.
                                        // This should NEVER happen as we check updates at buildBatch()
                                        // time to see if they are in removing state. If they are then 
                                        // that means cancellation could be under way and the update 
                                        // is not batched. If it is not in removing state then the add
                                        // cannot have been cancelled and will either be in the same 
                                        // batch as the update or will have already made it to disk.
                                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Update task batched after cancellation of add!");
                                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exit(this, tc, "addTask");
                                        throw new SevereMessageStoreException("Update task batched after cancellation of add!");
                                    }
                                }
                            }
                        } // while (outstandingOperationsToCancel && it.hasNext())
                    } // end if (outstandingOperationsToCancel && !_dispatchUpdateQueue.isEmpty())
    
                    // If we still have outstanding operations to cancel then we
                    // finally need to search the waiting queue.
                    if (outstandingOperationsToCancel &&  // We still have more operations to cancel for this task 
                        !_waitingQueue.isEmpty())         // AND the waiting queue is not empty       
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Searching the WAITING queue.");
    
                        Iterator it = _waitingQueue.iterator();
    
                        while (outstandingOperationsToCancel &&  // We still have some cancellation to do   
                               it.hasNext())                     // AND we have more of this queue to check 
                        {
                            // SIB0112d.ms.2
                            // No elements in the waiting queue should be batched so we don't 
                            // need to check here.
                            QueueElement  currentElement = (QueueElement)it.next();
                            Task             currentTask = currentElement.getTask();
                            AbstractItemLink currentLink = currentTask.getLink();
    
                            // Defect 530772
                            // Need to remove updates even if the add has 
                            // made it to disk.
                            if ((currentLink != null) &&                             // Task does not have a null link
                                currentLink == newTask.getLink() &&                  // AND Task's link matches that of the one being added
                                (currentLink.isRemoving() &&                         // AND (Task's Item is in the middle of being removed
                                 !currentTask.isDeleteOfPersistentRepresentation())) //      AND This task is not a delete. This should catch adds and updates)
                            {
                                // Remove the task from the queue
                                it.remove();
    
                                // Decrement the dispatchedBytes count
                                int currentBytes = currentElement.getDataSize();
                                _dispatchedBytes -= currentBytes;
    
                                // Inform the task that it is in a consistent state
                                ((Tuple)currentTask.getPersistable()).persistableOperationCancelled();
    
                                if (((Tuple)newTask.getPersistable()).persistableOperationsOutstanding() == 1)
                                {
                                    // The only operation outstanding on this task is
                                    // the delete we are currently trying to cancel.
                                    outstandingOperationsToCancel = false;
                                }
    
                                // Promotion causes a notification so since we're avoiding this, we need
                                // to notify the dispatch here
                                currentElement.notifyDispatch();
    
                                // If we have found our matching add then update the flag 
                                // to allow cancellation.
                                if (currentElement.isAdd())
                                {
                                    foundMatchingAdd = true;
                                }
    
                                // Update PMI data.
                                skipCount++;
                                skipBytes += currentBytes;
    
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Task matched and removed from WAITING queue: New Task: "+newTask+", Cancelled Task: "+currentTask);
                            }
                        }
                    } // end if (outstandingOperationsToCancel && !_waitingQueue.isEmpty())

                    // If we found a matching add we don't need to dispatch 
                    // this delete so set the cancelled flag.
                    cancelled = foundMatchingAdd;
                } // end if (newElement.isDelete)


                // SIB0112d.ms.2
                // If we didn't manage to cancel out the new work then we 
                // need to add it to the correct dispatch queue.
                if (!cancelled)
                {
                    // Defect 567591
                    // If this task is going to blow the batch limit then it will have 
                    // two affects on this thread:
                    // 
                    // 1) Dispatching will trigger batches to be built flushing all
                    //    possibly smaller tasks out of this threads dispatch queues
                    //    until this task can be batched
                    // 2) This task will then be batched on its own to avoid too big 
                    //    a batch
                    // 
                    // Part 1 has the unfortunate side affect of flushing lots of small
                    // tasks out of memory before they have the chance to be cancelled 
                    // when all we really want is to get the large task to disk and out of 
                    // memory. 
                    // 
                    // To avoid this we will check here to see if the task is likely to blow 
                    // the batch size limit and if so prepend it to the start of the list.
                    // This will have the affect of getting the large task batched and
                    // written straight away and return us to the queue size state we were 
                    // in before it was dispatched. The existing smaller tasks then still 
                    // have a chance to be cancelled before they are written to disk.
                    // 
                    // This does mean that large tasks will be dropped out of memory quicker
                    // than small ones thus making them inherently more costly in disk use.
                    // We do however have a tuning option where we can change the value of 
                    // _maxBytesPerBatch to control this crossover point in an easily
                    // understood way.
                    // 
                    // NOTE: This new approach will still mean uncancelled deletes get flushed
                    //       out to disk before our large task if it is an ADD. This is not 
                    //       too bad as they were always going to be written anyway so no 
                    //       chance to cancel was missed. 
                    //       Also in the case where our large task is an UPDATE then we will
                    //       still flush all deletes and adds before it can be written. This 
                    //       case should not be hit in practice as large tasks are likely to
                    //       be messages which are not updated. It is also still a slight 
                    //       improvement over the previous behaviour in that we don't flush
                    //       all outstanding updates.
                    if (bytesForTask >= _maxBytesPerBatch)
                    {
                        // Dispatch to the correct queue
                        if (newElement.isAdd())
                        {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Adding to FRONT of dispatch ADDING queue");

                            _dispatchAddingQueue.addFirst(newElement);
                        }
                        else if (newElement.isUpdate())
                        {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Adding to FRONT of dispatch UPDATE queue");

                            _dispatchUpdateQueue.addFirst(newElement);
                        }
                        else if (newElement.isDelete())
                        {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Adding to FRONT of dispatch REMOVE queue");

                            _dispatchRemoveQueue.addFirst(newElement);
                        }

                        // Add size to total
                        _dispatchedBytes += bytesForTask;

                        // Promotion causes a notification so since we're avoiding this, we need
                        // to notify the dispatch here
                        newElement.notifyDispatch();

                    }
                    // Defect 528533
                    // If we have an uncancelled delete then it is in our best 
                    // interests to get it dispatched as soon as possible for 
                    // several reasons:
                    // 
                    // 1. The sooner we delete on disk the sooner we release the
                    //    reference to the item and hence the memory that it uses.
                    // 
                    // 2. If we do not keep consumers (delete transactions) waiting
                    //    but still have a chance of making producers wait (when 
                    //    adding to the waiting queue) then we have a better chance
                    //    of keeping pace with the producers in the system.
                    else if (newElement.isDelete() ||                                            // New element is a delete 
                             (_waitingQueue.isEmpty() &&                                         // OR ( We are not yet using the waiting queue
                             (_dispatchedBytes + bytesForTask <= _maxDispatchedBytesPerThread))) //      AND there is space for this task )
                    {
                        // Dispatch to the correct queue
                        if (newElement.isAdd())
                        {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Adding to dispatch ADDING queue");

                            _dispatchAddingQueue.add(newElement);
                        }
                        else if (newElement.isUpdate())
                        {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Adding to dispatch UPDATE queue");

                            _dispatchUpdateQueue.add(newElement);
                        }
                        else if (newElement.isDelete())
                        {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Adding to dispatch REMOVE queue");

                            _dispatchRemoveQueue.add(newElement);
                        }

                        // Add size to total
                        _dispatchedBytes += bytesForTask;

                        // Promotion causes a notification so since we're avoiding this, we need
                        // to notify the dispatch here
                        newElement.notifyDispatch();
                    }
                    else // no space on dispatch queues
                    {
                        // Add to waiting queue
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Adding to WAITING queue");

                        _waitingQueue.add(newElement);

                        if (_isContributingToThreadWriteErrors)
                        {
                            newElement.notifyDispatch();
                        }
                        else
                        {
                            callerMustWait = true;
                        }
                    }
                }
                else // cancelled == true
                {
                    // SIB0112d.ms.2
                    // If we have cancelled then we need to inform the new task that 
                    // its persistence work is done.
                    ((Tuple)newTask.getPersistable()).persistableOperationCancelled();

                    // Promotion causes a notification so since we're avoiding this, we need
                    // to notify the dispatch here
                    newElement.notifyDispatch();

                    // Update PMI data.
                    skipCount++;
                    skipBytes += bytesForTask;
                } 
            } // end synchronized(_dispatchingLock)

           
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "addTask", Boolean.valueOf(callerMustWait));
            return callerMustWait;
        }


        /**
         * Notifies the thread that work has been dispatched to its waiting queue
         */
        private void notifyDispatchArrived()
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "notifyDispatchArrived");

            // Notify the threads affected by the dispatch request that there's
            // work to do            
            synchronized(_dispatchingLock)
            {
                int maxTasksInBatch = (_writeErrorOccurred) ? 1 : _maxTasksPerBatch;

                int dispatchAddingQueueSize = _dispatchAddingQueue.size();
                int dispatchUpdateQueueSize = _dispatchUpdateQueue.size();
                int dispatchRemoveQueueSize = _dispatchRemoveQueue.size();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                {
                    SibTr.debug(this, tc, "_notifyOutstanding=" + _notifyOutstanding);
                    SibTr.debug(this, tc, "_threadActive=" + _threadActive);
                    SibTr.debug(this, tc, "_dispatchAddingQueue.size()=" + dispatchAddingQueueSize);
                    SibTr.debug(this, tc, "_dispatchUpdateQueue.size()=" + dispatchUpdateQueueSize);
                    SibTr.debug(this, tc, "_dispatchRemoveQueue.size()=" + dispatchRemoveQueueSize);
                    SibTr.debug(this, tc, "_waitingQueue.size()=" + _waitingQueue.size());
                    SibTr.debug(this, tc, "maxTasksInBatch=" + maxTasksInBatch);
                    SibTr.debug(this, tc, "_dispatchedBytes=" + _dispatchedBytes);
                    SibTr.debug(this, tc, "_minBytesPerBatch=" + _minBytesPerBatch);
                    SibTr.debug(this, tc, "_maxBytesPerBatch=" + _maxBytesPerBatch);
                }

                if (!_notifyOutstanding &&                             // Don't already have a notify outstanding
                    !_threadActive &&                                  // AND the thread is not trying to write a batch
                    (((dispatchAddingQueueSize +                       // AND (The dispatch queues 
                       dispatchUpdateQueueSize +                       //          contain more than one
                       dispatchRemoveQueueSize) >= maxTasksInBatch) || //           batches worth of work 
                     (_dispatchedBytes >= _maxBytesPerBatch) ||        //      OR the size of work dispatched is more than one batches worth
                     !_waitingQueue.isEmpty()))                        //      OR we have started to fill the waiting queue)
                {
                    _notifyOutstanding = false;
                    _dispatchingLock.notify();
                }
            } // end synchronized

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "notifyDispatchArrived");
        }

        /**
         * This method builds a batch of requests to send to the persistence layer.
         *
         * @return The batch including cancelled tasks
         */
        private LinkedList buildBatch()
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "buildBatch");

            int maxTasksInBatch = (_writeErrorOccurred) ? 1 : _maxTasksPerBatch;
            LinkedList batch = new LinkedList();

            int dispatchAddingQueueSize = _dispatchAddingQueue.size();
            int dispatchUpdateQueueSize = _dispatchUpdateQueue.size();
            int dispatchRemoveQueueSize = _dispatchRemoveQueue.size();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                SibTr.debug(this, tc, "_dispatchAddingQueue.size()=" + dispatchAddingQueueSize);
                SibTr.debug(this, tc, "_dispatchUpdateQueue.size()=" + dispatchUpdateQueueSize);
                SibTr.debug(this, tc, "_dispatchRemoveQueue.size()=" + dispatchRemoveQueueSize);
                SibTr.debug(this, tc, "_waitingQueue.size()=" + _waitingQueue.size());
                SibTr.debug(this, tc, "maxTasksInBatch=" + maxTasksInBatch);
                SibTr.debug(this, tc, "_dispatchedBytes=" + _dispatchedBytes);
                SibTr.debug(this, tc, "_minBytesPerBatch=" + _minBytesPerBatch);
                SibTr.debug(this, tc, "_maxBytesPerBatch=" + _maxBytesPerBatch);
            }

            // Defect 496154
            // Only build a batch if there's enough data to bother with it. As
            // below we will stop batch creation if the dispatched bytes is lower
            // than the minimum bytes per batch we should only start the creation
            // process if that limit is passed.
            if (((dispatchAddingQueueSize + 
                  dispatchUpdateQueueSize + 
                  dispatchRemoveQueueSize) >= maxTasksInBatch) ||  // The dispatch queues contain more than one batches work 
                (_dispatchedBytes >= _minBytesPerBatch) ||         // OR the size of work dispatched is more than the lower batch limit
                !_waitingQueue.isEmpty())                          // OR we have started to fill the waiting queue
            {
                int numUncancelledTasksInBatch = 0;   // The number of uncancelled tasks in this batch
                long bytesInBatch = 0;                // The number of bytes in this batch
                long remainingDispatchedBytes = _dispatchedBytes; 

                // Defect 496154
                // As we are now dispatching across several lists we use these flags
                // to determine if we have any more tasks on the queues.
                boolean moreTasksToAdd = true;
                int dispatchQueuesChecked = 0;

                // Defect 496154
                // Write uncancellable deletes out first. This should give adds/updates
                // more time to be cancelled and free up space on disk. 
                // NOTE: if we don't have any deletes queued then we might aswell skip 
                // straight to the add/update queues and set the flags to reflect that.
                Iterator it = null;
                if (dispatchRemoveQueueSize > 0)
                {
                    // We have some removes to process.
                    it = _dispatchRemoveQueue.iterator();
                }
                else if (dispatchAddingQueueSize > 0)
                {
                    // No removes so go straight to the adds.
                    it = _dispatchAddingQueue.iterator();
                    dispatchQueuesChecked = 1;
                }
                else
                {
                    // No adds or removes so go straight 
                    // to the updates.
                    it = _dispatchUpdateQueue.iterator();
                    dispatchQueuesChecked = 2;
                }

                while (moreTasksToAdd &&                                  // There are tasks left in the dispatch queues
                       (numUncancelledTasksInBatch < maxTasksInBatch) &&  // AND we haven't reached the TASK limit for a batch yet
                       (bytesInBatch < _maxBytesPerBatch))                // AND we haven't reach the BYTE limit either
                {
                    // Check to see if we have come to the end of the queue. 
                    // If we have then we can move onto the next one. 
                    if (it.hasNext())
                    {
                        QueueElement qe = (QueueElement)it.next();

                        // Defect 463614
                        // We need to synchronize here to make sure that this task is not
                        // cancelled out from under us on the adding thread.
                        synchronized(qe)
                        {
                            Task task = qe.getTask();

                            if (!qe.isCancelled())
                            {
                                // Defect 496154
                                // If a delete for this item is in the system then
                                // we may be in the middle of cancelling the related 
                                // adds and updates. In order to avoid cancelling an 
                                // add only to find that the update has been batched
                                // we can check at build batch time to see if we are
                                // trying to batch an update that is in removing state.
                                if (!(qe.isUpdate()                   // We are NOT (looking at an update
                                    && task.getLink().isRemoving()))  //             which is about to be deleted)
                                {
                                    int bytesForTask = qe.getDataSize();
    
                                    // SIB0112d.ms.2
    
                                    // We want to avoid making batches too large, both in terms of number of
                                    // operations and number of bytes. The maximum byte size for a batch can only
                                    // be exceeded if there is one and only one item in the batch. This allows the
                                    // dispatcher to cope with large data sizes without blowing up by combining them
                                    // with more reasonably sized data.
                                    if ((bytesForTask + bytesInBatch >= _maxBytesPerBatch) && (numUncancelledTasksInBatch != 0))
                                    {
                                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Stopping batch creation as (bytesInBatch + newTask.size) > maxBytesPerBatch");
    
                                        break;
                                    }
    
                                    // SIB0112d.ms.2
                                    // As cancellation now happens at addTask() time we no longer need to check
                                    // for it here.
    
                                    // Defect 278083 
                                    // Once the number of dispatched bytes drops below the minimum threshold, we no
                                    // longer try to add work to the batch. This keeps small tasks on the queue for
                                    // another cycle of the dispatcher, thus making their cancellation more likely.
                                    // This gives quite a performance boost for small, short-lived messages.
                                    // For some benchmarks (mediated web services requests for small messages, for 
                                    // example), the server requires much less memory to sustain a heavy workload 
                                    if (remainingDispatchedBytes < _minBytesPerBatch)
                                    {
                                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Stopping batch creation as dispatchedBytes < minBytesPerBatch");
    
                                        break;
                                    }
    
                                    bytesInBatch += bytesForTask;
                                    numUncancelledTasksInBatch++;
    
                                    remainingDispatchedBytes -= bytesForTask;
    
                                    // SIB0112d.ms.2
                                    // Make sure the task knows that it is currently in a batch so that it can't be 
                                    // cancelled out from under us during an addTask().
                                    qe.setBatched(true);
    
                                    // Add the task to the batch
                                    batch.add(qe);
    
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Task added to batch: "+task);
                                }
                                else
                                {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Task skipped as it is a candidate for cancellation: "+task);
                                }
                            } // end if (!qe.isCancelled())
                            else
                            {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Task cancelled from under us: "+task);
                            }
                        } // end synchronized (qe)
                    } // end if (it.hasNext())
                    else
                    {
                        // Defect 496154
                        // We have got to the end of this queue? If we have 
                        // included any deletes then send them off on their own
                        // otherwise can we move onto the update queue?
                        switch (dispatchQueuesChecked)
                        {
                        case 0: // Defect 528533
                                // If we have delete operations in this batch 
                                // then don't move onto the next queues...
                                // i.e. deletes are always in a delete-only batch
                                moreTasksToAdd = false;
                                break;
                        case 1: // We've goone through all available adds now 
                                // we can look at our updates.
                                it = _dispatchUpdateQueue.iterator();
                                dispatchQueuesChecked = 2;
                                break;
                        default:
                                // We have checked all queues and still
                                // not got a next task so we must have 
                                // emptied the dispatch queues.
                                moreTasksToAdd = false;
                        }
                    }
                } // end while
            } // end if (enough data for batch)
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "No batch built");
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "buildBatch", Integer.valueOf(batch.size()));
            return batch;
        }

        /**
         * This method writes out a batch of requests using the persistence layer.
         * 
         * @param batch The batch to be written
         * 
         * @return <tt>true</tt> if the batch was written, else <tt>false</tt>
         * @throws SevereMessageStoreException 
         */
        private boolean writeBatch(LinkedList batch) throws SevereMessageStoreException
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeBatch", Integer.valueOf(batch.size()));

            boolean batchWritten = false;

            // Lazily initialise the batching context in case we end up with an empty batch                
            BatchingContext batchingContext = null;

            int skipCount = 0;
            int skipBytes = 0;

            Iterator it = batch.iterator();
            while (it.hasNext())
            {
                QueueElement qe = (QueueElement)it.next();
                Task task = qe.getTask();

                // Defect 530772
                // We need to check at this point for deletes whose adds may
                // have been cancelled due to the following chain of events:
                // 
                // 1. Add dispatched
                // 2. Add added to batch
                // 3. Delete dispatched
                //   3.1. Add not cancelled as in batch
                //   3.2 Delete added to dispatch queue as un-cancellable
                // 4. Delete committed
                //   4.1 Link for item set to null as delete has completed
                // 5. Add batch written
                //   5.1 Add fails during ensureDataAvailable() due to null link
                //   5.2 Add is cancelled.
                // 6. Delete batched
                // 7. Delete written
                //   7.1 Delete cancelled as add never actually made it to disk. Writing
                //       of delete at this point would trigger a Row/Object not found
                //       type error from the data/file store.
                if (qe.isDelete() &&                                                       // This task is a delete
                    !((Tuple)task.getPersistable()).persistableRepresentationWasCreated()) // AND we don't have a persistent representation
                {
                    // Delete task does not need to make it to disk as the 
                    // matching add didn't make it.
                    qe.setCancelled();
                }

                // SIB0112d.ms.2 
                // Don't need to check if we can cancel here as it should
                // have been checked at addTask() time.
                try
                {
                    task.ensureDataAvailable();
                }
                catch (PersistentDataEncodingException pdee)
                {
                    //No FFDC Code Needed.
                    // If the item cannot provide its persistent data, the item will have
                    // been marked as corrupt. The item is still OK unless it needs to have
                    // its persistent state read from disk. In this case, the item will be
                    // quietly removed from the MS.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "ensureDataAvailable encountered item which couldn't encode");

                    qe.setCancelled();
                }
                catch (SevereMessageStoreException smse)
                {
                    //No FFDC Code Needed.
                    // It is possible that the item comes out of the MS in between the
                    // call to isInStore() above and the request for the persistent
                    // data. This catch block allows us to catch this at the last
                    // possible moment, thus sparing ourselves the problem of the
                    // NotInMessageStore exception during the actual persistence
                    // layer operations
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "ensureDataAvailable encountered item not in store");

                    qe.setCancelled();
                }

                if (!qe.isCancelled())
                {
                    if (batchingContext == null)
                    {
                        batchingContext = _bcfactory.createBatchingContext();
                    }

                    task.persist(batchingContext, TransactionState.STATE_COMMITTED);
                }

                if (qe.isCancelled())
                {
                    skipCount++;
                    skipBytes += qe.getDataSize();
                }
            }

            if (batchingContext != null)
            {
                try
                {
                    batchingContext.executeBatch();
                    batchWritten = true;
                }
                catch (SeverePersistenceException spe)
                {
                	com.ibm.ws.ffdc.FFDCFilter.processException(spe, "com.ibm.ws.sib.msgstore.persistence.impl.SpillDispatcher.writeBatch", "1:1757:1.48", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception persisting batch", spe);

                    // 258476
                    throw new SevereMessageStoreException(spe);
                }
                // SIB0112d.ms.2
                catch (PersistenceException pe)
                {
                	com.ibm.ws.ffdc.FFDCFilter.processException(pe, "com.ibm.ws.sib.msgstore.persistence.impl.SpillDispatcher.writeBatch", "1:1766:1.48", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception persisting batch", pe);
                }
            }
            else
            {
                batchWritten = true;
            }
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeBatch", Boolean.valueOf(batchWritten));
            return batchWritten;
        }

        // Defect 496154
        /**
         * This method is called when a batch is successfully written.
         * It informs all tasks in the batch of their completion and
         * removes the same tasks from the dispatching queues.
         * 
         * @param batch  The batch of tasks that was written.
         * @throws SevereMessageStoreException 
         */
        private void batchCompleted(LinkedList batch) throws SevereMessageStoreException
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "batchCompleted");

            int batchSize = batch.size();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "batchSize="+batchSize);

            if (batchSize > 0)
            {
                // Scan through the batch telling the Persistables 
                // that they have been written.
                Iterator it = batch.iterator();
                synchronized (_dispatchingLock)
                {
                    while (it.hasNext())
                    {
                        // Task could have been cancelled if it failed to encode
                        // at write time so we need to make the correct call to
                        // keep the in-memory state in line with what is on disk.
                        QueueElement qe = (QueueElement)it.next();
                        if (qe.isCancelled())
                        {
                            ((Tuple)qe.getTask().getPersistable()).persistableOperationCancelled();
                        }
                        else
                        {
                            ((Tuple)qe.getTask().getPersistable()).persistableOperationCompleted();
                        }

                        // Check which dispatch queue to remove the task from.
                        if (qe.isAdd())
                        {
                            QueueElement qeTemp = (QueueElement)_dispatchAddingQueue.remove(0);

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Task removed from ADDING queue after batch completion: "+qeTemp.getTask());
                        }
                        else if (qe.isUpdate())
                        {
                            // Need to use remove(<element>) here as we could have 
                            // skipped an update that was a candidate for cancellation.
                            _dispatchUpdateQueue.remove(qe);

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Task removed from UPDATE queue after batch completion: "+qe.getTask());
                        }
                        else if (qe.isDelete())
                        {
                            QueueElement qeTemp = (QueueElement)_dispatchRemoveQueue.remove(0);

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Task removed from REMOVE queue after batch completion: "+qeTemp.getTask());
                        }

                        // Update the byte counter for the dispatch queues.
                        _dispatchedBytes -= qe.getDataSize();
                    }
                } // end synchronized
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "batchCompleted");
        }

        // Defect 496154
        /**
         * This method is called when an error occured while executing
         * a batch. It informs any tasks that may have been cancelled 
         * due to encoding problems and removes the same tasks from the
         * dispatching queues. For the remove it uses the remove(Object)
         * method which will be slower but more reliable as we do not 
         * know where in the list the cancelled elements are positioned.
         * This will be slower but we will only be on this path in an 
         * error case so shouldn't be a problem. 
         *  
         * @param batch  The batch of tasks that failed to execute.
         * @return int  How many items were cancelled.
         * @throws SevereMessageStoreException 
         */
        private int batchCancelled(LinkedList batch) throws SevereMessageStoreException
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "batchCancelled");

            int batchSize = batch.size();
            int cancelledTasks = 0;
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "batchSize="+batchSize);

            if (batchSize > 0)
            {
                // Scan through the batch telling the Persistables 
                // that they have been cancelled.
                Iterator it = batch.iterator();
                synchronized (_dispatchingLock)
                {
                    while (it.hasNext())
                    {
                        // Task could have been cancelled if it failed to encode
                        // at write time so we need to make the correct call to
                        // keep the in-memory state in line with what is on disk.
                        QueueElement qe = (QueueElement)it.next();
                        if (qe.isCancelled())
                        {
                            ((Tuple)qe.getTask().getPersistable()).persistableOperationCancelled();

                            // Check which dispatch queue to remove the task from.
                            if (qe.isAdd())
                            {
                                // Remove the element from the ADDING queue. Note that 
                                // in the cancel case we use the remove(<element>) method 
                                // which will be slower but will make sure the correct
                                // element is removed.
                                if (_dispatchAddingQueue.remove(qe))
                                {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Cancelled element removed from ADDING queue.");
                                }
                                else
                                {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Cancelled element not found in ADDING queue!");
                                }
                            }
                            else if (qe.isUpdate())
                            {
                                // Remove the element from the UPDATE queue. Note that 
                                // in the cancel case we use the remove(<element>) method 
                                // which will be slower but will make sure the correct
                                // element is removed.
                                if (_dispatchUpdateQueue.remove(qe))
                                {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Cancelled element removed from UPDATE queue.");
                                }
                                else
                                {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Cancelled element not found in UPDATE queue!");
                                }
                            }
                            else if (qe.isDelete())
                            {
                                // Remove the element from the REMOVE queue. Note that in
                                // the cancel case we use the remove(<element>) method 
                                // which will be slower but will make sure the correct
                                // element is removed.
                                if (_dispatchRemoveQueue.remove(qe))
                                {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Cancelled element removed from REMOVE queue.");
                                }
                                else
                                {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Cancelled element not found in REMOVE queue!");
                                }
                            }

                            // Update the byte counter for the dispatch queues.
                            _dispatchedBytes -= qe.getDataSize();
                            cancelledTasks++;
                        }
                    }
                } // end synchronized
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "batchCancelled", cancelledTasks);
            return cancelledTasks;

        }

        /**
         * Handle the write error just by setting a status flag. This
         * prevents batching from occurring in case excessive batching
         * was the cause of the problem.
         */
        private void handleWriteError(boolean fatalError, int batchSize)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "handleWriteError", new Object[] {Boolean.valueOf(fatalError), new Integer(batchSize)});

            boolean repeatedError = false;

            // Set the flags and counters indicating we're in trouble
            if (!_writeErrorOccurred || (_consecutiveWriteErrors == 0))
            {
                _writeErrorOccurred = true;
                _goodWritesSinceLastError = 0;
                _consecutiveWriteErrors = 1;
                _writesToResetErrorState = batchSize; 
                
                // Using zero is dangerous because a wait of 0 conventionally means forever
                _writeErrorRetryDelay = 1;
            }
            else
            {
                repeatedError = true;
            }

            if (fatalError || repeatedError)
            {
                if (!_isContributingToThreadWriteErrors)
                {
                    _isContributingToThreadWriteErrors = true;
                    threadWriteErrorOccurred(_threadNum);
                }

                _consecutiveWriteErrors++;
                _writeErrorRetryDelay = 5000 * ((_consecutiveWriteErrors > 5) ? 5 : _consecutiveWriteErrors);

                // Notify anything in the waiting queue so that it is not
                // blocked indefinitely. This means that we've exceptionally
                // accepted work in the waiting queue for dispatch, even though
                // we've had a write error. There's little choice if we are
                // to avoid blocking indefinitely.
                synchronized(_dispatchingLock)
                {
                    Iterator it = _waitingQueue.iterator();
                    while (it.hasNext())
                    {
                        QueueElement qe = (QueueElement)it.next();
                        qe.notifyDispatch();
                    }
                } // end synchronized
            }

            // SIB0112d.ms.2

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "handleWriteError");
        }

        /**
         * Promotes Tasks from the waiting queue to the dispatch queue.<p>
         * Every wait object associated with the promoted tasks is notified.
         */
        private void promoteWaiters()
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "promoteWaiters");

            // THIS SYNCHRONIZES ACCESS TO THE WAITING QUEUE ALSO
            synchronized(_dispatchingLock)
            {
                // If there is something waiting to be promoted and there is space to receive
                // extra work on the dispatch queue...
                if (!_waitingQueue.isEmpty() &&                        // We have work on the waiting queue
                    (_dispatchedBytes < _maxDispatchedBytesPerThread)) // AND we have space on the dispatch queues
                {
                    Iterator it = _waitingQueue.iterator();
                    while (it.hasNext())
                    {
                        QueueElement qe = (QueueElement)it.next();

                        // If we have too little data to trigger a write, OR we can cram a little
                        // more data onto the dispatch queue...
                        if ((_dispatchedBytes < _minBytesPerBatch) ||
                            (_dispatchedBytes + qe.getDataSize() <= _maxDispatchedBytesPerThread))
                        {
                            qe.notifyDispatch();

                            // Move it from the waiting queue to the dispatch queue                    
                            it.remove();

                            // Defect 496154
                            // Promote to the right dispatch queue for the
                            // type of task.
                            if (qe.isAdd())
                            {
                                _dispatchAddingQueue.add(qe);
                            }
                            else if (qe.isUpdate())
                            {
                                _dispatchUpdateQueue.add(qe);
                            }
                            else if (qe.isDelete())
                            {
                                _dispatchRemoveQueue.add(qe);
                            }

                            _dispatchedBytes += qe.getDataSize();
                        }
                        else
                        {
                            break;
                        }
                    }
                }
            } // end synchronized

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "promoteWaiters");
        }

        public boolean isInterruptible()
        {
            return _interruptible;
        }

        public String toString()
        {
            return _threadName;
        }
    }


    private static class QueueElement
    {
        private Task             _task;
        private int              _dataSize;
        private DispatchNotifier _waiter;

        private boolean _notified;
        private boolean _cancelled;
        // SIB0112d.ms.2
        private boolean _batched;
        //Defect 496154
        private boolean _isAdd;
        private boolean _isDelete;

        public QueueElement(Task task, DispatchNotifier waiter)
        {
            _task     = task;
            _dataSize = task.getPersistableInMemorySizeApproximation(TransactionState.STATE_COMMITTED);
            _waiter   = waiter;

            // Defect 496154
            _isAdd    = _task.isCreateOfPersistentRepresentation();
            _isDelete = _task.isDeleteOfPersistentRepresentation();
        }

        public Task getTask()
        {
            return _task;
        }

        public int getDataSize()
        {
            return _dataSize;
        }

        public void setCancelled()
        {
            _cancelled = true;
        }

        public boolean isCancelled()
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "isCancelled="+_cancelled);

            return _cancelled;
        }

        // SIB0112d.ms.2
        public void setBatched(boolean batched)
        {
            _batched = batched;
        }

        // SIB0112d.ms.2
        public boolean isBatched()
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "isBatched="+_batched);

            return _batched;
        }

        // Defect 496154
        /**
         * @return true if the associated task is an add
         */
        public final boolean isAdd()
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "isAdd="+_isAdd);

            return _isAdd;
        }

        // Defect 496154
        /**
         * @return true if the associated task is neither an add or delete.
         */
        public final boolean isUpdate()
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "isUpdate="+!(_isAdd || _isDelete));

            return !(_isAdd || _isDelete);
        }

        // Defect 496154
        /**
         * @return true if the associated task is a delete
         */
        public final boolean isDelete()
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "isDelete="+_isDelete);

            return _isDelete;
        }

        public void notifyDispatch()
        {
            // Notifications are performed through the QueueElement rather
            // than directly on the DispatchNotifier. This permits the dispatcher
            // to notify more than once without mangling the counting of notifications
            // in the DispatchNotifier. This occurs when there's a write error and
            // we notify everything in the waiting queue, only to notify as the
            // Tasks get promoted to the dispatching queue once the write error's been resolved
            if (!_notified)
            {
                _notified = true;
                _waiter.notifyDispatch();
            }
        }
    }
}
