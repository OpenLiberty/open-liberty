/*******************************************************************************
 * Copyright (c) 2020, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.ws.security.token.ltpa.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                ContextRootCookiePathTests.class,
                FATTest.class,
                LTPAKeyRotationTests.class,
                LTPAValidationKeyTests.class
})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

    /*@formatter:off*/
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(FeatureReplacementAction.NO_REPLACEMENT()
                                                         .fullFATOnly())
                                        .andWith(FeatureReplacementAction.EE9_FEATURES()
                                                         .liteFATOnly()) // Having all repeats in FULL mode causes bucket timeouts.
                                        .andWith(FeatureReplacementAction.EE10_FEATURES()
                                                         .conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                                        .andWith(FeatureReplacementAction.EE11_FEATURES());
    /*@formatter:on*/
}
