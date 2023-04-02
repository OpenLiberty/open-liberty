/*******************************************************************************
 * Copyright (c) 2012,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jca.workcontext;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.RejectedExecutionException;

import jakarta.resource.spi.work.WorkCompletedException;
import jakarta.resource.spi.work.WorkContextErrorCodes;
import jakarta.resource.spi.work.WorkContextLifecycleListener;

import com.ibm.wsspi.threadcontext.ThreadContext;

/**
 * This a fake thread context that we made up for testing purposes.
 * It's just a per-thread java.util.Collection that can be propagated
 * from one thread to another via the context propagation service.
 */
public class CollectionThreadContext extends LinkedList<String> implements ThreadContext {
    private static final long serialVersionUID = 4033660927662810157L;

    public static final String WORK_THAT_FAILS_CONTEXT_SETUP = "WorkThatFailsContextSetup";

    private final String identityName;
    private final WorkContextLifecycleListener listener;

    /**
     * Default collection context is empty.
     */
    CollectionThreadContext() {
        super();
        this.listener = null;
        this.identityName = null;
    }

    /**
     * Construct collection context based on an existing collection
     * 
     * @param source an existing collection
     * @param listener receives notifications when work context is applied (or fails to apply) to a thread
     * @param identityName optional name of the contextual task
     */
    CollectionThreadContext(Collection<String> source, WorkContextLifecycleListener listener, String identityName) {
        this.listener = listener;
        this.identityName = identityName;
        addAll(source);
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext clone() {
        return (CollectionThreadContext) super.clone();
    }

    /**
     * <p>Establishes context on the current thread.
     * When this method is used, expect that context will later be removed and restored
     * to its previous state via operationStopping.
     * 
     * <p>This method should fail if the context cannot be established on the thread.
     * In the event of failure, any partially applied context must be removed before this method returns.
     * 
     * @throws RejectedExecutionException if context cannot be propagated to the thread.
     */
    @Override
    public void taskStarting() throws RejectedExecutionException {

        try {
            if (WORK_THAT_FAILS_CONTEXT_SETUP.equals(identityName))
                throw new RejectedExecutionException(new WorkCompletedException("Intentionally caused failure", WorkContextErrorCodes.CONTEXT_SETUP_FAILED));

            CollectionContextProvider.threadlocal.get().push(new CollectionThreadContext(this, listener, identityName));
        } catch (Error x) {
            if (listener != null)
                listener.contextSetupFailed(WorkContextErrorCodes.CONTEXT_SETUP_FAILED);
            throw x;
        } catch (RuntimeException x) {
            if (listener != null)
                listener.contextSetupFailed(WorkContextErrorCodes.CONTEXT_SETUP_FAILED);
            throw x;
        }

        if (listener != null)
            listener.contextSetupComplete();
    }

    /**
     * <p>Restore the thread to its previous state from before the most recently applied context.
     */
    @Override
    public void taskStopping() {

        CollectionContextProvider.threadlocal.get().pop();
    }
}