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
package com.ibm.ws.security.saml.fat.logout.ibmSecurityLogout;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.actions.LargeProjectRepeatActions;
import com.ibm.ws.security.fat.common.utils.ldaputils.CommonLocalLDAPServerSuite;
import com.ibm.ws.security.saml.fat.logout.IDPInitiated_Login.IDPInitiatedLogin_ibmSecurityLogout_spLogoutFalse_LTPA_Tests;
import com.ibm.ws.security.saml.fat.logout.IDPInitiated_Login.IDPInitiatedLogin_ibmSecurityLogout_spLogoutFalse_Tests;
import com.ibm.ws.security.saml.fat.logout.IDPInitiated_Login.IDPInitiatedLogin_ibmSecurityLogout_spLogoutTrue_LTPA_Tests;
import com.ibm.ws.security.saml.fat.logout.IDPInitiated_Login.IDPInitiatedLogin_ibmSecurityLogout_spLogoutTrue_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.SolicitedSPInitiatedLogin_ibmSecurityLogout_spLogoutFalse_LTPA_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.SolicitedSPInitiatedLogin_ibmSecurityLogout_spLogoutFalse_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.SolicitedSPInitiatedLogin_ibmSecurityLogout_spLogoutTrue_LTPA_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.SolicitedSPInitiatedLogin_ibmSecurityLogout_spLogoutTrue_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.UnsolicitedSPInitiatedLogin_ibmSecurityLogout_spLogoutFalse_LTPA_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.UnsolicitedSPInitiatedLogin_ibmSecurityLogout_spLogoutFalse_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.UnsolicitedSPInitiatedLogin_ibmSecurityLogout_spLogoutTrue_LTPA_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.UnsolicitedSPInitiatedLogin_ibmSecurityLogout_spLogoutTrue_Tests;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.RepeatTests;

/**
 * Collection of all SAML logout related tests, including SAML Single Logout.
 */
@RunWith(Suite.class)
@SuiteClasses({
        AlwaysPassesTest.class,
        // login using each of the 3 flows, use ibm_security_logout with spLogout set to false (implying that we'll just logout locally)
        // SP Cookie
        IDPInitiatedLogin_ibmSecurityLogout_spLogoutFalse_Tests.class,
        SolicitedSPInitiatedLogin_ibmSecurityLogout_spLogoutFalse_Tests.class,
        UnsolicitedSPInitiatedLogin_ibmSecurityLogout_spLogoutFalse_Tests.class,

        // login using each of the 3 flows, use ibm_security_logout with spLogout set to true (implying that we'll also call out to the IDP)
        // SP Cookie
        IDPInitiatedLogin_ibmSecurityLogout_spLogoutTrue_Tests.class,
        SolicitedSPInitiatedLogin_ibmSecurityLogout_spLogoutTrue_Tests.class,
        UnsolicitedSPInitiatedLogin_ibmSecurityLogout_spLogoutTrue_Tests.class,

        // login using each of the 3 flows, use ibm_security_logout with spLogout set to false (implying that we'll just logout locally)
        // LTPA Token/Cookie
        IDPInitiatedLogin_ibmSecurityLogout_spLogoutFalse_LTPA_Tests.class,
        SolicitedSPInitiatedLogin_ibmSecurityLogout_spLogoutFalse_LTPA_Tests.class,
        UnsolicitedSPInitiatedLogin_ibmSecurityLogout_spLogoutFalse_LTPA_Tests.class,

        // login using each of the 3 flows, use ibm_security_logout with spLogout set to true (implying that we'll also call out to the IDP)
        // LTPA Token/Cookie
        IDPInitiatedLogin_ibmSecurityLogout_spLogoutTrue_LTPA_Tests.class,
        SolicitedSPInitiatedLogin_ibmSecurityLogout_spLogoutTrue_LTPA_Tests.class,
        UnsolicitedSPInitiatedLogin_ibmSecurityLogout_spLogoutTrue_LTPA_Tests.class,

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
