/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.connectionpool.monitor.metrics;

import java.time.Duration;

/**
 * Intended to be a service-component.
 * Implemented by subsequent Metric run-times in their respective bundles.
 */
public interface ConnectionPoolMetricAdapter {

    /**
     * Given the Connection pool's (JNDI) name , update the Connection pool wait time metric of the respective Metrics runtime
     *
     * @param poolName
     * @param duration
     * @param appName  This value can be `null` to indicate that we're updating metrics relating to the server. Not all Implementations may need the appname value.
     */
    public void updateWaitTimeMetrics(String poolName, Duration duration);

    /**
     * Given the Connection pool's (JNDI) name , update the Connection pool in use time metric of the respective Metrics runtime
     *
     * @param poolName
     * @param duration
     * @param appName  This value can be `null` to indicate that we're updating metrics relating to the server. Not all Implementations may need the appname value.
     */
    public void updateInUseTimeMetrics(String poolName, Duration duration);
}
