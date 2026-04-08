/*******************************************************************************
 * Copyright (c) 2017, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsonp11.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class, // so we report at least 1 test on java 7 builds
                JSONP11Test.class
})
public class FATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification() // run all tests as-is (e.g. EE8 features)
                    .andWith(FeatureReplacementAction.EE9_FEATURES()) // run all tests again with EE9 features+packages
                    .andWith(FeatureReplacementAction.EE10_FEATURES())
                    .andWith(FeatureReplacementAction.EE11_FEATURES());
}
