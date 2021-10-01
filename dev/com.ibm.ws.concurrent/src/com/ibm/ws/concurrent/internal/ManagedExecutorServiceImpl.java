/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
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
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedTask;

import org.eclipse.microprofile.context.ManagedExecutor;
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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrency.policy.ConcurrencyPolicy;
import com.ibm.ws.concurrent.ContextualAction;
import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.ws.concurrent.ext.ConcurrencyExtensionProvider;
import com.ibm.ws.javaee.version.JavaEEVersion;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.threading.CompletionStageExecutor;
import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleContext;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;
import com.ibm.wsspi.threadcontext.WSContextService;

@Component(configurationPid = "com.ibm.ws.concurrent.managedExecutorService", configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = { ExecutorService.class, ManagedExecutor.class, ManagedExecutorService.class, //
                       ResourceFactory.class, ApplicationRecycleComponent.class },
           reference = @Reference(name = "ApplicationRecycleCoordinator", service = ApplicationRecycleCoordinator.class))
public class ManagedExecutorServiceImpl implements ExecutorService, //
                ManagedExecutor, ManagedExecutorService, CompletionStageExecutor, //
                ResourceFactory, ApplicationRecycleComponent, WSManagedExecutorService {

    static {
        // Initialize ForkJoinPool when this class is initialized to avoid it failing later with access
        // control issues since ForkJoinPool doesn't have doPriv calls for getting some properties
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            @Trivial
            public Void run() {
                ForkJoinPool.commonPool();
                return null;
            }
        });
    }

    private static final TraceComponent tc = Tr.register(ManagedExecutorServiceImpl.class);

    /**
     * Name of reference to the ApplicationRecycleCoordinator
     */
    static final String APP_RECYCLE_SERVICE = "ApplicationRecycleCoordinator";

    /**
     * Jakarta EE Concurrency execution properties that specify to suspend the current transaction.
     */
    private static final Map<String, String> JAKARTA_SUSPEND_TRAN = Collections.singletonMap("jakarta.enterprise.concurrent.TRANSACTION", "SUSPEND");

    /**
     * Java EE Concurrency execution properties that specify to suspend the current transaction.
     */
    private static final Map<String, String> JAVAX_SUSPEND_TRAN = Collections.singletonMap("javax.enterprise.concurrent.TRANSACTION", "SUSPEND");

    private final boolean allowLifeCycleMethods;

    /**
     * Names of applications using this ResourceFactory
     */
    private final Set<String> applications = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /**
     * Collects common dependencies, including the ConcurrencyExtensionProvider, if any is available.
     */
    private ConcurrencyService concurrencySvc;

    /**
     * Reference to the context service for this managed executor service. Available only on the OSGi code path.
     */
    private final AtomicServiceReference<WSContextService> contextSvcRef = new AtomicServiceReference<WSContextService>("ContextService");

    /**
     * Default execution properties to use for tasks where none are specified.
     */
    private final AtomicReference<Map<String, String>> defaultExecutionProperties = new AtomicReference<Map<String, String>>();

    /**
     * Jakarta EE version if Jakarta EE 9 or higher. If 0, assume a lesser EE spec version.
     */
    volatile int eeVersion;

    /**
     * Tracks the most recently bound EE version service reference. Only use this within the set/unsetEEVersion methods.
     */
    private ServiceReference<JavaEEVersion> eeVersionRef;

    /**
     * Hash code for this instance.
     */
    private final int hash;

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
     * Reference to the transaction context provider.
     */
    private AtomicServiceReference<ThreadContextProvider> tranContextProviderRef = new AtomicServiceReference<ThreadContextProvider>("TransactionContextProvider");

    /**
     * Constructor for OSGi code path.
     */
    public ManagedExecutorServiceImpl() {
        mpContextService = null;
        allowLifeCycleMethods = false;
        hash = super.hashCode();
    }

    /**
     * Constructor for ManagedExecutorBuilder (from MicroProfile Context Propagation).
     */
    public ManagedExecutorServiceImpl(String name, int hash, int eeVersion,
                                      PolicyExecutor policyExecutor, ContextServiceImpl mpThreadContext,
                                      AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> tranContextProviderRef) {
        this.name.set(name);
        this.hash = hash;
        this.eeVersion = eeVersion;
        this.policyExecutor = policyExecutor;
        this.longRunningPolicyExecutorRef.set(policyExecutor);
        this.mpContextService = mpThreadContext;
        this.tranContextProviderRef = tranContextProviderRef;
        allowLifeCycleMethods = true;
        mpThreadContext.managedExecutor = this;
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
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (allowLifeCycleMethods)
            return getNormalPolicyExecutor().awaitTermination(timeout, unit);
        else // Section 3.1.6.1 of the Concurrency Utilities spec requires IllegalStateException
            throw new IllegalStateException(new UnsupportedOperationException("awaitTermination"));
    }

    @Override
    public ThreadContextDescriptor captureThreadContext(Map<String, String> props) {
        WSContextService contextSvc;
        if (mpContextService == null)
            contextSvc = contextSvcRef.getServiceWithException();
        else
            contextSvc = mpContextService;

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor threadContext = contextSvc.captureThreadContext(props);
        return threadContext;
    }

    @Override
    public <U> CompletableFuture<U> completedFuture(U value) {
        return ManagedCompletableFuture.completedFuture(value, this);
    }

    @Override
    public <U> CompletionStage<U> completedStage(U value) {
        return ManagedCompletableFuture.completedStage(value, this);
    }

    /**
     * This method was added to MicroProfile Context Propagation after v1.0.
     *
     * @return copy of the completion stage, where dependent stages of the copy uses this managed executor by default.
     */
    @Override
    @Trivial
    public final <T> CompletableFuture<T> copy(CompletableFuture<T> stage) {
        return (CompletableFuture<T>) copy((CompletionStage<T>) stage);
    }

    /**
     * This method was added to MicroProfile Context Propagation after v1.0.
     *
     * @return copy of the completion stage, where dependent stages of the copy uses this managed executor by default.
     */
    @Override
    public <T> CompletionStage<T> copy(CompletionStage<T> stage) {
        if (!MPContextPropagationVersion.atLeast(MPContextPropagationVersion.V1_1))
            throw new UnsupportedOperationException();

        final CompletableFuture<T> copy = ManagedCompletableFuture.JAVA8 //
                        ? new ManagedCompletableFuture<T>(new CompletableFuture<T>(), this, null) //
                        : new ManagedCompletableFuture<T>(this, null);

        stage.whenComplete((result, failure) -> {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(stage, tc, "whenComplete", result, failure);
            if (failure == null)
                copy.complete(result);
            else
                copy.completeExceptionally(failure);
        });

        return copy;
    }

    /**
     * Capture context for a list of tasks and create callbacks that apply context and notify the ManagedTaskListener, if any.
     * Context is not re-captured for any tasks that implement the ContextualAction marker interface.
     *
     * @param tasks collection of tasks.
     * @return entry consisting of a possibly modified copy of the task list (the key) and the list of callbacks (the value).
     */
    @SuppressWarnings("unchecked")
    private <T> Entry<Collection<? extends Callable<T>>, TaskLifeCycleCallback[]> createCallbacks(Collection<? extends Callable<T>> tasks) {
        int numTasks = tasks.size();
        TaskLifeCycleCallback[] callbacks = new TaskLifeCycleCallback[numTasks];
        List<Callable<T>> taskUpdates = null;

        if (numTasks == 1) {
            Callable<T> task = tasks.iterator().next();

            ThreadContextDescriptor contextDescriptor;
            if (task instanceof ContextualAction) {
                ContextualAction<Callable<T>> a = (ContextualAction<Callable<T>>) task;
                contextDescriptor = a.getContextDescriptor();
                task = a.getAction();
                taskUpdates = Arrays.asList(task);
            } else {
                contextDescriptor = captureThreadContext(getExecutionProperties(task));
            }

            callbacks[0] = new TaskLifeCycleCallback(this, contextDescriptor);
        } else {
            // Thread context capture is expensive, so reuse callbacks when execution properties match
            Map<Map<String, String>, TaskLifeCycleCallback> execPropsToCallback = new HashMap<Map<String, String>, TaskLifeCycleCallback>();
            WSContextService contextSvc = null;
            int t = 0;
            for (Callable<T> task : tasks) {
                if (task instanceof ContextualAction) {
                    ContextualAction<Callable<T>> a = (ContextualAction<Callable<T>>) task;
                    taskUpdates = taskUpdates == null ? new ArrayList<Callable<T>>(tasks) : taskUpdates;
                    taskUpdates.set(t, a.getAction());
                    callbacks[t++] = new TaskLifeCycleCallback(this, a.getContextDescriptor());
                } else {
                    Map<String, String> execProps = getExecutionProperties(task);
                    TaskLifeCycleCallback callback = execPropsToCallback.get(execProps);
                    if (callback == null) {
                        execPropsToCallback.put(execProps, callback = new TaskLifeCycleCallback(this, captureThreadContext(execProps)));
                    }
                    callbacks[t++] = callback;
                }
            }
        }

        return new SimpleEntry<Collection<? extends Callable<T>>, TaskLifeCycleCallback[]>(taskUpdates == null ? tasks : taskUpdates, callbacks);
    }

    /** {@inheritDoc} */
    @Override
    public Object createResource(final ResourceInfo ref) throws Exception {
        ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cData != null)
            applications.add(cData.getJ2EEName().getApplication());

        ConcurrencyExtensionProvider provider = concurrencySvc.extensionProvider;
        if (provider == null)
            return this;
        else
            return provider.provide(this, ref);
    }

    @Override
    public <U> CompletableFuture<U> failedFuture(Throwable ex) {
        return ManagedCompletableFuture.failedFuture(ex, this);
    }

    @Override
    public <U> CompletionStage<U> failedStage(Throwable ex) {
        return ManagedCompletableFuture.failedStage(ex, this);
    }

    @Override
    public ApplicationRecycleContext getContext() {
        return null;
    }

    @Deprecated // being replaced with captureThreadContext so that this method signature can change to spec
    @Trivial
    public WSContextService getContextService() {
        WSContextService contextSvc;
        if (mpContextService == null)
            try {
                contextSvc = contextSvcRef.getServiceWithException(); // doPriv is covered by AtomicServiceReference
            } catch (IllegalStateException x) {
                throw new RejectedExecutionException(x);
            }
        else
            contextSvc = mpContextService;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "getContextService: " + contextSvc);
        return contextSvc;
    }

    @Override
    public Set<String> getDependentApplications() {
        Set<String> members = new HashSet<String>(applications);
        applications.removeAll(members);
        return members;
    }

    @Override
    @Trivial
    public PolicyExecutor getLongRunningPolicyExecutor() {
        PolicyExecutor executor = longRunningPolicyExecutorRef.get();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "getLongRunningPolicyExecutor: " + executor);
        return executor;
    }

    @Override
    @Trivial
    public PolicyExecutor getNormalPolicyExecutor() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "getNormalPolicyExecutor: " + policyExecutor);

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
            String tranPropKey;
            String tranProp = execProps.remove(tranPropKey = "jakarta.enterprise.concurrent.TRANSACTION");
            if (tranProp == null)
                tranProp = execProps.remove(tranPropKey = "javax.enterprise.concurrent.TRANSACTION");
            if (tranProp != null && !"SUSPEND".equals(tranProp)) // USE_TRANSACTION_OF_EXECUTION_THREAD not valid for managed tasks
                throw new RejectedExecutionException(Tr.formatMessage(tc, "CWWKC1130.xprop.value.invalid", name, tranPropKey, tranProp));
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
     *                                     concurrencyPolicy[longRunningPolicy]
     *                                     managedExecutorService[executor1]/longRunningPolicy[default-0]
     * @return identifier to use in messages and for matching of tasks upon shutdown.
     */
    @Trivial
    final String getIdentifier(String policyExecutorIdentifier) {
        return policyExecutorIdentifier.startsWith("managed") //
                        ? policyExecutorIdentifier //
                        : new StringBuilder(name.get()).append(" (").append(policyExecutorIdentifier).append(')').toString();
    }

    /**
     * This method was added to MicroProfile Context Propagation after v1.0.
     *
     * @return the backing instance of MicroProfile ThreadContext.
     */
    @Override
    public org.eclipse.microprofile.context.ThreadContext getThreadContext() {
        if (mpContextService == null || !MPContextPropagationVersion.atLeast(MPContextPropagationVersion.V1_1))
            throw new UnsupportedOperationException();
        else
            return (org.eclipse.microprofile.context.ThreadContext) mpContextService;
    }

    @Override
    @Trivial
    public int hashCode() {
        return hash;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        Entry<Collection<? extends Callable<T>>, TaskLifeCycleCallback[]> entry = createCallbacks(tasks);
        tasks = entry.getKey();
        TaskLifeCycleCallback[] callbacks = entry.getValue();

        // Policy executor can optimize the last task in the list to run on the current thread if we submit under the same executor,
        PolicyExecutor executor = callbacks.length > 0 ? callbacks[callbacks.length - 1].policyExecutor : policyExecutor;
        return (List) executor.invokeAll(tasks, callbacks);
    }

    /** {@inheritDoc} */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        Entry<Collection<? extends Callable<T>>, TaskLifeCycleCallback[]> entry = createCallbacks(tasks);
        tasks = entry.getKey();
        TaskLifeCycleCallback[] callbacks = entry.getValue();

        PolicyExecutor executor = callbacks.length > 0 ? callbacks[0].policyExecutor : policyExecutor;
        return (List) executor.invokeAll(tasks, callbacks, timeout, unit);
    }

    /** {@inheritDoc} */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        Entry<Collection<? extends Callable<T>>, TaskLifeCycleCallback[]> entry = createCallbacks(tasks);
        tasks = entry.getKey();
        TaskLifeCycleCallback[] callbacks = entry.getValue();

        PolicyExecutor executor = callbacks.length > 0 ? callbacks[0].policyExecutor : policyExecutor;
        return executor.invokeAny(tasks, callbacks);
    }

    /** {@inheritDoc} */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        Entry<Collection<? extends Callable<T>>, TaskLifeCycleCallback[]> entry = createCallbacks(tasks);
        tasks = entry.getKey();
        TaskLifeCycleCallback[] callbacks = entry.getValue();

        PolicyExecutor executor = callbacks.length > 0 ? callbacks[0].policyExecutor : policyExecutor;
        return executor.invokeAny(tasks, callbacks, timeout, unit);
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public boolean isShutdown() {
        if (allowLifeCycleMethods)
            return getNormalPolicyExecutor().isShutdown();
        else // Section 3.1.6.1 of the Concurrency Utilities spec requires IllegalStateException
            throw new IllegalStateException(new UnsupportedOperationException("isShutdown"));
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public boolean isTerminated() {
        if (allowLifeCycleMethods)
            return getNormalPolicyExecutor().isTerminated();
        else // Section 3.1.6.1 of the Concurrency Utilities spec requires IllegalStateException
            throw new IllegalStateException(new UnsupportedOperationException("isTerminated"));
    }

    @Override
    public <U> CompletableFuture<U> newIncompleteFuture() {
        if (ManagedCompletableFuture.JAVA8)
            return new ManagedCompletableFuture<U>(new CompletableFuture<U>(), this, null);
        else
            return new ManagedCompletableFuture<U>(this, null);
    }

    @Override
    public CompletableFuture<Void> runAsync(Runnable runnable) {
        return ManagedCompletableFuture.runAsync(runnable, this);
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
     * Declarative Services method for setting the concurrency service.
     *
     * @param svc the service
     */
    @Reference(policy = ReferencePolicy.STATIC)
    protected void setConcurrencyService(ConcurrencyService svc) {
        concurrencySvc = svc;
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
     * Declarative Services method for setting the Jakarta/Java EE version
     *
     * @param ref reference to the service
     */
    @Reference(service = JavaEEVersion.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setEEVersion(ServiceReference<JavaEEVersion> ref) {
        String version = (String) ref.getProperty("version");
        if (version == null) {
            eeVersion = 0;
        } else {
            int dot = version.indexOf('.');
            String major = dot > 0 ? version.substring(0, dot) : version;
            eeVersion = Integer.parseInt(major);
        }
        eeVersionRef = ref;
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
        if (allowLifeCycleMethods)
            getNormalPolicyExecutor().shutdown();
        else // Section 3.1.6.1 of the Concurrency Utilities spec requires IllegalStateException
            throw new IllegalStateException(new UnsupportedOperationException("shutdown"));
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public List<Runnable> shutdownNow() {
        if (allowLifeCycleMethods)
            return getNormalPolicyExecutor().shutdownNow();
        else // Section 3.1.6.1 of the Concurrency Utilities spec requires IllegalStateException
            throw new IllegalStateException(new UnsupportedOperationException("shutdownNow"));
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public <T> Future<T> submit(Callable<T> task) {
        Map<String, String> execProps = getExecutionProperties(task);

        ThreadContextDescriptor contextDescriptor;
        if (task instanceof ContextualAction) {
            ContextualAction<Callable<T>> a = (ContextualAction<Callable<T>>) task;
            contextDescriptor = a.getContextDescriptor();
            task = a.getAction();
        } else {
            contextDescriptor = captureThreadContext(execProps);
        }

        TaskLifeCycleCallback callback = new TaskLifeCycleCallback(this, contextDescriptor);
        return callback.policyExecutor.submit(task, callback);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public <T> Future<T> submit(Runnable task, T result) {
        Map<String, String> execProps = getExecutionProperties(task);

        ThreadContextDescriptor contextDescriptor;
        if (task instanceof ContextualAction) {
            ContextualAction<Runnable> a = (ContextualAction<Runnable>) task;
            contextDescriptor = a.getContextDescriptor();
            task = a.getAction();
        } else {
            contextDescriptor = captureThreadContext(execProps);
        }

        TaskLifeCycleCallback callback = new TaskLifeCycleCallback(this, contextDescriptor);
        return callback.policyExecutor.submit(task, result, callback);
    }

    /** {@inheritDoc} */
    @Override
    public Future<?> submit(Runnable task) {
        return submit(task, null);
    }

    @Override
    public <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return ManagedCompletableFuture.supplyAsync(supplier, this);
    }

    /**
     * Uses the transaction context provider to suspends the currently active transaction or LTC on the thread.
     *
     * @return ThreadContext instance that must be used to restore the suspended transaction context if returned.
     *         Null if transaction context is unavailable.
     */
    @SuppressWarnings("deprecation")
    ThreadContext suspendTransaction() {
        Map<String, String> XPROPS_SUSPEND_TRAN = eeVersion < 9 ? JAVAX_SUSPEND_TRAN : JAKARTA_SUSPEND_TRAN;
        ThreadContextProvider tranContextProvider = tranContextProviderRef.getService(); // doPriv is covered by AtomicServiceReference
        ThreadContext suspendedTranSnapshot = tranContextProvider == null ? null : tranContextProvider.captureThreadContext(XPROPS_SUSPEND_TRAN, null);
        if (suspendedTranSnapshot != null)
            suspendedTranSnapshot.taskStarting();
        return suspendedTranSnapshot;
    }

    @Override
    @Trivial
    public String toString() {
        String s = name.get();
        return s != null && s.startsWith("ManagedExecutor@") ? s : ("ManagedExecutor@" + Integer.toHexString(hashCode()) + ' ' + s);
    }

    /**
     * Declarative Services method for unsetting the concurrency policy
     *
     * @param svc the service
     */
    protected void unsetConcurrencyPolicy(ConcurrencyPolicy svc) {
    }

    /**
     * Declarative Services method for unsetting the concurrency service
     *
     * @param svc the service
     */
    protected void unsetConcurrencyService(ConcurrencyService svc) {
        // As a static dependency, unset of the ConcurrencyService will deactivate this instance
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
     * Declarative Services method for unsetting the Jakarta/Java EE version
     *
     * @param ref reference to the service
     */
    protected void unsetEEVersion(ServiceReference<JavaEEVersion> ref) {
        if (eeVersionRef == ref) {
            eeVersionRef = null;
            eeVersion = 0;
        }
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