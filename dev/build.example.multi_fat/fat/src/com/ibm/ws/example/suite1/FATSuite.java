/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.example.suite1;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.example.SimpleTest;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                SimpleTest.class,
})
public class FATSuite {

    // Using the RepeatTests @ClassRule will cause all tests to be run three times.
    // 1) [FULL mode only] without any modifications
    // 2) [FULL mode] again with all features upgraded to their EE8 equivalents
    // 3) [LITE mode] again with all features _and applications_ upgraded to Jakarta EE 9 equivalents
    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.NO_REPLACEMENT().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE9_FEATURES());

}
