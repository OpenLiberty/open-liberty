/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.annotation.MinimumJavaLevel;

@RunWith(Suite.class)
@MinimumJavaLevel(javaLevel = 11)
@SuiteClasses({
                ClientWithNoCdi.class, 
                JaxRsIntegration.class,
                JaxRsIntegrationWithConcurrency.class,
                Telemetry10.class,
                TelemetryBeanTest.class,
                TelemetryMultiAppTest.class,
                TelemetrySpiTest.class,
                TelemetryConfigEnvTest.class,
                TelemetryConfigServerVarTest.class,
                TelemetryConfigSystemPropTest.class,
                TelemetryConfigEnvOnlyTest.class,
                TelemetryConfigNullTest.class,
                TelemetryServiceNameTest.class,
                TelemetryShimTest.class,
                TelemetryLoggingExporterTest.class,
                TelemetryAPITest.class,
                MultiThreadedContextTest.class,
                TelemetryMisconfigTest.class,
                TelemetryLongRunningTest.class,
                TelemetryGlobalOpenTelemetryTest.class,
                TelemetryDisabledTest.class,
})

public class FATSuite {
}
