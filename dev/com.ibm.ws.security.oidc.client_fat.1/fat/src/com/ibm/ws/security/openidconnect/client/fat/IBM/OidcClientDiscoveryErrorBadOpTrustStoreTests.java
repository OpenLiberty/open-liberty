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
public class OidcClientDiscoveryErrorBadOpTrustStoreTests extends CommonTest {
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

        thisClass = OidcClientDiscoveryErrorBadOpTrustStoreTests.class;

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
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.op", "op_server_ssl_bad_truststore_clientAuth.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

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

}
