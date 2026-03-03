/*******************************************************************************
 * Copyright (c) 2015, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.security.auth.data.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                DefaultPrincipalMappingTest.class,
                DefaultPrincipalMappingPassUtils11Test.class,
                SingleFeatureDefaultPrincipalMappingTest.class,
                SingleFeatureDefaultPrincipalMappingPassUtils11Test.class,
                Java2SecurityTest.class,
                Java2SecurityPassUtils11Test.class
})
public class FATSuite {

    /*
     * Run EE9 tests in LITE mode if Java 8, EE10 tests in LITE mode if >= Java 11, EE11 tests in LITE mode if >= Java 17 and run all tests in FULL mode.
     */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE9_FEATURES()
                                    .removeFeature("appSecurity-1.0")
                                    .addFeature("appSecurity-4.0")
                                    .conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                    .andWith(FeatureReplacementAction.EE10_FEATURES()
                                    .removeFeature("appSecurity-1.0")
                                    .removeFeature("appSecurity-4.0")
                                    .addFeature("appSecurity-5.0")
                                    .conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                    .andWith(FeatureReplacementAction.EE11_FEATURES()
                                    .removeFeature("appSecurity-1.0")
                                    .removeFeature("appSecurity-4.0")
                                    .removeFeature("appSecurity-5.0")
                                    .addFeature("appSecurity-6.0"));
}
