/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
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
package com.ibm.ws.example;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                SimpleTest.class,
})
public class FATSuite {

    // Using the RepeatTests @ClassRule will cause all tests to be run five times.
    // 1) [FULL mode only] without any modifications
    // 2) [FULL mode only] again with all features upgraded to their EE8 equivalents
    // 3) [FULL mode if Java 11 or later, else LITE mode] again with all features _and applications_ upgrade to Jakarta EE 9 equivalents
    // 4) [FULL mode if Java 17 or later, else LITE mode] again with all features _and applications_ upgrade to Jakarta EE 10 equivalents
    // 5) [LITE mode if Java 17 or later] again with all features _and applications_ upgrade to Jakarta EE 11 equivalents
    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.NO_REPLACEMENT().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                    .andWith(FeatureReplacementAction.EE10_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                    .andWith(FeatureReplacementAction.EE11_FEATURES());

}
