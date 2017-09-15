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

/**
 * This interface defines the common behavior of futures returned from asynchronous calls.
 * In particular, it describes the most general type of futures, that is futures for calls
 * to any asynchronous operations. In practice, this interface is subclassed by others
 * that describe call-specific behavior including calls that return a value from their
 * asynchronous operation.
 * <p>
 * Once an asynchronous operation has been invoked, the future 'represents' the operation
 * in subsequent calls to add a completion listener, cancel the operation, get its results,
 * and so on. Only the Async IO library creates futures.
 * </p>
 */
public interface IAbstractAsyncFuture {

    /**
     * Adds the given object as a listener to the completion event for the asynchronous
     * operation with assurance that the callback will occur on a pooled thread.
     * <p>
     * The listener will be notified once, and only once, on completion of the asynchronous
     * operation represented by the future.
     * </p> When the future completes, each listener is invoked with the
     * <code>futureCompleted(IAbstractAsyncFuture, Object)</code> method. The thread invoking
     * the callback is drawn from the <code>IResultThreadManager</code> of the channel
     * involved in the operation.
     * </p>
     * <p>The caller can attach some application state to the callback, using the
     * <code>state</code> parameter on this method.
     * 
     * @param listener
     *            an object which implements the {@link ICompletionListener} interface.
     * @param state
     *            an object that will be sent with the callback, or <code>null</code> if none.
     */
    void addCompletionListener(ICompletionListener listener, Object state);

    /**
     * Attempts to cancel the operation represented by the receiver with the specified reason.
     * Cancellation is a sequential operation, and will not succeed if the IO operation is
     * already complete. Subclasses define the exception that will occur when the operation
     * is queried.
     * 
     * @param reason
     */
    void cancel(Exception reason);

    /**
     * Answers <code>true</code> if the operation represented by the future has completed,
     * and <code>false</code> if the operation is still pending. If the receiver returns
     * <code>true</code> it will always subsequently return <code>true</code> (ie. it is a
     * "latch").
     * <p>
     * This call always returns immediately - this method does not block. This method
     * can be used to <em>Poll</em> for the completion of the future.
     * </p>
     * 
     * @return <code>true</code> if the operation is complete, and <code>false</code> otherwise.
     */
    boolean isCompleted();

    /**
     * Blocks the calling thread indefinitely until the future has completed. If the future
     * has already completed, the method returns immediately.
     * 
     * @throws InterruptedException
     *             if the waiting thread is interrupted before the future completes.
     */
    void waitForCompletion() throws InterruptedException;

    /**
     * Blocks the calling thread until either the future has completed or a timeout
     * period has expired. The timeout period is specified in milliseconds.
     * 
     * @param timeout
     *            the maximum time to wait in milliseconds. Specifying 0L means wait forever.
     * @throws AsyncTimeoutException
     *             if the timeout period elapsed.
     * @throws InterruptedException
     *             if the waiting thread is interrupted before the wait completes.
     */
    void waitForCompletion(long timeout) throws AsyncTimeoutException, InterruptedException;

    /**
     * Obtain an object which can be used to synchronize access to the future objects
     * 
     * @return future synchronization object
     */
    Object getCompletedSemaphore();

    /**
     * Set the timeout work item for this async future item.
     * 
     * @param twi
     */
    void setTimeoutWorkItem(TimerWorkItem twi);

    /**
     * Query the current timeout work item for this future, might be null.
     * 
     * @return TimerWorkItem
     */
    TimerWorkItem getTimeoutWorkItem();

    /**
     * Mark the fully complete state of this future object based on the input flag.
     * 
     * @param value
     */
    void setFullyCompleted(boolean value);

    /**
     * Re-use counter to allow some detection of whether async actions are working
     * with the future object they expect to be (i.e. comparing the current re-use
     * count against a previously stored value.
     * 
     * @return int
     */
    int getReuseCount();
}