/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.suite;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.FeatureSet;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import io.openliberty.microprofile.telemetry.internal.tests.AutoInstrumentationTest;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class, // Must keep this test to run something in the Java 6 builds.
                AutoInstrumentationTest.class
})

/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite extends TestContainerSuite {

    private static final FeatureSet MP50_MPTELEMETRY = MicroProfileActions.MP50.addFeature("mpTelemetry-1.0").build(MicroProfileActions.MP50_ID + "_MPTELEMETRY");

    @ClassRule
    public static final RepeatTests r = MicroProfileActions.repeat(null, MicroProfileActions.MP60, MP50_MPTELEMETRY);

}
