/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.faulttolerance30.internal.context;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.context.ThreadContext;

import com.ibm.ws.microprofile.faulttolerance.spi.context.ContextSnapshot;

/**
 * ContextSnapshot which uses a context applying {@link Executor} to apply context
 * <p>
 * It's expected that this executor came from {@link ThreadContext#currentContextExecutor()}
 * and it's required that it runs tasks synchronously.
 */
public class ExecutorContextSnapshot implements ContextSnapshot {

    private final Executor contextExecutor;

    /**
     * @param contextExecutor
     */
    public ExecutorContextSnapshot(Executor contextExecutor) {
        this.contextExecutor = contextExecutor;
    }

    @Override
    public void runWithContext(Runnable runnable) {
        contextExecutor.execute(runnable);
    }

    @Override
    public <V> V runWithContext(Callable<V> callable) throws Exception {
        AtomicReference<V> result = new AtomicReference<>();
        AtomicReference<Exception> exception = new AtomicReference<>();

        contextExecutor.execute(() -> {
            try {
                result.set(callable.call());
            } catch (Exception e) {
                exception.set(e);
            }
        });

        Exception e = exception.get();
        if (e != null) {
            throw e;
        }

        return result.get();
    }

}
