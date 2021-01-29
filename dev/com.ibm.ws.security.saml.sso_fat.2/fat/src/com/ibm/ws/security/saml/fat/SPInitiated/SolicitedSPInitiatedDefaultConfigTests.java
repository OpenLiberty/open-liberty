/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.fat.SPInitiated;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.saml.fat.common.DefaultConfigTests;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * The testcases in this class run varios tests with the default config.
 * SAML should work with only the feature in the server.xml. These tests
 * ensure that this works as well as testing some dynamic updates to add/remove
 * "details" of SAML config.
 *
 * The tests in this class validate the Solicited SP Iniatited Flow.
 */
@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class SolicitedSPInitiatedDefaultConfigTests extends DefaultConfigTests {

    private static final Class<?> thisClass = SolicitedSPInitiatedDefaultConfigTests.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        flowType = SAMLConstants.SOLICITED_SP_INITIATED;

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        // add any additional messages that you want the "start" to wait for
        // we should wait for any providers that this test requires
        List<String> extraMsgs = getDefaultSAMLStartMsgs();

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SAMLConstants.SAML_CLIENT_APP);

        startSPWithIDPServer("com.ibm.ws.security.saml.sso-2.0_fat.2", "server_defaultConfig.xml", extraMsgs, extraApps, true);

        testSAMLServer.addIgnoredServerException(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES);

    }

    //	@Mode(TestMode.LITE)
    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Test
    public void SolicitedSPInitiatedDefaultConfigTests_defaultConfig_useNonDefaultSP() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();

        updatedTestSettings.updatePartnerInSettings("sp1", true);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlowDefAppAgain, helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings));

    }

}
