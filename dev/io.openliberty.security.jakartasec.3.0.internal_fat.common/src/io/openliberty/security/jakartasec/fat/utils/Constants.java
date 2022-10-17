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
    public static final String BASE_SERVLET_MESSAGE = "Hello world from ";

    public static final String OPEN_ID_CONFIG_PROPERTIES = "/openIdConfig.properties";
    public static final String PROVIDER_CONFIG_PROPERTIES = "/providerConfig.properties";

    public static final String REDIRECT_URI = "redirectURI";
    public static final String CLIENT_ID = "clientId";
    public static final String CLIENT_SECRET = "clientSecret";
    public static final String PROVIDER_BASE = "providerBase";
    public static final String PROVIDER_SECURE_BASE = "providerSecureBase";
    public static final String CLIENT_BASE = "clientBase";
    public static final String CLIENT_SECURE_BASE = "clientSecureBase";
    public static final String USE_SESSION = "useSession";
    public static final String USE_SESSION_EXPRESSION = "useSessionExpression";

    public static final String CALLER_NAME_CLAIM = "callerNameClaim";
    public static final String CALLER_GROUPS_CLAIM = "callerGroupsClaim";

    public static final String EMPTY_VALUE = "EmptyValue";
    public static final String NULL_VALUE = "NullValue";

    // scopes
    public static final String OPENID_SCOPE = "openid";
    public static final String EMAIL_SCOPE = "email";
    public static final String PROFILE_SCOPE = "profile";

    // flows
    public static final String CODE_FLOW = "code";

    // Display
    public static final String PAGE_DISPLAY = "page";

    public static final int DEFAULT_JWKS_CONN_TIMEOUT = 500;
    public static final int TOKEN_MIN_VALIDITY = 10 * 1000;

}