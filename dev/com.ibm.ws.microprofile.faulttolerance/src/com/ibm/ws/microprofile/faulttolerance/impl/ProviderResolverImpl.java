/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.impl;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.faulttolerance.impl.policy.BulkheadPolicyImpl;
import com.ibm.ws.microprofile.faulttolerance.impl.policy.CircuitBreakerPolicyImpl;
import com.ibm.ws.microprofile.faulttolerance.impl.policy.FallbackPolicyImpl;
import com.ibm.ws.microprofile.faulttolerance.impl.policy.RetryPolicyImpl;
import com.ibm.ws.microprofile.faulttolerance.impl.policy.TimeoutPolicyImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.ExecutorBuilder;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceProviderResolver;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;
import com.ibm.ws.threading.PolicyExecutorProvider;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.threadcontext.WSContextService;

@Component(name = "com.ibm.ws.microprofile.faulttolerance.impl.ProviderResolverImpl", service = { FaultToleranceProviderResolver.class }, property = { "service.vendor=IBM" }, immediate = true)
public class ProviderResolverImpl extends FaultToleranceProviderResolver {

    private static final TraceComponent tc = Tr.register(ProviderResolverImpl.class);

    /**
     * Reference to the context service for this managed executor service.
     */
    private final AtomicServiceReference<WSContextService> contextSvcRef = new AtomicServiceReference<WSContextService>("ContextService");

    private final AtomicServiceReference<PolicyExecutorProvider> policyExecRef = new AtomicServiceReference<>("PolicyExecutorProvider");

    private final AtomicServiceReference<ScheduledExecutorService> scheduledExecRef = new AtomicServiceReference<>("ScheduledExecutorService");

    private ScheduledExecutorService jseScheduledExecutorService;

    /**
     * Activate a context and set the instance
     *
     * @param cc
     */
    public void activate(ComponentContext cc) {
        contextSvcRef.activate(cc);
        policyExecRef.activate(cc);
        scheduledExecRef.activate(cc);
        FaultToleranceProviderResolver.setInstance(this);
    }

    /**
     * Deactivate a context and set the instance to null
     *
     * @param cc
     */
    public void deactivate(ComponentContext cc) throws IOException {
        FaultToleranceProviderResolver.setInstance(null);
        scheduledExecRef.deactivate(cc);
        policyExecRef.deactivate(cc);
        contextSvcRef.deactivate(cc);
    }

    /**
     * Declarative Services method for setting the context service reference
     *
     * @param ref reference to the service
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected void setContextService(ServiceReference<WSContextService> ref) {
        contextSvcRef.setReference(ref);
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
     * Declarative Services method for setting the Policy Executor Provider reference
     *
     * @param ref reference to the service
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected void setPolicyExecutorProvider(ServiceReference<PolicyExecutorProvider> ref) {
        policyExecRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the Policy Executor Provider reference
     *
     * @param ref reference to the service
     */
    protected void unsetPolicyExecutorProvider(ServiceReference<PolicyExecutorProvider> ref) {
        policyExecRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for setting the Scheduled Executor Service reference
     *
     * @param ref reference to the service
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected void setScheduledExecutorService(ServiceReference<ScheduledExecutorService> ref) {
        scheduledExecRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the Scheduled Executor Service reference
     *
     * @param ref reference to the service
     */
    protected void unsetScheduledExecutorService(ServiceReference<ScheduledExecutorService> ref) {
        scheduledExecRef.unsetReference(ref);
    }

    @Override
    public BulkheadPolicy newBulkheadPolicy() {
        BulkheadPolicyImpl bulkhead = new BulkheadPolicyImpl();
        return bulkhead;
    }

    @Override
    public RetryPolicy newRetryPolicy() {
        RetryPolicyImpl retry = new RetryPolicyImpl();
        return retry;
    }

    @Override
    public CircuitBreakerPolicy newCircuitBreakerPolicy() {
        CircuitBreakerPolicyImpl circuitBreaker = new CircuitBreakerPolicyImpl();
        return circuitBreaker;
    }

    @Override
    public FallbackPolicy newFallbackPolicy() {
        FallbackPolicyImpl fallback = new FallbackPolicyImpl();
        return fallback;
    }

    /** {@inheritDoc} */
    @Override
    public TimeoutPolicy newTimeoutPolicy() {
        TimeoutPolicyImpl timeout = new TimeoutPolicyImpl();
        return timeout;
    }

    @Override
    public <T, R> ExecutorBuilder<T, R> newExecutionBuilder() {
        WSContextService contextService = getContextService();
        PolicyExecutorProvider policyExecutorProvider = getPolicyExecutorProvider();
        ScheduledExecutorService scheduledExecutorService = getScheduledExecutorService();
        ExecutorBuilderImpl<T, R> ex = new ExecutorBuilderImpl<T, R>(contextService, policyExecutorProvider, scheduledExecutorService);
        return ex;
    }

    private WSContextService getContextService() {
        WSContextService contextService = AccessController.doPrivileged(new PrivilegedAction<WSContextService>() {
            @Override
            public WSContextService run() {
                return contextSvcRef.getService();
            }
        });
        return contextService;
    }

    private PolicyExecutorProvider getPolicyExecutorProvider() {
        PolicyExecutorProvider policyExecutorProvider = AccessController.doPrivileged(new PrivilegedAction<PolicyExecutorProvider>() {
            @Override
            public PolicyExecutorProvider run() {
                return policyExecRef.getService();
            }
        });
        return policyExecutorProvider;
    }

    protected ScheduledExecutorService getScheduledExecutorService() {
        ScheduledExecutorService scheduledExecutorService = AccessController.doPrivileged(new PrivilegedAction<ScheduledExecutorService>() {
            @Override
            public ScheduledExecutorService run() {
                return scheduledExecRef.getService();
            }
        });

        if (scheduledExecutorService == null) {
            //this is really intended for unittest only, running outside of Liberty
            if ("true".equalsIgnoreCase(System.getProperty(FTConstants.JSE_FLAG))) {
                if (this.jseScheduledExecutorService == null) {
                    this.jseScheduledExecutorService = Executors.newScheduledThreadPool(10);
                }
                scheduledExecutorService = jseScheduledExecutorService;
            } else {
                throw new FaultToleranceException(Tr.formatMessage(tc, "internal.error.CWMFT4999E"));
            }
        }

        return scheduledExecutorService;
    }
}
