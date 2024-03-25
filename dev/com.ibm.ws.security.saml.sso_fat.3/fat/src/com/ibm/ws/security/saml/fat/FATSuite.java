/*******************************************************************************
 * Copyright (c) 2014, 2024 IBM Corporation and others.
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

package com.ibm.ws.security.saml.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.actions.LargeProjectRepeatActions;
import com.ibm.ws.security.fat.common.utils.ldaputils.CommonLocalLDAPServerSuite;
import com.ibm.ws.security.saml.fat.IDPInitiated.PkixIDPInitiatedTests;
import com.ibm.ws.security.saml.fat.IDPInitiated.TimeIDPInitiatedTests;
import com.ibm.ws.security.saml.fat.IDPInitiated.TrustedIssuerIDPInitiatedTests;
import com.ibm.ws.security.saml.fat.SPInitiated.PkixSolicitedSPInitiatedTests;
import com.ibm.ws.security.saml.fat.SPInitiated.PkixUnsolicitedSPInitiatedTests;
import com.ibm.ws.security.saml.fat.SPInitiated.TimeSolicitedSPInitiatedTests;
import com.ibm.ws.security.saml.fat.SPInitiated.TimeUnsolicitedSPInitiatedTests;
import com.ibm.ws.security.saml.fat.SPInitiated.TrustedIssuerSolicitedSPInitiatedTests;
import com.ibm.ws.security.saml.fat.SPInitiated.TrustedIssuerUnsolicitedSPInitiatedTests;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({

        AlwaysPassesTest.class,

        TimeIDPInitiatedTests.class,
        TimeSolicitedSPInitiatedTests.class,
        TimeUnsolicitedSPInitiatedTests.class, // no lite
        PkixIDPInitiatedTests.class,
        PkixSolicitedSPInitiatedTests.class,
        PkixUnsolicitedSPInitiatedTests.class, // no lite
        TrustedIssuerIDPInitiatedTests.class,
        TrustedIssuerSolicitedSPInitiatedTests.class,
        TrustedIssuerUnsolicitedSPInitiatedTests.class // no lite

})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
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
