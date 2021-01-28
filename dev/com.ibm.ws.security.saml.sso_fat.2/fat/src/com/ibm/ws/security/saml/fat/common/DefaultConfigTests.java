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

import java.util.List;

import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTest;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

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
public class DefaultConfigTests extends SAMLCommonTest {

    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Test
    public void testSaml_defaultConfig() throws Exception {

        //        WebClient webClient = getWebClient(true);
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        //		updatedTestSettings.updatePartnerInSettings("sp1", true) ;

        genericSAML(_testName, webClient, updatedTestSettings, standardFlowDefAppAgain, helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings));

    }

    // testSaml_defaultConfig_useNonDefaultSP moved to the IDP and SP specific tests as the behavior will be different for each.

    @Mode(TestMode.LITE)
    @Test
    public void testSaml_defaultConfig_noDefaultSSLConfig() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        testSAMLServer.reconfigServer("server_defaultConfig_noDefaultSSLConfig.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String failurePoint = helpers.getLastEntryInList(problemWithMetaDataTerminator); // the last step in the actions is where the failure will be
        List<validationData> expectations = vData.addSuccessStatusCodes();
        String expectedExceptionPattern = "org.apache.http.conn.HttpHostConnectException";
        expectations = vData.addExpectation(expectations, failurePoint, SAMLConstants.EXCEPTION_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not find expected exception content [" + expectedExceptionPattern + "].", null, expectedExceptionPattern);

        genericSAML(_testName, webClient, updatedTestSettings, problemWithMetaDataTerminator, expectations);
    }

}
