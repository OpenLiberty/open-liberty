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

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.faulttolerance.impl.ExecutionContextImpl;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 *
 */
public class QueuedFuture<R> implements Future<R>, Callable<Future<R>> {

    private static final TraceComponent tc = Tr.register(QueuedFuture.class);

    //innerTask is the task which will be run on the new thread
    private final Callable<Future<R>> innerTask;
    //outerFuture is the Future which represents the execution of the innerTask
    private Future<Future<R>> outerFuture;
    //threadContext is the context retrieved from the originating thread
    private final ThreadContextDescriptor threadContext;
    //executionContext is the overall FT execution context, covering both synchronous halves of the asynchronous execution
    private final ExecutionContextImpl executionContext;
    //has the user called the cancel method
    private boolean cancelled = false;
    private boolean internallyCancelled = false;

    public QueuedFuture(Callable<Future<R>> innerTask, ExecutionContextImpl executionContext, ThreadContextDescriptor threadContext) {
        this.innerTask = innerTask;
        this.executionContext = executionContext;
        this.threadContext = threadContext;
    }

    /** {@inheritDoc} */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        synchronized (this) {
            if (this.cancelled) {
                return false; //if already cancelled then this call should fail
            } else if (this.internallyCancelled) {
                this.cancelled = true; //if this was internally cancelled then just set the flag and return true
                return true;
            } else {
                //go ahead and cancel, return the result, setting the flags accordingly
                boolean outerCancelled = getOuterFuture().cancel(mayInterruptIfRunning);
                this.cancelled = outerCancelled;
                this.internallyCancelled = outerCancelled;
                return outerCancelled;
            }

        }
    }

    /**
     * Cancel the inner future and interrupt if running but do not set the cancelled flag for this QueuedFuture.
     * This is typically used as part of timeout.
     */
    public void internalCancel() {
        synchronized (this) {
            if (!this.internallyCancelled) {
                boolean outerCancelled = getOuterFuture().cancel(true);
                this.internallyCancelled = outerCancelled;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCancelled() {
        Future<Future<R>> outerFuture = getOuterFuture();
        if (outerFuture.isDone()) {
            if (outerFuture.isCancelled()) {
                return true;
            } else {
                try {
                    Future<R> innerFuture = outerFuture.get();
                    return innerFuture.isCancelled();
                } catch (InterruptedException e) {
                    //outerFuture was done so we should never get an interrupted exception
                    throw new FaultToleranceException(e);
                } catch (ExecutionException e) {
                    //this is most likely to be caused if an exception is thrown from the business method ... or a TimeoutException
                    //either way, the future was not cancelled
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore({ ExecutionException.class })
    public boolean isDone() {
        Future<Future<R>> outerFuture = getOuterFuture();
        if (outerFuture.isDone()) {
            if (outerFuture.isCancelled()) {
                return true;
            } else {
                try {
                    Future<R> innerFuture = outerFuture.get();
                    return innerFuture.isDone();
                } catch (InterruptedException e) {
                    //outerFuture was done so we should never get an interrupted exception
                    throw new FaultToleranceException(e);
                } catch (ExecutionException e) {
                    //this is most likely to be caused if an exception is thrown from the business method ... or a TimeoutException
                    //either way, the future is done
                    return true;
                }
            }
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public R get() throws InterruptedException, ExecutionException {
        R result = null;
        Future<Future<R>> outerFuture = getOuterFuture();

        try {
            executionContext.check();
        } catch (org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException te) {
            throw new ExecutionException(te);
        }

        try {
            result = outerFuture.get().get();
        } catch (InterruptedException | CancellationException e) {
            //if the future was interrupted or cancelled, check if it was because the FT Timeout popped
            try {
                executionContext.check();
            } catch (org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException te) {
                throw new ExecutionException(te);
            }

            throw e;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof com.ibm.ws.microprofile.faulttolerance.spi.ExecutionException) {
                throw new ExecutionException(e.getCause().getCause());
            } else {
                throw e;
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore({ CancellationException.class, org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException.class })
    public R get(long methodTimeout, TimeUnit methodUnit) throws InterruptedException, ExecutionException, TimeoutException {
        R result = null;
        Future<Future<R>> outerFuture = getOuterFuture();

        try {
            executionContext.check();
        } catch (org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException te) {
            throw new ExecutionException(te);
        }

        try {
            long start = System.nanoTime();
            long methodNanos = TimeUnit.NANOSECONDS.convert(methodTimeout, methodUnit);
            Future<R> innerFuture = outerFuture.get(methodNanos, TimeUnit.NANOSECONDS);
            long middle = System.nanoTime();
            long diff = middle - start;
            long remaining = methodNanos - diff;
            result = innerFuture.get(remaining, TimeUnit.NANOSECONDS);
        } catch (InterruptedException | CancellationException e) {
            //if the future was interrupted or cancelled, check if it was because the FT Timeout popped
            try {
                executionContext.check();
            } catch (org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException te) {
                throw new ExecutionException(te);
            }

            throw e;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof com.ibm.ws.microprofile.faulttolerance.spi.ExecutionException) {
                throw new ExecutionException(e.getCause().getCause());
            } else {
                throw e;
            }
        }
        return result;

    }

    /**
     * @return
     * @throws Exception
     */
    @Override
    public Future<R> call() throws Exception {
        Future<R> result = null;

        ArrayList<ThreadContext> contextAppliedToThread = null;
        if (this.threadContext != null) {
            //apply the JEE contexts to the thread before calling the inner task
            contextAppliedToThread = this.threadContext.taskStarting();
        }
        try {
            result = innerTask.call();
        } finally {
            if (contextAppliedToThread != null) {
                //remove the JEE contexts again since the thread will be re-used
                this.threadContext.taskStopping(contextAppliedToThread);
            }
        }
        return result;
    }

    private Future<Future<R>> getOuterFuture() {
        synchronized (this) {
            if (this.outerFuture == null) {
                //shouldn't be possible unless the QueuedFuture was created but not started
                throw new IllegalStateException(Tr.formatMessage(tc, "internal.error.CWMFT4999E"));
            }
        }
        return this.outerFuture;
    }

    /**
     * Submit this task for execution by the given executor service.
     *
     * @param executorService
     */
    public void start(ExecutorService executorService) {
        synchronized (this) {
            //submit the innerTask (wrapped by this) for execution
            this.outerFuture = executorService.submit(this);
        }
    }
}
