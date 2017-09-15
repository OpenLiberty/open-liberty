/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.impl.async;

import java.util.concurrent.Callable;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;

import com.ibm.ws.microprofile.faulttolerance.impl.ExecutionContextImpl;
import com.ibm.ws.microprofile.faulttolerance.impl.TaskRunner;
import com.ibm.ws.microprofile.faulttolerance.impl.sync.SimpleTaskRunner;
import com.ibm.ws.microprofile.faulttolerance.impl.sync.SynchronousExecutorImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.Executor;

/**
 *
 */
public class AsyncInnerExecutorImpl<R> extends SynchronousExecutorImpl<R> implements Executor<R> {

    private final TaskRunner<R> taskRunner;

    //internal constructor for the nested synchronous part of an asynchronous execution
    public AsyncInnerExecutorImpl() {
        this.taskRunner = new SimpleTaskRunner<>();
    }

    @Override
    protected Callable<R> createTask(Callable<R> callable, ExecutionContextImpl executionContext) {
        Callable<R> task = () -> {
            R result = this.taskRunner.runTask(callable, executionContext);
            return result;
        };
        return task;
    }

    @Override
    public R execute(Callable<R> callable, ExecutionContext executionContext) {
        ExecutionContextImpl executionContextImpl = (ExecutionContextImpl) executionContext;
        executionContextImpl.setNested();
        R result = super.execute(callable, executionContextImpl);
        return result;
    }

    @Override
    protected void preRun(ExecutionContextImpl executionContext) {
        // Overridden to avoid running executionContext.start() as that was already run by AsyncOuterExecutorImpl
    }
}
