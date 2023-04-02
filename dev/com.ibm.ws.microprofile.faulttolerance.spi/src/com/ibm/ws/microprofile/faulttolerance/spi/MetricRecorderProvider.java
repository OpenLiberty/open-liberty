/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.spi;

import java.lang.reflect.Method;

/**
 * Service which can create a {@link MetricRecorder} for a given method and fault tolerance policy
 */
public interface MetricRecorderProvider {

    static enum AsyncType {
        ASYNC,
        SYNC
    }

    /**
     * Get a metric recorder for invocations of the given method
     */
    public MetricRecorder getMetricRecorder(Method method,
                                            RetryPolicy retryPolicy,
                                            CircuitBreakerPolicy circuitBreakerPolicy,
                                            TimeoutPolicy timeoutPolicy,
                                            BulkheadPolicy bulkheadPolicy,
                                            FallbackPolicy fallbackPolicy,
                                            AsyncType isAsync);
}
