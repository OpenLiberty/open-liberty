/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.io.async;

import java.util.concurrent.ExecutorService;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.tcpchannel.internal.TCPChannelMessageConstants;

/**
 * Abstract superclass for all types of asynchronous operation futures.
 * <p>
 * Implements methods that relate to the completion of the operation
 * represented by the future.
 * <p>
 * When an asynchronous operation is requested, a future object is returned
 * which represents the operation concerned. The calling application can use
 * the future to obtain information about the completion of the operation.
 * <p>
 * There are three methods by which the calling application can get information
 * about the completion of the operation:
 * <ul>
 * <li>polling : using the {@link #isCompleted()} method, that will return true
 * when the operation finally completes.</li>
 * <li>blocking: using the {@link #waitForCompletion()} or {@link #waitForCompletion(long)} methods, that will block indefinitely or for
 * a limited period of time respectively. The blocking thread is woken when the
 * future completes.</li>
 * <li>callback : by registering a listener using {@link #addCompletionListener(ICompletionListener, Object)} the caller can
 * provider a listener that will be called back when the future completes. The
 * completed future and user state <code>Object</code> are passed as
 * arguments in the callback.</li>
 * </ul>
 * <p>
 * Concrete futures will be a subclass that is specialized to reflect the
 * operation that is requested. So, for example, where the operation is an IO
 * Read or Write the future will have methods that relate to the buffer
 * involved in the operation, and the number of bytes written or read.
 * 
 */
abstract class AbstractAsyncFuture implements IAbstractAsyncFuture {

    private static final TraceComponent tc = Tr.register(AbstractAsyncFuture.class,
                                                         TCPChannelMessageConstants.TCP_TRACE_NAME,
                                                         TCPChannelMessageConstants.TCP_BUNDLE);

    // This is the channel that hosted the asynchronous operation.
    protected final AbstractAsyncChannel channel;

    // If the operation caused an exception, the exception is stored here and
    // thrown on an attempt to get the affected byte count.
    protected volatile Exception exception = null;

    // The completed flag is a latch set when the async operation is being
    // completed.
    protected volatile boolean completed = false;

    // The fullyCompleted flag is a latch set when the async operation is fully
    // finished.
    protected volatile boolean fullyCompleted = false;

    // A timeout in progress flag so we don't throw back an exception we got
    // from cancelling an outstanding read/write when we do a timeout.
    protected volatile int cancelInProgress = 0;

    // The reuseCount flag is to make sure that we are not changing a new use
    // of the future object based on old future information.
    // Make this volatile since we don't want to add more sync logic.
    protected volatile int reuseCount = 0;

    protected final Object completedSemaphore = new Object();

    // listener for the completion event.
    protected ICompletionListener firstListener = null;
    protected Object firstListenerState = null;
    /** Optional callback object for this future */
    private WorkCallback myCallback = null;

    /**
     * Constructor for this class.
     * 
     * @param channel
     */
    protected AbstractAsyncFuture(AbstractAsyncChannel channel) {
        super();
        this.channel = channel;
    }

    public int getReuseCount() {
        return this.reuseCount;
    }

    public Object getCompletedSemaphore() {
        return this.completedSemaphore;
    }

    /**
     * Marks this call as fully completed, but that it completed with an error.
     * The error is described by the parameter. Note that the thread that
     * invokes this method also runs the code in the callback function, which
     * is arbitrarily long (as it is user code).
     * 
     * @param throwable
     */
    void completed(Exception throwable) {
        boolean needToFire = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "completed", throwable.getMessage());
        }
        synchronized (this.completedSemaphore) {
            // future can already be completed
            if (this.completed || !this.channel.isOpen()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Future completed after already cancelled or socket was closed");
                }
                return;
            }
            this.completed = true;

            // new timeout code - cancel active timeout request
            if (getTimeoutWorkItem() != null) {
                getTimeoutWorkItem().state = TimerWorkItem.ENTRY_CANCELLED;
            }

            this.exception = throwable;

            if (this.firstListener == null) {
                // Sync Read/Write request.
                // must do this inside the sync, or else syncRead/Write could complete
                // before we get here, and we would be doing this on the next
                // read/write request!
                needToFire = false;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "doing notify for future " + this);
                }
                this.completedSemaphore.notifyAll();
            }
        } // end of sync

        if (needToFire) {
            // ASync Read/Write request.
            // need to do this outside the sync, or else we will hold the sync
            // for the user's callback.
            fireCompletionActions();
        }

    }

    /**
     * When an async future has completed the following actions are performed,
     * in this order: <ol><li> The Future is marked completed <li> If
     * callbacks are registered, they are invoked in sequence, running
     * arbitrary code. </li><li> The semaphore blocking any waiting threads is
     * signalled to wake them up. </li></ol>
     * 
     */
    abstract protected void fireCompletionActions();

    /**
     * Runs a callback on the given listener safely. We cannot allow misbehaved application
     * callback code to spoil the notification of subsequent listeners or other tidy-up work,
     * so the callbacks have to be tightly wrappered in an exception hander that ignores the
     * error and continues with the next callback. This could lead to creative
     * problems in the app code, so callbacks must be written carefully.
     * 
     * @param listener
     * @param future
     * @param userState
     */
    protected void invokeCallback(ICompletionListener listener, AbstractAsyncFuture future, Object userState) {
        try {
            ExecutorService executorService = CHFWBundle.getExecutorService();
            if (null == executorService) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Unable to schedule callback, using this thread");
                }
                listener.futureCompleted(future, userState);
            } else {
                if (null == this.myCallback) {
                    this.myCallback = new WorkCallback(listener, userState);
                } else {
                    this.myCallback.myListener = listener;
                    this.myCallback.myState = userState;
                }
                executorService.execute(this.myCallback);
            }
        } catch (Throwable problem) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Error invoking callback, exception: " + problem + " : " + problem.getMessage());
            }
            FFDCFilter.processException(problem, getClass().getName() + ".invokeCallback", "182", this);
        }
    }

    /*
     * @see com.ibm.io.async.IAbstractAsyncFuture#addCompletionListener(com.ibm.io.async.ICompletionListener, java.lang.Object)
     */
    abstract public void addCompletionListener(ICompletionListener listener, Object state);

    /**
     * Cancel an operation represented by a future and specify the reason
     * why the operation was cancelled.
     * 
     * @param reason
     */
    abstract public void cancel(Exception reason);

    /**
     * Answers true if the operation represented by the AsyncFuture has
     * completed, and false if the operation is still pending. If the receiver
     * returns true it will always subsequently return true (ie. it is a
     * "latch").
     * 
     * Always returns immediately - this method does not block.
     * 
     * @return true if the operation is complete, and false otherwise.
     */
    public boolean isCompleted() {
        return this.completed;
    }

    /**
     * set the state of the fully completed flag
     * 
     * @param newValue new state
     */
    public void setFullyCompleted(boolean newValue) {
        this.fullyCompleted = newValue;
    }

    /**
     * Set the flag on whether a cancel is in progress or not to the input
     * value.
     * 
     * @param newValue (0==no, 1==yes)
     */
    public void setCancelInProgress(int newValue) {
        this.cancelInProgress = newValue;
    }

    /**
     * Query whether a cancel is currently being attempted.
     * 
     * @return int (0==no, 1==yes)
     */
    public int getCancelInProgress() {
        return this.cancelInProgress;
    }

    /**
     * Blocks the calling thread indefinitely until the AsyncFuture has
     * completed. If the AsyncFuture has already compeleted, the method
     * returns immediately.
     * 
     * @throws InterruptedException
     *             if the waiting thread is interrupted before
     *             the wait completes.
     */
    public void waitForCompletion() throws InterruptedException {
        try {
            waitForCompletion(0L);
        } catch (AsyncTimeoutException ate) {
            // cannot happen since arg of 0L means wait forever
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unexpected timeout on blocking wait call, exception: " + ate.getMessage());
            }
            FFDCFilter.processException(ate,
                                        getClass().getName() + ".waitForCompletion", "268", this);
            InterruptedException ie = new InterruptedException("Unexpected timeout");
            ie.initCause(ate);
            throw ie;
        }
    }

    /**
     * Blocks the calling thread until either the Future has completed or the
     * timeout period has expired. The timeout period is set in milliseconds.
     * 
     * @param timeout
     *            the maximum time to wait in milliseconds.
     *            Specifying 0L means wait forever.
     * @throws AsyncTimeoutException
     *             if the timeout period elapsed.
     * @throws InterruptedException
     *             if the waiting thread is interrupted before
     *             the wait completes.
     */
    public void waitForCompletion(long timeout) throws AsyncTimeoutException, InterruptedException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "waitForCompletion: " + timeout);
        }

        // First do a non-sync test, to see if we can return quickly
        if (this.fullyCompleted) {
            return;
        }

        synchronized (this.completedSemaphore) {
            if (this.completed) {
                return;
            }
            // not completed, so we must wait to be notified of completion.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "waiting for completion notification for future: " + this);
            }
            this.completedSemaphore.wait(timeout);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "done waiting for completion notification for future: " + this);
            }
        } // end-sync

        // we either woke up because the operation completed, or timed out.
        // For performance reasons, first do a lazy check to see if we are done.
        // If we are not then grab the semaphore, and throw a timed out
        // exception.
        if (this.fullyCompleted) {
            return;
        }

        synchronized (this.completedSemaphore) {
            if (!this.completed) {
                this.completed = true;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Sync operation timed out");
                }
                throw new AsyncTimeoutException();
            }
        } // end-sync
    }

    /**
     * Reset this future object back to the default values.
     */
    public void resetFuture() {
        // this must be done before "completed = false". Since reuseCount
        // is volatile, and we ONLY need it to be in sync with the thread
        // in AsyncSocketChannelHelper WHEN completed is detected as being
        // false, then we should not need further synchronization.
        this.reuseCount++;
        if (getTimeoutWorkItem() != null) {
            getTimeoutWorkItem().state = TimerWorkItem.ENTRY_CANCELLED;
        }
        setTimeoutWorkItem(null);

        this.completed = false;
        this.fullyCompleted = false;
        this.exception = null;
        this.firstListener = null;
        this.firstListenerState = null;
    }

    /**
     * Work runnable object used to handle the callbacks that require
     * happening on a new thread.
     */
    private class WorkCallback implements Runnable {
        protected ICompletionListener myListener;
        protected Object myState;

        /**
         * Constructor.
         * 
         * @param listener
         * @param state
         */
        protected WorkCallback(ICompletionListener listener, Object state) {
            this.myListener = listener;
            this.myState = state;
        }

        public void run() {
            this.myListener.futureCompleted(AbstractAsyncFuture.this, this.myState);
        }

    }
}
