/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * The interface between the core EJB container and the runtime environment
 * that has scheduled an asynchronous method to be invoked from a remote client.
 */
public interface RemoteAsyncResult extends Remote
{
    /**
     * Attempts to cancel execution of this task. This attempt will fail if the
     * task has already completed, already been cancelled, or could not be cancelled
     * for some other reason. If successful, and this task has not started when
     * cancel is called, this task should never run. If the task has already started,
     * then the mayInterruptIfRunning parameter determines whether the thread
     * executing this task should be interrupted in an attempt to stop the task.
     * 
     * @param mayInterruptIfRunning - true if the thread executing this task should
     *            be interrupted; otherwise, in-progress tasks are allowed to complete .
     * 
     * @return false if the task could not be cancelled, typically because it
     *         has already completed normally; true otherwise.
     * 
     * @throws RemoteException
     */
    public boolean cancel(boolean mayInterruptIfRunning) throws RemoteException;

    /**
     * Waits if necessary for the computation to complete, and then retrieves
     * its result.
     * 
     * @return the computed result.
     * 
     * @throws RemoteException
     */
    public Object get() throws CancellationException, ExecutionException, InterruptedException, RemoteException;

    /**
     * Waits if necessary for at most the given time for the computation to complete,
     * and then retrieves its result, if available.
     * 
     * @param timeout - the maximum time to wait.
     * @param unit - the time unit of the timeout argument.
     * 
     * @return the computed result.
     * 
     * @throws RemoteException
     */
    // This method substitute is temporary until it is decided if TimeUnit is a valid rmi -iiop object.
    // public Object get(long timeout, TimeUnit unit) throws RemoteException;
    public Object get(long timeout, String unit) throws CancellationException, ExecutionException, InterruptedException, TimeoutException, RemoteException;

    /**
     * Allows clients to check the Future object to see if the
     * method was cancelled before it got a chance to execute.
     * 
     * @return true if this task was cancelled before it completed normally
     * 
     * @throws RemoteException
     */
    public boolean isCancelled() throws RemoteException;

    /**
     * Allows clients to poll the Future object and only get results once the method
     * has finished (ie. either due to normal termination, an exception, or cancelled).
     * 
     * @return true if this task completed. Completion may be due to normal termination,
     *         an exception, or cancellation -- in all of these cases, this method will return true.
     * 
     * @throws RemoteException
     */
    public boolean isDone() throws RemoteException;

} // end RemoteAsyncResult
