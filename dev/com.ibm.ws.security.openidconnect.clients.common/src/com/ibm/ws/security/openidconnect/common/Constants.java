/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.common;

/**
 * Common constants for OpenID Connect
 */
public class Constants {
    public static final String UTF_8 = "UTF-8";
    public static final String IMPLICIT = "implicit";
    public static final String SCOPE = "scope";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String USERINFO_STR = "userinfo_string";
    public static final String ID_TOKEN = "id_token";
    public static final String ID_TOKEN_OBJECT = "id_token_object";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String GRANT_TYPE = "grant_type";
    public static final String RESPONSE_TYPE_CODE = "code";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String CODE = "code";
    public static final String STATE = "state";
    public static final String SESSION_STATE = "session_state";
    public static final String BROWSER_STATE_COOKIE = "oidc_bsc";
    public static final String ERROR = "error";
    public static final String ERROR_DESCRIPTION = "error_description";
    public static final String SIG_ALG_NONE = "none";
    public static final String SIG_ALG_HS256 = "HS256";
    public static final String SIG_ALG_RS256 = "RS256";
    public static final String CREDENTIAL_STORING_TIME_MILLISECONDS = "com.ibm.wssi.security.oidc.client.credential.storing.utc.time.milliseconds"; // GMT==UTC

    public static final int STATE_LENGTH = 20;

    public static final String TOKEN_TYPE_ID_TOKEN = "IDToken";
    public static final String TOKEN_TYPE_ACCESS_TOKEN = "AccessToken";
    public static final String TOKEN_TYPE_JWT = "JsonWebToken";
}