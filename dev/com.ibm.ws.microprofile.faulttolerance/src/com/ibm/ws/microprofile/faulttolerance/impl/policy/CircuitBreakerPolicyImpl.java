/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.impl.policy;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;

/**
 *
 */
public class CircuitBreakerPolicyImpl implements CircuitBreakerPolicy {

    private static final TraceComponent tc = Tr.register(BulkheadPolicyImpl.class);

    private Class<? extends Throwable>[] failOn;
    private Class<? extends Throwable>[] skipOn;
    private Duration delay;
    private int requestVolumeThreshold;
    private double failureRatio;
    private int successThreshold;

    /**
     *
     */
    @SuppressWarnings("unchecked")
    public CircuitBreakerPolicyImpl() {
        try {
            failOn = (Class<? extends Throwable>[]) CircuitBreaker.class.getMethod("failOn").getDefaultValue();
            skipOn = new Class[0];
            long longDelay = (long) CircuitBreaker.class.getMethod("delay").getDefaultValue();
            ChronoUnit delayUnit = (ChronoUnit) CircuitBreaker.class.getMethod("delayUnit").getDefaultValue();
            delay = Duration.of(longDelay, delayUnit);
            requestVolumeThreshold = (int) CircuitBreaker.class.getMethod("requestVolumeThreshold").getDefaultValue();
            failureRatio = (double) CircuitBreaker.class.getMethod("failureRatio").getDefaultValue();
            successThreshold = (int) CircuitBreaker.class.getMethod("successThreshold").getDefaultValue();
        } catch (NoSuchMethodException | SecurityException e) {
            throw new FaultToleranceException(Tr.formatMessage(tc, "internal.error.CWMFT4998E", e), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Class<? extends Throwable>[] getFailOn() {
        return failOn;
    }

    /** {@inheritDoc} */
    @Override
    public void setFailOn(Class<? extends Throwable>... failOn) {
        this.failOn = failOn;
    }

    /** {@inheritDoc} */
    @Override
    public Class<? extends Throwable>[] getSkipOn() {
        return skipOn;
    }

    /** {@inheritDoc} */
    @Override
    public void setSkipOn(Class<? extends Throwable>... skipOn) {
        this.skipOn = skipOn;
    }

    /** {@inheritDoc} */
    @Override
    public Duration getDelay() {
        return delay;
    }

    /** {@inheritDoc} */
    @Override
    public void setDelay(Duration delay) {
        this.delay = delay;
    }

    /** {@inheritDoc} */
    @Override
    public int getRequestVolumeThreshold() {
        return requestVolumeThreshold;
    }

    /** {@inheritDoc} */
    @Override
    public void setRequestVolumeThreshold(int threshold) {
        this.requestVolumeThreshold = threshold;
    }

    /** {@inheritDoc} */
    @Override
    public double getFailureRatio() {
        return failureRatio;
    }

    /** {@inheritDoc} */
    @Override
    public void setFailureRatio(double ratio) {
        this.failureRatio = ratio;
    }

    /** {@inheritDoc} */
    @Override
    public int getSuccessThreshold() {
        return successThreshold;
    }

    /** {@inheritDoc} */
    @Override
    public void setSuccessThreshold(int threshold) {
        this.successThreshold = threshold;
    }

}
