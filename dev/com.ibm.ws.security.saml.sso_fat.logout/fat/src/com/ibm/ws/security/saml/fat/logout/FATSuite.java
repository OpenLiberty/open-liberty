/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.saml.fat.logout;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.AlwaysRunAndPassTest;
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
        AlwaysRunAndPassTest.class,
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
public class FATSuite {

}
