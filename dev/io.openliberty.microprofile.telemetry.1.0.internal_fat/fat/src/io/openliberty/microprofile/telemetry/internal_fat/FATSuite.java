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
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@MinimumJavaLevel(javaLevel = 11)
@SuiteClasses({
                //ClientWithNoCdi.class,
                JaxRsIntegration.class,
                /*JaxRsIntegrationWithConcurrency.class,
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
                TelemetryServletTest.class,
                TelemetryWithSpanErrorTest.class*/
})

public class FATSuite {

    public static final String BETA_ID = MicroProfileActions.MP61_ID + "_BETA";

    public static RepeatTests aboveMP50Repeats(String serverName) {
<<<<<<< HEAD
        return TelemetryActions
                        .repeat(serverName, TelemetryActions.MP50_MPTEL11, MicroProfileActions.MP60, MicroProfileActions.MP61)
                        .andWith(FeatureReplacementAction.EE10_FEATURES().withBeta().fullFATOnly().withID(BETA_ID));
=======
        return MicroProfileActions
                        .repeat(serverName, TelemetryActions.MP14_MPTEL11);
                        //.andWith(FeatureReplacementAction.EE10_FEATURES().withBeta().fullFATOnly().withID(BETA_ID));
>>>>>>> ca0b8d80ea (Add TelemetryJaxRsProviderRegister)
    }
}
