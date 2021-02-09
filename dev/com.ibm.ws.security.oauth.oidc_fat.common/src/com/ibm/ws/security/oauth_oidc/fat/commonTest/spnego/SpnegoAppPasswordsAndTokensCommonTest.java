/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest.spnego;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.joda.time.Instant;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.fat.common.utils.MySkipRule;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.AppPasswordsAndTokensCommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ClientRegistrationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTestHelpers;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.AppPasswordsAndTokensCommonTest.AllTokens;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.AppPasswordsAndTokensCommonTest.TokenValues;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

/**
 * Test class for app-password and app-token endpoint test tools
 *
 * @author chrisc
 *
 */
public class SpnegoAppPasswordsAndTokensCommonTest extends SpnegoOIDCCommonTest {

    private static final Class<?> thisClass = SpnegoAppPasswordsAndTokensCommonTest.class;


    /**
     * Get an access_token - the configuration will dictate whether it is an access_token or a JWT access_token
     *
     * @param settings
     *            - the current test settings
     * @return - returns the generated access_token
     * @throws Exception
     */
    public String getAccessTokenWithWebClient(TestSettings settings) throws Exception {
       
        return getAllNewTokensWithWebClient(settings).getAccessToken();
    }

    public AllTokens getAllNewTokensWithWebClient(TestSettings settings) throws Exception {

		String[] BASIC_TOKEN;
		WebClient webClient = CommonTestHelpers.getWebClient(true);
		List<validationData> expectations = null;
		
		if (settings.getAdminUser() == SpnegoOIDCConstants.NTLM_TOKEN) {
			String[] BASIC_TOKEN1 = { SpnegoOIDCConstants.INVOKE_AUTH_ENDPOINT};
			expectations = vData.addSuccessStatusCodes(expectations, SpnegoOIDCConstants.INVOKE_AUTH_ENDPOINT);
			expectations = vData.addResponseStatusExpectation(expectations, SpnegoOIDCConstants.INVOKE_AUTH_ENDPOINT, 401);
			expectations = vData.addResponseExpectation(expectations, SpnegoOIDCConstants.INVOKE_AUTH_ENDPOINT, "CWWKS4307E",
					"CWWKS4307E: <html><head><title>An NTLM Token was received.</title>");
			BASIC_TOKEN = BASIC_TOKEN1;
		}

		else {
			String[] BASIC_TOKEN1 = { SpnegoOIDCConstants.INVOKE_AUTH_ENDPOINT, SpnegoOIDCConstants.INVOKE_TOKEN_ENDPOINT };
			expectations=vData.addSuccessStatusCodes();
			expectations=vData.addExpectation(expectations, SpnegoOIDCConstants.PERFORM_LOGIN, SpnegoOIDCConstants.RESPONSE_URL,
					SpnegoOIDCConstants.STRING_DOES_NOT_CONTAIN,
					"Ended up back on the login page but should not have. Check if user authentication failed.", null,
					"login.jsp");
			
			BASIC_TOKEN=BASIC_TOKEN1;

		}
        Object response = genericOP(_testName, webClient, settings, BASIC_TOKEN, expectations);
        AllTokens tokens = new AllTokens();
        tokens.setAccessToken(validationTools.getTokenForType(settings, response));
        tokens.setRefreshToken(validationTools.getTokenFromResponse(response, SpnegoOIDCConstants.REFRESH_TOKEN_KEY));
        tokens.setIdToken(validationTools.getTokenFromResponse(response, SpnegoOIDCConstants.ID_TOKEN_KEY));
        Log.info(thisClass, "getAccessToken", "The access_token value is: " + tokens.getAccessToken());
        Log.info(thisClass, "getRefreshToken", "The refresh_token value is: " + tokens.getRefreshToken());
        Log.info(thisClass, "getIdToken", "The id_token value is: " + tokens.getIdToken());
        return tokens;

    }
    
   
    public TokenValues createAppPasswordswebClient(TestSettings settings, String accessToken, String appName, boolean expectAnAppPassword, List<validationData> expectations) throws Exception {
    	WebClient webClient = CommonTestHelpers.getWebClient(true);
        Object response = invokeAppPasswordsEndpoint_create(_testName, webClient, settings, accessToken, appName, usedByNotSetOrUnknown, Constants.HEADER, expectations);
        List<TokenValues> values = null;
        if (expectAnAppPassword) {
            // create should generate info for one app-password - the tool will return a list of TokenValues - just grab the first entry
            values = getTokenInfoFromResponse(response);
            TokenValues aTokenValue = null;
            if (values != null) {
                aTokenValue = values.get(0);
                Log.info(thisClass, "createAppPassword", "The app-password value is: " + aTokenValue.getApp_password());
                // make sure that we were issued an opaque access_token (not a jwt)
                if (aTokenValue.getApp_password() != null) {
                    int len = aTokenValue.getApp_password().split("\\.").length;
                    if (len != 1) {
                        fail("Expected an opaque app-password that should have 1 part.  The app-password has " + len + " parts.");
                    }
                }
            }
            return aTokenValue;

        } else {
            // return null - there is no app-password - the call to invokeAppPasswordEndpoint_create should have validated
            // the response/behavior.  Any additional checking can be done by the caller
            return null;
        }
    }

    public void revokeAppPasswords(TestSettings settings, String accessToken, String userId, String appId, List<validationData> expectations) throws Exception {
    	WebClient webClient = CommonTestHelpers.getWebClient(true);
        invokeAppPasswordsEndpoint_revoke(_testName, webClient, settings, accessToken, userId, appId, Constants.HEADER, expectations);
    }
    
}
