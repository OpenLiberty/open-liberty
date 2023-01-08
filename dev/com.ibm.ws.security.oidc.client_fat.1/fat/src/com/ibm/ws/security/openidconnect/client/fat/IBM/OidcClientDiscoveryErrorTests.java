/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
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

package com.ibm.ws.security.openidconnect.client.fat.IBM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTestHelpers;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.DiscoveryUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.openidconnect.client.fat.CommonTests.GenericOidcClientTests;
import com.meterware.httpunit.WebConversation;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that will run error scenarios with OpenID Connect RP tests with an RP configuration (using access token
 * and X509 cert) which includes discovery of OP endpoints.
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
 * This test configures the sslRef as part of the openidConnectClient configuration so that the SSL processing is handled by the
 * client rather than
 * outside this configuration.
 *
 **/

@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException", "java.security.cert.CertPathBuilderException", "sun.security.validator.ValidatorException", "javax.net.ssl.SSLHandshakeException", "com.ibm.websphere.ssl.SSLException", "java.lang.Exception" })
@RunWith(FATRunner.class)
public class OidcClientDiscoveryErrorTests extends CommonTest {
    public static Class<?> thisClass = GenericOidcClientTests.class;
    public static HashMap<String, Integer> defRespStatusMap = null;

    public static String[] test_GOOD_LOGIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;
    public static String[] test_GOOD_POST_LOGIN_ACTIONS = Constants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT;
    public static String[] test_GOOD_LOGIN_AGAIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_AGAIN_ACTIONS;
    public static String[] test_LOGIN_PAGE_ONLY = Constants.GET_LOGIN_PAGE_ONLY;
    public static String test_FinalAction = Constants.LOGIN_USER;
    protected static String hostName = "localhost";

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

        thisClass = OidcClientDiscoveryErrorTests.class;

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.OPENID_APP);
            }
        };

        testSettings = new TestSettings();

        // Set config parameters for Access token with X509 Certificate
        String tokenType = Constants.ACCESS_TOKEN_KEY;
        String certType = Constants.X509_CERT;

        // Start the OIDC OP server
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.op", "op_server_orig.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        DiscoveryUtils.waitForOPDiscoveryToBeReady(testSettings);

        //Start the OIDC RP server and setup default values
        testRPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.rpd", "rp_server_orig.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY,
                Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        testOPServer.addIgnoredServerException("CWOAU0039W"); // Ignore errors from the malformed discovery url
        testOPServer.addIgnoredServerException("CWWKO0801E");
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
        testRPServer.addIgnoredServerException("CWWKG0058E"); // Ignore SSL failure message

        // override actions that generic tests should use - Need to skip consent form as httpunit
        // cannot process the form because of embedded javascript

        test_GOOD_LOGIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;
        test_GOOD_POST_LOGIN_ACTIONS = Constants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT;
        test_GOOD_LOGIN_AGAIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_AGAIN_ACTIONS;
        test_FinalAction = Constants.LOGIN_USER;
        testSettings.setFlowType(Constants.RP_FLOW);

        // Force discovery to run to access login page so that the discovery service is active. Then, in the actual tests the reconfig will initiate discovery and cause the
        // error messages to be generated on discovery failures during the reconfig (rather than waiting until the first authorization request in the runtime).
        CommonTestHelpers helpers = new CommonTestHelpers();

        // try to wait for discovery to have populated the RP config
        // Don't stop if this fails (there is a chance it could be ready by the time the tests actually run
        DiscoveryUtils.waitForRPDiscoveryToBeReady(testSettings);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>This is an error scenario where the discovery endpoint is NOT configured with HTTPS as required:
     * discoveryEndpointUrl="http://localhost:${bvt.prop.OP_HTTP_default.secure}/oidc/endpoint/OidcConfigSample/.well-known/openid-configuration"
     * <LI>This should result in error messages indicating that discovery cannot be completed.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The discovery should be attempted and should fail with 401 when the RP tries process the first authentication request
     * and the following messages should be logged
     * <LI>CWWKS1524E: The OpenID Connect client failed to obtain OpenID Connect Provider information through the discovery
     * endpoint...
     * </OL>
     */
    @Mode(TestMode.LITE)
    // different versions of java issue different exceptions - allow appropriate exceptions from any of our supported java versions
    @AllowedFFDC({ "javax.net.ssl.SSLPeerUnverifiedException" })
    @Test
    public void OidcClientDiscoverySSLTest_DiscoveryEndpointNotHTTPS() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setTestURL(updatedTestSettings.getTestURL().replace(Constants.DEFAULT_SERVLET, "simple/httpDiscovery"));

        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.GET_LOGIN_PAGE);
        expectations = vData.addResponseStatusExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.UNAUTHORIZED_STATUS);

        testRPServer.addIgnoredServerException("CWWKS1534E");

        WebConversation wc = new WebConversation();
        genericRP(_testName, wc, updatedTestSettings, test_LOGIN_PAGE_ONLY, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>This is an error scenario where the discovery endpoint is mal-formed and does not have the required syntax (.mal-formed
     * instead of .well-known)
     * discoveryEndpointUrl="https://localhost:${bvt.prop.OP_HTTP_default.secure}/oidc/endpoint/OidcConfigSample/.mal-formed/openid-configuration"
     * <LI>This should result in error messages indicating that discovery cannot be completed.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The discovery should be attempted and should fail with 401 when the RP tries process the first authentication request
     * and the following messages should be logged
     * <LI>CWWKS1525E: A successful response was not returned from the URL
     * [https://localhost:8947/oidc/endpoint/OidcConfigSample/.mal-formed/openid-configuration]. This is the [404] response status
     * and the [CWOAU0073E: An error was encountered while authenticating a user.
     * Please try authenticating again, or contact the site administrator if the problem persists.] error from the discovery
     * request.
     * <LI>CWWKS1524E: The OpenID Connect client failed to obtain OpenID Connect Provider information through the discovery
     * endpoint...
     * </OL>
     */
    @AllowedFFDC("java.lang.Exception")
    @Test
    public void OidcClientDiscoverySSLTest_DiscoveryEndpointMalFormed() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setTestURL(updatedTestSettings.getTestURL().replace(Constants.DEFAULT_SERVLET, "simple/malformedDiscovery"));

        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.GET_LOGIN_PAGE);
        expectations = vData.addResponseStatusExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.UNAUTHORIZED_STATUS);

        testRPServer.addIgnoredServerException("CWWKS1534E");

        WebConversation wc = new WebConversation();
        genericRP(_testName, wc, updatedTestSettings, test_LOGIN_PAGE_ONLY, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>With discovery configured along with other endpoints, attempt to access a test servlet specifying both a discovery
     * endpoint and authorization, token, userinfo and jwk endpoints in the RP config.
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute in server.xml as:
     * autoAuthorize="true"
     * <LI>A message is logged to indicate that discovery is processed and the other configured endpoints will be ignored.
     * <LI>
     * <LI>Test is showing a good main path flow between RP and OPusing discovered endpoints. In this scenario, a message is
     * logged to indicate that the values
     * <LI>obtained from the discovery endpoint URL will override the other configured endpoints.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue
     * <LI>Message CWWKS1521W is received to indicate the discovery information is used rather than other configured endpoints.
     * </OL>
     */
    @Test
    public void OidcClientDiscoveryTestDiscoveredEndpointsOverrideConfiguredEndpoints() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.addRequestParms();
        updatedTestSettings.setTestURL(updatedTestSettings.getTestURL().replace(Constants.DEFAULT_SERVLET, "simple/discConflict"));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);

        expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>With discovery configured, attempt to access a test servlet when the OP config contains a
     * configured Issuer Identifier and the RP contains an Issuer Identifier which differs from the OP.
     * <LI>The RP should discover and use the issuer endpoint as defined in the OP
     * configuration and access to the servlet should succeed
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The message should be logged to indicate that the discovery takes precedence:
     * CWWKS1522W: The OpenId Connect client (Relying Party or RP) [client01] configuration specifies both the discovery endpoint
     * URL [discoveryEndpointUrl] and the issuer identifier [issuerIdentifier].
     * RP will use the information from the discovery request and ignore the configured issuer identifier.
     * <LI>Should authenticate and access the test servlet without issue
     * </OL>
     */
    @Test
    public void OidcClientDiscoveryTestWithDiscoveredIssuerIdentifierFromOPConfigOverridesRPConfig() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.addRequestParms();
        updatedTestSettings.setTestURL(updatedTestSettings.getTestURL().replace(Constants.DEFAULT_SERVLET, "simple/issuerIdentifierOverride"));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);

        expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }
}
