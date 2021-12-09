/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.server.fat.BasicTests.CommonTests;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.AuthorizationHelpers;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
public class genericWebClientAuthCertTest extends CommonTest {

    protected static AuthorizationHelpers authHelpers = new AuthorizationHelpers();
    protected static String server_cert_config = "";
    protected static String server_allow_cert_config = "";
    protected static String server_do_not_allow_cert_config = "";
    protected static String server_cert_no_user_config = "";
    protected static String server_cert_user_not_in_trust_config = "";

    /**
     * TestDescription:
     * Brief:
     * This test case is similar to the testAuthCodeBasicFlow in genericWebClientAuthCodeTest.java
     * But
     * 1) The junit testing client enables the client certificate authentication
     * 2) The certAuthentication is set to false in oauthProvider configuration
     * 3) The allowCertAuthentication attribute is also set to false
     * Since certAuthentication and allowCertAuthentication are set to false (not enabled), after SSL handshaking,
     * the Client Certificate is ignored. So, the procedure goes down to the same procedure as
     * testAuthCodeBasicFlow. The test results are the same.
     * 
     * Detail:
     * This testcase performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code". In this scenario, the client uses the
     * authorization server as an intermediary to obtain the authorization code
     * and then uses this authorization code to request access token from the
     * token endpoint. The authorization server authenticates the resource owner
     * before issuing the authorization code which is redirected to the client.
     * In this scenario, the autoauthz parameter is set to true, so the resource
     * owner does not receive the consent form from the authorizarion server.
     * The test verifies that the Oauth code flow, using the authorization grant
     * type of "authorization code" works correctly.
     * 
     */
    @Test
    public void testWebClientAuthCodeCertDisabled_doNotAllowCertAuth() throws Exception {
        testOPServer.reconfigServer(server_do_not_allow_cert_config, _testName);

        WebConversation wc = new WebConversation();

        List<validationData> expectations = authHelpers.setGoodAuthExpectations(eSettings, testSettings, _testName, Constants.WEB_CLIENT_FLOW);

        genericOP(_testName, wc, testSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);
    }

    /**
     * Test Purpose:
     * - certAuthentication is disabled, but allowCertAuthentication is enabled
     * - A certificate will be presented and should be honored since cert authentication is allowed
     * Expected Results:
     * 
     * Should authenticate and access the test servlet without issue as testuser should be
     * part of Employee role.
     */
    @Test
    public void testWebClientAuthCodeCertDisabled_allowCertAuth() throws Exception {
        testOPServer.reconfigServer(server_allow_cert_config, _testName);

        WebConversation wc = new WebConversation();

        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null, Constants.RECV_AUTH_CODE);
        expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null, Constants.RECV_FROM_TOKEN_ENDPOINT);
        String providerType = testSettings.getProviderType();
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, providerType, Constants.SUBMIT_TO_AUTH_SERVER, testSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, providerType, Constants.SUBMIT_TO_AUTH_SERVER, testSettings);
        genericOP(_testName, wc, testSettings, Constants.SUBMIT_ACTIONS, expectations);
    }

    /**
     * Test Purpose:
     * 
     * This is similar to testAuthCodeBasicFlowCertDisabled()
     * But
     * 1) The junit testing client enables the client certificate authentication
     * 2) The certAuthentication is set to true in oauthProvider configuration
     * 
     * Since the certAuthentication is set to true, the client-certificate-authentication is checked.
     * So, the client-certificate provided from junit-client will be honored when it handled the redirection from
     * RP to the OP. The client-certificate has CN=user1 in dname. (See
     * com.ibm.ws.security.oauth-oidc_fat.commonTest/securitykeys/commonSslClientDefault.jks)
     * The request will be honored as the user1 login. So, no login panel will be displayed.
     * The test needs to skip the login procedure.
     * The request is expected to be executed successfully.
     * 
     * Expected Results:
     * 
     * Should authenticate and access the test servlet without issue as testuser should be
     * part of Employee role.
     * 
     */
    @Mode(TestMode.LITE)
    @Test
    public void testWebClientAuthCodeCert() throws Exception {
        testOPServer.reconfigServer(server_cert_config, _testName);

        WebConversation wc = new WebConversation();

        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null, Constants.RECV_AUTH_CODE);
        expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null, Constants.RECV_FROM_TOKEN_ENDPOINT);
        String providerType = testSettings.getProviderType();
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, providerType, Constants.SUBMIT_TO_AUTH_SERVER, testSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, providerType, Constants.SUBMIT_TO_AUTH_SERVER, testSettings);
        genericOP(_testName, wc, testSettings, Constants.SUBMIT_ACTIONS, expectations);
    }

    /**
     * Test Purpose:
     * 
     * This is similar to testWebClientAuthCodeCert with
     * a few differences.
     * 1) The junit testing client enables the client certificate authentication
     * 2) The certAuthentication is set to true in oauthProvider configuration
     * 3) But user1, which is in the dname of client-certificate, is not listed
     * in the basic user registry of OP.
     * When the junit client tries to login with its client-certificate,
     * the login fails accordingly.
     * 
     * The request is expected to be rejected since the user is an invalid user.
     * ** Also refer to the comments in the above OidcClientTestMinimumConfigRS256ClientAuth()
     * 
     * Expected Results:
     * 
     * The request is rejected with error code 401.
     * 
     */
    @Test
    @ExpectedFFDC(value = { "com.ibm.oauth.core.api.error.oauth20.OAuth20AccessDeniedException" })
    public void testWebClientAuthCodeCertUserNotInReg() throws Exception {
        testOPServer.reconfigServer(server_cert_no_user_config, _testName);

        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.SUBMIT_TO_AUTH_SERVER);
        expectations = vData.addResponseStatusExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.UNAUTHORIZED_STATUS);

        // put the ProviderType to OAUTH-OP to make sure no ID-token even in OIDC
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, Constants.OAUTH_OP, Constants.SUBMIT_TO_AUTH_SERVER, testSettings);
        expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, testOPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Do not get the error of the invalid user of client certification", null, "CWWKS1441E:.*because the Client Certificate provided.*can not.*verified");
        WebConversation wc = new WebConversation();
        
        testOPServer.addIgnoredServerExceptions("CWWKS1441E", "SRVE8094W", "SRVE8115W");
        
        genericOP(_testName, wc, testSettings, Constants.SUBMIT_ACTIONS, expectations);

        return;
    }

    /**
     * Test Purpose:
     * 
     * This is similar to testWebClientAuthCodeCert with
     * a few differences.
     * 1) The junit testing client enables the client certificate authentication
     * 2) The certAuthentication is set to true in oauthProvider configuration
     * 3) But, the cert is not in the server's trust store
     * When the junit client tries to make a request to the server, we get
     * a connection refused failure
     * 
     * The request is expected to be rejected since the cert is missing from the trust store
     * 
     * Expected Results:
     * 
     * An exception is thrown on the request
     * 
     */
    @AllowedFFDC(value = { "com.ibm.wsspi.channelfw.exception.ChannelException", "java.lang.IllegalArgumentException" })
    @Test
    public void testWebClientAuthCodeCertUserNotInTrust() throws Exception {
        testOPServer.reconfigServer(server_cert_user_not_in_trust_config, _testName);

        List<validationData> expectations = vData.addSuccessStatusCodes();

        expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.EXCEPTION_MESSAGE, null, "Did not recieve the expected exception of: Connection refused", "javax.net.ssl.SSLHandshakeException", "Connection refused");
        WebConversation wc = new WebConversation();

        testOPServer.addIgnoredServerExceptions("CWPKI0807W", "CWPKI0033E", "CWPKI0809W");
        
        genericOP(_testName, wc, testSettings, Constants.SUBMIT_ACTIONS, expectations);

        return;
    }

    // no user passed as parm in request, so no need for a test verifying behavior in a mismatch with user in cert

}
