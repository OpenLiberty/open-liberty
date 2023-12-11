/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import io.openliberty.microprofile.telemetry.internal.tests.AgentConfigTest;
import io.openliberty.microprofile.telemetry.internal.tests.AgentConfigMultiAppTest;
import io.openliberty.microprofile.telemetry.internal.tests.AgentTest;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class, //Must keep this test to run something in the Java 6 builds.
                AgentTest.class,
                AgentConfigTest.class,
                AgentConfigMultiAppTest.class,
                CrossFeatureJaegerTest.class,
                CrossFeatureZipkinTest.class,
                JaegerSecureOtelCollectorTest.class,
                JaegerSecureOtlpTest.class,
                JaegerOtlpTest.class,
                JaegerOtelCollectorTest.class,
                JaegerLegacyTest.class,
                TracingNotEnabledTest.class,
                ZipkinOtelCollectorTest.class,
                ZipkinTest.class,
})

@MinimumJavaLevel(javaLevel = 11)
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite extends TestContainerSuite {

    public static RepeatTests allMPRepeats(String serverName) {
        return TelemetryActions.repeat(serverName,
                                       MicroProfileActions.MP61, TelemetryActions.MP14_MPTEL11, TelemetryActions.MP41_MPTEL11, TelemetryActions.MP50_MPTEL11,
                                       MicroProfileActions.MP60);
    }

    public static RepeatTests mp60Repeat(String serverName) {
        return TelemetryActions.repeat(serverName, MicroProfileActions.MP60, TelemetryActions.MP14_MPTEL11, TelemetryActions.MP41_MPTEL11);
    }

    public static RepeatTests telemetry11Repeats(String serverName) {
        return TelemetryActions.repeat(serverName, MicroProfileActions.MP61, TelemetryActions.MP14_MPTEL11);
    }

}
