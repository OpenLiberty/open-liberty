/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.logging.internal.container.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

@RunWith(Suite.class)
@SuiteClasses({
                LoggingServletTest.class,
                JULDuplicateTest.class,
                JULLogServletTest.class
})

public class FATSuite extends TestContainerSuite {

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(FeatureReplacementAction.ALL_SERVERS,
                                                             TestMode.FULL,
                                                             MicroProfileActions.MP70_EE11,
                                                             TelemetryActions.MP14_MPTEL20,
                                                             TelemetryActions.MP41_MPTEL20,
                                                             TelemetryActions.MP50_MPTEL20,
                                                             TelemetryActions.MP50_MPTEL20_JAVA8,
                                                             MicroProfileActions.MP70_EE10);
}
