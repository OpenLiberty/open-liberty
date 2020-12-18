/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import java.util.List;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

public class AuthorizationHelpers {

    //private static final Class<?> thisClass = AuthorizationHelpers.class;
    public ValidationData vData = new ValidationData();
    public static CommonValidationTools validationTools = new CommonValidationTools();

    //public static EndpointSettings eSettings = new EndpointSettings();

    /**
     * Set expectations for a test that will bypass consent and get to the protected resource
     * 
     * @param flowType
     *            TODO
     * 
     */
    public List<validationData> setGoodAuthExpectations(EndpointSettings eSettings, TestSettings testSettings, String testName, String flowType) throws Exception {

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes();

        if (flowType.equals(Constants.WEB_CLIENT_FLOW)) {
            // Check if we got authorization code
            expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null, Constants.RECV_AUTH_CODE);
            // Check if we got the access token
            expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null, Constants.RECV_FROM_TOKEN_ENDPOINT);
            // Make sure we get to the app
            expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Could not invoke protected application", null, Constants.APP_TITLE);
            // Response should not have an ltpa token
            expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null, "false");

            // add generic id_token expectations
            expectations = validationTools.addDefaultIDTokenExpectations(expectations, testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, testSettings);
            // add generic r esponse expectations
            expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, testSettings);
        } else {
            if (flowType.equals(Constants.IMPLICIT_FLOW)) {

                // now, we want to add expecations that actually validate responses received during the flow of the generic test.
                // Check if we got the redirect access token
                expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not receive redirect access token", null, Constants.REDIRECT_ACCESS_TOKEN);

                expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive login page", null, Constants.LOGIN_PROMPT);

                expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.INVOKE_AUTH_SERVER);
            } else {
                if (flowType.equals(Constants.CLIENT_CREDENTIAL_FLOW)) {
                    expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.JSON_OBJECT, Constants.STRING_MATCHES, "Did not receive access token", Constants.ACCESS_TOKEN_KEY, ".*");
                    expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, testName, Constants.OAUTH_OP, Constants.INVOKE_TOKEN_ENDPOINT, testSettings);
                } else {
                    if (flowType.equals(Constants.PASSWORD_FLOW)) {
                        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.JSON_OBJECT, Constants.STRING_MATCHES, "Did not receive access token", Constants.ACCESS_TOKEN_KEY, ".*");
                        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, testName, Constants.OAUTH_OP, Constants.INVOKE_TOKEN_ENDPOINT, testSettings);
                    }
                }
            }

        }
        return expectations;
    }

    /**
     * Set expectations for a test that will hit the consent panel - test will stop there
     * 
     */
    public List<validationData> setConsentExpectations() throws Exception {

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Check if we got the approval form	
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get the approval form", null, Constants.APPROVAL_FORM);
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_TITLE, Constants.STRING_CONTAINS, "Did not get the approval form", null, Constants.APPROVAL_HEADER);
        return expectations;

    }

    public List<validationData> setScopeMismatchExpectations(String scope, String action) throws Exception {

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null, Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null, Constants.RECV_FROM_TOKEN_ENDPOINT);

        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive login page", null, Constants.LOGIN_PROMPT);

        // make sure that id_token is NOT in the response
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_ID_TOKEN, Constants.STRING_CONTAINS, "Token validate response found the id_token in the response and should not have", Constants.ID_TOKEN_KEY, Constants.NOT_FOUND);
        expectations = vData.addNoTokensInResponseExpectations(expectations, action);

        return expectations;
    }

    public TestSettings updateAuthTestSettings(EndpointSettings eSettings, TestSettings testSettings, String client, String autoAuth, String origProvider, String updatedProvider, String newScope) throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        if (client != null) {
            updatedTestSettings.setClientName(client);
            updatedTestSettings.setClientID(client);
        }
        if (autoAuth != null) {
            updatedTestSettings.setAutoAuthz(autoAuth);
        }
        if ((origProvider != null) && (updatedProvider != null)) {
            updatedTestSettings.setAuthorizeEndpt(valueToSet(updatedTestSettings.getAuthorizeEndpt(), origProvider, updatedProvider));
            updatedTestSettings.setTokenEndpt(valueToSet(updatedTestSettings.getTokenEndpt(), origProvider, updatedProvider));
            updatedTestSettings.setIntrospectionEndpt(valueToSet(updatedTestSettings.getIntrospectionEndpt(), origProvider, updatedProvider));
            updatedTestSettings.setRevocationEndpt(valueToSet(updatedTestSettings.getRevocationEndpt(), origProvider, updatedProvider));
            updatedTestSettings.setDiscoveryEndpt(valueToSet(updatedTestSettings.getDiscoveryEndpt(), origProvider, updatedProvider));
            updatedTestSettings.setUserinfoEndpt(valueToSet(updatedTestSettings.getUserinfoEndpt(), origProvider, updatedProvider));
        }
        if (updatedProvider.contains("False")) {
            updatedTestSettings.setProtectedResource(valueToSet(updatedTestSettings.getProtectedResource(), "snoop", "snapp"));
        }

        // if newScope is null and case is OAUTH, the scope will have been set at startup with NO openid
        if (newScope != null) {
            // if called for an oauth test, we have to remove openid from the scope
            if (eSettings.getProviderType().equals(Constants.OAUTH_OP)) {
                String updatedScope = null;
                if (newScope.contains("openid")) {
                    updatedScope = newScope.replace("openid", "").trim();
                } else {
                    updatedScope = newScope;
                }
                updatedTestSettings.setScope(updatedScope);
            } else {
                updatedTestSettings.setScope(newScope);
            }

        }
        return updatedTestSettings;

    }

    public String valueToSet(String current, String origValue, String newValue) throws Exception {
        if (current == null) {
            return null;
        } else {
            return current.replace(origValue, newValue);
        }

    }

    public String fixScope(EndpointSettings eSettings, TestSettings testSettings, String newScope) throws Exception {

        String updatedScope = null;
        // if called for an oauth test, we have to remove openid from the scope
        if (eSettings.getProviderType().equals(Constants.OAUTH_OP)) {
            if (newScope.contains("openid")) {
                updatedScope = newScope.replace("openid", "").trim();
            } else {
                updatedScope = newScope;
            }
        } else {
            updatedScope = newScope;
        }
        return updatedScope;

    }
}
