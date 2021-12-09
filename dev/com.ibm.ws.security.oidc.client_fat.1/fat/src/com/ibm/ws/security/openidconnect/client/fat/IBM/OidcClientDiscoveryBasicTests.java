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

package com.ibm.ws.security.openidconnect.client.fat.IBM;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.DiscoveryUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.openidconnect.client.fat.CommonTests.GenericOidcClientTests;
import com.meterware.httpunit.WebConversation;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that will run basic OpenID Connect RP tests with an RP configuration (using access token and X509 cert)
 * which includes discovery of OP endpoints.
 * Discovery of OP endpoints is configured in the openidConnectClient section in server.xml by specifying the discoveryEndpointUrl
 * instead of configuring
 * each individual OP endpoint URL (such as authorizationEndpointUrl and tokenEndpointUrl) separately. This line below is used to
 * configure RP discovery:
 *
 * discoveryEndpointUrl="https://localhost:${bvt.prop.OP_HTTP_default.secure}/oidc/endpoint/OidcConfigSample/.well-known/openid-configuration"
 *
 * With discovery, all the discovered endpoints are HTTPS. Any endpoints which are specified in the configuration will be ignored
 * and the
 * discovered endpoints will be used instead.
 *
 * This test class extends GenericOidcClientTests and exercises the authorization, token and userinfo endpoints after discovery.
 * The token request results in an access token with X509 certificate.
 * GenericOidcClientTests contains common code for all RP tests.
 *
 **/

@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException", "java.lang.Exception" }) // dynamic reconfig generates a failure due to a malformed discovery endpoint used by some tests
@RunWith(FATRunner.class)
public class OidcClientDiscoveryBasicTests extends GenericOidcClientTests {

    public static RSCommonTestTools rsTools = new RSCommonTestTools();

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

        thisClass = OidcClientDiscoveryBasicTests.class;

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.OPENID_APP);
            }
        };

        testSettings = new TestSettings();

        // Set config parameters for Access token with X509 Certificate
        String tokenType = Constants.ACCESS_TOKEN_KEY;
        String certType = Constants.X509_CERT;

        //        String[] propagationTokenTypes = rsTools.chooseTokenSettings(Constants.OIDC_OP);
        //        String tokenType = propagationTokenTypes[0];
        //        String certType = propagationTokenTypes[1];
        Log.info(thisClass, "setupBeforeTest", "inited tokenType to: " + tokenType);

        // Start the OIDC OP server
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.op", "op_server_orig.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        DiscoveryUtils.waitForOPDiscoveryToBeReady(testSettings);

        //Start the OIDC RP server and setup default values
        testRPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.rpd", "rp_server_orig.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        testOPServer.addIgnoredServerException("CWOAU0039W"); // Ignore errors from the malformed discovery url
        testRPServer.addIgnoredServerException("CWWKS1859E"); //Ignore exceptions from bad client secret test
        testRPServer.addIgnoredServerException("SRVE8094W");
        testRPServer.addIgnoredServerException("SRVE0190E");
        testRPServer.addIgnoredServerException("CWWKS1741W"); // Ignore from sigAlgMismatch2 - message is not always logged
        testRPServer.addIgnoredServerException("SRVE8115W"); // Ignore setStatus WARNING
        testRPServer.addIgnoredServerException("CWWKS1521W"); // Ignore message indicating conflicting configured endpoints are ignored
        testRPServer.addIgnoredServerException("CWWKS1524E"); // Ignore message indicating unsuccessful response from endpoint
        testRPServer.addIgnoredServerException("CWWKS1525E"); // Ignore message indicating discovery failed
        testRPServer.addIgnoredServerException("CWWKS1522W"); // Ignore message indicating conflicting endpoints for issuer identifier
        testRPServer.addIgnoredServerException("CWPKI0022E"); // Ignore message indicating conflicting configured endpoints are ignored
        testRPServer.addIgnoredServerException("CWPKI0823E"); // Ignore message indicating conflicting configured endpoints are ignored
        testRPServer.addIgnoredServerException("CWWKG0058E"); // Ignore SSL failure message

        // override actions that generic tests should use - Need to skip consent form as httpunit
        // cannot process the form because of embedded javascript

        test_GOOD_LOGIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;
        test_GOOD_POST_LOGIN_ACTIONS = Constants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT;
        test_GOOD_LOGIN_AGAIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_AGAIN_ACTIONS;
        test_FinalAction = Constants.LOGIN_USER;
        testSettings.setFlowType(Constants.RP_FLOW);

        // try to wait for discovery to have populated the RP config
        // Don't stop if this fails (there is a chance it could be ready by the time the tests actually run
        DiscoveryUtils.waitForRPDiscoveryToBeReady(testSettings);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet when the OP config contains a configured
     * <LI>Issuer identifier which does not match what the RP would typically calculate based on the
     * <LI>token endpoint. Because the disovery endpoint is used to discover OP endpoints, the
     * <LI>RP should discover and use the issuer endpoint as defined in the OP configuration and access
     * <LI>to the servlet should succeed
     *
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue
     * </OL>
     */
    @Test
    public void OidcClientDiscoveryTestWithDiscoveredIssuerIdentifierFromOPConfig() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.addRequestParms();
        updatedTestSettings.setTestURL(updatedTestSettings.getTestURL().replace(Constants.DEFAULT_SERVLET, "simple/badIssuer"));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

}
