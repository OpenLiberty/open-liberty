/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.persistent.osgi.internal;

import static com.ibm.ejs.container.ContainerProperties.AllowCachedTimerDataFor;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.Timer;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.PersistentTimer;
import com.ibm.ejs.container.PersistentTimerTaskHandler;
import com.ibm.ejs.container.TimerServiceException;
import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.PersistentStoreException;
import com.ibm.websphere.concurrent.persistent.TaskState;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.persistent.ejb.TimerStatus;
import com.ibm.ws.concurrent.persistent.ejb.TimersPersistentExecutor;
import com.ibm.ws.ejbcontainer.osgi.EJBPersistentTimerRuntime;
import com.ibm.ws.ejbcontainer.osgi.EJBTimerRuntime;
import com.ibm.ws.ejbcontainer.util.ParsedScheduleExpression;
import com.ibm.ws.exception.RuntimeWarning;
import com.ibm.ws.metadata.ejb.AutomaticTimerBean;
import com.ibm.ws.metadata.ejb.TimerMethodData;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(service = { EJBPersistentTimerRuntime.class, EJBPersistentTimerRuntimeImpl.class },
           configurationPid = "com.ibm.ws.ejbcontainer.timer.runtime",
           configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class EJBPersistentTimerRuntimeImpl implements EJBPersistentTimerRuntime {

    private static final TraceComponent tcContainer = Tr.register(EJBPersistentTimerRuntimeImpl.class, "EJBContainer", "com.ibm.ejs.container.container");

    private static final String REFERENCE_EJB_TIMER_RUNTIME = "ejbTimerRuntime";
    private static final String REFERENCE_DEFAULT_PERSISTENT_EXECUTOR = "defaultEJBPersistentTimerExecutor";
    private static final String MISSED_TIMER_ACTION = "missedPersistentTimerAction";

    /**
     * Enumeration of the timerService.missedPersistentTimerAction configuration allowed options.
     */
    enum MissedTimerAction {
        ALL, ONCE
    }

    private final AtomicServiceReference<EJBTimerRuntime> ejbTimerRuntimeServiceRef = new AtomicServiceReference<EJBTimerRuntime>(REFERENCE_EJB_TIMER_RUNTIME);
    private final AtomicServiceReference<PersistentExecutor> defaultPersistentExecutorRef = new AtomicServiceReference<PersistentExecutor>(REFERENCE_DEFAULT_PERSISTENT_EXECUTOR);

    private boolean enabledDatabasePolling, hasSetupTimers;
    private volatile boolean serverStopping;

    // Configured missedPersistentTimerAction; null if not explicitly declared
    private MissedTimerAction missedTimerAction;

    /**
     * Map of Java EE Name -> allowCachedTimerData integer value; where J2EEName is either the
     * String representation of a J2EEName or the wild card, "*".
     */
    private volatile Map<String, Integer> allowCachedTimerDataMap;

    @Reference(name = REFERENCE_EJB_TIMER_RUNTIME, service = EJBTimerRuntime.class)
    protected void setEJBTimerRuntime(ServiceReference<EJBTimerRuntime> ref) {
        ejbTimerRuntimeServiceRef.setReference(ref);
    }

    protected void unsetEJBTimerRuntime(ServiceReference<EJBTimerRuntime> ref) {
        ejbTimerRuntimeServiceRef.unsetReference(ref);
    }

    @Reference(name = REFERENCE_DEFAULT_PERSISTENT_EXECUTOR,
               service = PersistentExecutor.class,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               cardinality = ReferenceCardinality.OPTIONAL,
               target = "(id=defaultEJBPersistentTimerExecutor)")
    protected void setDefaultEJBPersistentTimerExecutor(ServiceReference<PersistentExecutor> ref) {
        defaultPersistentExecutorRef.setReference(ref);
    }

    protected void unsetDefaultEJBPersistentTimerExecutor(ServiceReference<PersistentExecutor> ref) {
        defaultPersistentExecutorRef.unsetReference(ref);
    }

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> properties) {
        ejbTimerRuntimeServiceRef.activate(cc);
        defaultPersistentExecutorRef.activate(cc);
        updateConfiguration(properties);
        PersistentTimerTaskHandlerImpl.persistentTimerRuntime = this;
    }

    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> properties) {
        updateConfiguration(properties);
    }

    private void updateConfiguration(Map<String, Object> properties) {

        // Read the missedPersistentTimerAction configuration; do not set a default as
        // that will vary based on whether the PersistentExecutor has failover enabled
        String missedTimerActionProperty = (String) properties.get(MISSED_TIMER_ACTION);
        missedTimerAction = (missedTimerActionProperty != null) ? MissedTimerAction.valueOf(missedTimerActionProperty) : null;

        if (TraceComponent.isAnyTracingEnabled() && tcContainer.isDebugEnabled())
            Tr.debug(tcContainer, "MissedTimerAction = " + missedTimerAction);

        // For quick access, regardless of whether an application is even installed,
        // build a map of Java EE name string to timer data cache setting; where the
        // Java EE name string may be "*" to indicate all beans.
        if (AllowCachedTimerDataFor != null) {
            Map<String, Integer> newAllowCachedTimerDataMap = new HashMap<String, Integer>();
            StringTokenizer st = new StringTokenizer(AllowCachedTimerDataFor, ":");

            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                int assignmentPivot = token.indexOf('=');

                if (assignmentPivot > 0) { //case where we have 'j2eename=<integer>' or '*=<integer>'

                    String tokenName = token.substring(0, assignmentPivot).trim(); //get j2eename or '*'
                    String tokenValue = token.substring(assignmentPivot + 1).trim();//get <integer>
                    try {
                        newAllowCachedTimerDataMap.put(tokenName, Integer.parseInt(tokenValue));
                    } catch (NumberFormatException e) {
                        // FFDC will be logged; ignore this entry
                    }
                } else { //token did not include an equals sign....case where we have just 'j2eename' or '*'.  Apply all caching in this case.
                    newAllowCachedTimerDataMap.put(token, -1);
                }
            } // while loop
            allowCachedTimerDataMap = newAllowCachedTimerDataMap;
        } else {
            allowCachedTimerDataMap = null;
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        ejbTimerRuntimeServiceRef.deactivate(cc);
        defaultPersistentExecutorRef.deactivate(cc);
    }

    TimersPersistentExecutor getPersistentExecutor() {
        TimersPersistentExecutor pExecutor = (TimersPersistentExecutor) ejbTimerRuntimeServiceRef.getServiceWithException().getPersistentExecutor();
        if (pExecutor == null) {
            pExecutor = (TimersPersistentExecutor) defaultPersistentExecutorRef.getService();
            if (pExecutor == null) {
                throw new IllegalStateException("The ejbPersistentTimer feature is enabled, but the defaultEJBPersistentTimerExecutor"
                                                + " persistent executor cannot be resolved. The most likely cause is that the DefaultDataSource datasource"
                                                + " has not been configured. Persistent EJB timers require a datasource configuration for persistence.");
            }
        }
        return pExecutor;
    }

    @Override
    public Timer createPersistentExpirationTimer(BeanId beanId, Date expiration, long interval, @Sensitive Serializable info) {
        J2EEName j2eeName = beanId.getJ2EEName();
        boolean singleton = beanId.getHome().isSingletonSessionHome();
        PersistentTimerTaskHandlerImpl tthandler = singleton ? new SGPersistentTimerTaskHandlerImpl(j2eeName, info, expiration, interval) : new PersistentTimerTaskHandlerImpl(j2eeName, info, expiration, interval);
        TimerStatus<?> timerStatus = (TimerStatus<?>) getPersistentExecutor().schedule(tthandler, tthandler);
        return new PersistentTimerImpl(timerStatus.getTaskId(), j2eeName, beanId.getBeanMetaData().allowCachedTimerDataForMethods, tthandler, timerStatus, this);
    }

    @Override
    public Timer createPersistentCalendarTimer(BeanId beanId, ParsedScheduleExpression parsedExpr, @Sensitive Serializable info) {
        J2EEName j2eeName = beanId.getJ2EEName();
        boolean singleton = beanId.getHome().isSingletonSessionHome();
        PersistentTimerTaskHandlerImpl tthandler = singleton ? new SGPersistentTimerTaskHandlerImpl(j2eeName, info, parsedExpr) : new PersistentTimerTaskHandlerImpl(j2eeName, info, parsedExpr);

        // Don't schedule timer if first runtime (parsedSchedule.getFirstTimeout()) has already passed
        long firstTimeout = parsedExpr.getFirstTimeout();
        if (firstTimeout == -1) {
            return new PersistentTimerImpl(null, j2eeName, beanId.getBeanMetaData().allowCachedTimerDataForMethods, tthandler, null, this);
        }

        TimerStatus<?> timerStatus = (TimerStatus<?>) getPersistentExecutor().schedule(tthandler, tthandler);
        return new PersistentTimerImpl(timerStatus.getTaskId(), j2eeName, beanId.getBeanMetaData().allowCachedTimerDataForMethods, tthandler, timerStatus, this);
    }

    @Override
    public int createPersistentAutomaticTimers(String appName, String moduleName, List<AutomaticTimerBean> timerBeans) throws RuntimeWarning {
        String modulePropertyName = PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyName(appName, moduleName);

        int numCreated = 0;
        boolean commit = true;
        TimersPersistentExecutor persistentExecutor = getPersistentExecutor();
        EmbeddableWebSphereTransactionManager transactionManager = EmbeddableTransactionManagerFactory.getTransactionManager();

        // create all the timers - delegate the work to EJBPersistentTimerRuntime
        try {
            // transaction context
            transactionManager.begin();

            // check if they've already been created - get property group - PersistentExecutor
            if (!persistentExecutor.createProperty(modulePropertyName, "0")) {
                //timers have already been created
                if (TraceComponent.isAnyTracingEnabled() && tcContainer.isDebugEnabled())
                    Tr.debug(tcContainer, "Persistent Automatic Timers have already been created for " + appName + "#" + moduleName);
                commit = false;
                return 0;
            }

            for (AutomaticTimerBean timerBean : timerBeans) {
                if (timerBean.getNumPersistentTimers() != 0) {
                    for (TimerMethodData timerMethod : timerBean.getMethods()) {
                        for (TimerMethodData.AutomaticTimer timer : timerMethod.getAutomaticTimers()) {
                            if (timer.isPersistent()) {

                                ParsedScheduleExpression parsedSchedule = timerBean.parseScheduleExpression(timer);

                                // don't create timer if first runtime (parsedSchedule.getFirstTimeout()) has already passed
                                long firstTimeout = parsedSchedule.getFirstTimeout();
                                if (firstTimeout != -1) {

                                    String className = null;

                                    if (!timer.isXML()) {
                                        className = timerMethod.getMethod().getDeclaringClass().getName();
                                    }

                                    // create and schedule the timer
                                    BeanId beanId = timerBean.getBeanId();
                                    int methodId = timerMethod.getMethodId();
                                    boolean singleton = timerBean.getBeanMetaData().isSingletonSessionBean();
                                    PersistentTimerTaskHandlerImpl tthandler = singleton ? new SGPersistentTimerTaskHandlerImpl(beanId.getJ2EEName(), timer.getInfo(), parsedSchedule, methodId, timerMethod.getMethod().getName(), className) : new PersistentTimerTaskHandlerImpl(beanId.getJ2EEName(), timer.getInfo(), parsedSchedule, methodId, timerMethod.getMethod().getName(), className);
                                    persistentExecutor.schedule(tthandler);

                                    numCreated++;
                                }
                            }
                        }
                    }
                }
            }
            // update the previously created property group after making all the timers - PersistentExecutor
            persistentExecutor.setProperty(modulePropertyName, Integer.toString(numCreated));

            transactionManager.commit();

        } catch (Throwable t) {
            commit = false;
            Tr.error(tcContainer, "AUTOMATIC_TIMER_CREATION_FAILURE_CNTR0218E", moduleName, t);
            throw new RuntimeWarning(t);
        } finally {
            try {
                if (!commit)
                    transactionManager.rollback();
            } catch (Throwable t) {
                // Automatic FFDC only
            }
        }
        return numCreated;
    }

    @Override
    public PersistentTimer getPersistentTimer(long taskId, J2EEName j2eeName, PersistentTimerTaskHandler taskHandler) {
        return new PersistentTimerImpl(taskId, j2eeName, getAllowCachedTimerData(j2eeName), taskHandler, null, this);
    }

    @Override
    public Timer getPersistentTimer(long taskId) {
        // Deserializing timer for stateful bean activation; no caching will be
        // used, until the J2EEName is determined later.
        return new PersistentTimerImpl(taskId, null, 0, null, null, this);
    }

    @Override
    public Timer getPersistentTimerFromStore(long taskId) throws NoSuchObjectLocalException {

        // Since the timer needs to be located in the database to insure it exists, go
        // ahead and read in the timer task handler to determine the J2EEName for the
        // target bean and the data caching options. Go through the TimerStatus to
        // insure both are cached in case caching is allowed.

        TimerStatus<?> timerStatus;
        PersistentTimerTaskHandler taskHandler = null;
        try {
            timerStatus = getPersistentExecutor().getTimerStatus(taskId);
            if (timerStatus != null) {
                taskHandler = (PersistentTimerTaskHandler) timerStatus.getTimer();
            }
        } catch (Throwable ex) {
            String msg = "An error occurred accessing the persistent EJB timer with the " + taskId + " task identifier : " + ex.getMessage();
            throw new TimerServiceException(msg, ex);
        }

        if (taskHandler == null) {
            String msg = "Persistent EJB timer with the " + taskId + " task identifier no longer exists.";
            throw new NoSuchObjectLocalException(msg);
        }

        J2EEName j2eeName = taskHandler.getJ2EEName();
        int cachedTimerDataAllowed = getAllowCachedTimerData(j2eeName);

        return new PersistentTimerImpl(taskId, j2eeName, cachedTimerDataAllowed, taskHandler, timerStatus, this);
    }

    /**
     * Returns the expected next execution of the task with the specified id.
     *
     * @param taskId unique identifier for the task.
     * @return the expected next execution of the task with the specified id.
     *         If the task is not found, is not accessible to the caller, or has ended then <code>null</code> is returned.
     * @throws Exception if an error occurs accessing the persistent store.
     */
    Date getNextExecutionTime(long taskId) throws Exception {
        return getPersistentExecutor().getNextExecutionTime(taskId);
    }

    /**
     * Returns timer task status from the persistent store for the task
     * with the specified id.
     *
     * @param taskId unique identifier for the task.
     *
     * @return status for the persistent task with the specified id.
     *         If the task is not found, <code>null</code> is returned.
     *
     * @throws Exception if an error occurs accessing the persistent store.
     */
    TimerStatus<?> getTimerStatus(long taskId) throws Exception {
        return getPersistentExecutor().getTimerStatus(taskId);
    }

    /**
     * Cancels and removes the specified timer task from the persistent store.
     *
     * @param taskId unique identifier for the task.
     *
     * @return <code>true</code> if the information for the task was removed.
     *         <code>false</code> if the task is not found in the persistent store or is not accessible to the caller.
     *
     * @throws PersistentStoreException if an error occurs that is related to the persistent store.
     */
    boolean remove(long taskId) {
        return getPersistentExecutor().remove(taskId);
    }

    @Override
    public Collection<Timer> getTimers(BeanId beanId) {
        Collection<Timer> timers = new HashSet<Timer>();
        J2EEName j2eeName = beanId.getJ2EEName();
        String taskName = PersistentTimerTaskHandlerImpl.getTaskNameBeanPattern(j2eeName);
        int cachedTimerDataAllowed = beanId.getBeanMetaData().allowCachedTimerDataForMethods;

        try {
            if (cachedTimerDataAllowed != 0) {

                // Caching has been enabled for at least some of the timer data, so
                // go ahead and read all of the timer data from the database at this
                // time. Although the finder method being used here only returns the
                // TaskStatus objects, the corresponding triggers have also been read
                // and are cached on the status, so we may retrieve them later without
                // going to the database.

                List<TimerStatus<?>> tasks = getPersistentExecutor().findTimerStatus(j2eeName.getApplication(), taskName, '\\', TaskState.ANY, true, null, null);

                for (TimerStatus<?> taskStatus : tasks) {
                    timers.add(new PersistentTimerImpl(taskStatus.getTaskId(), j2eeName, cachedTimerDataAllowed, null, taskStatus, this));
                }
            } else {

                // Caching of timer data is NOT allowed, so only read the minimum
                // timer information from the database at this time; specifically
                // just the task Ids. Any subsequent access by the application will
                // require a trip to the database to insure the timer still exists,
                // so additional timer data will be read at that time.

                List<Long> taskIds = getPersistentExecutor().findTaskIds(taskName, '\\', TaskState.ANY, true, null, null);

                for (Long taskId : taskIds) {
                    timers.add(new PersistentTimerImpl(taskId, j2eeName, cachedTimerDataAllowed, null, null, this));
                }
            }
        } catch (Throwable ex) {
            String msg = "An error occurred accessing the persistent EJB timers for the " +
                         j2eeName.getComponent() + " bean in the " + j2eeName.getModule() +
                         " in the " + j2eeName.getApplication() + " application : " + ex.getMessage();
            throw new TimerServiceException(msg, ex);
        }

        return timers;
    }

    @Override
    public Collection<Timer> getAllTimers(String appName, String moduleName, boolean allowsCachedTimerData) {
        Collection<Timer> timers = new HashSet<Timer>();
        String taskName = PersistentTimerTaskHandlerImpl.getTaskNameModulePattern(moduleName);

        try {
            if (allowsCachedTimerData) {

                // Caching has been enabled for at least some of the timer data for
                // some beans in the module, so go ahead and read all of the timer data
                // from the database at this time. Although the finder method being used
                // here only returns the TimerStatus objects, the corresponding triggers
                // have also been read and are cached on the status, so we may retrieve
                // them later without going to the database. J2EEName and caching options
                // will be determined later as needed.

                List<TimerStatus<?>> tasks = getPersistentExecutor().findTimerStatus(appName, taskName, '\\', TaskState.ANY, true, null, null);

                for (TimerStatus<?> taskStatus : tasks) {
                    timers.add(new PersistentTimerImpl(taskStatus.getTaskId(), null, 0, null, taskStatus, this));
                }
            } else {

                // Caching of timer data is NOT allowed, so only read the minimum
                // timer information from the database at this time; specifically
                // just the task IDs.

                List<Long> taskIds = getPersistentExecutor().findTaskIds(taskName, '\\', TaskState.ANY, true, null, null);

                for (Long taskId : taskIds) {
                    timers.add(new PersistentTimerImpl(taskId, null, 0, null, null, this));
                }
            }
        } catch (Throwable ex) {
            String msg = "An error occurred accessing the persistent EJB timers for the " +
                         moduleName + " in the " + appName + " application : " + ex.getMessage();
            throw new TimerServiceException(msg, ex);
        }

        return timers;
    }

    /**
     * Return the allowed cached timer data setting for the specified bean.
     */
    protected int getAllowCachedTimerData(J2EEName j2eeName) {
        Integer allowCachedTimerData = null;
        Map<String, Integer> localAllowCachedTimerDataMap = allowCachedTimerDataMap;
        if (localAllowCachedTimerDataMap != null) {
            allowCachedTimerData = localAllowCachedTimerDataMap.get(j2eeName.toString());
            if (allowCachedTimerData == null) {
                allowCachedTimerData = localAllowCachedTimerDataMap.get("*");
            }
        }
        return allowCachedTimerData != null ? allowCachedTimerData : 0;
    }

    @Override
    public void resetAndCheckDatabasePolling() {
        enabledDatabasePolling = false;
        checkStartPolling();
    }

    @Override
    public void enableDatabasePolling() {
        hasSetupTimers = true;
        checkStartPolling();
    }

    private void checkStartPolling() {
        if (serverStopping) {
            return;
        }

        if (!enabledDatabasePolling && hasSetupTimers) {
            Long initialPollDelay;
            ServiceReference<PersistentExecutor> defaultPExecutorRef;
            EJBTimerRuntime timerRuntime = ejbTimerRuntimeServiceRef.getService();
            if (timerRuntime != null && timerRuntime.getPersistentExecutorRef() != null) {
                initialPollDelay = (Long) timerRuntime.getPersistentExecutorRef().getProperty("initialPollDelay");
            } else if ((defaultPExecutorRef = defaultPersistentExecutorRef.getReference()) != null) {
                initialPollDelay = (Long) defaultPExecutorRef.getProperty("initialPollDelay");
            } else {
                // Unable to determine which PersistentExecutor is being used; configuration is likely
                // incomplete.

                // Go ahead and attempt to start polling below, which will result in the
                // appropriate exception being thrown based on the configuration that is present.
                initialPollDelay = null;
            }

            if (TraceComponent.isAnyTracingEnabled() && tcContainer.isDebugEnabled()) {
                if (initialPollDelay != null) {
                    Tr.debug(tcContainer, "initial: " + initialPollDelay.longValue());
                } else {
                    Tr.debug(tcContainer, "Unable to determine which PersistentExecutor is being used");
                }
            }
            if (initialPollDelay == null || initialPollDelay.longValue() == -1) {
                getPersistentExecutor().startPolling();
                enabledDatabasePolling = true;
            }
        }
    }

    @Override
    public void serverStopping() {
        serverStopping = true;
    }

    @Trivial
    @Override
    public boolean isConfigured() {
        ServiceReference<PersistentExecutor> defaultPExecutorRef;
        EJBTimerRuntime timerRuntime = ejbTimerRuntimeServiceRef.getService();
        if (timerRuntime != null && timerRuntime.getPersistentExecutorRef() != null) {
            if (TraceComponent.isAnyTracingEnabled() && tcContainer.isDebugEnabled())
                Tr.debug(tcContainer, "isServiceConfigured : true : configured persistent executor");
            return true;
        } else if ((defaultPExecutorRef = defaultPersistentExecutorRef.getReference()) != null) {
            if (TraceComponent.isAnyTracingEnabled() && tcContainer.isDebugEnabled())
                Tr.debug(tcContainer, "isServiceConfigured : true : default persistent executor");
            return true;
        }
        // Unable to determine which PersistentExecutor is being used; configuration is incomplete.
        if (TraceComponent.isAnyTracingEnabled() && tcContainer.isDebugEnabled())
            Tr.debug(tcContainer, "isServiceConfigured : false : no persistent executor");
        return false;
    }

    /**
     * Returns the configured missed timer action for persistent timers. When not
     * explicitly configured, the missed timer action will default to ONCE when
     * failover is enabled for the PersistentExecutor, otherwise ALL.
     *
     * @return missed timer action for persistent timers
     */
    @Trivial
    MissedTimerAction getMissedTimerAction() {
        // Explicitly configured for timerService
        if (missedTimerAction != null) {
            if (TraceComponent.isAnyTracingEnabled() && tcContainer.isDebugEnabled())
                Tr.debug(tcContainer, "MissedTimerAction = " + missedTimerAction);
            return missedTimerAction;
        }

        try {
            // Default to ONCE if failover is enabled
            if (getPersistentExecutor().isFailOverEnabled()) {
                if (TraceComponent.isAnyTracingEnabled() && tcContainer.isDebugEnabled())
                    Tr.debug(tcContainer, "MissedTimerAction = " + MissedTimerAction.ONCE);
                return MissedTimerAction.ONCE;
            }
        } catch (Throwable ex) {
            // Although unusual; obtaining the PersistentExecutor may fail if it has not
            // been configured properly. Allow FFDC to be collected, but otherwise just
            // fall through and return the default of ALL. The error will be reported
            // later when another attempt is made to use the PersistentExecutor.
            if (TraceComponent.isAnyTracingEnabled() && tcContainer.isDebugEnabled())
                Tr.debug(tcContainer, "getMissedTimerAction : " + ex);
        }

        // Legacy default has been to run all expirations
        if (TraceComponent.isAnyTracingEnabled() && tcContainer.isDebugEnabled())
            Tr.debug(tcContainer, "MissedTimerAction = " + MissedTimerAction.ALL);
        return MissedTimerAction.ALL;
    }
}
