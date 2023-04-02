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
package io.openliberty.security.jakartasec.fat.configs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.web.WebRequestUtils;

import io.openliberty.security.jakartasec.fat.utils.Constants;
import jakarta.security.enterprise.authentication.mechanism.http.openid.DisplayType;
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

    protected static WebRequestUtils webReqUtils = new WebRequestUtils();

    /*************** OpenIdAuthenticationMechanismDefinition ************/
    public static Map<String, Object> getProviderUri(String opBase, String provider) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.PROVIDER_URI, opBase + "/oidc/endpoint/" + provider);
        return updatedMap;
    }

    public static Map<String, Object> getProviderDiscUri(String rpBase, String provider) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.PROVIDER_URI, rpBase + "/Discovery");
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

    public static Map<String, Object> getLiteralRedirectURI(String rpHttpsBase) throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.REDIRECT_URI, rpHttpsBase + "/literalRedirectURI/Callback");
        return updatedMap;
    }

    public static Map<String, Object> getNotRegisteredWithOPRedirectURI(String rpHttpsBase) throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.REDIRECT_URI, rpHttpsBase + "/notRegisteredWithOPRedirectURI/Callback");
        return updatedMap;
    }

    public static Map<String, Object> getDoesNotExistRedirectURI(String rpHttpsBase) throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.REDIRECT_URI, rpHttpsBase + "/doesNotExistRedirectURI/doesNotExist");
        return updatedMap;
    }

    public static Map<String, Object> getBadRedirectURI() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.REDIRECT_URI, "notARedirectURI");
        return updatedMap;
    }

    public static Map<String, Object> getEmptyRedirectURI() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.REDIRECT_URI, Constants.EMPTY_VALUE);
        return updatedMap;
    }

    public static Map<String, Object> getELRedirectURI() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.REDIRECT_URI, "${baseURL}/Callback");
        return updatedMap;
    }

    public static Map<String, Object> getELRedirectURIConcatInSingleEvalExpression() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.REDIRECT_URI, "${baseURL += openIdConfig.callbackServlet}");
        return updatedMap;
    }

    public static Map<String, Object> getELRedirectURITwoEvalExpressions() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.REDIRECT_URI, "${baseURL}${openIdConfig.callbackServlet}");
        return updatedMap;
    }

    public static Map<String, Object> getELRedirectURIDoesNotContainBaseURL(String rpHttpsBase) throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.REDIRECT_URI, rpHttpsBase + "/ELRedirectURIDoesNotContainBaseURL/${openIdConfig.callbackServlet}");
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

    public static Map<String, Object> getResponseTypeCode() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.RESPONSE_TYPE, Constants.CODE_FLOW);
        return updatedMap;
    }

    public static Map<String, Object> getResponseTypeIdToken() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.RESPONSE_TYPE, Constants.IDTOKEN_FLOW);
        return updatedMap;
    }

    public static Map<String, Object> getResponseTypeIdTokenToken() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.RESPONSE_TYPE, Constants.IDTOKEN_TOKEN_FLOW);
        return updatedMap;
    }

    public static Map<String, Object> getResponseTypeCodeIdToken() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.RESPONSE_TYPE, Constants.CODE_IDTOKEN_FLOW);
        return updatedMap;
    }

    public static Map<String, Object> getResponseTypeCodeToken() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.RESPONSE_TYPE, Constants.CODE_TOKEN_FLOW);
        return updatedMap;
    }

    public static Map<String, Object> getResponseTypeCodeIdTokenToken() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.RESPONSE_TYPE, Constants.CODE_IDTOKEN_TOKEN_FLOW);
        return updatedMap;
    }

    public static Map<String, Object> getResponseTypeUnknown() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.RESPONSE_TYPE, "unknown");
        return updatedMap;
    }

    public static Map<String, Object> getResponseTypeEmpty() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.RESPONSE_TYPE, Constants.EMPTY_VALUE);
        return updatedMap;
    }

    public static Map<String, Object> getScopeExpressionOpenId() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.SCOPE_EXPRESSION, Constants.OPENID_SCOPE);
        return updatedMap;
    }

    public static Map<String, Object> getScopeExpressionOpenIdProfile() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.SCOPE_EXPRESSION, String.join(" ", Constants.OPENID_SCOPE, Constants.PROFILE_SCOPE));
        return updatedMap;
    }

    public static Map<String, Object> getScopeExpressionOpenIdEmail() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.SCOPE_EXPRESSION, String.join(" ", Constants.OPENID_SCOPE, Constants.EMAIL_SCOPE));
        return updatedMap;
    }

    public static Map<String, Object> getScopeExpressionOpenIdProfileEmail() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.SCOPE_EXPRESSION, String.join(" ", Constants.OPENID_SCOPE, Constants.PROFILE_SCOPE, Constants.EMAIL_SCOPE));
        return updatedMap;
    }

    public static Map<String, Object> getScopeExpressionNoOpenId() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.SCOPE_EXPRESSION, Constants.PROFILE_SCOPE);
        return updatedMap;
    }

    public static Map<String, Object> getScopeExpressionEmpty() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.SCOPE_EXPRESSION, Constants.EMPTY_VALUE);
        return updatedMap;
    }

    public static Map<String, Object> getScopeExpressionNoScopesInCommonExceptOpenId() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.SCOPE_EXPRESSION, String.join(" ", Constants.OPENID_SCOPE, "scope1"));
        return updatedMap;
    }

    public static Map<String, Object> getScopeExpressionMoreScopesThanConfiguredOnServer() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.SCOPE_EXPRESSION, String.join(" ", Constants.OPENID_SCOPE, Constants.PROFILE_SCOPE, Constants.EMAIL_SCOPE, "scope1"));
        return updatedMap;
    }

    public static Map<String, Object> getScopeExpressionUppercaseScopes() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.SCOPE_EXPRESSION, String.join(" ", Constants.OPENID_SCOPE, Constants.PROFILE_SCOPE, Constants.EMAIL_SCOPE).toUpperCase());
        return updatedMap;
    }

    public static Map<String, Object> getScopeExpressionUnknownScope() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.SCOPE_EXPRESSION, "unknown");
        return updatedMap;
    }

    public static Map<String, Object> getScopeExpressionDuplicateScope() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.SCOPE_EXPRESSION, String.join(" ", Constants.OPENID_SCOPE, Constants.OPENID_SCOPE));
        return updatedMap;
    }

    public static Map<String, Object> getResponseModeQuery() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.RESPONSE_MODE, Constants.QUERY_RESPONSE_MODE);
        return updatedMap;
    }

    public static Map<String, Object> getResponseModeFragment() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.RESPONSE_MODE, Constants.FRAGMENT_RESPONSE_MODE);
        return updatedMap;
    }

    public static Map<String, Object> getResponseModeFormPost() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.RESPONSE_MODE, Constants.FORM_POST_RESPONSE_MODE);
        return updatedMap;
    }

    public static Map<String, Object> getResponseModeError() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.RESPONSE_MODE, "error");
        return updatedMap;
    }

    public static Map<String, Object> getUseNonceExpressionTrue() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.USE_NONCE_EXPRESSION, String.valueOf(true));
        return updatedMap;
    }

    public static Map<String, Object> getUseNonceExpressionFalse() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.USE_NONCE_EXPRESSION, String.valueOf(false));
        return updatedMap;
    }

    public static Map<String, Object> getExtraParametersTwoParams() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.EXTRA_PARAMETERS_EXPRESSION, "key1=value1,key2=value2");
        return updatedMap;
    }

    public static Map<String, Object> getExtraParametersDuplicateKeys() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.EXTRA_PARAMETERS_EXPRESSION, "key1=value1,key1=value2");
        return updatedMap;
    }

    public static Map<String, Object> getExtraParametersContainsSpaceInKey() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.EXTRA_PARAMETERS_EXPRESSION, "key 1=value1");
        return updatedMap;
    }

    public static Map<String, Object> getExtraParametersContainsTrailingSpaceInKey() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.EXTRA_PARAMETERS_EXPRESSION, "key1 =value1");
        return updatedMap;
    }

    public static Map<String, Object> getExtraParametersContainsSpaceInValue() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.EXTRA_PARAMETERS_EXPRESSION, "key1=value 1");
        return updatedMap;
    }

    public static Map<String, Object> getExtraParametersContainsLeadingSpaceInValue() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.EXTRA_PARAMETERS_EXPRESSION, "key1= value1");
        return updatedMap;
    }

    public static Map<String, Object> getExtraParametersContainsTrailingSpaceInValue() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.EXTRA_PARAMETERS_EXPRESSION, "key1=value1 ");
        return updatedMap;
    }

    public static Map<String, Object> getExtraParametersContainsEmptyKey() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.EXTRA_PARAMETERS_EXPRESSION, "=value1");
        return updatedMap;
    }

    public static Map<String, Object> getExtraParametersContainsEmptyValue() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.EXTRA_PARAMETERS_EXPRESSION, "key1=");
        return updatedMap;
    }

    public static Map<String, Object> getExtraParametersContainsSpaceAsValue() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.EXTRA_PARAMETERS_EXPRESSION, "key1= ");
        return updatedMap;
    }

    public static Map<String, Object> getExtraParametersContainsTwoEqualsSigns() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.EXTRA_PARAMETERS_EXPRESSION, "key1=value1=value2");
        return updatedMap;
    }

    public static Map<String, Object> getExtraParametersMissingEqualsSign() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.EXTRA_PARAMETERS_EXPRESSION, "key1value1");
        return updatedMap;
    }

    public static Map<String, Object> getExtraParametersIsEmpty() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.EXTRA_PARAMETERS_EXPRESSION, "");
        return updatedMap;
    }

    public static Map<String, Object> getExtraParametersIsAnEqualsSign() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.EXTRA_PARAMETERS_EXPRESSION, "=");
        return updatedMap;
    }

    public static Map<String, Object> getExtraParametersContainsSpecialCharacterInKey() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.EXTRA_PARAMETERS_EXPRESSION, "key&1=value1");
        return updatedMap;
    }

    public static Map<String, Object> getExtraParametersContainsSpecialCharacterInValue() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.EXTRA_PARAMETERS_EXPRESSION, "key1=value&1");
        return updatedMap;
    }

    public static Map<String, Object> getDisplayPage() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.DISPLAY_EXPRESSION, DisplayType.PAGE);
        return updatedMap;
    }

    public static Map<String, Object> getDisplayPopup() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.DISPLAY_EXPRESSION, DisplayType.POPUP);
        return updatedMap;
    }

    public static Map<String, Object> getDisplayTouch() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.DISPLAY_EXPRESSION, DisplayType.TOUCH);
        return updatedMap;
    }

    public static Map<String, Object> getDisplayWap() throws Exception {
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.DISPLAY_EXPRESSION, DisplayType.WAP);
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

    public static Map<String, Object> getPromptExpressionLoginAndConsent() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.PROMPT_EXPRESSION, new PromptType[] { PromptType.LOGIN, PromptType.CONSENT });
        return updatedMap;

    }

    public static Map<String, Object> getPromptExpressionDuplicates() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.PROMPT_EXPRESSION, new PromptType[] { PromptType.LOGIN, PromptType.LOGIN });
        return updatedMap;
    }

    public static Map<String, Object> getTokenMinValidity5s() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.TOKEN_MIN_VALIDITY_EXPRESSION, 5 * 1000);
        return updatedMap;

    }

    public static Map<String, Object> getTokenMinValidity15s() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.TOKEN_MIN_VALIDITY_EXPRESSION, 15 * 1000);
        return updatedMap;

    }

    public static Map<String, Object> getTokenMinValidity20s() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.TOKEN_MIN_VALIDITY_EXPRESSION, 20 * 1000);
        return updatedMap;

    }

    public static Map<String, Object> getTokenMinValidity60s() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.TOKEN_MIN_VALIDITY_EXPRESSION, 60 * 1000);
        return updatedMap;

    }

    public static Map<String, Object> getTokenMinValidity90s() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.TOKEN_MIN_VALIDITY_EXPRESSION, 90 * 1000);
        return updatedMap;

    }

    public static Map<String, Object> getTokenMinValidity0s() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.TOKEN_MIN_VALIDITY_EXPRESSION, 0);
        return updatedMap;

    }

    public static Map<String, Object> getTokenMinValidityNegative5s() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.TOKEN_MIN_VALIDITY_EXPRESSION, -5 * 1000);
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

    public static Map<String, Object> getIdTokenSigningAlgValuesSupported(String... algs) {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        String value = Constants.EMPTY_VALUE;
        if (algs != null) {
            value = null;
            for (String alg : algs) {
                if (value == null) {
                    value = alg;
                } else {
                    value = value + "," + alg;
                }
            }
        }
        updatedMap.put(Constants.IDTOKENSIGNINGALGORITHMSSUPPORTED, value);
        return updatedMap;
    }

    public static Map<String, Object> getJwksConnectTimeoutExpression(int jwksConnectTimeout) {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        String value = Integer.toString(jwksConnectTimeout);
        updatedMap.put(Constants.JWKSCONNECTTIMEOUTEXPRESSION, value);
        return updatedMap;
    }

    public static Map<String, Object> getJwksReadTimeoutExpression(int jwksReadTimeout) {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        String value = Integer.toString(jwksReadTimeout);
        updatedMap.put(Constants.JWKSREADTIMEOUTEXPRESSION, value);
        return updatedMap;
    }

    public static Map<String, Object> getJwksURI_BadHost(String rpPort, String provider) {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.JWKSURI, "https://192.194.99.99:" + rpPort + "/oidc/endpoint/" + provider + "/jwk");
        return updatedMap;
    }

    public static Map<String, Object> getJwksURI_TestAppSleeps5Seconds(String rpBase) {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.JWKSURI, rpBase + "/JwksEndpoint/JwksSleeps5Seconds");
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
    public static Map<String, Object> getRedirectURIGood_Logout(String rpBase) throws Exception {
        return getRedirectURIGood_Logout(rpBase, null);
    }

    public static Map<String, Object> getRedirectURIGoodWithExtraParms_Logout(String rpBase) throws Exception {
        Map<String, List<String>> parmsMap = new HashMap<String, List<String>>();
        parmsMap.put("testParm1", Arrays.asList("testParm1_value"));
        parmsMap.put("testParm2", Arrays.asList("testParm2_value"));
        parmsMap.put("testParm3", Arrays.asList("testParm3_value"));
        return getRedirectURIGood_Logout(rpBase, parmsMap);
    }

    public static Map<String, Object> getRedirectURIGood_Logout(String rpBase, Map<String, List<String>> parms) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        if (parms == null) {
            updatedMap.put(Constants.LOGOUT_REDIRECT_URI, rpBase + "/PostLogoutServlet/PostLogout");
        } else {
            String parmString = webReqUtils.buildUrlQueryString(parms);
            updatedMap.put(Constants.LOGOUT_REDIRECT_URI, rpBase + "/PostLogoutServlet/PostLogout?" + parmString);
        }
        return updatedMap;

    }

    public static Map<String, Object> getRedirectURIBad_Logout(String opBase) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.LOGOUT_REDIRECT_URI, opBase);
        return updatedMap;

    }

    public static Map<String, Object> getRedirectURIEndSession_Logout(String opBase, String provider) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.LOGOUT_REDIRECT_URI, opBase + "/oidc/endpoint/" + provider + "/end_session");
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

    public static Map<String, Object> getAuthorizationEndpoint(String rpBase, String authApp) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.AUTHORIZATION_ENDPOINT, rpBase + "/Authorization/" + authApp);
        return updatedMap;

    }

    public static Map<String, Object> getDefaultAuthorizationEndpoint(String opBase, String provider) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.AUTHORIZATION_ENDPOINT, opBase + "/oidc/endpoint/" + provider + "/authorize");
        return updatedMap;

    }

    public static Map<String, Object> getTestAuthorizationEndpoint(String rpBase) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.AUTHORIZATION_ENDPOINT, rpBase + "/Endpoints/authorize");
        return updatedMap;

    }

    public static Map<String, Object> getEmptyAuthorizationEndpoint() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.AUTHORIZATION_ENDPOINT, "");
        return updatedMap;

    }

    public static Map<String, Object> getBadAuthorizationEndpoint(String rpBase) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.AUTHORIZATION_ENDPOINT, rpBase + "/Endpoints/doesntExist");
        return updatedMap;

    }

    public static Map<String, Object> getDefaultTokenEndpoint(String opBase, String provider) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.TOKEN_ENDPOINT, opBase + "/oidc/endpoint/" + provider + "/token");
        return updatedMap;

    }

    public static Map<String, Object> getTestTokenEndpoint(String rpBase) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.TOKEN_ENDPOINT, rpBase + "/Endpoints/token");
        return updatedMap;

    }

    public static Map<String, Object> getEmptyTokenEndpoint() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.TOKEN_ENDPOINT, "");
        return updatedMap;

    }

    public static Map<String, Object> getBadTokenEndpoint(String rpBase) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.TOKEN_ENDPOINT, rpBase + "/Endpoints/doesntExist");
        return updatedMap;

    }

    public static Map<String, Object> getTestTokenSaveEndpoint(String rpBase) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.TOKEN_ENDPOINT, rpBase + "/TokenEndpointServlet/getToken");
        return updatedMap;

    }

    public static Map<String, Object> getTestJwksURI(String rpBase) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.JWKSURI, rpBase + "/Endpoints/jwk");
        return updatedMap;

    }

    public static Map<String, Object> getDefaultJwksURI(String opBase, String provider) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.JWKSURI, opBase + "/oidc/endpoint/" + provider + "/jwk");
        return updatedMap;

    }

    public static Map<String, Object> getEmptyJwksURI() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.JWKSURI, "");
        return updatedMap;

    }

    public static Map<String, Object> getTestUserinfoEndpoint(String rpBase) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.USERINFO_ENDPOINT, rpBase + "/Endpoints/userinfo");
        return updatedMap;

    }

    public static Map<String, Object> getGoodUserInfo(String opBase, String provider) throws Exception {

        String userinfoApp = opBase + "oidc/endpoint/" + provider + "/userinfo";
        Map<String, Object> updatedMap = new HashMap<String, Object>();
        Log.info(thisClass, "", "userinfoApp: " + userinfoApp);
        updatedMap.put(Constants.USERINFO_ENDPOINT, userinfoApp);
        return updatedMap;

    }

    public static Map<String, Object> getUserInfo(String rpBase, String userinfoApp) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        Log.info(thisClass, "", "userinfoApp: " + userinfoApp);
        updatedMap.put(Constants.USERINFO_ENDPOINT, rpBase + "/UserInfo/" + userinfoApp);
        return updatedMap;

    }

    public static Map<String, Object> getUserInfoSplash(String opBase) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        Log.info(thisClass, "", "userinfoApp: " + opBase);
        updatedMap.put(Constants.USERINFO_ENDPOINT, opBase);
        return updatedMap;

    }

    public static Map<String, Object> getUserInfoEmpty() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        Log.info(thisClass, "", "userinfoApp:  (not set)");
        updatedMap.put(Constants.USERINFO_ENDPOINT, "");
        return updatedMap;

    }

    public static Map<String, Object> getEndSessionEndpoint(String serverBase, String appName) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        Log.info(thisClass, "", "endSessionApp: " + appName);
        updatedMap.put(Constants.ENDSESSION_ENDPOINT, serverBase + "/" + appName);
        return updatedMap;

    }

    public static Map<String, Object> getEmptyEndSessionEndpoint() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        Log.info(thisClass, "", "endSessionApp: \"\"");
        updatedMap.put(Constants.ENDSESSION_ENDPOINT, "");
        return updatedMap;

    }

    public static Map<String, Object> getDefaultIssuer(String opBase, String provider) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.ISSUER, opBase + "/oidc/endpoint/" + provider);
        return updatedMap;

    }

    public static Map<String, Object> getEmptyIssuer() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.ISSUER, "");
        return updatedMap;

    }

    public static Map<String, Object> getBadIssuer(String rpBase) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.ISSUER, rpBase + "/someProvider");
        return updatedMap;

    }

    public static Map<String, Object> getDefaultSubjectTypeSupported() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.SUBJECTTYPESUPPORTED, "public");
        return updatedMap;

    }

    public static Map<String, Object> getEmptySubjectTypeSupported() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.SUBJECTTYPESUPPORTED, "");
        return updatedMap;

    }

    public static Map<String, Object> getBadSubjectTypeSupported() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.SUBJECTTYPESUPPORTED, "SomeValue");
        return updatedMap;

    }

    public static Map<String, Object> getDefaultResponseTypeSupported() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.RESPONSETYPESUPPORTED, "code,id_token,token id_token");
        return updatedMap;

    }

    public static Map<String, Object> getEmptyResponseTypeSupported() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.RESPONSETYPESUPPORTED, "");
        return updatedMap;

    }

    public static Map<String, Object> getBadResponseTypeSupported() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.RESPONSETYPESUPPORTED, "SomeValue");
        return updatedMap;

    }

    /********************* helper methods *************************/
    public static Map<String, Object> mergeMaps(Map<String, Object> updatedMap, Map<String, Object> newMap) throws Exception {

        if (updatedMap == null) {
            updatedMap = new HashMap<String, Object>();
        }

        updatedMap.putAll(newMap);
        return updatedMap;

    }
}
