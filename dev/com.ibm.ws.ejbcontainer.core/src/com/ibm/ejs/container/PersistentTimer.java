/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejs.container;

import static com.ibm.ejs.container.ContainerConfigConstants.ALLOW_CACHED_TIMER_GET_HANDLE;
import static com.ibm.ejs.container.ContainerConfigConstants.ALLOW_CACHED_TIMER_GET_INFO;
import static com.ibm.ejs.container.ContainerConfigConstants.ALLOW_CACHED_TIMER_GET_NEXT_TIMEOUT;
import static com.ibm.ejs.container.ContainerConfigConstants.ALLOW_CACHED_TIMER_GET_SCHEDULE;
import static com.ibm.ejs.container.ContainerConfigConstants.ALLOW_CACHED_TIMER_GET_TIME_REMAINING;
import static com.ibm.ejs.container.ContainerConfigConstants.ALLOW_CACHED_TIMER_IS_CALENDAR_TIMER;
import static com.ibm.ejs.container.ContainerConfigConstants.ALLOW_CACHED_TIMER_IS_PERSISTENT;

import java.io.Serializable;
import java.util.Date;

import javax.ejb.EJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerHandle;

import com.ibm.ejs.container.passivator.PassivatorSerializable;
import com.ibm.ejs.container.passivator.PassivatorSerializableHandle;
import com.ibm.ejs.util.Util;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.util.ParsedScheduleExpression;

/**
 * Provides the common implementation of the javax.ejb.Timer interface,
 * and contains information about a timer that was created through the
 * EJB Timer Service. <p>
 **/
public abstract class PersistentTimer implements Timer, PassivatorSerializable {

    private static final TraceComponent tc = Tr.register(PersistentTimer.class, "EJBContainer", "com.ibm.ejs.container.container");

    /**
     * Uniquely identifies the task (i.e. primary key of row in the database).
     *
     * This field will be null if this is a calendar-based timer that was created with no future timeouts.
     */
    protected Long taskId;

    /**
     * Uniquely identifies the EJB. May be null if it hasn't been retrieved from database yet.
     */
    protected J2EEName j2eeName;

    /**
     * Used to determine if the timeout callback method is running. This is important when allowing
     * a user to read potentially stale Timer data. If the timeout callback method is running, the data
     * is not stale because the timer data was just loaded in order to run the Timer.
     */
    protected boolean isTimeoutCallback;

    /**
     * Indicates which, if any, timer methods may use stale/cached timer data. Should be
     * defaulted to indicate no caching is allowed (0), until the J2EEName is determined.
     *
     * @see ContainerConfigConstants#allowCachedTimerDataFor
     */
    protected int cachedTimerDataAllowed;

    /**
     * Cached timer state information (user info / next timeout).
     *
     * Should only be used if timer accessed from timeout method, or customer has
     * configured the type of access to allow stale data.
     */
    protected PersistentTimerTaskHandler cachedTaskHandler;

    /**
     * Constructs a persistent timer. <p>
     *
     * Used when a new timer is being created (all parameters non-null unless
     * the timer will never run); when existing timers are restored from
     * the database to execute timeout callback or provide a collection of
     * existing timers; when obtaining a timer from a timer handle, either
     * when getTimer is called or when a Timer is restored as part of
     * Stateful bean activation. <p>
     *
     * @param taskId unique identity of the timer; may be null if timer will never run
     * @param j2eeName identity of the EJB for the Timer callback; may be null
     * @param cachedTimerDataAllowed indicates which timer methods may use cached timer data
     * @param taskHandler cacheable information about the timer; may be null
     */
    protected PersistentTimer(Long taskId, J2EEName j2eeName, int cachedTimerDataAllowed, PersistentTimerTaskHandler taskHandler) {
        this.taskId = taskId;
        this.j2eeName = j2eeName;
        this.cachedTimerDataAllowed = cachedTimerDataAllowed;
        this.cachedTaskHandler = taskHandler;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init> : " + this);
    }

    // --------------------------------------------------------------------------
    //
    // Methods from Timer interface
    //
    // --------------------------------------------------------------------------

    /**
     * Get the number of milliseconds that will elapse before the next
     * scheduled timer expiration. <p>
     *
     * @throws IllegalStateException If this method is invoked while the
     *             instance is in a state that does not allow access to this method.
     * @throws NoSuchObjectLocalException If invoked on a timer that has
     *             expired or has been cancelled.
     * @throws EJBException If this method could not complete due to a
     *             system-level failure.
     **/
    @Override
    public long getTimeRemaining() throws IllegalStateException, NoSuchObjectLocalException, EJBException {
        Date nextTime = getNextTimeout(ALLOW_CACHED_TIMER_GET_TIME_REMAINING);
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
     * @throws IllegalStateException If this method is invoked while the
     *             instance is in a state that does not allow access to this method.
     * @throws NoSuchObjectLocalException If invoked on a timer that has
     *             expired or has been cancelled.
     * @throws EJBException If this method could not complete due to a
     *             system-level failure.
     **/
    @Override
    public Date getNextTimeout() throws IllegalStateException, NoSuchObjectLocalException, EJBException {
        Date nextTime = getNextTimeout(ALLOW_CACHED_TIMER_GET_NEXT_TIMEOUT);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getNextTimeout: " + nextTime);

        return nextTime;
    }

    /**
     * Get the information associated with the timer at the time of creation. <p>
     *
     * @return The Serializable object that was passed in at timer creation,
     *         or null if the info argument passed in at timer creation was null.
     *
     * @throws IllegalStateException If this method is invoked while the
     *             instance is in a state that does not allow access to this method.
     * @throws NoSuchObjectLocalException If invoked on a timer that has
     *             expired or has been cancelled.
     * @throws EJBException If this method could not complete due to a
     *             system-level failure.
     **/
    @Override
    public Serializable getInfo() throws IllegalStateException, NoSuchObjectLocalException, EJBException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getInfo: " + this);

        // Determine if the calling bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerAccess();

        PersistentTimerTaskHandler taskHandler = getTimerTaskHandler(ALLOW_CACHED_TIMER_GET_INFO);
        Serializable userInfo = taskHandler.getUserInfo(); // returns a new copy every time

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getInfo: " + Util.identity(userInfo));

        return userInfo;
    }

    /**
     * Get a serializable handle to the timer. This handle can be used at
     * a later time to re-obtain the timer reference. <p>
     *
     * @return A serializable handle to the timer.
     *
     * @throws IllegalStateException If this method is invoked while the
     *             instance is in a state that does not allow access to this method.
     * @throws NoSuchObjectLocalException If invoked on a timer that has
     *             expired or has been cancelled.
     * @throws EJBException If this method could not complete due to a
     *             system-level failure.
     **/
    @Override
    public TimerHandle getHandle() throws IllegalStateException, NoSuchObjectLocalException, EJBException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getHandle: " + this);

        // Determine if the calling bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerAccess();

        // Determine if the timer still exists; configuration may allow stale data
        checkTimerExists(ALLOW_CACHED_TIMER_GET_HANDLE);

        TimerHandle timerHandle = new PersistentTimerHandle(taskId, false);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getHandle: " + timerHandle);

        return timerHandle;
    }

    /**
     * Get the schedule expression corresponding to this timer.
     *
     * @throws IllegalStateException If this method is invoked while the
     *             instance is in a state that does not allow access to this
     *             method.
     * @throws NoSuchObjectLocalException If invoked on a timer that has
     *             expired or has been cancelled.
     * @throws EJBException If this method could not complete due to a
     *             system-level failure.
     */
    @Override
    public ScheduleExpression getSchedule() {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getSchedule: " + this);

        // Determine if the calling bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerAccess();

        PersistentTimerTaskHandler taskHandler = getTimerTaskHandler(ALLOW_CACHED_TIMER_GET_SCHEDULE);
        ParsedScheduleExpression parsedSchedule = taskHandler.getParsedSchedule();
        if (parsedSchedule == null) {
            IllegalStateException ise = new IllegalStateException("Timer is not a calendar-based timer: " + toString());
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getSchedule: " + ise);
            throw ise;
        }

        ScheduleExpression schedule = EJSContainer.getDefaultContainer().ivObjectCopier.copy(parsedSchedule.getSchedule());

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getSchedule: " + schedule);
        return schedule;
    }

    /**
     * Query whether this timer has persistent semantics.
     *
     * @return true if this timer has persistent guarantees.
     * @throws IllegalStateException If this method is invoked while the
     *             instance is in a state that does not allow access to this
     *             method.
     * @throws NoSuchObjectLocalException If invoked on a timer that has
     *             expired or has been cancelled.
     * @throws EJBException If this method could not complete due to a
     *             system-level failure.
     */
    @Override
    public boolean isPersistent() {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "isPersistent: " + this);

        // Determine if the calling bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerAccess();

        // Determine if the timer still exists; configuration may allow stale data
        checkTimerExists(ALLOW_CACHED_TIMER_IS_PERSISTENT);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "isPersistent: true");
        return true;
    }

    /**
     * Query whether this timer is a calendar-based timer.
     *
     * @return true if this timer is a calendar-based timer.
     *
     * @throws IllegalStateException If this method is invoked while the
     *             instance is in a state that does not allow access to this
     *             method. Also thrown if invoked on a timer that is not a
     *             calendar-based timer.
     * @throws NoSuchObjectLocalException If invoked on a timer that has
     *             expired or has been cancelled.
     * @throws EJBException If this method could not complete due to a
     *             system-level failure.
     */
    @Override
    public boolean isCalendarTimer() {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "isCalendarTimer: " + this);

        // Determine if the calling bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerAccess();

        PersistentTimerTaskHandler taskHandler = getTimerTaskHandler(ALLOW_CACHED_TIMER_IS_CALENDAR_TIMER);
        boolean result = taskHandler.getParsedSchedule() != null;

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "isCalendarTimer: " + result);
        return result;
    }

    // --------------------------------------------------------------------------
    //
    // Methods from PassivatorSerializable interface
    //
    // --------------------------------------------------------------------------

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
     * See {@link com.ibm.ejs.container.passivator.NewOutputStream} and {@link com.ibm.ejs.container.passivator.NewInputStream}. <p>
     *
     * @return A serializable handle to the timer.
     **/
    @Override
    public PassivatorSerializableHandle getSerializableObject() {
        return new PersistentTimerHandle(taskId, true);
    }

    // --------------------------------------------------------------------------
    //
    // Non-Interface / Internal Implementation Methods
    //
    // --------------------------------------------------------------------------

    /**
     * Get the point in time at which the next timer expiration is scheduled
     * to occur. <p>
     *
     * Determines if caching is allowed and if so returns the cached value. If
     * caching is not allowed a request will be made to the scheduler service
     * to get the most current Timer data.<p>
     *
     * @throws IllegalStateException If this method is invoked while the
     *             instance is in a state that does not allow access to this method.
     * @throws NoSuchObjectLocalException If invoked on a timer that has
     *             expired or has been cancelled.
     * @throws EJBException If this method could not complete due to a
     *             system-level failure.
     **/
    protected abstract Date getNextTimeout(int operation) throws IllegalStateException, NoSuchObjectLocalException, EJBException;

    /**
     * Checks whether any cached data can be used for the current timer.
     *
     * @return true if any cached data can be used
     */
    protected boolean isAnyCachingAllowed() {
        return isTimeoutCallback || cachedTimerDataAllowed != 0;
    }

    /**
     * Checks whether cached data can be used for the specified operation.
     *
     * @param operation an ALLOW_CACHED_TIMER constant from ContainerConfigConstants.
     * @return true if cached data can be used
     */
    protected boolean isCachingAllowed(int operation) {
        return isTimeoutCallback || (cachedTimerDataAllowed & operation) != 0;
    }

    /**
     * Returns the corresponding PersistentTimerTaskHandler which contains the
     * state information about the timer. By default, the returned object will
     * be retrieved from persistent storage, to insure the timer still exists.
     * However, a cached (and potentially stale) instance will be returned if
     * the timer is currently running, or the specified timer operation has been
     * configured to allow stale data.
     *
     * @param operation the current timer operation
     * @throws NoSuchObjectLocalException If invoked on a timer that has
     *             expired or has been cancelled.
     * @throws TimerServiceException if the timer state fails to deserialize from the persistent store.
     */
    protected abstract PersistentTimerTaskHandler getTimerTaskHandler(int operation);

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
     * @throws IllegalStateException If this instance is in a state that does
     *             not allow timer service method operations.
     * @throws NoSuchObjectLocalException If this is a calendar-based timer
     *             that has no timeouts.
     **/
    protected void checkTimerAccess() {

        BeanO beanO = EJSContainer.getCallbackBeanO();
        if (beanO != null) {
            beanO.checkTimerServiceAccess();
        } else if (EJSContainer.getDefaultContainer().allowTimerAccessOutsideBean) {
            // Beginning with EJB 3.2, the specification was updated to allow Timer
            // and TimerHandle access outside of an EJB. Although it would seem to
            // make sense that a Timer could also be accessed from an EJB in any
            // state, that part of the specification was not updated, so the above
            // checking is still performed when there is a callback BeanO.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "checkTimerAccess: Timer access permitted outside of bean");
        } else {
            // EJB 3.1 and earlier restricted access to the Timer API to beans only
            IllegalStateException ise = new IllegalStateException("Timer methods not allowed - no active EJB");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "checkTimerAccess: " + ise);
            throw ise;
        }

        // The timer was created with no scheduled timeouts; so considered not to exist.
        if (taskId == null) {
            NoSuchObjectLocalException nsoe = new NoSuchObjectLocalException(this.toString());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "checkTimerAccess: " + nsoe);
            throw nsoe;
        }
    }

    /**
     * Determines if the timer exists in the persistent store, or if a stale
     * copy may be used for the specified Timer operation.
     *
     * @param operation the timer operation being checked
     * @throws NoSuchObjectLocalException If invoked on a timer that has
     *             expired or has been cancelled.
     */
    protected abstract void checkTimerExists(int operation);

    /**
     * Overridden to provide state based equality. <p>
     *
     * This override of the default Object.equals is required, even though
     * there are type specific overloads, in case the caller does not have
     * (or know) the parameter as the specific type. <p>
     **/
    @Override
    public boolean equals(Object obj) {

        // Note : Keep in synch with TimerHandleImpl.equals().
        if (obj instanceof PersistentTimer) {
            if (taskId == null)
                // Calendar-based timers created with no future timeouts
                // cannot be serialized, so object identity is sufficient.
                return this == obj;

            PersistentTimer timer = (PersistentTimer) obj;
            return taskId.equals(timer.taskId);
        }
        return false;
    }

    /**
     * Overridden to provide state based hashcode.
     **/
    @Override
    public int hashCode() {
        // Note : Keep in synch with PersistentTimerHandle.hashCode().
        return taskId == null ? 0 : (int) (taskId % Integer.MAX_VALUE);
    }

    /**
     * Overridden to improve trace.
     **/
    @Override
    public String toString() {
        return (getClass().getSimpleName() + "(" + taskId + ", " + j2eeName + ")");
    }
}
