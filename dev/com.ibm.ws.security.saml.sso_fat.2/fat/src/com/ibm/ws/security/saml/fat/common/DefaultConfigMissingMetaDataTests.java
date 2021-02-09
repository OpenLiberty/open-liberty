/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.fat.common;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTest;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * The testcases in this class run varios tests with the default config.
 * SAML should work with only the feature in the server.xml. These tests
 * ensure that this works as well as testing some dynamic updates to add/remove
 * "details" of SAML config.
 */
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class DefaultConfigMissingMetaDataTests extends SAMLCommonTest {

    private static final Class<?> thisClass = DefaultConfigMissingMetaDataTests.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        // add any additional messages that you want the "start" to wait for
        // we should wait for any providers that this test requires
        List<String> extraMsgs = getDefaultSAMLStartMsgs();

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SAMLConstants.SAML_CLIENT_APP);

        startSPWithIDPServer("com.ibm.ws.security.saml.sso-2.0_fat.2.missingMetaData", "server_defaultConfig.xml", extraMsgs, extraApps, false);

        testSAMLServer.addIgnoredServerException(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES);

    }

    @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void testSaml_defaultConfig_IDPInitiated_missingIDPMetaDataFile() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not fail to find a valid identity provider", SAMLMessageConstants.CWWKS5045E_INVALID_ISSUER);

        genericSAML(_testName, webClient, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);

    }

    @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void testSaml_defaultConfig_SolicitedSPInitiated_missingIDPMetaDataFile() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not fail to find a valid identity provider", SAMLMessageConstants.CWWKS5079E_CANNOT_FIND_IDP_URL_IN_METATDATA);

        genericSAML(_testName, webClient, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP, expectations);

    }

}
