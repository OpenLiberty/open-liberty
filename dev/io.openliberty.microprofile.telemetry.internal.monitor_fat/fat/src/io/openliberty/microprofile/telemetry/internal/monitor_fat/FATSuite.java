/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.monitor_fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.containers.TestContainerSuite;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;

import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

@RunWith(Suite.class)
@SuiteClasses({ 
				LibertyMetricsTest.class,
				SessionMetricsTest.class,
				ConnectionPoolMetricsTest.class})
public class FATSuite extends TestContainerSuite {

    public static RepeatTests testRepeatMPTel20(String serverName) {
        return TelemetryActions
                .repeat(serverName, MicroProfileActions.MP70_EE11, MicroProfileActions.MP70_EE10,
                        TelemetryActions.MP50_MPTEL20_JAVA8, TelemetryActions.MP41_MPTEL20, TelemetryActions.MP14_MPTEL20);
    }
}