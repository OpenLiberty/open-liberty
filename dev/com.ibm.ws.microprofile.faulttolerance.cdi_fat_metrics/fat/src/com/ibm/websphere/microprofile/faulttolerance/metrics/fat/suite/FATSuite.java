/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance.metrics.fat.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.CDIFallbackTest;
import com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.MetricRemovalTest;
import com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.circuitbreaker.CircuitBreakerMetricTest;

@RunWith(Suite.class)
@SuiteClasses({
                CDIFallbackTest.class,
                MetricRemovalTest.class,
                CircuitBreakerMetricTest.class,
})
public class FATSuite {

}
