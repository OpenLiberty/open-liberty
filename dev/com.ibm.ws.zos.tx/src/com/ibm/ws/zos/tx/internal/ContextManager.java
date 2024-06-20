/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.tx.internal;

import com.ibm.ws.Transaction.UOWCoordinator;

/**
 * Internal interface for the context manager used by the native transaction
 * manager. This interface is not intended to be used outside of the
 * com.ibm.ws.zos.tx component.
 */
public interface ContextManager {

    /**
     * Begins a new work context on this task.
     *
     * @param coord The UOWCoordinator currently running on this task, who is
     *                  requesting the context.
     */
    public void begin(UOWCoordinator coord);

    /**
     * Ends the current work unit on this task.
     *
     * @param coord The UOWCoordinator which has just ended. The coordinator
     *                  may or may not be current on this task. The context manager
     *                  will check to verify that the context being ended was
     *                  started by the same UOWCoodinator.
     */
    public void end(UOWCoordinator coord);

    /**
     * Suspends the current work unit on this task.
     *
     * @param coord The current UOWCoordinator on this task. This must also be
     *                  the UOWCoordinator which started the current context on this
     *                  task.
     */
    public void suspend(UOWCoordinator coord);

    /**
     * Resumes the work unit associated with the given unit of work.
     *
     * @param coord The coordinator which is being resumed on this task. The
     *                  context currently associated with this coordinator will be
     *                  resumed.
     */
    public void resume(UOWCoordinator coord);

    /**
     * Initializes the context manager with the information it needs to create
     * contexts and handle new work.
     *
     * @param rmToken The resource manager token to use to create contexts.
     */
    public void initialize(byte[] rmToken);

    /**
     * Tells the caller if the context manager is initialized.
     */
    public boolean isInitialized();

    /**
     * Retrieves the context object reference from the current thread.
     *
     * @return The context currently attached to the calling thread.
     */
    public Context getCurrentContext();

    /**
     * Destroy the context manager and any contexts which are currently
     * pooled.
     *
     * @param timeoutMillis The number of milliseconds that the context manager
     *                          is allowed to wait before forcing a shutdown if
     *                          some contexts are dirty.
     */
    public void destroyContextManager(long timeoutMillis);
}