/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedTask;

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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrency.policy.ConcurrencyPolicy;
import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleContext;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * All declarative services annotations on this class are ignored.
 * The annotations on
 * com.ibm.ws.concurrent.ee.ManagedExecutorServiceImpl and
 * com.ibm.ws.concurrent.mp.ManagedExecutorImpl
 * apply instead.
 */
@Component(configurationPid = "com.ibm.ws.concurrent.managedExecutorService", configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = { ExecutorService.class, ManagedExecutorService.class, ResourceFactory.class, ApplicationRecycleComponent.class },
           reference = @Reference(name = ManagedExecutorServiceImpl.APP_RECYCLE_SERVICE, service = ApplicationRecycleCoordinator.class),
           property = { "creates.objectClass=java.util.concurrent.ExecutorService",
                        "creates.objectClass=javax.enterprise.concurrent.ManagedExecutorService" })
public class ManagedExecutorServiceImpl implements ExecutorService, ManagedExecutorService, ResourceFactory, ApplicationRecycleComponent, WSManagedExecutorService {
    private static final TraceComponent tc = Tr.register(ManagedExecutorServiceImpl.class);

    /**
     * Name of reference to the ApplicationRecycleCoordinator
     */
    static final String APP_RECYCLE_SERVICE = "ApplicationRecycleCoordinator";

    /**
     * Names of applications using this ResourceFactory
     */
    private final Set<String> applications = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /**
     * Privileged action to lazily obtain the context service. Available only on the OSGi code path.
     */
    private final PrivilegedAction<WSContextService> contextSvcAccessor = new PrivilegedAction<WSContextService>() {
        @Override
        @Trivial
        public WSContextService run() {
            try {
                return contextSvcRef.getServiceWithException();
            } catch (IllegalStateException x) {
                throw new RejectedExecutionException(x);
            }
        }
    };

    /**
     * Reference to the context service for this managed executor service. Available only on the OSGi code path.
     */
    private final AtomicServiceReference<WSContextService> contextSvcRef = new AtomicServiceReference<WSContextService>("ContextService");

    /**
     * Default execution properties to use for tasks where none are specified.
     */
    private final AtomicReference<Map<String, String>> defaultExecutionProperties = new AtomicReference<Map<String, String>>();

    /**
     * Reference to the JNDI name (if any) of this managed executor service.
     */
    private final AtomicReference<String> jndiNameRef = new AtomicReference<String>();

    /**
     * Reference to the executor that runs tasks according to the long running concurrency policy for this managed executor.
     * Null if longRunningPolicy is not configured, in which case the executor for the normal concurrency policy should be used instead.
     */
    final AtomicReference<PolicyExecutor> longRunningPolicyExecutorRef = new AtomicReference<PolicyExecutor>();

    /**
     * Available only on the MicroProfile code path (CDI injection or ManagedExecutorBuilder).
     */
    private final WSContextService mpContextService;

    /**
     * Reference to the name of this managed executor service.
     * The name is the jndiName if specified, otherwise the config id.
     */
    final AtomicReference<String> name = new AtomicReference<String>();

    /**
     * Executor that runs tasks against the general concurrency policy for this managed executor.
     */
    volatile PolicyExecutor policyExecutor;

    /**
     * Privileged action to lazily obtain the transaction context provider.
     */
    private final PrivilegedAction<ThreadContextProvider> tranContextProviderAccessor = new PrivilegedAction<ThreadContextProvider>() {
        @Override
        @Trivial
        public ThreadContextProvider run() {
            return tranContextProviderRef.getService();
        }
    };

    /**
     * Reference to the transaction context provider.
     */
    private AtomicServiceReference<ThreadContextProvider> tranContextProviderRef = new AtomicServiceReference<ThreadContextProvider>("TransactionContextProvider");

    /**
     * Constructor for OSGi code path.
     */
    @Trivial
    public ManagedExecutorServiceImpl() {
        mpContextService = null;
    }

    /**
     * Constructor for MicroProfile Concurrency (ManagedExecutorBuilder and CDI injected ManagedExecutor).
     */
    @Trivial // traced in super super class
    public ManagedExecutorServiceImpl(String name, PolicyExecutor policyExecutor, WSContextService mpThreadContext,
                                      AtomicServiceReference<ThreadContextProvider> tranContextProviderRef) {
        this.name.set(name);
        this.policyExecutor = policyExecutor;
        this.longRunningPolicyExecutorRef.set(policyExecutor);
        this.mpContextService = mpThreadContext;
        this.tranContextProviderRef = tranContextProviderRef;
    }

    /**
     *
     * @param context DeclarativeService defined/populated component context
     */
    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        contextSvcRef.activate(context);
        tranContextProviderRef.activate(context);

        String jndiName = (String) properties.get("jndiName");
        jndiNameRef.set(jndiName);
        String xsvcName = jndiName == null ? (String) properties.get("config.displayId") : jndiName;
        name.set(xsvcName);

        Map<String, String> execProps = new TreeMap<String, String>();
        execProps.put(WSContextService.DEFAULT_CONTEXT, WSContextService.UNCONFIGURED_CONTEXT_TYPES);
        execProps.put(WSContextService.TASK_OWNER, xsvcName);
        defaultExecutionProperties.set(execProps);
    }

    /*
     *
     * @param context DeclarativeService defined/populated component context
     */
    @Modified
    protected void modified(final ComponentContext context, Map<String, Object> properties) {
        String jndiName = (String) properties.get("jndiName");
        String oldJNDIName = jndiNameRef.getAndSet(jndiName);
        String xsvcName = jndiName == null ? (String) properties.get("config.displayId") : jndiName;
        name.set(xsvcName);

        Map<String, String> execProps = new TreeMap<String, String>();
        execProps.put(WSContextService.DEFAULT_CONTEXT, WSContextService.UNCONFIGURED_CONTEXT_TYPES);
        execProps.put(WSContextService.TASK_OWNER, xsvcName);
        defaultExecutionProperties.set(execProps);

        // If the JNDI name changes, notify the application recycle coordinator
        if (jndiName == null ? oldJNDIName != null : !jndiName.equals(oldJNDIName))
            if (!applications.isEmpty()) {
                ApplicationRecycleCoordinator appCoord = AccessController.doPrivileged(new PrivilegedAction<ApplicationRecycleCoordinator>() {
                    @Override
                    public ApplicationRecycleCoordinator run() {
                        return (ApplicationRecycleCoordinator) context.locateService(APP_RECYCLE_SERVICE);
                    }
                });
                Set<String> members = new HashSet<String>(applications);
                applications.removeAll(members);
                appCoord.recycleApplications(members);
            }
    }

    /**
     *
     * @param context DeclarativeService defined/populated component context
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        // Cancel submitted or running tasks
        int count = policyExecutor.cancel(getIdentifier(policyExecutor.getIdentifier()), true);

        PolicyExecutor longRunningExecutor = longRunningPolicyExecutorRef.get();
        if (longRunningExecutor != null)
            count += longRunningExecutor.cancel(getIdentifier(longRunningExecutor.getIdentifier()), true);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, count + " submitted tasks canceled");

        contextSvcRef.deactivate(context);
        tranContextProviderRef.deactivate(context);
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        // Section 3.1.6.1 of the Concurrency Utilities spec requires IllegalStateException
        throw new IllegalStateException(new UnsupportedOperationException("awaitTermination"));
    }

    /**
     * Capture context for a list of tasks and create callbacks that apply context and notify the ManagedTaskListener, if any.
     *
     * @param tasks collection of tasks.
     * @return list of callbacks.
     */
    @SuppressWarnings("unchecked")
    private <T> TaskLifeCycleCallback[] createCallbacks(Collection<? extends Callable<T>> tasks) {
        WSContextService contextSvc = getContextService();

        int numTasks = tasks.size();
        TaskLifeCycleCallback[] callbacks = new TaskLifeCycleCallback[numTasks];

        if (numTasks == 1)
            callbacks[0] = new TaskLifeCycleCallback(this, contextSvc.captureThreadContext(getExecutionProperties(tasks.iterator().next())));
        else {
            // Thread context capture is expensive, so reuse callbacks when execution properties match
            Map<Map<String, String>, TaskLifeCycleCallback> execPropsToCallback = new HashMap<Map<String, String>, TaskLifeCycleCallback>();
            int t = 0;
            for (Callable<T> task : tasks) {
                Map<String, String> execProps = getExecutionProperties(task);
                TaskLifeCycleCallback callback = execPropsToCallback.get(execProps);
                if (callback == null)
                    execPropsToCallback.put(execProps, callback = new TaskLifeCycleCallback(this, contextSvc.captureThreadContext(execProps)));
                callbacks[t++] = callback;
            }
        }

        return callbacks;
    }

    /** {@inheritDoc} */
    @Override
    public Object createResource(final ResourceInfo ref) throws Exception {
        ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cData != null)
            applications.add(cData.getJ2EEName().getApplication());

        return this;
    }

    @Override
    public ApplicationRecycleContext getContext() {
        return null;
    }

    @Override
    public WSContextService getContextService() {
        return mpContextService == null ? AccessController.doPrivileged(contextSvcAccessor) : mpContextService;
    }

    @Override
    public Set<String> getDependentApplications() {
        Set<String> members = new HashSet<String>(applications);
        applications.removeAll(members);
        return members;
    }

    @Override
    public PolicyExecutor getNormalPolicyExecutor() {
        return policyExecutor;
    }

    /** {@inheritDoc} */
    @Override
    public void execute(Runnable command) {
        // Delegating to submit because we need to track a Future for the task, in case we need to cancel it
        submit(command, null);
    }

    /**
     * Returns execution properties for the task.
     *
     * @param task the task being submitted for execution.
     * @return execution properties for the task.
     */
    final Map<String, String> getExecutionProperties(Object task) {
        if (task == null) // NullPointerException is required per the JavaDoc API
            throw new NullPointerException(Tr.formatMessage(tc, "CWWKC1111.task.invalid", (Object) null));

        Map<String, String> execProps = task instanceof ManagedTask ? ((ManagedTask) task).getExecutionProperties() : null;
        if (execProps == null)
            execProps = defaultExecutionProperties.get();
        else {
            execProps = new TreeMap<String, String>(execProps);
            String tranProp = execProps.remove(ManagedTask.TRANSACTION);
            if (tranProp != null && !ManagedTask.SUSPEND.equals(tranProp)) // USE_TRANSACTION_OF_EXECUTION_THREAD not valid for managed tasks
                throw new RejectedExecutionException(Tr.formatMessage(tc, "CWWKC1130.xprop.value.invalid", name, ManagedTask.TRANSACTION, tranProp));
            if (!execProps.containsKey(WSContextService.DEFAULT_CONTEXT))
                execProps.put(WSContextService.DEFAULT_CONTEXT, WSContextService.UNCONFIGURED_CONTEXT_TYPES);
            if (!execProps.containsKey(WSContextService.TASK_OWNER))
                execProps.put(WSContextService.TASK_OWNER, name.get());
        }
        return execProps;
    }

    /**
     * Utility method to compute the identifier to be used in combination with the specified policy executor identifier.
     * We prepend the managed executor name if it isn't already included in the policy executor's identifier.
     *
     * @param policyExecutorIdentifier unique identifier for the policy executor. Some examples:
     *            concurrencyPolicy[longRunningPolicy]
     *            managedExecutorService[executor1]/longRunningPolicy[default-0]
     * @return identifier to use in messages and for matching of tasks upon shutdown.
     */
    @Trivial
    final String getIdentifier(String policyExecutorIdentifier) {
        return policyExecutorIdentifier.startsWith("managed") //
                        ? policyExecutorIdentifier //
                        : new StringBuilder(name.get()).append(" (").append(policyExecutorIdentifier).append(')').toString();
    }

    /** {@inheritDoc} */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        TaskLifeCycleCallback[] callbacks = createCallbacks(tasks);
        // Policy executor can optimize the last task in the list to run on the current thread if we submit under the same executor,
        PolicyExecutor executor = callbacks.length > 0 ? callbacks[callbacks.length - 1].policyExecutor : policyExecutor;
        return (List) executor.invokeAll(tasks, callbacks);
    }

    /** {@inheritDoc} */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        TaskLifeCycleCallback[] callbacks = createCallbacks(tasks);
        PolicyExecutor executor = callbacks.length > 0 ? callbacks[0].policyExecutor : policyExecutor;
        return (List) executor.invokeAll(tasks, callbacks, timeout, unit);
    }

    /** {@inheritDoc} */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        TaskLifeCycleCallback[] callbacks = createCallbacks(tasks);
        PolicyExecutor executor = callbacks.length > 0 ? callbacks[0].policyExecutor : policyExecutor;
        return executor.invokeAny(tasks, callbacks);
    }

    /** {@inheritDoc} */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        TaskLifeCycleCallback[] callbacks = createCallbacks(tasks);
        PolicyExecutor executor = callbacks.length > 0 ? callbacks[0].policyExecutor : policyExecutor;
        return executor.invokeAny(tasks, callbacks, timeout, unit);
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
     * Declarative Services method for setting the concurrency policy.
     *
     * @param svc the service
     */
    @Reference(policy = ReferencePolicy.DYNAMIC, target = "(id=unbound)")
    protected void setConcurrencyPolicy(ConcurrencyPolicy svc) {
        policyExecutor = svc.getExecutor();
    }

    /**
     * Declarative Services method for setting the context service reference
     *
     * @param ref reference to the service
     */
    @Reference(policy = ReferencePolicy.DYNAMIC, target = "(id=unbound)")
    protected void setContextService(ServiceReference<WSContextService> ref) {
        contextSvcRef.setReference(ref);
    }

    /**
     * Declarative Services method for setting the long running concurrency policy.
     *
     * @param svc the service
     */
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, target = "(id=unbound)")
    protected void setLongRunningPolicy(ConcurrencyPolicy svc) {
        longRunningPolicyExecutorRef.set(svc.getExecutor());
    }

    /**
     * Declarative Services method for setting the transaction context provider service reference
     *
     * @param ref reference to the service
     */
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, target = "(component.name=com.ibm.ws.transaction.context.provider)")
    protected void setTransactionContextProvider(ServiceReference<ThreadContextProvider> ref) {
        tranContextProviderRef.setReference(ref);
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
    public <T> Future<T> submit(Callable<T> task) {
        Map<String, String> execProps = getExecutionProperties(task);

        WSContextService contextSvc = getContextService();

        @SuppressWarnings("unchecked")
        TaskLifeCycleCallback callback = new TaskLifeCycleCallback(this, contextSvc.captureThreadContext(execProps));
        return callback.policyExecutor.submit(task, callback);
    }

    /** {@inheritDoc} */
    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        Map<String, String> execProps = getExecutionProperties(task);

        WSContextService contextSvc = getContextService();

        @SuppressWarnings("unchecked")
        TaskLifeCycleCallback callback = new TaskLifeCycleCallback(this, contextSvc.captureThreadContext(execProps));
        return callback.policyExecutor.submit(task, result, callback);
    }

    /** {@inheritDoc} */
    @Override
    public Future<?> submit(Runnable task) {
        return submit(task, null);
    }

    /**
     * Uses the transaction context provider to suspends the currently active transaction or LTC on the thread.
     *
     * @return ThreadContext instance that must be used to restore the suspended transaction context if returned.
     *         Null if transaction context is unavailable.
     */
    ThreadContext suspendTransaction() {
        ThreadContextProvider tranContextProvider = AccessController.doPrivileged(tranContextProviderAccessor);
        ThreadContext suspendedTranSnapshot = tranContextProvider == null ? null : tranContextProvider.captureThreadContext(AbstractTask.XPROPS_SUSPEND_TRAN, null);
        if (suspendedTranSnapshot != null)
            suspendedTranSnapshot.taskStarting();
        return suspendedTranSnapshot;
    }

    /**
     * Declarative Services method for unsetting the concurrency policy
     *
     * @param svc the service
     */
    protected void unsetConcurrencyPolicy(ConcurrencyPolicy svc) {}

    /**
     * Declarative Services method for unsetting the context service reference
     *
     * @param ref reference to the service
     */
    protected void unsetContextService(ServiceReference<WSContextService> ref) {
        contextSvcRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for unsetting the long running concurrency policy
     *
     * @param svc the service
     */
    protected void unsetLongRunningPolicy(ConcurrencyPolicy svc) {
        longRunningPolicyExecutorRef.compareAndSet(svc.getExecutor(), null);
    }

    /**
     * Declarative Services method for unsetting the transaction context provider service reference
     *
     * @param ref reference to the service
     */
    protected void unsetTransactionContextProvider(ServiceReference<ThreadContextProvider> ref) {
        tranContextProviderRef.unsetReference(ref);
    }
}
