/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.async.osgi.internal;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

import com.ibm.ejs.container.ClientAsyncResult;
import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.ejs.container.EJSWrapperBase;
import com.ibm.ejs.container.RemoteAsyncResult;
import com.ibm.ejs.container.WrapperInterface;
import com.ibm.ejs.container.util.ExceptionUtil;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ejbcontainer.EJBMethodInterface;
import com.ibm.ws.ejbcontainer.EJBPMICollaborator;
import com.ibm.ws.ejbcontainer.osgi.EJBAsyncRuntime;
import com.ibm.ws.ejbcontainer.osgi.EJBRemoteRuntime;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Provides the runtime environment which enables EJB asynchronous methods
 * in the core container.
 */
@Component(service = EJBAsyncRuntime.class,
           configurationPid = "com.ibm.ws.ejbcontainer.asynchronous.runtime",
           configurationPolicy = ConfigurationPolicy.OPTIONAL,
           property = "contextService.target=(id=unbound)")
public class EJBAsyncRuntimeImpl implements EJBAsyncRuntime {

    private static final TraceComponent tc = Tr.register(EJBAsyncRuntimeImpl.class);

    // This value is taken from metatype.xml.
    private static final long DEFAULT_UNCLAIMED_REMOTE_RESULT_TIMEOUT_MILLIS = 24 * 60 * 60 * 1000;
    // This value is taken from metatype.xml.
    private static final int DEFAULT_MAX_UNCLAIMED_REMOTE_RESULTS = 1000;

    /**
     * This is the value of javax.enterprise.concurrent.ManagedTask.IDENTITY_NAME,
     * but is hard-coded here to avoid a dependency on the concurrency feature.
     */
    private static final String MANAGEDTASK_IDENTITY_NAME = "javax.enterprise.concurrent.IDENTITY_NAME";

    /**
     * Create the default set of thread context providers that must be propagated
     * for EJB async methods. Specifically, the specification calls out:
     *
     * - Security
     *
     * The EJB Container will establish the following, so they don't need to
     * be propagated:
     *
     * - ClassLoader
     * - Java EE Component
     * - Transaction
     *
     * And the following known context providers are not required by the
     * EJB Specification, so will not be propagated by default; only
     * if a custom context service is configured:
     *
     * - WLM Enclave (com.ibm.ws.zos.wlm.context.provider)
     * - SyncToOSThread (com.ibm.ws.security.thread.zos.context.provider)
     *
     * When EJB async methods are not configured with a specific contextService
     * instance, this set will indicate to the default context service singleton
     * all of the contexts that will be propagated. However, when concurrency is
     * enabled and a context service has been configured for use by EJB async
     * methods, then this list will not be used; the custom context service will
     * be used exactly as configured by the customer.
     */
    @SuppressWarnings("unchecked")
    private static final Map<String, ?>[] DEFAULT_ASYNC_REQUIRED_CONTEXTS = new Map[] {
                                                                                        Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER,
                                                                                                                 "com.ibm.ws.security.context.provider"),
    };

    private WSContextService defaultContextService;
    private ExecutorService executorService;
    private RemoteAsyncResultReaper remoteAsyncResultReaper;

    private long unclaimedRemoteResultTimeoutMillis;
    private int maxUnclaimedRemoteResults;

    private static final String REFERENCE_CONTEXT_SERVICE = "contextService";
    private static final String REFERENCE_REMOTE_RUNTIME = "remoteRuntime";
    private static final String REFERENCE_SCHEDULED_EXECUTOR_SERVICE = "scheduledExecutorService";

    // ContextService from asynchronous configuration
    private final AtomicServiceReference<WSContextService> contextServiceRef = new AtomicServiceReference<WSContextService>(REFERENCE_CONTEXT_SERVICE);
    private final AtomicServiceReference<EJBRemoteRuntime> remoteRuntimeRef = new AtomicServiceReference<EJBRemoteRuntime>(REFERENCE_REMOTE_RUNTIME);
    private final AtomicServiceReference<ScheduledExecutorService> scheduledExecutorServiceRef = new AtomicServiceReference<ScheduledExecutorService>(REFERENCE_SCHEDULED_EXECUTOR_SERVICE);

    @Reference(target = "(service.pid=com.ibm.ws.context.manager)")
    protected void setDefaultContextService(WSContextService contextService) {
        defaultContextService = contextService;
    }

    protected void unsetDefaultContextService(WSContextService contextService) {
        defaultContextService = null;
    }

    @Reference
    protected void setExecutorService(ExecutorService executor) {
        executorService = executor;
    }

    protected void unsetExecutorService(ExecutorService executor) {
        executorService = null;
    }

    @Reference(name = REFERENCE_CONTEXT_SERVICE,
               service = WSContextService.class,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               cardinality = ReferenceCardinality.OPTIONAL)
    protected void setContextService(ServiceReference<WSContextService> ref) {
        contextServiceRef.setReference(ref);
    }

    protected void updatedContextService(ServiceReference<WSContextService> ref) {
        // Don't care if the referenced context service has been updated
    }

    protected void unsetContextService(ServiceReference<WSContextService> ref) {
        contextServiceRef.unsetReference(ref);
    }

    @Reference(name = REFERENCE_REMOTE_RUNTIME,
               service = EJBRemoteRuntime.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setRemoteRuntime(ServiceReference<EJBRemoteRuntime> ref) {
        remoteRuntimeRef.setReference(ref);
    }

    protected void unsetRemoteRuntime(ServiceReference<EJBRemoteRuntime> ref) {
        synchronized (this) {
            if (remoteAsyncResultReaper != null) {
                remoteAsyncResultReaper.finalReap();
                remoteAsyncResultReaper = null;
            }
        }

        remoteRuntimeRef.unsetReference(ref);
    }

    @Reference(name = REFERENCE_SCHEDULED_EXECUTOR_SERVICE,
               service = ScheduledExecutorService.class,
               target = "(deferrable=true)")
    protected void setScheduledExecutorService(ServiceReference<ScheduledExecutorService> ref) {
        scheduledExecutorServiceRef.setReference(ref);
    }

    protected void unsetScheduledExecutor(ServiceReference<ScheduledExecutorService> ref) {
        scheduledExecutorServiceRef.unsetReference(ref);
    }

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> properties) {
        contextServiceRef.activate(cc);
        remoteRuntimeRef.activate(cc);
        scheduledExecutorServiceRef.activate(cc);

        modified(properties);
    }

    @Modified
    protected synchronized void modified(Map<String, Object> properties) {
        Long unclaimedRemoteResultTimeout = (Long) properties.get("unclaimedRemoteResultTimeout");
        this.unclaimedRemoteResultTimeoutMillis = unclaimedRemoteResultTimeout != null ? TimeUnit.MILLISECONDS.convert(unclaimedRemoteResultTimeout,
                                                                                                                       TimeUnit.SECONDS) : DEFAULT_UNCLAIMED_REMOTE_RESULT_TIMEOUT_MILLIS;

        Integer maxUnclaimedRemoteResults = (Integer) properties.get("maxUnclaimedRemoteResults");
        this.maxUnclaimedRemoteResults = maxUnclaimedRemoteResults != null ? maxUnclaimedRemoteResults : DEFAULT_MAX_UNCLAIMED_REMOTE_RESULTS;

        synchronized (this) {
            if (remoteAsyncResultReaper != null) {
                remoteAsyncResultReaper.configure(unclaimedRemoteResultTimeoutMillis, this.maxUnclaimedRemoteResults);
            }
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        synchronized (this) {
            if (remoteAsyncResultReaper != null) {
                remoteAsyncResultReaper.finalReap();
                remoteAsyncResultReaper = null;
            }
        }

        contextServiceRef.deactivate(cc);
        remoteRuntimeRef.deactivate(cc);
        scheduledExecutorServiceRef.deactivate(cc);
    }

    @Override
    @Trivial
    @FFDCIgnore(Throwable.class)
    public Future<?> scheduleAsync(EJSWrapperBase wrapper, EJBMethodInfoImpl methodInfo, int methodId, Object[] args) throws RemoteException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "scheduleAsync : " + wrapper.beanId + ", " + methodInfo.getMethodName());

        RemoteAsyncResultImpl remoteResult = null;
        ServerAsyncResultImpl serverResult;
        Future<?> clientResult;
        EJBPMICollaborator pmiBean = wrapper.ivPmiBean;
        boolean isVoidReturnType = (methodInfo.getMethod().getReturnType() == Void.TYPE);
        boolean startWorkAttemped = false;

        J2EEName j2eeName = wrapper.beanId.getJ2EEName();
        String taskOwner = j2eeName.getApplication() + "/" + j2eeName.getModule() + "/" + j2eeName.getComponent();
        String taskIdentity = taskOwner + "-" + methodInfo.getMethodName();

        try {
            // Build the asynchronous task (Runnable) that will run on the Executor.
            // Note that the server's internal async result (ie. Future) implementation
            // is passed here because the task object running in the Executor will be
            // updating results directly into this object. Also, note that if the
            // method is fire-and-forget (ie. return type = void), no async result is
            // passed into the task object because no results will be returned.

            if (!isVoidReturnType) {
                if (methodInfo.getEJBMethodInterface() == EJBMethodInterface.REMOTE) {
                    remoteResult = new RemoteAsyncResultImpl(remoteRuntimeRef.getServiceWithException(), getRemoteAsyncResultReaper(), pmiBean);
                    RemoteAsyncResult remoteStub = remoteResult.exportObject();

                    serverResult = remoteResult;

                    boolean isBusinessRmiRemote = wrapper.ivInterface == WrapperInterface.BUSINESS_RMI_REMOTE;
                    clientResult = new ClientAsyncResult(remoteStub, isBusinessRmiRemote); // d614994
                } else {
                    serverResult = new ServerAsyncResultImpl(pmiBean);
                    clientResult = serverResult;
                }
            } else {
                serverResult = null;
                clientResult = null;
            }

            Runnable asyncWrapper = new AsyncMethodWrapperImpl(wrapper, methodId, args, serverResult);

            // Capture the thread contexts from the current thread. When the default
            // singleton context service is used, the only contexts captured will be
            // ASYNC_REQUIRED_CONTEXTS. When a configured context service is being
            // used, then only the configured contexts will be propagated.
            Map<String, String> executionProperties = new HashMap<String, String>();
            executionProperties.put(MANAGEDTASK_IDENTITY_NAME, taskIdentity);
            executionProperties.put(WSContextService.TASK_OWNER, taskOwner);

            Map<String, ?>[] requiredContexts = null;
            WSContextService contextService = contextServiceRef.getService();
            if (contextService == null) {
                contextService = defaultContextService;
                requiredContexts = DEFAULT_ASYNC_REQUIRED_CONTEXTS;
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "Using default contextService : " + contextService);
            } else {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "Using configured contextService : " + contextService);
            }

            ThreadContextDescriptor tcDescriptor = contextService.captureThreadContext(executionProperties, requiredContexts);

            // Create a proxy for the EJB wrapper that interposes context propagation.
            Runnable asyncTask = contextService.createContextualProxy(tcDescriptor, asyncWrapper, Runnable.class);

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "Submitting Async Task : " + asyncTask + ", wrapper : " + asyncWrapper + ", result : " + serverResult);

            // Increment the number of methods added to the work manager queue
            // to calculate the queue size.
            if (pmiBean != null) {
                pmiBean.asyncQueSizeIncrement();
                startWorkAttemped = true;
            }

            // Submit the asynchronous method to the ExecutorService. It will run as soon
            // as a thread is available. If the task buffer is exceeded, trying to start
            // more work may result in an exception or block the current thread.
            if (serverResult != null) {
                serverResult.ivTaskFuture = executorService.submit(asyncTask);
            } else {
                executorService.execute(asyncTask);
            }
        } catch (Throwable t) {

            // No FFDC in "normal" code paths: the spec allows customer
            // applications to catch the following EJBException and retry.

            if (pmiBean != null) {
                if (isVoidReturnType) {
                    pmiBean.asyncFNFFailed();
                }
                if (startWorkAttemped) {
                    pmiBean.asyncQueSizeDecrement();
                }
            }

            if (remoteResult != null) {
                remoteResult.unexportObject();
            }

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "scheduleAsync : EJBException: " + t);

            // As required by the specification, wrap exceptions in an EJBException
            // and throw to the client.
            throw ExceptionUtil.EJBException("Failed to schedule asynchronous work", t);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "scheduleAsync : " + clientResult);
        return clientResult;
    }

    @Trivial
    private synchronized RemoteAsyncResultReaper getRemoteAsyncResultReaper() {
        RemoteAsyncResultReaper reaper = remoteAsyncResultReaper;
        if (reaper == null) {
            reaper = new RemoteAsyncResultReaper(scheduledExecutorServiceRef.getServiceWithException());
            reaper.configure(unclaimedRemoteResultTimeoutMillis, maxUnclaimedRemoteResults);
            remoteAsyncResultReaper = reaper;
        }
        return reaper;
    }
}
