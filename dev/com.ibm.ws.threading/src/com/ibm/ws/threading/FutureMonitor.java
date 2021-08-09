/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading;

import java.util.concurrent.Future;

import com.ibm.ws.threading.listeners.CompletionListener;

/**
 * Futures are a nice feature in Java which allow you to do work asynchronously, while
 * also getting a response. However a Future is synchronous, so you can poll a future,
 * or block until it is done, but you are not able to get notified when the result has
 * been received.
 * 
 * <p>The FutureMonitor service aims to bridge that gap. It allows you to register a
 * listener so when the Future is complete the listener will be invoked. In essence
 * it moves the polling out of the client code, and into one central place.
 * </p>
 * 
 * <p>Polling Futures is not that efficient though, so this service also provides APIs
 * that allow the creation of Futures that will not block. That is the completion of
 * the future will notify the listeners when the future is completed.
 * </p>
 */
public interface FutureMonitor {
    /**
     * Register a listener to be notified when a future is done. If the Future was
     * created by the FutureMonitor the notification of the completion will short circuit
     * and use the thread that marks the future done.
     * 
     * @param work The Future to monitor
     * @param l The listener to notify
     */
    public <T> void onCompletion(Future<T> work, CompletionListener<T> l);

    /**
     * Update a Future as done where the result of the Future was an error. It allows
     * a Throwable to be associated with the Future. The result of this would
     * result in an exception from the poll method.
     * 
     * <p>This method only works with the Futures created by this service.</p>
     * 
     * <p>This method will notify any completion listeners using the callers thread.</p>
     * 
     * <p>This method has no effect if the Future is already done.</p>
     * 
     * @param future The future to set the error on.
     * @param error The error to associate with the Future.
     */
    public void setResult(Future<?> future, Throwable error);

    /**
     * Update a Future as done with the result.
     * 
     * <p>This method only works with the Futures created by this service.</p>
     * 
     * <p>This method will notify any completion listeners using the callers thread.</p>
     * 
     * <p>This method has no effect if the Future is already done.</p>
     * 
     * @param future The future to set the error on.
     * @param error The error to associate with the Future.
     */
    public <T> void setResult(Future<T> future, T result);

    /**
     * Creates a future that is "done" at the time it is created.
     * This creates a future with an associated exception so this results in the
     * poll method throwing an exception.
     * 
     * @param type The type of the result of this future.
     * @param t The throwable that indicates the error.
     * @return The created future.
     */
    public <T> Future<T> createFutureWithResult(Class<T> type, Throwable t);

    /**
     * Creates a future that is "done" at the time it is created.
     * 
     * @param t The result associated with the future.
     * @return The created future.
     */
    public <T> Future<T> createFutureWithResult(T result);

    /**
     * Creates a future of the specified type that can be completed
     * at a later date.
     * 
     * <p>This Future is completed by calling one of the setResult methods shown
     * above.</p>
     * 
     * @param type the type of the result of the Future.
     * @return the future which is not yet complete.
     */
    public <T> Future<T> createFuture(Class<T> type);
}