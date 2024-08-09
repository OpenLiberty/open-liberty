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

import componenttest.annotation.MinimumJavaLevel;
import componenttest.containers.TestContainerSuite;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@MinimumJavaLevel(javaLevel = 11)
@SuiteClasses({ 
				LibertyMetricsTest.class,
				SessionMetricsTest.class,
				ConnectionPoolMetricsTest.class})
public class FATSuite extends TestContainerSuite {
	
    public static RepeatTests testRepeatMPTel20(String serverName) {
        return RepeatTests.with(FeatureReplacementAction.EE11_FEATURES())
                        .andWith(FeatureReplacementAction.EE10_FEATURES().fullFATOnly())
                        .andWith(FeatureReplacementAction.EE9_FEATURES().fullFATOnly())
                        .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly())
                        .andWith(FeatureReplacementAction.EE7_FEATURES().fullFATOnly());

    }

}