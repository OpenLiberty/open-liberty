/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.internal.oauth20;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.ibm.oauth.core.internal.OAuthConstants;

public interface OAuth20Constants extends OAuthConstants {

    /*********************************************************
     * Request types for identifying processing entry points
     **********************************************************/

    public static final String REQUEST_TYPE = "request_type";

    public static final String REQUEST_TYPE_AUTHORIZATION = "authorization";

    public static final String REQUEST_TYPE_ACCESS_TOKEN = "access_token";

    public static final String REQUEST_TYPE_RESOURCE = "resource";

    /*
     * Message parameters and values
     */
    public static final String RESPONSE_TYPE = "response_type";
    public static final String RESPONSE_TYPE_CODE = "code";
    public static final String RESPONSE_TYPE_TOKEN = "token";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String TRUSTED_URI_PREFIXES = "trustedUriPrefixes";
    public static final String RESOURCE = "resource";
    public static final String RESOURCE_IDS = "resourceIds";
    public static final String RESOURCE_OWNER_USERNAME = "username";
    public static final String RESOURCE_OWNER_OVERRIDDEN_USERNAME = "overriddenusername";
    public static final String RESOURCE_OWNER_PASSWORD = "password";
    public static final String SCOPE = "scope";
    public static final String GRANT_TYPE = "grant_type";
    public static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    public static final String GRANT_TYPE_PASSWORD = "password";
    public static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    public static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
    public static final String GRANT_TYPE_RESOURCE_OWNER = "resource_owner";
    public static final String GRANT_TYPE_IMPLICIT = "implicit";
    public static final String GRANT_TYPE_IMPLICIT_INTERNAL = "implicit_internal";
    public static final String GRANT_TYPE_APP_TOKEN = "app_token";
    public static final String GRANT_TYPE_APP_PASSWORD = "app_password";
    public static final String GRANT_TYPE_JWT = "urn:ietf:params:oauth:grant-type:jwt-bearer";
    public static final String[] ALL_GRANT_TYPES = {
            GRANT_TYPE_AUTHORIZATION_CODE, GRANT_TYPE_PASSWORD,
            GRANT_TYPE_REFRESH_TOKEN, GRANT_TYPE_CLIENT_CREDENTIALS,
            GRANT_TYPE_IMPLICIT, GRANT_TYPE_IMPLICIT_INTERNAL, GRANT_TYPE_JWT, GRANT_TYPE_APP_PASSWORD, GRANT_TYPE_APP_TOKEN };
    public static final HashSet<String> ALL_GRANT_TYPES_SET = new HashSet<String>(Arrays
            .asList(ALL_GRANT_TYPES));
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String ID_TOKEN = "id_token";
    public static final String CODE = "code";
    public static final String EXPIRES_IN = "expires_in";
    public static final String TOKEN_TYPE = "token_type";
    public static final String STATE = "state";

    public static final String CODE_CHALLENGE = "code_challenge";
    public static final String CODE_CHALLENGE_METHOD = "code_challenge_method";
    public static final String CODE_VERIFIER = "code_verifier";
    public static final String CODE_CHALLENGE_METHOD_PLAIN = "plain";
    public static final String CODE_CHALLENGE_METHOD_S256 = "S256";
    public static final String CODE_CHALLENGE_ALG_METHOD_SHA256 = "SHA-256";
    public static final String CODE_VERIFIER_ASCCI = "US-ASCII";
    public static final int CODE_VERIFIER_MIN_LENGTH = 43;
    public static final int CODE_VERIFIER_MAX_LENGTH = 128;

    public static final String ISSUER_IDENTIFIER = "issuerIdentifier";
    public static final String REFRESH_TOKEN_KEY = "refresh_key";
    public static final String OLD_REFRESH_TOKEN_KEY = "old_refresh_key";

    public static final String OAUTH_REQUEST_OBJECT_ATTR_NAME = "OAuth20Request";
    public static final String OIDC_REQUEST_OBJECT_ATTR_NAME = "OidcRequest";

    /*
     * Token types, subtypes, and token map data
     */
    public static final String TOKENTYPE_AUTHORIZATION_GRANT = "authorization_grant";
    public static final String TOKENTYPE_ACCESS_TOKEN = ACCESS_TOKEN;
    public static final String SUBTYPE_BEARER = "Bearer";
    public static final String SUBTYPE_AUTHORIZATION_CODE = GRANT_TYPE_AUTHORIZATION_CODE;
    public static final String SUBTYPE_REFRESH_TOKEN = GRANT_TYPE_REFRESH_TOKEN;
    public static final String LIFETIME = "LIFETIME";
    public static final String LENGTH = "LENGTH";
    public static final String COMPONENTID = "COMPONENTID";
    public static final String RESPONSEATTR_EXPIRES = "expires";

    // Plugin JWT constants
    public static final String KEY_CUSTOMIZED_GRANT_TYPE_HANDLER_ID_QUAL = "com.ibm.ws.security.oauth20.provider.granttype.handler.id";

    public final static String ATTRTYPE_PARAM_JWT = "urn:ibm:names:jwt:param";
    public final static String ATTRTYPE_PARAM_OAUTH_REQUEST = "urn:ibm:names:oauth:param:request";

    public final static String CLAIM_NAME_ISS = "iss";
    public final static String CLAIM_NAME_SUB = "sub";
    public final static String CLAIM_NAME_AUD = "aud";
    public final static String CLAIM_NAME_EXP = "exp";
    public final static String CLAIM_NAME_NBF = "nbf";
    public final static String CLAIM_NAME_IAT = "iat";
    public final static String CLAIM_NAME_JTI = "jti";
    public final static String CLAIM_NAME_SCOPE = "scope";

    /*********************************************************
     * These are extra meta parameters that are returned in the attribute list
     * along with any tokens or grants, but are not to be returned to the OAuth
     * 2.0 client
     **********************************************************/

    public static final String AUTHORIZATION_CODE_ID = GRANT_TYPE_AUTHORIZATION_CODE
            + "_id";

    public static final String REFRESH_TOKEN_ID = GRANT_TYPE_REFRESH_TOKEN
            + "_id";

    public static final String ACCESS_TOKEN_ID = ACCESS_TOKEN + "_id";

    /*********************************************************
     * This parameter is used to indicate whether request is for oidc.
     **********************************************************/
    public final static String REQUEST_FEATURE = "request_feature";
    public final static String REQUEST_FEATURE_OIDC = "oidc";
    public final static String REQUEST_FEATURE_OAUTH2 = "oauth2";

    /**
     * This is a custom response attribute for when a bearer token is presented
     * - it allows a mediator or resource application to know the OAuth
     * client to whom the authorization grant was originally provided. It can be
     * used (for example) to compare against the username to see if client
     * credentials grant type was used (i.e. two-legged OAuth 2.0).
     */
    public static final String OAUTH_TOKEN_CLIENT_ID = "oauth_token_client_id";

    public static final String[] OAuth20RequestParameters = { CLIENT_ID,
            CLIENT_SECRET, RESPONSE_TYPE, REDIRECT_URI, SCOPE, STATE,
            GRANT_TYPE, CODE, USERNAME, PASSWORD, REFRESH_TOKEN, ACCESS_TOKEN };

    public static final Set<String> OAuth20RequestParametersSet = new HashSet<String>(
            Arrays.asList(OAuth20RequestParameters));
    public static final String SIGNATURE_ALGORITHM_RS256 = "RS256";
    public static final String SIGNATURE_ALGORITHM_HS256 = "HS256";
    public static final String SIGNATURE_ALGORITHM_NONE = "none"; // default to HS256
    public static final String EXTERNAL_CLAIM_NAMES = "externalClaimNames";
    public static final String EXTERNAL_CLAIMS = "com.ibm.wsspi.security.oidc.external.claims"; //
    public static final String EXTERNAL_CLAIMS_PREFIX = "com.ibm.wsspi.security.oidc.external.claims:"; //
    public static final String EXTERNAL_MEDIATION = "com.ibm.wsspi.security.oidc.external.mediation"; //
    public static final int EXTERNAL_CLAIMS_PREFIX_LENGTH = EXTERNAL_CLAIMS_PREFIX.length(); //

    public static final String DEFAULT_AUTHZ_LOGIN_URL = "login.jsp";

    public static final String PROXY_HOST = "X-Forwarded-Host";
    public final static String ATTRTYPE_PARAM_HEADER = "urn:ibm:names:header:param";

    // for resource since it's the parameter handled by oauth by related oidc a lot
    final static public String OAUTH20_AUTHZ_PARAM_RESOURCES = "resource";
    public static final String OAUTH20_AUTHZ_PARAM_NONCE = "nonce";
    public static final String OAUTH20_AUTHEN_PARAM_RESOURCE = "urn:ibm:names:authn:param:resource";

    // constants used for processing long-lived app-passwords or app-tokens
    public static final String APP_PASSWORD_STATE_ID = "iamapppasswordstateid";
    public static final String APP_TOKEN_STATE_ID = "iamapptokenstateid";
    public static final String APP_PASSWORD = "app_password";
    public static final String APP_TOKEN = "app_token";
    public static final String APP_PASSWORD_URI = "app-passwords"; // also hardcoded in oidc server metatype.xml
    public static final String APP_TOKEN_URI = "app-tokens"; // also hardcoded in oidc server metatype.xml
    public static final String AUTHORIZE_URI = "authorize"; // also hardcoded in oidc server metatype.xml
    public static final String USERS_TOKEN_MGMT_URI = "usersTokenManagement"; // also hardcoded in oidc server metatype.xml
    public static final String PERS_TOKEN_MGMT_URI = "personalTokenManagement"; // also hardcoded in oidc server metatype.xml
    public static final String CLIENT_MGMT_URI = "clientManagement"; // also hardcoded in oidc server metatype.xml
    public static final String CLIENT_METATYPE_URI = "clientMetatype";
    public static final String USED_FOR = "used_for";
    public static final String USED_BY = "used_by";
    public static final String APP_NAME = "app_name";
    public static final String APP_ID = "app_id";
    public static final String LAST_ACCESS = "last_access";
    public static final String CREATED_AT = "created_at";
    public static final String EXPIRES_AT = "expires_at";
    public static final String TOKEN_MANAGER_ROLE = "tokenManager";
    public static final String PARAM_USER_ID = "user_id";

    public static final String REFRESH_TOKEN_ORIGINAL_GT = "originalGrantType";
    public static final String HASH = "hash"; // matches result from PasswordUtil.getCryptoAlgorithm
    public static final String APP_PASSWORD_HASH_SALT = "notrandom";
    public static final String PLAIN_ENCODING = "plain";
    public static final String APP_PASSWORD_TOKEN_STATE_ID = "iamapppasswordorapptokenstateid";
    public final static String XOR = "xor"; // matches result from PasswordUtil.getCryptoAlgorithm
    public static final String SALT = "salt";
    public static final String HASH_ALGORITHM = "hash_alg";
    public static final String HASH_ITERATIONS = "hash_itr";
    public static final String HASH_LENGTH = "hash_len";

}
