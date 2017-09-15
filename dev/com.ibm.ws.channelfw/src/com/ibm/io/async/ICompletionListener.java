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

/**
 * Defines the method to be implemented by future listeners.
 * <p>
 * This interface is used by a future object to notify registered listeners that the operation represented by the future
 * has completed. These <em>callback</em> methods can perform significant work in the application code.
 * </p>
 */
public interface ICompletionListener {

    /**
     * This method is called by the future object when the operation has completed.
     * <p>
     * If the listener is registered with a future when the operation has already completed, the listener is called immediately during the
     * register operation.
     * </p>
     * <p>
     * The threads available for running completion callbacks are provided by the <code>ThreadPool</code>, under a policy encoded in
     * the <code>IResultThreadHandler</code>.
     * </p>
     * 
     * @param result
     *            the future that has completed.
     * @param userState
     *            the object that was passed in as an argument when the listener was registered, or <code>null</code> if the argument was
     *            <code>null</code>.
     * @see IAbstractAsyncFuture#addCompletionListener(ICompletionListener, Object)
     */
    public void futureCompleted(IAbstractAsyncFuture result, Object userState);

}