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

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.impl.AbstractAnnotationConfig;
import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceProvider;

public class BulkheadConfig extends AbstractAnnotationConfig<Bulkhead> {

    private static final TraceComponent tc = Tr.register(BulkheadConfig.class);

    private final AnnotationParameterConfig<Integer> valueConfig = getParameterConfig("value", Integer.class);
    private final AnnotationParameterConfig<Integer> waitingTaskQueueConfig = getParameterConfig("waitingTaskQueue", Integer.class);

    public BulkheadConfig(Class<?> annotatedClass, Bulkhead annotation) {
        super(annotatedClass, annotation, Bulkhead.class);
    }

    public BulkheadConfig(Method annotatedMethod, Class<?> annotatedClass, Bulkhead annotation) {
        super(annotatedMethod, annotatedClass, annotation, Bulkhead.class);
    }

    private int value() {
        return valueConfig.getValue();
    }

    private int waitingTaskQueue() {
        return waitingTaskQueueConfig.getValue();
    }

    /**
     * Validate Bulkhead configure and make sure the value and waitingTaskQueue must be greater than or equal to 1.
     */
    @Override
    public void validate() {
        //validate the parameters
        String target = getTargetName();
        if (value() < 1) {
            throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "bulkhead.parameter.invalid.value.CWMFT5016E", "value ", value(), target));
        }
        //validate the parameters
        if (waitingTaskQueue() < 1) {
            throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "bulkhead.parameter.invalid.value.CWMFT5016E", "waitingTaskQueue", waitingTaskQueue(),
                                                                         target));
        }

    }

    public BulkheadPolicy generatePolicy() {
        int maxThreads = value();
        int queueSize = waitingTaskQueue();

        BulkheadPolicy bulkheadPolicy = FaultToleranceProvider.newBulkheadPolicy();

        bulkheadPolicy.setMaxThreads(maxThreads);
        bulkheadPolicy.setQueueSize(queueSize);

        return bulkheadPolicy;
    }
}
