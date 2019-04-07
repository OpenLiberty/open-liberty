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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 *
 */
public class QueuedFuture<R> implements Future<R>, Runnable {

    private static final TraceComponent tc = Tr.register(QueuedFuture.class);

    //innerTask is the task which will be run on the new thread
    private Callable<Future<R>> innerTask;

    /**
     * Future which will be completed with either the result or an exception
     * <p>
     * This will either complete when the task ends or when {@link #abort(Throwable)} is called
     */
    private CompletableFuture<Future<R>> resultFuture;

    /**
     * Future which controls the asynchronous execution of {@link #innerTask}
     * <p>
     * It's the future we got back from the execution service and can be used to cancel the execution.
     * <p>
     * Don't use it to get the result, use {@link #resultFuture} instead.
     */
    private Future<?> taskFuture;

    //threadContext is the context retrieved from the originating thread
    private ThreadContextDescriptor threadContext;

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore({ ExecutionException.class })
    public boolean cancel(boolean mayInterruptIfRunning) {
        Future<Future<R>> resultFuture = getResultFuture();
        boolean cancellationResult = resultFuture.cancel(false); // Sets the result to be a CancellationException

        // If cancellationResult is true, then either we've just cancelled the task, or it was already cancelled
        if (cancellationResult) {
            getTaskFuture().cancel(mayInterruptIfRunning); // Attempt to cancel the queued/running task
        } else if (resultFuture.isDone() && !resultFuture.isCancelled()) {
            try {
                Future<R> innerFuture = resultFuture.get();
                cancellationResult = innerFuture.cancel(mayInterruptIfRunning);
            } catch (InterruptedException e) {
                //outerFuture was done so we should never get an interrupted exception
                throw new FaultToleranceException(e);
            } catch (ExecutionException e) {
                //this is most likely to be caused if an exception is thrown from the business method ... or a TimeoutException
                //either way, the future cannot be cancelled
                cancellationResult = false;
            }
        }

        return cancellationResult;
    }

    /**
     * Complete the result future with the given exception and attempt to cancel the task.
     * <p>
     * This is typically used as part of timeout.
     */
    public void abort(Throwable t) {
        // Set the result to the given exception
        boolean abortSuccessful = getResultFuture().completeExceptionally(t);

        if (abortSuccessful) {
            // Attempt to abort the task
            getTaskFuture().cancel(true);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore({ ExecutionException.class })
    public boolean isCancelled() {
        Future<Future<R>> resultFuture = getResultFuture();
        if (resultFuture.isDone()) {
            if (resultFuture.isCancelled()) {
                return true;
            } else {
                try {
                    Future<R> innerFuture = resultFuture.get();
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
        Future<Future<R>> resultFuture = getResultFuture();
        if (resultFuture.isDone()) {
            if (resultFuture.isCancelled()) {
                return true;
            } else {
                try {
                    Future<R> innerFuture = resultFuture.get();
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
    @FFDCIgnore({ ExecutionException.class })
    public R get() throws InterruptedException, ExecutionException {
        R result = null;
        Future<Future<R>> resultFuture = getResultFuture();

        try {
            result = resultFuture.get().get();
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
    @FFDCIgnore({ ExecutionException.class })
    public R get(long methodTimeout, TimeUnit methodUnit) throws InterruptedException, ExecutionException, TimeoutException {
        R result = null;
        Future<Future<R>> resultFuture = getResultFuture();

        try {
            long start = System.nanoTime();
            long methodNanos = TimeUnit.NANOSECONDS.convert(methodTimeout, methodUnit);
            Future<R> innerFuture = resultFuture.get(methodNanos, TimeUnit.NANOSECONDS);
            long middle = System.nanoTime();
            long diff = middle - start;
            long remaining = methodNanos - diff;
            result = innerFuture.get(remaining, TimeUnit.NANOSECONDS);
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
     * Run the task and store the result in the {@link #resultFuture}
     */
    @Override
    @FFDCIgnore(Throwable.class)
    public void run() {
        Future<R> result = null;
        Throwable thrownException = null;

        try {
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
        } catch (Throwable e) {
            if (e instanceof com.ibm.ws.microprofile.faulttolerance.spi.ExecutionException) {
                thrownException = e.getCause();
            } else {
                thrownException = e;
            }
        } finally {
            // We've finished running and caught any exceptions that occurred, now store the result in the resultFuture
            // If abort() has already been called then the result future will already be completed and the result (or exception) here is discarded.
            if (thrownException == null) {
                resultFuture.complete(result);
            } else {
                resultFuture.completeExceptionally(thrownException);
            }
        }
    }

    private CompletableFuture<Future<R>> getResultFuture() {
        synchronized (this) {
            if (this.resultFuture == null) {
                //shouldn't be possible unless the QueuedFuture was created but not started
                throw new IllegalStateException(Tr.formatMessage(tc, "internal.error.CWMFT4999E"));
            }
        }
        return this.resultFuture;
    }

    private Future<?> getTaskFuture() {
        synchronized (this) {
            if (this.taskFuture == null) {
                //shouldn't be possible unless the QueuedFuture was created but not started
                throw new IllegalStateException(Tr.formatMessage(tc, "internal.error.CWMFT4999E"));
            }
        }
        return this.taskFuture;
    }

    /**
     * Submit this task for execution by the given executor service.
     *
     * @param executorService the executor service to use
     * @param innerTask the task to run
     * @param threadContext the thread context to apply when running the task
     */
    public void start(ExecutorService executorService, Callable<Future<R>> innerTask, ThreadContextDescriptor threadContext) {
        synchronized (this) {
            this.innerTask = innerTask;
            this.threadContext = threadContext;
            //set up the future to hold the result
            this.resultFuture = new CompletableFuture<Future<R>>();
            //submit the innerTask (wrapped by this) for execution
            this.taskFuture = executorService.submit(this);
        }
    }
}
