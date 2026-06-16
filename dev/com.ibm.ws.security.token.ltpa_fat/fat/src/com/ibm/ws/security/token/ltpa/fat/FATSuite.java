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

import java.util.Locale;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                ContextRootCookiePathTests.class,
                FATTest.class,
                LTPAKeyRotationTests.class,
                LTPAValidationKeyTests.class,
                LTPAKeyPasswordTests.class
})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");

    @ClassRule
    public static RepeatTests repeat;

    /*@formatter:off*/
    static {
        if (isWindows && !FATRunner.FAT_TEST_LOCALRUN) {
            // Only one FULL mode repeat on Windows to avoid bucket timeouts.
            repeat = RepeatTests.with(FeatureReplacementAction.NO_REPLACEMENT()
                                            .fullFATOnly())
                            .andWith(FeatureReplacementAction.EE9_FEATURES()
                                            .liteFATOnly()) // Having all repeats in FULL mode causes bucket timeouts.
                            .andWith(FeatureReplacementAction.EE10_FEATURES()
                                            .liteFATOnly())
                            .andWith(FeatureReplacementAction.EE11_FEATURES()
                                            .liteFATOnly());
        } else {
            repeat = RepeatTests.with(FeatureReplacementAction.NO_REPLACEMENT()
                                            .fullFATOnly())
                            .andWith(FeatureReplacementAction.EE9_FEATURES()
                                            .liteFATOnly()) // Having all repeats in FULL mode causes bucket timeouts.
                            .andWith(FeatureReplacementAction.EE10_FEATURES()
                                            .liteFATOnly())
                            .andWith(FeatureReplacementAction.EE11_FEATURES());
        }
    }
    /*@formatter:on*/

}
