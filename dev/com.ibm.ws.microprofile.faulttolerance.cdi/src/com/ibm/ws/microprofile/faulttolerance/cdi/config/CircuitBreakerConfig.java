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

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.impl.AbstractAnnotationConfig;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceProvider;

public class CircuitBreakerConfig extends AbstractAnnotationConfig<CircuitBreaker> {

    private static final TraceComponent tc = Tr.register(CircuitBreakerConfig.class);

    private final AnnotationParameterConfig<Class<? extends Throwable>[]> failOnConfig = getParameterConfigClassArray("failOn", Throwable.class);
    private final AnnotationParameterConfig<Long> delayConfig = getParameterConfig("delay", Long.class);
    private final AnnotationParameterConfig<ChronoUnit> delayUnitConfig = getParameterConfig("delayUnit", ChronoUnit.class);
    private final AnnotationParameterConfig<Integer> requestVolumeThresholdConfig = getParameterConfig("requestVolumeThreshold", Integer.class);
    private final AnnotationParameterConfig<Double> failureRatioConfig = getParameterConfig("failureRatio", Double.class);
    private final AnnotationParameterConfig<Integer> successThresholdConfig = getParameterConfig("successThreshold", Integer.class);

    public CircuitBreakerConfig(Class<?> annotatedClass, CircuitBreaker annotation) {
        super(annotatedClass, annotation, CircuitBreaker.class);
    }

    public CircuitBreakerConfig(Method annotatedMethod, Class<?> annotatedClass, CircuitBreaker annotation) {
        super(annotatedMethod, annotatedClass, annotation, CircuitBreaker.class);
    }

    private Class<? extends Throwable>[] failOn() {
        return failOnConfig.getValue();
    }

    private long delay() {
        return delayConfig.getValue();
    }

    private ChronoUnit delayUnit() {
        return delayUnitConfig.getValue();
    }

    private int requestVolumeThreshold() {
        return requestVolumeThresholdConfig.getValue();
    }

    private double failureRatio() {
        return failureRatioConfig.getValue();
    }

    private int successThreshold() {
        return successThresholdConfig.getValue();
    }

    /**
     * Validate the CircuitBreaker policy
     */
    @Override
    public void validate() {
        String target = getTargetName();
        //validate the parameters
        if (delay() < 0) {
            throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "circuitBreaker.parameter.delay.invalid.value.CWMFT5012E", "delay", delay(), target));
        }
        if ((failureRatio() < 0) || (failureRatio() > 1)) {
            throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "circuitBreaker.parameter.failureRatio.invalid.value.CWMFT5013E", "failureRatio",
                                                                         failureRatio(),
                                                                         target));
        }
        if (requestVolumeThreshold() < 1) {
            throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "circuitBreaker.parameter.requestVolumeThreshold.invalid.value.CWMFT5014E", "requestVolumeThreshold",
                                                                         requestVolumeThreshold(), target));
        }
        if (successThreshold() < 1) {
            throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "circuitBreaker.parameter.successThreshold.invalid.value.CWMFT5015E", "successThreshold",
                                                                         successThreshold(), target));
        }
        if (failOn().length == 0) {
            throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "circuitBreaker.parameter.failOn.invalid.value.CWMFT5018E", "failOn", target));
        }

    }

    public CircuitBreakerPolicy generatePolicy() {
        Class<? extends Throwable>[] failOn = failOn();
        Duration delay = Duration.of(delay(), delayUnit());
        int requestVolumeThreshold = requestVolumeThreshold();
        double failureRatio = failureRatio();
        int successThreshold = successThreshold();

        CircuitBreakerPolicy circuitBreakerPolicy = FaultToleranceProvider.newCircuitBreakerPolicy();

        circuitBreakerPolicy.setFailOn(failOn);
        circuitBreakerPolicy.setDelay(delay);
        circuitBreakerPolicy.setRequestVolumeThreshold(requestVolumeThreshold);
        circuitBreakerPolicy.setFailureRatio(failureRatio);
        circuitBreakerPolicy.setSuccessThreshold(successThreshold);

        return circuitBreakerPolicy;
    }
}
