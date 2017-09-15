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

package com.ibm.ws.ejbcontainer.timer.persistent.osgi.internal;

import java.util.Date;

import javax.ejb.EJBException;
import javax.ejb.NoMoreTimeoutsException;
import javax.ejb.NoSuchObjectLocalException;

import com.ibm.ejs.container.PersistentTimer;
import com.ibm.ejs.container.PersistentTimerTaskHandler;
import com.ibm.ejs.container.TimerServiceException;
import com.ibm.websphere.concurrent.persistent.TaskStatus;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.persistent.ejb.TimerStatus;

/**
 * Provides an implementation of the javax.ejb.Timer interface, and contains
 * information about a timer that was created through the EJB Timer Service. <p>
 **/
public class PersistentTimerImpl extends PersistentTimer {

    private static final TraceComponent tc = Tr.register(PersistentTimerImpl.class, "EJBContainer", "com.ibm.ejs.container.container");

    /**
     * EJB persistent timer runtime implementation.
     */
    private final EJBPersistentTimerRuntimeImpl persistentTimerRuntime;

    /**
     * Cached timer task status.
     *
     * Should only be used if timer accessed from timeout method, or customer has
     * configured the type of access to allow stale data.
     */
    private TimerStatus<?> cachedTimerStatus;

    /**
     * Constructs a persistent timer instance; does not create the timer task
     * in the timer database. <p>
     *
     * Since the real timer state, except the task ID, is contained in the timer
     * task handler, this implementation of Timer acts as an accessor to the
     * state in the task handler. And, this constructor may be used to obtain
     * a Timer for all scenarios, including :
     * <ul>
     * <li>when a new timer is being created (all parameters non-null unless the timer will never run)
     * <li>when existing timers are restored from the database to execute timeout callbacks
     * <li>when existing timers are restored form the database to provide a collection of timers
     * <li>when obtaining a timer from a timer handle when getTimer is called
     * <li>when obtaining a timer from a timer handle when a Timer is restored as part of Stateful bean activation.
     * </ol>
     *
     * @param taskId unique identity of the timer; may be null if timer doesn't exist or will never run
     * @param j2eeName identity of the EJB for the Timer callback; required
     * @param cachedTimerDataAllowed indicates which timer methods may use cached timer data
     * @param taskHandler cacheable information about the timer; may be null
     * @param timerStatus cacheable status for the timer; may be null
     * @param persistentTimerRuntime EJB persistent timer runtime; required
     */
    @Trivial
    public PersistentTimerImpl(Long taskId, J2EEName j2eeName, int cachedTimerDataAllowed, PersistentTimerTaskHandler taskHandler,
                               TimerStatus<?> timerStatus, EJBPersistentTimerRuntimeImpl persistentTimerRuntime) {
        super(taskId, j2eeName, cachedTimerDataAllowed, taskHandler);
        this.persistentTimerRuntime = persistentTimerRuntime;
        this.cachedTimerStatus = timerStatus;
    }

    // --------------------------------------------------------------------------
    //
    // Methods from Timer interface
    //
    // --------------------------------------------------------------------------

    /**
     * Cause the timer and all its associated expiration notifications
     * to be cancelled. <p>
     *
     * @throws IllegalStateException If this method is invoked while the
     *             instance is in a state that does not allow access to this method.
     * @throws NoSuchObjectLocalException If invoked on a timer that has
     *             expired or has been cancelled.
     * @throws EJBException If this method could not complete due to a
     *             system-level failure.
     **/
    @Override
    @Trivial
    public void cancel() throws IllegalStateException, NoSuchObjectLocalException, EJBException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "cancel: " + this);

        boolean removed;

        // Determine if the calling bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerAccess();

        try {
            removed = persistentTimerRuntime.remove(taskId);
        } catch (Throwable ex) {
            throw newTimerServiceException(ex);
        }

        if (!removed) {
            throw newNoSuchObjectLocalException();
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "cancel: successful");
    }

    // --------------------------------------------------------------------------
    //
    // Non-Interface / Internal Implementation Methods
    //
    // --------------------------------------------------------------------------

    @Override
    @Trivial
    protected Date getNextTimeout(int operation) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getNextTimeout: " + operation);

        // Determine if the calling bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerAccess();

        Date nextTimeout;
        if (j2eeName == null || isAnyCachingAllowed()) {
            TaskStatus<?> taskStatus = getTimerStatus(operation);
            nextTimeout = taskStatus.getNextExecutionTime();
        } else {
            try {
                nextTimeout = persistentTimerRuntime.getNextExecutionTime(taskId);
            } catch (Throwable ex) {
                throw newTimerServiceException(ex);
            }
            if (nextTimeout == null) {
                throw newNoSuchObjectLocalException();
            }
        }

        if (isTimeoutCallback) {
            PersistentTimerTaskHandler taskHandler = getTimerTaskHandler(operation);
            Date afterCurrentTimeout = taskHandler.getNextTimeout(nextTimeout, null);

            if (afterCurrentTimeout != null) {
                nextTimeout = afterCurrentTimeout;
            } else {
                // The calendar-based timer associated with a timeout callback must
                // throw NoMoreTimeoutsException while within the timeout callback.
                if (taskHandler.getParsedSchedule() != null) {
                    NoMoreTimeoutsException nmte = new NoMoreTimeoutsException(toString());
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "getNextTimeout: " + nmte);
                    throw nmte;
                }
                // Otherwise, it is a single action timer, return the current timeout
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getNextTimeout: " + nextTimeout);

        return nextTimeout;
    }

    /**
     * Returns the corresponding timer TaskStatus. By default, the returned object
     * will be retrieved from the persistent store, to insure the timer still exists.
     * However, a cached (and potentially stale) instance may be returned if the
     * timer is currently running, or the specified timer operation has been
     * configured to allow stale data. <p>
     *
     * @param operation the current timer operation
     *
     * @throws NoSuchObjectLocalException If invoked on a timer that has
     *             expired or has been cancelled.
     * @throws TimerServiceException if an error occurs accessing the persistent store.
     */
    private TimerStatus<?> getTimerStatus(int operation) {
        TimerStatus<?> timerStatus = cachedTimerStatus;
        if (timerStatus != null && isCachingAllowed(operation)) {
            return timerStatus;
        }
        try {
            timerStatus = cachedTimerStatus = persistentTimerRuntime.getTimerStatus(taskId);
        } catch (Throwable ex) {
            throw newTimerServiceException(ex);
        }
        if (timerStatus == null) {
            throw newNoSuchObjectLocalException();
        }
        if (j2eeName == null) {
            try {
                PersistentTimerTaskHandler taskHandler = cachedTaskHandler = (PersistentTimerTaskHandler) timerStatus.getTimer();
                j2eeName = taskHandler.getJ2EEName();
                cachedTimerDataAllowed = persistentTimerRuntime.getAllowCachedTimerData(j2eeName);
            } catch (Throwable ex) {
                throw newTimerServiceException(ex);
            }
        }
        return timerStatus;
    }

    @Override
    protected PersistentTimerTaskHandler getTimerTaskHandler(int operation) {
        PersistentTimerTaskHandler taskHandler = cachedTaskHandler;
        if (taskHandler != null && isCachingAllowed(operation)) {
            return taskHandler;
        }
        TimerStatus<?> taskStatus = getTimerStatus(operation);
        try {
            taskHandler = cachedTaskHandler = (PersistentTimerTaskHandler) taskStatus.getTimer();
        } catch (Throwable ex) {
            throw newTimerServiceException(ex);
        }
        return taskHandler;
    }

    @Override
    protected void checkTimerExists(int operation) {

        // If caching is allowed for this operation, then nothing to do; the timer exists,
        // but when caching is not allowed, perform the minimal access of timer data to
        // insure it still exists... get the next execution time from PersistentExector.
        if (!isCachingAllowed(operation)) {
            Date nextExecution = null;
            try {
                nextExecution = persistentTimerRuntime.getNextExecutionTime(taskId);
            } catch (Throwable ex) {
                throw newTimerServiceException(ex);
            }
            if (nextExecution == null) {
                throw newNoSuchObjectLocalException();
            }
        }
    }

    private TimerServiceException newTimerServiceException(Throwable ex) {
        StringBuilder msg = new StringBuilder();
        msg.append("An error occurred accessing the persistent EJB timer with the ").append(taskId).append(" task identifier");
        if (j2eeName != null) {
            msg.append(" for the ").append(j2eeName.getComponent()).append(" bean in the ");
            msg.append(j2eeName.getModule()).append(" module in the ").append(j2eeName.getApplication()).append(" application");
        }
        msg.append(" : ").append(ex.getMessage());
        return new TimerServiceException(msg.toString(), ex);
    }

    private NoSuchObjectLocalException newNoSuchObjectLocalException() {
        StringBuilder msg = new StringBuilder();
        msg.append("Persistent EJB timer with the ").append(taskId).append(" task identifier");
        if (j2eeName != null) {
            msg.append(" for the ").append(j2eeName.getComponent()).append(" bean in the ");
            msg.append(j2eeName.getModule()).append(" module in the ").append(j2eeName.getApplication()).append(" application");
        }
        msg.append(" no longer exists.");
        return new NoSuchObjectLocalException(msg.toString());
    }
}
