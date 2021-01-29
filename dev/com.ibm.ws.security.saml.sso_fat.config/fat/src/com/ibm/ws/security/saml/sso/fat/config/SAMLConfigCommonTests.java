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
package com.ibm.ws.security.saml.sso.fat.config;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTest;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@Mode(TestMode.FULL)
public class SAMLConfigCommonTests extends SAMLCommonTest {

    // TODO - Product not fully baked yet - keyStoreRef (with encryption in
    // mind), keyPassword, trustStoreRef, authnContextClassRef,
    // tokenReplayTimeout
    /**
     * The tests in the class will ensure the proper function/handling of SAML
     * configuration. It will ensure the proper handling of all settings. The
     * tests will make sure that we get the proper error messages and ensure
     * that these messages are appropriate for the audience that receives them.
     * These tests will need to use different partners and federations on the
     * IDP servers that we test with.
     */

    private static final Class<?> thisClass = SAMLConfigCommonTests.class;
    private final Boolean runSolicitedSPInitiatedTests = true;
    private final Boolean runUnsolicitedSPInitiatedTests = true;
    private final Boolean runIDPInitiatedTests = true;
    protected String testServerConfigFile = null;

    List<String> commonAddtlMsgs = new ArrayList<String>();;
    // would like to add passwords for keystores and fimuser (Liberty,
    // LibertyServer, fimuser), but
    // they conflict with valid log entries (change Liberty to LibertyPW,
    // LibertyServer to LibertyServerPW, fimuser to fimuserPW, ...)
    static String[] pwList = { "testuserpwd", "user1pwd", "user2pwd", "keyspass", "samlSPPass", "invalidPassword" };

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        // add any additional messages that you want the "start" to wait for
        // we should wait for any providers that this test requires
        List<String> extraMsgs = getDefaultSAMLStartMsgs();

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SAMLConstants.SAML_CLIENT_APP);

        setDefaultConfigSettings();

        startSPWithIDPServer("com.ibm.ws.security.saml.sso-2.0_fat.config", "server_orig.xml", extraMsgs, extraApps, true);

        // comment out second server - forceAuthn test may need a second server
        // - other tests do NOT need 2 servers at this time
        // copyMetaData = false ;
        // testSAMLServer2 =
        // commonSetUp("com.ibm.ws.security.saml.sso-2.0_fat.config.2",
        // "server_orig.xml", SAMLConstants.SAML_ONLY_SETUP,
        // SAMLConstants.SAML_SERVER_TYPE, extraApps, extraMsgs);

        testSAMLServer.setPasswordsUsedWithServer(pwList);
        testSAMLServer.addIgnoredServerException(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES);

    }

    @Before
    public void beforeTest() {
        testServerConfigFile = "server_" + testName.getMethodName() + ".xml";
    }

    private static void setDefaultConfigSettings() {

        //        String serverRoot = testSAMLServer.getServer().getServerRoot();
        String serverRoot = "${server.config.dir}";

        baseSamlServerConfig = "server_orig.xml.base";

        samlConfigSettings.setFeatureFile(serverRoot + "/imports/saml_only_features.xml");
        samlConfigSettings.setSSLConfigSettingsFile(serverRoot + "/imports/goodSSLSettings.xml");

        samlConfigSettings.setRegistryFiles(serverRoot + "/imports/BasicRegistry.xml");
        samlConfigSettings.setApplicationFiles(serverRoot + "/imports/samlTestApplication.xml");
        samlConfigSettings.setMiscFile(serverRoot + "/imports/misc.xml");

        samlConfigSettings.setDefaultSamlProviderAndAuthFilters();
    }

    // Convenience methods to allow us to write tests to call both IDP and SP
    // initiated flows and enable us to set global flags to
    // enable/disable either of those flows
    public Object solicited_SP_initiated_SAML(String testName, SAMLTestSettings updatedTestSettings, String[] actions, List<validationData> expectations) throws Exception {
        if (runSolicitedSPInitiatedTests) {
            printTestTrace("solicited_SP_initiated_SAML", "Running Solicited SP Initiated SAML Flow");
            WebClient webClient = SAMLCommonTestHelpers.getWebClient();
            return genericSAML(testName, webClient, updatedTestSettings, actions, expectations);
        } else {
            return null;
        }

    }

    public Object IDP_initiated_SAML(String testName, SAMLTestSettings updatedTestSettings, String[] actions, List<validationData> expectations) throws Exception {
        if (runIDPInitiatedTests) {
            printTestTrace("IDP_initiated_SAML", "Running IDP Initiated SAML Flow");
            WebClient webClient = SAMLCommonTestHelpers.getWebClient();
            return genericSAML(testName, webClient, updatedTestSettings, actions, expectations);
        } else {
            return null;
        }

    }

    public Object unsolicited_SP_initiated_SAML(String testName, SAMLTestSettings updatedTestSettings, String[] actions, List<validationData> expectations) throws Exception {
        if (runUnsolicitedSPInitiatedTests) {
            printTestTrace("unsolicited_SP_initiated_SAML", "Running Unsolicited SP Initiated SAML Flow");
            WebClient webClient = SAMLCommonTestHelpers.getWebClient();
            return genericSAML(testName, webClient, updatedTestSettings, actions, expectations);
        } else {
            return null;
        }

    }

    public List<validationData> setDefaultGoodLoginAgainExpectations(String startingAction, SAMLTestSettings testSettings) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, startingAction, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on the IDP form login form.", null, cttools.getLoginTitle(testSettings.getIdpRoot()));
        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML POST response", null, cttools.getResponseTitle(testSettings.getIdpRoot()));
        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML Response", null, SAMLConstants.SAML_RESPONSE);
        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.SAML_TOKEN, SAMLConstants.STRING_CONTAINS, "SAML Token did not contain expected values", null, null);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on the IDP form login form.", null, cttools.getLoginTitle(testSettings.getIdpRoot()));
        return expectations;
    }

    /**
     * Helper method - determines if any of the JsessionCookies are invalid
     *
     * @param webClient
     * @return
     */
    public Boolean anyInvalidJSessionCookie(WebClient webClient) {
        boolean invalidJsession = false;
        ArrayList<Cookie> cookies = helpers.extractJsessionCookies(webClient);
        Log.info(thisClass, null, cookies.size() + " Jsession cookies");
        for (Cookie cookie : cookies) {
            if (cookie != null && cookie.getValue() != null) {
                if (cookie.getValue().contains(":-1")) {
                    invalidJsession = true;
                }
            }
        }

        return invalidJsession;
    }

}
