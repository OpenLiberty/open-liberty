/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.builder;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.actions.LargeProjectRepeatActions;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
        // Ported list of tests (some already renamed)
        AlwaysPassesTest.class,

        // Basic Functional tests
        JwtBuilderApiBasicTests.class,
        JwtBuilderApiWithLDAPBasicTests.class,
        JwkEndpointValidationUrlTests.class,

        // Configuration Tests
        JwtBuilderAPIConfigTests.class,
        JwtBuilderAPIConfigNoIdTests.class,
        JwtBuilderAPIConfigAltKeyStoreTests.class,
        JwtBuilderAPIWithLDAPConfigTests.class,
        JwtBuilderAPIMinimumConfigTests.class,
        JwtBuilderAPIMinimumRunnableConfigTests.class,
        JwtBuilderAPIMinimumSSLConfigGlobalTests.class,
        JwtBuilderAPIMinimumSSLConfigBuilderTests.class

})

public class FATSuite {

    public static boolean runAsCollection = false;

    /*
     * Run EE9 and EE10 tests in LITE mode (but not on Windows) and run all tests in FULL mode.
     */
    @ClassRule
    public static RepeatTests repeat = LargeProjectRepeatActions.createEE9OrEE10Repeats(null, null, null, null, "publish/servers", "publish/shared/config");

}
