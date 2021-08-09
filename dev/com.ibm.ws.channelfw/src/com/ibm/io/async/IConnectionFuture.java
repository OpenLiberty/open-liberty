/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.io.async;

import java.io.IOException;

/**
 * A future object for client socket connection operations.
 */
public interface IConnectionFuture extends IAbstractAsyncFuture {

    /**
     * Waits indefinitely for the asynchronous socket connection to complete.
     * <p>
     * This method is the equivalent of the generic {@link IAbstractAsyncFuture#waitForCompletion()}waiting method, except that this method
     * may throw an <code>IOException</code> if a problem occurs on the connection.
     * </p>
     * 
     * @throws InterruptedException
     *             the waiting thread was interrupted.
     * @throws IOException
     *             the operation completed, but failed with an IOException
     */
    public void complete() throws InterruptedException, IOException;

    /**
     * Waits for the given period of time for the asynchronous connection operation to complete.
     * <p>
     * This method is the equivalent of the generic {@link IAbstractAsyncFuture#waitForCompletion(long)}waiting method, except that this
     * method may throw an <code>IOException</code> if a problem occurs on the connection.
     * </p>
     * 
     * @param timeout
     *            the time to wait for completion, in milliseconds
     * @throws InterruptedException
     *             the thread was interrupted while waiting for the operation to complete
     * @throws AsyncTimeoutException
     *             the wait timed out before the operation completed
     * @throws IOException
     *             the operation completed, but failed with an IOException
     */
    public void complete(long timeout) throws InterruptedException, IOException;
}