/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oidc_social.claimPropagation.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.utils.ldaputils.CommonLocalLDAPServerSuite;
import com.ibm.ws.security.oidc_social.claimPropagation.fat.OIDC.OIDCBasicIdTokenClaimPropagationTestss;
import com.ibm.ws.security.oidc_social.claimPropagation.fat.Social.SocialBasicIdTokenClaimPropagationTestss;

@RunWith(Suite.class)
@SuiteClasses({

        // No OAuth tests - doesn't use ID token

        // specify OIDC tests
        OIDCBasicIdTokenClaimPropagationTestss.class,

        // specify Social client tests
        SocialBasicIdTokenClaimPropagationTestss.class,

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

    //    @ClassRule
    //    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().liteFATOnly())
    //            .andWith(new SecurityTestRepeatAction().onlyOnWindows().fullFATOnly())
    //            .andWith(new SecurityTestFeatureEE9RepeatAction().notOnWindows().fullFATOnly());

}
