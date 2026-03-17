/*******************************************************************************
 * Copyright (c) 2012, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.app.manager.wab.installer.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

/**
 * Collection of all example tests
 */
@RunWith(Suite.class)
/*
 * The classes specified in the @SuiteClasses annotation
 * below should represent all of the test cases for this FAT.
 */
@SuiteClasses({ ConfigurableWABTests.class, WabStartDelayTests.class, WabAdditionalTests.class })
public class FATSuite {
    /*@formatter:off*/
    @ClassRule
    // run tests as-is and again with EE9 and EE10 features+packages
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(FeatureReplacementAction.EE9_FEATURES()
                                    .fullFATOnly())
                    .andWith(FeatureReplacementAction.EE10_FEATURES()
                                    .alwaysAddFeature("servlet-6.0")
                                    .fullFATOnly())
                    .andWith(FeatureReplacementAction.EE11_FEATURES()
                                    .alwaysAddFeature("servlet-6.1")
                                    .fullFATOnly());
    /*@formatter:on*/

}
