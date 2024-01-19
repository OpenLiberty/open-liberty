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
package com.ibm.ws.security.saml.fat.logout.IDPInitiated;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.actions.LargeProjectRepeatActions;
import com.ibm.ws.security.fat.common.utils.ldaputils.CommonLocalLDAPServerSuite;
import com.ibm.ws.security.saml.fat.logout.IDPInitiated_Login.IDPInitiatedLogin_IDPInitiated_LogoutUrl_LTPA_Tests;
import com.ibm.ws.security.saml.fat.logout.IDPInitiated_Login.IDPInitiatedLogin_IDPInitiated_LogoutUrl_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.SolicitedSPInitiatedLogin_IDPInitiated_LogoutUrl_LTPA_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.SolicitedSPInitiatedLogin_IDPInitiated_LogoutUrl_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.UnsolicitedSPInitiatedLogin_IDPInitiated_LogoutUrl_LTPA_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.UnsolicitedSPInitiatedLogin_IDPInitiated_LogoutUrl_Tests;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.RepeatTests;

/**
 * Collection of all SAML logout related tests, including SAML Single Logout.
 */
@RunWith(Suite.class)
@SuiteClasses({
        AlwaysPassesTest.class,
        // login using each of the 3 flows, use the IDP logout url to logout to do an IDP initiated Logout
        // Using SP Cookies
        IDPInitiatedLogin_IDPInitiated_LogoutUrl_Tests.class,
        SolicitedSPInitiatedLogin_IDPInitiated_LogoutUrl_Tests.class,
        UnsolicitedSPInitiatedLogin_IDPInitiated_LogoutUrl_Tests.class,

        // Using LTPA Cookies
        IDPInitiatedLogin_IDPInitiated_LogoutUrl_LTPA_Tests.class,
        SolicitedSPInitiatedLogin_IDPInitiated_LogoutUrl_LTPA_Tests.class,
        UnsolicitedSPInitiatedLogin_IDPInitiated_LogoutUrl_LTPA_Tests.class,

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
