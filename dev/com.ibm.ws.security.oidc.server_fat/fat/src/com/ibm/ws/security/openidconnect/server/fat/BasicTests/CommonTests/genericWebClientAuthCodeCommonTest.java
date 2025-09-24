/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.DerbyUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MongoDBUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings.StoreType;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
public class genericWebClientAuthCodeCommonTest extends CommonTest {

    //private static final Class<?> thisClass = genericWebClientAuthCodeDerbyTest.class;

    private static final Class<?> thisClass = genericWebClientAuthCodeCommonTest.class;

    // tried to add code to bypass LDAPUtils, but, that's called from the FATSuite
    // and some of the test classes do need LDAP, so, can't remove it from the suite

    /**
     * If we have several tests that check the secret type of the preloaded database users,
     * we could make this method an @Before test. Since we only have one test that currently
     * depends on the starting "from scratch" on preloaded database clients, this method will
     * be called directly.
     *
     * @throws Exception
     */
    //@Before
    public void refreshDatabaseUsers() throws Exception {
        Log.info(thisClass, "refreshDatabaseUsers", "calling setup");

        if (testOPServer.isUsingMongoDB()) {
            MongoDBUtils.clearClientEntries(testOPServer.getHttpString(), testOPServer.getHttpDefaultPort());
        } else if (testOPServer.isUsingDerby()) {
            DerbyUtils.clearClientEntries(testOPServer.getHttpString(), testOPServer.getHttpDefaultPort());
        }
    }

    /**
     * TestDescription:
     *
     * This test case performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code" . In this scenario, the OAuth client is
     * registered using database, instead of using XML file. Derby database or custom
     * store is used for storing the registered clients. In this scenario, the autoauthz
     * parameter is set to true, so the resource owner does not receive the
     * consent form from the authorization server. The test verifies that the
     * Oauth code flow, using the authorization grant type of "authorization
     * code" works correctly with JDBC database client.
     *
     */
    @Test
    @AllowedFFDC({ "java.sql.SQLRecoverableException", "java.sql.SQLException" })
    //Only for OIDC
    public void testAuthCodeBasicFlow() throws Exception {
        refreshDatabaseUsers(); // dump and recreate database users so the secret type starts as plain

        String compID = "OAuthConfigDerby";

        verifySecretType(testSettings.getClientID(), "plain", compID);

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null,
                                            Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                            Constants.RECV_FROM_TOKEN_ENDPOINT);
        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Could not invoke protected application", null, Constants.APP_TITLE);
        // Response should not have an ltpa token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null,
                                            "false");

        // verify that refresh_token is issued
        expectations = vData.addTokenInResponseExpectation(expectations, Constants.PERFORM_LOGIN, Constants.REFRESH_TOKEN_GRANT_TYPE);

        genericOP(_testName, wc, testSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

        if (testSettings.isHash()) { // secret should be upgraded to hash after validateClient
            verifySecretType(testSettings.getClientID(), "hash", compID);
        } else {
            verifySecretType(testSettings.getClientID(), "plain", compID);
        }

    }

    /**
     * TestDescription:
     *
     * This test case performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code" . In this scenario, the OAuth client is
     * registered using database, instead of using XML file. Derby database or custom
     * store is used for storing the registered clients. In this scenario, the autoauthz
     * parameter is set to true, so the resource owner does not receive the
     * consent form from the authorizarion server. The test verifies that the
     * Oauth code flow, using the authorization grant type of "authorization
     * code" works correctly with JDBC database client.
     *
     */
    @Test
    @AllowedFFDC({ "java.sql.SQLRecoverableException", "java.sql.SQLException" })
    //Only for Oauth
    public void testAuthCodeBasicFlowAllAuthOAuthRoles() throws Exception {
        testOPServer.reconfigServer(getXMLForAuthCodeBasicFlowAllAuthOAuthRoles(), _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null,
                                            Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                            Constants.RECV_FROM_TOKEN_ENDPOINT);
        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Could not invoke protected application", null, Constants.APP_TITLE);
        // Response should not have an ltpa token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null,
                                            "false");

        genericOP(_testName, wc, testSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

    }

    private String getXMLForAuthCodeBasicFlowAllAuthAuthRoles() {
        String xmlName = "xml_not_found";

        if (testSettings.getStoreType() == StoreType.DATABASE) {
            if (testSettings.isHash()) {
                xmlName = "server_derby_auth_roles_all_auth.xml";
            } else {
                xmlName = "server_derby_auth_roles_all_auth_xor.xml";
            }
        } else if (testSettings.getStoreType() == StoreType.CUSTOM) {
            if (testSettings.isHash()) {
                xmlName = "server_customstore_auth_roles_all_auth.xml";
            } else {
                xmlName = "server_customstore_auth_roles_all_auth_xor.xml";
            }
        } else if (testSettings.getStoreType() == StoreType.CUSTOMBELL) {
            if (testSettings.isHash()) {
                xmlName = "server_customstore_bell_auth_roles_all_auth.xml";
            } else {
                fail("No config for running this test with hash and a bell custom store");
            }
        }
        return xmlName;
    }

    private String getXMLForAuthCodeBasicFlowAllAuthOAuthAndAuthRoles() {
        String xmlName = "xml_not_found";
        if (testSettings.getStoreType() == StoreType.DATABASE) {
            if (testSettings.isHash()) {
                xmlName = "server_derby_oauth_and_auth_roles_all_auth.xml";
            } else {
                xmlName = "server_derby_oauth_and_auth_roles_all_auth_xor.xml";
            }
        } else if (testSettings.getStoreType() == StoreType.CUSTOM) {
            if (testSettings.isHash()) {
                xmlName = "server_customstore_oauth_and_auth_roles_all_auth.xml";
            } else {
                xmlName = "server_customstore_oauth_and_auth_roles_all_auth_xor.xml";
            }
        } else if (testSettings.getStoreType() == StoreType.CUSTOMBELL) {
            if (testSettings.isHash()) {
                xmlName = "server_customstore_bell_oauth_and_auth_roles_all_auth.xml";
            } else {
                fail("No config for running this test with hash and a bell custom store");
            }
        }
        return xmlName;
    }

    private String getXMLForAuthCodeBasicFlowAllAuthOAuthRoles() {
        String xmlName = "xml_not_found";
        if (testSettings.getStoreType() == StoreType.DATABASE) {
            if (testSettings.isHash()) {
                xmlName = "server_derby_oauth_roles_all_auth.xml";
            } else {
                xmlName = "server_derby_oauth_roles_all_auth_xor.xml";
            }
        } else if (testSettings.getStoreType() == StoreType.CUSTOM) {
            if (testSettings.isHash()) {
                xmlName = "server_customstore_oauth_roles_all_auth.xml";
            } else {
                xmlName = "server_customstore_oauth_roles_all_auth_xor.xml";
            }
        } else if (testSettings.getStoreType() == StoreType.CUSTOMBELL) {
            if (testSettings.isHash()) {
                xmlName = "server_customstore_bell_oauth_roles_all_auth.xml";
            } else {
                fail("No config for running this test with hash and a bell custom store");
            }
        }
        return xmlName;
    }

    /**
     * TestDescription:
     *
     * This test case performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code" . In this scenario, the OAuth client is
     * registered using database, instead of using XML file. Derby database or custom
     * store is used for storing the registered clients. In this scenario, the autoauthz
     * parameter is set to true, so the resource owner does not receive the
     * consent form from the authorizarion server. The test verifies that the
     * Oauth code flow, using the authorization grant type of "authorization
     * code" works correctly with JDBC database client.
     *
     */
    @Test
    @AllowedFFDC({ "java.sql.SQLRecoverableException", "java.sql.SQLException" })
    public void testAuthCodeBasicFlowAllAuthAuthRoles() throws Exception {
        testOPServer.reconfigServer(getXMLForAuthCodeBasicFlowAllAuthAuthRoles(), _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null,
                                            Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                            Constants.RECV_FROM_TOKEN_ENDPOINT);
        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Could not invoke protected application", null, Constants.APP_TITLE);
        // Response should not have an ltpa token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null,
                                            "false");

        genericOP(_testName, wc, testSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

    }

    /**
     * TestDescription:
     *
     * This test case performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code" . In this scenario, the OAuth client is
     * registered using database, instead of using XML file. Derby database or custom
     * store is used for storing the registered clients. In this scenario, the autoauthz
     * parameter is set to true, so the resource owner does not receive the
     * consent form from the authorizarion server. The test verifies that the
     * Oauth code flow, using the authorization grant type of "authorization
     * code" works correctly with JDBC database client.
     *
     */
    @Test
    @AllowedFFDC({ "java.sql.SQLRecoverableException", "java.sql.SQLException" })
    public void testAuthCodeBasicFlowAllAuthOAuthAndAuthRoles() throws Exception {
        testOPServer.reconfigServer(getXMLForAuthCodeBasicFlowAllAuthOAuthAndAuthRoles(), _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null,
                                            Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                            Constants.RECV_FROM_TOKEN_ENDPOINT);
        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Could not invoke protected application", null, Constants.APP_TITLE);
        // Response should not have an ltpa token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null,
                                            "false");

        genericOP(_testName, wc, testSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

    }

    /**
     * TestDescription:
     *
     * This is a negative test case for the authorization grant type of
     * "authorization code", using OAuth provider that uses JDBC database for
     * storing registered clients. In this scenario, a non-registered client
     * tries to obtain authorization code from the authorization server. Since
     * the client is not registered, the request is expected to be rejected with
     * an appropriate exception. This test verifies that the OAuth authorization
     * server will reject any unregistered OAuth client if it tries to obtain
     * access token.
     *
     */
    @Test
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidClientException" })
    @AllowedFFDC({ "java.sql.SQLRecoverableException", "java.sql.SQLException" })
    public void testAuthCodeJDBCUnRegClient() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientName("unreg02");
        updatedTestSettings.setClientID("unreg02");
        updatedTestSettings
                        .setTokenEndpt(eSettings.assembleEndpoint(testOPServer.getHttpsString(), Constants.ENDPOINT_TYPE, testSettings.getConfigDerby(), Constants.TOKEN_ENDPOINT));
        updatedTestSettings.setProtectedResource(eSettings.assembleProtectedResource(testOPServer.getHttpsString(), testSettings.getConfigTAI(), Constants.SSODEMO));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive "
                                                                                                                                               + updatedTestSettings.getClientID()
                                                                                                                                               + " not found response",
                                            null,
                                            Constants.CLIENT_COULD_NOT_BE_FOUND);

        genericOP(_testName, wc, updatedTestSettings, Constants.SUBMIT_ACTIONS, expectations);

    }

    /***
     * Test Purpose: Good end to end flow - With an OAuth provider, ensure that
     * there is no id_token in the response. With an OIDC provider, ensure that
     * the id_token is in the response. Also verify the content of the id_token
     * - all required parms are included and make sure that the values in those
     * required parms are correct. Status code returned should be 200.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC({ "java.sql.SQLRecoverableException", "java.sql.SQLException" })
    public void testWebClientDerby_validateIDToken() throws Exception {
        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRealm("BasicRealm");

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null,
                                            Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                            Constants.RECV_FROM_TOKEN_ENDPOINT);
        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Could not invoke protected application", null, Constants.APP_TITLE);
        // Response should not have an ltpa token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null,
                                            "false");
        // add generic id_token expectations
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, updatedTestSettings);

        // verify that refresh_token is issued
        expectations = vData.addTokenInResponseExpectation(expectations, Constants.PERFORM_LOGIN, Constants.REFRESH_TOKEN_GRANT_TYPE);

        genericOP(_testName, wc, testSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

    }

    /***
     * Test Purpose: Specify scope without "openid". This is allowed for both
     * OAuth and OIDC. For OAuth - it does not return an id_token anyway. OIDC
     * will allow the omission, but will not return the id_token. The Status
     * code returned should be 200.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "java.sql.SQLRecoverableException", "java.sql.SQLException" })
    public void testWebClientDerby_missingOpenidInScope() throws Exception {
        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("scope1 scope2");

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null,
                                            Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                            Constants.RECV_FROM_TOKEN_ENDPOINT);

        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive login page", null,
                                            Constants.LOGIN_PROMPT);

        // make sure that id_token is NOT in the response
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_ID_TOKEN, Constants.STRING_CONTAINS,
                                            "Token validate response found the id_token in the response and should not have", Constants.ID_TOKEN_KEY, Constants.NOT_FOUND);
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, expectations);

    }

    /**
     * TestDescription:
     *
     * This test case performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code" . In this scenario, the OAuth client is
     * registered using database, instead of using XML file. Derby database is
     * used for storing the registered clients. In this scenario, the autoauthz
     * parameter is set to true, so the resource owner does not receive the
     * consent form from the authorizarion server. The test verifies that the
     * Oauth code flow, using the authorization grant type of "authorization
     * code" works correctly with JDBC database client.
     *
     */
    @Test
    @AllowedFFDC({ "java.sql.SQLRecoverableException", "java.sql.SQLException" })
    public void testAuthCodeBasicFlowWithBasicAuth() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes();

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null,
                                            Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                            Constants.RECV_FROM_TOKEN_ENDPOINT);
        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Could not invoke protected application", null, Constants.APP_TITLE);
        // Response should not have an ltpa token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null,
                                            "false");
        genericOP(_testName, wc, testSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS_WITH_BASIC_AUTH, expectations);

    }

    /**
     * TestDescription:
     *
     */
    @Test
    @AllowedFFDC({ "java.sql.SQLRecoverableException", "java.sql.SQLException" })
    public void testRefreshTokenNotIssuedWithDerby() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("scope1 scope2");
        updatedTestSettings.setClientName("dclient03");
        updatedTestSettings.setClientID("dclient03");

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes();

        // verify that refresh_token is not issued
        expectations = vData.addNoTokenInResponseExpectation(expectations, Constants.PERFORM_LOGIN, Constants.REFRESH_TOKEN_GRANT_TYPE);

        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, expectations);

    }

    /**
     * TestDescription:
     *
     */
    @Test
    @AllowedFFDC({ "java.sql.SQLRecoverableException", "java.sql.SQLException" })
    public void testRefreshTokenNotIssuedWithDerbyDefaultGrantTypes() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("scope1 scope2");
        updatedTestSettings.setClientName("dclient04");
        updatedTestSettings.setClientID("dclient04");

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes();

        // verify that refresh_token is not issued
        expectations = vData.addNoTokenInResponseExpectation(expectations, Constants.PERFORM_LOGIN, Constants.REFRESH_TOKEN_GRANT_TYPE);

        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, expectations);

    }

    @Test
    //Only for OIDC
    public void testAuthCodeBasicFlowXORToHashSecret() throws Exception {
        String clientID = "xorClient";
        String xorSecret = "{xor}BxANDDo8LTorbg==";
        String compID = "OAuthConfigDerby";

        // Add user directly to the database
        addClient(clientID, xorSecret, compID, null, null, null, null);

        // Double check the secret is set as expected
        verifySecretType(clientID, "xor", compID);

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings ts = testSettings.copyTestSettings();
        ts.setClientID(clientID);
        ts.setClientName(clientID);
        ts.setClientSecret("XORSecret1");

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null,
                                            Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                            Constants.RECV_FROM_TOKEN_ENDPOINT);
        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Could not invoke protected application", null, Constants.APP_TITLE);
        // Response should not have an ltpa token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null,
                                            "false");

        // verify that refresh_token is issued
        expectations = vData.addTokenInResponseExpectation(expectations, Constants.PERFORM_LOGIN, Constants.REFRESH_TOKEN_GRANT_TYPE);

        genericOP(_testName, wc, ts, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

        if (testSettings.isHash()) { // confirm that after the validate with hasing enabled, the secret is now hashed.
            verifySecretType(clientID, "hash", compID);
        } else { // if hash is not enabled, we shouldn't change the secret
            verifySecretType(clientID, "xor", compID);
        }

    }

    @Test
    public void testAuthCodeBasicFlowHashSecret() throws Exception {

        boolean isFips = testOPServer.getServer().isFIPS140_3EnabledAndSupported();
        String clientID = "hashClient";
        // if the hashtype, salt, iterations or key length change, this hash of password "hashSecret" needs to be updated.
        String hashSecret = isFips ? "{hash}ARAAAAAUUEJLREYyV2l0aEhtYWNTSEE1MTIgAAM0UDAAAAAsSFN0RkRsVENaNHJKUVRoYlNkZFpHNXEwT3ZmMm96RGxySnRUMUFZaVVoTT1AAAAAIKXbFUzZRRrAc1crX0XHX/jTvSvrRK3R3FyVKMcu5GKw" : "{hash}ARAAAAAUUEJLREYyV2l0aEhtYWNTSEE1MTIgAAAIAFAAAAAgMAAAACxIU3RGRGxUQ1o0ckpRVGhiU2RkWkc1cTBPdmYyb3pEbHJKdFQxQVlpVWhNPUAAAAAE/u6cVw==";
        Integer hashLen = isFips ? 256 : 32;
        String compID = "OAuthConfigDerby";
        String salt = "HStFDlTCZ4rJQThbSddZG5q0Ovf2ozDlrJtT1AYiUhM=";
        String algorithm = "PBKDF2WithHmacSHA512";
        Integer iterations = isFips ? 210000 : 2048;
        // Add user directly to the database
        addClient(clientID, hashSecret, compID, salt, algorithm, hashLen, iterations); // using default iterations and keys

        // Double check the secret is set as expected
        verifySecretType(clientID, "hash", compID);

        if (testSettings.isHash() && (testOPServer.isUsingMongoDB() || testOPServer.isUsingDerby())) {
            String msg = null;
            String alg = null;

            if (testOPServer.isUsingMongoDB()) {
                msg = MongoDBUtils.checkIteration(testOPServer.getHttpString(), testOPServer.getHttpDefaultPort(),
                                                  clientID, compID);
                alg = MongoDBUtils.checkAlgorithm(testOPServer.getHttpString(), testOPServer.getHttpDefaultPort(),
                                                  clientID, compID);
            } else {
                msg = DerbyUtils.checkIteration(testOPServer.getHttpString(), testOPServer.getHttpDefaultPort(),
                                                clientID, compID);
                alg = DerbyUtils.checkAlgorithm(testOPServer.getHttpString(), testOPServer.getHttpDefaultPort(),
                                                clientID, compID);
            }

            assertNotNull("Servlet should have returned an iteration type for " + clientID, msg);
            assertEquals("Iteration is incorrect in the database for client " + clientID, "" + iterations, msg);

            assertNotNull("Servlet should have returned an algorithm type for " + clientID, alg);
            assertEquals("Algorithm is incorrect in the database for client " + clientID, algorithm, alg);
        }

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings ts = testSettings.copyTestSettings();
        ts.setClientID(clientID);
        ts.setClientName(clientID);
        ts.setClientSecret("hashSecret");

        List<validationData> expectations = null;

        if (testSettings.isHash()) { // expect a normal login on test with hashing enabled
            // expect good (200) status codes for all steps
            expectations = vData.addSuccessStatusCodes(null);

            // Check if we got authorization code
            expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code",
                                                null,
                                                Constants.RECV_AUTH_CODE);
            // Check if we got the access token
            expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                                Constants.RECV_FROM_TOKEN_ENDPOINT);
            // Make sure we get to the app
            expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                "Could not invoke protected application", null, Constants.APP_TITLE);
            // Response should not have an ltpa token
            expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not",
                                                null,
                                                "false");

            // verify that refresh_token is issued
            expectations = vData.addTokenInResponseExpectation(expectations, Constants.PERFORM_LOGIN, Constants.REFRESH_TOKEN_GRANT_TYPE);
        } else { // log with a hashed password will fail with xor enabled
            expectations = vData.addSuccessStatusCodes(null, Constants.LOGIN_USER);
            expectations = vData.addResponseStatusExpectation(expectations, Constants.LOGIN_USER, Constants.FORBIDDEN_STATUS);
            expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
            expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                "Did Not get HTTP 403 Authentication exception ", null, Constants.AUTHORIZATION_FAILED);
        }

        genericOP(_testName, wc, ts, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

        // needs to match Strings set in oAuth20MongoSetup, confirm that after the validate, the secret is still hashed.
        verifySecretType(clientID, "hash", compID);

    }

    private void addClient(String clientID, String hashSecret, String compID, String salt, String alg, Integer hashLen, Integer iterations) throws Exception {
        if (testOPServer.isUsingMongoDB()) {
            MongoDBUtils.addMongoDBEntry(testOPServer.getHttpString(), testOPServer.getHttpDefaultPort(), clientID,
                                         hashSecret, compID, salt, alg, hashLen, iterations);
        } else {
            DerbyUtils.addDerbyEntry(testOPServer.getHttpString(), testOPServer.getHttpDefaultPort(), clientID,
                                     hashSecret, compID, salt, alg, hashLen, iterations);
        }
    }

    public void verifySecretType(String clientId, String expectedType, String compID) throws Exception {
        String msg = null;
        if (testOPServer.isUsingMongoDB()) {
            msg = MongoDBUtils.checkSecretType(testOPServer.getHttpString(), testOPServer.getHttpDefaultPort(),
                                               clientId, compID);
        } else {
            msg = DerbyUtils.checkSecretType(testOPServer.getHttpString(), testOPServer.getHttpDefaultPort(), clientId,
                                             compID);
        }
        assertNotNull("Servlet should have returned a secret type", msg);
        assertEquals("Secret type is incorrect in the database.", expectedType, msg);
    }

}