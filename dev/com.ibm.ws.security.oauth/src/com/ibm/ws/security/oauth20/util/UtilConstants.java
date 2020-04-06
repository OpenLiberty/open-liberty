/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.util;

public class UtilConstants extends com.ibm.ws.security.oauth20.api.Constants {

    public static final String PROVIDER = "provider";
    public static final String PROVIDER_ = "provider_";
    public static final String INDEX = "index";
    public static final String LTPA_COOKIENAME = "LtpaToken";
    public static final String LTPA_V2_COOKIENAME = "LtpaToken2";
    public static final String GRANT_TYPE = "grant_type";
    public final static String ACCESS_TOKEN = "access_token";
    public final static String REFRESH_TOKEN = "refresh_token";
    public final static String OAUTH_ATTRIBUTES = "oauth_attributes";
    public final static String RESPONSE_TYPE = "response_type";
    public final static String CODE = "code";
    public final static String TOKEN = "token";
    public final static String USERINFO = "userinfo";
    public final static String AUTHORIZE = "authorize";
    public final static String PASSWORD = "password";
    public final static String CLIENT_CREDENTIALS = "client_credentials";
    public final static String AUTHORIZATION_CODE = "authorization_code";
    public final static String OAUTH_SERVICE_APP = "com.ibm.ws.security.oauth.2.0";
    public final static String OIDC_SERVICE_APP = "com.ibm.ws.security.openidconnect.server";
    public final static String OAUTH_PROVIDER_NAME = "OAuthProvider";
    public final static String CHARACTER_ENCODING = "characterEncoding";
    public final static String AUTHORIZATION_HEADER_NAME = "Authorization";
    public final static String AUTHORIZATION_HEADER_TYPE_BEARER = "Bearer";
    public final static String ERROR = "error";

    /*
     * OAuth service provider name
     */
    public static final String NAME = "name";
    /*
     * OAuth service filter
     */
    public static final String FILTER = "filter";
    /*
     * To include OAuth access token in Subject, default is true
     */
    public static final String INCLUDE_TOKEN = "includeToken";
    /*
     * To remove LTPA cookie. Default is false
     */
    public static final String INCLUDE_LTPA = "returnLtpaCookie";

    /*
     * Config.xml WebSphere Constants
     * Core constants are found in com.ibm.oauth.core.api.config.OAuthComponentConfigurationConstants
     */

    /*
     * Option to fallback to available authentication if no OAuth Access token
     */
    public final static String AUTH_WITH_OAUTH_ONLY = "oauthOnly";

    /*
     * Option to specify if TAI should intercept for requests for resource access
     */
    public final static String PROCESS_ALL = "processAll";
    /*
     * Option to specify if TAI should intercept for requests for resource access
     */
    public final static String LTPA_SUBSTITUTE = "ltpaSubstitute";
}
