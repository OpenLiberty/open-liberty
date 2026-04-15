/*******************************************************************************
 * Copyright (c) 2019, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.*;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                HttpAuthenticationMechanismDBTest.class,
                HttpAuthenticationMechanismDBNoUserTest.class,
                HttpAuthenticationMechanismDBAuthAliasTest.class,
                HttpAuthenticationMechanismDBAuthDataTest.class,
                HttpAuthenticationMechanismDBShortNameTest.class,
                HttpAuthenticationMechanismDBHashTest.class,
                HttpAuthenticationMechanismDBHashBeanTest.class,
                HttpAuthenticationMechanismDBHashNoConfigTest.class,
                HttpAuthenticationMechanismDBAnnotationTest.class,
                DatabaseIdentityStoreDeferredSettingsTest.class,
                DatabaseIdentityStoreImmediateSettingsTest.class,
                ProgrammaticTest.class,
                MultipleModuleNoExpandTest.class,
                MultipleModuleExpandTest.class
})

/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

    /*
     * Run EE9 tests in LITE mode and run all tests in FULL mode.
     */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().fullFATOnly()).andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11)).andWith(FeatureReplacementAction.EE10_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17)).andWith(FeatureReplacementAction.EE11_FEATURES());
}
