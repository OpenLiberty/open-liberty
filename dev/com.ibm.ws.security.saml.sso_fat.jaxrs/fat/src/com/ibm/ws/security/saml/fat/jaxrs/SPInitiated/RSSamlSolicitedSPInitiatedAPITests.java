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
package com.ibm.ws.security.saml.fat.jaxrs.SPInitiated;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml.fat.jaxrs.common.RSSamlAPITests;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * The testcases in this class were ported from tWAS' test SamlWebSSOTests.
 * If a tWAS test is not applicable, it will be noted in the comments below.
 * If a tWAS test fits better into another test class, it will be noted
 * which test project/class it now resides in.
 * In general, these tests perform a simple IdP initiated SAML Web SSO, using
 * httpunit to simulate browser requests. In this scenario, a Web client
 * accesses a static Web page on IdP and obtains a a SAML HTTP-POST link
 * to an application installed on a WebSphere SP. When the Web client
 * invokes the SP application, it is redirected to a TFIM IdP which issues
 * a login challenge to the Web client. The Web Client fills in the login
 * form and after a successful login, receives a SAML 2.0 token from the
 * TFIM IdP. The client invokes the SP application by sending the SAML
 * 2.0 token in the HTTP POST request.
 */
@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class RSSamlSolicitedSPInitiatedAPITests extends RSSamlAPITests {

    private static final Class<?> thisClass = RSSamlSolicitedSPInitiatedAPITests.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        className = "APISolicitedSPInitiatedTests";
        flowType = SAMLConstants.SOLICITED_SP_INITIATED;

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        // add any additional messages that you want the "start" to wait for
        // we should wait for any providers that this test requires
        List<String> extraMsgs = getDefaultSAMLStartMsgs();

        List<String> extraMsgs2 = new ArrayList<String>();

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SAMLConstants.SAML_CLIENT_APP);
        extraApps.add("jaxrsclient");
        List<String> extraApps2 = new ArrayList<String>();
        extraApps2.add("helloworld");

        copyMetaData = false;

        testAppServer = commonSetUp(APPServerName, "server_apiTest.xml", SAMLConstants.SAML_ONLY_SETUP, SAMLConstants.APP_SERVER_TYPE, extraApps2, extraMsgs2, false);
        commonUtils.fixServer2Ports(testAppServer);
        startSPWithIDPServer(SPServerName, "server_apiTest.xml", extraMsgs, extraApps, true);

        // Allow the warning on the ignored attributes of samlWebSso20 inboundPropagation true or false
        testAppServer.addIgnoredServerExceptions(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES, SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, SAMLMessageConstants.CWWKS5013E_MISSING_SAML_ASSERTION_ERROR);
        testSAMLServer.addIgnoredServerException(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES);

        // set default values for jaxrs settings
        testSettings.setRSSettings();

        // set test app
        testSettings.setSpTargetApp(testSAMLServer.getServerHttpsString() + "/" + SAMLConstants.JAXRS_PROTECTED_SVC_CLIENT);
        testSettings.setSpAlternateApp(testSAMLServer.getServerHttpsString() + "/" + SAMLConstants.JAXRS_SVC_CLIENT);
        //		testSettings.setSpDefaultApp(testAppServer.getServerHttpsString() + "/" + SAMLConstants.PARTIAL_HELLO_WORLD_URI);
        testSettings.setSpDefaultApp(testAppServer.getServerHttpString() + "/" + SAMLConstants.PARTIAL_HELLO_WORLD_URI);
        testSettings.updatePartnerInSettings("sp1", true);

        actionList = standardFlow;
        endAction = SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE;

    }

    /**
     * Call 2 protected apps on the same server and get a response with saml tokens each time.
     * Each app uses a different user to login.
     * After getting 2 responses, invoke ACS in an attempt to invoke yet another app on the
     * rs_saml server.
     * Make sure that the svc client invokes the saml api to objain the saml assertion correctly.
     * This test will use the random selection of how the token is be sent. That means the svc client
     * will sometime use the text, encoded or compressed encoded form of the saml token.
     * The app on the rs_saml server should print the correct user, ...
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void RSSamlSolicitedSPInitiatedAPITests_useJaxRSCLientServlet_switchUsers() throws Exception {

        // commented out lines for wc2, wc3 and 2c4 copy all but the ltpatoken cookie from the previous conversation

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();
        WebClient webClient2 = SAMLCommonTestHelpers.getWebClient();
        WebClient webClient3 = SAMLCommonTestHelpers.getWebClient();
        WebClient webClient4 = SAMLCommonTestHelpers.getWebClient();

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsAIPTests(flowType, testSettings);
        Object page1 = genericSAML(_testName, webClient, testSettings, justGetSAMLToken, expectations);

        //		wc2 = helpers.addAllCookiesExcept(wc2, helpers.extractAllCookiesExcept(wc, "LtpaToken2"), "LtpaToken2");
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setIdpUserName("user1");
        updatedTestSettings.setIdpUserPwd("security");
        updatedTestSettings.updatePartnerInSettings("sp1", "sp2", true);
        updatedTestSettings.setSpTargetApp(testSAMLServer.getServerHttpsString() + "/" + SAMLConstants.JAXRS_SP2_SVC_CLIENT);
        List<validationData> expectations2 = commonUtils.getGoodExpectationsForJaxrsAIPTests(flowType, updatedTestSettings);
        Object page2 = genericSAML(_testName, webClient2, updatedTestSettings, justGetSAMLToken, expectations2);

        //		wc3 = helpers.addAllCookiesExcept(wc3, helpers.extractAllCookiesExcept(wc2, "LtpaToken2"), "LtpaToken2");
        helpers.invokeACSWithSAMLResponse(_testName, webClient3, page1, testSettings, expectations);
        //		wc4 = helpers.addAllCookiesExcept(wc4, helpers.extractAllCookiesExcept(wc3, "LtpaToken2"), "LtpaToken2");
        helpers.invokeACSWithSAMLResponse(_testName, webClient4, page2, updatedTestSettings, expectations2);

    }
}
