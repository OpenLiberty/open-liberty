/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.client.fat.IBM;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.ClientTestHelpers;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
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
 * This is the test class that will run basic OpenID Connect RP tests.
 * This test class extends GenericRPTests.
 * GenericRPTests contains common code for all RP tests.
 *
 **/

@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException" })
@RunWith(FATRunner.class)
public class OidcClientBasicTests extends GenericOidcClientTests {

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

        thisClass = OidcClientBasicTests.class;

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.OPENID_APP);
            }
        };

        testSettings = new TestSettings();

        // Set config parameters for Access token with X509 Certificate in OP config files
        String tokenType = Constants.ACCESS_TOKEN_KEY;
        String certType = Constants.X509_CERT;

        // Start the OIDC OP server
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.op", "op_server_orig.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS,
                Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        //Start the OIDC RP server and setup default values
        testRPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.rp", "rp_server_orig.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY,
                Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        // override actions that generic tests should use - Need to skip consent form as httpunit
        // cannot process the form because of embedded javascript

        test_GOOD_LOGIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;
        test_GOOD_POST_LOGIN_ACTIONS = Constants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT;
        test_GOOD_LOGIN_AGAIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_AGAIN_ACTIONS;
        test_FinalAction = Constants.LOGIN_USER;
        testSettings.setFlowType(Constants.RP_FLOW);
    }

    // expected behavior changed for defect 120321 - RP won't know to expect a response without an id_token
    //      - this will result in a 403
    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>There is nothing special about the configuration - it just needs to be valid.
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true"
     * <LI>Test is showing that the OP doesn't return an id_token
     * <LI>The RP doesn't know about oauth and doesn't know to not expect an id_token.
     * <LI>The RP's httpsRequired attribute is set to "true" by default
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should receive a 403
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientTestUseOAuth() throws Exception {

        WebConversation wc = new WebConversation();

        // Reconfigure OP server with basic SSL settings
        // Reconfigure RP server with basic SSL settings
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_https_redirect.xml", "rp_server_cl_use_oauth.xml");

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("profile");
        // fix these two endpoints once we're NOT using hard coded ports
        updatedTestSettings.setAuthorizeEndpt("https://localhost:8945/oauth2/endpoint/OAuthConfigSample/authorize");
        updatedTestSettings.setTokenEndpt("https://localhost:8945/oauth2/endpoint/OAuthConfigSample/token");

        List<validationData> expectations = validationTools.add401Responses(Constants.GET_LOGIN_PAGE);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, testRPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that the required scope \"openid\" is missing", null, "CWWKS1713E.*client01.*openid");

        genericRP(_testName, wc, updatedTestSettings, test_LOGIN_PAGE_ONLY, expectations);
    }

    // expected behavior changed for defect 120321 - RP won't know to expect a response without an id_token
    //      - this will result in a 403
    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>There is nothing special about the configuration - it just needs to be valid.
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true"
     * <LI>Test is showing that even with openid in the scope, the OP doesn't return an id_token
     * <LI>The RP doesn't know about oauth and doesn't know to not expect an id_token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should receive a 403
     * </OL>
     */
    @Test
    public void OidcClientTestUseOAuthOpenidInScope() throws Exception {

        WebConversation wc = new WebConversation();

        // Reconfigure OP server with basic SSL settings
        // Reconfigure RP server with openidconnect client settings using oauth instead of oidc
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_orig.xml", "rp_server_cl_use_oauth_openid_in_scope.xml");

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, testRPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                "Client messages.log should contain a message indicating that the ID Token is missing in the response", null, "CWWKS1712E.*client01.*ID token");

        testRPServer.addIgnoredServerExceptions("CWWKS1712E");
        
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet with a client request to an invalid token endpoint
     * <LI>The request should fail with appropriate 403 error and serviceable messages in
     * <LI>messages.log at RP and OP to aid the user in problem determination.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail with HTTP 403 error.
     * <LI>OP messages.log contains CWOAU0039W: The request directed to [/OAuthConfigSample/bad/token] was not recognized as a
     * valid request.
     * <LI>RP messages.log contains CWWKS1708E: The OpenID Connect client [client01] is unable to contact the OpenID Connect
     * provider to receive an ID token due to [Failed to reach endpoint: Error 404: SRVE0295E: Error reported: 404].
     * <LI>RP expects An FFDC Incident has been created: "java.io.IOException: Failed to reach endpoint: Error 404
     * </OL>
     */
    @AllowedFFDC("java.io.IOException")
    @Test
    public void OidcClientTestBadTokenEndpoint() throws Exception {

        WebConversation wc = new WebConversation();
        // Reconfigure OP server with basic SSL settings
        // Reconfigure RP server with bad endpoint
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_basic_ssl.xml", "rp_server_bad_tokenendpt.xml");

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, testOPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                "Server messages.log should contain a message indicating that the OP received a request that was not valid.", null, Constants.MSG_RP_REQUEST_INVALID);
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, testRPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                "Server messages.log should contain a message indicating that the RP is unable to contact the OP for client01", null, Constants.MSG_UNABLE_TO_CONTACT_OP);
        expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.LOGIN_USER);
        
        testOPServer.addIgnoredServerExceptions("CWOAU0039W");
        
        genericRP(_testName, wc, testSettings, test_GOOD_LOGIN_ACTIONS, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet with a client request to an invalid authorization endpoint
     * <LI>The request should fail with appropriate 404 error and serviceable message in
     * <LI>messages.log at OP to aid the user in problem determination.
     * <LI>The not found error, should come with a message indicating the RP request was invalid.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail with HTTP 404 error.
     * <LI>OP messages.log contains CWOAU0039W: The request directed to the endpoint URL of [/bad/OidcConfigSample/authorize] was
     * not recognized by the OAuth provider as a valid request.
     * </OL>
     */
    @Test
    public void OidcClientTestBadAuthorizationEndpointOAuthError() throws Exception {
        WebConversation wc = new WebConversation();
        //Reconfigure OP server with basic SSL settings
        //Reconfigure RP server with bad authorization endpoint
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_basic_ssl.xml", "rp_server_bad_authorizationendpt_oauth.xml");

        String expected404Step = Constants.GET_LOGIN_PAGE;
        List<validationData> expectations = validationTools.getDefault404VDataExpectations(expected404Step);

        expectations = vData.addExpectation(expectations, expected404Step, testOPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                "Did not get a message indicating bad endpoint in OP messages.log", null, Constants.MSG_RP_REQUEST_INVALID + ".*bad/OidcConfigSample");
        
        testOPServer.addIgnoredServerExceptions("CWOAU0039W");

        genericRP(_testName, wc, testSettings, test_LOGIN_PAGE_ONLY, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet with a client request to an invalid authorization endpoint
     * <LI>The request should fail with appropriate 404 error and serviceable message in
     * <LI>messages.log at OP to aid the user in problem determination.
     * <LI>The not found error, should come with a message originating from the web container
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail with HTTP 404 error.
     * <LI>OP messages.log contains SRVE0190E: File not found: /v10/endpoint/bad/OAuthConfigSample/authorize
     * </OL>
     */
    @Test
    @AllowedFFDC("jakarta.servlet.ServletException") // TODO See https://github.com/OpenLiberty/open-liberty/issues/16136
    public void OidcClientBadAuthorizeEndpointWebContainerError() throws Exception {

        WebConversation wc = new WebConversation();
        //Reconfigure OP server with basic SSL settings
        //Reconfigure RP server with bad authorize endpoint
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_basic_ssl.xml", "rp_server_bad_authorizationendpt_webcontainer.xml");

        String expected404Step = Constants.GET_LOGIN_PAGE;
        List<validationData> expectations = validationTools.getDefault404VDataExpectations(expected404Step);

        expectations = vData.addExpectation(expectations, expected404Step, testOPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                "Did not get a message indicating bad endpoint in OP messages.log", null, Constants.MSG_FILE_NOT_FOUND + ".*v10/endpoint/bad/");
        
        testOPServer.addIgnoredServerExceptions("SRVE0190E");
        
        genericRP(_testName, wc, testSettings, test_LOGIN_PAGE_ONLY, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet when the OP config contains a
     * <LI>bad Issuer identifier which does not match what the RP calculates based on the
     * <LI>token endpoint. If the OP specifies an issuer identifier that does not
     * <LI>match the calculated identifier. The user is prompted to login and after
     * <LI>entering ID and password, an error message is returned to the end-user.
     * <LI>The request should fail with appropriate error to end-user, and useful error
     * <LI>messages should be logged in messages.log so the system administrator can
     * <LI>disagnose the problem without calling IBM support.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Message is returned to the end-user: 403 AuthenticationFailed
     * <LI>The RP messages.log gives a more detailed message in FFDC for the system administrator to resolve the problem:
     * <LI>CWWKS1751E: Validation failed for the ID token requested by [client01] because the (iss) issuer
     * [https://localhost:8945/oidc/endpoint/bad/OidcConfigSample] specified in the token does not match the [issuerIdentifier]
     * attribute [https://localhost:8945/oidc/endpoint/OidcConfigSample] for the provider specified in the OpenID Connect client
     * configuration.
     * </OL>
     */
    @Test
    public void OidcClientTestBadIssuerIdentifierOPConfig() throws Exception {

        WebConversation wc = new WebConversation();

        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_badissuer_ssl.xml", "rp_server_basic_ssl.xml");

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, testRPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                "Did not get a message in RP messages.log indicating the validation failed because the issuer specified did not match the provider.", null, "CWWKS1751E:.*/endpoint/bad/OidcConfigSample");
        
        testRPServer.addIgnoredServerExceptions("CWWKS1751E");
        
        genericRP(_testName, wc, testSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true". The RP keystore and truststore is not configured.
     * <LI>The login attempt should fail with appropriate error.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Client connection is refused as RP server is not listening on SSL port
     * <LI>due to missing keystore and truststore.
     * </OL>
     */
    @Test
    public void OidcClientSSLTest_NoRPTrustStore() throws Exception {

        // Reconfigure OP server with SSL settings
        // Reconfigure RP server with SSL settings
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_basic_ssl.xml", "rp_server_basic_ssl_no_truststore.xml");

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");

        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.GET_LOGIN_PAGE);

        // verify ssl config error in RP server messages.log
        //        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, testRPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
        //                "RP Server messages.log should contain msg indicating that ssl config error", null,
        //                "CWWKG0058E:.*missing required attribute keyStoreRef.*");
        //        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, testRPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
        //                "RP Server messages.log should contain msg indicating that ssl is not listening", null,
        //                "CWWKO0220I:.*has stopped listening for requests.*");
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.EXCEPTION_MESSAGE, Constants.STRING_CONTAINS,
                "Should have received an exception for a connection refused exception", null,
                "java.net.ConnectException");
        WebConversation wc = new WebConversation();
        
        testRPServer.addIgnoredServerExceptions("CWWKG0058E");

        genericRP(_testName, wc, updatedTestSettings, test_LOGIN_PAGE_ONLY, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true". The RP truststore is missing the certificate for OP.
     * <LI>The login attempt should fail with appropriate error.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to authenticate - and receive authentication error.
     * <LI>For serviceability, the following messages appear in the messages.log file:
     * <LI>CWPKI0022E: SSL HANDSHAKE FAILURE: A signer with SubjectDN CN=localhost, O=ibm, C=us was sent from the target host. The
     * signer might need to be added to local trust store
     * /test/jazz_build/jbe_serav/jazz/buildsystem/buildengine/eclipse/build/image
     * /output/wlp/usr/servers/com.ibm.ws.security.openidconnect.client-1.0_fat.rp/commonBadTrustStore.jks, located in SSL
     * configuration alias DefaultSSLSettings. The extended error message from the SSL handshake exception is: PKIX path building
     * failed: java.security.cert.CertPathBuilderException: unable to find valid certification path to requested target
     * <LI>CWWKS1708E: The OpenID Connect client [client01] is unable to contact the OpenID Connect provider at
     * [https://localhost:8945/oidc/endpoint/OidcConfigSample/token] to receive an ID token due to
     * [java.security.cert.CertificateException: PKIX path building failed: java.security.cert.CertPathBuilderException: unable to
     * find valid certification path to requested target].
     * </OL>
     */
    @Test
    @ExpectedFFDC("javax.net.ssl.SSLHandshakeException")
    @AllowedFFDC({ "java.security.cert.CertPathBuilderException", "sun.security.validator.ValidatorException", "com.ibm.security.cert.IBMCertPathBuilderException" })
    public void OidcClientSSLTest_BadRPTrustStore() throws Exception {

        // Reconfigure OP server with SSL settings
        // Reconfigure RP server with SSL settings
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_basic_ssl.xml", "rp_server_basic_ssl_bad_truststore.xml");

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);

        // Client get login page
        expectations = vData.addResponseStatusExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.OK_STATUS);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);

        // login will fail
        expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.LOGIN_USER);

        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, testRPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                "RP Server messages.log should contain msg indicating that ssl failure", null,
                "CWWKS1708E:.*unable to contact the OpenID Connect provider.*");

        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, testRPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                "RP Server messages.log should contain msg indicating an SSL handshake error occurred.", null,
                "CWPKI0823E: SSL HANDSHAKE FAILURE.*");

        WebConversation wc = new WebConversation();
        
        testRPServer.addIgnoredServerExceptions("CWPKI0823E", "CWWKS1708E");
        
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true". The OP truststore is missing the certificate for RP.
     * <LI>clientAuthentication is set to "true" for OP server.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The SSL connection should fail as OP cannot authenticate RP and terminates the handshake.
     * </OL>
     */
    @Test
    public void OidcClientSSLTest_BadOPTrustStoreWithClientAuth() throws Exception {

        // Reconfigure OP server with SSL settings
        // Reconfigure RP server with SSL settings
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_ssl_bad_truststore_clientAuth.xml", "rp_server_basic_ssl.xml");

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");

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
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true". The OP keystore and truststore is not configured.
     * <LI>SSL connection from RP to OP is refused.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The SSL connection is refused as OP server is not listening on SSL port
     * <LI>due to missing keystore and truststore.
     * </OL>
     */
    @Test
    public void OidcClientSSLTest_NoOPTrustStore() throws Exception {

        // Reconfigure OP server with SSL settings
        // Reconfigure RP server with SSL settings
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_basic_ssl_no_truststore.xml", "rp_server_basic_ssl.xml");

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");

        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // verify ssl config error in OP server messages.log

        // this message is actually issued when the server is reconfigured, not when we try to use the keystore
        //        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, testOPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
        //                "OP Server messages.log should contain msg indicating that ssl config error", null, "CWWKG0058E");
        //
        //        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, testOPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
        //                "OP Server messages.log should contain msg indicating that ssl is not listening", null,
        //                "CWWKO0220I:.*has stopped listening for requests.*");

        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.EXCEPTION_MESSAGE, Constants.STRING_CONTAINS,
                "Should have received an exception for a connection refused exception", null,
                "java.net.ConnectException");
        WebConversation wc = new WebConversation();

        testOPServer.addIgnoredServerException("CWWKG0058E");
        
        genericRP(_testName, wc, updatedTestSettings, test_LOGIN_PAGE_ONLY, expectations);
    }

    /*
     * This tests the configuration attribute useSystemPropertiesForHttpClientConnections.
     * A proxy host and port are defined in usr/(server)/jvm.options, but won't take effect until this attribute is set to true.
     * When the attribute is set to true, we expect a failure because the token retrieval call should
     * be redirected to the non-existent proxy server.
     *
     * Testing the full path would require a proxy server, which the FAT framework does not have, but it has been done manually.
     * As the oidc client and social oidc clients use the same authentication code, this also covers most of the path used
     * by the social oidc clients.
     *
     * Note that the proxy properties DO NOT TAKE EFFECT FOR LOCALHOST, so if you are debugging, you cannot configure the OP
     * to be localhost(blah) and expect it to work.
     *
     */
    @AllowedFFDC({ "org.apache.http.conn.HttpHostConnectException", "org.apache.http.conn.ConnectTimeoutException", "java.net.SocketException", "java.net.NoRouteToHostException" })
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientTestUseJvmProps() throws Exception {
        testRPServer.reconfigServer("rp_server_orig_usejvmproxyprops.xml", _testName, false, null);
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.addRequestParms();

        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        // CWWKS1708E: ... [Connect to 1.2.3.4:34567 [/1.2.3.4] failed: Connection timed out: connect].
        // some non-windows java11: CWWKS1708E: ... [Connection reset]

        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                "Client messages.log should contain a message indicating that use of the proxy was attempted", MessageConstants.CWWKS1708E_CLIENT_FAILED_TO_CONTACT_PROVIDER);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }
}
