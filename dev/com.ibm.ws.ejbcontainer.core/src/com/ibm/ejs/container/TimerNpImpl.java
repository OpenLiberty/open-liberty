/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ejb.EJBException;
import javax.ejb.NoMoreTimeoutsException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerHandle;

import com.ibm.ejs.container.passivator.PassivatorSerializable;
import com.ibm.ejs.container.passivator.PassivatorSerializableHandle;
import com.ibm.ejs.csi.EJBApplicationMetaData;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.ejs.util.Util;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.util.ParsedScheduleExpression;
import com.ibm.ws.ejbcontainer.util.ScheduleExpressionParser;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.util.UUID;

/**
 * The TimerNpImpl provides an implementation of the javax.ejb.Timer interface
 * for non-persistent EJB timers and contains information about a timer that
 * was created through the EJB Timer Service. <p>
 **/
public final class TimerNpImpl implements Timer, PassivatorSerializable {
    private static final TraceComponent tc = Tr.register(TimerNpImpl.class, "EJBContainer", "com.ibm.ejs.container.container");

    /**
     * Map of all active TimerNpImpl objects, keyed by unique taskId string.
     * These timers have either been scheduled or queued to be scheduled.
     *
     * Access to this map must be synchronized AND an attempt to lock a
     * TimerNpImpl instance must never occur while holding the lock on
     * this map. When locking both is required, the instance lock must
     * be obtained first.
     */
    private static final Map<String, TimerNpImpl> svActiveTimers = new LinkedHashMap<String, TimerNpImpl>();

    /**
     * Uniquely identifies the task, which is really an instance of this
     * TimerNpImpl Timer class. The term "task" is used to maintain some
     * analogy with the persistent TimerImpl Timer class, which employs
     * Scheduler tasks in the implementation of a timer. While initialized
     * arbitrarily to "_NP", ivTaskId will be the concatenation of
     *
     * ivTaskSeqNo + "_NP_" + a UUID instance
     *
     * e.g.
     * 1_NP_3F245CAB-0120-4000-E000-1BDB090A7490
     * 2_NP_3F245CAB-0120-4000-E001-1BDB090A7490
     * 3_NP_3F245CAB-0120-4000-E002-1BDB090A7490
     * 4_NP_3F245CAB-0120-4000-E003-1BDB090A7490
     **/
    protected String ivTaskId = "_NP"; // 591279
    private static long ivTaskSeqNo = 0; // F743-3618.1

    /** The container that hosts this timer. **/
    private final EJSContainer ivContainer; // F743-506

    /**
     * Uniquely identifies the EJB, and provides access to the bean home. If
     * this timer was an automatically created timer, then this BeanId might not
     * be fully initialized.
     */
    private final BeanId ivBeanId;

    /**
     * The metadata for the bean to which this timer belongs.
     */
    private final BeanMetaData ivBMD; // d589357

    /** The method index for the timeout callback method. */
    int ivMethodId; // F743-506

    /**
     * Customer provided serializable info object.
     */
    private Serializable ivInfo;

    /**
     * The millisecond on which the timer will next expire, or 0 if the timer
     * has no future expirations.
     */
    private long ivExpiration;

    /** Interval on which a repeating timer expires **/
    private long ivInterval;

    /**
     * Parsed calendar-based schedule expression. When non-null, ivInterval
     * will be 0.
     */
    private ParsedScheduleExpression ivParsedScheduleExpression;

    /**
     * true when this timer instance has been canceled (and transaction committed),
     * or when the timer has expired and run successfully for the last time.
     *
     * Made volatile so all code paths that just need to check the variable to
     * decide whether to throw an exception don't need to synchronize. Code
     * paths that modify the state, or use the state to modify other state
     * must synchronize.
     **/
    private volatile boolean ivDestroyed = false;

    /**
     * The thread that is currently executing the timeout callback method,
     * or null if none is currently executing or the timeout callback has
     * called cancel.
     *
     * Provides support similar to ivIsExecutingEJBTimeout and isCachingAllowed
     * used for persistent timers.
     */
    Thread ivTimeoutThread;

    /**
     * Holds the last time on which the timer has been expired. To be used in
     * getNextTimeout() when the it is called from a Timeout method and
     * there is no more timeouts to occur.
     */
    private long ivLastExpiration; // 598265

    /**
     * The scheduler service implementation specific task handler.
     */
    private TimerNpRunnable ivTaskHandler;

    /**
     * Initializes fields of the timer.
     *
     * @param container The container that hosts this timer.
     * @param bmd       The metadata for the timed object for the timer.
     * @param beanId    BeanId identifying the timed object for the timer.
     * @param info      Application information to be delivered along with the timer
     *                      expiration notification. This can be null.
     */
    private TimerNpImpl(EJSContainer container, BeanMetaData bmd, BeanId beanId, Serializable info) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            String infoClassName = (info == null) ? null : info.getClass().getName();
            Tr.entry(tc, "TimerNpImpl : " + beanId + ", " + infoClassName);
        }

        ivContainer = container; // F743-506
        ivBMD = bmd; // d589357
        ivBeanId = beanId;
        ivInfo = ivContainer.ivObjectCopier.copy(info); // d587232, RTC102299
        ivTaskId = incrementTaskSeqNo();
    }

    /**
     * Create a single-action timer that expires at a given point in time. <p>
     *
     * Assumption: caller does so only for a non-persistent timer. <p>
     *
     * @param beanId     BeanId identifying the timed object for the timer.
     * @param expiration The point in time at which the timer must (first) expire.
     * @param info       Application information to be delivered along with the timer
     *                       expiration notification. This can be null.
     * @param interval   The number of milliseconds that must elapse
     *                       between timer expiration notifications.
     *                       A negative value indicates this is a single-action timer.
     **/
    public TimerNpImpl(BeanId beanId, Date expiration, long interval, Serializable info) {
        this(((EJSHome) beanId.home).container, ((EJSHome) beanId.home).beanMetaData, beanId, info); // F743-7591, F743-506, d589357

        // Set the Timer instance variables.
        ivExpiration = expiration.getTime();
        ivInterval = interval;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, this.toString());
    }

    /**
     * Constructor for an expired task. Used when an expired timer is
     * deserialized during SFSB activation.
     *
     * @param beanId identity of the EJB for the Timer callback
     * @param taskId unique identity of the Timer/task
     **/
    private TimerNpImpl(BeanId beanId, String taskId) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "TimerNpImpl : " + beanId + ", " + taskId);

        ivContainer = ((EJSHome) beanId.home).container; // F743-506
        ivTaskId = taskId;
        ivBMD = ((EJSHome) beanId.home).beanMetaData; // d589357
        ivBeanId = beanId;
        ivDestroyed = true; // F743-425.CodRev

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, this.toString());
    }

    /**
     * Creates a calendar-based timer that expires based on the given parsed
     * schedule expression.
     *
     * @param container  The container that hosts this timer.
     * @param bmd        The metadata for the timed object for the timer.
     * @param beanId     BeanId identifying the timed object for the timer.
     * @param methodId   The method index for the timeout callback method
     * @param parsedExpr The parsed schedule expression
     * @param info       Application information to be delivered along with the timer
     *                       expiration notification. This can be null.
     */
    public TimerNpImpl(EJSContainer container,
                       BeanMetaData bmd,
                       BeanId beanId,
                       int methodId,
                       ParsedScheduleExpression parsedExpr,
                       Serializable info) {
        this(container, bmd, beanId, info);

        ivMethodId = methodId; // F743-506
        // F7437591.codRev - getFirstTimeout returns -1 for "no timeouts", but
        // this class uses ivExpiration=0.
        ivExpiration = Math.max(0, parsedExpr.getFirstTimeout());
        ivParsedScheduleExpression = parsedExpr;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, toString());
    }

    /**
     * Creates a calendar-based timer that expires based on the given parsed
     * schedule expression.
     *
     * @param beanId     BeanId identifying the timed object for the timer.
     * @param parsedExpr The parsed schedule expression
     * @param info       Application information to be delivered along with the timer
     *                       expiration notification. This can be null.
     */
    public TimerNpImpl(BeanId beanId, ParsedScheduleExpression parsedExpr, Serializable info) {
        this(((EJSHome) beanId.home).container, ((EJSHome) beanId.home).beanMetaData, beanId, 0, parsedExpr, info); // F743-506, d589357
    }

    String getTaskId() {
        return ivTaskId;
    }

    /** Safely increment the task sequence number **/
    private static synchronized String incrementTaskSeqNo() {
        ivTaskSeqNo++;
        String taskId = ivTaskSeqNo + "_NP_" + new UUID(); // F743-3618.1
        return taskId;
    }

    public BeanId getIvBeanId() {
        return ivBeanId;
    }

    public BeanMetaData getBeanMetaData() {
        return this.ivBMD;
    }

    /** @return ivDestroyed; not synchronized */
    boolean isIvDestroyed() {
        return ivDestroyed;
    }

    /**
     * Registers the timer with the TimerService (so that it will be found by
     * getTimers) and either schedules the timer for execution or places it
     * on a queue to be scheduled once the application has started. <p>
     *
     * There are multiple callers for start():
     *
     * - Called by the EJBRuntime for automatic non-persistent timers
     * immediately after being created.
     *
     * - Called by the EJBRuntime when a timer is first created if
     * there is no active ContainerTx (for backward compatibility
     * with BMT in EJB 1.1 modules).
     *
     * - Called by ContainerTx when a timer is first created if there
     * is no global transaction (i.e local transaction)
     *
     * - Called by ContainerTx during afterCompletion for all newly
     * created timers that are committing in a global transaction.
     *
     * - Called by ContainerTx during afterCompletion for all cancelled
     * timers that are being rescheduled due to rollback.
     */
    public void start() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "start: " + this);

        EJBModuleMetaDataImpl ejbModuleMetaData = ivBMD._moduleMetaData;
        EJBApplicationMetaData ejbAmd = ejbModuleMetaData.getEJBApplicationMetaData();

        // Synchronization must insure a destroyed timer is not placed in the
        // active timers map.                                            RTC107334
        synchronized (this) {
            // If the application is stopping/stopped, then the timer should neither
            // be active nor scheduled; move to destroyed.
            if (ejbAmd.isStopping()) {
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "start: not started - application stop");
                ivDestroyed = true;
                return;
            }

            // If this is a single-action timer which is already destroyed,
            // then return without creating another Alarm.  It's lifecycle is over.
            // F7437591.codRev - Alternatively, if this is a calendar-based timer
            // with no expirations, don't create the alarm.
            if (ivDestroyed || ivExpiration == 0) {
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "start: not started - destroyed = " + ivDestroyed + ", expiration = " + ivExpiration);
                return;
            }

            synchronized (svActiveTimers) {
                svActiveTimers.put(ivTaskId, this);
            }
        }

        // Note that this is not synchronized. Just because a timer is considered
        // active does not mean it is scheduled. Scheduling will be synchronized
        // separately; and must insure a destroyed timer is not scheduled, nor
        // allow a timer to be scheduled multiple times.

        // Do not start the alarm for the timer until the application has fully
        // started. EJBApplicationMetaData controls when to queue or schedule. d589357
        boolean queuedOrStarted = ejbAmd.queueOrStartNonPersistentTimerAlarm(this, ejbModuleMetaData);

        // There is a small window where an application could begin stopping after the timer
        // has been added to the active list above but before the call to queue or start.
        // The queue or start method will return false if the application is now stopping,
        // so the timer needs to be removed from the active list to stay in sync.
        if (!queuedOrStarted) {
            remove(true);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "start");
    }

    /**
     * Schedules the timer to actually run. <p>
     *
     * This method should only be called by EJBApplicationMetaData once
     * the application has fully started.
     */
    public synchronized void schedule() {
        // Avoid scheduling a timer that was destroyed on another thread and
        // avoid scheduling a timer twice, which might be attempted if two
        // threads concurrently attempt to cancel and then both rollback.
        if (ivTaskHandler == null && !ivDestroyed) {
            // Create the timer task handler (Runnable/Listener) and schedule the task
            ivTaskHandler = ivContainer.getEJBRuntime().createNonPersistentTimerTaskHandler(this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "schedule: scheduling timer for " + ivExpiration + " / " + new Date(ivExpiration));

            ivTaskHandler.schedule(ivExpiration);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "schedule: timer already scheduled or destroyed : destroyed=" + ivDestroyed);
        }
    }

    /**
     * Calculate the next time that the timer should fire.
     *
     * @return the number of milliseconds until the next timeout, or 0 if the
     *         timer should not fire again
     */
    synchronized long calculateNextExpiration() {
        boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "calculateNextExpiration: " + this);

        ivLastExpiration = ivExpiration; // 598265

        if (ivParsedScheduleExpression != null) {
            // F7437591.codRev - getNextTimeout returns -1 for "no more timeouts",
            // but this class uses ivExpiration=0.
            ivExpiration = Math.max(0, ivParsedScheduleExpression.getNextTimeout(ivExpiration));
        } else {
            if (ivInterval > 0) {
                ivExpiration += ivInterval; // 597753
            } else {
                ivExpiration = 0;
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "calculateNextExpiration: " + ivExpiration); // 597753

        return ivExpiration;
    }

    /**
     * Called by the TimerTaskHandler after successfully completing a timer
     * expiration to schedule the next expiration. The timer will be cancelled
     * if there are no further expirations. <p>
     *
     * {@link #calculateNextExpiration()} must be called prior to this method
     * or the timer will not be scheduled properly. <p>
     */
    synchronized void scheduleNext() {
        // Synchronized to insure a destroyed or cancelled (ivTaskHanler=null)
        // timer is not re-scheduled. If the timer is in the cancelled state
        // but not yet destroyed, then nothing should be done with it here
        // as the canceling thread will either destroy it or rollback and
        // and call schedule to re-create the task handler.              RTC107334

        if (!ivDestroyed) {
            if (ivExpiration != 0) {
                if (ivTaskHandler != null) {
                    ivTaskHandler.scheduleNext(ivExpiration);
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "scheduleNext: not scheduled, timer in cancelled state : " + ivTaskId);
                }
            } else {
                remove(true); // permanently remove the timer since it will never expire again
            }
        }
    }

    /**
     * Called by the TimerTaskHandler after failing to complete a timer
     * expiration, to schedule a retry expiration. <p>
     *
     * {@link #calculateNextExpiration()} must be called prior to the first
     * call to this method or the timer will not be scheduled properly. <p>
     */
    synchronized void scheduleRetry(long retryInterval) {
        // Synchronized to insure a destroyed or cancelled (ivTaskHanler=null)
        // timer is not re-scheduled. If the timer is in the cancelled state
        // but not yet destroyed, then reset the expiration back to the last
        // expiration since it was never completed successfully. If the cancel
        // does rollback then schedule will be called again and re-create the
        // task handler. The only side effect of this is that the retry count
        // is reset; so the timer may experience more retries than configured. RTC107334

        if (!ivDestroyed) {
            if (ivTaskHandler != null) {
                ivTaskHandler.scheduleRetry(retryInterval);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "scheduleRetry: not scheduled, timer in cancelled state : " + ivTaskId);
                ivExpiration = ivLastExpiration;
            }
        }
    }

    /**
     * Determines if Timer methods are allowed based on the 'cancelled' state
     * of the timer instance relative to the current thread and transaction.
     * NoSuchObjectLocalException is thrown if the timer has been cancelled. <p>
     *
     * Returns true if the current thread is running in the scope of the timeout
     * method, and access is allowed to Timer state even if another thread has
     * cancelled the timer; otherwise returns false.
     *
     * Note that if the current thread is running in the scope of the timeout
     * method, it should have access to timer state even if another thread
     * cancels the timer. However, if the timeout method calls cancel, then
     * that access is no longer allowed. The EJB specification does indicate
     * that calling Timer methods should fail after calling cancel, and not
     * specifically commit of the cancel transaction.
     *
     * This is similar to the caching support provided for persistent timers.
     *
     * Must be called by all Timer methods to insure EJB Specification
     * compliance. <p>
     *
     * @throws NoSuchObjectLocalException if this instance has been cancelled
     *                                        relative to the current thread and transaction.
     */
    private void checkIfCancelled() {
        // If a cancel has been committed then the timer "does not exist",
        // unless the cancel occurred after the timer started running
        // and the current thread is for that running timeout method.
        if (ivDestroyed && ivTimeoutThread != Thread.currentThread()) {
            String msg = "Timer with ID " + ivTaskId + " has been canceled.";
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "checkIfCancelled: NoSuchObjectLocalException : " + msg);
            throw new NoSuchObjectLocalException(msg);
        }

        // If cancel was called during the current transaction then the timer
        // "does not exist", regardless of whether or not the current thread
        // is a running timeout method.
        ContainerTx tx = ivContainer.getCurrentContainerTx();
        if (tx != null && tx.timersCanceled != null && tx.timersCanceled.containsValue(this)) {
            String msg = "Timer with ID " + ivTaskId + " has been canceled in the current transaction.";
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "checkIfCancelled: NoSuchObjectLocalException : " + msg);
            throw new NoSuchObjectLocalException(msg);
        }

        // Handle the rather odd scenario where a calendar Timer was created that has
        // no expirations. If last expiration is non-zero, then timer is currently running,
        // so still exists until method completes and ivDestroyed is set. The only way both
        // expiration and last expiration can be 0 is if the timer was created with no
        // expirations.
        if (ivExpiration == 0 && ivLastExpiration == 0) {
            String msg = "Timer with ID " + ivTaskId + " was created with no scheduled timeouts.";
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "checkIfCancelled: NoSuchObjectLocalException : no expirations : " + msg);
            throw new NoSuchObjectLocalException(msg);
        }
    }

    /**
     * Logs a warning if the current time is greater than the configured
     * lateTimerThreshold after the scheduled timer expiration.
     */
    void checkLateTimerThreshold() {
        ivContainer.getEJBRuntime().checkLateTimerThreshold(new Date(ivLastExpiration), ivTaskId, ivBeanId.j2eeName);
    }

    // --------------------------------------------------------------------------
    //
    // Methods from Timer interface
    //
    // --------------------------------------------------------------------------

    /**
     * Cause the timer and all its associated expiration notifications
     * to be canceled. <p>
     *
     * Assumed to be called via a bean, hence checkTimerAccess() is used to
     * verify that the calling bean is in the proper state.
     *
     * @exception IllegalStateException      If this method is invoked while the
     *                                           instance is in a state that does not allow access to this method.
     * @exception NoSuchObjectLocalException If invoked on a timer that has
     *                                           expired or has been canceled.
     * @exception EJBException               If this method could not complete due to a
     *                                           system-level failure.
     **/
    @Override
    public void cancel() throws IllegalStateException, NoSuchObjectLocalException, EJBException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "cancel: " + this);

        // Determine if the calling bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerAccess();

        // Determine if this timer is considered cancelled for the current
        // thread/transaction - throws NoSuchObjectLocalException if cancelled.
        checkIfCancelled();

        // -----------------------------------------------------------------------
        // Concurrency behavior for Non-persistent timer cancel:         RTC107334
        //
        // Concurrent Cancel and getTimers() / Expiration
        // - Once cancel has called (but before the transaction commits), timer
        //   expiration will not occur and getTimers() will not return the
        //   cancelled timer. If rollback occurs, the timer will be re-scheduled
        //   and once again be returned by getTimers().
        // > Differs from persistent timers; persistent timers may or may not
        //   block expiration (depending on database); but getTimers() would
        //   return the timer until the cancel commits. Difference in getTimers()
        //   behavior maintained since it was originally implemented that way
        //   and would cause no significant problem for applications.
        //
        // Concurrent Cancel and Expiration:
        // - If cancel occurs just before the timeout begins running, then the
        //   timer will not run; unless rollback occurs, then it will be
        //   re-scheduled and run immediately.
        // - If the timeout method has started running, the cancel will not be
        //   blocked; however the timer will not re-schedule if it fails. The
        //   cancel transaction will be able to complete even if the timer
        //   is still running; but all other attempts to access it will
        //   report it does not exist.
        // - If the cancel rolls back while the timer is running, the code will
        //   insure it is only re-scheduled one time.
        // > Differs from persistent timers largely depending on database;
        //   closer to the behavior of using optimistic locking (Oracle).
        //
        // Concurrent Cancels
        // - Neither cancel will be blocked; the timer will not run if
        //   expiration occurs after either cancel is called.
        // - Both transactions will be allowed to commit; the timer will be
        //   permanently destroyed after the first commit.
        // - If one commits, then the other rolls back, the rollback will have
        //   no effect on the timer; it has been permanently removed.
        // - If one rolls back, then the other one commits; the roll back will
        //   re-schedule the timer, then the commit will remove it again and
        //   permanently delete it.
        // - There is a possibility the timer may run after the first one rolls
        //   back and the second one commits.
        // > Differs from persistent timers largely depending on database;
        //   closer to the behavior of using optimistic locking (Oracle), where
        //   both commits are allowed to proceed.
        // -----------------------------------------------------------------------

        ContainerTx tx = ivContainer.getCurrentContainerTx();

        if (tx != null) {
            boolean queuedToStart = (tx.timersQueuedToStart != null) ? tx.timersQueuedToStart.remove(ivTaskId) != null : false;
            if (queuedToStart) {
                // If this timer was also created in the same transaction, then it
                // just needs to be removed from the queue of timers to start, and
                // remove called with destroy=true to update state; the cancel
                // cannot ever be rolled back.
                remove(true);
            } else {
                // Remove from the active timers map and cancel, but do not permanently
                // remove to permit restore on rollback.
                remove(false);

                if (tx.timersCanceled == null) {
                    tx.timersCanceled = new LinkedHashMap<String, TimerNpImpl>();
                }
                tx.timersCanceled.put(ivTaskId, this);
            }
        } else {
            // Here for backward compatibility. Could only occur for EJB 1.1
            // module with BMT bean, which the customer has re-coded to
            // implement TimedObject.

            // Remove from the active timers map and cancel permanently (destroy)
            remove(true);
        }

        if (ivTimeoutThread == Thread.currentThread()) {
            // clear the firing thread; subsequent attempts to access this timer
            // will result in NoSuchObjectLocalException.                 RTC127173
            ivTimeoutThread = null;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "cancel: successful");
    }

    /**
     * Get the number of milliseconds that will elapse before the next
     * scheduled timer expiration. <p>
     *
     * Per EJB 3.1 section 18.2.5.3, If the bean invokes the getNextTimeout or
     * getTimeRemaining method on the timer associated with a timeout callback
     * while within the timeout callback, and there are no future timeouts for
     * this calendar-based timer, a NoMoreTimeoutsException must be thrown.
     *
     * @exception IllegalStateException      If this method is invoked while the
     *                                           instance is in a state that does not allow access to this method.
     * @exception NoSuchObjectLocalException If invoked on a timer that has
     *                                           expired or has been canceled.
     * @exception EJBException               If this method could not complete due to a
     *                                           system-level failure.
     **/
    @Override
    public long getTimeRemaining() throws IllegalStateException, NoSuchObjectLocalException, EJBException {
        Date nextTime = getNextTimeout();
        long currentTime = System.currentTimeMillis();
        long remaining = nextTime.getTime() - currentTime;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getTimeRemaining: " + remaining);

        return remaining;
    }

    /**
     * Get the point in time at which the next timer expiration is scheduled
     * to occur. <p>
     *
     * Per EJB 3.1 section 18.2.5.3, If the bean invokes the getNextTimeout or
     * getTimeRemaining method on the timer associated with a timeout callback
     * while within the timeout callback, and there are no future timeouts for
     * this calendar-based timer, a NoMoreTimeoutsException must be thrown.
     *
     * @exception IllegalStateException      If this method is invoked while the
     *                                           instance is in a state that does not allow access to this method.
     * @exception NoSuchObjectLocalException If invoked on a timer that has
     *                                           expired or has been canceled.
     * @exception EJBException               If this method could not complete due to a
     *                                           system-level failure.
     **/
    @Override
    public Date getNextTimeout() throws IllegalStateException, NoSuchObjectLocalException, EJBException, NoMoreTimeoutsException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getNextTimeout: " + this);

        // Determine if the calling bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerAccess();

        // Determine if this timer is considered cancelled for the current
        // thread/transaction - throws NoSuchObjectLocalException if cancelled.
        checkIfCancelled();

        if (ivExpiration == 0) {

            if (ivParsedScheduleExpression != null) {
                // NoMoreTimeoutsException is only thrown for calendar-based timers
                // called from a timeout callback method.
                if (ivTimeoutThread == Thread.currentThread()) {
                    String msg = "Timer with ID " + ivTaskId + " has no more timeouts.";
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "getNextTimeout: NoMoreTimeoutsException : " + msg);
                    throw new NoMoreTimeoutsException(msg);
                }
            }

            // If the expiration is 0 (and the timer hasn't been cancelled), then
            // the timer must be running, though this may or may not be the timer
            // callback thread. Either way, return the last expiration.
            //
            // For single action timers, for the timeout callback thread, the last
            // value is returned to be consistent with persistent timers, and that
            // is what is documented in info center.
            //
            // For non-timeout callback threads, the update to the expiration by
            // the timeout callback thread should not be visible yet, since
            // it hasn't committed, so also return the last expiration.
            Date nextTime = new Date(ivLastExpiration);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getNextTimeout: no timeouts : " + nextTime);
            return nextTime;
        }

        // If the timer is running, but this isn't the timeout callback thread
        // then the last expiration needs to be returned as the current thread
        // should not have visibility to the updated expiration just yet.
        if (ivTimeoutThread != null && ivTimeoutThread != Thread.currentThread()) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getNextTimeout: non firing thread : " + new Date(ivLastExpiration));
            return new Date(ivLastExpiration);
        }

        Date nextTime = new Date(ivExpiration);
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getNextTimeout: " + nextTime);
        return nextTime;
    }

    /**
     * Get the information associated with the timer at the time of creation. <p>
     *
     * @return The Serializable object that was passed in at timer creation,
     *         or null if the info argument passed in at timer creation was null.
     *
     * @exception IllegalStateException      If this method is invoked while the
     *                                           instance is in a state that does not allow access to this method.
     * @exception NoSuchObjectLocalException If invoked on a timer that has
     *                                           expired or has been canceled.
     * @exception EJBException               If this method could not complete due to a
     *                                           system-level failure.
     **/
    @Override
    public Serializable getInfo() throws IllegalStateException, NoSuchObjectLocalException, EJBException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getInfo: " + this);

        // Determine if the calling bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerAccess();

        // Determine if this timer is considered cancelled for the current
        // thread/transaction - throws NoSuchObjectLocalException if cancelled.
        checkIfCancelled();

        Serializable info = ivContainer.ivObjectCopier.copy(ivInfo); // d587232, RTC102299
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getInfo: " + Util.identity(info));

        return info;
    }

    @Override
    public TimerHandle getHandle() throws IllegalStateException, NoSuchObjectLocalException, EJBException {
        throw new IllegalStateException("getHandle method not allowed on non-persistent timers."); // F7437591.codRev
    }

    /**
     * Query whether this timer is a calendar-based timer.
     *
     * @return true if this timer is a calendar-based timer.
     *
     * @exception IllegalStateException      If this method is invoked while the
     *                                           instance is in a state that does not allow access to this
     *                                           method. Also thrown if invoked on a timer that is not a
     *                                           calendar-based timer.
     * @exception NoSuchObjectLocalException If invoked on a timer that has
     *                                           expired or has been cancelled.
     * @exception EJBException               If this method could not complete due to a
     *                                           system-level failure.
     */
    @Override
    public boolean isCalendarTimer() {
        boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "isCalendarTimer: " + this);

        // Determine if the calling bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerAccess();

        // Determine if this timer is considered cancelled for the current
        // thread/transaction - throws NoSuchObjectLocalException if cancelled.
        checkIfCancelled();

        boolean result = ivParsedScheduleExpression != null;

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "isCalendarTimer: " + result);

        return result;
    }

    /**
     * Get the schedule expression corresponding to this timer.
     *
     * @exception IllegalStateException      If this method is invoked while the
     *                                           instance is in a state that does not allow access to this
     *                                           method. Also thrown if invoked on a timer that is not a
     *                                           calendar-based timer.
     * @exception NoSuchObjectLocalException If invoked on a timer that has
     *                                           expired or has been cancelled.
     * @exception EJBException               If this method could not complete due to a
     *                                           system-level failure.
     */
    @Override
    public ScheduleExpression getSchedule() {
        boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getSchedule: " + this);

        // Determine if the calling bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerAccess();

        // Determine if this timer is considered cancelled for the current
        // thread/transaction - throws NoSuchObjectLocalException if cancelled.
        checkIfCancelled();

        if (!isCalendarTimer()) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getSchedule: IllegalStateException : Timer is not a calendar-based timer");
            throw new IllegalStateException("Timer is not a calendar-based timer");
        }

        ScheduleExpression result = ivContainer.ivObjectCopier.copy(ivParsedScheduleExpression.getSchedule()); // d587232, RTC102299

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getSchedule: " + ScheduleExpressionParser.toString(result));

        return result;
    }

    /**
     * By definition, a TimerNpImpl instance is not persistent
     */
    @Override
    public boolean isPersistent() {
        // Determine if the calling bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerAccess();

        // Determine if this timer is considered cancelled for the current
        // thread/transaction - throws NoSuchObjectLocalException if cancelled.
        checkIfCancelled();

        return false;
    }

    // --------------------------------------------------------------------------
    //
    // Non-Interface / Internal Implementation Methods
    //
    // --------------------------------------------------------------------------

    /**
     * Removes the timer from the active timers map and cancels all associated
     * expiration notifications. Optionally, the timer is permanently destroyed
     * such that it cannot be restored; otherwise it may still be restored due
     * to a transaction rollback. <p>
     *
     * @param destroy true indicates the timer should be permanently destroyed;
     *                    otherwise the timer may be restored after a rollback occurs.
     **/
    protected void remove(boolean destroy) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "remove(destroy=" + destroy + ") : " + this);

        // Synchronized on the timer to insure the remove from the active map
        // and the cancellation of the task occur together; without another
        // thread re-scheduling the timer at the same time.              RTC107334
        synchronized (this) {
            // Regardless whether or not remove was called previously with
            // destroy=false, the timer must be removed from the active timer
            // map again in case another transaction also called to cancel the
            // timer but has since rolled back and rescheduled the timer.
            synchronized (svActiveTimers) {
                svActiveTimers.remove(ivTaskId);
            }

            // Destroy must be after remove from active timers map to insure
            // a destroyed timer is never in the active timers map.
            if (destroy) {
                ivDestroyed = true;
            }

            if (ivTaskHandler != null) {
                ivTaskHandler.cancel();
                ivTaskHandler = null;
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "remove");
    }

    /**
     * Returns a collection of all active Timers associated with the specified
     * BeanId. (It does not look for timers queued to start or on a cancelled queue)<p>
     *
     * @param beanId Bean identity for which active Timers will be returned.
     *
     * @return a collection of Timers associated with the specified bean Id.
     **/
    public static Collection<Timer> findTimersByBeanId(BeanId beanId, ContainerTx tx) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "findTimersByBeanId: " + beanId);

        int numActive = 0;
        ArrayList<Timer> timers = new ArrayList<Timer>();

        // Search the active timers for all timer taskIDs associated with the given beanId.
        synchronized (svActiveTimers) {
            for (TimerNpImpl npTimer : svActiveTimers.values()) {
                if (npTimer.ivBeanId.equals(beanId)) {
                    timers.add(npTimer);
                }
            }
            numActive = svActiveTimers.size(); // for trace
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "findTimersByBeanId: " + timers.size() + "(of " + numActive + " active)");

        return timers;
    }

    /**
     * Returns a collection of all active Timers associated with the specified
     * module. (It does not look for timers queued to start or on a cancelled
     * queue) <p>
     *
     * @param mmd caller's module metadata
     *
     * @return a collection of Timers associated with the specified module.
     **/
    public static Collection<Timer> findTimersByModule(ModuleMetaData mmd) {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "findTimersByModule: " + mmd);

        int numActive = 0;
        ArrayList<Timer> timers = new ArrayList<Timer>();

        // Search the active timers for all timer taskIDs associated with the given beanId.
        synchronized (svActiveTimers) {
            for (TimerNpImpl npTimer : svActiveTimers.values()) {
                if (npTimer.ivBMD.getModuleMetaData() == mmd) {
                    timers.add(npTimer);
                }
            }
            numActive = svActiveTimers.size(); // for trace
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "findTimersByModule: " + timers.size() + "(of " + numActive + " active)");

        return timers;
    }

    /**
     * Removes all timers associated with the specified application or module.
     *
     * @param j2eeName the name of the application or module
     */
    public static void removeTimersByJ2EEName(J2EEName j2eeName) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "removeTimersByJ2EEName: " + j2eeName);

        Collection<TimerNpImpl> timersToRemove = null;

        // Although this seems inefficient, removing all of the timers for an
        // application or module that is stopping is done in two steps to avoid
        // obtaining locks on both the active timers map and any individual
        // timer concurrently. Many methods on a timer will also need to obtain
        // the lock on the active timers map as well, so the two step approach
        // avoids the possibility of deadlock.                           RTC107334

        synchronized (svActiveTimers) {
            if (svActiveTimers.size() > 0) {
                String appToRemove = j2eeName.getApplication();
                String modToRemove = j2eeName.getModule();

                for (TimerNpImpl timer : svActiveTimers.values()) {
                    J2EEName timerJ2EEName = timer.ivBeanId.j2eeName;
                    if (appToRemove.equals(timerJ2EEName.getApplication()) &&
                        (modToRemove == null || modToRemove.equals(timerJ2EEName.getModule()))) {
                        // save the timer to be removed outside the active timers map lock
                        if (timersToRemove == null) {
                            timersToRemove = new ArrayList<TimerNpImpl>();
                        }
                        timersToRemove.add(timer);
                    }
                }
            }
        }

        if (timersToRemove != null) {
            for (TimerNpImpl timer : timersToRemove) {
                timer.remove(true); // permanently remove the timer; cannot be rolled back
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "removeTimersByJ2EEName: removed " + (timersToRemove != null ? timersToRemove.size() : 0));
    }

    /**
     * Determines if Timer methods are allowed based on the current state
     * of bean instance associated with the current transaction. This includes
     * the methods on the javax.ejb.Timer interface. <p>
     *
     * Must be called by all Timer methods to insure EJB Specification
     * compliance. <p>
     *
     * Note: This method does not apply to the EJBContext.getTimerService()
     * method, as getTimerService may be called for more bean states.
     * getTimerServcie() must provide its own checking. <p>
     *
     * @exception IllegalStateException If this instance is in a state that does
     *                                      not allow timer service method operations.
     **/
    protected void checkTimerAccess() throws IllegalStateException {
        BeanO beanO = EJSContainer.getCallbackBeanO();
        if (beanO != null) {
            beanO.checkTimerServiceAccess();
        } else if (ivContainer.allowTimerAccessOutsideBean) {
            // Beginning with EJB 3.2, the specification was updated to allow Timer
            // and TimerHandle access outside of an EJB. Although it would seem to
            // make sense that a Timer could also be accessed from an EJB in any
            // state, that part of the specification was not updated, so the above
            // checking is still performed when there is a callback BeanO.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "checkTimerAccess: Timer access permitted outside of bean");
        } else {
            // EJB 3.1 and earlier restricted access to the Timer API to beans only
            IllegalStateException ise = new IllegalStateException("Timer: Timer methods not allowed " + "- no active EJB");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "checkTimerAccess: " + ise);
            }
            throw ise;
        }
    }

    /**
     * Get a serializable handle to the timer. This handle can be used at
     * a later time to re-obtain the timer reference. <p>
     *
     * This method is intended for use by the Stateful passivation code, when
     * a Stateful EJB is being passivated, and contains a Timer (not a
     * TimerHanle). <p>
     *
     * This method differs from {@link #getHandle} in that it performs none
     * of the checking required by the EJB Specification, such as if the Timer
     * is still valid. When passivating a Stateful EJB, none of this checking
     * should be performed. <p>
     *
     * Also, this method 'marks' the returned TimerHandle, so that it will
     * be replaced by the represented Timer when read from a stream.
     * See {@link com.ibm.ejs.container.passivator.StatefulPassivator} <p>
     *
     * @return A serializable handle to the timer.
     **/
    @Override
    public PassivatorSerializableHandle getSerializableObject() {
        TimerNpHandleImpl timerHandle;

        timerHandle = new TimerNpHandleImpl(ivBeanId, ivTaskId); // F743-425.CodRev

        return timerHandle;
    }

    /**
     * Returns the timer reference that was represented by a TimerHandle
     * obtained from {@link #getSerializableObject()}. For non-persistent
     * timers, a serialized TimerHandle always represents a Timer; never
     * a TimerHandle. <p>
     *
     * This method is intended for use by the Stateful passivation code, when
     * a Stateful EJB is being passivated, and contains a Timer (not a
     * Serializable object). <p>
     *
     * Note: if the timer represented by the TimerHandle no longer
     * exists, a destroyed timer will still be returned. <p>
     *
     * @param taskId the TaskId from the requesting TimerHandle.
     * @return a reference to the Timer represented by the TaskId.
     **/
    protected static Timer getDeserializedTimer(BeanId beanId, String taskId) {
        // Look first for existing timer (in all 3 places)
        // If not found, THEN new TimerNpImpl, but with new ctor to set
        // ivDestroyed=true, since this timer will never run

        // F743-22582
        // A stateful session bean may have a reference to a non-persistent Timer.
        //
        // By definition, a non-persistent Timer only exists in the JVM its defined
        // in, and so if the stateful bean is failed over to another server, the
        // Timer is no longer valid.
        //
        // In the failover scenario, we still want to hydrate that instance variable
        // in the stateful bean with a Timer instance (ie, we don't want that variable
        // to be null)...but that Timer instance must be invalid, such that if the
        // user invokes a method on it, they get a NoSuchObjectLocalException error.
        //
        // In the failover scenario, this code (which is attempting to rehydrate the Timer)
        // will be running on a different server than the one the TimerNpImpl instance
        // was created on.  Therefore, the TimerNpImpl instance we are looking for will
        // not be in the server-specific map we are dealing with, and so we'll bring back
        // a new TimerNpImpl instance which knows its not valid, and which produces the
        // desired NoSuchObjectLocalException when a method is invoked on it.

        Timer timer;

        synchronized (svActiveTimers) {
            timer = svActiveTimers.get(taskId);

            if (timer != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "getDeserializedTimer: found in active timer map : " + timer);
                return timer;
            }
        }

        EJSHome home = (EJSHome) beanId.home;
        ContainerTx containerTx = home.container.getCurrentContainerTx();

        if (containerTx != null) {
            if (containerTx.timersQueuedToStart != null) {
                timer = containerTx.timersQueuedToStart.get(taskId);

                if (timer != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "getDeserializedTimer: found in timersQueuedToStart : " + timer);
                    return timer;
                }
            }

            if (containerTx.timersCanceled != null) {
                timer = containerTx.timersCanceled.get(taskId);

                if (timer != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "getDeserializedTimer: found in timersCanceled : " + timer);
                    return timer;
                }
            }
        }

        // Since timer was not found to exist, instantiate a dummy in the
        // destroyed state that will fail as expected when accessed.
        timer = new TimerNpImpl(beanId, taskId);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getDeserializedTimer: not found - returning destroyed Timer : " + timer);

        return timer;
    }

    /**
     * Overridden to provide state based equality. <p>
     *
     * This override of the default Object.equals is required, even though
     * there are type specific overloads, in case the caller does not have
     * (or know) the parameter as the specific type. <p>
     **/
    @Override
    public boolean equals(Object obj) {
        // Note : Keep in synch with TimerNpHandleImpl.equals().
        if (obj instanceof TimerNpImpl) {
            // Only the task id is needed to determine object equality.
            TimerNpImpl timer = (TimerNpImpl) obj;
            return ivTaskId.equals(timer.ivTaskId);
        }

        return false;
    }

    /**
     * Overridden to provide state based hashcode.
     **/
    @Override
    public int hashCode() {
        // Note : Keep in synch with TimerNpHandleImpl.hashcode().
        return ivTaskId.hashCode();
    }

    /**
     * Overridden to improve trace.
     **/
    @Override
    public String toString() {
        return ("TimerNpImpl(" + ivTaskId + ", "
                + ivBeanId + ", "
                + ivExpiration + ", "
                + ivInterval + ", "
                + ivDestroyed + ", "
                + Util.identity(ivInfo) + ")");
    }

}
