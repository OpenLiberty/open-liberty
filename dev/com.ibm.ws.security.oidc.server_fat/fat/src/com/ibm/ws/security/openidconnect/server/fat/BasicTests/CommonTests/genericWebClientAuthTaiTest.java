/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.server.fat.BasicTests.CommonTests;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class genericWebClientAuthTaiTest extends CommonTest {

    protected TestSettings setDefaultTaiSettings() throws Exception {
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientName("client01");
        updatedTestSettings.setClientID("client01");
        updatedTestSettings.setClientSecret("secret1234");
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setAuthorizeEndpt(eSettings.assembleEndpoint(testOPServer.getHttpsString(), Constants.ENDPOINT_TYPE, testSettings.getConfigSample(),
                                                                         Constants.AUTHORIZE_ENDPOINT));
        updatedTestSettings.setTokenEndpt(eSettings.assembleEndpoint(testOPServer.getHttpsString(), Constants.ENDPOINT_TYPE, testSettings.getConfigSample(),
                                                                     Constants.TOKEN_ENDPOINT));
        updatedTestSettings.setProtectedResource(eSettings.assembleProtectedResource(testOPServer.getHttpsString(), testSettings.getConfigTAI(), Constants.SNIFFING));
        return updatedTestSettings;
    }

    // Copy OidcTAI files into their proper locations in the build.image
    static protected void copyTaiFiles(String serverDir) throws Exception {
        LibertyServer aTestServer = LibertyServerFactory.getLibertyServer(serverDir);
        if (JakartaEEAction.isEE9OrLaterActive()) {
            aTestServer.copyFileToLibertyInstallRoot("lib", "lib/com.ibm.ws.security.tai_2.0.jar");
            aTestServer.copyFileToLibertyInstallRoot("lib", "lib/com.ibm.ws.security.tai.sample_2.0.jar");
            aTestServer.copyFileToLibertyInstallRoot("lib/features", "lib/features/oidcTai-2.0.mf");
            aTestServer.copyFileToLibertyInstallRoot("lib/features", "lib/features/sampleTai-2.0.mf");
        } else {
            aTestServer.copyFileToLibertyInstallRoot("lib", "lib/com.ibm.ws.security.tai_1.0.jar");
            aTestServer.copyFileToLibertyInstallRoot("lib", "lib/com.ibm.ws.security.tai.sample_1.0.jar");
            aTestServer.copyFileToLibertyInstallRoot("lib/features", "lib/features/oidcTai-1.0.mf");
            aTestServer.copyFileToLibertyInstallRoot("lib/features", "lib/features/sampleTai-1.0.mf");
        }
        aTestServer.copyFileToLibertyInstallRoot("lib/features", "lib/features/securitylibertyinternals-1.0.mf");
        aTestServer.copyFileToLibertyInstallRoot("lib/features/l10n", "lib/features/l10n/customTai.properties");
        aTestServer.copyFileToLibertyInstallRoot("lib/features/l10n", "lib/features/l10n/sampleTai.properties");
    }

    /**
     * TestDescription
     * Brief:
     * This test is similar to testPublicClientAuthCodeBasicFlow
     * Except:
     * 1) It sets up an TAI (oidcTAI) to handle the login.
     * (No user-Login handling needed)
     * 2) It sets up scope and preAuthorizedScope to make the scopes, which are openid and profile, authorized automatically
     * (No consent form handling needed, neither)
     * It directly responds with access_token, and id_token if oidc, after we submit the client.jsp.
     *
     * Detail:
     * This test case performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code" for a client. In this scenario,
     * the client uses the authorization server as an intermediary to obtain the
     * authorization code and then uses this authorization code to request
     * access token from the token endpoint. The authorization server
     * authenticates the resource owner through TAI(OidcTAI) before issuing the authorization code
     * which is redirected to the client. In this scenario, the scope and preAuthorizedScope
     * is set up, so the resource owner does not receive the
     * consent form from the authorization server.
     * The test verifies that the Oauth code flow, using the authorization grant type of
     * "authorization code" works correctly for a web client.
     *
     */
    @Mode(TestMode.LITE)
    // not needed
    @Test
    public void testWebClientAuthCodeTai() throws Exception {
        testOPServer.reconfigServer("server_tai.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = setDefaultTaiSettings();

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code",
                                            null,
                                            Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                            Constants.RECV_FROM_TOKEN_ENDPOINT);
        String providerType = updatedTestSettings.getProviderType();
        // add generic id_token expectations
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, providerType, Constants.SUBMIT_TO_AUTH_SERVER, updatedTestSettings);
        // add generic response expectations
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, providerType, Constants.SUBMIT_TO_AUTH_SERVER, updatedTestSettings);
        genericOP(_testName, wc, updatedTestSettings, Constants.SUBMIT_ACTIONS, expectations);
    }

    /**
     * TestDescription
     * Brief:
     * This test is similar to the above testWebClientAuthCodeTai()
     * Except:
     * 1) It set up externalClaimNames in the openidConnectProvider
     * 2) The TAI (sampleTAI) is set up to handle the externalClaimNames
     * It directly responds with access_token, and id_token if oidc, after we submit the client.jsp.
     * And also the externalClaimNames will show up in the payload of IDToken if oidc.
     */
    @Mode(TestMode.LITE)
    // not needed
    @Test
    public void testWebClientAuthCodeTaiBlue() throws Exception {
        testOPServer.reconfigServer("server_tai_blue.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = setDefaultTaiSettings();

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code",
                                            null,
                                            Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                            Constants.RECV_FROM_TOKEN_ENDPOINT);
        String providerType = updatedTestSettings.getProviderType();
        // add generic id_token expectations

        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, providerType, Constants.SUBMIT_TO_AUTH_SERVER, updatedTestSettings);

        if (providerType.equals(Constants.OIDC_OP)) { // OAuth2 does not contain id_token
            // let's check the externalClaimNames in the payload of id_token
            expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.RESPONSE_ID_TOKEN,
                                                Constants.STRING_MATCHES, "Did not get claim username as user1", "username", "user1");
            //email="bluemix54321@austin.ibm.com"
            expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.RESPONSE_ID_TOKEN,
                                                Constants.STRING_MATCHES, "Did not get claim email as bluemix54321@austin.ibm.com", "email", "bluemix54321@austin.ibm.com");
            //site="faked.yahoo.com"
            expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.RESPONSE_ID_TOKEN,
                                                Constants.STRING_MATCHES, "Did not get claim site as faked.yahoo.com", "site", "faked.yahoo.com");
            //tenant="IDoNotKnow"
            expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.RESPONSE_ID_TOKEN,
                                                Constants.STRING_MATCHES, "Did not get claim tenant as IDoNotKnow", "tenant", "IDoNotKnow");
        }
        // add generic response expectations
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, providerType, Constants.SUBMIT_TO_AUTH_SERVER, updatedTestSettings);
        genericOP(_testName, wc, updatedTestSettings, Constants.SUBMIT_ACTIONS, expectations);
    }
}
