/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package com.ibm.ws.security.oidc.client.claimPropagation.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.actions.SecurityTestFeatureEE10RepeatAction;
import com.ibm.ws.security.fat.common.actions.SecurityTestFeatureEE9RepeatAction;
import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;
import com.ibm.ws.security.fat.common.utils.ldaputils.CommonLocalLDAPServerSuite;
import com.ibm.ws.security.oidc.client.claimPropagation.fat.OIDC.OIDCBasicIdTokenClaimPropagationTests;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({

        // No OAuth tests - doesn't use ID token

        // specify OIDC tests
        OIDCBasicIdTokenClaimPropagationTests.class,

// specify OIDC with SAML tests - N/A as SAML doesn't have an ID Token

})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite extends CommonLocalLDAPServerSuite {

    /*
     * Run EE9 tests in only FULL mode and run EE7/EE8 tests only in LITE mode.
     *
     * This was done to increase coverage of EE9 while not adding a large amount of test runtime.
     *
     */

    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().liteFATOnly())
            .andWith(new SecurityTestRepeatAction().onlyOnWindows().fullFATOnly())
            .andWith(new SecurityTestFeatureEE9RepeatAction().alwaysAddFeature("servlet-5.0").notOnWindows().fullFATOnly())
            .andWith(new SecurityTestFeatureEE10RepeatAction().alwaysAddFeature("servlet-6.0").notOnWindows().fullFATOnly());

}
