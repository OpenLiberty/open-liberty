/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.config_fat;

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
                TelemetryConfigEnvTest.class,
                TelemetryConfigServerVarTest.class,
                TelemetryConfigSystemPropTest.class,
                TelemetryConfigEnvOnlyTest.class,
                TelemetryConfigNullTest.class,
                TelemetryConfigRuntimeModeIgnoresMPConfigTest.class,
                TelemetryDisabledProvidersTest.class,
                TelemetryDisabledTest.class,
                TelemetryLoggingExporterTest.class,
                TelemetryMisconfigTest.class,
                TelemetrySpiTest.class,

})
public class FATSuite {

    public static String getTelemetryVersionUnderTest() {
        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            return "1.0";
        } else if (RepeatTestFilter.isAnyRepeatActionActive(MicroProfileActions.MP70_EE11_ID, MicroProfileActions.MP70_EE10_ID, TelemetryActions.MP61_MPTEL20_ID,
                                                            TelemetryActions.MP50_MPTEL20_JAVA8_ID,
                                                            TelemetryActions.MP50_MPTEL20_ID, TelemetryActions.MP41_MPTEL20_ID, TelemetryActions.MP14_MPTEL20_ID)) {
            return "2.0";
        } else if (RepeatTestFilter.isAnyRepeatActionActive(MicroProfileActions.MP71_EE11_ID, MicroProfileActions.MP71_EE10_ID, TelemetryActions.MP50_MPTEL21_JAVA8_ID,
                                                            TelemetryActions.MP50_MPTEL21_ID, TelemetryActions.MP41_MPTEL21_ID, TelemetryActions.MP14_MPTEL21_ID)) {
            return "2.1";
        } else {
            return "1.1";
        }
    }
}
