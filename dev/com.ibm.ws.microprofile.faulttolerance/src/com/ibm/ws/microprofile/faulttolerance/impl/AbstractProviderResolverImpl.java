/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.faulttolerance.impl.policy.BulkheadPolicyImpl;
import com.ibm.ws.microprofile.faulttolerance.impl.policy.CircuitBreakerPolicyImpl;
import com.ibm.ws.microprofile.faulttolerance.impl.policy.FallbackPolicyImpl;
import com.ibm.ws.microprofile.faulttolerance.impl.policy.RetryPolicyImpl;
import com.ibm.ws.microprofile.faulttolerance.impl.policy.TimeoutPolicyImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceProviderResolver;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;
import com.ibm.ws.threading.PolicyExecutorProvider;

/**
 *
 */
public abstract class AbstractProviderResolverImpl extends FaultToleranceProviderResolver {

    private static final TraceComponent tc = Tr.register(AbstractProviderResolverImpl.class);
    @Reference
    protected PolicyExecutorProvider policyExecutorProvider;
    @Reference(target = "(deferrable=false)")
    protected ScheduledExecutorService scheduledExecutorService;
    private ScheduledExecutorService jseScheduledExecutorService;

    /**
     *
     */
    public AbstractProviderResolverImpl() {
        super();
    }

    /**
     * Activate a context and set the instance
     *
     * @param cc
     */
    public void activate(ComponentContext cc) {
        FaultToleranceProviderResolver.setInstance(this);
    }

    /**
     * Deactivate a context and set the instance to null
     *
     * @param cc
     */
    public void deactivate(ComponentContext cc) throws IOException {
        FaultToleranceProviderResolver.setInstance(null);
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

    protected ScheduledExecutorService getScheduledExecutorService() {
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