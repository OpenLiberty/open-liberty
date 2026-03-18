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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
    private static final Set<String> EE78_FEATURES;
    private static final String[] EE78_FEATURES_ARRAY = {
                                                          "appSecurity-1.0",
                                                          "jsp-2.2",
    };

    private static final Set<String> EE9_FEATURES;
    private static final String[] EE9_FEATURES_ARRAY = {
    };

    private static final Set<String> EE10_FEATURES;
    private static final String[] EE10_FEATURES_ARRAY = {
    };

    private static final Set<String> EE11_FEATURES;
    private static final String[] EE11_FEATURES_ARRAY = {
    };

    static {
        EE78_FEATURES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE78_FEATURES_ARRAY)));
        EE9_FEATURES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE9_FEATURES_ARRAY)));
        EE10_FEATURES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE10_FEATURES_ARRAY)));
        EE11_FEATURES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE11_FEATURES_ARRAY)));
    }

    /*
     * Run EE9 tests in LITE mode if Java 8, EE10 tests in LITE mode if >= Java 11 and run all tests in FULL mode.
     */
    /*@formatter:off*/
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(FeatureReplacementAction.NO_REPLACEMENT()
                                                         .fullFATOnly())
                                        .andWith(FeatureReplacementAction.EE9_FEATURES()
                                                         .removeFeatures(EE78_FEATURES)
                                                         .addFeatures(EE9_FEATURES)
                                                         .conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                                        .andWith(FeatureReplacementAction.EE10_FEATURES()
                                                         .removeFeatures(EE78_FEATURES)
                                                         .removeFeatures(EE9_FEATURES)
                                                         .addFeatures(EE10_FEATURES)
                                                         .conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                                        .andWith(FeatureReplacementAction.EE11_FEATURES()
                                                         .removeFeatures(EE78_FEATURES)
                                                         .removeFeatures(EE9_FEATURES)
                                                         .removeFeatures(EE10_FEATURES)
                                                         .addFeatures(EE11_FEATURES));
    /*@formatter:on*/
}
