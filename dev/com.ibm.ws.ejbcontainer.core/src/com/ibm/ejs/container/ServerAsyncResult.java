/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ejb.AsyncResult;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.EJBPMICollaborator;

/**
 * A <code>ServerAsyncResult</code> object packages the results of an
 * EJB asynchronous method call. This is the server-side result object. It may
 * be returned to clients within the server process, but it is not meant to be
 * returned to remote clients.
 *
 */
public abstract class ServerAsyncResult implements Future<Object> {
    private static final TraceComponent tc = Tr.register(ServerAsyncResult.class, "EJBContainer", "com.ibm.ejs.container.container");

    /**
     * The future object returned from the bean. This field will only be
     * non-null if ivDone is true and the bean returned non-null.
     */
    private Future<?> ivFuture; // d650178

    /**
     * The exception object when the async method call ends with exception.
     * This field is only non-null if ivDone is true.
     */
    private Throwable ivException = null;

    /**
     * True if the method was canceled prior to being executed. This field is
     * volatile because it is accessed unsynchronized from isCancelled.
     */
    private volatile boolean ivCancelled = false;

    /**
     * True if cancel(true) was called after the method had started executing.
     * This field is volatile because it is accessed unsynchronized from
     * wasCancelCalled. This field will never be true if ivCancelled is true.
     */
    private volatile boolean ivWasCancelCalled = false; // F743-11774

    /**
     * True if the method was canceled prior to being executed or if the method
     * finished executing (with or without an exception), which means that
     * ivCancelled, ivException, or ivResult must be set. This field is volatile
     * because it is accessed unsynchronized from isDone.
     */
    private volatile boolean ivDone = false;

    /**
     * Gate for blocking "get" calls until the method is either canceled or the
     * async method finishes executing on the work manager thread. The count is
     * "1" which says that only one other thread needs to free the gate, and all
     * threads waiting on it will begin executing again.
     */
    private final CountDownLatch ivGate = new CountDownLatch(1);

    /**
     * Performance data object for this wrapper.
     */
    public final EJBPMICollaborator ivPmiBean; // F743-22763

    /**
     * Construct the ServerAsyncResult to be used by the server for asynchronous method results.
     *
     * @param pmiBean - the performance data collection object.
     */
    public ServerAsyncResult(EJBPMICollaborator pmiBean) { // F743-22763
        this.ivPmiBean = pmiBean;
    }

    /**
     * Internal method used to signal that the final result of the async method
     * execution can be made available to clients. Prior to calling this
     * method, the caller must set ivCancelled, ivException, or ivResult.
     */
    protected void done() { // F743-11774  F743-15582
        ivDone = true;
        ivGate.countDown();
    }

    /**
     * Cancel the async method if possible. If the method is still waiting on a
     * work manager queue and has not been dispatched, we can cancel it.
     * Otherwise, it is not likely that the method will be able to stop
     * execution.<p>
     *
     * This method is releases the gate when cancel is successful
     * just in case there is another client thread waiting in a "get"
     * method below (this is not really expected to happen).
     *
     * @param mayInterruptIfRunning
     *
     * @return - true if the method was successfully canceled. Otherwise, false.
     */
    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "cancel - Future object: " + this, mayInterruptIfRunning);

        boolean cancelled = false;

        // F743-11774 - If we have not already canceled the work, then attempt
        // to remove it from the queue.  Note that if the work is removed from
        // the queue prior to executing, then there will be no opportunity for
        // wasCancelCalled to be called, so it is unnecessary to set
        // ivWasCancelCalled.
        if (!ivCancelled) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "attempting to remove from work queue");

            cancelled = doCancel();
            if (cancelled) {
                ivCancelled = true;
                done();
                // F743-22763
                if (ivPmiBean != null) {
                    ivPmiBean.asyncMethodCallCanceled();
                    ivPmiBean.asyncQueSizeDecrement();
                }
            } else {
                ivWasCancelCalled = mayInterruptIfRunning;
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "cancel", cancelled);

        return (cancelled);
    }

    /**
     * Attempt to cancel the asynchronous method associated with this
     * ServerAsyncResult. <p>
     *
     * The mechanism for canceling submitted asynchronous methods will
     * vary depending on the service used to manage the asynchronous
     * threads (i.e. WorkManager or Executor). A subclass implementation
     * is required for each supported service. <p>
     *
     * @return true if the asynchronous method could be removed from the
     *         queue of work before beginning to run.
     */
    protected abstract boolean doCancel();

    /**
     * Returns true if cancel(true) was called after the method had begun
     * execution.
     */
    public boolean wasCancelCalled() { // F743-11774
        return ivWasCancelCalled;
    }

    /**
     * This get method returns the result of the async method call if it is
     * available. Otherwise, it blocks until the result is available. It is
     * unblocked when the Work object that runs the async method on a work
     * manager finishes (i.e. with either good results or an exception), and
     * sets results on this instance.<p>
     *
     * @return - the result object
     *
     * @throws InterruptedException - if the thread is interrupted while waiting
     * @throws CancellationException - if the async method was canceled successfully
     * @throws ExecutionException - if the async method ended with an exception
     */
    @Override
    public Object get() throws InterruptedException, ExecutionException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "get - Future object: " + this);
        }

        await(0, null); // F16043
        Object result = getResult(); // d650178

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "get: " + Util.identity(result));
        return result;
    }

    /**
     * This get method returns the result of the async method call. This method
     * must not be called unless ivGate indicates that results are available.
     *
     * @return - the result object
     *
     * @throws InterruptedException - if the thread is interrupted while waiting
     * @throws CancellationException - if the async method was canceled successfully
     * @throws ExecutionException - if the async method ended with an exception
     */
    private Object getResult() throws InterruptedException, ExecutionException {
        if (ivCancelled) { // F743-11774
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getResult: throwing CancellationException");
            }
            throw new CancellationException(); // F743-11774
        }

        // Method ended with an exception
        if (ivException != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getResult: " + ivException);
            }
            throw new ExecutionException(ivException);
        }

        // F743-609CodRev
        // If the result object is itself a Future object, we need to call "get" on it
        // so that we unwrap the results and place them in this Future object.  This
        // is done to support asynchronous method calls that return results wrapped
        // in Future objects, and also to support nested asynchronous method calls.
        // Also, note that "null" is an acceptable result.
        // F743-16193
        // Remove instanceof check for Future object.  Return type validation check
        // moved to EJBMDOrchestrator.java
        Object resultToReturn = null;
        if (ivFuture != null) {
            resultToReturn = ivFuture.get(); // d650178
        }

        return (resultToReturn);
    }

    /**
     * This get method returns the result of the asynch method call. This method
     * must not be called unless ivGate indicates that results are available.
     *
     * @param timeout - the timeout value
     * @param unit - the time unit for the timeout value (e.g. milliseconds, seconds, etc.)
     *
     * @return - the result object
     *
     * @throws InterruptedException - if the thread is interrupted while waiting
     * @throws CancellationException - if the async method was canceled successfully
     * @throws ExecutionException - if the async method ended with an exception
     * @throws TimeoutException - if the timeout period expires before the asynch method completes
     */
    private Object getResult(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (ivCancelled) { // F743-11774
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getResult: throwing CancellationException");
            }
            throw new CancellationException(); // F743-11774
        }

        // Method ended with an exception
        if (ivException != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getResult: " + ivException);
            }
            throw new ExecutionException(ivException);
        }

        // F743-609CodRev
        // If the result object is itself a Future object, we need to call "get" on it
        // so that we unwrap the results and place them in this Future object.  This
        // is done to support asynchronous method calls that return results wrapped
        // in Future objects, and also to support nested asynchronous method calls.
        // Also, note that "null" is an acceptable result.
        // F743-16193
        // Remove instanceof check for Future object.  Return type validation check
        // moved to EJBMDOrchestrator.java
        Object resultToReturn = null;
        if (ivFuture != null) {
            // AsyncResult EJB3.2 API just throws IllegalStateExceptions for everything but .get().
            // Even in EJB3.1 API get(timeout, unit) just immediately returned. In a long nested
            // chain of futures, only the last one will be AsyncResult so we won't need to pass
            // down the remaining timeout anyway.
            if (ivFuture instanceof AsyncResult) {
                resultToReturn = ivFuture.get();
            } else {
                resultToReturn = ivFuture.get(timeout, unit); // d650178
            }
        }

        return (resultToReturn);
    }

    protected boolean await(long timeout, TimeUnit unit) throws InterruptedException { // F16043
        // Block if the async method is not done executing on the work manager.
        // Proceed when it finishes or the timeout occurs.  If it is already done,
        // proceed right away.

        if (unit == null) {
            ivGate.await();
            return true;
        }

        return ivGate.await(timeout, unit);
    }

    /**
     * This get method returns the result of the async method call if it is
     * available. Otherwise, it blocks until the result is available, or the
     * timeout expires. It is unblocked when the Work object that runs the async
     * method on a work manager finishes and sets results on this instance, or
     * when the timeout expires.<p>
     *
     * @param timeout - the timeout value
     * @param unit - the time unit for the timeout value (e.g. milliseconds, seconds, etc.)
     *
     * @return - the result object
     *
     * @throws InterruptedException - if the thread is interrupted while waiting
     * @throws CancellationException - if the async method was canceled successfully
     * @throws ExecutionException - if the async method ended with an exception
     * @throws TimeoutException - if the timeout period expires before the async method completes
     */
    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) {
            String completeTime = "Timeout setting: " + timeout + " " + unit;
            Tr.entry(tc, "get - " + completeTime + " Future object: " + this);
        }

        if (unit == null) { // F16043
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "get - null unit");
            throw new NullPointerException("unit");
        }

        long startTime = System.nanoTime();

        if (!await(timeout, unit)) { // F16043
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "get - asynchronous method timed out, throwing TimeoutException.");
            throw new TimeoutException();
        }

        long remainingTime = timeout - unit.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);

        Object resultToReturn = getResult(remainingTime, unit); // d650178

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "get: " + Util.identity(resultToReturn));
        }
        return (resultToReturn);
    }

    /**
     * This method allows clients to check the Future object to see if the asynch
     * method was canceled before it got a chance to execute.
     */
    @Override
    public boolean isCancelled() {

        //F743-609CodRev - read volatile variable only once
        boolean cancelled = ivCancelled;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "isCancelled: " + cancelled + " Future object: " + this);
        }

        return (cancelled);
    }

    /**
     * This method allows clients to poll the Future object and only get results
     * once the async method has finished (i.e. either with good results or an
     * exception).
     */
    @Override
    public boolean isDone() {

        //F743-609CodRev - read volatile variable only once
        boolean done = ivDone;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "isDone: " + done + " Future object: " + this);
        }

        return (done);
    }

    /****************************************************************************
     * The following two methods are used by the async method work objects
     * running on the work manager to return results. They are not part of the
     * java.util.concurrency.Future interface so clients will not be able to call
     * them. The methods call the gate to unblock any client threads that
     * may be stuck waiting in the "get" methods above.
     ****************************************************************************/

    // The async method finished with a good result.  It was not canceled, and it
    // did not throw an exception.
    void setResult(Future<?> theFuture) { // d650178

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setResult: " + Util.identity(theFuture) + " Future object: " + this);
        }

        // set result, we are done
        ivFuture = theFuture;
        done(); // F743-11774
    }

    // The async method ended with an exception
    void setException(Throwable theException) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setException - Future object: " + this, theException);
        }

        // set exception, we are done
        ivException = theException;
        done(); // F743-11774
    }

} // ServerAsyncResult
