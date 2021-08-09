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

import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.impl.AbstractAnnotationConfig;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceProvider;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;

public class TimeoutConfig extends AbstractAnnotationConfig<Timeout> {

    private static final TraceComponent tc = Tr.register(TimeoutConfig.class);

    private final AnnotationParameterConfig<Long> valueConfig = getParameterConfig("value", Long.class);
    private final AnnotationParameterConfig<ChronoUnit> unitConfig = getParameterConfig("unit", ChronoUnit.class);

    public TimeoutConfig(Class<?> annotatedClass, Timeout annotation) {
        super(annotatedClass, annotation, Timeout.class);
    }

    public TimeoutConfig(Method annotatedMethod, Class<?> annotatedClass, Timeout annotation) {
        super(annotatedMethod, annotatedClass, annotation, Timeout.class);
    }

    private long value() {
        return valueConfig.getValue();
    }

    private ChronoUnit unit() {
        return unitConfig.getValue();
    }

    /**
     * Validate the Timeout policy to make sure all the parameters e.g. maxRetries, delay, jitter, maxDuration must not be negative.
     */
    @Override
    public void validate() {
        String target = getTargetName();
        //validate the parameters
        if (value() < 0) {
            throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "timeout.parameter.invalid.value.CWMFT5011E", value(), target));
        }

    }

    public TimeoutPolicy generatePolicy() {
        Duration timeoutDuration = Duration.of(value(), unit());
        TimeoutPolicy timeoutPolicy = FaultToleranceProvider.newTimeoutPolicy();
        timeoutPolicy.setTimeout(timeoutDuration);
        return timeoutPolicy;
    }

}
