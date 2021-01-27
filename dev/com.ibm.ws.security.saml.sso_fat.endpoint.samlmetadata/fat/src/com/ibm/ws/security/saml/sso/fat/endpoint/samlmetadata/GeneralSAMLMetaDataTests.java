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
package com.ibm.ws.security.saml.sso.fat.endpoint.samlmetadata;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonMessageTools;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTest;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class GeneralSAMLMetaDataTests extends SAMLCommonTest {

    private static final Class<?> thisClass = GeneralSAMLMetaDataTests.class;
    public static CommonMessageTools msgUtils = new CommonMessageTools();
    List<String> commongAddtlMsgs = new ArrayList<String>();

    // would like to add passwords for keystores and fimuser (Liberty, LibertyServer, fimuser), but
    // they conflict with valid log entries (change Liberty to LibertyPW, LibertyServer to LibertyServerPW, fimuser to fimuserPW, ...)
    static String[] pwList = { "testuserpwd", "user1pwd", "user2pwd", "keyspass" };

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        // add any additional messages that you want the "start" to wait for
        // we should wait for any providers that this test requires
        List<String> extraMsgs = getDefaultSAMLStartMsgs();

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SAMLConstants.SAML_CLIENT_APP);

        // We don't need an IDP server for this test
        copyMetaData = false;
        //testSettings = new SAMLTestSettings();
        testSAMLServer = commonSetUp("com.ibm.ws.security.saml.sso-2.0_fat.endpoint.samlmetadata", "server_orig.xml", SAMLConstants.SAML_ONLY_SETUP, SAMLConstants.SAML_SERVER_TYPE, extraApps, extraMsgs);
        testSAMLServer.setPasswordsUsedWithServer(pwList);

        testSAMLServer.addIgnoredServerException(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES);

    }

    // set metadata expectations
    public List<validationData> setMetaDataTestExpectations(SAMLTestSettings settings, Boolean signOutbound, Boolean signInbound) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodes();
        // this first line calls the method that parses the xml response - it doesn't validate anything, just prints the different parts of the metadata generated
        //expectations = vData.addExpectation(expectations, SAMLConstants.SAML_META_DATA_ENDPOINT, SAMLConstants.SAML_METADATA, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML POST response", null, null);
        expectations = vData.addExpectation(expectations, SAMLConstants.SAML_META_DATA_ENDPOINT, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive correct value for AuthnRequestsSigned", null, "AuthnRequestsSigned=\"" + signOutbound + "\"");
        expectations = vData.addExpectation(expectations, SAMLConstants.SAML_META_DATA_ENDPOINT, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive correct value for WantAssertionsSigned", null, "WantAssertionsSigned=\"" + signInbound + "\"");
        expectations = vData.addExpectation(expectations, SAMLConstants.SAML_META_DATA_ENDPOINT, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive correct value for entityID", null, "entityID=\"" + settings.getSpConsumer() + "\"");
        expectations = vData.addExpectation(expectations, SAMLConstants.SAML_META_DATA_ENDPOINT, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_MATCHES, "Did not receive correct value for ACS", null, "AssertionConsumerService.*Location=\"" + settings.getSpConsumer() + "/acs\"");
        expectations = vData.addExpectation(expectations, SAMLConstants.SAML_META_DATA_ENDPOINT, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_MATCHES, "Did not receive correct value for SLO", null, "SingleLogoutService.*Location=\"" + settings.getSpConsumer() + "/slo\"");

        return expectations;

    }

    /**
     * Validate generated metadata
     */
    @Mode(TestMode.LITE)
    @Test
    public void generalSAMLMetaDataTests_generalValidation() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        genericSAML(_testName, webClient, updatedTestSettings, SAMLConstants.SP_SAMLMETADATAENDPOINT_FLOW, setMetaDataTestExpectations(updatedTestSettings, true, true));
    }

    /**
     * Validate generated metadata after a reconfig - make sure that the endpoint sees the new
     * config settings and records those in the metadata generated
     *
     * @throws Exception
     */
    //	@Mode(TestMode.LITE)
    @Test
    public void generalSAMLMetaDataTests_dynamicUpdate() throws Exception {

        testSAMLServer.reconfigServer("server_signing_false.xml", _testName, commongAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        genericSAML(_testName, webClient, updatedTestSettings, SAMLConstants.SP_SAMLMETADATAENDPOINT_FLOW, setMetaDataTestExpectations(updatedTestSettings, false, false));
    }

}
