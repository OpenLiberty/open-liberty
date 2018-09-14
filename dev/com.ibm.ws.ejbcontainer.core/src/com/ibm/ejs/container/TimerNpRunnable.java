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

import com.ibm.ejs.container.util.ExceptionUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * A task handler for all non-persistent EJB timers. <p>
 *
 * This abstract class provides the common logic to run EJB timers when
 * they reach their scheduled expiration. A subclass must be provided
 * for each supported scheduling service. </p>
 */
public abstract class TimerNpRunnable implements Runnable {
    private static final String CLASS_NAME = TimerNpRunnable.class.getName();
    private static final TraceComponent tc = Tr.register(TimerNpRunnable.class, "EJBContainer", "com.ibm.ejs.container.container");

    private BeanId ivBeanId;
    private final int ivMethodId; // F743-506
    private long ivRetries = 0; // 591279 597753
    private final int ivRetryLimit; // 591279
    private final long ivRetryInterval; // in ms 591279
    protected final TimerNpImpl ivTimer;

    private static volatile boolean serverStopping;

    public TimerNpRunnable(TimerNpImpl timerNpImpl, int retryLimit, long retryInterval) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init>: " + timerNpImpl);

        ivBeanId = timerNpImpl.getIvBeanId();
        ivMethodId = timerNpImpl.ivMethodId; // F743-506
        ivTimer = timerNpImpl;
        ivRetryLimit = retryLimit; // 591279 596293
        ivRetryInterval = retryInterval; // 591279
    }

    public static void serverStopping() {
        serverStopping = true;
    }

    /**
     * Overridden to improve trace.
     **/
    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + ivTimer.ivTaskId + ", " + ivBeanId + ", " + ivRetries + ")";
    }

    /**
     * Executes the timer work with configured retries.
     *
     * The EJB 3.1 spec, section 18.4.3 says, "If the transaction fails or
     * is rolled back, the container must retry the timeout at least once."
     * We allow the user to configure a retry count of 0, which will cause
     * NO retries to be performed. If the retry count is not set, we will
     * retry once immediately, then every retryInterval, indefinitely.
     */
    @Override
    public void run() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) // F743-425.CodRev
            Tr.entry(tc, "run: " + ivTimer.ivTaskId); // F743-425.CodRev

        if (serverStopping) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "Server shutting down; aborting");
            return;
        }

        if (ivRetries == 0) // This is the first try
        {
            // F743-7591 - Calculate the next expiration before calling the timeout
            // method.  This ensures that Timer.getNextTimeout will properly throw
            // NoMoreTimeoutsException.
            ivTimer.calculateNextExpiration();
        }

        // Log a warning if this timer is starting late
        ivTimer.checkLateTimerThreshold();

        try // F743-425.CodRev
        {
            // Call the timeout method; last chance effort to abort if cancelled
            if (!ivTimer.isIvDestroyed()) {
                doWork();
            } else {
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "Timer has been cancelled; aborting");
                return;
            }
            ivRetries = 0;

            // re-schedule the alarm to go off again if it needs to,
            // and if timer had not been canceled
            ivTimer.scheduleNext(); // RTC107334
        } catch (Throwable ex) // F743-425.CodRev
        {
            // Do not FFDC... that has already been done when the method failed
            // All exceptions from timeout methods are system exceptions...
            // indicating the timeout method failed, and should be retried. d667153
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "NP Timer failed : " + ex.getClass().getName() + ":" + ex.getMessage(), ex);
            }

            if ((ivRetryLimit != -1) && // not configured to retry indefinitely
                (ivRetries >= ivRetryLimit)) // and retry limit reached
            {
                // Note: ivRetryLimit==0 means no retries at all

                ivTimer.calculateNextExpiration();
                ivTimer.scheduleNext();
                ivRetries = 0;
                Tr.warning(tc, "NP_TIMER_RETRY_LIMIT_REACHED_CNTR0179W", ivRetryLimit);
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "Timer retry limit has been reached; aborting");
                return;
            }

            ivRetries++;

            // begin 597753
            if (ivRetries == 1) {
                // do first retry immediately, by re-entering this method
                run();
            } else {
                // re-schedule the alarm to go off after the retry interval
                // (if timer had not been canceled)
                ivTimer.scheduleRetry(ivRetryInterval); // RTC107334
            }
        }

        if (isTraceOn && tc.isEntryEnabled()) // F743-425.CodRev
            Tr.exit(tc, "run"); // F743-425.CodRev
    } // end doWorkWithRetries

    /**
     * Performs the work associated with the scheduled EJB Timer. <p>
     *
     * When the EJB Timer expires, this method will be called. A wrapper for
     * the target TimedObject bean will be obtained, and the ejbTimeout method
     * will be invoked. <p>
     *
     * The wrapper is similar to a generated Local wrapper, and will activate
     * the bean as needed. It will also perform exception handling /
     * mapping as required by the EJB Specification.
     **/
    private void doWork() {
        boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "doWork: " + ivTimer);

        TimedObjectWrapper timedObject;
        try {
            ivBeanId = ivBeanId.getInitializedBeanId(); // F743-506CodRev
        } catch (Exception ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".doWork", "247", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "doWork: " + ex);
            throw ExceptionUtil.EJBException(ex);
        }
        EJSHome home = (EJSHome) ivBeanId.home;

        // Get the TimedObjectWrapper from the pool to execute the method.
        timedObject = home.getTimedObjectWrapper(ivBeanId);

        // Invoke ejbTimeout on the wrapper.  No need to handle exceptions,
        // as the wrapper will have already handled/mapped any exceptions
        // as expected.
        try {
            ivTimer.ivTimeoutThread = Thread.currentThread(); // d595255
            timedObject.invokeCallback(ivTimer, ivMethodId, false); // F743-506
        } finally {
            ivTimer.ivTimeoutThread = null; // d595255
            // Always return the TimedObjectWrapper to the pool.
            home.putTimedObjectWrapper(timedObject);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "doWork");
        }
    }

    /**
     * Schedules the first expiration for a non-persistent timer.
     *
     * @param expiration the point in time at which the first scheduled
     *            expiration must occur; specified in milliseconds since
     *            the standard base time (the epoch).
     */
    protected abstract void schedule(long expiration);

    /**
     * Schedules recurring timers for the next scheduled expiration.
     *
     * @param nextExpiration the point in time at which the next scheduled
     *            expiration must occur; specified in milliseconds since
     *            the standard base time (the epoch).
     */
    protected abstract void scheduleNext(long nextExpiration);

    /**
     * Schedules a timer that has failed to run to be retried.
     *
     * @param retryInterval the time in milliseconds from the current time when
     *            the timer should expire and attempt to run again.
     */
    protected abstract void scheduleRetry(long retryInterval);

    /**
     * Cancels the timer task and all future invocations. <p>
     *
     * If the timer task has already started to run, then this method
     * has no effect. If the timer task has no further expirations or
     * has previously been cancelled, then this method has no effect. <p>
     *
     * Aside from canceling a timer, implementations of this method
     * may also perform resource cleanup; depending on the underlying
     * scheduler implementation.
     */
    protected abstract void cancel();

}
