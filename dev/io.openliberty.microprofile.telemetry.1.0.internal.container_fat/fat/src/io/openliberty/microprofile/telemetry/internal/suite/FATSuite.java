/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.AlwaysPassesTest;
import io.openliberty.microprofile.telemetry.internal.tests.Agent129Test;
import io.openliberty.microprofile.telemetry.internal.tests.Agent250Test;
import io.openliberty.microprofile.telemetry.internal.tests.AgentConfigMultiAppTest;
import io.openliberty.microprofile.telemetry.internal.tests.AgentConfigTest;
import io.openliberty.microprofile.telemetry.internal.tests.AgentTest;
import io.openliberty.microprofile.telemetry.internal.tests.CrossFeatureJaegerTest;
import io.openliberty.microprofile.telemetry.internal.tests.CrossFeatureZipkinTest;
import io.openliberty.microprofile.telemetry.internal.tests.JaegerLegacyTest;
import io.openliberty.microprofile.telemetry.internal.tests.JaegerOtelCollectorTest;
import io.openliberty.microprofile.telemetry.internal.tests.JaegerOtlpTest;
import io.openliberty.microprofile.telemetry.internal.tests.JaegerSecureOtelCollectorTest;
import io.openliberty.microprofile.telemetry.internal.tests.JaegerSecureOtlpTest;
import io.openliberty.microprofile.telemetry.internal.tests.TracingNotEnabledTest;
import io.openliberty.microprofile.telemetry.internal.tests.ZipkinOtelCollectorTest;
import io.openliberty.microprofile.telemetry.internal.tests.ZipkinTest;
import io.openliberty.microprofile.telemetry.internal.tests.JvmMetricsOtelCollectorTest;
import io.openliberty.microprofile.telemetry.internal.tests.MetricsApiOtelCollectorTest;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class, //Must keep this test to run something in the Java 6 builds.
                AgentTest.class,
                Agent129Test.class,
                Agent250Test.class,
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
                JvmMetricsOtelCollectorTest.class,
                MetricsApiOtelCollectorTest.class,
                ZipkinOtelCollectorTest.class,
                ZipkinTest.class,

})

/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite extends TestContainerSuite {

}