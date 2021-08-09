/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.Timer;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.PersistentTimer;
import com.ibm.ejs.container.PersistentTimerTaskHandler;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.ejbcontainer.util.ParsedScheduleExpression;
import com.ibm.ws.exception.RuntimeWarning;
import com.ibm.ws.metadata.ejb.AutomaticTimerBean;

/**
 * The interface between the core container and the services provided by
 * the EJB Persistent Timer runtime environment.
 */
public interface EJBPersistentTimerRuntime {

    /**
     * Creates a persistent expiration based (single action or interval) EJB timer.
     *
     * @param beanId the bean Id for which the timer is being created
     * @param expiration the initial expiration for a interval-based timer
     * @param interval the interval for an interval-based timer, or -1 for a single-action timer
     * @param info application information to be delivered to the timeout method, or null
     */
    Timer createPersistentExpirationTimer(BeanId beanId, Date expiration, long interval, Serializable info);

    /**
     * Creates a persistent calendar based EJB timer (not automatic).
     *
     * @param beanId the bean Id for which the timer is being created
     * @param parsedExpr the parsed values of the schedule for a calendar-based timer
     * @param info application information to be delivered to the timeout method, or null
     */
    Timer createPersistentCalendarTimer(BeanId beanId, ParsedScheduleExpression parsedExpr, Serializable info);

    /**
     * Only called if persistent automatic timers exist
     *
     * Creates the persistent automatic timers for the specified module. <p>
     *
     * @param appName the application name
     * @param moduleName the module name
     * @param timerBeans the beans with automatic timers
     * @return the number of timers created
     */
    int createPersistentAutomaticTimers(String appName, String moduleName, List<AutomaticTimerBean> timerBeans) throws RuntimeWarning;

    /**
     * Platform specific method to obtain a persistent timer instance for
     * a deserialized timer task handler. <p>
     *
     * This method is intended for use by the timer task handler when it
     * has been deserialized and is invoking the timeout callback. <p>
     *
     * @param taskId unique identity of the Timer
     * @param j2eeName unique EJB name composed of Application-Module-Component
     * @param taskHandler persistent timer task handler associated with the taskId
     * @return persistent timer instance
     */
    PersistentTimer getPersistentTimer(long taskId, J2EEName j2eeName, PersistentTimerTaskHandler taskHandler);

    /**
     * Platform specific method to obtain a persistent timer without regard
     * to whether the timer still exists. <p>
     *
     * This method is intended for use by the Stateful activation code, when
     * a Stateful EJB is being activated and contained a Timer (that was
     * serialized as a TimerHandle). <p>
     *
     * @param taskId unique identity of the Timer
     *
     * @return Timer represented by the specified taskId
     */
    Timer getPersistentTimer(long taskId);

    /**
     * Platform specific method to restore a persistent timer from storage. <p>
     *
     * The EJB specification requires a NoSuchObjectLoaclException if the timer no longer
     * exists, but this method may return a Timer with cache information if the customer
     * has configured an option to allow stale data. <p>
     *
     * @param taskId unique identity of the Timer
     *
     * @return Timer represented by the specified taskId
     * @throws NoSuchObjectLocalException if the timer no longer exists in persistent store
     *             or has been cancelled in the current transaction.
     */
    Timer getPersistentTimerFromStore(long taskId) throws NoSuchObjectLocalException;

    /**
     * Gets all the timers associated with the specified bean.
     *
     * @param beanId the bean
     * @return the timers associated with the bean
     */
    Collection<Timer> getTimers(BeanId beanId);

    /**
     * Gets all timers associated with the specified application and module.
     *
     * @param appName name of the application
     * @param moduleName name of the module (really the module URI from J2EEName)
     * @param allowsCachedTimerData true if at least one bean in the module allows caching of timer data
     * @return all timers associated with the module
     */
    Collection<Timer> getAllTimers(String appName, String moduleName, boolean allowsCachedTimerData);

    /**
     * Tells the current Persistent Executor to start polling if initial polling is delayed
     * After it successfully calls PersistentExecutor.startPolling(), this method is a no-op
     *
     * @throws IllegalStateException if no Persistent Executor can be resolved
     */
    void enableDatabasePolling();

    /**
     * Resets the no-op flag and then tells the current Persistent Executor to start
     * polling if initial polling is delayed
     *
     * @throws IllegalStateException if no Persistent Executor can be resolved
     */
    void resetAndCheckDatabasePolling();

    /**
     * Inform the EJB persistent timer runtime that the server is stopping.
     * No new activity for persistent timers should be started after this
     * notification occurs.
     */
    void serverStopping();

    /**
     * Determines if the EJB persistent timer runtime has been explicitly configured
     * or has a working default configuration. Since the EJB persistent timer runtime
     * supports an optional configuration of a persistent executor and otherwise uses
     * the default persistent executor for the server, the component may start even
     * if there is no working configuration (i.e. even the default persistent
     * executor does not have a datasource). <p>
     *
     * If an explicit configuration has not been provided, and the default also
     * is not available, then the EJBContainer should treat access to the EJB
     * persistent timer runtime similar to the feature not being enabled
     * at all, except applications with persistent automatic timers should not
     * be allowed to start, rather than just ignoring the persistent timers. <p>
     *
     * @return true if the EJB persistent timer runtime has been configured; otherwise false.
     */
    boolean isConfigured();
}
