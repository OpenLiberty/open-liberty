/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package oidc.client.base.servlets;

import io.openliberty.security.jakartasec.fat.utils.Constants;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;
import jakarta.security.enterprise.authentication.mechanism.http.openid.PromptType;

/**
 * Load the config values from the default config property file.
 * If a value is found for an expected config attribute, load it, if a value is NOT found set this tests suites default value.
 * Test applications can override methods to specify their own "default" values.
 * A value of "UnsetValue" will cause this tooling to return null (this allows the test specified config files to indicate that
 * they don't
 * want even the default config values to be set.)
 */

@Named
@Dependent
public class BaseOpenIdConfig extends MinimumBaseOpenIdConfig {

    public String getProviderURI() {
        System.out.println("in BaseOpenIdConfig - getProviderURI");
        String value = "providerURI_notSet";
        if (config.containsKey(Constants.PROVIDER_URI)) {
            System.out.println("in BaseOpenIdConfig - getProviderURI - found value in the config file");
            value = getStringValue(Constants.PROVIDER_URI);
        }
        return value;
    }

    public String getRedirectURI() {
        System.out.println("in BaseOpenIdConfig - getRedirectURI");
        String value = "${baseURL}/Callback";
        if (config.containsKey(Constants.REDIRECT_URI)) {
            System.out.println("in BaseOpenIdConfig - getRedirectURI - found value in the config file");
            value = getStringValue(Constants.REDIRECT_URI);
        }
        return value;
    }

    public boolean getRedirectToOriginalResourceExpression() {

        boolean value = false;
        if (config.containsKey(Constants.RECIRECT_TO_ORIGINAL_RESOURCE_EXPRESSION)) {
            value = getBooleanValue(Constants.RECIRECT_TO_ORIGINAL_RESOURCE_EXPRESSION);
        }

        System.out.println(Constants.RECIRECT_TO_ORIGINAL_RESOURCE_EXPRESSION + Boolean.toString(value));
        return value;
    }

    public String getClientId() {

        String value = "client_1";
        if (config.containsKey(Constants.CLIENT_ID)) {
            value = getStringValue(Constants.CLIENT_ID);
        }

        return value;
    }

    public String getClientSecret() {

        String value = "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger";
        if (config.containsKey(Constants.CLIENT_SECRET)) {
            value = getStringValue(Constants.CLIENT_SECRET);
        }

        return value;
    }

    public boolean getUseSessionExpression() {

        boolean value = true;
        if (config.containsKey(Constants.USE_SESSION_EXPRESSION)) {
            value = getBooleanValue(Constants.USE_SESSION_EXPRESSION);
        }

        return value;
    }

    public boolean getTokenAutoRefreshExpression() {

        boolean value = false;
        if (config.containsKey(Constants.TOKEN_AUTO_REFRESH_EXPRESSION)) {
            value = getBooleanValue(Constants.TOKEN_AUTO_REFRESH_EXPRESSION);
        }

        if (value) {
            System.out.println("getTokenAutoRefreshExpression: true");
        } else {
            System.out.println("getTokenAutoRefreshExpression: false");

        }
        return value;
    }

    public PromptType[] getPromptExpression() {

        PromptType[] value = {};
        if (config.containsKey(Constants.PROMPT_EXPRESSION)) {
            value = getPromptTypeValue(Constants.PROMPT_EXPRESSION);
        }

        return value;
    }

    public String getCallerNameClaim() {

        System.out.println("in getCallerNameClaim");
        String value = "sub";
        if (config.containsKey(Constants.CALLER_NAME_CLAIM)) {
            value = getStringValue(Constants.CALLER_NAME_CLAIM);
        }

        return value;

    }

    public String getCallerGroupsClaim() {

        String value = "groupIds";
        if (config.containsKey(Constants.CALLER_GROUPS_CLAIM)) {
            value = getStringValue(Constants.CALLER_GROUPS_CLAIM);
        }

        return value;

    }

    public boolean getNotifyProviderExpression() {

        boolean value = false;
        if (config.containsKey(Constants.NOTIFY_PROVIDER_EXPRESSION)) {
            value = getBooleanValue(Constants.NOTIFY_PROVIDER_EXPRESSION);
        }

        return value;
    }

    public boolean getAccessTokenExpiryExpression() {

        boolean value = false;
        if (config.containsKey(Constants.ACCESS_TOKEN_EXPIRY_EXPRESSION)) {
            value = getBooleanValue(Constants.ACCESS_TOKEN_EXPIRY_EXPRESSION);
        }

        return value;
    }

    public boolean getIdentityTokenExpiryExpression() {

        boolean value = false;
        if (config.containsKey(Constants.IDENTITY_TOKEN_EXPIRY_EXPRESSION)) {
            value = getBooleanValue(Constants.IDENTITY_TOKEN_EXPIRY_EXPRESSION);
        }

        return value;
    }

    public String getLogoutRedirectURI() {
        System.out.println("in BaseOpenIdConfig - getLogoutRedirectURI");
        String value = "";
        if (config.containsKey(Constants.LOGOUT_REDIRECT_URI)) {
            System.out.println("in BaseOpenIdConfig - getLogoutRedirectURI - found value in the config file");
            value = getStringValue(Constants.LOGOUT_REDIRECT_URI);
        }
        return value;
    }

}
