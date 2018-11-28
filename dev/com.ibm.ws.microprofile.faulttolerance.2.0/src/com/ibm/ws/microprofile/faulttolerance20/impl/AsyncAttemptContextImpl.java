/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.impl;

import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.ws.microprofile.faulttolerance20.state.TimeoutState;

/**
 * Stores context for one async retry attempt
 *
 * @param <W> the return type of the code being executed, which is also the type of the return wrapper (e.g. {@code Future<String>})
 */
public class AsyncAttemptContextImpl<W> {

    private final AsyncExecutionContextImpl<W> executionContext;
    private TimeoutState timeoutState;
    private final AtomicBoolean complete = new AtomicBoolean(false);

    public AsyncAttemptContextImpl(AsyncExecutionContextImpl<W> executionContext) {
        this.executionContext = executionContext;
    }

    public AsyncExecutionContextImpl<W> getExecutionContext() {
        return executionContext;
    }

    public TimeoutState getTimeoutState() {
        return timeoutState;
    }

    public void setTimeoutState(TimeoutState timeoutState) {
        this.timeoutState = timeoutState;
    }

    /**
     * Mark that the attempt has ended
     * <p>
     * This method exists to ensure that any end-of-attempt logic is run exactly once.
     * <p>
     * The first time this method is called, it will return {@code true}, all subsequent calls will return false
     *
     * @return {@code true} if {@code end()} had not been called previously, {@code false} otherwise
     */
    public boolean end() {
        return complete.compareAndSet(false, true);
    }
}
