/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.threadcontext;

import java.io.Serializable;
import java.util.concurrent.RejectedExecutionException;

/**
 * <p>Serializable captured thread context.
 * 
 * <p>The consumer of this interface may choose to (but is not required to) serialize it
 * (for example, to a database or file) and reuse it multiple times on any threads
 * (although only serially, never concurrently) at any future points in time, including across release
 * boundaries or on different servers.
 * 
 * <p>A thread context implementation must be capable of handling all of these usage patterns.
 */
public interface ThreadContext extends Cloneable, Serializable {
    /**
     * Clones thread context. Thread context is cloned before applying context concurrently to threads
     * so that a ThreadContext instance may reliably store state information that is needed to
     * restore the previous context after the contextual operation ends.
     * The clone method should not copy state information for restoring previous thread context.
     * 
     * @return copy of thread context.
     */
    ThreadContext clone();

    /**
     * <p>Establishes context on the current thread.
     * When this method is used, expect that context will later be removed and restored
     * to its previous state via taskStopping.
     * 
     * <p>This method should fail if the context cannot be established on the thread.
     * In the event of failure, any partially applied context must be removed before this method returns.
     * 
     * @throws RejectedExecutionException if context cannot be established on the thread.
     */
    void taskStarting() throws RejectedExecutionException;

    /**
     * <p>Restore the thread to its previous state from before the most recently applied context.
     */
    void taskStopping();
}