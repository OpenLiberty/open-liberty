/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
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

package com.ibm.ws.security.saml.fat.logout;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.actions.LargeProjectRepeatActions;
import com.ibm.ws.security.fat.common.utils.ldaputils.CommonLocalLDAPServerSuite;
import com.ibm.ws.security.saml.fat.logout.IDPInitiated_Login.IDPInitiatedLogin_2ServerLogout_usingApps_Tests;
import com.ibm.ws.security.saml.fat.logout.IDPInitiated_Login.IDPInitiatedLogin_2ServerLogout_usingServlets_Tests;
import com.ibm.ws.security.saml.fat.logout.IDPInitiated_Login.IDPInitiatedLogin_Timeout_usingApps_Tests;
import com.ibm.ws.security.saml.fat.logout.IDPInitiated_Login.IDPInitiatedLogin_Timeout_usingServlets_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.SolicitedSPInitiatedLogin_2ServerLogout_usingApps_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.SolicitedSPInitiatedLogin_2ServerLogout_usingServlets_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.SolicitedSPInitiatedLogin_Timeout_usingApps_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.SolicitedSPInitiatedLogin_Timeout_usingServlets_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.UnsolicitedSPInitiatedLogin_2ServerLogout_usingApps_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.UnsolicitedSPInitiatedLogin_2ServerLogout_usingServlets_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.UnsolicitedSPInitiatedLogin_Timeout_usingApps_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.UnsolicitedSPInitiatedLogin_Timeout_usingServlets_Tests;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.RepeatTests;

/**
 * Collection of all SAML logout related tests, including SAML Single Logout.
 */
@RunWith(Suite.class)
@SuiteClasses({

        // ***********************************************************************
        // There are many more test classes in this project than we have
        // included in this FATSuite.
        // The tests that are NOT listed here are used (and included in the FATSuite
        // of other projects)
        // The projects that use the other test classes are:
        //   com.ibm.ws.security.saml.sso_fat.logout.httpServletRequest
        //   com.ibm.ws.security.saml.sso_fat.logout.ibm_security_logout
        //   com.ibm.ws.security.saml.sso_fat.logout.IDP_initiated
        AlwaysPassesTest.class,
        // 2 server tests
        IDPInitiatedLogin_2ServerLogout_usingServlets_Tests.class,
        IDPInitiatedLogin_2ServerLogout_usingApps_Tests.class,
        SolicitedSPInitiatedLogin_2ServerLogout_usingServlets_Tests.class,
        SolicitedSPInitiatedLogin_2ServerLogout_usingApps_Tests.class,
        UnsolicitedSPInitiatedLogin_2ServerLogout_usingServlets_Tests.class,
        UnsolicitedSPInitiatedLogin_2ServerLogout_usingApps_Tests.class,

        // Timeout tests
        IDPInitiatedLogin_Timeout_usingServlets_Tests.class,
        IDPInitiatedLogin_Timeout_usingApps_Tests.class,
        SolicitedSPInitiatedLogin_Timeout_usingServlets_Tests.class,
        SolicitedSPInitiatedLogin_Timeout_usingApps_Tests.class,
        UnsolicitedSPInitiatedLogin_Timeout_usingServlets_Tests.class,
        UnsolicitedSPInitiatedLogin_Timeout_usingApps_Tests.class,

})
public class FATSuite extends CommonLocalLDAPServerSuite {

    /*
     * On Windows, always run the default/empty/EE7/EE8 tests.
     * On other Platforms:
     * - if Java 8, run default/empty/EE7/EE8 tests.
     * - All other Java versions
     * -- If LITE mode, run EE9
     * -- If FULL mode, run EE10
     *
     */
    @ClassRule
    public static RepeatTests repeat = LargeProjectRepeatActions.createEE9OrEE10SamlRepeats();

}
