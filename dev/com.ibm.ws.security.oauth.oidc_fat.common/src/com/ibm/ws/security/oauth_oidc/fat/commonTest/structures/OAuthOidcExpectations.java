/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest.structures;

import java.util.ArrayList;
import java.util.Arrays;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;

public class OAuthOidcExpectations extends Expectations {

    static private final Class<?> thisClass = OAuthOidcExpectations.class;

    protected static String[] ALL_TEST_ACTIONS = null;
    static {
        ArrayList<String> temp = new ArrayList<String>();
        temp.addAll(Arrays.asList(Constants.OP_TEST_ACTIONS));
        temp.addAll(Arrays.asList(Constants.RP_TEST_ACTIONS));
        temp.addAll(Arrays.asList(Constants.BUILDER_TEST_ACTIONS));

        ALL_TEST_ACTIONS = temp.toArray(new String[Constants.OP_TEST_ACTIONS.length + Constants.RP_TEST_ACTIONS.length + Constants.BUILDER_TEST_ACTIONS.length]);
    }

    public OAuthOidcExpectations() {
        super(ALL_TEST_ACTIONS);
    }

    public OAuthOidcExpectations(String[] testActions) {
        super(testActions);
    }

    /**
     * Creates and returns a new OAuthOidcExpectations object with successful status code expectations for all of the default
     * test actions.
     */
    public static OAuthOidcExpectations createSuccessfulStatusExpectations() {
        OAuthOidcExpectations expectations = new OAuthOidcExpectations();
        expectations.addSuccessStatusCodes();
        return expectations;
    }

    public void addNoTokensInResponseExpectations(String action) {
        addExpectation(OAuthOidcExpectation.createNoAccessTokenInResponseExpectation(action));
        addExpectation(OAuthOidcExpectation.createNoIdTokenInResponseExpectation(action));
    }

    public void addNoTokenInResponseExpectation(String action, String token) {
        addExpectation(OAuthOidcExpectation.createResponseMissingValueExpectation(action, token));
    }

    public void addTokenInResponseExpectation(String action, String token) {
        addExpectation(OAuthOidcExpectation.createResponseExpectation(action, token, "Did not find [" + token + "] in the response."));
    }

    public void addDefaultGeneralResponseExpectations(String providerType, String testStep, TestSettings settings) {
        if (settings == null) {
            Log.warning(thisClass, "Cannot set default general response expectations because settings object is null");
            return;
        }
        String scope = settings.getScope();
        if (Constants.OAUTH_OP.equals(providerType)) {
            // oauth will not contain id_token
            addExpectation(new OAuthOidcExpectation(testStep, Constants.RESPONSE_GENERAL, Constants.STRING_CONTAINS, null, "Token validate response found the id_token in the response and should not have."));
        } else {
            // if openid is not in the scope, we should make sure that id_token in not in the general response
            if (scope != null && scope.contains("openid")) {
                // validate general as well as specific information in the response
                addExpectation(new OAuthOidcExpectation(testStep, Constants.RESPONSE_GENERAL, Constants.STRING_CONTAINS, Constants.OIDC_OP, "The general content of the response was incorrect."));
            } else {
                addExpectation(new OAuthOidcExpectation(testStep, Constants.RESPONSE_GENERAL, Constants.STRING_CONTAINS, null, "Token validate response found the id_token in the response and should not have (openid was missing in scope)."));
            }
        }
        addExpectation(new OAuthOidcExpectation(testStep, Constants.RESPONSE_GENERAL, Constants.STRING_CONTAINS, "scope", scope, "The scope content of the response was incorrect."));
    }

    public void addDefaultIdTokenExpectations(String providerType, String testStep, TestSettings settings) {
        if (Constants.OAUTH_OP.equals(providerType)) {
            // oauth will not contain id_token
            addExpectation(new OAuthOidcExpectation(testStep, Constants.RESPONSE_ID_TOKEN, Constants.STRING_CONTAINS, Constants.ID_TOKEN_KEY, Constants.NOT_FOUND, "Token validate response found the id_token in the response and should not have."));
        } else {
            // even if we're testing with OIDC, if openid is NOT in the scope, we WON'T get an id_token (check for NO id_token)
            if (settings != null && settings.getScope() != null && settings.getScope().contains("openid")) {
                addDefaultIdTokenExpectations(testStep, settings);
            } else { // (check for NO id_token)
                addExpectation(new OAuthOidcExpectation(testStep, Constants.RESPONSE_ID_TOKEN, Constants.STRING_CONTAINS, Constants.ID_TOKEN_KEY, Constants.NOT_FOUND, "Token validate response found the id_token in the response and should not have."));
            }
        }
    }

    private void addDefaultIdTokenExpectations(String testStep, TestSettings settings) {
        // validate general as well as specific information in the id_token
        addExpectation(new OAuthOidcExpectation(testStep, Constants.RESPONSE_ID_TOKEN, Constants.STRING_CONTAINS, null, "The general content of the id_token was incorrect."));
        addExpectation(new OAuthOidcExpectation(testStep, Constants.RESPONSE_ID_TOKEN, Constants.STRING_MATCHES, Constants.IDTOK_AUDIENCE_KEY, settings.getClientID(), "Client id (aud) was not correct in the id_token."));
        addExpectation(new OAuthOidcExpectation(testStep, Constants.RESPONSE_ID_TOKEN, Constants.STRING_MATCHES, Constants.IDTOK_SUBJECT_KEY, settings.getAdminUser(), "Userid id (sub) was not correct in the id_token."));
        addExpectation(new OAuthOidcExpectation(testStep, Constants.RESPONSE_ID_TOKEN, Constants.STRING_CONTAINS, Constants.IDTOK_UNIQ_SEC_NAME_KEY, settings.getAdminUser(), "Unique security name (uniqueSecurityName) was not correct in the id_token."));
        String realmName = settings.getRealm();
        if (realmName != null) {
            addExpectation(new OAuthOidcExpectation(testStep, Constants.RESPONSE_ID_TOKEN, Constants.STRING_MATCHES, Constants.IDTOK_REALM_KEY, realmName, "Realm name (realmName) was not correct in the id_token."));
        } else {
            addExpectation(new OAuthOidcExpectation(testStep, Constants.RESPONSE_ID_TOKEN, Constants.STRING_MATCHES, Constants.IDTOK_REALM_KEY, Constants.BASIC_REALM, "Realm name (realmName (default)) was not correct in the id_token."));
        }
        if (settings.getNonce() != null && !settings.getNonce().isEmpty()) {
            addExpectation(new OAuthOidcExpectation(testStep, Constants.RESPONSE_ID_TOKEN, Constants.STRING_MATCHES, Constants.IDTOK_NONCE_KEY, settings.getNonce(), "Nonce (nonce) was not correct in the id_token."));
        }
    }

}