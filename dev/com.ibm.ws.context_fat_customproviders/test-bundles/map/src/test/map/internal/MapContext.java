/*******************************************************************************
 * Copyright (c) 2012,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.map.internal;

import java.util.HashMap;
import java.util.concurrent.RejectedExecutionException;

import com.ibm.wsspi.threadcontext.ThreadContext;

/**
 * This a fake thread context that we made up for testing purposes.
 * It's just a per-thread java.util.HashMap that can be propagated
 * from one thread to another via the context propagation service.
 */
public class MapContext extends HashMap<String, String> implements ThreadContext {
    /**  */
    private static final long serialVersionUID = -183468135746876387L;

    /**
     * Default map context is an empty map.
     */
    public MapContext() {
        super();
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext clone() {
        return (ThreadContext) super.clone();
    }

    /**
     * <p>Establishes context on the current thread.
     * When this method is used, expect that context will later be removed and restored
     * to its previous state via operationStopping.
     *
     * <p>This method should fail if the context cannot be established on the thread.
     * In the event of failure, any partially applied context must be removed before this method returns.
     *
     * @throws RejectedExecutionException if the context provider isn't available.
     */
    @Override
    public void taskStarting() throws RejectedExecutionException {
        MapService.threadlocal.get().push((MapContext) this.clone());
    }

    /**
     * <p>Restore the thread to its previous state from before the most recently applied context.
     */
    @Override
    public void taskStopping() {
        // Remove most recent, which restores the previous
        MapService.threadlocal.get().pop();
    }
}