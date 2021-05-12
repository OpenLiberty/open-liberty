/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.social.fat.LibertyOP;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                LibertyOP_BasicTests_oauth_usingSocialConfig.class,
                LibertyOP_BasicTests_oidc_usingSocialConfig.class,
                LibertyOP_BasicConfigTests_oauth_usingSocialConfig.class,
                LibertyOP_BasicConfigTests_oidc_usingSocialConfig.class,
                LibertyOP_BasicConfigTests_oauth_usingSocialConfig_noServerSSL.class,
                LibertyOP_BasicConfigTests_oidc_usingSocialConfig_noServerSSL.class,
                LibertyOP_BasicTests_oidc_usingSocialDiscoveryConfig.class,
                LibertyOP_BasicConfigTests_oidc_usingSocialDiscoveryConfig.class,
                LibertyOP_ErrorConfigTests_oidc_usingSocialDiscoveryConfig.class,
                LibertyOP_Social_SamesiteTests_oauth_usingSocialConfig.class,
                LibertyOP_Social_SamesiteTests_oidc_usingSocialConfig.class

})

/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

    public static String UserApiEndpoint = Constants.USERINFO_ENDPOINT;

    /*
     * Run EE9 tests in LITE mode and run all tests in FULL mode.
     */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().fullFATOnly()).andWith(new JakartaEE9Action());
}
