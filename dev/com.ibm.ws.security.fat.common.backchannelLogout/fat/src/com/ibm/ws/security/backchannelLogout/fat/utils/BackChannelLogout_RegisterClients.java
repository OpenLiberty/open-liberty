/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package com.ibm.ws.security.backchannelLogout.fat.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.TestHelpers;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonMessageTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTestHelpers;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonValidationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

/**
 * This class supplies support methods to register OAuth client for the back channel logout tests.
 */

public class BackChannelLogout_RegisterClients {

    protected static Class<?> thisClass = BackChannelLogout_RegisterClients.class;

    protected static EndpointSettings eSettings = new EndpointSettings();
    protected static CommonTestTools cttools = new CommonTestTools();
    protected static ValidationData vData = new ValidationData();
    protected static CommonMessageTools msgUtils = new CommonMessageTools();
    protected static CommonTestHelpers helpers = new CommonTestHelpers();
    protected static CommonValidationTools validationTools = new CommonValidationTools();
    protected static TestHelpers testHelpers = new TestHelpers();

    protected static String registrationUri;

    protected static final String ADMIN_USER = "testuser";
    protected static final String ADMIN_PASS = "testuserpwd";

    protected static TestServer testOPServer;
    protected static TestServer clientServer;
    protected static TestServer clientServer2;
    protected static TestServer genericTestServer;

    public BackChannelLogout_RegisterClients(TestServer OP, TestServer client1, TestServer client2) {
        Log.info(thisClass, "constructor", "Starting to save servers");
        testOPServer = OP;
        clientServer = client1;
        clientServer2 = client2;
        Log.info(thisClass, "constructor", "Finished saving servers");

    }

    public BackChannelLogout_RegisterClients(TestServer OP, TestServer client1, TestServer client2, TestServer rs) {
        this(OP, client1, client2);

        Log.info(thisClass, "constructor", "Adding RS server");
        genericTestServer = rs;
        Log.info(thisClass, "constructor", "Finished saving servers");

    }

    protected List<endpointSettings> setRequestHeaders() throws Exception {

        msgUtils.printMethodName("setRequestHeaders");

        List<endpointSettings> headers = new ArrayList<EndpointSettings.endpointSettings>();
        endpointSettings authorization = createBasicAuthenticationHeader(ADMIN_USER, ADMIN_PASS);
        headers = eSettings.addEndpointSettings(headers, "Accept", "application/json");
        headers = eSettings.addEndpointSettings(headers, "Content-Type", "application/json");
        headers.add(authorization);
        return headers;
    }

    protected endpointSettings createBasicAuthenticationHeader(String clientID, String clientSecret) throws Exception {

        msgUtils.printMethodName("createBasicAuthenticationHeader");

        String basicAuth = cttools.buildBasicAuthCred(clientID, clientSecret);
        return new EndpointSettings().new endpointSettings("Authorization", basicAuth);
    }

    private JSONObject setRegistrationBody(String clientId, String bcl, String postRedirect, String[] redirectOverride, boolean appPasswordAllowed, boolean appTokenAllowed, boolean introspectTokens) {

        msgUtils.printMethodName("setRegistrationBody");
        String defaultSecret = "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger";

        JSONObject prefix = new JSONObject();

        prefix.put("client_id", clientId);
        prefix.put("client_name", clientId);

        prefix.put("enabled", true);
        prefix.put("_isEnabled", true);

        // if public client, set the public flag and don't configure a secret
        if (clientId.toLowerCase().contains("public")) {
            prefix.put("publicClient", true);
            if (clientId.toLowerCase().contains("withsecret")) {
                prefix.put("client_secret", defaultSecret);
            }
            // otherwise omit the secret
        } else {
            prefix.put("client_secret", defaultSecret);
        }

        // default config value for httpsRequired is true, so, only need to set the value when false
        if (clientId.toLowerCase().contains("httpsRequired")) {
            if (clientId.toLowerCase().contains("httpsRequired_false")) {
                prefix.put("httpsRequired", "false");
            }
        }

        // need to update this to allow a client without a bcl
        if (bcl != null) {
            prefix.put("backchannel_logout_uri", bcl);
            prefix.put("backchannel_logout_supported", "true");
            prefix.put("backchannel_logout_session_supported", "true");
        }

        if (postRedirect != null) {
            JSONArray logoutUris = new JSONArray();
            logoutUris.add(postRedirect);
            prefix.put("post_logout_redirect_uris", logoutUris);
        }

        JSONArray redirectUris = new JSONArray();
        if (redirectOverride != null) {
            for (String extension : redirectOverride) {
                redirectUris = updateRedirects(redirectUris, clientId, extension);
            }
        } else {
            redirectUris = updateRedirects(redirectUris, clientId, null);
        }
        prefix.put("redirect_uris", redirectUris);

        String[] grants = { "authorization_code", "implicit", "refresh_token", "client_credentials", "password", "urn:ietf:params:oauth:grant-type:jwt-bearer" };
        JSONArray grantTypes = new JSONArray();
        for (String grant : grants) {
            grantTypes.add(grant);
        }
        prefix.put("grant_types", grantTypes);

        String[] rspTypes = { "code", "token", "id_token token" };
        JSONArray responseTypes = new JSONArray();
        for (String r : rspTypes) {
            responseTypes.add(r);
        }
        prefix.put("response_types", responseTypes);

        prefix.put("scope", "ALL_SCOPES");
        prefix.put("enabled", "true");

        prefix.put("appPasswordAllowed", appPasswordAllowed);
        prefix.put("appTokenAllowed", appTokenAllowed);
        //        prefix.put("introspectTokens", introspectTokens);
        prefix.put("introspect_tokens", introspectTokens);

        return prefix;
    }

    public JSONArray updateRedirects(JSONArray redirectUris, String clientId, String extension) {
        String redirect = clientId;

        if (extension != null) {
            redirect = clientId + "_" + extension;
        }
        redirectUris.add(clientServer.getServerHttpsString() + "/oidcclient/redirect/" + redirect);
        redirectUris.add(clientServer.getServerHttpsString() + "/ibm/api/social-login/redirect/" + redirect);
        if (clientServer2 != null) {
            redirectUris.add(clientServer2.getServerHttpsString() + "/oidcclient/redirect/" + redirect);
            redirectUris.add(clientServer2.getServerHttpsString() + "/ibm/api/social-login/redirect/" + redirect);
        }
        if (genericTestServer != null) {
            redirectUris.add(genericTestServer.getServerHttpsString() + "/oidcclient/redirect/" + redirect);
            redirectUris.add(genericTestServer.getServerHttpsString() + "/ibm/api/social-login/redirect/" + redirect);
        }
        return redirectUris;

    }

    public void registerClientWithDefaultBclAndExtraConfig(String provider, String clientId, String postRedirect, HashMap<String, String> extraConfig) throws Exception {

        String bcl = clientServer.getServerHttpsCanonicalString() + "/oidcclient/backchannel_logout/" + clientId;
        JSONObject prefix = setRegistrationBody(clientId, bcl, postRedirect, null, false, false, true);
        for (String i : extraConfig.keySet()) {
            prefix.put(i, extraConfig.get(i));
        }

        registerClient(provider, prefix, vData.addResponseStatusExpectation(null, Constants.INVOKE_REGISTRATION_ENDPOINT, Constants.CREATED_STATUS));

    }

    public void registerClientWithDefaultBcl(String provider, String clientId, String postRedirect) throws Exception {

        String bcl = clientServer.getServerHttpsCanonicalString() + "/oidcclient/backchannel_logout/" + clientId;
        registerClient(provider, clientId, bcl, postRedirect);
    }

    public void registerClient(String provider, String clientId, String bcl, String postRedirect) throws Exception {
        registerClient(provider, clientId, bcl, postRedirect, null);
    }

    public void registerClient(String provider, String clientId, String bcl, String postRedirect, String[] redirectOverride) throws Exception {
        registerClient(provider, clientId, bcl, postRedirect, redirectOverride, false, false, true);
    }

    public void registerClient(String provider, String clientId, String bcl, String postRedirect, String[] redirectOverride, boolean appPasswordAllowed, boolean appTokenAllowed, boolean introspectTokens) throws Exception {
        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_REGISTRATION_ENDPOINT, Constants.CREATED_STATUS);
        registerClient(provider, clientId, bcl, postRedirect, redirectOverride, appPasswordAllowed, appTokenAllowed, introspectTokens, expectations);

    }

    public JSONObject registerClient(String provider, String clientId, String bcl, String postRedirect, String[] redirectOverride, boolean appPasswordAllowed, boolean appTokenAllowed, boolean introspectTokens, List<validationData> expectations) throws Exception {

        //        String thisMethod = "registerClient";
        //        msgUtils.printMethodName(thisMethod);
        //
        //        WebClient webClient = TestHelpers.getWebClient(true);
        //
        //        List<endpointSettings> headers = setRequestHeaders();
        //
        // build a default, standard registrationBody
        JSONObject prefix = setRegistrationBody(clientId, bcl, postRedirect, redirectOverride, appPasswordAllowed, appTokenAllowed, introspectTokens);
        registerClient(provider, prefix, expectations);
        return prefix;
        //
        //        String registrationEndpoint = testOPServer.getServerHttpsString() + "/oidc/endpoint/" + provider + "/registration";
        //        Log.info(thisClass, "registerClient", "registrationEndpoint: " + registrationEndpoint);
        //        Log.info(thisClass, "registerClient", "Client registration body values: " + prefix.toString());
        //
        //        waitTillMongoDbReady(provider);
        //
        //        helpers.invokeEndpointWithBody("testSetup", webClient, registrationEndpoint, Constants.POSTMETHOD, Constants.INVOKE_REGISTRATION_ENDPOINT, null, headers, expectations, prefix.toString());
    }

    public void registerClient(String provider, JSONObject prefix, List<validationData> expectations) throws Exception {

        String thisMethod = "registerClient";
        msgUtils.printMethodName(thisMethod);

        WebClient webClient = TestHelpers.getWebClient(true);

        List<endpointSettings> headers = setRequestHeaders();

        String registrationEndpoint = testOPServer.getServerHttpsString() + "/oidc/endpoint/" + provider + "/registration";
        Log.info(thisClass, "registerClient", "registrationEndpoint: " + registrationEndpoint);
        Log.info(thisClass, "registerClient", "Client registration body values: " + prefix.toString());

        waitTillMongoDbReady(provider);

        helpers.invokeEndpointWithBody("testSetup", webClient, registrationEndpoint, Constants.POSTMETHOD, Constants.INVOKE_REGISTRATION_ENDPOINT, null, headers, expectations, prefix.toString());

    }

    class Looper {

        int currentRetry = 1;
        boolean retry = true;

    }

    public void waitTillMongoDbReady(String provider) throws Exception {

        String thisMethod = "waitTillMongoDbReady";
        // use showClients to determine if mongo is ready
        int maxRetries = 10;

        Looper looper = new Looper();

        while (looper.retry) {
            try {
                // don't use the normal validation tools to check the status code - it'll terminate processing if it's not what's expected
                Page response = showClients(provider, null);
                int statusCode = AutomationTools.getResponseStatusCode(response);
                if (statusCode == Constants.OK_STATUS) {
                    looper.retry = false;
                } else {
                    updateRetry(looper, maxRetries);
                }
            } catch (Exception e) {
                Log.error(thisClass, thisMethod, e);
                updateRetry(looper, maxRetries);
            }

        }
        Log.info(thisClass, thisMethod, "Tried to show registered clients " + Integer.toString(looper.currentRetry) + " times.");

    }

    public void updateRetry(Looper looper, int maxRetries) throws Exception {

        if (looper.currentRetry <= maxRetries) {
            looper.currentRetry += 1;
            looper.retry = true;
            testHelpers.testSleep(5);
        } else {
            Log.info(thisClass, "waitTillMongoDbReady", "Max attempts to use Mongo made - we may see issues trying to register the test clients");
            looper.retry = false;
        }

    }

    public Page showClients(String provider, List<validationData> expectations) throws Exception {

        String thisMethod = "showClients";
        msgUtils.printMethodName(thisMethod);

        WebClient webClient = TestHelpers.getWebClient(true);

        List<endpointSettings> headers = setRequestHeaders();

        String registrationEndpoint = testOPServer.getServerHttpsString() + "/oidc/endpoint/" + provider + "/registration";
        Log.info(thisClass, "showClients", "registrationEndpoint: " + registrationEndpoint);

        Page response = helpers.invokeEndpointWithBody("testSetup", webClient, registrationEndpoint,
                Constants.GETMETHOD,
                Constants.INVOKE_REGISTRATION_ENDPOINT, null, headers,
                expectations, null);

        msgUtils.printMethodName("End: " + thisMethod);
        return response;
    }

    public void registerClientsForBasicBCLTests() throws Exception {

        Log.info(thisClass, "registerClientsForBasicBCLTests", "Setting up mongo clients");
        String clientHttpRoot = clientServer.getHttpString();
        String clientHttpsRoot = clientServer.getHttpsString();
        String jSessionPostRedirect = clientHttpRoot + "/backchannelLogoutTestApp/backChannelLogoutJSessionId";
        registerClientWithDefaultBcl("OidcConfigSample_mainPath", "bcl_mainPath_confClient", null);
        registerClientWithDefaultBcl("OidcConfigSample_mainPath", "bcl_mainPath_publicClient_withSecret", null);
        registerClientWithDefaultBcl("OidcConfigSample_mainPath", "bcl_mainPath_publicClient_withoutSecret", null);
        registerClientWithDefaultBcl("OidcConfigSample_mainPath", "variableIntrospectValidationEndpoint", null);
        registerClientWithDefaultBcl("OidcConfigSample_userClientTokenLimit", "bcl_userClientTokenLimit_Client", null);

        HashMap<String, String> extraConfig = new HashMap<String, String>();
        extraConfig.put("accessTokenCacheEnabled", "false");
        registerClientWithDefaultBclAndExtraConfig("OidcConfigSample_mainPath", "bcl_mainPath_accessTokenCacheEnabled_false_confClient", null, extraConfig);

        registerClientWithDefaultBcl("OidcConfigSample_mainPath", "bcl_mainPath_accessTokenInLtpaCookie_true_confClient", null);

        registerClient("OidcConfigSample_defaultBCLTimeout", "bcl_defaultBCLTimeout", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutSleep", jSessionPostRedirect);
        registerClient("OidcConfigSample_defaultBCLTimeout", "bcl_otherDefaultBCLTimeout", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutSleep", jSessionPostRedirect);

        registerClient("OidcConfigSample_shortBCLTimeout", "bcl_shortBCLTimeout", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutSleep", jSessionPostRedirect);
        registerClient("OidcConfigSample_shortBCLTimeout", "bcl_otherShortBCLTimeout", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutSleep", jSessionPostRedirect);

        registerClient("OidcConfigSample_invalidBCL", "bcl_invalidBCLUri", clientHttpsRoot + "/invalidUri", jSessionPostRedirect);
        registerClient("OidcConfigSample_invalidBCL", "bcl_omittedBCLUri", null, jSessionPostRedirect);
        registerClient("OidcConfigSample_invalidBCL", "bcl_returns400", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogout400", jSessionPostRedirect);
        registerClient("OidcConfigSample_invalidBCL", "bcl_returns501", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogout501", jSessionPostRedirect);
        registerClient("OidcConfigSample_invalidBCL", "bcl_logsMsg", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg", jSessionPostRedirect);

        registerClient("OidcConfigSample_logger1", "loggerClient1-1", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/loggerClient1-1", null);
        registerClient("OidcConfigSample_logger1", "loggerClient1-2", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/loggerClient1-2", null);
        registerClient("OidcConfigSample_logger1", "loggerClient1-3", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/loggerClient1-3", null);
        registerClient("OidcConfigSample_logger1", "loggerClient1-4", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/loggerClient1-4", null);

        registerClient("OidcConfigSample_logger2", "loggerClient2-1", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/loggerClient2-1", null);
        registerClient("OidcConfigSample_logger2", "loggerClient2-2", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/loggerClient2-2", null);
        registerClient("OidcConfigSample_logger2", "loggerClient2-3", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/loggerClient2-3", null);
        registerClient("OidcConfigSample_logger2", "loggerClient2-4", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/loggerClient2-4", null);

        registerClient("OidcConfigSample_logger3", "loggerClient3-1", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/loggerClient3-1", null);

        registerClient("OidcConfigSample_logger4", "loggerClient4-1", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/loggerClient4-1", null);

        registerClient("OidcConfigSample_useLogoutTokenForAccess", "useLogoutTokenForAccess", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutUri/useLogoutTokenForAccess", null, new String[] { Constants.INTROSPECTION_ENDPOINT, Constants.USERINFO_ENDPOINT });

        registerClient("OidcConfigSample_multiClientWithAndWithoutBCL", "bcl_client1", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/bcl_client1", null);
        registerClient("OidcConfigSample_multiClientWithAndWithoutBCL", "nobcl_client1", null, null);
        registerClient("OidcConfigSample_multiClientWithAndWithoutBCL", "bcl_client2", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/bcl_client2", null);

        registerClient("OidcConfigSample_checkDuplicateBCLCalls", "checkDupBcl_client1", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/checkDupBcl_client1", null);
        registerClient("OidcConfigSample_checkDuplicateBCLCalls", "checkDupBcl_client2", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/checkDupBcl_client2", null);

        registerClientWithDefaultBcl("OidcConfigSample_http_httpsRequired_true", "bcl_http_confClient_httpsRequired_true", null);
        registerClientWithDefaultBcl("OidcConfigSample_http_httpsRequired_false", "bcl_http_confClient_httpsRequired_false", null);
        // We can't register a "bad" client - the public client test will test for the proper behavior in this case
        //            registerClient("OidcConfigSample_http_httpsRequired_true", "bcl_http_publicClient", clientHttpRoot + "/oidcclient/backchannel_logout/bcl_http_publicClient_httpsRequired_true_withSecret", null);

        registerClient("OidcConfigSample_idTokenCacheEnabledFalse", "idTokenCacheEnabledFalseClient-1", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/idTokenCacheEnabledFalseClient-1", null);
        registerClient("OidcConfigSample_idTokenCacheEnabledFalse", "idTokenCacheEnabledFalseClient-2", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/idTokenCacheEnabledFalseClient-2", null);
        registerClient("OidcConfigSample_idTokenCacheEnabledFalse", "idTokenCacheEnabledFalseClient-3", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/idTokenCacheEnabledFalseClient-3", null);

        registerClient("OidcConfigSample_accessTokenCacheEnabledFalse", "accessTokenCacheEnabledFalseClient-1", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/accessTokenCacheEnabledFalseClient-1", null);
        registerClient("OidcConfigSample_accessTokenCacheEnabledFalse", "accessTokenCacheEnabledFalseClient-2", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/accessTokenCacheEnabledFalseClient-2", null);
        registerClient("OidcConfigSample_accessTokenCacheEnabledFalse", "accessTokenCacheEnabledFalseClient-3", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/accessTokenCacheEnabledFalseClient-3", null);

        registerClient("OidcConfigSample_appPasswords", "bcl_appPasswordsClient1", clientServer.getServerHttpsCanonicalString() + "/oidcclient/backchannel_logout/" + "bcl_appPasswordsClient1", null, null, true, false, true);
        registerClient("OidcConfigSample_appPasswords", "bcl_appPasswordsClient2", clientServer.getServerHttpsCanonicalString() + "/oidcclient/backchannel_logout/" + "bcl_appPasswordsClient2", null, null, true, false, true);

        registerClient("OidcConfigSample_appTokens", "bcl_appTokensClient1", clientServer.getServerHttpsCanonicalString() + "/oidcclient/backchannel_logout/" + "bcl_appTokensClient1", null, null, false, true, true);
        registerClient("OidcConfigSample_appTokens", "bcl_appTokensClient2", clientServer.getServerHttpsCanonicalString() + "/oidcclient/backchannel_logout/" + "bcl_appTokensClient2", null, null, false, true, true);

        //        registerClient("OidcConfigSample_appPasswords", "bcl_appPasswordsClient1", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/bcl_appPasswordsClient1", null, null, true, false, true);
        //        registerClient("OidcConfigSample_appPasswords", "bcl_appPasswordsClient2", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/bcl_appPasswordsClient2", null, null, true, false, true);
        //
        //        registerClient("OidcConfigSample_appTokens", "bcl_appTokensClient1", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/bcl_appTokensClient1", null, null, false, true, true);
        //        registerClient("OidcConfigSample_appTokens", "bcl_appTokensClient2", clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutLogMsg/bcl_appTokensClient2", null, null, false, true, true);

    }

    /**
     * Create the clients needed for the MultiServer back channel tests
     *
     * @throws Exception
     */
    public void registerClientsForMultiServerBCLTests() throws Exception {

        Log.info(thisClass, "registerClientsForMultiServerBCLTests", "Setting up mongo clients");
        String clientHttpsRoot = clientServer.getHttpsString();

        String bclEndpoint = clientHttpsRoot + "/backchannelLogoutTestApp/backChannelLogoutMultiServer";

        registerClient("OidcConfigSample_multiServer1", "bcl_multiServer_client1-1", bclEndpoint, null, null, false, false, true);
        registerClient("OidcConfigSample_multiServer1", "bcl_multiServer_client1-2", bclEndpoint, null, null, false, false, true);
        registerClient("OidcConfigSample_multiServer1", "bcl_multiServer_client1-3", bclEndpoint, null, null, false, false, true);
        registerClient("OidcConfigSample_multiServer2", "bcl_multiServer_client2-1", bclEndpoint, null, null, false, false, true);
        registerClient("OidcConfigSample_multiServer2", "bcl_multiServer_client2-2", bclEndpoint, null, null, false, false, true);
        registerClient("OidcConfigSample_multiServer2", "bcl_multiServer_client2-3", bclEndpoint, null, null, false, false, true);

    }
}
