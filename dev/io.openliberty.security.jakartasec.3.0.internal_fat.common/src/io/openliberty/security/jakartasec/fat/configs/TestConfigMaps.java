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
package io.openliberty.security.jakartasec.fat.configs;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.simplicity.log.Log;

import io.openliberty.security.jakartasec.fat.utils.Constants;
import jakarta.security.enterprise.authentication.mechanism.http.openid.PromptType;

public class TestConfigMaps {
    protected static Class<?> thisClass = TestConfigMaps.class;

    // constants for userinfo apps that return good or bad group information
    public static final String UserInfoDefaultCallerGroupsClaimGoodGroups = "UserInfoDefaultCallerGroupsClaimGoodGroups";
    public static final String UserInfoDefaultCallerGroupsClaimBadGroups = "UserInfoDefaultCallerGroupsClaimBadGroups";
    public static final String UserInfoOtherCallerGroupsClaimGoodGroups = "UserInfoOtherCallerGroupsClaimGoodGroups";
    public static final String UserInfoOtherCallerGroupsClaimBadGroups = "UserInfoOtherCallerGroupsClaimBadGroups";

    public static final String UserInfoDefaultCallerNameClaimGoodCaller = "UserInfoDefaultCallerNameClaimGoodCaller";
    public static final String UserInfoDefaultCallerNameClaimBadCaller = "UserInfoDefaultCallerNameClaimBadCaller";
    public static final String UserInfoOtherCallerNameClaimGoodCaller = "UserInfoOtherCallerNameClaimGoodCaller";
    public static final String UserInfoOtherCallerNameClaimBadCaller = "UserInfoOtherCallerNameClaimBadCaller";

    public static final String AllDefault = "UserInfo";

    /*************** OpenIdAuthenticationMechanismDefinition ************/
    public static Map<String, Object> getProviderUri(String opBase, String provider) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.PROVIDER_URI, opBase + "/oidc/endpoint/" + provider);
        return updatedMap;
    }

    public static Map<String, Object> getBadClientId() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.CLIENT_ID, "badCLient");
        return updatedMap;
    }

    public static Map<String, Object> getEmptyClientId() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.CLIENT_ID, Constants.EMPTY_VALUE);
        return updatedMap;
    }

    public static Map<String, Object> getBadClientSecret() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.CLIENT_SECRET, "badCLientSecret");
        //        updatedMap.put(Constants.CLIENT_SECRET, "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger");
        return updatedMap;
    }

    public static Map<String, Object> getEmptyClientSecret() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.CLIENT_SECRET, Constants.EMPTY_VALUE);
        return updatedMap;
    }

    public static Map<String, Object> getUseSessionExpressionTrue() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.USE_SESSION_EXPRESSION, String.valueOf(true));
        return updatedMap;
    }

    public static Map<String, Object> getUseSessionExpressionFalse() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.USE_SESSION_EXPRESSION, String.valueOf(false));
        return updatedMap;
    }

    public static Map<String, Object> getRedirectToOriginalResourceExpressionTrue() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.RECIRECT_TO_ORIGINAL_RESOURCE_EXPRESSION, String.valueOf(true));
        return updatedMap;
    }

    public static Map<String, Object> getRedirectToOriginalResourceExpressionFalse() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.RECIRECT_TO_ORIGINAL_RESOURCE_EXPRESSION, String.valueOf(false));
        return updatedMap;
    }

    public static Map<String, Object> getOP2() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.PROVIDER, "OP2");
        return updatedMap;
    }

    public static Map<String, Object> getPromptExpressionLogin() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.PROMPT_EXPRESSION, PromptType.LOGIN);
        return updatedMap;

    }

    public static Map<String, Object> getPromptExpressionEmpty() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.PROMPT_EXPRESSION, Constants.EMPTY_VALUE);
        return updatedMap;

    }

    public static Map<String, Object> getPromptExpressionNone() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.PROMPT_EXPRESSION, PromptType.NONE);
        return updatedMap;

    }

    public static Map<String, Object> getPromptExpressionConsent() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.PROMPT_EXPRESSION, PromptType.CONSENT);
        return updatedMap;

    }

    public static Map<String, Object> getPromptExpressionSelectAccount() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.PROMPT_EXPRESSION, PromptType.SELECT_ACCOUNT);
        return updatedMap;

    }

    public static Map<String, Object> getTokenAutoRefreshExpressionTrue() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.TOKEN_AUTO_REFRESH_EXPRESSION, String.valueOf(true));
        return updatedMap;

    }

    public static Map<String, Object> getTokenAutoRefreshExpressionFalse() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.TOKEN_AUTO_REFRESH_EXPRESSION, String.valueOf(false));
        return updatedMap;

    }

    /****************** ClaimDefinitions ********************/
    public static Map<String, Object> getBadCallerNameClaim() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.CALLER_NAME_CLAIM, "badCallerName");
        return updatedMap;

    }

    public static Map<String, Object> getEmptyCallerNameClaim() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.CALLER_NAME_CLAIM, Constants.EMPTY_VALUE);
        return updatedMap;

    }

    public static Map<String, Object> getBadCallerGroupsClaim() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.CALLER_GROUPS_CLAIM, "badCallerGroups");
        return updatedMap;

    }

    public static Map<String, Object> getEmptyCallerGroupsClaim() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.CALLER_GROUPS_CLAIM, Constants.EMPTY_VALUE);
        return updatedMap;

    }

    /****************** LogoutDefinitions ********************/
    public static Map<String, Object> getRedirectURIGood_Logout(String opBase, String provider) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.LOGOUT_REDIRECT_URI, opBase + "/oidc/endpoint/" + provider + "/end_session");
        return updatedMap;

    }

    public static Map<String, Object> getRedirectURIBad_Logout(String opBase) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.LOGOUT_REDIRECT_URI, opBase);
        return updatedMap;

    }

    public static Map<String, Object> getRedirectURIEmpty_Logout() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.LOGOUT_REDIRECT_URI, Constants.EMPTY_VALUE);
        return updatedMap;

    }

    public static Map<String, Object> getNotifyProviderExpressionTrue() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.NOTIFY_PROVIDER_EXPRESSION, String.valueOf(true));
        return updatedMap;

    }

    public static Map<String, Object> getNotifyProviderExpressionFalse() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.NOTIFY_PROVIDER_EXPRESSION, String.valueOf(false));
        return updatedMap;

    }

    public static Map<String, Object> getAccessTokenExpiryExpressionTrue() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.ACCESS_TOKEN_EXPIRY_EXPRESSION, String.valueOf(true));
        return updatedMap;

    }

    public static Map<String, Object> getAccessTokenExpiryExpressionFalse() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.ACCESS_TOKEN_EXPIRY_EXPRESSION, String.valueOf(false));
        return updatedMap;

    }

    public static Map<String, Object> getIdentityTokenExpiryExpressionTrue() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.IDENTITY_TOKEN_EXPIRY_EXPRESSION, String.valueOf(true));
        return updatedMap;

    }

    public static Map<String, Object> getIdentityTokenExpiryExpressionFalse() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.IDENTITY_TOKEN_EXPIRY_EXPRESSION, String.valueOf(false));
        return updatedMap;

    }

    public static Map<String, Object> getUserInfo(String rpBase, String userinfoApp) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        Log.info(thisClass, "", "userinfoApp: " + userinfoApp);
        updatedMap.put(Constants.USERINFOENDPOINT, rpBase + "/UserInfo/" + userinfoApp);
        return updatedMap;

    }

    public static Map<String, Object> getUserInfoEmpty() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        Log.info(thisClass, "", "userinfoApp:  (not set)");
        updatedMap.put(Constants.USERINFOENDPOINT, "");
        return updatedMap;

    }

    public static Map<String, Object> mergeMaps(Map<String, Object> updatedMap, Map<String, Object> newMap) throws Exception {

        if (updatedMap == null) {
            updatedMap = new HashMap<String, Object>();
        }

        updatedMap.putAll(newMap);
        return updatedMap;

    }
}