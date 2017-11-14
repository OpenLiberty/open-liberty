/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleContext;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;
import com.ibm.wsspi.threadcontext.WSContextService;

@Component(configurationPid = "com.ibm.ws.concurrent.managedExecutorService", configurationPolicy = ConfigurationPolicy.REQUIRE,
           reference = @Reference(name = ManagedExecutorServiceImpl.APP_RECYCLE_SERVICE, service = ApplicationRecycleCoordinator.class),
           property = { "creates.objectClass=java.util.concurrent.ExecutorService",
                        "creates.objectClass=javax.enterprise.concurrent.ManagedExecutorService" })
public class ManagedExecutorServiceImpl implements ExecutorService, ManagedExecutorService, ResourceFactory, ApplicationRecycleComponent {
    private static final TraceComponent tc = Tr.register(ManagedExecutorServiceImpl.class);

    /**
     * Name of reference to the ApplicationRecycleCoordinator
     */
    static final String APP_RECYCLE_SERVICE = "ApplicationRecycleCoordinator";

    /**
     * Name of the unique identifier property
     */
    private static final String CONFIG_ID = "config.displayId";

    /**
     * Controls how often we purge the list of tracked futures.
     */
    static final int FUTURE_PURGE_INTERVAL = 20;

    /**
     * Names of applications using this ResourceFactory
     */
    private final Set<String> applications = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /**
     * Privileged action to lazily obtain the context service.
     */
    final PrivilegedAction<WSContextService> contextSvcAccessor = new PrivilegedAction<WSContextService>() {
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
     * Reference to the context service for this managed executor service.
     */
    private final AtomicServiceReference<WSContextService> contextSvcRef = new AtomicServiceReference<WSContextService>("ContextService");

    /**
     * Default execution properties to use for tasks where none are specified.
     */
    private final AtomicReference<Map<String, String>> defaultExecutionProperties = new AtomicReference<Map<String, String>>();

    /**
     * Count of futures. In combination with FUTURE_PURGE_INTERVAL, this determines when to purge completed futures from the list.
     */
    volatile int futureCount;

    /**
     * Futures for tasks that might be scheduled or running. These are tracked so that we can meet the requirement
     * of canceling or interrupting tasks that are scheduled or running when the managed executor service goes away.
     */
    final ConcurrentLinkedQueue<Future<?>> futures = new ConcurrentLinkedQueue<Future<?>>();

    /**
     * Reference to the JNDI name (if any) of this managed executor service.
     */
    private final AtomicReference<String> jndiNameRef = new AtomicReference<String>();

    /**
     * Reference to the executor that runs tasks according to the long running concurrency policy for this managed executor.
     */
    final AtomicReference<PolicyExecutor> longRunningPolicyExecutorRef = new AtomicReference<PolicyExecutor>();

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
    final PrivilegedAction<ThreadContextProvider> tranContextProviderAccessor = new PrivilegedAction<ThreadContextProvider>() {
        @Override
        @Trivial
        public ThreadContextProvider run() {
            return tranContextProviderRef.getService();
        }
    };

    /**
     * Reference to the transaction context provider.
     */
    private final AtomicServiceReference<ThreadContextProvider> tranContextProviderRef = new AtomicServiceReference<ThreadContextProvider>("TransactionContextProvider");

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
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        contextSvcRef.deactivate(context);
        tranContextProviderRef.deactivate(context);

        // Cancel submitted or running tasks // TODO have the policy executor(s) do this
        for (Future<?> future = futures.poll(); future != null; future = futures.poll())
            if (!future.isDone() && future.cancel(true))
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "canceled", future);
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
        WSContextService contextSvc = AccessController.doPrivileged(contextSvcAccessor);

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
    public Set<String> getDependentApplications() {
        Set<String> members = new HashSet<String>(applications);
        applications.removeAll(members);
        return members;
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
     * Purge completed futures from the list we are tracking.
     * This method should be invoked every so often so that we don't leak memory.
     */
    @Trivial
    final void purgeFutures() {
        for (Iterator<Future<?>> it = futures.iterator(); it.hasNext();)
            if (it.next().isDone())
                it.remove();
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

        WSContextService contextSvc = AccessController.doPrivileged(contextSvcAccessor);

        @SuppressWarnings("unchecked")
        TaskLifeCycleCallback callback = new TaskLifeCycleCallback(this, contextSvc.captureThreadContext(execProps));
        Future<T> future = callback.policyExecutor.submit(task, callback);

        if (futures.add(future) && ++futureCount % FUTURE_PURGE_INTERVAL == 0)
            purgeFutures();
        return future;
    }

    /** {@inheritDoc} */
    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        Map<String, String> execProps = getExecutionProperties(task);

        WSContextService contextSvc = AccessController.doPrivileged(contextSvcAccessor);

        @SuppressWarnings("unchecked")
        TaskLifeCycleCallback callback = new TaskLifeCycleCallback(this, contextSvc.captureThreadContext(execProps));
        Future<T> future = callback.policyExecutor.submit(task, result, callback);

        if (futures.add(future) && ++futureCount % FUTURE_PURGE_INTERVAL == 0)
            purgeFutures();
        return future;
    }

    /** {@inheritDoc} */
    @Override
    public Future<?> submit(Runnable task) {
        return submit(task, null);
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
