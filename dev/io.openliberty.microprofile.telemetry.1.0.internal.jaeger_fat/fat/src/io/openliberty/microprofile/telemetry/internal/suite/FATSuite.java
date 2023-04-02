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
import io.openliberty.microprofile.telemetry.internal.tests.AgentConfigTest;
import io.openliberty.microprofile.telemetry.internal.tests.AgentTest;
import io.openliberty.microprofile.telemetry.internal.tests.CrossFeatureTest;
import io.openliberty.microprofile.telemetry.internal.tests.JaegerLegacyTest;
import io.openliberty.microprofile.telemetry.internal.tests.JaegerOltpTest;
import io.openliberty.microprofile.telemetry.internal.tests.TracingNotEnabledTest;
import io.openliberty.microprofile.telemetry.internal.tests.ZipkinTest;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class, // Must keep this test to run something in the Java 6 builds.
                TracingNotEnabledTest.class,
                JaegerOltpTest.class,
                JaegerLegacyTest.class,
                ZipkinTest.class,
                AgentTest.class,
                AgentConfigTest.class,
                CrossFeatureTest.class,
})

@MinimumJavaLevel(javaLevel = 11)
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite extends TestContainerSuite {

}
