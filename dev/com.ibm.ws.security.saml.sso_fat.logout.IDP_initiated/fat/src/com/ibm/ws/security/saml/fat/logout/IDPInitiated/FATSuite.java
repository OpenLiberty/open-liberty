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
package com.ibm.ws.security.saml.fat.logout.IDPInitiated;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.AlwaysRunAndPassTest;
import com.ibm.ws.security.saml.fat.logout.IDPInitiated_Login.IDPInitiatedLogin_IDPInitiated_LogoutUrl_LTPA_Tests;
import com.ibm.ws.security.saml.fat.logout.IDPInitiated_Login.IDPInitiatedLogin_IDPInitiated_LogoutUrl_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.SolicitedSPInitiatedLogin_IDPInitiated_LogoutUrl_LTPA_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.SolicitedSPInitiatedLogin_IDPInitiated_LogoutUrl_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.UnsolicitedSPInitiatedLogin_IDPInitiated_LogoutUrl_LTPA_Tests;
import com.ibm.ws.security.saml.fat.logout.SPInitiated_Login.UnsolicitedSPInitiatedLogin_IDPInitiated_LogoutUrl_Tests;

/**
 * Collection of all SAML logout related tests, including SAML Single Logout.
 */
@RunWith(Suite.class)
@SuiteClasses({
        AlwaysRunAndPassTest.class,
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
public class FATSuite {

}
