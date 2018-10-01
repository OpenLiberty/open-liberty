/*******************************************************************************
 * Copyright (c) 2003, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.persistent.osgi.internal;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJBNotFoundException;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.PersistentTimer;
import com.ibm.ejs.container.PersistentTimerTaskHandler;
import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskIdAccessor;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.persistent.ejb.TimerTrigger;
import com.ibm.ws.ejbcontainer.runtime.EJBRuntime;
import com.ibm.ws.ejbcontainer.util.ParsedScheduleExpression;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Extends the core PersistentTimerTaskHandler implementation to provide
 * integration with the Liberty profile PersistentExecutor Service.
 *
 * Note: PersistentExecutor has been optimized to detect when the same object
 * implements both Runnable and Trigger and only persist the object once.
 **/
class PersistentTimerTaskHandlerImpl extends PersistentTimerTaskHandler implements TimerTrigger {

    private static final long serialVersionUID = -8200752857441853748L;

    private static final TraceComponent tc = Tr.register(PersistentTimerTaskHandlerImpl.class, "EJBContainer", "com.ibm.ejs.container.container");

    private static final String TIMER_NAME_PREFIX_AUTO = "!EJBTimerA!";
    private static final String TIMER_NAME_PREFIX_PROG = "!EJBTimerP!";
    private static final String TIMER_NAME_PREFIX_PATTERN = "!EJBTimer_!";

    /**
     * Constructor for expiration based persistent timers. Expiration based timers are
     * either "single-action" timers that run just once at a specific time (expiration),
     * or "interval" timers that run initially at a specific time (expiration) and then
     * repeat at a designated interval. <p>
     *
     * Automatic timers cannot be based on an initial expiration. <p>
     *
     * @param j2eeName identity of the Timer bean that is the target of the associated task.
     * @param info the user data associated with this timer
     * @param expiration The point in time at which the timer must expire.
     * @param interval The number of milliseconds that must elapse between timer expiration notifications.
     *            A negative value indicates this is a single-action timer.
     *
     * @throws IOException if the serializable user object cannot be serialized.
     **/
    @Trivial
    protected PersistentTimerTaskHandlerImpl(J2EEName j2eeName, @Sensitive Serializable info,
                                             Date expiration, long interval) {
        super(j2eeName, info, expiration, interval);
    }

    /**
     * Constructor for calendar based persistent timers (not automatic).
     *
     * @param j2eeName identity of the Timer bean that is the target of the associated task.
     * @param info the user data associated with this timer; may be null
     * @param parsedSchedule the parsed schedule expression for calendar-based timers; must be non-null
     *
     * @throws IOException if the serializable user object cannot be serialized.
     **/
    @Trivial
    protected PersistentTimerTaskHandlerImpl(J2EEName j2eeName, @Sensitive Serializable info,
                                             ParsedScheduleExpression parsedSchedule) {
        super(j2eeName, info, parsedSchedule);
    }

    /**
     * Constructor for automatic calendar based persistent timers.
     *
     * @param j2eeName identity of the Timer bean that is the target of the associated task.
     * @param info the user data associated with this timer; may be null
     * @param parsedSchedule the parsed schedule expression for calendar-based timers; must be non-null
     * @param methodId timeout callback method identifier; must be a non-zero value
     * @param methodame timeout callback method name; used for validation
     * @param className timeout callback class name; used for validation (null if defined in XML)
     *
     * @throws IOException if the serializable user object cannot be serialized.
     **/
    @Trivial
    protected PersistentTimerTaskHandlerImpl(J2EEName j2eeName, @Sensitive Serializable info,
                                             ParsedScheduleExpression parsedSchedule,
                                             int methodId,
                                             String methodName,
                                             String className) {
        super(j2eeName, info, parsedSchedule, methodId, methodName, className);
    }

    // --------------------------------------------------------------------------
    //
    // Implemented abstract methods
    //
    // --------------------------------------------------------------------------

    @Override
    protected PersistentTimer createTimer(EJBRuntime ejbRuntime) {
        return ejbRuntime.getPersistentTimer(TaskIdAccessor.get(), j2eeName, this);
    }

    @Override
    public Date getNextTimeout(Date lastExecution, Date timerCreationTime) {

        // For 'expiration' based timers, return one of the following:
        // 1 - first expiration if timer has never run
        // 2 - null for single action timers that have run
        // 3 - last scheduled time + interval
        if (parsedSchedule == null) {
            if (lastExecution == null) {
                return new Date(expiration);
            }
            if (interval < 0) {
                return null;
            }
            return new Date(lastExecution.getTime() + interval);
        }

        // For 'calendar' based timers, calculate the next run time based on either
        // the time the timer was created (first run) or the last scheduled run time.
        long nextTimeout;
        if (lastExecution != null) {
            nextTimeout = parsedSchedule.getNextTimeout(lastExecution.getTime());
        } else {
            nextTimeout = parsedSchedule.getFirstTimeout();

            // A timer would never be scheduled if getFirstTimeout() had originally returned
            // -1, therefore the first (and only) timeout has passed while creating the timer.
            // In this scenario, run the timer immediately by returning the creation time.
            if (nextTimeout == -1) {
                //Need to shave off milliseconds from timerCreationTime
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTime(timerCreationTime);
                cal.set(GregorianCalendar.MILLISECOND, 0);
                cal.add(GregorianCalendar.SECOND, 1);
                return cal.getTime();
            }
        }
        if (nextTimeout == -1) {
            return null;
        }
        return new Date(nextTimeout);
    }

    // --------------------------------------------------------------------------
    //
    // Methods from interface javax.enterprise.concurrent.ManagedTask
    //
    // --------------------------------------------------------------------------

    @Override
    public Map<String, String> getExecutionProperties() {
        String taskOwner = j2eeName.getApplication() + "/" + j2eeName.getModule() + "/" + j2eeName.getComponent();

        BeanMetaData bmd = getBeanMetaData();
        HashMap<String, String> props = new HashMap<String, String>();

        // Value for TaskName column that may be queried.
        props.put(ManagedTask.IDENTITY_NAME, getTaskName());

        // Indicates whether timer runs under PersistentExecutor transaction
        // set to SUSPEND for NOT_SUPPORTED or BMT
        props.put(ManagedTask.TRANSACTION, runInGlobalTransaction(bmd.timedMethodInfos[methodId]) ? ManagedTask.USE_TRANSACTION_OF_EXECUTION_THREAD : ManagedTask.SUSPEND);

        // Useful for future MBean support provided by PersistentExecutor
        props.put(WSContextService.TASK_OWNER, taskOwner);

        // Pass global transaction timeout
        props.put(PersistentExecutor.TRANSACTION_TIMEOUT, Integer.toString(bmd._globalTran.getTransactionTimeout()));

        return props;
    }

    @Override
    @Trivial
    public ManagedTaskListener getManagedTaskListener() {
        // Not supported in WebSphere/Liberty; not needed for timers
        return null;
    }

    // --------------------------------------------------------------------------
    //
    // Methods from interface javax.enterprise.concurrent.Trigger
    //
    // --------------------------------------------------------------------------

    @Override
    public Date getNextRunTime(LastExecution lastExecutionInfo, Date taskScheduledTime) {
        Date lastExecution = lastExecutionInfo == null ? null : lastExecutionInfo.getScheduledStart();
        return getNextTimeout(lastExecution, taskScheduledTime);
    }

    @Override
    public boolean skipRun(LastExecution lastExecutionInfo, Date scheduledRunTime) {
        // EJB Timers never skip, however this is where a warning may be logged
        // if a timer is running noticeably later than scheduled, like ASYN0091_LATE_ALARM on traditional WAS
        EJBRuntime ejbRuntime = EJSContainer.getDefaultContainer().getEJBRuntime();
        ejbRuntime.checkLateTimerThreshold(scheduledRunTime, TaskIdAccessor.get().toString(), j2eeName);

        return false;
    }

    // --------------------------------------------------------------------------
    //
    // Methods from interface com.ibm.ws.concurrent.persistent.ejb.TimerTrigger
    //
    // --------------------------------------------------------------------------

    /**
     * @see com.ibm.ws.concurrent.persistent.ejb.TimerTrigger#getAppName()
     */
    @Override
    public String getAppName() {
        return j2eeName.getApplication();
    }

    /**
     * @see com.ibm.ws.concurrent.persistent.ejb.TimerTrigger#getClassLoader()
     */
    @Override
    public ClassLoader getClassLoader() {
        ClassLoader cl;
        try {
            cl = EJSContainer.getDefaultContainer().getInstalledHome(j2eeName).getBeanMetaData().ivContextClassLoader;
        } catch (EJBNotFoundException e) {
            Tr.warning(tc, "HOME_NOT_FOUND_CNTR0092W", j2eeName.toString());
            throw new IllegalStateException(e);
        }
        return cl;
    }

    // --------------------------------------------------------------------------
    //
    // Internal implementation methods
    //
    // --------------------------------------------------------------------------

    /**
     * Returns the task name that should be used when creating timer tasks in
     * the persistent store. <p>
     *
     * The task name is composed of an EJB TimerService specific prefix,
     * !EJBTimerA! for automatic timers or !EJBTimerP! for programmatic
     * timers, that will differentiate it from other tasks in the persistent
     * store, followed by the module URI and bean name (from J2EEName) <p>
     *
     * For example: !EJBTimerP!ejbmodule.jar#BeanOneName <p>
     *
     * This task name will not be unique, but when used in conjunction with
     * the task owner (application name) column in persistent store it will
     * uniquely identify the timers for the specified J2EEName. <p>
     *
     * As long as the full name in this format is less than 254 bytes,
     * then a query using this string will match only entries for the
     * specific bean using the default table definitions. For applications
     * with module and bean names that exceed this limit, the customer
     * must create or alter tables with a wider format for the
     * task name column. <p>
     *
     * @return the task name to be used when creating persistent timer tasks.
     **/
    @Trivial
    String getTaskName() {
        return getTaskName(j2eeName, isAutomaticTimer() ? TIMER_NAME_PREFIX_AUTO : TIMER_NAME_PREFIX_PROG, false);
    }

    private static String getTaskName(J2EEName j2eeName, String timerNamePrefix, boolean escape) {

        StringBuffer sb = new StringBuffer();

        sb.append(timerNamePrefix);
        sb.append(escape ? escapePattern(j2eeName.getModule()) : j2eeName.getModule());
        sb.append('#');
        sb.append(escape ? escapePattern(j2eeName.getComponent()) : j2eeName.getComponent());

        return sb.toString();
    }

    /**
     * Returns the task name that should be used when finding timer tasks in the
     * persistent store for a specific bean. <p>
     *
     * The value returned is the same as that of {@link #getTaskName()} except
     * the automatic/programmatic indicator (A/P) is replaced with the SQL single
     * character wild card (_).
     *
     * @return the task name pattern to be used when finding persistent timer tasks for a bean
     **/
    @Trivial
    protected static String getTaskNameBeanPattern(J2EEName j2eeName) {
        return getTaskName(j2eeName, TIMER_NAME_PREFIX_PATTERN, true);
    }

    protected static String getAutomaticTimerTaskNameBeanPattern(J2EEName j2eeName) {
        return getTaskName(j2eeName, TIMER_NAME_PREFIX_AUTO, true);
    }

    /**
     * Returns the task name that should be used when finding timer tasks in the
     * persistent store for a specific module. <p>
     *
     * The value returned is the same as that of {@link #getTaskName()} except
     * the automatic/programmatic indicator (A/P) is replaced with the SQL single
     * character wild card (_) and bean name is replaced with the
     * SQL zero or more character wild card (%).
     *
     * @return the task name pattern to be used when finding persistent timer tasks for a module
     **/
    protected static String getTaskNameModulePattern(String moduleName) {
        return getTaskNameModulePattern(moduleName, TIMER_NAME_PREFIX_PATTERN);
    }

    protected static String getAutomaticTimerTaskNameModulePattern(String moduleName) {
        return getTaskNameModulePattern(moduleName, TIMER_NAME_PREFIX_AUTO);
    }

    private static String getTaskNameModulePattern(String moduleName, String prefix) {
        StringBuffer sb = new StringBuffer();

        sb.append(prefix);
        sb.append(escapePattern(moduleName));
        sb.append("#%");

        return sb.toString();
    }

    public static String getAutomaticTimerPropertyName(String appName, String moduleName) {
        StringBuilder sb = new StringBuilder();

        sb.append(TIMER_NAME_PREFIX_AUTO);
        sb.append(appName);
        sb.append('#');
        sb.append(moduleName);

        return sb.toString();
    }

    @Trivial
    private static String escapePattern(String s) {
        return s.replace("\\", "\\\\").replace("_", "\\_").replace("%", "\\%");
    }

    public static String getAutomaticTimerPropertyPattern(String appName, String moduleName) {
        return getAutomaticTimerPropertyName(escapePattern(appName), moduleName != null ? escapePattern(moduleName) : "%");
    }
}
