/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.cdi.config;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.impl.AbstractAnnotationConfig;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceProvider;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;

public class RetryConfig extends AbstractAnnotationConfig<Retry> {

    private static final TraceComponent tc = Tr.register(RetryConfig.class);

    private final AnnotationParameterConfig<Integer> maxRetriesConfig = getParameterConfig("maxRetries", Integer.class);
    private final AnnotationParameterConfig<Long> delayConfig = getParameterConfig("delay", Long.class);
    private final AnnotationParameterConfig<ChronoUnit> delayUnitConfig = getParameterConfig("delayUnit", ChronoUnit.class);
    private final AnnotationParameterConfig<Long> maxDurationConfig = getParameterConfig("maxDuration", Long.class);
    private final AnnotationParameterConfig<ChronoUnit> durationUnitConfig = getParameterConfig("durationUnit", ChronoUnit.class);
    private final AnnotationParameterConfig<Long> jitterConfig = getParameterConfig("jitter", Long.class);
    private final AnnotationParameterConfig<ChronoUnit> jitterDelayUnitConfig = getParameterConfig("jitterDelayUnit", ChronoUnit.class);
    private final AnnotationParameterConfig<Class<? extends Throwable>[]> retryOnConfig = getParameterConfigClassArray("retryOn", Throwable.class);
    private final AnnotationParameterConfig<Class<? extends Throwable>[]> abortOnConfig = getParameterConfigClassArray("abortOn", Throwable.class);

    public RetryConfig(Class<?> annotatedClass, Retry annotation) {
        super(annotatedClass, annotation, Retry.class);
    }

    public RetryConfig(Method annotatedMethod, Class<?> annotatedClass, Retry annotation) {
        super(annotatedMethod, annotatedClass, annotation, Retry.class);
    }

    private int maxRetries() {
        return maxRetriesConfig.getValue();
    }

    private long delay() {
        return delayConfig.getValue();
    }

    private ChronoUnit delayUnit() {
        return delayUnitConfig.getValue();
    }

    private long maxDuration() {
        return maxDurationConfig.getValue();
    }

    private ChronoUnit durationUnit() {
        return durationUnitConfig.getValue();
    }

    private long jitter() {
        return jitterConfig.getValue();
    }

    private ChronoUnit jitterDelayUnit() {
        return jitterDelayUnitConfig.getValue();
    }

    private Class<? extends Throwable>[] retryOn() {
        return retryOnConfig.getValue();
    }

    private Class<? extends Throwable>[] abortOn() {
        return abortOnConfig.getValue();
    }

    /**
     * Validate the Retry policy to make sure all the parameters e.g. maxRetries, delay, jitter, maxDuration must not be negative.
     */
    @Override
    public void validate() {
        //validate the parameters
        String target = getTargetName();
        if (maxRetries() < -1) {

            throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "retry.parameter.invalid.value.CWMFT5010E", "maxRetries", maxRetries(), target, "-1"));
        }
        if ((delay() < 0)) {

            throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "retry.parameter.invalid.value.CWMFT5010E", "delay", delay(), target, "0"));
        }
        if ((jitter() < 0)) {

            throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "retry.parameter.invalid.value.CWMFT5010E", "jitter", jitter(), target, "0"));
        }
        if (maxDuration() < 0) {

            throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "retry.parameter.invalid.value.CWMFT5010E", "maxDuration", maxDuration(), target, "0"));
        }
        if (maxDuration() != 0) {
            Duration maxDuration = Duration.of(maxDuration(), durationUnit());
            Duration delay = Duration.of(delay(), delayUnit());
            if (maxDuration.compareTo(delay) < 0) {
                throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "retry.parameter.invalid.value.CWMFT5017E", maxDuration(), durationUnit(), target,
                                                                             delay(),
                                                                             delayUnit()));

            }
        }
        if (tc.isWarningEnabled()) {
            if ((delay() != 0) && (jitter() != 0)) {
                Duration jitter = Duration.of(jitter(), jitterDelayUnit());
                Duration delay = Duration.of(delay(), delayUnit());
                if (jitter.compareTo(delay) >= 0) {
                    Tr.warning(tc, "retry.parameter.invalid.value.CWMFT5019W", jitter(), jitterDelayUnit(), target,
                               delay(),
                               delayUnit());

                }
            }
        }
    }

    public RetryPolicy generatePolicy() {
        int maxRetries = maxRetries();
        Duration delay = Duration.of(delay(), delayUnit());
        Duration maxDuration = Duration.of(maxDuration(), durationUnit());
        Duration jitter = Duration.of(jitter(), jitterDelayUnit());
        Class<? extends Throwable>[] retryOn = retryOn();
        Class<? extends Throwable>[] abortOn = abortOn();

        RetryPolicy retryPolicy = FaultToleranceProvider.newRetryPolicy();

        retryPolicy.setMaxRetries(maxRetries);
        retryPolicy.setDelay(delay);
        retryPolicy.setMaxDuration(maxDuration);
        retryPolicy.setJitter(jitter);
        retryPolicy.setRetryOn(retryOn);
        retryPolicy.setAbortOn(abortOn);

        return retryPolicy;
    }

}
