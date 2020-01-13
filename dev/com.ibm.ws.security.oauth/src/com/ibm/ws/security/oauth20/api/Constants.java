/*******************************************************************************
 * Copyright (c) 1997, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.api;

public class Constants {

    // command input parameters
    public static final String PARM_CONFIG_FILE = "fileName";
    public static final String PARM_PROVIDER_NAME = "providerName";

    // XML parameters
    public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    public static final String XML_CAT_PARAM = "OAuthServiceConfiguration";
    public static final String XML_TAG_PARAM = "parameter";
    public static final String XML_TAG_VALUE = "value";
    public static final String XML_ATTR_PARAM_TYPE = "type";
    public static final String XML_ATTR_PARAM_NAME = "name";
    public static final String XML_ATTR_PARAM_CUSTOMIZABLE = "customizable";
    public static final String XML_PARAM_TYPE_WAS = "ws";
    public static final String XML_PARAM_TYPE_COMPONENT = "cc";
    public static final String XML_PARAM_TYPE_TAI = "tai";

    public static final String XML_CAT_CLIENT = "OAuthClientConfiguration";
    public static final String XML_TAG_CLIENT = "client";
    public static final String XML_ATTR_CLIENT_ID = "id";
    public static final String XML_ATTR_CLIENT_COMPONENT = "component";
    public static final String XML_ATTR_CLIENT_SECRET = "secret";
    public static final String XML_ATTR_CLIENT_DISPLAYNAME = "displayname";
    public static final String XML_ATTR_CLIENT_REDIRECT = "redirect";
    public static final String XML_ATTR_CLIENT_ENABLED = "enabled";

    public static final String XML_TEMP_FILENAME_SUFFIX = "_temp";

    // Environment variables
    public static final String CONFIG_FILE_DIR = "oauth20";
    public static final String RESOURCE_BUNDLE = "com.ibm.ws.security.oauth20.resources.ProviderMsgs";

    // extended parameter names
    public static final String PARAM_AUTHZ_FORM_TEMPLATE = "oauth20.authorization.form.template";
    public static final String PARAM_AUTHZ_LOGIN_URL = "oauth20.authorization.loginURL";
    public static final String PARAM_AUTHZ_ERROR_TEMPLATE = "oauth20.authorization.error.template";

    // dynacache config keys
    public static final String DYNACACHE_CONFIG_MEM_TOKENS = "oauth20.token.cache.jndi.tokens";
    public static final String DYNACACHE_CONFIG_MEM_TOKENOWNERS = "oauth20.token.cache.jndi.users";
    public static final String DYNACACHE_CONFIG_DB_TOKENS = "oauth20.db.token.cache.jndi.tokens";
    public static final String DYNACACHE_CONFIG_DB_CLIENTS = "oauth20.db.token.cache.jndi.clients";

    // dynacache defaults
    public static final String DEFAULT_DYNACACHE_JNDI_MEM_TOKENS = "services/cache/OAuth20MemTokenCache";
    public static final String DEFAULT_DYNACACHE_JNDI_MEM_TOKENOWNERS = "services/cache/OAuth20MemTokenOwnerCache";
    public static final String DEFAULT_DYNACACHE_JNDI_DB_TOKENS = "services/cache/OAuth20DBTokenCache";
    public static final String DEFAULT_DYNACACHE_JNDI_DB_CLIENTS = "services/cache/OAuth20DBClientCache";

    // Consent form prompt values
    public static final String PROMPT_NONE = "none";
    public static final String PROMPT_CONSENT = "consent";
    public static final String PROMPT_LOGIN = "login";

    // Optional config params
    public static final String CLIENT_URI_SUBSTITUTIONS = "oauth20.client.uri.substitutions";

    public static final String OAUTH20_CONSENT_CACHE_ENTRY_LIFETIME_SECONDS = "oauth20.consent.cache.entry.lifetime.seconds";
    public static final String OAUTH20_CONSENT_CACHE_SIZE = "oauth20.consent.cache.size";

    public static final String INVOKE_TAI_BEFORE_SSO = "com.ibm.websphere.security.InvokeTAIbeforeSSO";

    public static final String USER_CLIENT_TOKEN_LIMIT = "oauth20.token.userClientTokenLimit";

    public static final String TOKEN_STORE_SIZE = "tokenStoreSize";

    public static final String REQUEST_AUTHEN_TYPE = "request.authentication.type";
    public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

    /*
     * Constants for validation endpoint result JSON
     */
    public static final String INTROSPECT_CLAIM_IAT = "iat";
    public static final String INTROSPECT_CLAIM_AUD = "aud";
    public static final String INTROSPECT_CLAIM_SUB = "sub";
    public static final String INTROSPECT_CLAIM_EXP = "exp";
    public static final String INTROSPECT_CLAIM_SCOPE = "scope";
    public static final String INTROSPECT_CLIENT_ID = "client_id";
    public static final String INTROSPECT_CLAIM_ACTIVE = "active";
    public static final String INTROSPECT_CLAIM_TOKEN_TYPE = "token_type";
    public static final String INTROSPECT_CLAIM_GRANT_TYPE = "grant_type";
    public static final String INTROSPECT_CLAIM_FUNCTIONAL_USERID = "functional_user_id";
    public static final String INTROSPECT_CLAIM_FUNCTIONAL_USER_GROUPIDS = "functional_user_groupIds";
    public static final String INTROSPECT_CLAIM_ISS = "iss";

    public static final String USERINFO_CLAIM_SUB = "sub";

    public static final String ERROR_CODE_INVALID_REQUEST = "invalid_request";
    public static final String ERROR_CODE_INVALID_TOKEN = "invalid_token";
    public static final String ERROR_CODE_UNAUTHORIZED_CLIENT = "unauthorized_client";
    public static final String ERROR_CODE_INVALID_CLIENT = "invalid_client";
    public static final String ERROR_CODE_INVALID_GRANT = "invalid_grant";
    public static final String ERROR_CODE_UNSUPPORTED_TOKEN_TYPE = "unsupported_token_type";
    public static final String ERROR_CODE_INSUFFICIENT_SCOPE = "insufficient_scope";

    // an attribute name which indicates initial login
    public static final String ATTR_AFTERLOGIN = "afterLogin";
    public static final String ALL_SCOPES = "ALL_SCOPES";
}
