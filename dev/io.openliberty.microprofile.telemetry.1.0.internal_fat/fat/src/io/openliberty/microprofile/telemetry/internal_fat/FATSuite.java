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
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
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
                TelemetryServiceNameTest.class,
                TelemetryShimTest.class,
                TelemetryLoggingExporterTest.class,
                TelemetryAPITest.class,
                MultiThreadedContextTest.class,
                TelemetryMisconfigTest.class,
                TelemetryLongRunningTest.class,
                TelemetryGlobalOpenTelemetryTest.class,
                TelemetryDisabledTest.class,
                TelemetryServletTest.class,
                TelemetryWithSpanErrorTest.class
})
public class FATSuite {

    public static RepeatTests allMPRepeats(String serverName) {
        return TelemetryActions
                        .repeat(serverName, MicroProfileActions.MP61, TelemetryActions.MP14_MPTEL11, TelemetryActions.MP41_MPTEL11, TelemetryActions.MP50_MPTEL11,
                                MicroProfileActions.MP60);
    }

    public static String getTelemetryVersionUnderTest() {
        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            return "1.0";
        } else {
            return "1.1";
        }
    }
}
