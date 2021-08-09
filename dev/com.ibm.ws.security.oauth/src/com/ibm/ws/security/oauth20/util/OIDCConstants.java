/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.util;

import java.util.Arrays;
import java.util.HashSet;

import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;

public interface OIDCConstants extends OAuth20Constants {

    public static final String RESPONSE_TYPE_ID_TOKEN = "id_token";
    public static final String ID_TOKEN = "id_token";
    public static final String TOKENTYPE_ID_TOKEN = ID_TOKEN;
    public static final String SCOPE_OPENID = "openid";
    public static final String DEFAULT_OIDC_GRANT_TYPE_HANDLER_FACTORY_CLASSNAME = "com.ibm.ws.security.openidconnect.server.plugins.OIDCGrantTypeHandlerFactoryImpl";
    public static final String DEFAULT_OIDC10_RESPONSE_TYPE_HANDLER_FACTORY_CLASSNAME = "com.ibm.ws.security.openidconnect.server.plugins.OIDCResponseTypeHandlerFactoryImpl";
    public static final String DEFAULT_ID_TOKEN_HANDLER_CLASS = "com.ibm.ws.security.openidconnect.server.plugins.IDTokenHandler";
    public static final String SIGNATURE_ALGORITHM = "signatureAlgorithm";
    public static final String PREDEFINED_SCOPES = "predefinedScopes";
    public static final String DEFAULT_SCOPE = "defaultScope";
    public static final String ASSERTION = "assertion";
    /**
     * Defines the implementation class for an id token issuer. Currently
     * only one internal implementation is supported
     */
    public static final String OAUTH20_ID_TOKENTYPEHANDLER_CLASSNAME = com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigurationImpl.OAUTH20_ID_TOKENTYPEHANDLER_CLASSNAME; // "oauth20.id.tokentypehandler.classname";

    /**
     * Defines the implementation class for an OAuth20GrantTypeHandlerFactory
     * only one internal implementation is supported
     */
    public static final String OAUTH20_GRANT_TYPE_HANDLER_FACTORY_CLASSNAME = com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigurationImpl.OAUTH20_GRANT_TYPE_HANDLER_FACTORY_CLASSNAME; // "oauth20.grant.type.handler.factory.classname";

    /**
     * Defines the implementation class for an OAuth20GrantTypeHandlerFactory
     * only one internal implementation is supported
     */
    public static final String OAUTH20_RESPONSE_TYPE_HANDLER_FACTORY_CLASSNAME = com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigurationImpl.OAUTH20_RESPONSE_TYPE_HANDLER_FACTORY_CLASSNAME; // "oauth20.response.type.handler.factory.classname";

    /* defined encryption types */
    public static final String OIDC_ENC_TYPE_RS256 = "RS256";
    public static final String OIDC_ENC_TYPE_HS256 = "HS256";
    public static final String OIDC_ENC_TYPE_NONE = "none";

    /* optional parameters for authorization request */
    public static final String OIDC_AUTHZ_PARAM_NONCE = OAUTH20_AUTHZ_PARAM_NONCE; // "nonce";
    public static final String OIDC_AUTHZ_PARAM_DISPLAY = "display";
    public static final String OIDC_AUTHZ_PARAM_PROMPT = "prompt";
    public static final String OIDC_AUTHZ_PARAM_MAX_AGE = "max_age";
    public static final String OIDC_AUTHZ_PARAM_UI_LOCALES = "ui_locales";
    public static final String OIDC_AUTHZ_PARAM_CLAIMS_LOCALES = "claims_locales";
    public static final String OIDC_AUTHZ_PARAM_ID_TOKEN_HINT = "id_token_hint";
    public static final String OIDC_AUTHZ_PARAM_LOGIN_HINT = "login_hint";
    public static final String OIDC_AUTHZ_PARAM_ACR_VALUES = "acr_values";
    public static final String OIDC_AUTHZ_PARAM_RESPONSE_MODE = "response_mode";

    public static final String OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_USERNAME = "id_token_hint_username";
    public static final String OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_CLIENTID = "id_token_hint_clientid";
    public static final String OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS = "id_token_hint_status";
    public static final String OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS_SUCCESS = "success";
    public static final String OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS_FAIL_INVALID_ID_TOKEN = "fail_invalid_id_token";

    /* defined values for display parameter */
    public static final String OIDC_AUTHZ_PARAM_DISPLAY_PAGE = "page";
    public static final String OIDC_AUTHZ_PARAM_DISPLAY_POPUP = "popup";
    public static final String OIDC_AUTHZ_PARAM_DISPLAY_TOUCH = "touch";
    public static final String OIDC_AUTHZ_PARAM_DISPLAY_WAP = "wap";

    /* defined values for prompt parameter */
    public static final String OIDC_AUTHZ_PARAM_PROMPT_NONE = "none";
    public static final String OIDC_AUTHZ_PARAM_PROMPT_LOGIN = "login";
    public static final String OIDC_AUTHZ_PARAM_PROMPT_CONSENT = "consent";
    public static final String OIDC_AUTHZ_PARAM_PROMPT_SELECT = "select_account";

    /* defined attribute names for session support */
    public static final String OIDC_SESSION_STATE = "session_state";
    public static final String OIDC_SESSION_MANAGED = "sessionManaged";

    public static final String OIDC_LOGOUT_AUTO_LOGOUT = "autoLogout";
    public static final String OIDC_LOGOUT_ID_TOKEN_HINT = OIDC_AUTHZ_PARAM_ID_TOKEN_HINT;
    public static final String OIDC_LOGOUT_REDIRECT_URI = "post_logout_redirect_uri";

    /* parameters for oidc discovery response */
    public static final String OIDC_DISC_ISSUER = "issuer";
    public static final String OIDC_DISC_AUTH_EP = "authorization_endpoint";
    public static final String OIDC_DISC_TOKEN_EP = "token_endpoint";
    public static final String OIDC_DISC_JWKS_URI = "jwks_uri";
    public static final String OIDC_DISC_RESP_TYPES_SUPP = "response_types_supported";
    public static final String OIDC_DISC_SUB_TYPES_SUPP = "subject_types_supported";
    public static final String OIDC_DISC_ID_TOKEN_SIGNING_ALG_VAL_SUPP = "id_token_signing_alg_values_supported";
    public static final String OIDC_DISC_USERINFO_EP = "userinfo_endpoint";
    public static final String OIDC_DISC_REGISTRATION_EP = "registration_endpoint";
    public static final String OIDC_DISC_SCOPES_SUPP = "scopes_supported";
    public static final String OIDC_DISC_CLAIMS_SUPP = "claims_supported";
    public static final String OIDC_DISC_RESP_MODES_SUPP = "response_modes_supported";
    public static final String OIDC_DISC_GRANT_TYPES_SUPP = "grant_types_supported";
    public static final String OIDC_DISC_ACR_VALUES_SUPP = "acr_values_supported";
    public static final String OIDC_DISC_ID_TOKEN_ENC_ALG_VAL_SUPP = "id_token_encryption_alg_values_supported";
    public static final String OIDC_DISC_ID_TOKEN_ENC_ENC_VAL_SUPP = "id_token_encryption_enc_values_supported";
    public static final String OIDC_DISC_USERINFO_SIGNING_ALG_VAL_SUPP = "userinfo_signing_alg_values_supported";
    public static final String OIDC_DISC_USERINFO_ENC_ALG_VAL_SUPP = "userinfo_encryption_alg_values_supported";
    public static final String OIDC_DISC_USERINFO_ENC_ENC_VAL_SUPP = "userinfo_encryption_enc_values_supported";
    public static final String OIDC_DISC_REQ_OBJ_SIGNING_ALG_VAL_SUPP = "request_object_signing_alg_values_supported";
    public static final String OIDC_DISC_REQ_OBJ_ENC_ALG_VAL_SUPP = "request_object_encryption_alg_values_supported";
    public static final String OIDC_DISC_REQ_OBJ_ENC_ENC_VAL_SUPP = "request_object_encryption_enc_values_supported";
    public static final String OIDC_DISC_TOKEN_EP_AUTH_METH_SUPP = "token_endpoint_auth_methods_supported";
    public static final String OIDC_DISC_TOKEN_EP_AUTH_SIGNING_ALG_VAL_SUPP = "token_endpoint_auth_signing_alg_values_supported";
    public static final String OIDC_DISC_DISPLAY_VAL_SUPP = "display_values_supported";
    public static final String OIDC_DISC_CLAIM_TYPES_SUPP = "claim_types_supported";
    public static final String OIDC_DISC_SERVICE_DOC = "service_documentation";
    public static final String OIDC_DISC_CLAIMS_LOCALES_SUPP = "claims_locales_supported";
    public static final String OIDC_DISC_UI_LOCALES_SUPP = "ui_locales_supported";
    public static final String OIDC_DISC_CLAIMS_PARAM_SUPP = "claims_parameter_supported";
    public static final String OIDC_DISC_REQ_PARAM_SUPP = "request_parameter_supported";
    public static final String OIDC_DISC_REQ_URI_PARAM_SUPP = "request_uri_parameter_supported";
    public static final String OIDC_DISC_REQUIRE_REQ_URI_REGISTRATION = "require_request_uri_registration";
    public static final String OIDC_DISC_OP_POLICY_URI = "op_policy_uri";
    public static final String OIDC_DISC_OP_TOS_URI = "op_tos_uri";
    public static final String OIDC_DISC_REVOKE_EP = "revocation_endpoint";
    public static final String OIDC_DISC_APP_PASSWORDS_EP = "app_passwords_endpoint";
    public static final String OIDC_DISC_APP_TOKENS_EP = "app_tokens_endpoint";
    public static final String OIDC_DISC_PERSONAL_TOKEN_MGMT_EP = "personal_token_mgmt_endpoint";
    public static final String OIDC_DISC_USERS_TOKEN_MGMT_EP = "users_token_mgmt_endpoint";
    public static final String OIDC_DISC_CLIENT_MGMT_EP = "client_mgmt_endpoint";
    public static final String OIDC_DISC_PKCE_CODE_CHALLENGE_METHODS_SUPP = "code_challenge_methods_supported";

    /* parameters for oidc discovery response with origins from session management */
    public static final String OIDC_SESS_CHECK_SESSION_IFRAME = "check_session_iframe";
    public static final String OIDC_SESS_END_SESSION_EP = "end_session_endpoint";

    /* parameters for WAS/Rational based oidc discovery response with origins from Jazz */
    public static final String JAS_INTROSPECTION_EP = "introspection_endpoint";
    public static final String JAS_COVERAGE_MAP_EP = "coverage_map_endpoint";
    public static final String JAS_PROXY_EP = "proxy_endpoint";
    public static final String JAS_BACKING_IDP_URI_PREFIX = "backing_idp_uri_prefix";

    /* defined values for various oidc discovery response properties */
    public static final String OIDC_DISC_RESP_TYPES_SUPP_CODE = "code";
    public static final String OIDC_DISC_RESP_TYPES_SUPP_TOKEN = "token";
    public static final String OIDC_DISC_RESP_TYPES_SUPP_ID_TOKEN = "id_token";

    public static final String OIDC_DISC_RESP_TYPES_SUPP_ID_TOKEN_TOKEN = OIDC_DISC_RESP_TYPES_SUPP_ID_TOKEN + " " + OIDC_DISC_RESP_TYPES_SUPP_TOKEN;

    public static final String OIDC_DISC_RESP_TYPES_SUPP_TOKEN_ID_TOKEN = OIDC_DISC_RESP_TYPES_SUPP_TOKEN + " " + OIDC_DISC_RESP_TYPES_SUPP_ID_TOKEN;

    public static final String[] OIDC_SUPP_RESP_TYPES = { OIDC_DISC_RESP_TYPES_SUPP_CODE, OIDC_DISC_RESP_TYPES_SUPP_TOKEN,
            OIDC_DISC_RESP_TYPES_SUPP_ID_TOKEN_TOKEN, OIDC_DISC_RESP_TYPES_SUPP_TOKEN_ID_TOKEN,
            OIDC_DISC_RESP_TYPES_SUPP_ID_TOKEN };

    public static final HashSet<String> OIDC_SUPP_RESP_TYPES_SET = new HashSet<String>(Arrays.asList(OIDC_SUPP_RESP_TYPES));

    public static final String OIDC_DISC_SUB_TYPES_SUPP_PAIRWISE = "pairwise";
    public static final String OIDC_DISC_SUB_TYPES_SUPP_PUBLIC = "public";

    public static final String OIDC_DISC_ID_TOKEN_SIGNING_ALG_VAL_SUPP_RS256 = OIDC_ENC_TYPE_RS256;
    public static final String OIDC_DISC_ID_TOKEN_SIGNING_ALG_VAL_SUPP_HS256 = OIDC_ENC_TYPE_HS256;
    public static final String OIDC_DISC_ID_TOKEN_SIGNING_ALG_VAL_SUPP_NONE = OIDC_ENC_TYPE_NONE;

    public static final String OIDC_DISC_SCOPES_SUPP_OPENID = "openid";
    public static final String OIDC_DISC_SCOPES_SUPP_PROFILE = "profile";
    public static final String OIDC_DISC_SCOPES_SUPP_EMAIL = "email";
    public static final String OIDC_DISC_SCOPES_SUPP_ADDRESS = "address";
    public static final String OIDC_DISC_SCOPES_SUPP_PHONE = "phone";
    public static final String OIDC_DISC_SCOPES_SUPP_OFFLINE_ACC = "offline_access";
    public static final String OIDC_DISC_SCOPES_SUPP_GENERAL = "general"; // Invented by Jazz

    public static final String OIDC_DISC_CLAIMS_SUPP_SUB = "sub";
    public static final String OIDC_DISC_CLAIMS_SUPP_NAME = "name";
    public static final String OIDC_DISC_CLAIMS_SUPP_PREF_USERNAME = "preferred_username";
    public static final String OIDC_DISC_CLAIMS_SUPP_PROFILE = "profile";
    public static final String OIDC_DISC_CLAIMS_SUPP_PICTURE = "picture";
    public static final String OIDC_DISC_CLAIMS_SUPP_EMAIL = "email";
    public static final String OIDC_DISC_CLAIMS_SUPP_LOCALE = "locale";
    public static final String OIDC_DISC_CLAIMS_SUPP_GROUP_IDS = "groupIds"; // Invented by WAS

    public static final String OIDC_DISC_RESP_MODES_SUPP_QUERY = "query";
    public static final String OIDC_DISC_RESP_MODES_SUPP_FRAG = "fragment";
    public static final String OIDC_DISC_RESP_MODES_SUPP_FORM_POST = "form_post";

    public static final String OIDC_DISC_GRANT_TYPES_SUPP_AUTH_CODE = OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE;
    public static final String OIDC_DISC_GRANT_TYPES_SUPP_IMPLICIT = OAuth20Constants.GRANT_TYPE_IMPLICIT;
    public static final String OIDC_DISC_GRANT_TYPES_SUPP_REFRESH_TOKEN = OAuth20Constants.GRANT_TYPE_REFRESH_TOKEN;
    public static final String OIDC_DISC_GRANT_TYPES_SUPP_CLIENT_CREDS = OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS;
    public static final String OIDC_DISC_GRANT_TYPES_SUPP_PASSWORD = OAuth20Constants.GRANT_TYPE_PASSWORD;
    public static final String OIDC_DISC_GRANT_TYPES_SUPP_JWT_BEARER = "urn:ietf:params:oauth:grant-type:jwt-bearer";

    public static final String OIDC_DISC_USERINFO_SIGNING_ALG_VAL_SUPP_RS256 = OIDC_ENC_TYPE_RS256;
    public static final String OIDC_DISC_USERINFO_SIGNING_ALG_VAL_SUPP_HS256 = OIDC_ENC_TYPE_HS256;
    public static final String OIDC_DISC_USERINFO_SIGNING_ALG_VAL_SUPP_NONE = OIDC_ENC_TYPE_NONE;

    public static final String OIDC_DISC_TOKEN_EP_AUTH_METH_SUPP_CLIENT_SECRET_POST = "client_secret_post";
    public static final String OIDC_DISC_TOKEN_EP_AUTH_METH_SUPP_CLIENT_SECRET_BASIC = "client_secret_basic";
    public static final String OIDC_DISC_TOKEN_EP_AUTH_METH_SUPP_CLIENT_SECRET_JWT = "client_secret_jwt";
    public static final String OIDC_DISC_TOKEN_EP_AUTH_METH_SUPP_PRIVATE_KEY_JWT = "private_key_jwt";
    public static final String OIDC_DISC_TOKEN_EP_AUTH_METH_SUPP_NONE = "none";

    public static final String OIDC_DISC_CLAIM_TYPES_SUPP_NORMAL = "normal";
    public static final String OIDC_DISC_CLAIM_TYPES_SUPP_AGGREGATED = "aggregated";
    public static final String OIDC_DISC_CLAIM_TYPES_SUPP_DISTRIBUTED = "distributed";

    public static final String OIDC_DISC_PKCE_CODE_CHALLENGE_METHODS_SUPP_PLAIN = "plain";
    public static final String OIDC_DISC_PKCE_CODE_CHALLENGE_METHODS_SUPP_S256 = "S256";

    public static final String[] OIDC_DISC_PKCE_CODE_CHALLENGE_METHODS_SUPPORTED = { OIDC_DISC_PKCE_CODE_CHALLENGE_METHODS_SUPP_PLAIN, OIDC_DISC_PKCE_CODE_CHALLENGE_METHODS_SUPP_S256 };

    // OIDC Discovery Properties for use in OAuth20Parameter
    public static final String KEY_OIDC_ISSUER_ID = "issuerIdentifier";
    public static final String KEY_OIDC_AUTHORIZATION_EP = "authorizationEndpoint";
    public static final String KEY_OIDC_TOKEN_EP = "tokenEndpoint";
    public static final String KEY_OIDC_JWKS_URI = "jwksURI";
    public static final String KEY_OIDC_RESPONSE_TYPES_SUPP = "responseTypesSupported";
    public static final String KEY_OIDC_SUB_TYPES_SUPP = "subjectTypesSupported";
    public static final String KEY_OIDC_ID_TOKEN_SIGNING_ALG_VAL_SUPP = "idTokenSigningAlgValuesSupported";
    public static final String KEY_OIDC_USERINFO_EP = "userinfoEndpoint";
    public static final String KEY_OIDC_REGISTRATION_EP = "registrationEndpoint";
    public static final String KEY_OIDC_SCOPES_SUPP = "scopesSupported";
    public static final String KEY_OIDC_CLAIMS_SUPP = "claimsSupported";
    public static final String KEY_OIDC_RESP_MODES_SUPP = "responseModesSupported";
    public static final String KEY_OIDC_GRANT_TYPES_SUPP = "grantTypesSupported";
    public static final String KEY_OIDC_TOKEN_EP_AUTH_METHODS_SUPP = "tokenEndpointAuthMethodsSupported";
    public static final String KEY_OIDC_DISPLAY_VAL_SUPP = "displayValuesSupported";
    public static final String KEY_OIDC_CLAIM_TYPES_SUPP = "claimTypesSupported";
    public static final String KEY_OIDC_CLAIM_PARAM_SUPP = "claimsParameterSupported";
    public static final String KEY_OIDC_REQ_PARAM_SUPP = "requestParameterSupported";
    public static final String KEY_OIDC_REQ_URI_PARAM_SUPP = "requestUriParameterSupported";
    public static final String KEY_OIDC_REQUIRE_REQ_URI_REGISTRATION = "requireRequestUriRegistration";
    public static final String KEY_OIDC_CHECK_SESSION_IFRAME = "checkSessionIframe";
    public static final String KEY_OIDC_END_SESSION_EP = "endSessionEndpoint";
    public static final String KEY_OIDC_INTROSPECTION_EP = "introspectionEndpoint";
    public static final String KEY_OIDC_COVERAGE_MAP_EP = "coverageMapEndpoint";
    public static final String KEY_OIDC_PROXY_EP = "proxyEndpoint";
    public static final String KEY_OIDC_BACKING_IDP_URI_PREFIX = "backingIdpUriPrefix";
    public static final String KEY_OIDC_REVOKE_EP = "revocationEndpoint";
    public static final String KEY_OIDC_APP_PASSWORDS_EP = "appPasswordsEndpoint";
    public static final String KEY_OIDC_APP_TOKENS_EP = "appTokensEndpoint";
    public static final String KEY_OIDC_PERSONAL_TOKEN_MGMT_EP = "personalTokenMgmtEndpoint";
    public static final String KEY_OIDC_USERS_TOKEN_MGMT_EP = "usersTokenMgmtEndpoint";
    public static final String KEY_OIDC_CLIENT_MGMT_EP = "clientMgmtEndpoint";
    public static final String KEY_OIDC_PKCE_CODE_CHALLENGE_METHODS_SUPP = "codeChallengeMethodsSupported";

    // Server config property for coverageMap
    static final String JSA_QUAL = "jsa.provider.";
    public static final String KEY_JSA_COVERAGE_MAP_SESSION_MAX_AGE = "coverageMapSessionMaxAge";
    public static String KEY_JSA_COVERAGE_MAP_SESSION_MAX_AGE_QUAL = JSA_QUAL + KEY_JSA_COVERAGE_MAP_SESSION_MAX_AGE;

    // OIDC Discovery Properties (Qualified) used as keys in list of OAuth20Parameters
    static final String OIDC_QUAL = "oidc.provider.";
    public static final String KEY_OIDC_ISSUER_ID_QUAL = OIDC_QUAL + KEY_OIDC_ISSUER_ID;
    public static final String KEY_OIDC_AUTHORIZATION_EP_QUAL = OIDC_QUAL + KEY_OIDC_AUTHORIZATION_EP;
    public static final String KEY_OIDC_TOKEN_EP_QUAL = OIDC_QUAL + KEY_OIDC_TOKEN_EP;
    public static final String KEY_OIDC_JWKS_URI_QAL = OIDC_QUAL + KEY_OIDC_JWKS_URI;
    public static final String KEY_OIDC_RESPONSE_TYPES_SUPP_QUAL = OIDC_QUAL + KEY_OIDC_RESPONSE_TYPES_SUPP;
    public static final String KEY_OIDC_SUB_TYPES_SUPP_QUAL = OIDC_QUAL + KEY_OIDC_SUB_TYPES_SUPP;
    public static final String KEY_OIDC_ID_TOKEN_SIGNING_ALG_VAL_SUPP_QUAL = OIDC_QUAL + KEY_OIDC_ID_TOKEN_SIGNING_ALG_VAL_SUPP;
    public static final String KEY_OIDC_USERINFO_EP_QUAL = OIDC_QUAL + KEY_OIDC_USERINFO_EP;
    public static final String KEY_OIDC_REGISTRATION_EP_QUAL = OIDC_QUAL + KEY_OIDC_REGISTRATION_EP;
    public static final String KEY_OIDC_SCOPES_SUPP_QUAL = OIDC_QUAL + KEY_OIDC_SCOPES_SUPP;
    public static final String KEY_OIDC_CLAIMS_SUPP_QUAL = OIDC_QUAL + KEY_OIDC_CLAIMS_SUPP;
    public static final String KEY_OIDC_RESP_MODES_SUPP_QUAL = OIDC_QUAL + KEY_OIDC_RESP_MODES_SUPP;
    public static final String KEY_OIDC_GRANT_TYPES_SUPP_QUAL = OIDC_QUAL + KEY_OIDC_GRANT_TYPES_SUPP;
    public static final String KEY_OIDC_TOKEN_EP_AUTH_METHODS_SUPP_QUAL = OIDC_QUAL + KEY_OIDC_TOKEN_EP_AUTH_METHODS_SUPP;
    public static final String KEY_OIDC_DISPLAY_VAL_SUPP_QUAL = OIDC_QUAL + KEY_OIDC_DISPLAY_VAL_SUPP;
    public static final String KEY_OIDC_CLAIM_TYPES_SUPP_QUAL = OIDC_QUAL + KEY_OIDC_CLAIM_TYPES_SUPP;
    public static final String KEY_OIDC_CLAIM_PARAM_SUPP_QUAL = OIDC_QUAL + KEY_OIDC_CLAIM_PARAM_SUPP;
    public static final String KEY_OIDC_REQ_PARAM_SUPP_QUAL = OIDC_QUAL + KEY_OIDC_REQ_PARAM_SUPP;
    public static final String KEY_OIDC_REQ_URI_PARAM_SUPP_QUAL = OIDC_QUAL + KEY_OIDC_REQ_URI_PARAM_SUPP;
    public static final String KEY_OIDC_REQUIRE_REQ_URI_REGISTRATION_QUAL = OIDC_QUAL + KEY_OIDC_REQUIRE_REQ_URI_REGISTRATION;
    public static final String KEY_OIDC_CHECK_SESSION_IFRAME_QUAL = OIDC_QUAL + KEY_OIDC_CHECK_SESSION_IFRAME;
    public static final String KEY_OIDC_END_SESSION_EP_QUAL = OIDC_QUAL + KEY_OIDC_END_SESSION_EP;
    public static final String KEY_OIDC_INTROSPECTION_EP_QUAL = OIDC_QUAL + KEY_OIDC_INTROSPECTION_EP;
    public static final String KEY_OIDC_COVERAGE_MAP_EP_QUAL = OIDC_QUAL + KEY_OIDC_COVERAGE_MAP_EP;
    public static final String KEY_OIDC_PROXY_EP_QUAL = OIDC_QUAL + KEY_OIDC_PROXY_EP;
    public static final String KEY_OIDC_BACKING_IDP_URI_PREFIX_QUAL = OIDC_QUAL + KEY_OIDC_BACKING_IDP_URI_PREFIX;
    public static final String KEY_OIDC_REVOKE_EP_QUAL = OIDC_QUAL + KEY_OIDC_REVOKE_EP;
    public static final String KEY_OIDC_APP_PASSWORDS_EP_QUAL = OIDC_QUAL + KEY_OIDC_APP_PASSWORDS_EP;
    public static final String KEY_OIDC_APP_TOKENS_EP_QUAL = OIDC_QUAL + KEY_OIDC_APP_TOKENS_EP;
    public static final String KEY_OIDC_PERSONAL_TOKEN_MGMT_EP_QUAL = OIDC_QUAL + KEY_OIDC_PERSONAL_TOKEN_MGMT_EP;
    public static final String KEY_OIDC_USERS_TOKEN_MGMT_EP_QUAL = OIDC_QUAL + KEY_OIDC_USERS_TOKEN_MGMT_EP;
    public static final String KEY_OIDC_CLIENT_MGMT_EP_QUAL = OIDC_QUAL + KEY_OIDC_CLIENT_MGMT_EP;
    public static final String KEY_OIDC_PKCE_CODE_CHALLENGE_METHODS_SUPP_QUAL = OIDC_QUAL + KEY_OIDC_PKCE_CODE_CHALLENGE_METHODS_SUPP;

    /**
     * Supported OIDC client registration parameter names
     */
    public static final String OIDC_CLIENTREG_POST_LOGOUT_URIS = "post_logout_redirect_uris";
    public static final String OIDC_CLIENTREG_APP_TYPE = "application_type";
    public static final String OIDC_CLIENTREG_SUB_TYPE = "subject_type";
    public static final String OIDC_CLIENTREG_REDIRECT_URIS = "redirect_uris";
    public static final String OIDC_CLIENTREG_TOKEN_EP_AUTH_METH = "token_endpoint_auth_method";
    public static final String OIDC_CLIENTREG_PREAUTHORIZED_SCOPE = "preauthorized_scope";
    public static final String JSA_CLIENTREG_TRUSTED_URI_PREFIXES = "trusted_uri_prefixes";
    public static final String JSA_CLIENTREG_FUNCTIONAL_USER_GROUP_IDS = "functional_user_groupIds";
    public static final String OIDC_CLIENTREG_ISSUED_AT = "client_id_issued_at";
    public static final String OIDC_CLIENTREG_SECRET_EXPIRES_AT = "client_secret_expires_at";
    public static final String OIDC_CLIENTREG_REGISTRATION_CLIENT_URI = "registration_client_uri";

    /**
     * Supported OIDC client registration parameter values
     */
    public static final String OIDC_CLIENTREG_PARM_NATIVE = "native";
    public static final String OIDC_CLIENTREG_PARM_WEB = "web";

    /*
     * Attribute types used for normalizing OpenIDConnect unique HTTP requests into an
     * AttributeList and preparing responses.
     */
    public final static String ATTRTYPE_REQUEST = "urn:ibm:names:oidc:request";

    /* error codes for endpoints */
    public static final String ERROR_INTERACTION_REQUIRED = "interaction_required";
    public static final String ERROR_LOGIN_REQUIRED = "login_required";
    public static final String ERROR_ACCOUNT_SELECTION_REQUIRED = "account_selection_required";
    public static final String ERROR_CONSENT_REQUIRED = "consent_required";
    public static final String ERROR_INVALID_REQUEST_URI = "invalid_request_uri";
    public static final String ERROR_INVALID_REQUEST_OBJECT = "invalid_request_object";
    public static final String ERROR_REQUEST_NOT_SUPPORTED = "request_not_supported";
    public static final String ERROR_REQUEST_URI_NOT_SUPPORTED = "request_uri_not_supported";
    public static final String ERROR_REGISTRATION_NOT_SUPPORTED = "registration_not_supported";
    public static final String ERROR_INVALID_REDIRECT_URI = "invalid_redirect_uri";
    public static final String ERROR_INVALID_CLIENT_METADATA = "invalid_client_metadata";
    public static final String ERROR_INVALID_CLIENT = "invalid_client";
    public static final String ERROR_INVALID_REQUEST = "invalid_request";
    public static final String ERROR_SERVER_ERROR = "server_error";
    public static final String ERROR_ACCESS_DENIED = "access_denied";

    /* description of error codes */
    /* According to the OAuth2.0 spec, the error_description should not be translated (RFC 6749 OAuth 2.0 4.1.2.1) */
    public static final String MESSAGE_LOGIN_REQUIRED = "End-User authentication is required. The prompt parameter value needs to be modified in order to display a login form, or a user credential needs to be supplied.";
    public static final String MESSAGE_LOGIN_REQUIRED_ID_TOKEN_HINT_MISMATCH = "End-User authentication was unsuccessful. The id_token_hint parameter value does not match the user credential or client id parameter.";
    public static final String MESSAGE_LOGIN_REQUIRED_ID_TOKEN_HINT_INVALID = "End-User authentication was unsuccessful. The id_token_hint parameter value is not valid.";
    public static final String MESSAGE_CONSENT_REQUIRED = "End-User consent is required. The prompt parameter value needs to be modified in order to display a consent form.";

    /************************ jwt_bearer header from HeaderConstants ************************/
    public static final String HEADER_TYPE = "typ";
    public static final String HEADER_CONTENT_TYPE = "cty";
    public static final String HEADER_ALGORITHM = "alg";
    public static final String HEADER_JWK_URL = "jku";
    public static final String HEADER_JWK = "jwk";
    public static final String HEADER_KEY_ID = "kid";
    public static final String HEADER_X509_URL = "x5u";
    public static final String HEADER_X509_TP = "x5t";
    public static final String HEADER_X509_CERT = "x5c";
    public static final String HEADER_CRITICAL = "crit";

    /************************ jwt_bearer payload from PayloadConstants ************************/
    public static final String PAYLOAD_EXPIRATION_TIME_IN_SECS = "exp";
    public static final String PAYLOAD_NOT_BEFORE_TIME_IN_SECS = "nbf";
    public static final String PAYLOAD_ISSUED_AT_TIME_IN_SECS = "iat";
    public static final String PAYLOAD_ISSUER = "iss";
    public static final String PAYLOAD_AUDIENCE = "aud";
    public static final String PAYLOAD_JWTID = "jti";
    public static final String PAYLOAD_TYPE = "typ";
    public static final String PAYLOAD_SUBJECT = "sub";
    public static final String PAYLOAD_AUTHZ_TIME_IN_SECS = "auth_time";
    public static final String PAYLOAD_AUTHORIZED_PARTY = "azp";
    public static final String PAYLOAD_NONCE = "nonce";
    public static final String PAYLOAD_AT_HASH = "at_hash";
    public static final String PAYLOAD_CLASS_REFERENCE = "acr";
    public static final String PAYLOAD_METHODS_REFERENCE = "amr";

    public static final String CLIENT_REDIRECT_URI = "client_redirect_uri";
    /* session management constants */
    public static final String OIDC_BROWSER_STATE_COOKIE = "oidc_bsc";
    public static final String OIDC_BROWSER_STATE_UNAUTHENTICATE = "UNAUTHENTICATE";

    public static final String DEFAULT_TEMPLATE_HTML = "template.html";

    /* uri prefix for a regexp redirect */
    public static final String REGEXP_PREFIX = "regexp:";

}
