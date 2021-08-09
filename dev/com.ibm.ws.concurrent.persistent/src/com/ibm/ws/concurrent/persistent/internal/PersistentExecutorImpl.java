/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.Trigger;
import javax.management.MalformedObjectNameException;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.concurrent.persistent.AutoPurge;
import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.PersistentStoreException;
import com.ibm.websphere.concurrent.persistent.TaskState;
import com.ibm.websphere.concurrent.persistent.TaskStatus;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.classloading.ClassLoaderIdentifierService;
import com.ibm.ws.concurrent.persistent.db.DatabaseTaskStore;
import com.ibm.ws.concurrent.persistent.ejb.TaskLocker;
import com.ibm.ws.concurrent.persistent.ejb.TimerStatus;
import com.ibm.ws.concurrent.persistent.ejb.TimerTrigger;
import com.ibm.ws.concurrent.persistent.ejb.TimersPersistentExecutor;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.feature.ServerStarted;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.update.RuntimeUpdateListener;
import com.ibm.ws.runtime.update.RuntimeUpdateManager;
import com.ibm.ws.runtime.update.RuntimeUpdateNotification;
import com.ibm.ws.serialization.SerializationService;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleContext;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;
import com.ibm.wsspi.concurrent.persistent.PartitionRecord;
import com.ibm.wsspi.concurrent.persistent.TaskRecord;
import com.ibm.wsspi.concurrent.persistent.TaskStore;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;
import com.ibm.wsspi.persistence.DDLGenerationParticipant;
import com.ibm.wsspi.persistence.DatabaseStore;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Schedules and runs persistent tasks.
 */
@Component(
           name = "com.ibm.ws.concurrent.persistent.executor",
           service = {
                       ApplicationRecycleComponent.class,
                       DDLGenerationParticipant.class,
                       ExecutorService.class,
                       ManagedExecutorService.class,
                       ManagedScheduledExecutorService.class,
                       PersistentExecutor.class,
                       ResourceFactory.class,
                       RuntimeUpdateListener.class,
                       ScheduledExecutorService.class,
                       ServerQuiesceListener.class },
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true,
           property = {
                        "creates.objectClass=java.util.concurrent.ExecutorService",
                        "creates.objectClass=java.util.concurrent.ScheduledExecutorService",
                        "creates.objectClass=javax.enterprise.concurrent.ManagedExecutorService",
                        "creates.objectClass=javax.enterprise.concurrent.ManagedScheduledExecutorService",
                        "creates.objectClass=com.ibm.websphere.concurrent.persistent.PersistentExecutor" })
public class PersistentExecutorImpl implements ApplicationRecycleComponent, DDLGenerationParticipant, ResourceFactory, RuntimeUpdateListener, PersistentExecutor, ServerQuiesceListener, TimersPersistentExecutor {
    private static final TraceComponent tc = Tr.register(PersistentExecutorImpl.class);

    /**
     * Name of reference to the ApplicationRecycleCoordinator
     */
    private static final String APP_RECYCLE_SERVICE = "appRecycleService";

    /**
     * Privileged action to obtain the host name. The host name (along with server name and executor name)
     * is needed to identify the partition that correspond to this persistentExecutor instance.
     */
    private static final PrivilegedExceptionAction<String> getHostName = new PrivilegedExceptionAction<String>() {
        @Override
        @Trivial
        public String run() throws UnknownHostException {
            return InetAddress.getLocalHost().getHostName();
        }
    };

    /**
     * Constant for ManagedTask.IDENTITY_NAME in whichever of Jakarta vs Java EE is NOT enabled
     */
    private static final String OTHER_SPEC_IDENTITY_NAME;

    /**
     * Constant for ManagedTask.LONGRUNNING_HINT in whichever of Jakarta vs Java EE is NOT enabled
     */
    private static final String OTHER_SPEC_LONGRUNNING_HINT;

    /**
     * Constant for ManagedTask.TRANSACTION in whichever of Jakarta vs Java EE is NOT enabled
     */
    private static final String OTHER_SPEC_TRANSACTION_CONSTANT;

    static {
        boolean jakarta = ManagedTask.IDENTITY_NAME.charAt(7) == '.';

        OTHER_SPEC_IDENTITY_NAME = new StringBuilder(jakarta ? 41 : 43) //
                        .append(jakarta ? "javax" : "jakarta") //
                        .append(".enterprise.concurrent.IDENTITY_NAME") //
                        .toString();

        OTHER_SPEC_LONGRUNNING_HINT = new StringBuilder(jakarta ? 44 : 46) //
                        .append(jakarta ? "javax" : "jakarta") //
                        .append(".enterprise.concurrent.LONGRUNNING_HINT") //
                        .toString();

        OTHER_SPEC_TRANSACTION_CONSTANT = new StringBuilder(jakarta ? 39 : 41) //
                        .append(jakarta ? "javax" : "jakarta") //
                        .append(".enterprise.concurrent.TRANSACTION") //
                        .toString();
    }

    /**
     * Names of applications using this ResourceFactory
     */
    private final Set<String> applications = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /**
     * Keeps track of application availability and tasks that are deferred until applications become available.
     */
    final AtomicServiceReference<ApplicationTracker> appTrackerRef = new AtomicServiceReference<ApplicationTracker>("ApplicationTracker");

    /**
     * Class loader identifier service.
     */
    ClassLoaderIdentifierService classloaderIdSvc;

    /**
     * Modifiable configuration for this persistent executor instance.
     */
    final AtomicReference<Config> configRef = new AtomicReference<Config>();

    /**
     * Reference to the thread context service.
     */
    private final AtomicServiceReference<WSContextService> contextSvcRef = new AtomicServiceReference<WSContextService>("ContextService");

    /**
     * Indicates if this persistent executor instance has been deactivated.
     */
    volatile boolean deactivated;

    /**
     * Default execution properties to use when none are present for the task.
     */
    private final Map<String, String> defaultExecProps = new TreeMap<String, String>();

    /**
     * Common Liberty thread pool.
     */
    ExecutorService executor;

    /**
     * Set of task ids that are scheduled in memory. When polling for tasks, we can ignore these because they are already scheduled.
     */
    final ConcurrentHashMap<Long, Boolean> inMemoryTaskIds = new ConcurrentHashMap<Long, Boolean>();

    /**
     * Reference to a service that controls local transactions.
     */
    private final AtomicServiceReference<LocalTransactionCurrent> localTranCurrentRef = new AtomicServiceReference<LocalTransactionCurrent>("LocalTransactionCurrent");

    /**
     * WsLocationAdmin service.
     */
    private WsLocationAdmin locationAdmin;

    /**
     * Persistent Executor MBean
     */
    private PersistentExecutorMBeanImpl mbean = null;

    /**
     * Id (if top level and has an id),
     * JNDI name (if top level and has no id),
     * or config.displayId (if nested)
     * of this persistentExecutor instance.
     */
    String name;

    /**
     * Unique identifier for a partition of the task table based on the combination of
     * (persistent executor, Liberty server, host name).
     * Only used when fail over (missedTaskThreshold) is disabled.
     */
    private long partitionId;

    /**
     * Unique identifier for a partition of the responsibility for polling across all
     * instances that are configured with fail over (missedTaskThreshold) enabled and
     * pollInterval unspecified.
     */
    private long partitionIdForPolling;

    /**
     * Lock that must be used when accessing the partitionId and partitionIdForPolling.
     */
    private final ReadWriteLock partitionIdLock = new ReentrantReadWriteLock();

    /**
     * Persistent store.
     */
    private DatabaseStore persistentStore;

    /**
     * config.displayId for the persistent store configuration element.
     */
    private String persistentStoreDisplayId;

    /**
     * Reference to the future for the next (or current) poll.
     */
    private final AtomicReference<ScheduledFuture<?>> pollingFutureRef = new AtomicReference<ScheduledFuture<?>>();

    /**
     * Indicates if we received a signal from the user to start polling.
     * This applies if the initialPollDelay is set to -1.
     */
    private final AtomicBoolean pollingStartSignalReceived = new AtomicBoolean();

    /**
     * Liberty scheduled executor.
     */
    @Reference(target = "(deferrable=false)")
    protected volatile ScheduledExecutorService scheduledExecutor;

    /**
     * Liberty serialization service.
     */
    private final AtomicServiceReference<SerializationService> serializationSvcRef = new AtomicServiceReference<SerializationService>("SerializationService");

    /**
     * Persistent task store.
     */
    TaskStore taskStore;

    /**
     * Reference to the transaction manager, which is also the unit of work manager.
     */
    final AtomicServiceReference<EmbeddableWebSphereTransactionManager> tranMgrRef = new AtomicServiceReference<EmbeddableWebSphereTransactionManager>("TransactionManager");

    /**
     * The variable registry for the server.
     */
    private VariableRegistry variableRegistry;

    /**
     * Indication that the polling task can be started.
     */
    private final PollingManager readyForPollingTask = new PollingManager();

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context DeclarativeService defined/populated component context
     * @throws MalformedObjectNameException
     */
    @Trivial
    protected void activate(ComponentContext context) throws MalformedObjectNameException {
        Dictionary<String, ?> properties = context.getProperties();
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "activate", properties);

        Config config = new Config(properties);
        configRef.set(config);

        // Precedence for identifying the persistent identifier name must be:
        // 1) config.displayId if contains "]/persistentExecutor[" which means we are nested
        // 2) id
        // 3) jndiName
        // 4) error
        String displayId = (String) properties.get("config.displayId");
        name = displayId.contains("]/persistentExecutor[") ? displayId : (String) properties.get("id");
        if (name == null) {
            name = config.jndiName;
            if (name == null)
                throw new IllegalArgumentException("id: null, jndiName: null");
        }

        defaultExecProps.put(ManagedTask.TRANSACTION, ManagedTask.USE_TRANSACTION_OF_EXECUTION_THREAD);
        defaultExecProps.put(WSContextService.DEFAULT_CONTEXT, WSContextService.UNCONFIGURED_CONTEXT_TYPES);
        defaultExecProps.put(WSContextService.TASK_OWNER, name);

        appTrackerRef.activate(context);
        contextSvcRef.activate(context);
        localTranCurrentRef.activate(context);
        serializationSvcRef.activate(context);
        tranMgrRef.activate(context);

        taskStore = DatabaseTaskStore.get(persistentStore);

        if (config.initialPollDelay < 0)
            readyForPollingTask.add(PollingManager.SIGNAL_REQUIRED);

        if (config.enableTaskExecution)
            readyForPollingTask.add(PollingManager.EXECUTION_ENABLED);

        if (readyForPollingTask.addAndCheckIfReady(PollingManager.DS_READY))
            startPollingTask(config);

        if (config.missedTaskThreshold == -1) {
            // PersistentExecutorMBean is undocumented, experimental, and not supported.
            // We would like to remove its registration under all circumstances, but that could potentially break some one who is using it
            mbean = new PersistentExecutorMBeanImpl(this);
            mbean.register(InvokerTask.priv.getBundleContext(context));
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "activate");
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        // Section 3.1.6.1 of the Concurrency Utilities spec requires IllegalStateException
        throw new IllegalStateException(new UnsupportedOperationException("awaitTermination"));
    }

    /** {@inheritDoc} */
    @Override
    public int cancel(String pattern, Character escape, TaskState state, boolean inState) {
        String owner = getOwner();
        if (owner == null)
            return 0;

        pattern = pattern == null ? null : Utils.normalizeString(pattern);
        int updateCount = 0;
        TransactionController tranController = new TransactionController();
        try {
            tranController.preInvoke();
            updateCount = taskStore.cancel(pattern, escape, state, inState, owner);
        } catch (Throwable x) {
            tranController.setFailure(x);
        } finally {
            PersistentStoreException x = tranController.postInvoke(PersistentStoreException.class); // TODO proposed spec class
            if (x != null)
                throw x;
        }

        return updateCount;
    }

    /** {@inheritDoc} */
    @Override
    public boolean createProperty(String name, String value) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("name: " + name);
        if (value == null || value.length() == 0)
            throw new IllegalArgumentException("value: " + value);
        boolean created = false;
        TransactionController tranController = new TransactionController();
        try {
            tranController.preInvoke();
            created = taskStore.createProperty(name, value);
            if (!created)
                tranController.expectRollback();
        } catch (Throwable x) {
            tranController.setFailure(x);
        } finally {
            PersistentStoreException x = tranController.postInvoke(PersistentStoreException.class); // TODO proposed spec class
            if (x != null)
                throw x;
        }

        return created;
    }

    /** {@inheritDoc} */
    @Override
    public Object createResource(ResourceInfo ref) throws Exception {
        ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cData != null)
            applications.add(cData.getJ2EEName().getApplication());

        return this;
    }

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context DeclarativeService defined/populated component context
     */
    protected void deactivate(ComponentContext context) throws Exception {
        deactivated = true;
        if (mbean != null) {
            mbean.unregister();
            mbean = null;
        }

        if (taskStore != null)
            DatabaseTaskStore.unget(persistentStore);
        appTrackerRef.deactivate(context);
        contextSvcRef.deactivate(context);
        localTranCurrentRef.deactivate(context);
        serializationSvcRef.deactivate(context);
        tranMgrRef.deactivate(context);
    }

    /**
     * Utility method that deserializes an object from bytes.
     *
     * @param bytes  from which to deserialize an object. If null, then null should be returned as the result.
     * @param loader optional class loader to use when deserializing.
     * @return deserialized object.
     * @throws IOException if an error occurs deserializing the object.
     */
    public final Object deserialize(byte[] bytes, ClassLoader loader) throws ClassNotFoundException, IOException {
        if (bytes == null)
            return null;

        InputStream iin = new InflaterInputStream(new ByteArrayInputStream(bytes));
        SerializationService serializationSvc = serializationSvcRef.getService();
        ObjectInputStream oin = loader == null || serializationSvc == null ? new ObjectInputStream(iin) : serializationSvc.createObjectInputStream(iin, loader);
        try {
            return oin.readObject();
        } finally {
            oin.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void execute(Runnable runnable) {
        TaskInfo taskInfo = new TaskInfo(false);
        taskInfo.initForOneShotTask(0l); // run immediately

        newTask(runnable, taskInfo, null, null);
    }

    /**
     * Finds partition information in the persistent store. All of the parameters are optional.
     * If a parameter is specified, only entries that match it are retrieved from the persistent store.
     *
     * This method is for the mbean only.
     *
     * @param hostName           the host name.
     * @param userDir            wlp.user.dir
     * @param libertyServerName  name of the Liberty server.
     * @param executorIdentifier config.displayId of the persistent executor.
     * @return a list of partition information. Each entry in the list consists of
     *         (partitionId, hostName, userDir, libertyServerName, executorIdentifier)
     * @throws Exception if an error occurs.
     */
    String[][] findPartitionInfo(String hostName, String userDir, String libertyServerName, String executorIdentifier) throws Exception {
        Config config = configRef.get();
        if (config.missedTaskThreshold != -1)
            throw new UnsupportedOperationException(); // should be unreachable

        PartitionRecord criteria = new PartitionRecord(false);
        if (hostName != null)
            criteria.setHostName(hostName);
        if (userDir != null)
            criteria.setUserDir(userDir);
        if (libertyServerName != null)
            criteria.setLibertyServer(libertyServerName);
        if (executorIdentifier != null)
            criteria.setExecutor(executorIdentifier);

        List<PartitionRecord> records = null;
        TransactionController tranController = new TransactionController();
        try {
            tranController.preInvoke();
            records = taskStore.find(criteria);
        } catch (Throwable x) {
            tranController.setFailure(x);
        } finally {
            Exception x = tranController.postInvoke(Exception.class);
            if (x != null)
                throw x;
        }

        String[][] partitionInfo = new String[records == null ? 0 : records.size()][5];
        if (records != null) // guard against impossible null value to make FindBugs stop complaining
            for (int i = 0; i < records.size(); i++) {
                PartitionRecord record = records.get(i);
                partitionInfo[i][0] = Long.toString(record.getId());
                partitionInfo[i][1] = record.getHostName();
                partitionInfo[i][2] = record.getUserDir();
                partitionInfo[i][3] = record.getLibertyServer();
                partitionInfo[i][4] = record.getExecutor();
            }
        return partitionInfo;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> findProperties(String pattern, Character escape) throws Exception {
        if (pattern == null)
            throw new NullPointerException("pattern");

        Map<String, String> map = null;
        TransactionController tranController = new TransactionController();
        try {
            tranController.preInvoke();
            map = taskStore.getProperties(Utils.normalizeString(pattern), escape);
        } catch (Throwable x) {
            tranController.setFailure(x);
        } finally {
            Exception x = tranController.postInvoke(Exception.class);
            if (x != null)
                throw x;
        }

        return map;
    }

    /**
     * Find all task IDs for tasks that match the specified partition id and the presence or absence
     * (as determined by the inState attribute) of the specified state.
     * For example, to find taskIDs for the first 100 tasks in partition 12 that have not completed all executions,
     * taskStore.findTaskIds(12, TaskState.ENDED, false, null, 100);
     *
     * This method is for the mbean only.
     *
     * @param partition  identifier of the partition in which to search for tasks.
     * @param state      a task state. For example, TaskState.SCHEDULED
     * @param inState    indicates whether to include or exclude results with the specified state
     * @param minId      minimum value for task id to be returned in the results. A null value means no minimum.
     * @param maxResults limits the number of results to return to the specified maximum value. A null value means no limit.
     * @return in-memory, ordered list of task ID.
     * @throws Exception if an error occurs when attempting to access the persistent task store.
     */
    Long[] findTaskIds(long partition, TaskState state, boolean inState, Long minId, Integer maxResults) throws Exception {
        Config config = configRef.get();
        if (config.missedTaskThreshold != -1)
            throw new UnsupportedOperationException(); // should be unreachable

        Long[] results = null;
        TransactionController tranController = new TransactionController();
        try {
            tranController.preInvoke();
            List<Long> ids = taskStore.findTaskIds(null, null, state, inState, minId, maxResults, null, partition);
            results = ids.toArray(new Long[ids.size()]);
        } catch (Throwable x) {
            tranController.setFailure(x);
        } finally {
            Exception x = tranController.postInvoke(Exception.class);
            if (x != null)
                throw x;
        }

        return results;
    }

    /** {@inheritDoc} */
    @Override
    public List<Long> findTaskIds(String pattern, Character escape, TaskState state, boolean inState, Long minId, Integer maxResults) {
        String owner = getOwner();
        if (owner == null)
            return Collections.emptyList(); // empty results

        pattern = pattern == null ? null : Utils.normalizeString(pattern);
        List<Long> results = null;
        TransactionController tranController = new TransactionController();
        try {
            tranController.preInvoke();
            results = taskStore.findTaskIds(pattern, escape, state, inState, minId, maxResults, owner, null);
        } catch (Throwable x) {
            tranController.setFailure(x);
        } finally {
            PersistentStoreException x = tranController.postInvoke(PersistentStoreException.class); // TODO proposed spec class
            if (x != null)
                throw x;
        }

        return results;
    }

    /** {@inheritDoc} */
    @Override
    public List<TimerStatus<?>> findTimerStatus(String appName, String pattern, Character escape, TaskState state, boolean inState, Long minId,
                                                Integer maxResults) throws Exception {
        pattern = pattern == null ? null : Utils.normalizeString(pattern);
        List<?> results = null;
        TransactionController tranController = new TransactionController();
        try {
            tranController.preInvoke();
            results = taskStore.findTaskStatus(pattern, escape, state, inState, minId, maxResults, appName, true, this);
        } catch (Throwable x) {
            tranController.setFailure(x);
        } finally {
            Exception x = tranController.postInvoke(Exception.class);
            if (x != null)
                throw x;
        }

        @SuppressWarnings("unchecked")
        List<TimerStatus<?>> timerStatusResults = (List<TimerStatus<?>>) results;
        return timerStatusResults;
    }

    /** {@inheritDoc} */
    @Override
    public List<TaskStatus<?>> findTaskStatus(String pattern, Character escape, TaskState state, boolean inState, Long minId, Integer maxResults) {
        String owner = getOwner();
        if (owner == null)
            return Collections.emptyList();

        pattern = pattern == null ? null : Utils.normalizeString(pattern);
        List<TaskStatus<?>> results = null;
        TransactionController tranController = new TransactionController();
        try {
            tranController.preInvoke();
            results = taskStore.findTaskStatus(pattern, escape, state, inState, minId, maxResults, owner, false, this);
        } catch (Throwable x) {
            tranController.setFailure(x);
        } finally {
            PersistentStoreException x = tranController.postInvoke(PersistentStoreException.class); // TODO proposed spec class
            if (x != null)
                throw x;
        }

        return results;
    }

    /**
     * @see com.ibm.wsspi.persistence.DDLGenerationParticipant#generate(java.io.Writer)
     */
    @Override
    public void generate(Writer out) throws Exception {
        ((DatabaseTaskStore) taskStore).getPersistenceServiceUnit().generateDDL(out);
    }

    @Override
    public ApplicationRecycleContext getContext() {
        return null;
    }

    /**
     * @see com.ibm.wsspi.persistence.DDLGenerationParticipant#getDDLFileName()
     */
    @Override
    public String getDDLFileName() {
        // If databaseStore is a nested element, create file names of the form,
        // persistentExecutor[myExecutor]/databaseStore[default-0].dll

        // If databaseStore is a top level element, create files names of the form,
        // databaseStore[topLevelId]_persistentExecutor.dll

        String name = persistentStoreDisplayId;
        if (!name.contains("]/"))
            name += "_persistentExecutor";

        return name;
    }

    @Override
    public Set<String> getDependentApplications() {
        Set<String> members = new HashSet<String>(applications);
        applications.removeAll(members);
        return members;
    }

    /**
     * Returns the execution properties for the specified task.
     *
     * @param task Callable or Runnable which might or might not implement ManagedTask.
     * @return the execution properties for the specified task.
     */
    @Trivial
    Map<String, String> getExecutionProperties(Object task) {
        Map<String, String> execProps = task instanceof ManagedTask ? ((ManagedTask) task).getExecutionProperties() : null;
        if (execProps == null)
            execProps = defaultExecProps;
        else {
            Map<String, String> mergedProps;
            if (execProps.containsKey(ManagedTask.TRANSACTION) || execProps.containsKey(OTHER_SPEC_TRANSACTION_CONSTANT)) {
                mergedProps = new TreeMap<String, String>();
                mergedProps.put(WSContextService.DEFAULT_CONTEXT, WSContextService.UNCONFIGURED_CONTEXT_TYPES);
                mergedProps.put(WSContextService.TASK_OWNER, name);
            } else {
                mergedProps = new TreeMap<String, String>(defaultExecProps);
            }
            mergedProps.putAll(execProps);
            execProps = mergedProps;
        }
        return execProps;
    }

    /** {@inheritDoc} */
    @Override
    public Date getNextExecutionTime(long taskId) throws Exception {
        TaskRecord taskRecord = null;
        TransactionController tranController = new TransactionController();
        try {
            tranController.preInvoke();
            taskRecord = taskStore.getNextExecutionTime(taskId, null);
        } catch (Throwable x) {
            tranController.setFailure(x);
        } finally {
            Exception x = tranController.postInvoke(Exception.class);
            if (x != null)
                throw x;
        }
        return taskRecord == null || (taskRecord.getState() & TaskState.ENDED.bit) != 0 ? null : new Date(taskRecord.getNextExecutionTime());
    }

    /**
     * Returns the partition id for this (persistent executor, Liberty server, host name) combination.
     *
     * @return the partition id.
     * @throws Exception if unable to obtain a partition id.
     */
    @FFDCIgnore(Exception.class)
    long getPartitionId() throws Exception {
        partitionIdLock.readLock().lock();
        try {
            if (partitionId != 0)
                return partitionId;
        } finally {
            partitionIdLock.readLock().unlock();
        }

        PartitionRecord partitionEntry = new PartitionRecord(false);
        partitionEntry.setExecutor(name);
        partitionEntry.setLibertyServer(locationAdmin.getServerName());
        partitionEntry.setUserDir(variableRegistry.resolveString(VariableRegistry.USER_DIR));
        partitionEntry.setHostName(AccessController.doPrivileged(getHostName));
        partitionEntry.setExpiry(Long.MAX_VALUE);

        // Run under a new transaction and commit right away
        EmbeddableWebSphereTransactionManager tranMgr = tranMgrRef.getServiceWithException();
        int tranStatus = tranMgr.getStatus();
        LocalTransactionCurrent ltcCurrent = tranStatus == Status.STATUS_NO_TRANSACTION ? localTranCurrentRef.getServiceWithException() : null;
        LocalTransactionCoordinator suspendedLTC = ltcCurrent == null ? null : ltcCurrent.suspend();
        Transaction suspendedTran = tranStatus == Status.STATUS_ACTIVE ? tranMgr.suspend() : null;
        Exception failure = null;
        try {
            partitionIdLock.writeLock().lock();
            try {
                if (partitionId != 0)
                    return partitionId;

                Long newPartitionId = null;
                for (int i = 0; i < 2 && newPartitionId == null && !deactivated; i++) {
                    failure = null;
                    tranMgr.begin();
                    try {
                        newPartitionId = taskStore.findOrCreate(partitionEntry);
                    } catch (Exception x) {
                        failure = x;
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(this, tc, "deactivated? " + deactivated, x);
                    } finally {
                        if (failure == null && newPartitionId != null) {
                            tranMgr.commit();
                            partitionId = newPartitionId;
                        } else
                            tranMgr.rollback();
                    }
                }

                return partitionId;
            } finally {
                partitionIdLock.writeLock().unlock();
            }
        } finally {
            try {
                // resume
                if (suspendedTran != null)
                    tranMgr.resume(suspendedTran);
                else if (suspendedLTC != null)
                    ltcCurrent.resume(suspendedLTC);
            } finally {
                if (failure != null)
                    throw failure;
            }
        }
    }

    /**
     * Returns the id of the poll partition, which is shared across all instances that are configured
     * with missedTaskThreshold enabled and pollInterval unspecified.
     *
     * @return the partition id for the polling partition.
     * @throws Exception if unable to obtain the poll partition id.
     */
    @FFDCIgnore(Exception.class)
    long getPollPartitionId() throws Exception {
        partitionIdLock.readLock().lock();
        try {
            if (partitionIdForPolling != 0)
                return partitionIdForPolling;
        } finally {
            partitionIdLock.readLock().unlock();
        }

        // Run under a new transaction and commit right away
        EmbeddableWebSphereTransactionManager tranMgr = tranMgrRef.getServiceWithException();
        int tranStatus = tranMgr.getStatus();
        LocalTransactionCurrent ltcCurrent = tranStatus == Status.STATUS_NO_TRANSACTION ? localTranCurrentRef.getServiceWithException() : null;
        LocalTransactionCoordinator suspendedLTC = ltcCurrent == null ? null : ltcCurrent.suspend();
        Transaction suspendedTran = tranStatus == Status.STATUS_ACTIVE ? tranMgr.suspend() : null;
        Exception failure = null;
        try {
            partitionIdLock.writeLock().lock();
            try {
                if (partitionIdForPolling != 0)
                    return partitionIdForPolling;

                Long newPollPartitionId = null;
                for (int i = 0; i < 10 && newPollPartitionId == null && !deactivated; i++) {
                    failure = null;
                    tranMgr.begin();
                    try {
                        newPollPartitionId = taskStore.findOrCreatePollPartition();
                    } catch (Exception x) {
                        failure = x;
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(this, tc, "deactivated? " + deactivated, x);
                    } finally {
                        if (failure == null && newPollPartitionId != null) {
                            tranMgr.commit();
                            partitionIdForPolling = newPollPartitionId;
                        } else
                            tranMgr.rollback();
                    }
                }

                return partitionIdForPolling;
            } finally {
                partitionIdLock.writeLock().unlock();
            }
        } finally {
            try {
                // resume
                if (suspendedTran != null)
                    tranMgr.resume(suspendedTran);
                else if (suspendedLTC != null)
                    ltcCurrent.resume(suspendedLTC);
            } finally {
                if (failure != null)
                    throw failure;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getProperty(String name) {
        String value = null;
        if (name != null) {
            TransactionController tranController = new TransactionController();
            try {
                tranController.preInvoke();
                value = taskStore.getProperty(name);
            } catch (Throwable x) {
                tranController.setFailure(x);
            } finally {
                PersistentStoreException x = tranController.postInvoke(PersistentStoreException.class); // TODO proposed spec class
                if (x != null)
                    throw x;
            }
        }

        return value;
    }

    /**
     * Returns status for the persistent task with the specified id.
     *
     * @param taskId unique identifier for the task.
     * @return status for the persistent task with the specified id.
     *         If the task is not found, <code>null</code> is returned.
     */
    @Override
    public <T> TaskStatus<T> getStatus(long taskId) {
        String owner = getOwner();
        if (owner == null)
            return null;

        TransactionController tranController = new TransactionController();
        TaskRecord taskRecord = null;
        try {
            tranController.preInvoke();
            taskRecord = taskStore.findById(taskId, owner, false);
        } catch (Throwable x) {
            tranController.setFailure(x);
        } finally {
            PersistentStoreException x = tranController.postInvoke(PersistentStoreException.class); // TODO proposed spec class
            if (x != null)
                throw x;
        }

        return taskRecord == null ? null : new TaskStatusImpl<T>(taskRecord, this);
    }

    /** {@inheritDoc} */
    @Override
    public <T> TimerStatus<T> getTimerStatus(long taskId) throws Exception {
        TransactionController tranController = new TransactionController();
        TaskRecord taskRecord = null;
        try {
            tranController.preInvoke();
            taskRecord = taskStore.findById(taskId, null, true);
        } catch (Throwable x) {
            tranController.setFailure(x);
        } finally {
            Exception x = tranController.postInvoke(Exception.class);
            if (x != null)
                throw x;
        }

        return taskRecord == null ? null : new TaskStatusImpl<T>(taskRecord, this);
    }

    /** {@inheritDoc} */
    @Override
    public TimerTrigger getTimer(long taskId) throws ClassNotFoundException, Exception, IOException {
        TaskRecord taskRecord = null;
        TransactionController tranController = new TransactionController();
        try {
            tranController.preInvoke();
            taskRecord = taskStore.getTrigger(taskId);
        } catch (Throwable x) {
            tranController.setFailure(x);
        } finally {
            Exception x = tranController.postInvoke(Exception.class);
            if (x != null)
                throw x;
        }
        if (taskRecord == null || (taskRecord.getState() & TaskState.ENDED.bit) != 0)
            return null;

        // The task/trigger for EJB persistent timers does not require the application class loader to deserialize.
        byte[] triggerBytes = taskRecord.getTrigger();
        TimerTrigger trigger = triggerBytes == null ? null : (TimerTrigger) deserialize(triggerBytes, InvokerTask.priv.getSystemClassLoader());
        return trigger;
    }

    /**
     * Utility method to compute the owner that should be used for isolation of tasks (typically between applications).
     * If component metadata exists on the thread, the application name is used.
     * Otherwise, if a thread context class loader is present, the application name from the class loader identifier is used.
     * Otherwise, null is returned.
     *
     * @return name of the task owner.
     */
    @Trivial
    private final String getOwner() {
        ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        String name = cData == null ? null : cData.getJ2EEName().getApplication();
        if (name == null) {
            ClassLoader threadContextClassLoader = InvokerTask.priv.getContextClassLoader();
            String identifier = classloaderIdSvc.getClassLoaderIdentifier(threadContextClassLoader);
            // Parse the app name from the identifier. For example, from WebModule:app#module#component
            if (identifier != null) {
                int start = identifier.indexOf(':');
                if (start > 0) {
                    int end = identifier.indexOf('#', ++start);
                    if (end > 0)
                        name = identifier.substring(start, end);
                }
            }
        }
        return name;
    }

    /**
     * Dump internal state to the introspector.
     *
     * @param out writer for the introspector.
     */
    void introspect(PrintWriter out) {
        out.println(toString() + ' ' + name + (deactivated ? " is deactivated" : ""));

        out.print("  Partition ");
        if (partitionIdLock.readLock().tryLock())
            try {
                out.println(partitionId);
            } finally {
                partitionIdLock.readLock().unlock();
            }
        else
            out.println("lock temporarily unavailable");

        out.println("  Config " + configRef.get());

        out.println("  Accessed from " + applications);

        out.println("  Signalled to poll? " + pollingStartSignalReceived);
        out.println("  PollingManager state " + readyForPollingTask.bits);

        ScheduledFuture<?> pollFuture = pollingFutureRef.get();
        if (pollFuture != null)
            out.println("  Next poll in " + pollFuture.getDelay(TimeUnit.MILLISECONDS) + "ms");

        if (configUpdatePendingQueueLock.readLock().tryLock())
            try {
                out.print("  Config updates (" + configUpdatesInProgress + ") in progress, which block tasks:");
                for (InvokerTask task : configUpdatePendingQueue)
                    out.print(' ' + task.taskId);
                out.println();
            } finally {
                configUpdatePendingQueueLock.readLock().unlock();
            }

        out.println("  In-memory list of known pending (or active) tasks: " + inMemoryTaskIds.keySet());
        out.println();
    }

    /** {@inheritDoc} */
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> callables) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> callables, long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> callables) throws InterruptedException, ExecutionException {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> callables, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isFailOverEnabled() {
        return configRef.get().missedTaskThreshold > 0;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public boolean isShutdown() {
        // Section 3.1.6.1 of the Concurrency Utilities spec requires IllegalStateException
        throw new IllegalStateException(new UnsupportedOperationException("isShutdown"));
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public boolean isTerminated() {
        // Section 3.1.6.1 of the Concurrency Utilities spec requires IllegalStateException
        throw new IllegalStateException(new UnsupportedOperationException("isTerminated"));
    }

    /**
     * DS method to modify this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context DeclarativeService defined/populated component context
     * @throws MalformedObjectNameException
     */
    @Modified
    @Trivial
    protected void modified(ComponentContext context) throws MalformedObjectNameException {
        Dictionary<String, ?> properties = context.getProperties();
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "modified", properties);

        // Validate new config before we do anything destructive
        Config newConfig = new Config(properties);
        Config oldConfig = configRef.get();

        readyForPollingTask.remove(PollingManager.DS_READY);

        Future<?> previousFuture = pollingFutureRef.getAndSet(null);
        if (previousFuture != null)
            previousFuture.cancel(false);

        if (oldConfig.enableTaskExecution != newConfig.enableTaskExecution)
            if (newConfig.enableTaskExecution)
                readyForPollingTask.add(PollingManager.EXECUTION_ENABLED);
            else {
                readyForPollingTask.remove(PollingManager.EXECUTION_ENABLED);
                inMemoryTaskIds.clear();
            }

        if (oldConfig.initialPollDelay != newConfig.initialPollDelay)
            if (newConfig.initialPollDelay == -1)
                readyForPollingTask.add(PollingManager.SIGNAL_REQUIRED);
            else if (oldConfig.initialPollDelay == -1)
                readyForPollingTask.remove(PollingManager.SIGNAL_REQUIRED);

        configRef.set(newConfig);

        boolean mbeanChange = oldConfig.missedTaskThreshold == -1 && newConfig.missedTaskThreshold != -1;

        // If the JNDI name changes, notify the application recycle coordinator and re-register the mbean
        if (newConfig.jndiName == null ? oldConfig.jndiName != null : !newConfig.jndiName.equals(oldConfig.jndiName)) {
            if (!applications.isEmpty()) {
                ApplicationRecycleCoordinator appCoord = (ApplicationRecycleCoordinator) context.locateService(APP_RECYCLE_SERVICE);
                Set<String> members = new HashSet<String>(applications);
                applications.removeAll(members);
                appCoord.recycleApplications(members);
            }

            mbeanChange = true;
        }

        if (mbeanChange) {
            if (mbean != null) {
                mbean.unregister();
                mbean = null;
            }

            if (newConfig.missedTaskThreshold == -1) {
                // PersistentExecutorMBean is undocumented, experimental, and not supported.
                // We would like to remove its registration under all circumstances, but that could potentially break some one who is using it
                mbean = new PersistentExecutorMBeanImpl(this);
                mbean.register(InvokerTask.priv.getBundleContext(context));
            }
        }

        if (readyForPollingTask.addAndCheckIfReady(PollingManager.DS_READY))
            startPollingTask(newConfig);

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "modified");
    }

    /**
     * Create and persist a new task to the persistent store.
     *
     * @param task     the task.
     * @param taskInfo information to store with the task in binary form which is not directly queryable.
     * @param trigger  trigger for the task (if any).
     * @param result   optional predetermined result for the task (if any).
     * @return snapshot of task status reflecting the initial creation of the task.
     */
    private <T> TimerStatus<T> newTask(Object task, TaskInfo taskInfo, Trigger trigger, Object result) {
        if (task == null)
            throw new NullPointerException(taskInfo.isSubmittedAsCallable() ? Callable.class.getName() : Runnable.class.getName());

        TaskRecord record = new TaskRecord(true);
        record.unsetId();

        TimerTrigger timerTrigger = trigger instanceof TimerTrigger ? (TimerTrigger) trigger : null;
        String owner = timerTrigger == null ? getOwner() : timerTrigger.getAppName();
        if (owner == null)
            throw new RejectedExecutionException(Tr.formatMessage(tc, "CWWKC1540.thread.cannot.submit.tasks"));
        else
            record.setIdentifierOfOwner(owner);

        Config config = configRef.get();

        long taskAssignmentInfo = -1;
        if (config.missedTaskThreshold < 1) {
            try {
                taskAssignmentInfo = getPartitionId();
            } catch (RuntimeException x) {
                throw x;
            } catch (Exception x) {
                throw new RejectedExecutionException(x);
            }
        }

        Map<String, String> execProps = getExecutionProperties(task);

        // ManagedTask.IDENTITY_NAME
        String name = execProps.get(ManagedTask.IDENTITY_NAME);
        if (name == null)
            name = execProps.get(OTHER_SPEC_IDENTITY_NAME);
        record.setName(Utils.normalizeString(name));

        // ManagedTask.LONGRUNNING_HINT
        String key;
        String longRunningHint = execProps.get(key = ManagedTask.LONGRUNNING_HINT);
        if (longRunningHint == null)
            longRunningHint = execProps.get(key = OTHER_SPEC_LONGRUNNING_HINT);
        if (Boolean.parseBoolean(longRunningHint))
            throw new RejectedExecutionException(key + ": " + longRunningHint);

        int txTimeout;
        String txTimeoutString = execProps.get(PersistentExecutor.TRANSACTION_TIMEOUT);
        if (txTimeoutString == null)
            txTimeout = config.missedTaskThreshold > 0 ? (int) config.missedTaskThreshold : 0;
        else
            try {
                txTimeout = Integer.parseInt(txTimeoutString);
                if (txTimeout < 0)
                    throw new IllegalArgumentException(PersistentExecutor.TRANSACTION_TIMEOUT + ": " + txTimeoutString);
            } catch (NumberFormatException x) {
                throw new IllegalArgumentException(PersistentExecutor.TRANSACTION_TIMEOUT + ": " + txTimeoutString, x);
            }
        record.setTransactionTimeout(txTimeout);

        // ManagedTask.TRANSACTION
        String transaction = execProps.get(ManagedTask.TRANSACTION);
        if (transaction == null)
            transaction = execProps.get(OTHER_SPEC_TRANSACTION_CONSTANT);

        short flags = 0;
        String autoPurge = execProps.get(AutoPurge.PROPERTY_NAME);
        if (autoPurge == null || AutoPurge.ON_SUCCESS.toString().equals(autoPurge))
            flags |= TaskRecord.Flags.AUTO_PURGE_ON_SUCCESS.bit;
        else if (AutoPurge.ALWAYS.toString().equals(autoPurge))
            flags |= TaskRecord.Flags.AUTO_PURGE_ALWAYS.bit | TaskRecord.Flags.AUTO_PURGE_ON_SUCCESS.bit;
        if (trigger instanceof TaskLocker)
            flags |= TaskRecord.Flags.EJB_SINGLETON.bit;
        if (trigger instanceof TimerTrigger)
            flags |= TaskRecord.Flags.EJB_TIMER.bit;
        if (taskInfo.getInterval() == -1 && taskInfo.getInitialDelay() != -1)
            flags |= TaskRecord.Flags.ONE_SHOT_TASK.bit;
        if (config.missedTaskThreshold > 0 || "SUSPEND".equals(transaction)) // ManagedTask.SUSPEND
            flags |= TaskRecord.Flags.SUSPEND_TRAN_OF_EXECUTOR_THREAD.bit;

        record.setMiscBinaryFlags(flags);

        Date now = new Date();
        long originalSubmitTime = now.getTime();
        record.setOriginalSubmitTime(originalSubmitTime);

        record.setState((short) (TaskState.SCHEDULED.bit | TaskState.UNATTEMPTED.bit));

        taskInfo.initThreadContext(contextSvcRef.getServiceWithException(), execProps);

        // Avoid serializing task if the same instance is shared with trigger
        if (task != trigger)
            if (task instanceof Serializable)
                try {
                    record.setTask(serialize(task));
                } catch (IOException x) {
                    throw new IllegalArgumentException(Utils.toString(task), x);
                }
            else
                taskInfo.initForNonSerializableTask(task.getClass().getName());

        ClassLoader loader = timerTrigger == null ? InvokerTask.priv.getContextClassLoader() : timerTrigger.getClassLoader();
        record.setIdentifierOfClassLoader(classloaderIdSvc.getClassLoaderIdentifier(loader));

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            Tr.debug(this, tc, "submitter info", cData == null ? null : cData.getJ2EEName(), loader);
        }

        record.setTrigger(null);
        long initialDelay = taskInfo.getInitialDelay();
        if (initialDelay >= 0)
            record.setNextExecutionTime(originalSubmitTime + initialDelay);
        else {
            if (trigger == null)
                throw new NullPointerException(Trigger.class.getName());

            Date nextExecutionDate;
            try {
                nextExecutionDate = trigger.getNextRunTime(null, now);
            } catch (Throwable x) {
                throw new RejectedExecutionException(x);
            }
            if (nextExecutionDate == null)
                throw new RejectedExecutionException("Trigger.getNextRunTime: null");
            record.setNextExecutionTime(nextExecutionDate.getTime());

            // Trigger must be serialized after getNextRunTime to properly capture its state
            if (trigger instanceof Serializable)
                try {
                    record.setTrigger(serialize(trigger));
                } catch (IOException x) {
                    throw new IllegalArgumentException(Utils.toString(trigger), x);
                }
            else
                taskInfo.initForNonSerializableTrigger(trigger.getClass().getName());
        }

        try {
            record.setTaskInformation(serialize(taskInfo));
        } catch (IOException x) {
            throw new RejectedExecutionException(x);
        }

        if (result != null)
            try {
                record.setResult(serialize(result));
            } catch (IOException x) {
                throw new IllegalArgumentException(Utils.toString(result), x);
            }

        // Determine whether or not to start out with a claim on the task
        boolean claimFirstExecution = false;
        long nextExecTime = record.getNextExecutionTime();
        if (config.missedTaskThreshold > 0 && config.enableTaskExecution
            && (config.pollInterval < 0 || nextExecTime <= System.currentTimeMillis() + config.pollInterval)) {
            taskAssignmentInfo = nextExecTime + config.missedTaskThreshold * 1000;
            claimFirstExecution = true;
        }

        record.setClaimExpiryOrPartition(taskAssignmentInfo);

        TransactionController tranController = new TransactionController();
        try {
            tranController.preInvoke();

            taskStore.create(record);

            // Immediately schedule tasks that should run in the near future or run on other instances if the transaction commits
            boolean scheduleToSelf = config.missedTaskThreshold > 0 //
                            ? claimFirstExecution //
                            : config.enableTaskExecution && (config.pollInterval < 0 || nextExecTime <= System.currentTimeMillis() + config.pollInterval);
            if (scheduleToSelf) {
                Synchronization autoSchedule = new InvokerTask(this, record.getId(), nextExecTime, record.getMiscBinaryFlags(), txTimeout);
                UOWCurrent uowCurrent = (UOWCurrent) tranController.tranMgr;
                tranController.tranMgr.registerSynchronization(uowCurrent.getUOWCoord(), autoSchedule, EmbeddableWebSphereTransactionManager.SYNC_TIER_OUTER);
            }
        } catch (Throwable x) {
            tranController.setFailure(x);
        } finally {
            RejectedExecutionException x = tranController.postInvoke(RejectedExecutionException.class);
            if (x != null)
                throw x;
        }

        return new TaskStatusImpl<T>(record, this);
    }

    /**
     * Invoked by a controller to notify a persistent executor that a task has been assigned to it.
     *
     * @param taskId             unique identifier for the task.
     * @param nextExecTime       next execution time for the task.
     * @param binaryFlags        combination of bits for various binary values.
     * @param transactionTimeout transaction timeout.
     */
    public void notifyOfTaskAssignment(long taskId, long nextExecTime, short binaryFlags, int transactionTimeout) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        Boolean previous = inMemoryTaskIds.put(taskId, Boolean.TRUE);
        if (previous == null) {
            InvokerTask task = new InvokerTask(this, taskId, nextExecTime, binaryFlags, transactionTimeout);
            long delay = nextExecTime - new Date().getTime();
            if (trace && tc.isDebugEnabled())
                Tr.debug(PersistentExecutorImpl.this, tc, "Found task " + taskId + " for " + delay + "ms from now");
            scheduledExecutor.schedule(task, delay, TimeUnit.MILLISECONDS);
        } else {
            if (trace && tc.isDebugEnabled())
                Tr.debug(PersistentExecutorImpl.this, tc, "Found task " + taskId + " already scheduled");
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean remove(long taskId) {
        String owner = getOwner();
        if (owner == null)
            return false;

        TransactionController tranController = new TransactionController();
        boolean removed = false;
        try {
            tranController.preInvoke();
            removed = taskStore.remove(taskId, owner, true);
        } catch (Throwable x) {
            tranController.setFailure(x);
        } finally {
            PersistentStoreException x = tranController.postInvoke(PersistentStoreException.class); // TODO proposed spec class
            if (x != null)
                throw x;
        }

        if (removed) {
            long[] runningTaskState = InvokerTask.runningTaskState.get();
            if (runningTaskState != null && runningTaskState[0] == taskId)
                runningTaskState[1] = InvokerTask.REMOVED_BY_SELF;
        }
        return removed;
    }

    /** {@inheritDoc} */
    @Override
    public int remove(String pattern, Character escape, TaskState state, boolean inState) {
        String owner = getOwner();
        if (owner == null)
            return 0;

        pattern = pattern == null ? null : Utils.normalizeString(pattern);
        int updateCount = 0;
        TransactionController tranController = new TransactionController();
        try {
            tranController.preInvoke();
            updateCount = taskStore.remove(pattern, escape, state, inState, owner);
        } catch (Throwable x) {
            tranController.setFailure(x);
        } finally {
            PersistentStoreException x = tranController.postInvoke(PersistentStoreException.class); // TODO proposed spec class
            if (x != null)
                throw x;
        }

        return updateCount;
    }

    /**
     * Removes partition information from the persistent store. All of the parameters are optional.
     * If a parameter is specified, only entries that match it are removed from the persistent store.
     *
     * This method is for the mbean only.
     *
     * @param hostName           the host name.
     * @param userDir            wlp.user.dir
     * @param libertyServerName  name of the Liberty server.
     * @param executorIdentifier config.displayId of the persistent executor.
     * @return the number of entries removed from the persistent store.
     * @throws Exception if an error occurs.
     */
    int removePartitionInfo(String hostName, String userDir, String libertyServerName, String executorIdentifier) throws Exception {
        Config config = configRef.get();
        if (config.missedTaskThreshold != -1)
            throw new UnsupportedOperationException(); // should be unreachable

        PartitionRecord criteria = new PartitionRecord(false);
        if (hostName != null)
            criteria.setHostName(hostName);
        if (userDir != null)
            criteria.setUserDir(userDir);
        if (libertyServerName != null)
            criteria.setLibertyServer(libertyServerName);
        if (executorIdentifier != null)
            criteria.setExecutor(executorIdentifier);

        int numRemoved = 0;
        TransactionController tranController = new TransactionController();
        try {
            tranController.preInvoke();
            numRemoved = taskStore.remove(criteria);
        } catch (Throwable x) {
            tranController.setFailure(x);
        } finally {
            Exception x = tranController.postInvoke(Exception.class);
            if (x != null)
                throw x;
        }

        return numRemoved;
    }

    /** {@inheritDoc} */
    @Override
    public int removeProperties(String pattern, Character escape) throws Exception {
        if (pattern == null)
            throw new NullPointerException("pattern");

        int count = 0;
        TransactionController tranController = new TransactionController();
        try {
            tranController.preInvoke();
            count = taskStore.removeProperties(Utils.normalizeString(pattern), escape);
        } catch (Throwable x) {
            tranController.setFailure(x);
        } finally {
            Exception x = tranController.postInvoke(Exception.class);
            if (x != null)
                throw x;
        }

        return count;
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeProperty(String name) {
        boolean removed = false;
        if (name != null && name.length() > 0) {
            TransactionController tranController = new TransactionController();
            try {
                tranController.preInvoke();
                removed = taskStore.removeProperty(name);
            } catch (Throwable x) {
                tranController.setFailure(x);
            } finally {
                PersistentStoreException x = tranController.postInvoke(PersistentStoreException.class); // TODO proposed spec class
                if (x != null)
                    throw x;
            }
        }

        return removed;
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeTimer(long taskId) throws Exception {
        TransactionController tranController = new TransactionController();
        boolean removed = false;
        try {
            tranController.preInvoke();
            removed = taskStore.remove(taskId, null, true);
        } catch (Throwable x) {
            tranController.setFailure(x);
        } finally {
            Exception x = tranController.postInvoke(Exception.class);
            if (x != null)
                throw x;
        }

        if (removed) {
            long[] runningTaskState = InvokerTask.runningTaskState.get();
            if (runningTaskState != null && runningTaskState[0] == taskId)
                runningTaskState[1] = InvokerTask.REMOVED_BY_SELF;
        }
        return removed;
    }

    /** {@inheritDoc} */
    @Override
    public int removeTimers(String appName, String pattern, Character escape, TaskState state, boolean inState) throws Exception {
        pattern = pattern == null ? null : Utils.normalizeString(pattern);
        int updateCount = 0;
        TransactionController tranController = new TransactionController();
        try {
            tranController.preInvoke();
            updateCount = taskStore.remove(pattern, escape, state, inState, appName);
        } catch (Throwable x) {
            tranController.setFailure(x);
        } finally {
            Exception x = tranController.postInvoke(Exception.class);
            if (x != null)
                throw x;
        }

        return updateCount;
    }

    @Override
    public <V> TaskStatus<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        int compare = unit.compareTo(TimeUnit.MILLISECONDS);
        delay = delay <= 0 ? 0 : compare == 0 ? delay // no conversion needed
                        : compare < 0 ? unit.toMillis(delay - 1) + 1 // round up to nearest millisecond
                                        : unit.toMillis(delay);

        TaskInfo taskInfo = new TaskInfo(true);
        taskInfo.initForOneShotTask(delay);

        return newTask(callable, taskInfo, null, null);
    }

    @Override
    public <V> TaskStatus<V> schedule(Callable<V> callable, Trigger trigger) {
        TaskInfo taskInfo = new TaskInfo(true);

        return newTask(callable, taskInfo, trigger, null);
    }

    @Override
    public <T> TimerStatus<T> schedule(TimerTrigger task) throws Exception {
        TaskInfo taskInfo = new TaskInfo(task instanceof Callable);

        return newTask(task, taskInfo, task, null);
    }

    @Override
    public TaskStatus<?> schedule(Runnable runnable, long delay, TimeUnit unit) {
        int compare = unit.compareTo(TimeUnit.MILLISECONDS);
        delay = delay <= 0 ? 0 : compare == 0 ? delay // no conversion needed
                        : compare < 0 ? unit.toMillis(delay - 1) + 1 // round up to nearest millisecond
                                        : unit.toMillis(delay);

        TaskInfo taskInfo = new TaskInfo(false);
        taskInfo.initForOneShotTask(delay);

        return newTask(runnable, taskInfo, null, null);
    }

    @Override
    public TaskStatus<?> schedule(Runnable runnable, Trigger trigger) {
        TaskInfo taskInfo = new TaskInfo(false);

        return newTask(runnable, taskInfo, trigger, null);
    }

    @Override
    public TaskStatus<?> scheduleAtFixedRate(Runnable runnable, long initialDelay, long period, TimeUnit unit) {
        int compare = unit.compareTo(TimeUnit.MILLISECONDS);

        initialDelay = initialDelay <= 0 ? 0 : compare == 0 ? initialDelay // no conversion needed
                        : compare < 0 ? unit.toMillis(initialDelay - 1) + 1 // round up to nearest millisecond
                                        : unit.toMillis(initialDelay);

        if (period > 0)
            period = compare == 0 ? period : compare < 0 ? unit.toMillis(period - 1) + 1 // round up to nearest millisecond
                            : unit.toMillis(period);
        else
            throw new IllegalArgumentException(Long.toString(period));

        TaskInfo taskInfo = new TaskInfo(false);
        taskInfo.initForRepeatingTask(true, initialDelay, period);

        return newTask(runnable, taskInfo, null, null);
    }

    @Override
    public TaskStatus<?> scheduleWithFixedDelay(Runnable runnable, long initialDelay, long delay, TimeUnit unit) {
        int compare = unit.compareTo(TimeUnit.MILLISECONDS);

        initialDelay = initialDelay <= 0 ? 0 : compare == 0 ? initialDelay // no conversion needed
                        : compare < 0 ? unit.toMillis(initialDelay - 1) + 1 // round up to nearest millisecond
                                        : unit.toMillis(initialDelay);

        if (delay > 0)
            delay = compare == 0 ? delay : compare < 0 ? unit.toMillis(delay - 1) + 1 // round up to nearest millisecond
                            : unit.toMillis(delay);
        else
            throw new IllegalArgumentException(Long.toString(delay));

        TaskInfo taskInfo = new TaskInfo(false);
        taskInfo.initForRepeatingTask(false, initialDelay, delay);

        return newTask(runnable, taskInfo, null, null);
    }

    /**
     * Utility method that serializes an object to bytes.
     *
     * @param object object to serialize.
     * @return bytes representing the object. Null if the object is null.
     * @throws IOException if an error occurs serializing the object.
     */
    public final byte[] serialize(Object object) throws IOException {
        if (object == null)
            return null;

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        SerializationService serializationSvc = serializationSvcRef.getServiceWithException();
        ObjectOutputStream oout = serializationSvc.createObjectOutputStream(new DeflaterOutputStream(bout));
        oout.writeObject(object);
        oout.flush();
        oout.close();
        byte[] bytes = bout.toByteArray();
        return bytes;
    }

    /**
     * Invoked when the server is stopped without the {@code --force}.
     * This gives us an opportunity to stop polling for tasks to run
     * and mark ourselves as deactivated so that we don't start any new tasks.
     *
     * @see com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener#serverStopping()
     */
    @Override
    public void serverStopping() {
        deactivated = true;
        Future<?> pollingFuture = pollingFutureRef.get();
        if (pollingFuture != null)
            pollingFuture.cancel(false);
    }

    /**
     * Declarative Services method for setting the ApplicationTracker reference
     *
     * @param ref reference to the service
     */
    @Reference(service = ApplicationTracker.class)
    protected void setApplicationTracker(ServiceReference<ApplicationTracker> ref) {
        appTrackerRef.setReference(ref);
    }

    /**
     * Declarative Services method for setting the ApplicationRecycleCoordinator service
     *
     * @param ref reference to the service
     */
    @Reference(service = ApplicationRecycleCoordinator.class)
    protected void setAppRecycleService(ServiceReference<ApplicationRecycleCoordinator> ref) {
    }

    /**
     * Declarative Services method for setting the class loader identifier service.
     *
     * @param svc the service
     */
    @Reference
    protected void setClassLoaderIdentifierService(ClassLoaderIdentifierService svc) {
        classloaderIdSvc = svc;
    }

    /**
     * Declarative Services method for setting the context service reference
     *
     * @param ref reference to the service
     */
    @Reference(service = WSContextService.class,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               target = "(id=unbound)")
    protected void setContextService(ServiceReference<WSContextService> ref) {
        contextSvcRef.setReference(ref);
    }

    /**
     * Declarative Services method for setting the Liberty executor.
     *
     * @param svc the service
     */
    @Reference(target = "(component.name=com.ibm.ws.threading)")
    protected void setExecutor(ExecutorService svc) {
        executor = svc;
    }

    /**
     * Declarative Services method for setting the LocalTransactionCurrent.
     *
     * @param ref reference to the service
     */
    @Reference(service = LocalTransactionCurrent.class)
    protected void setLocalTransactionCurrent(ServiceReference<LocalTransactionCurrent> ref) {
        localTranCurrentRef.setReference(ref);
    }

    /**
     * Declarative Services method for setting the WSLocationAdmin service.
     *
     * @param svc the service
     */
    @Reference
    protected void setLocationAdmin(WsLocationAdmin svc) {
        locationAdmin = svc;
    }

    /** {@inheritDoc} */
    @Override
    public boolean setProperty(String name, String value) {
        if (value == null || value.length() == 0)
            throw new IllegalArgumentException("value: " + value);
        boolean exists = false;
        if (name != null && name.length() > 0) {
            TransactionController tranController = new TransactionController();
            try {
                tranController.preInvoke();
                exists = taskStore.setProperty(name, value);
            } catch (Throwable x) {
                tranController.setFailure(x);
            } finally {
                PersistentStoreException x = tranController.postInvoke(PersistentStoreException.class);
                if (x != null)
                    throw x;
            }
        }
        return exists;
    }

    /**
     * Declarative Services method for setting the serialization service
     *
     * @param ref reference to the service
     */
    @Reference(service = SerializationService.class)
    protected void setSerializationService(ServiceReference<SerializationService> ref) {
        serializationSvcRef.setReference(ref);
    }

    /**
     * Declarative Services method for setting the database store
     *
     * @param svc   the service
     * @param props service properties
     */
    @Reference(target = "(id=unbound)")
    protected void setTaskStore(DatabaseStore svc, Map<String, Object> props) {
        persistentStore = svc;
        persistentStoreDisplayId = (String) props.get("config.displayId");
    }

    /**
     * Declarative Services method for setting the transaction manager
     *
     * @param ref reference to the service
     */
    @Reference(service = EmbeddableWebSphereTransactionManager.class)
    protected void setTransactionManager(ServiceReference<EmbeddableWebSphereTransactionManager> ref) {
        tranMgrRef.setReference(ref);
    }

    /**
     * Declarative Services method for setting the variable registry
     *
     * @param svc the service
     */
    @Reference
    protected void setVariableRegistry(VariableRegistry svc) {
        variableRegistry = svc;
    }

    /**
     * Declarative services method that is invoked once the server is started.
     * Only after this method is invoked is the initial polling for
     * persistent tasks performed.
     *
     * @param ref reference to the ServerStarted service
     */
    @Reference(service = ServerStarted.class,
               policy = ReferencePolicy.DYNAMIC,
               cardinality = ReferenceCardinality.OPTIONAL,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setServerStarted(ServiceReference<ServerStarted> ref) {

        // We don't want to start the polling task if we haven't been activated yet.

        // Not starting polling if not executing tasks.  Need to coordinate with activate.
        if (readyForPollingTask.addAndCheckIfReady(PollingManager.SERVER_STARTED))
            startPollingTask(configRef.get());
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public void shutdown() {
        // Section 3.1.6.1 of the Concurrency Utilities spec requires IllegalStateException
        throw new IllegalStateException(new UnsupportedOperationException("shutdown"));
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public List<Runnable> shutdownNow() {
        // Section 3.1.6.1 of the Concurrency Utilities spec requires IllegalStateException
        throw new IllegalStateException(new UnsupportedOperationException("shutdownNow"));
    }

    /** {@inheritDoc} */
    @Override
    public void startPolling() {
        Config config = configRef.get();
        if (config.initialPollDelay != -1)
            throw new IllegalStateException("initialPollDelay: " + config.initialPollDelay);
        if (!deactivated
            && !pollingStartSignalReceived.getAndSet(true)
            && readyForPollingTask.addAndCheckIfReady(PollingManager.SIGNAL_RECEIVED))
            startPollingTask(config);
    }

    /** {@inheritDoc} */
    @Override
    public <T> TaskStatus<T> submit(Callable<T> callable) {
        TaskInfo taskInfo = new TaskInfo(true);
        taskInfo.initForOneShotTask(0l); // run immediately

        return newTask(callable, taskInfo, null, null);
    }

    /** {@inheritDoc} */
    @Override
    public <T> TaskStatus<T> submit(Runnable runnable, T result) {
        TaskInfo taskInfo = new TaskInfo(false);
        taskInfo.initForOneShotTask(0l); // run immediately

        return newTask(runnable, taskInfo, null, result);
    }

    /** {@inheritDoc} */
    @Override
    public TaskStatus<?> submit(Runnable runnable) {
        TaskInfo taskInfo = new TaskInfo(false);
        taskInfo.initForOneShotTask(0l); // run immediately

        return newTask(runnable, taskInfo, null, null);
    }

    /**
     * Transfers tasks that have not yet ended to this persistent executor instance.
     *
     * This method is for the mbean only.
     *
     * @param maxTaskId      task id including and up to which to transfer non-ended tasks from the old partition to this partition.
     *                           If null, all non-ended tasks are transferred from the old partition to this partition.
     * @param oldPartitionId partition id to which tasks are currently assigned.
     * @return count of transferred tasks.
     */
    int transfer(Long maxTaskId, long oldPartitionId) throws Exception {
        Config config = configRef.get();
        if (config.missedTaskThreshold != -1)
            throw new UnsupportedOperationException(); // should be unreachable

        long partitionId = getPartitionId();

        TransactionController tranController = new TransactionController();
        int count = 0;
        try {
            tranController.preInvoke();

            count = taskStore.transfer(maxTaskId, oldPartitionId, partitionId);

            if (config.enableTaskExecution && count > 0 && config.pollInterval < 0) {
                // Schedule a poll to find the transferred tasks
                Synchronization autoPoll = new PollingTask(config);
                UOWCurrent uowCurrent = (UOWCurrent) tranController.tranMgr;
                tranController.tranMgr.registerSynchronization(uowCurrent.getUOWCoord(), autoPoll, EmbeddableWebSphereTransactionManager.SYNC_TIER_OUTER);
            }
        } catch (Throwable x) {
            tranController.setFailure(x);
        } finally {
            Exception x = tranController.postInvoke(Exception.class);
            if (x != null)
                throw x;
        }

        return count;
    }

    /**
     * Declarative Services method for unsetting the ApplicationTracker reference
     *
     * @param ref reference to the service
     */
    protected void unsetApplicationTracker(ServiceReference<ApplicationTracker> ref) {
        appTrackerRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for unsetting the ApplicationRecycleCoordinator service
     *
     * @param ref reference to the service
     */
    protected void unsetAppRecycleService(ServiceReference<ApplicationRecycleCoordinator> ref) {
    }

    /**
     * Declarative Services method for unsetting the class loader identifier service.
     *
     * @param svc the service
     */
    protected void unsetClassLoaderIdentifierService(ClassLoaderIdentifierService svc) {
        classloaderIdSvc = null;
    }

    /**
     * Declarative Services method for unsetting the context service reference
     *
     * @param ref reference to the service
     */
    protected void unsetContextService(ServiceReference<WSContextService> ref) {
        contextSvcRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for unsetting the Liberty executor.
     *
     * @param svc the service
     */
    protected void unsetExecutor(ExecutorService svc) {
        executor = null;
    }

    /**
     * Declarative Services method for unsetting the LocalTransactionCurrent.
     *
     * @param ref reference to the service
     */
    protected void unsetLocalTransactionCurrent(ServiceReference<LocalTransactionCurrent> ref) {
        localTranCurrentRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for unsetting the WSLocationAdmin service.
     *
     * @param svc the service
     */
    protected void unsetLocationAdmin(WsLocationAdmin svc) {
        locationAdmin = null;
    }

    /**
     * Declarative Services method for unsetting the serialization service
     *
     * @param ref reference to the service
     */
    protected void unsetSerializationService(ServiceReference<SerializationService> ref) {
        serializationSvcRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for unsetting the persistent store
     *
     * @param svc the service
     */
    protected void unsetTaskStore(DatabaseStore svc) {
    }

    /**
     * Declarative Services method for unsetting the transaction manager
     *
     * @param ref reference to the service
     */
    protected void unsetTransactionManager(ServiceReference<EmbeddableWebSphereTransactionManager> ref) {
        tranMgrRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for unsetting the variable registry
     *
     * @param svc the service
     */
    protected void unsetVariableRegistry(VariableRegistry svc) {
        variableRegistry = null;
    }

    /**
     * Declarative Services method for unsetting the ServerStarted service
     *
     * @param ref reference to the service
     */
    protected synchronized void unsetServerStarted(ServiceReference<ServerStarted> ref) {
        // server is shutting down
        deactivated = true;
    }

    /**
     * Start the polling task.
     *
     * @param config snapshot of configuration.
     */
    private void startPollingTask(Config config) {
        ScheduledFuture<?> future;
        PollingTask pollingTask = new PollingTask(config);

        future = scheduledExecutor.schedule(pollingTask, config.initialPollDelay, TimeUnit.MILLISECONDS);

        pollingFutureRef.getAndSet(future);
    }

    /**
     * This method is driven for various RuntimeUpdateNotification's. When we receive a
     * notification we will bump our count of active configuration updates in progress. A non-zero count value
     * signifies configuration update(s) are in progress.
     *
     * We monitor the completion of the Futures related to the notifications. When we
     * are driven for their completion we decrement our configuration update in progress count.
     *
     * @see com.ibm.ws.runtime.update.RuntimeUpdateListener#notificationCreated(com.ibm.ws.runtime.update.RuntimeUpdateManager, com.ibm.ws.runtime.update.RuntimeUpdateNotification)
     */
    @Override
    public void notificationCreated(RuntimeUpdateManager updateManager, RuntimeUpdateNotification notification) {

        class MyCompletionListener implements CompletionListener<Boolean> {
            String notificationName;

            public MyCompletionListener(String name) {
                notificationName = name;
            }

            @Override
            public void successfulCompletion(Future<Boolean> future, Boolean result) {
                // The configuration update for which we were monitoring is now complete.
                // Update our awareness to the update and perform any deferred actions.
                configUpdateCompleted(notificationName);
            }

            @Override
            public void failedCompletion(Future<Boolean> future, Throwable t) {
                // The configuration update failed, but we still want to resume functions.
                // Update our awareness to the update and perform any deferred actions.
                configUpdateCompleted(notificationName);
            }
        }

        if (deactivated)
            return;

        int prevCnt = configUpdateInProgress();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(PersistentExecutorImpl.this, tc, "Notified of \"" + notification.getName() + "\" previous configUpdatesInProgress: " + prevCnt);

        FutureMonitor futureMonitor = _futureMonitor;
        if (futureMonitor != null) {
            MyCompletionListener newListener = new MyCompletionListener(notification.getName());

            futureMonitor.onCompletion(notification.getFuture(), newListener);
        }
    }

    /**
     * Lock object to serial updates to the configUpdatesInProgress and the processing of the deferred list of
     * Runnables encountered under a configuration update.
     */
    private final ReadWriteLock configUpdatePendingQueueLock = new ReentrantReadWriteLock();

    /**
     * Indicates if a configuration update is currently in progress.
     */
    private int configUpdatesInProgress = 0;

    /**
     * List of Tasks to execute after configuration updates have completed.
     */
    private final LinkedList<InvokerTask> configUpdatePendingQueue = new LinkedList<InvokerTask>();

    /**
     * futureMonitor used to track the outcome of a configuration update
     */
    private volatile FutureMonitor _futureMonitor;

    @Reference(service = FutureMonitor.class)
    protected void setFutureMonitor(FutureMonitor futureMonitor) {
        _futureMonitor = futureMonitor;
    }

    protected void unsetFutureMonitor(FutureMonitor futureMonitor) {
        _futureMonitor = null;
    }

    /**
     * If a configuration update is currently in progress add the targetRunnable to a
     * local queue to drive its run method after the configuration update(s) is complete.
     *
     * @param targetRunnable task eligible for execution.
     * @return return true if a configuration update is in progress, false if not.
     */
    @Trivial
    boolean deferExecutionForConfigUpdate(InvokerTask targetRunnable) {
        boolean returnValue = false;

        configUpdatePendingQueueLock.readLock().lock();
        try {
            if (configUpdatesInProgress == 0) {
                return returnValue;
            }
        } finally {
            configUpdatePendingQueueLock.readLock().unlock();
        }

        configUpdatePendingQueueLock.writeLock().lock();
        try {
            if (configUpdatesInProgress > 0) {
                configUpdatePendingQueue.add(targetRunnable);
                returnValue = true;
            }
        } finally {
            configUpdatePendingQueueLock.writeLock().unlock();
        }

        if (returnValue && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "execution deferred while config update in progress");
        return returnValue;
    }

    /**
     * Bump the number of Configuration updates that we are monitoring.
     *
     * @return previous number of Configuration updates that we are monitoring.
     */
    private int configUpdateInProgress() {
        int retVal;

        configUpdatePendingQueueLock.writeLock().lock();
        try {
            retVal = configUpdatesInProgress;
            configUpdatesInProgress++;
        } finally {
            configUpdatePendingQueueLock.writeLock().unlock();
        }
        return retVal;
    }

    /**
     * Update our awareness to the update and perform any deferred actions.
     * Note: if we're going to do anything significant we should get off of the notification
     * thread.
     *
     * @param notificationName name of associated Notification
     *
     * @return previous number of Configuration updates that we are monitoring.
     */
    int configUpdateCompleted(String notificationName) {
        int retVal;

        configUpdatePendingQueueLock.writeLock().lock();
        try {
            retVal = configUpdatesInProgress;
            configUpdatesInProgress--;

            if (!deactivated && configUpdatesInProgress == 0) {
                executor.submit(processConfigUpdateQueue);
            }
        } finally {
            configUpdatePendingQueueLock.writeLock().unlock();
        }
        return retVal;
    }

    /**
     * Push the deferred Tasks, from a Configuration update, to the executor pool to run.
     */
    private final Runnable processConfigUpdateQueue = new Runnable() {
        @Override
        @Trivial
        public void run() {
            InvokerTask r;
            for (;;) {

                configUpdatePendingQueueLock.writeLock().lock();
                try {
                    r = configUpdatePendingQueue.poll();
                    if (r == null)
                        return;
                } finally {
                    configUpdatePendingQueueLock.writeLock().unlock();
                }

                if (!deactivated)
                    executor.submit(r);
            }
        }
    };

    /**
     * Polls the persistent task store for tasks that ought to run in the near future and then schedules them.
     *
     * For scenarios where tasks a transferred to a persistent executor where repeated polling is disabled (pollInterval < 0),
     * an instance of this class can be registered as a Synchronization with a transaction in order to
     * automatically schedule a poll after the transaction commits.
     */
    @Trivial
    private class PollingTask implements Runnable, Synchronization {
        /**
         * Config instance from when this PollingTask was initially scheduled. We should stop running if we find that it changes.
         */
        private final Config initialConfig;

        private boolean isFirstTimeCoordinating = true;

        private PollingTask(Config config) {
            initialConfig = config;
        }

        /**
         * Upon successful transaction commit, automatically schedules a poll for tasks.
         *
         * @see javax.transaction.Synchronization#afterCompletion(int)
         */
        @Override
        public void afterCompletion(int status) {
            final boolean trace = TraceComponent.isAnyTracingEnabled();
            if (trace && tc.isEntryEnabled())
                Tr.entry(PersistentExecutorImpl.this, tc, "afterCompletion", status);

            if (status == Status.STATUS_COMMITTED && readyForPollingTask.addAndCheckIfReady(0))
                executor.submit(this);

            if (trace && tc.isEntryEnabled())
                Tr.exit(PersistentExecutorImpl.this, tc, "afterCompletion");
        }

        @Override
        public void beforeCompletion() {
        }

        /**
         * Using the persistent store, coordinates with other instances to partition out the responsibility for polling at the desired interval.
         *
         * @param config persistent executor configuration, including the desired poll interval.
         * @return the computed time (millis) at which this task should poll again.
         */
        private long coordinateNextPoll(Config config) {
            final boolean trace = TraceComponent.isAnyTracingEnabled();

            long expiry;
            try {
                EmbeddableWebSphereTransactionManager tranMgr = tranMgrRef.getServiceWithException();

                long pollPartitionId = getPollPartitionId();
                boolean successful = false;

                tranMgr.begin();
                try {
                    Object[] expiryAndLastUpdated = taskStore.findPollInfoForUpdate(pollPartitionId);
                    expiry = (Long) expiryAndLastUpdated[0];
                    long lastUpdated = (Long) expiryAndLastUpdated[1];
                    long now = System.currentTimeMillis();
                    int slot = 0;
                    long delay = -1;
                    // number of poll intervals to miss before resetting
                    final int missedPollsThreshold = 2; // Could be make configurable in the future.

                    /*
                     * If we have missed more than two poll intervals in a row we want to reset the polling data, as we're assuming
                     * something bad happened to the cluster.
                     * If a PollingTask is running for the first time and the next poll is to be scheduled more than 5 intervals into the future,
                     * reset the polling data. This prevents a massive outage that recovered very quickly (under 2 poll intervals) from creating
                     * a potentially large gap in polling.
                     */
                    // We add 1 to missedPollsThreshold to allow up to the next poll interval before considering it a miss.
                    if (now - lastUpdated > (missedPollsThreshold + 1) * config.pollInterval || (isFirstTimeCoordinating && expiry - now > 5 * config.pollInterval)) {
                        expiry = now / 1000 * 1000 + 600;
                        delay = config.pollInterval - (now - expiry);
                        slot = 1;
                        if (trace && tc.isDebugEnabled()) {
                            if (now - lastUpdated > missedPollsThreshold * config.pollInterval) {
                                Tr.debug(PersistentExecutorImpl.this, tc, "Detected " + missedPollsThreshold + " or more poll intervals were missed.  Resetting poll info.");
                            } else {
                                Tr.debug(PersistentExecutorImpl.this, tc, "Detected starting new PollingTask. Resetting poll info.");
                            }
                        }
                    } else {
                        while (delay < 0) {
                            delay = ((++slot) * config.pollInterval) - (now - expiry);
                        }

                    }
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(PersistentExecutorImpl.this, tc, "now = " + now + "; slot = " + slot);

                    expiry = expiry + slot * config.pollInterval;
                    taskStore.updatePollInfo(pollPartitionId, expiry);
                    successful = true;
                } finally {
                    if (successful)
                        tranMgr.commit();
                    else
                        tranMgr.rollback();
                }
            } catch (Throwable x) {
                expiry = System.currentTimeMillis() + config.pollInterval;
            }

            isFirstTimeCoordinating = false;
            return expiry;
        }

        /**
         * Polls for tasks that are owned by the current partition.
         *
         * @param config configuration of this instance.
         * @throws Exception if an error occurs.
         */
        private void partitionBasedPoll(Config config) throws Exception {
            final boolean trace = TraceComponent.isAnyTracingEnabled();

            EmbeddableWebSphereTransactionManager tranMgr = tranMgrRef.getServiceWithException();

            long partitionId = getPartitionId();
            long now = System.currentTimeMillis();
            long maxNextExecTime = config.pollInterval >= 0 ? (config.pollInterval + now) : Long.MAX_VALUE;
            List<Object[]> results;
            tranMgr.begin();
            try {
                results = taskStore.findUpcomingTasks(partitionId, maxNextExecTime, config.pollSize);
            } finally {
                tranMgr.commit();
            }
            for (Object[] result : results) {
                long taskId = (Long) result[0];
                Boolean previous = inMemoryTaskIds.put(taskId, Boolean.TRUE);
                if (previous == null) {
                    short mbits = (Short) result[1];
                    long nextExecTime = (Long) result[2];
                    int txTimeout = (Integer) result[3];
                    InvokerTask task = new InvokerTask(PersistentExecutorImpl.this, taskId, nextExecTime, mbits, txTimeout);
                    long delay = nextExecTime - new Date().getTime();
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(PersistentExecutorImpl.this, tc, "Found task " + taskId + " for " + delay + "ms from now");
                    scheduledExecutor.schedule(task, delay, TimeUnit.MILLISECONDS);
                } else {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(PersistentExecutorImpl.this, tc, "Found task " + taskId + " already scheduled");
                }
            }
        }

        /**
         * Polls for all tasks that should start within the next poll interval which have not already been
         * claimed by other executor instances.
         *
         * @param config configuration of this instance.
         * @throws Exception if an error occurs.
         */
        private void pollForUnclaimedTasks(Config config) throws Exception {
            final boolean trace = TraceComponent.isAnyTracingEnabled();

            EmbeddableWebSphereTransactionManager tranMgr = tranMgrRef.getServiceWithException();

            long now = System.currentTimeMillis();
            long maxNextExecTime = config.pollInterval >= 0 ? (config.pollInterval + now) : Long.MAX_VALUE;
            List<Object[]> results;
            tranMgr.begin();
            try {
                results = taskStore.findUnclaimedTasks(maxNextExecTime, config.pollSize);
            } finally {
                tranMgr.commit();
            }

            for (Object[] result : results) {
                long taskId = (Long) result[0];
                boolean claimed = false;
                Boolean previous = inMemoryTaskIds.put(taskId, Boolean.TRUE);
                if (previous == null)
                    try {
                        long nextExecTime = (Long) result[2];
                        int version = (Integer) result[4];
                        now = System.currentTimeMillis();
                        long claimUntilTime = (now > nextExecTime ? now : nextExecTime) + config.missedTaskThreshold * 1000;

                        boolean claimedPendingCommit = false;
                        tranMgr.begin();
                        try {
                            claimedPendingCommit = taskStore.claimIfNotLocked(taskId, version, claimUntilTime);
                        } finally {
                            tranMgr.commit();
                        }

                        if (claimed = claimedPendingCommit) {
                            short mbits = (Short) result[1];
                            int txTimeout = (Integer) result[3];
                            InvokerTask task = new InvokerTask(PersistentExecutorImpl.this, taskId, nextExecTime, mbits, txTimeout);
                            long delay = nextExecTime - new Date().getTime();
                            if (trace && tc.isDebugEnabled())
                                Tr.debug(PersistentExecutorImpl.this, tc, "Found task " + taskId + " for " + delay + "ms from now");
                            scheduledExecutor.schedule(task, delay, TimeUnit.MILLISECONDS);
                        }
                    } finally {
                        if (!claimed)
                            inMemoryTaskIds.remove(taskId);
                    }
                else {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(PersistentExecutorImpl.this, tc, "Found task " + taskId + " already scheduled");
                }
            }
        }

        @Override
        public void run() {
            final boolean trace = TraceComponent.isAnyTracingEnabled();
            if (trace && tc.isEntryEnabled())
                Tr.entry(PersistentExecutorImpl.this, tc, "run[poll]");

            Config config = configRef.get();

            if (deactivated || !config.enableTaskExecution || config != initialConfig) {
                if (trace && tc.isEntryEnabled())
                    Tr.exit(PersistentExecutorImpl.this, tc, "run[poll]", deactivated ? "deactivated" : config);
                return;
            }

            Throwable failure = null;
            try {
                long beginPoll = System.nanoTime();
                try {
                    if (config.missedTaskThreshold > 0)
                        pollForUnclaimedTasks(config);
                    else
                        partitionBasedPoll(config);
                } finally {
                    // Schedule next poll
                    config = configRef.get();
                    if (config.enableTaskExecution && config.pollInterval >= 0 && config == initialConfig) {
                        ScheduledFuture<?> future;
                        long duration;
                        if (config.pollingCoordination && config.missedTaskThreshold > 0) {
                            long expiry = coordinateNextPoll(config);
                            duration = System.nanoTime() - beginPoll;
                            future = scheduledExecutor.schedule(this, expiry - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                        } else {
                            duration = System.nanoTime() - beginPoll;
                            long delay = config.pollInterval - TimeUnit.NANOSECONDS.toMillis(duration);
                            future = scheduledExecutor.schedule(this, delay, TimeUnit.MILLISECONDS);
                        }

                        if (trace && tc.isDebugEnabled())
                            Tr.debug(PersistentExecutorImpl.this, tc, "Poll completed in " + duration + "ns. Next poll " + future.getDelay(TimeUnit.MILLISECONDS) + "ms from now");

                        pollingFutureRef.getAndSet(future);
                    }
                }
            } catch (Throwable x) {
                if (failure == null)
                    failure = x;
            }

            if (trace && tc.isEntryEnabled())
                Tr.exit(PersistentExecutorImpl.this, tc, "run[poll]", failure);
        }
    }

    /**
     * Abstraction that helps ensure we always run in a global transaction -
     * either the caller's or a new one that we establish after suspending the LTC.
     */
    class TransactionController {
        private boolean expectRollback;
        private Throwable failure;
        private LocalTransactionCurrent ltcCurrent;
        private LocalTransactionCoordinator suspendedLTC;
        private final EmbeddableWebSphereTransactionManager tranMgr;
        private boolean tranStarted;

        @Trivial
        TransactionController() {
            tranMgr = tranMgrRef.getServiceWithException();
        }

        /**
         * Indicates that we expect the transaction status to be STATUS_MARKED_ROLLBACK,
         * in which case we will roll back the transaction without raising any error.
         */
        private void expectRollback() {
            expectRollback = true;
        }

        /**
         * Enlists in the current global transaction (if any),
         * or suspends the current LTC (if any) and starts a new global transaction.
         */
        void preInvoke() throws NotSupportedException, SystemException {
            int tranStatus = tranMgr.getStatus();
            ltcCurrent = tranStatus == Status.STATUS_NO_TRANSACTION ? localTranCurrentRef.getServiceWithException() : null;
            suspendedLTC = ltcCurrent == null ? null : ltcCurrent.suspend();

            if (tranStatus == Status.STATUS_NO_TRANSACTION) {
                tranMgr.begin();
                tranStarted = true;
            }
        }

        /**
         * Commits or rolls back the global transaction started by preInvoke (if any).
         * Resumes the LTC that was suspended by preInvoke (if any).
         *
         * @param exceptionClass type of exception to raise if a declared exception occurs.
         * @return failure wrapped in the specified type. Null if no failure occurs or has occurred previously.
         *         If the failure is an Error or RuntimeException, it is thrown instead.
         */
        <T extends Throwable> T postInvoke(@Sensitive Class<T> exceptionClass) {
            try {
                if (tranStarted)
                    if (failure != null || expectRollback && tranMgr.getStatus() == Status.STATUS_MARKED_ROLLBACK)
                        try {
                            tranMgr.rollback();
                        } catch (Throwable x) {
                        }
                    else
                        tranMgr.commit();
            } catch (Throwable x) {
                setFailure(x);
            } finally {
                if (suspendedLTC != null)
                    ltcCurrent.resume(suspendedLTC);
            }

            if (failure == null)
                return null;
            if (failure instanceof Error)
                throw (Error) failure;
            if (failure instanceof RuntimeException)
                throw (RuntimeException) failure;
            try {
                if (exceptionClass.isInstance(failure))
                    return exceptionClass.cast(failure);

                T result = exceptionClass.newInstance();
                result.initCause(failure);
                return result;
            } catch (Throwable t) {
                throw new RuntimeException(failure);
            }
        }

        /**
         * Sets the recorded failure if a previous failure has not already been recorded.
         *
         * @param failure the failure.
         */
        void setFailure(Throwable failure) {
            if (this.failure == null)
                this.failure = failure;
        }
    }
}