/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
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
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.MicroProfileActions;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

@RunWith(Suite.class)
@MinimumJavaLevel(javaLevel = 11)
@SuiteClasses({
                ClientWithNoCdi.class,
                JaxRsIntegration.class,
                JaxRsIntegrationWithConcurrency.class,
                ReactiveMessageThatTriggersClientTest.class,
                Telemetry10.class,
                TelemetryAloneTest.class,
                TelemetryBeanTest.class,
                TelemetryMultiAppTest.class,
                TelemetrySpiTest.class,
                TelemetryConfigEnvTest.class,
                TelemetryConfigServerVarTest.class,
                TelemetryConfigSystemPropTest.class,
                TelemetryConfigEnvOnlyTest.class,
                TelemetryConfigNullTest.class,
                TelemetryConfigRuntimeModeIgnoresMPConfigTest.class,
                TelemetryDisabledProvidersTest.class,
                TelemetryServiceNameTest.class,
                TelemetryShimTest.class,
                TelemetryLoggingExporterTest.class,
                TelemetryAPITest.class,
                MultiThreadedContextTest.class,
                TelemetryMisconfigTest.class,
                TelemetryMultipleMetricsTest.class,
                TelemetryLongRunningTest.class,
                TelemetryGlobalOpenTelemetryTest.class,
                TelemetryDisabledTest.class,
                TelemetryServletTest.class,
                TelemetryUserFeatureTest.class,
                TelemetryUserFeatureAppScopedTest.class,
                TelemetryWithSpanErrorTest.class,
                TelemetryAttributesTest.class,
                TelemetryRuntimeInstanceTest.class,

})
public class FATSuite {

    public static String getTelemetryVersionUnderTest() {
        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            return "1.0";
        } else if (RepeatTestFilter.isAnyRepeatActionActive(MicroProfileActions.MP70_EE11_ID, MicroProfileActions.MP70_EE10_ID, TelemetryActions.MP61_MPTEL20_ID,
                                                            TelemetryActions.MP50_MPTEL20_ID, TelemetryActions.MP41_MPTEL20_ID, TelemetryActions.MP14_MPTEL20_ID)) {
            return "2.0";
        } else {
            return "1.1";
        }
    }
}
