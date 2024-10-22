/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
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
package com.ibm.websphere.microprofile.faulttolerance.metrics.fat.suite;

import com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.CDIFallbackTest;
import com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.MetricRemovalTest;
import com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.circuitbreaker.CircuitBreakerMetricTest;
import com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.TelemetryMetricIsolationTest;
import com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.TelemetryMetricCombinationTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                CDIFallbackTest.class,
                MetricRemovalTest.class,
                CircuitBreakerMetricTest.class,
                TelemetryMetricIsolationTest.class,
                TelemetryMetricCombinationTest.class,
})
public class FATSuite {

}
