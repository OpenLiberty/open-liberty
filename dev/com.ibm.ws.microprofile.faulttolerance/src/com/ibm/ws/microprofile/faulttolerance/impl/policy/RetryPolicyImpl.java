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
package com.ibm.ws.microprofile.faulttolerance.impl.policy;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;

/**
 *
 */
public class RetryPolicyImpl implements RetryPolicy {

    private static final TraceComponent tc = Tr.register(RetryPolicyImpl.class);

    private int maxRetries;
    private Duration delay;
    private Duration maxDuration;
    private Duration jitter;
    private Class<? extends Throwable>[] retryOn;
    private Class<? extends Throwable>[] abortOn;

    /**
     *
     */
    @SuppressWarnings("unchecked")
    public RetryPolicyImpl() {
        try {

            maxRetries = (int) Retry.class.getMethod("maxRetries").getDefaultValue();

            long longDelay = (long) Retry.class.getMethod("delay").getDefaultValue();
            ChronoUnit delayUnit = (ChronoUnit) Retry.class.getMethod("delayUnit").getDefaultValue();
            delay = Duration.of(longDelay, delayUnit);

            long longMaxDuration = (long) Retry.class.getMethod("maxDuration").getDefaultValue();
            ChronoUnit durationUnit = (ChronoUnit) Retry.class.getMethod("durationUnit").getDefaultValue();
            maxDuration = Duration.of(longMaxDuration, durationUnit);

            long longJitter = (long) Retry.class.getMethod("jitter").getDefaultValue();
            ChronoUnit jitterUnit = (ChronoUnit) Retry.class.getMethod("jitterDelayUnit").getDefaultValue();
            jitter = Duration.of(longJitter, jitterUnit);

            retryOn = (Class<? extends Throwable>[]) Retry.class.getMethod("retryOn").getDefaultValue();
            abortOn = (Class<? extends Throwable>[]) Retry.class.getMethod("abortOn").getDefaultValue();
        } catch (NoSuchMethodException | SecurityException e) {
            throw new FaultToleranceException(Tr.formatMessage(tc, "internal.error.CWMFT4998E", e), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxRetries() {
        return this.maxRetries;
    }

    /** {@inheritDoc} */
    @Override
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /** {@inheritDoc} */
    @Override
    public Duration getDelay() {
        return this.delay;
    }

    /** {@inheritDoc} */
    @Override
    public void setDelay(Duration delay) {
        this.delay = delay;
    }

    /** {@inheritDoc} */
    @Override
    public Duration getMaxDuration() {
        return this.maxDuration;
    }

    /** {@inheritDoc} */
    @Override
    public void setMaxDuration(Duration maxDuration) {
        this.maxDuration = maxDuration;
    }

    /** {@inheritDoc} */
    @Override
    public Duration getJitter() {
        return this.jitter;
    }

    /** {@inheritDoc} */
    @Override
    public void setJitter(Duration jitter) {
        this.jitter = jitter;
    }

    /** {@inheritDoc} */
    @Override
    public Class<? extends Throwable>[] getRetryOn() {
        return this.retryOn;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public void setRetryOn(Class<? extends Throwable>... retryOn) {
        this.retryOn = retryOn;
    }

    /** {@inheritDoc} */
    @Override
    public Class<? extends Throwable>[] getAbortOn() {
        return this.abortOn;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public void setAbortOn(Class<? extends Throwable>... abortOn) {
        this.abortOn = abortOn;
    }

}
