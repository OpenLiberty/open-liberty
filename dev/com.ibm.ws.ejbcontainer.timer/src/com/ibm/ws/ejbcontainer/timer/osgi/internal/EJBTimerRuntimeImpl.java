/*******************************************************************************
 * Copyright (c) 2013, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.osgi.internal;

import java.util.HashMap;
import java.util.Map;
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

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.TimerNpImpl;
import com.ibm.ejs.container.TimerNpRunnable;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ejbcontainer.osgi.EJBPersistentTimerRuntime;
import com.ibm.ws.ejbcontainer.osgi.EJBTimerRuntime;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

@Component(name = "com.ibm.ws.ejbcontainer.osgi.EJBTimerRuntime",
           service = EJBTimerRuntime.class,
           configurationPid = "com.ibm.ws.ejbcontainer.timer.runtime",
           configurationPolicy = ConfigurationPolicy.OPTIONAL,
           property = { "service.vendor=IBM", "contextService.target=(id=unbound)" })
public class EJBTimerRuntimeImpl implements EJBTimerRuntime {

    private static final TraceComponent tc = Tr.register(EJBTimerRuntimeImpl.class);

    private static final String REFERENCE_PERSISTENT_EXECUTOR = "persistentExecutor";
    private static final String REFERENCE_EJB_PERSISTENT_TIMER_RUNTIME = "ejbPersistentTimerRuntime";
    private static final String REFERENCE_NON_PERSISTENT_CONTEXT_SERVICE = "nonPersistentContextService";

    private static final String PERSISTENT_EXECUTOR_REF = "persistentExecutorRef";
    private static final String LATE_TIMER_THRESHOLD = "lateTimerThreshold";
    private static final String NP_RETRY_INTERVAL = "nonPersistentRetryInterval";
    private static final String NP_MAX_RETRIES = "nonPersistentMaxRetries";

    private ScheduledExecutorService executorService;

    private volatile String configuredPersistentExecutor;
    private final AtomicServiceReference<ScheduledExecutorService> persistentExecutorRef = new AtomicServiceReference<ScheduledExecutorService>(REFERENCE_PERSISTENT_EXECUTOR);
    private final AtomicServiceReference<EJBPersistentTimerRuntime> ejbPersistentTimerRuntimeServiceRef = new AtomicServiceReference<EJBPersistentTimerRuntime>(REFERENCE_EJB_PERSISTENT_TIMER_RUNTIME);
    private final AtomicServiceReference<WSContextService> nonPersistentContextServiceRef = new AtomicServiceReference<WSContextService>(REFERENCE_NON_PERSISTENT_CONTEXT_SERVICE);

    /**
     * This is the value of javax.enterprise.concurrent.ManagedTask.IDENTITY_NAME,
     * but is hard-coded here to avoid a dependency on the concurrency feature.
     */
    private static final String MANAGEDTASK_IDENTITY_NAME = "javax.enterprise.concurrent.IDENTITY_NAME";

    /**
     * Late timer warning threshold in milliseconds, set from
     * lateTimerThreshold attribute on timerService configuration.
     */
    private volatile long lateTimerThreshold = 5 * 60 * 1000;

    /**
     * Timer timeout roll backs will be retried the following number of times (once
     * immediately, and thereafter on the retry interval. The EJB 3.1 spec, section
     * 18.4.3 requires at least one retry be attempted. If -1, retry indefinitely.
     *
     * Set from nonPersistentTimerRetryCount attribute on EJBTimer config.
     * Default is not set, which is represented here as -1, and means
     * to retry indefinitely. Configuration of 0 is allowed, and means
     * no retries, but is not a spec compliant configuration.
     */
    private volatile int npTimerServiceTimerRetryCount = -1;

    /**
     * Non-persistent timer retry interval in milliseconds, set from
     * nonPersistentTimerRetryInterval attribute on EJBTimer config. Default is
     * 300 sec. 0 means retry immediately (unless retry count has been reached).
     */
    private volatile long npTimerServiceTimerRetryInterval = 300 * 1000;

    @Reference
    protected void setExecutorService(ScheduledExecutorService executor) {
        executorService = executor;
    }

    protected void unsetExecutorService(ScheduledExecutorService executor) {
        executorService = null;
    }

    @Reference(name = REFERENCE_PERSISTENT_EXECUTOR,
               service = ScheduledExecutorService.class,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               cardinality = ReferenceCardinality.OPTIONAL,
               target = "(id=unbound)")
    protected void setPersistentExecutor(ServiceReference<ScheduledExecutorService> ref) {
        persistentExecutorRef.setReference(ref);

        EJBPersistentTimerRuntime pTRuntime = getPersistentTimerRuntime();
        if (pTRuntime != null) {
            pTRuntime.resetAndCheckDatabasePolling();
        }
    }

    protected void unsetPersistentExecutor(ServiceReference<ScheduledExecutorService> ref) {
        persistentExecutorRef.unsetReference(ref);

        EJBPersistentTimerRuntime pTRuntime = getPersistentTimerRuntime();
        if (pTRuntime != null) {
            pTRuntime.resetAndCheckDatabasePolling();
        }
    }

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> properties) {
        persistentExecutorRef.activate(cc);
        ejbPersistentTimerRuntimeServiceRef.activate(cc);
        nonPersistentContextServiceRef.activate(cc);
        updateConfiguration(properties);
    }

    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> properties) {
        updateConfiguration(properties);
    }

    @Trivial
    private void updateConfiguration(Map<String, Object> properties) {
        configuredPersistentExecutor = (String) properties.get(PERSISTENT_EXECUTOR_REF);
        Long lateTimerThresholdMinutes = (Long) properties.get(LATE_TIMER_THRESHOLD);
        Integer npMaxRetries = (Integer) properties.get(NP_MAX_RETRIES);
        Long npRetryIntervalSeconds = (Long) properties.get(NP_RETRY_INTERVAL);
        if (lateTimerThresholdMinutes != null) {
            lateTimerThreshold = TimeUnit.MINUTES.toMillis(lateTimerThresholdMinutes);
        }
        if (npMaxRetries != null) {
            npTimerServiceTimerRetryCount = npMaxRetries;
        }
        if (npRetryIntervalSeconds != null) {
            npTimerServiceTimerRetryInterval = TimeUnit.MILLISECONDS.convert(npRetryIntervalSeconds, TimeUnit.SECONDS);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, LATE_TIMER_THRESHOLD + "=" + lateTimerThreshold + ", " +
                         NP_MAX_RETRIES + "=" + npTimerServiceTimerRetryCount + ", " +
                         NP_RETRY_INTERVAL + "=" + npTimerServiceTimerRetryInterval);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        persistentExecutorRef.deactivate(cc);
        ejbPersistentTimerRuntimeServiceRef.deactivate(cc);
        nonPersistentContextServiceRef.deactivate(cc);
    }

    @Override
    public TimerNpRunnable createNonPersistentTimerTaskHandler(TimerNpImpl timer) {
        TimerNpSERunnable timerRunnable = new TimerNpSERunnable(executorService, timer, npTimerServiceTimerRetryCount, npTimerServiceTimerRetryInterval);
        Runnable r = createNonPersistantTimerContextualProxy(timerRunnable);
        timerRunnable.setContextualProxyRunnable(r);
        return timerRunnable;
    }

    @Override
    public long getLateTimerThreshold() {
        return lateTimerThreshold;
    }

    @Override
    public ScheduledExecutorService getPersistentExecutor() {
        ScheduledExecutorService pExecutor = persistentExecutorRef.getService();
        if (pExecutor == null && configuredPersistentExecutor != null) {
            throw new IllegalStateException("The ejbPersistentTimer feature is enabled, but the "
                                            + configuredPersistentExecutor
                                            + " persistent executor configured for the timerService element in the server.xml"
                                            + " file cannot be resolved. Correct the configuration of the "
                                            + configuredPersistentExecutor
                                            + " persistent executor and ensure the referenced datasource is configured properly.");
        }
        return pExecutor;
    }

    @Override
    public ServiceReference<ScheduledExecutorService> getPersistentExecutorRef() {
        return persistentExecutorRef.getReference();
    }

    /**
     * Can return null
     */
    private EJBPersistentTimerRuntime getPersistentTimerRuntime() {
        return ejbPersistentTimerRuntimeServiceRef.getService();
    }

    @Reference(name = REFERENCE_EJB_PERSISTENT_TIMER_RUNTIME,
               service = EJBPersistentTimerRuntime.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected synchronized void setEJBPersistentTimerRuntime(ServiceReference<EJBPersistentTimerRuntime> ref) {
        this.ejbPersistentTimerRuntimeServiceRef.setReference(ref);
    }

    protected void unsetEJBPersistentTimerRuntime(ServiceReference<EJBPersistentTimerRuntime> ref) {
        this.ejbPersistentTimerRuntimeServiceRef.unsetReference(ref);
    }

    @Reference(name = REFERENCE_NON_PERSISTENT_CONTEXT_SERVICE,
               service = WSContextService.class,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               cardinality = ReferenceCardinality.OPTIONAL)
    protected void setNonPersistentContextServiceRef(ServiceReference<WSContextService> npContextServiceRef) {
        nonPersistentContextServiceRef.setReference(npContextServiceRef);
    }

    protected void updatedNonPersistentContextServiceRef(ServiceReference<WSContextService> npContextServiceRef) {
        // Don't care if the referenced context service has been updated
    }

    protected void unsetNonPersistentContextServiceRef(ServiceReference<WSContextService> npContextServiceRef) {
        nonPersistentContextServiceRef.unsetReference(npContextServiceRef);
    }

    /**
     * Creates contextual proxy with thread contexts needed for context propagation of non persistent EJB timers.
     * Only creates proxy if <contextService/> is configured with contexts, otherwise no contexts are propagated.
     *
     * @param taskHandler
     * @return Runnable proxy
     */
    public Runnable createNonPersistantTimerContextualProxy(TimerNpSERunnable taskHandler) {
        Runnable proxy = null;
        WSContextService contextService = nonPersistentContextServiceRef.getService();

        if (contextService != null) {
            TimerNpImpl tmr = taskHandler.getNpTimer();
            BeanMetaData beanMetaData = tmr.getBeanMetaData();
            int methodId = taskHandler.getMethodId();
            J2EEName j2eeName = beanMetaData.getJ2EEName();

            // Get method name from BeanMetaData
            String methodName;
            if (beanMetaData.timedMethodInfos != null) {
                methodName = beanMetaData.timedMethodInfos[methodId].getMethodName();
            } else {
                methodName = beanMetaData.ivInitData.ivTimerMethods.get(methodId).getMethod().getName();
            }

            String taskOwner = j2eeName.getApplication() + "/" + j2eeName.getModule() + "/" + j2eeName.getComponent();
            String taskIdentity = taskOwner + "-" + methodName;
            Map<String, String> executionProperties = new HashMap<String, String>();
            executionProperties.put(MANAGEDTASK_IDENTITY_NAME, taskIdentity);
            executionProperties.put(WSContextService.TASK_OWNER, taskOwner);
            Map<String, ?>[] requiredContexts = null;
            ThreadContextDescriptor tcDescriptor = contextService.captureThreadContext(executionProperties);
            proxy = contextService.createContextualProxy(tcDescriptor, taskHandler, Runnable.class);

            if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Created proxy for np timer.");
            }
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Context Service is null. Did not create proxy for np timer.");
            }
        }
        return proxy;
    }
}
