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
import componenttest.annotation.ExpectedFFDC;
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

        DiscoveryUtils.waitForDiscoveryToBeReady(testSettings);

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
        WebConversation wc = new WebConversation();
        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        helpers.rpLoginPage("Setup", wc, testSettings, expectations, Constants.GETMETHOD);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>With discovery enabled on the RP, attempt to access a test servlet specifying valid OP url
     * <LI>The RP keystore and truststore is not configured.
     * <LI>The login attempt should fail with appropriate error.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Client connection is refused as RP server is not listening on SSL port
     * <LI>due to missing keystore and truststore with the following message logged:
     * <LI>CWWKS1524E: The OpenID Connect client failed to obtain OpenID Connect Provider information through the discovery
     * endpoint...
     * </OL>
     */
    @Test
    @AllowedFFDC({ "java.security.cert.CertPathBuilderException", "sun.security.validator.ValidatorException", "javax.net.ssl.SSLHandshakeException", "com.ibm.websphere.ssl.SSLException" })
    public void OidcClientDiscoverySSLTest_NoRPTrustStore() throws Exception {

        // have to keep the RP reconfig since we're altering the server wide SSL settings
        // Reconfigure RP server with SSL settings
        testRPServer.reconfigServer("rp_server_basic_ssl_no_truststore.xml", _testName, Constants.JUNIT_REPORTING, null);

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");

        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.GET_LOGIN_PAGE);

        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.EXCEPTION_MESSAGE, Constants.STRING_CONTAINS, "Should have received an exception for a connection refused exception", null, "java.net.ConnectException");

        testRPServer.addIgnoredServerExceptions("CWWKG0058E");

        WebConversation wc = new WebConversation();
        genericRP(_testName, wc, updatedTestSettings, test_LOGIN_PAGE_ONLY, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>With discovery enabled on the RP, attempt to access a test servlet specifying valid OP url. The RP
     * <LI>trust store does not contain proper trust for discovery of the OP endpoints. The oidcClient
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true". The RP truststore is missing the certificate for OP.
     * <LI>The login attempt should fail with appropriate error.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The authentication request triggers the discovery process and discovery should fail and produce error messages.
     * <LI>For serviceability, the following messages appear in the messages.log file:
     * <LI>CWWKS1524E: The OpenID Connect client failed to obtain OpenID Connect Provider information through the discovery
     * endpoint...
     * </OL>
     */
    @Test
    // TODO: (chc - enabling now that we have more ssl restart logic in the test framework - this may be ok now.)
    // TODO: (maybe issue resolved) This test encounters an error when run with other tests as SSL update timing differs.
    @ExpectedFFDC("javax.net.ssl.SSLHandshakeException")
    @AllowedFFDC({ "java.security.cert.CertPathBuilderException", "sun.security.validator.ValidatorException", "com.ibm.security.cert.IBMCertPathBuilderException", "com.ibm.websphere.ssl.SSLException" })
    public void OidcClientDiscoverySSLTest_BadRPTrustStore() throws Exception {

        // have to keep the reconfig's since we're altering the server wide SSL settings
        // Reconfigure RP server with SSL settings
        testRPServer.reconfigServer("rp_server_basic_ssl_bad_truststore.xml", _testName, Constants.JUNIT_REPORTING, null);

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);

        // Client get login page
        expectations = vData.addResponseStatusExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.OK_STATUS);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);

        // login will fail
        expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.LOGIN_USER);

        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "RP Server messages.log should contain msg indicating that ssl failure", "CWWKS1708E:.*unable to contact the OpenID Connect provider.*");

        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "RP Server messages.log should contain msg indicating an SSL handshake error occurred.", "CWPKI0823E: SSL HANDSHAKE FAILURE.*");

        WebConversation wc = new WebConversation();

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>With discovery configured at the RP, attempt to access a test servlet specifying valid OP url with bad OP trust store
     * <LI>results in 401 error with messages logged.
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true". The OP truststore is missing the certificate for RP.
     * <LI>clientAuthentication is set to "true" for OP server.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The discovery should be attempted and should fail with 401 when the RP tries process the first authentication request
     * and the following messages should be logged
     * <LI>CWWKS1525E: A successful response was not returned from the URL <discovery endpoint>
     * <LI>CWWKS1524E: The OpenID Connect client failed to obtain OpenID Connect Provider information through the discovery
     * endpoint...
     * </OL>
     */
    // different versions of java issue different exceptions - allow appropriate exceptions from any of our supported java versions
    @AllowedFFDC({ "java.net.SocketException", "javax.net.ssl.SSLHandshakeException", "javax.net.ssl.SSLException", "javax.net.ssl.SSLProtocolException" })
    @Test
    public void OidcClientDiscoverySSLTest_BadOPTrustStoreWithClientAuth() throws Exception {

        // have to keep the reconfig since we're altering the server wide SSL settings
        // Reconfigure OP server with SSL settings
        testOPServer.reconfigServer("op_server_ssl_bad_truststore_clientAuth.xml", _testName, Constants.JUNIT_REPORTING, null);

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setTestURL(updatedTestSettings.getTestURL().replace(Constants.DEFAULT_SERVLET, "simple/goodSSL"));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        WebConversation wc = new WebConversation();

        try {
            genericRP(_testName, wc, updatedTestSettings, test_LOGIN_PAGE_ONLY, expectations);
        } catch (Exception e) {
            // there can be one of multiple exceptions thrown for this erroneous
            // condition - the exceptions vary by JDK - validating the exception
            // here allows the test to handle instances where we can get one of
            // multiple possible error better than using an expectation.
            msgUtils.assertTrueAndLog(_testName, "Expected one of the following in the exception: javax.net.ssl.SSLException, javax.net.ssl.SSLHandshakeException, java.net.SocketException actually received: " + e,
                    validationTools.foundOneErrorInException(e, "javax.net.ssl.SSLException", "javax.net.ssl.SSLHandshakeException", "java.net.SocketException", "java.io.IOException"));

        }

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>When the RP is configured with discovery, attempt to access a test servlet specifying valid OP url
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true". The OP keystore and truststore is not configured.
     * <LI>SSL connection from RP to OP is refused and the discovery process cannot complete.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The login page cannot be displayed and returns 401
     * <LI>Messages are logged to indicate that discovery did not complete:
     * <LI>CWWKS1525E: A successful response was not returned from the URL
     * [https://localhost:8947/oidc/endpoint/OidcConfigSample/.well-known/openid-configuration]. This is the [404] response status
     * and the [CWOAU0073E: An error was encountered while authenticating a user.
     * Please try authenticating again, or contact the site administrator if the problem persists.] error from the discovery
     * request.
     * <LI>CWWKS1524E: The OpenID Connect client [client01] failed to obtain Open ID Connect Provider endpoint information through
     * the discovery endpoint URL [https://localhost:8947/oidc/endpoint/OidcConfigSample/.mal-formed/openid-configuration].
     * Update the configuration for the OpenID Connect client with the correct HTTPS discovery endpoint URL.
     * </OL>
     */
    @AllowedFFDC({ "org.apache.http.conn.HttpHostConnectException", "javax.net.ssl.SSLException", "java.net.SocketException" })
    @Test
    public void OidcClientDiscoverySSLTest_NoOPTrustStore() throws Exception {

        // have to keep the reconfig's since we're altering the server wide SSL settings
        // Reconfigure OP server with SSL settings
        // NOTE:  The op reconfig will take longer since we're disabling ssl and there is a check in the reconfig logic that will wait for SSL to
        testOPServer.reconfigServer("op_server_basic_ssl_no_truststore.xml", _testName, false, null);

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setTestURL(updatedTestSettings.getTestURL().replace(Constants.DEFAULT_SERVLET, "simple/goodSSL"));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.EXCEPTION_MESSAGE, Constants.STRING_CONTAINS, "Should have received an exception for a connection refused exception", null, "java.net.ConnectException");
        WebConversation wc = new WebConversation();

        testOPServer.addIgnoredServerException("CWWKG0058E");

        genericRP(_testName, wc, updatedTestSettings, test_LOGIN_PAGE_ONLY, expectations);

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
