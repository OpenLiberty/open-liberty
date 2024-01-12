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
import com.ibm.ws.security.saml.fat.IDPInitiated.EncryptionIDPInitiatedTests;
import com.ibm.ws.security.saml.fat.IDPInitiated.IDPInitiatedDefaultConfigTests;
import com.ibm.ws.security.saml.fat.IDPInitiated.MultipleSPIDPInitiatedTests;
import com.ibm.ws.security.saml.fat.IDPInitiated.UserFeatureIDPInitiatedTests;
import com.ibm.ws.security.saml.fat.IDPInitiated.UserFeatureOnlyIDPInitiatedTests;
import com.ibm.ws.security.saml.fat.SPInitiated.EncryptionSolicitedSPInitiatedTests;
import com.ibm.ws.security.saml.fat.SPInitiated.EncryptionUnsolicitedSPInitiatedTests;
import com.ibm.ws.security.saml.fat.SPInitiated.MultipleSPSolicitedSPInitiatedTests;
import com.ibm.ws.security.saml.fat.SPInitiated.SolicitedSPInitiatedDefaultConfigTests;
import com.ibm.ws.security.saml.fat.SPInitiated.UserFeatureOnlySolicitedSPInitiatedTests;
import com.ibm.ws.security.saml.fat.SPInitiated.UserFeatureSolicitedSPInitiatedTests;
import com.ibm.ws.security.saml.fat.common.DefaultConfigMissingMetaDataTests;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({

        AlwaysPassesTest.class,

        // running default config tests not possible for unsolicited SP initiated flow
        // as the trigger for that flow is a modified value for loginPageURL
        // TODO - chc - fix shibboleth nameId handling
        IDPInitiatedDefaultConfigTests.class,
        SolicitedSPInitiatedDefaultConfigTests.class,
        DefaultConfigMissingMetaDataTests.class,

        EncryptionIDPInitiatedTests.class,
        EncryptionSolicitedSPInitiatedTests.class,
        EncryptionUnsolicitedSPInitiatedTests.class, // not in lite

        MultipleSPIDPInitiatedTests.class,
        MultipleSPSolicitedSPInitiatedTests.class,
        // TODO MultipleSPUnsolicitedSPInitiatedTests.class,

        UserFeatureIDPInitiatedTests.class,
        UserFeatureOnlyIDPInitiatedTests.class, // not in lite
        UserFeatureSolicitedSPInitiatedTests.class,
        UserFeatureOnlySolicitedSPInitiatedTests.class // not in lite

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
