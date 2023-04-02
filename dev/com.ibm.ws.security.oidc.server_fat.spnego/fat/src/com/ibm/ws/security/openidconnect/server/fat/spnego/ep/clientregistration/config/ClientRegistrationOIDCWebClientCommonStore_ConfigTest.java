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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.fat.spnego.ep.clientregistration.config;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth20.web.OAuth20Request.EndpointType;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.spnego.SpnegoOIDCConstants;
import com.ibm.ws.security.spnego.fat.config.InitClass;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@RunWith(FATRunner.class)
public class ClientRegistrationOIDCWebClientCommonStore_ConfigTest extends CommonClientRegistrationOIDC_ConfigTests {

    private static final Class<?> thisClass = ClientRegistrationOIDCWebClientCommonStore_ConfigTest.class;

    String defaultCompID = "OAuthConfigSample";

    @BeforeClass
    public static void setupBefore() throws Exception {
        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");

        if (InitClass.isRndHostName) {
            Log.info(thisClass, "setupBeforeTest", thisClass.getName() + " is not running because randomized hostname is used.");
            Assume.assumeTrue(false); //This disables this test class. None of the tests in the class will be run.
        } else {
            // add any additional messages that you want the "start" to wait for 
            // we should wait for any providers that this test requires
            List<String> extraMsgs = new ArrayList<String>();

            List<String> extraApps = new ArrayList<String>();

            testSettings = new TestSettings();
            testOPServer = commonSetUpRegistrationEP("com.ibm.ws.security.openidconnect.server-1.0_fat.endpoint.clientregistration", "server_clientregistration_oidc_customstore_no_spnego_config.xml", SpnegoOIDCConstants.OIDC_OP, extraApps, SpnegoOIDCConstants.DO_NOT_USE_DERBY, SpnegoOIDCConstants.USE_MONGODB, extraMsgs, null, null, true, true, Constants.ACCESS_TOKEN_KEY, Constants.X509_CERT, Constants.JUNIT_REPORTING, null, false, false, null, false, false, true, true);
            testOPServer.addIgnoredServerException("CWWKS4312E");
            testOPServer.addIgnoredServerException("CWWKS4313E");
            registrationUri = eSettings.assembleEndpoint(testOPServer.getHttpsString(), Constants.ENDPOINT_TYPE, Constants.OIDCCONFIGSAMPLE_APP, EndpointType.registration.name());
        }
    }

    @Mode(TestMode.LITE)
    @Test
    @ExpectedFFDC("com.ibm.oauth.core.api.error.OidcServerException")
    public void test_SPNEGO_OIDC_CreateClientWithEveryParamVariation1() throws Exception {
        WebClient webClient = getAndSaveWebClient(true);

        TestSettings mytest = testSettings.copyTestSettings();
        mytest.setRegistrationEndpt(testSettings.getRegistrationEndpt().replace("localhost", testOPServer.getServerCanonicalHostname()));

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_REGISTRATION_ENDPOINT, Constants.UNAUTHORIZED_STATUS);

        List<endpointSettings> headers = setRequestHeaders();

        URL url = new URL(mytest.getRegistrationEndpt());

        JSONObject prefix = setFullRegistrationBody();

        invokeEndpointWithBody(_testName, webClient, null, url, Constants.POSTMETHOD, Constants.INVOKE_REGISTRATION_ENDPOINT, null, headers, expectations, prefix.toString(), "application/json");
    }

    private JSONObject setFullRegistrationBody() {
        JSONObject prefix = setRegistrationBody();

        prefix.put("client_id", "myClientId");
        prefix.put("client_secret", "mySecret");
        prefix.put("client_name", "myClientName");

        return prefix;
    }

    private JSONObject setRegistrationBody() {
        JSONObject prefix = new JSONObject();

        JSONArray redirectUris = new JSONArray();
        redirectUris.add("https://" + testOPServer.getServerCanonicalHostname() + ":8999/resource/redirect1");
        redirectUris.add("https://" + testOPServer.getServerCanonicalHostname() + ":9000/resource/redirect2");
        prefix.put("redirect_uris", redirectUris);

        prefix.put("token_endpoint_auth_method", "client_secret_basic");

        prefix.put("scope", "openid profile email general");

        JSONArray grantTypes = new JSONArray();
        grantTypes.add("authorization_code");
        grantTypes.add("implicit");
        grantTypes.add("refresh_token");
        grantTypes.add("client_credentials");
        grantTypes.add("password");
        grantTypes.add("urn:ietf:params:oauth:grant-type:jwt-bearer");
        prefix.put("grant_types", grantTypes);

        JSONArray responseTypes = new JSONArray();
        responseTypes.add("code");
        responseTypes.add("token");
        responseTypes.add("id_token token");
        prefix.put("response_types", responseTypes);

        prefix.put("application_type", "web");

        prefix.put("subject_type", "public");

        JSONArray logoutUris = new JSONArray();
        logoutUris.add("https://" + testOPServer.getServerCanonicalHostname() + ":9000/logout/");
        logoutUris.add("https://" + testOPServer.getServerCanonicalHostname() + ":9001/exit/");
        prefix.put("post_logout_redirect_uris", logoutUris);

        prefix.put("preauthorized_scope", "openid profile email general");

        prefix.put("introspect_tokens", false);

        JSONArray uriArray = new JSONArray();
        uriArray.add(PROVIDER_URI1);
        uriArray.add(PROVIDER_URI4);
        prefix.put("trusted_uri_prefixes", uriArray);

        prefix.put("functional_user_id", "testuser");

        JSONArray fcnUserGroupIds = new JSONArray();
        fcnUserGroupIds.add("group1");
        fcnUserGroupIds.add("group2");
        prefix.put("functional_user_groupIds", fcnUserGroupIds);

        return prefix;
    }

}
