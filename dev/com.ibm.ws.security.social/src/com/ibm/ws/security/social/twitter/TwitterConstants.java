/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.twitter;

import java.util.Arrays;
import java.util.List;

import com.ibm.ws.security.social.internal.TwitterLoginConfigImpl;

public class TwitterConstants {

    public static final String TWITTER_CONFIG_CLASS = TwitterLoginConfigImpl.class.getName();

    public static final String HMAC_SHA1 = "HMAC-SHA1";

    public static final String COOKIE_NAME_REQUEST_TOKEN = "WASSocialRequestToken";
    public static final String COOKIE_NAME_ACCESS_TOKEN = "WASSocialAccessToken";
    public static final String COOKIE_NAME_ACCESS_TOKEN_SECRET = "WASSocialAccessTokenSecret";

    public static final String TWITTER_ENDPOINT_HOST = "api.twitter.com";
    public static final String TWITTER_ENDPOINT_REQUEST_TOKEN = "/oauth/request_token";
    public static final String TWITTER_ENDPOINT_AUTHORIZE = "/oauth/authenticate";
    public static final String TWITTER_ENDPOINT_ACCESS_TOKEN = "/oauth/access_token";
    public static final String TWITTER_ENDPOINT_VERIFY_CREDENTIALS = "/1.1/account/verify_credentials.json";

    public static final String PARAM_OAUTH_CONSUMER_KEY = "oauth_consumer_key";
    public static final String PARAM_OAUTH_NONCE = "oauth_nonce";
    public static final String PARAM_OAUTH_SIGNATURE = "oauth_signature";
    public static final String PARAM_OAUTH_SIGNATURE_METHOD = "oauth_signature_method";
    public static final String PARAM_OAUTH_TIMESTAMP = "oauth_timestamp";
    public static final String PARAM_OAUTH_TOKEN = "oauth_token";
    public static final String PARAM_OAUTH_VERSION = "oauth_version";
    public static final String PARAM_OAUTH_CALLBACK = "oauth_callback";
    public static final String PARAM_OAUTH_VERIFIER = "oauth_verifier";
    public static final String PARAM_INCLUDE_EMAIL = "include_email";
    public static final String PARAM_SKIP_STATUS = "skip_status";

    public final static List<String> AUTHZ_HEADER_PARAMS = Arrays.asList(PARAM_OAUTH_CONSUMER_KEY, PARAM_OAUTH_NONCE, PARAM_OAUTH_SIGNATURE,
            PARAM_OAUTH_SIGNATURE_METHOD, PARAM_OAUTH_TIMESTAMP, PARAM_OAUTH_TOKEN, PARAM_OAUTH_VERSION, PARAM_OAUTH_CALLBACK);

    public static final String INCLUDE_EMAIL = "true";
    public static final String SKIP_STATUS = "true";

    public static final String RESPONSE_OAUTH_TOKEN = "oauth_token";
    public static final String RESPONSE_OAUTH_TOKEN_SECRET = "oauth_token_secret";
    public static final String RESPONSE_OAUTH_CALLBACK_CONFIRMED = "oauth_callback_confirmed";
    public static final String RESPONSE_USER_ID = "user_id";
    public static final String RESPONSE_SCREEN_NAME = "screen_name";
    public static final String RESPONSE_EMAIL = "email";
    public static final String RESPONSE_ID = "id";

    public static final String RESULT_ERROR = "error";
    public static final String RESULT_SUCCESS = "success";
    public static final String RESULT_RESPONSE_STATUS = "response_status";
    public static final String RESULT_MESSAGE = "message";
    public static final String RESULT_ACCESS_TOKEN = "access_token";
    public static final String RESULT_ACCESS_TOKEN_SECRET = "access_token_secret";
    public static final String RESULT_USER_ID = "id";
    public static final String RESULT_SCREEN_NAME = "screen_name";
    public static final String RESULT_EMAIL = "email";

}
