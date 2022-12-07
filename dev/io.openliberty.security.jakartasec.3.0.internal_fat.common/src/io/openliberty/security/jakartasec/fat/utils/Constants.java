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
package io.openliberty.security.jakartasec.fat.utils;

public class Constants extends com.ibm.ws.security.fat.common.Constants {

    public static final String OPEN_ID_CONFIG_PROPERTIES = "/openIdConfig.properties";
    public static final String PROVIDER_CONFIG_PROPERTIES = "/providerConfig.properties";

    public static final String PROVIDER_URI = "providerURI";
    public static final String REDIRECT_URI = "redirectURI";
    public static final String RECIRECT_TO_ORIGINAL_RESOURCE = "redirectToOriginalResource";
    public static final String RECIRECT_TO_ORIGINAL_RESOURCE_EXPRESSION = "redirectToOriginalResourceExpression";
    public static final String CLIENT_ID = "clientId";
    public static final String CLIENT_SECRET = "clientSecret";
    public static final String PROVIDER_BASE = "providerBase";
    public static final String PROVIDER_SECURE_BASE = "providerSecureBase";
    public static final String CLIENT_BASE = "clientBase";
    public static final String CLIENT_SECURE_BASE = "clientSecureBase";
    public static final String USE_SESSION = "useSession";
    public static final String USE_SESSION_EXPRESSION = "useSessionExpression";
    public static final String RESPONSE_TYPE = "responseType";
    public static final String PROVIDER = "provider";
    public static final String LOGOUT = "logout";
    public static final String LOGOUT_REDIRECT_URI = "logoutRedirectURI";
    public static final String PROMPT = "prompt";
    public static final String PROMPT_EXPRESSION = "promptExpression";

    public static final String CALLER_NAME_CLAIM = "callerNameClaim";
    public static final String CALLER_GROUPS_CLAIM = "callerGroupsClaim";

    public static final String NOTIFY_PROVIDER = "notifyProvider";
    public static final String NOTIFY_PROVIDER_EXPRESSION = "notifyProviderExpression";
    public static final String ACCESS_TOKEN_EXPIRY = "accessTokenExpiry";
    public static final String ACCESS_TOKEN_EXPIRY_EXPRESSION = "accessTokenExpiryExpression";
    public static final String IDENTITY_TOKEN_EXPIRY = "identityTokenExpiry";
    public static final String IDENTITY_TOKEN_EXPIRY_EXPRESSION = "identityTokenExpiryExpression";
    public static final String TOKEN_AUTO_REFRESH = "tokenAutoRefresh";
    public static final String TOKEN_AUTO_REFRESH_EXPRESSION = "tokenAutoRefreshExpression";

    public static final String USERINFOENDPOINT = "userinfoEndpoint";
    public static final String IDTOKENSIGNINGALGORITHMSSUPPORTED = "idTokenSigningAlgorithmsSupported";
    public static final String JWKSCONNECTTIMEOUTEXPRESSION = "jwksConnectTimeoutExpression";
    public static final String JWKSREADTIMEOUTEXPRESSION = "jwksReadTimeoutExpression";

    public static final String EMPTY_VALUE = "EmptyValue";
    public static final String NULL_VALUE = "NullValue";

    // scopes
    public static final String OPENID_SCOPE = "openid";
    public static final String EMAIL_SCOPE = "email";
    public static final String PROFILE_SCOPE = "profile";

    // authorization code flow
    public static final String CODE_FLOW = "code";

    // implicit flows
    public static final String IDTOKEN_FLOW = "id_token";
    public static final String IDTOKEN_TOKEN_FLOW = "id_token token";

    // hybrid flows
    public static final String CODE_IDTOKEN_FLOW = "code id_token";
    public static final String CODE_TOKEN_FLOW = "code token";
    public static final String CODE_IDTOKEN_TOKEN_FLOW = "code id_token token";

    // Display
    public static final String PAGE_DISPLAY = "page";

    public static final int DEFAULT_JWKS_CONN_TIMEOUT = 500;
    public static final int OVERRIDE_DEFAULT_JWKS_READ_TIMEOUT = 60000;
    public static final int TOKEN_MIN_VALIDITY = 10 * 1000;

    public static final String OPEN_LIBERTY = "Open Liberty";

    public static final String USERINFO_JWT = "userinfoJwt";
    public static final String USERINFO_JSONOBJECT = "userinfoJsonObject";
}