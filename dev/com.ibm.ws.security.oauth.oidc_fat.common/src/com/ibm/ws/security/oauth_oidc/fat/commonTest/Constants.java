/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

// import com.ibm.ws.security.openid20.fat.utils.common.FATDataHelpers.parm;

public class Constants extends com.ibm.ws.security.fat.common.Constants {

    // dummy constructor
    public Constants() {
        super();
    }

    /*
     * set TCPMON_LISTENER_PORT to actual port of TCP monitor if you want to
     * collect traffic with the server - only works for http, not https
     */
    // static final int TCPMON_LISTENER_PORT = 9088;
    public static final int TCPMON_LISTENER_PORT = 0;

    public static final int SUBMIT_NUM_TIMES = 1;

    public static final String[] IDP_SERVER_LIST = { "idp_host_1:9080:9443", "idp_host_2:9080:9443", "idp_host_3:9080:9443", "idp_host_3:9080:9443" };
    public static final String[] SHIBBOLETH_SERVER_LIST = { "localhost:8019:8029" };
    public static final String[] SAML_SUPPORTED_PORTS = { "8945", "8946", "8947", "8948", "8949", "8950", "8951", "8952", "8953", "8954" };

    public static final String JSON_LIST_MATCHES = "jsonlistmatches";

    public static final String EXIST_WITH_ANY_VALUE = ".+";

    // public static final parm CONSENT_NOT_REQUESTED = null;

    /* Test types */
    public static final String OIDC = "OIDC";
    public static final String OAUTH = "OAuth";

    public static final String OAUTH_OP = "OAuth_OP";
    public static final String OIDC_OP = "OpenIDConnect_OP";
    public static final String ISAM_OP = "ISAM";
    // public static final String OAUTH = OAUTH_OP;
    // public static final String OIDC = OIDC_OP ;
    public static final String OIDC_RP = "OIDC_RP";
    public static final String GENERIC_SERVER = "Generic_Server";
    public static final String JWT_CONSUMER = "JWT_Consumer";
    public static final String JWT_BUILDER = "JWT_Builder";
    public static final String IDP_SERVER_TYPE = "IDP_SERVER";

    /* Flow types */
    public static final String WEB_CLIENT_FLOW = "web_client_flow";
    public static final String IMPLICIT_FLOW = "Implicit_Flow";
    public static final String CLIENT_CREDENTIAL_FLOW = "client_credential_flow";
    public static final String PASSWORD_FLOW = "password_flow";
    public static final String RP_FLOW = "RP_Flow";

    /* */
    public static final String HEADER = "header";
    public static final String PARM = "parameter";

    /* */
    public static final boolean USE_DERBY = true;
    public static final boolean DO_NOT_USE_DERBY = false;

    public static final boolean USE_MONGODB = true;
    public static final boolean DO_NOT_USE_MONGODB = false;

    public static final String SYS_PROP_PORT_OP_HTTP_DEFAULT = "security_1_HTTP_default"; // "OP_HTTP_default";
    public static final String SYS_PROP_PORT_RP_HTTP_DEFAULT = "security_2_HTTP_default"; // "RP_HTTP_default";
    public static final String SYS_PROP_PORT_IDP_HTTP_DEFAULT = "security_3_HTTP_default"; // "IDP_HTTP_default";
    public static final String SYS_PROP_PORT_OP_HTTPS_DEFAULT = "security_1_HTTP_default.secure"; // "OP_HTTP_default.secure";
    public static final String SYS_PROP_PORT_RP_HTTPS_DEFAULT = "security_2_HTTP_default.secure"; // "RP_HTTP_default.secure";
    public static final String SYS_PROP_PORT_IDP_HTTPS_DEFAULT = "security_3_HTTP_default.secure"; // "IDP_HTTP_default.secure";

    public static final String BOOT_PROP_OIDC_CREATE_JWT = "oidcCreateJWTToken";
    public static final String BOOT_PROP_OIDC_JWK_ENABLED = "oidcJWKEnabled";
    public static final String BOOT_PROP_OIDC_JWK_VALIDATION_URL = "oidcJWKValidationURL";
    public static final String BOOT_PROP_OIDC_JWK_VALIDATION_URL_2 = "oidcJWKValidationURL2";
    public static final String BOOT_PROP_OIDC_SIG_ALG = "oidcSignAlg";
    public static final String BOOT_PROP_OIDC_TOKEN_FORMAT = "oidcTokenFormat";
    public static final String BOOT_PROP_PROVIDER_ROOT = "providerRoot";
    public static final String BOOT_PROP_PROVIDER_SAMPLE = "providerSample";
    public static final String BOOT_PROP_RS_VALIDATION_ENDPOINT = "oAuthOidcRSValidationEndpoint";
    public static final String BOOT_PROP_RS_VALIDATION_TYPE = "oAuthOidcRSValidationType";
    public static final String BOOT_PROP_USERAPI_TYPE = "userApiType";

    /* ********************* Steps in the process *********************** */

    public static final String LOGOUT = "processLogout";

    /* ******************** steps in the OP test process ***************** */
    public static final String INVOKE_OAUTH_CLIENT = "invokeOAuthClient";
    public static final String SUBMIT_TO_AUTH_SERVER = "submitToAuthServer";
    public static final String SUBMIT_TO_AUTH_SERVER_FOR_TOKEN = "submitToAuthServerForToken";
    public static final String SUBMIT_TO_AUTH_SERVER_WITH_BASIC_AUTH = "submitToAuthServerWithBasicAuth";
    public static final String INVOKE_AUTH_SERVER = "invokeAuthorizationServer";
    public static final String INVOKE_AUTH_SERVER_WITH_BASIC_AUTH = "invokeAuthorizationServerWithBasicAuth";
    public static final String PERFORM_LOGIN = "performLogin";
    public static final String PERFORM_ISAM_LOGIN = "performIsamLogin";
    public static final String INVOKE_PROTECTED_RESOURCE = "invokeProtectedResource";
    public static final String INVOKE_RS_PROTECTED_RESOURCE = "invokeRsProtectedResource";
    public static final String INVOKE_INTROSPECTION_ENDPOINT = "invokeIntrospectionEndpoint";
    public static final String INVOKE_USERINFO_ENDPOINT = "invokeUserinfoEndpoint";
    public static final String INVOKE_INTROSPECTION_ENDPOINT_AGAIN = "invokeIntrospectionEndpointAgain";
    public static final String INVOKE_INTROSPECTION_ENDPOINT_AFTER_REVOKE = "invokeIntrospectionEndpointAfterRevoke";
    public static final String INVOKE_REVOCATION_ENDPOINT = "invokeRevocationEndpoint";
    public static final String INVOKE_DISCOVERY_ENDPOINT = "invokeDiscoveryEndpoint";
    public static final String INVOKE_COVERAGEMAP_ENDPOINT = "invokeCoverageMapEndpoint";
    public static final String INVOKE_REGISTRATION_ENDPOINT = "invokeRegistrationEndpoint";
    public static final String INVOKE_REGISTRATION_ENDPOINT_GET = "invokeRegistrationEndpointGet";
    public static final String INVOKE_REGISTRATION_ENDPOINT_UPDATE = "invokeRegistrationEndpointUpdate";
    public static final String INVOKE_REGISTRATION_ENDPOINT_CREATE = "invokeRegistrationEndpointCreate";
    public static final String INVOKE_REFRESH_ENDPOINT = "invokeRefreshEndpoint";
    public static final String INVOKE_REVOKE_ENDPOINT = "invokeRevokeEndpoint";
    public static final String INVOKE_JWT_ENDPOINT = "invokeJwtEndpoint";
    public static final String INVOKE_JWT_TOKEN_ENDPOINT = "invokeJwtTokenEndpoint";
    public static final String INVOKE_ENDPOINT = "invokeEndpoint";
    public static final String INVOKE_AUTH_ENDPOINT = "invokeAuthorizationEndpoint";
    public static final String INVOKE_AUTH_ENDPOINT_WITH_BASIC_AUTH = "invokeAuthorizationEndpointWithBasicAuth";
    public static final String INVOKE_TOKEN_ENDPOINT = "invokeTokenEndpoint";
    public static final String INVOKE_TOKEN_ENDPOINT_CL_CRED = "invokeTokenEndpointClCred";
    public static final String INVOKE_TOKEN_ENDPOINT_PASSWORD = "invokeTokenEndpointPassword";
    //    public static final String INVOKE_TOKEN_ENDPOINT_APP_PASSWORD = "invokeTokenEndpointAppPassword";
    public static final String INVOKE_APP_PASSWORDS_ENDPOINT_CREATE = "invokeAppPasswordCreateEndpoint";
    public static final String INVOKE_APP_PASSWORDS_ENDPOINT_LIST = "invokeAppPasswordListEndpoint";
    public static final String INVOKE_APP_PASSWORDS_ENDPOINT_REVOKE = "invokeAppPasswordDeleteEndpoint";
    public static final String INVOKE_APP_TOKENS_ENDPOINT_CREATE = "invokeAppTokenCreateEndpoint";
    public static final String INVOKE_APP_TOKENS_ENDPOINT_LIST = "invokeAppTokenListEndpoint";
    public static final String INVOKE_APP_TOKENS_ENDPOINT_REVOKE = "invokeAppTokenDeleteEndpoint";
    public static final String PERFORM_IDP_LOGIN = "performIDPLogin";
    public static final String PERFORM_IDP_LOGIN_JAVASCRIPT_DISABLED = "performIDPLoginWithJavascriptDisabled";
    public static final String INVOKE_ACS = "invokeACS";
    public static final String INVOKE_JWK_ENDPOINT = "invokeJwkEndpoint";
    public static final String BUILD_POST_SP_INITIATED_REQUEST = "buildPostSPInitiatedRequest";

    // All OP actions/tasks should be included in this list!
    public static final String[] OP_TEST_ACTIONS = { INVOKE_OAUTH_CLIENT, SUBMIT_TO_AUTH_SERVER, SUBMIT_TO_AUTH_SERVER_FOR_TOKEN, INVOKE_AUTH_SERVER, PERFORM_LOGIN,
            PERFORM_ISAM_LOGIN, INVOKE_PROTECTED_RESOURCE, INVOKE_ENDPOINT, INVOKE_INTROSPECTION_ENDPOINT, INVOKE_REVOCATION_ENDPOINT,
            INVOKE_DISCOVERY_ENDPOINT, INVOKE_JWT_ENDPOINT, INVOKE_AUTH_ENDPOINT, INVOKE_AUTH_ENDPOINT_WITH_BASIC_AUTH,
            INVOKE_TOKEN_ENDPOINT, LOGOUT, INVOKE_TOKEN_ENDPOINT_CL_CRED, INVOKE_TOKEN_ENDPOINT_PASSWORD, INVOKE_REFRESH_ENDPOINT,
            PERFORM_IDP_LOGIN, PERFORM_IDP_LOGIN, INVOKE_ACS, INVOKE_RS_PROTECTED_RESOURCE };

    public static final String[] BASIC_AUTHENTICATION_ACTIONS = { INVOKE_OAUTH_CLIENT, SUBMIT_TO_AUTH_SERVER, PERFORM_LOGIN };
    // TODO For now, the actions are the same since we're using the id_token,
    // use the normal OP
    public static final String[] BASIC_CREATE_JWT_TOKEN_ACTIONS = { INVOKE_OAUTH_CLIENT, SUBMIT_TO_AUTH_SERVER, PERFORM_ISAM_LOGIN };
    public static final String[] BASIC_PROTECTED_RESOURCE_ACTIONS = { INVOKE_OAUTH_CLIENT, SUBMIT_TO_AUTH_SERVER, PERFORM_LOGIN, INVOKE_PROTECTED_RESOURCE };
    public static final String[] BASIC_PROTECTED_RESOURCE_WITH_JWT_ACTIONS = { INVOKE_OAUTH_CLIENT, SUBMIT_TO_AUTH_SERVER, PERFORM_ISAM_LOGIN, INVOKE_PROTECTED_RESOURCE };
    public static final String[] AUTH_ENDPOINT_NOJSP_ACTIONS = { INVOKE_AUTH_ENDPOINT };
    public static final String[] AUTH_ENDPOINT_NOJSP_ACTIONS_WITH_BASIC_AUTH = { INVOKE_AUTH_ENDPOINT_WITH_BASIC_AUTH };
    public static final String[] BASIC_PROTECTED_RESOURCE_ACTIONS_WITH_CLIENT_CRED = { INVOKE_OAUTH_CLIENT, SUBMIT_TO_AUTH_SERVER_FOR_TOKEN, INVOKE_PROTECTED_RESOURCE };
    public static final String[] BASIC_AUTHENTICATION_NOJSP_ACTIONS = { INVOKE_AUTH_ENDPOINT, PERFORM_LOGIN };
    public static final String[] BASIC_AUTHENTICATION_NOJSP_ACTIONS_WITH_BASIC_AUTH = { INVOKE_AUTH_ENDPOINT_WITH_BASIC_AUTH };
    public static final String[] BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS = { INVOKE_AUTH_ENDPOINT, PERFORM_LOGIN, INVOKE_TOKEN_ENDPOINT, INVOKE_PROTECTED_RESOURCE };
    public static final String[] BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS2 = { INVOKE_AUTH_ENDPOINT, PERFORM_IDP_LOGIN, INVOKE_ACS, INVOKE_TOKEN_ENDPOINT,
            INVOKE_PROTECTED_RESOURCE };
    public static final String[] BASIC_TOKEN_NOJSP_ACTIONS = { INVOKE_AUTH_ENDPOINT, PERFORM_LOGIN, INVOKE_TOKEN_ENDPOINT };
    public static final String[] BASIC_TOKEN_NOJSP_ACTIONS_WITH_BASIC_AUTH = { INVOKE_AUTH_ENDPOINT_WITH_BASIC_AUTH, INVOKE_TOKEN_ENDPOINT };
    public static final String[] BASIC_TOKEN_NOJSP_ONLY_ACTIONS = { INVOKE_TOKEN_ENDPOINT };
    public static final String[] CLIENT_CREDENTIAL_NOJSP_ACTIONS = { INVOKE_TOKEN_ENDPOINT_CL_CRED };
    public static final String[] CLIENT_CREDENTIAL_PROTECTED_RESOURCE_NOJSP_ACTIONS = { INVOKE_TOKEN_ENDPOINT_CL_CRED, INVOKE_PROTECTED_RESOURCE };
    public static final String[] PASSWORD_NOJSP_ACTIONS = { INVOKE_TOKEN_ENDPOINT_PASSWORD };
    public static final String[] PASSWORD_PROTECTED_RESOURCE_NOJSP_ACTIONS = { INVOKE_TOKEN_ENDPOINT_PASSWORD, INVOKE_PROTECTED_RESOURCE };
    public static final String[] APP_PASSWORD_NOJSP_ACTIONS = { INVOKE_TOKEN_ENDPOINT };
    public static final String[] APP_PASSWORD_PROTECTED_RESOURCE_NOJSP_ACTIONS = { INVOKE_TOKEN_ENDPOINT, INVOKE_PROTECTED_RESOURCE };
    public static final String[] BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS_WITH_BASIC_AUTH = { INVOKE_AUTH_ENDPOINT_WITH_BASIC_AUTH, INVOKE_TOKEN_ENDPOINT,
            INVOKE_PROTECTED_RESOURCE };
    public static final String[] BASIC_PROTECTED_RESOURCE_NOJSP_AGAIN_ACTIONS = { INVOKE_AUTH_ENDPOINT, INVOKE_TOKEN_ENDPOINT, INVOKE_PROTECTED_RESOURCE };
    public static final String[] BASIC_AUTHENTICATE_ACTIONS_WITH_BASIC_AUTH = { INVOKE_OAUTH_CLIENT, SUBMIT_TO_AUTH_SERVER_WITH_BASIC_AUTH, };
    public static final String[] BASIC_PROTECTED_RESOURCE_ACTIONS_WITH_BASIC_AUTH = { INVOKE_OAUTH_CLIENT, SUBMIT_TO_AUTH_SERVER_WITH_BASIC_AUTH, INVOKE_PROTECTED_RESOURCE };
    public static final String[] ONLY_PROTECTED_RESOURCE_ACTIONS = { INVOKE_PROTECTED_RESOURCE };
    public static final String[] IMPLICIT_AUTHENTICATION_ACTIONS = { INVOKE_AUTH_SERVER, PERFORM_LOGIN };
    public static final String[] IMPLICIT_AUTHENTICATION_ACTION_WITH_ERR = { INVOKE_AUTH_SERVER };
    public static final String[] IMPLICIT_AUTHENTICATION_ACTIONS_WITH_BASIC_AUTH = { INVOKE_AUTH_SERVER_WITH_BASIC_AUTH };
    public static final String[] TOKEN_ACTIONS = { INVOKE_OAUTH_CLIENT, SUBMIT_TO_AUTH_SERVER_FOR_TOKEN };
    public static final String[] SUBMIT_ACTIONS = { INVOKE_OAUTH_CLIENT, SUBMIT_TO_AUTH_SERVER };
    // needs work public static final String[]
    // IMPLICIT_PROTECTED_RESOURCE_ACTIONS = { INVOKE_AUTH_SERVER,
    // PERFORM_LOGIN, INVOKE_PROTECTED_RESOURCE } ;
    public static final String[] GOOD_ENDPOINT_ACTIONS = { INVOKE_OAUTH_CLIENT, SUBMIT_TO_AUTH_SERVER, PERFORM_LOGIN, INVOKE_ENDPOINT };
    public static final String[] INTROSPECTION_ENDPOINT_ACTIONS = { INVOKE_INTROSPECTION_ENDPOINT };
    public static final String[] REVOCATION_ENDPOINT_ACTIONS = { INVOKE_REVOCATION_ENDPOINT, INVOKE_INTROSPECTION_ENDPOINT };
    public static final String[] DISCOVERY_ENDPOINT_ACTIONS = { INVOKE_DISCOVERY_ENDPOINT };
    public static final String[] COVERAGE_MAP_ENDPOINT_ACTIONS = { INVOKE_COVERAGEMAP_ENDPOINT };
    public static final String[] REGISTRATION_ENDPOINT_ACTIONS = { INVOKE_REGISTRATION_ENDPOINT };
    public static final String[] LOGOUT_ONLY_ACTIONS = { LOGOUT };
    public static final String[] INVOKE_ACTIONS = { INVOKE_OAUTH_CLIENT };
    //    public static final String[] BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS = { INVOKE_AUTH_ENDPOINT, PERFORM_LOGIN, INVOKE_TOKEN_ENDPOINT, INVOKE_PROTECTED_RESOURCE };
    //    public static final String[] OP_WITH_SAML_PROTECTED_RESOURCE_ACTIONS = { INVOKE_AUTH_ENDPOINT };
    // use this    public static final String[] OP_WITH_SAML_PROTECTED_RESOURCE_ACTIONS = { INVOKE_AUTH_ENDPOINT, PERFORM_IDP_LOGIN, INVOKE_ACS, INVOKE_TOKEN_ENDPOINT, INVOKE_PROTECTED_RESOURCE };
    //    public static final String[] OP_WITH_SAML_PROTECTED_RESOURCE_ACTIONS = { INVOKE_AUTH_ENDPOINT, PERFORM_IDP_LOGIN, INVOKE_TOKEN_ENDPOINT, INVOKE_PROTECTED_RESOURCE };
    public static final String[] OP_WITH_SAML_PROTECTED_RESOURCE_ACTIONS = { INVOKE_AUTH_ENDPOINT, PERFORM_IDP_LOGIN, INVOKE_PROTECTED_RESOURCE };
    //    public static final String[] OP_WITH_SAML_PROTECTED_RESOURCE_ACTIONS = { INVOKE_OAUTH_CLIENT, SUBMIT_TO_AUTH_SERVER, PERFORM_IDP_LOGIN, INVOKE_ACS, INVOKE_PROTECTED_RESOURCE };
    public static final String[] OP_WITH_SAML_THROUGH_ACS = { INVOKE_OAUTH_CLIENT, SUBMIT_TO_AUTH_SERVER, PERFORM_IDP_LOGIN, INVOKE_ACS };
    public static final String[] IMPLICIT_OP_WITH_SAML_THROUGH_ACS = { INVOKE_AUTH_SERVER, PERFORM_IDP_LOGIN, INVOKE_ACS };
    public static final String[] SOLICITED_SP_INITIATED_FLOW = { BUILD_POST_SP_INITIATED_REQUEST, PERFORM_IDP_LOGIN };
    public static final String[] BASIC_RS_PROTECTED_RESOURCE_ACTIONS = { INVOKE_OAUTH_CLIENT, SUBMIT_TO_AUTH_SERVER, PERFORM_LOGIN, INVOKE_RS_PROTECTED_RESOURCE };
    public static final String[] BASIC_JWT_RS_PROTECTED_RESOURCE_ACTIONS = { INVOKE_OAUTH_CLIENT, SUBMIT_TO_AUTH_SERVER, PERFORM_ISAM_LOGIN, INVOKE_RS_PROTECTED_RESOURCE };
    public static final String[] BASIC_PROTECTED_RESOURCE_RS_PROTECTED_RESOURCE_ACTIONS = { INVOKE_OAUTH_CLIENT, SUBMIT_TO_AUTH_SERVER, PERFORM_LOGIN, INVOKE_PROTECTED_RESOURCE,
            INVOKE_RS_PROTECTED_RESOURCE };
    public static final String[] BASIC_PROTECTED_RESOURCE_JWT_RS_PROTECTED_RESOURCE_ACTIONS = { INVOKE_OAUTH_CLIENT, SUBMIT_TO_AUTH_SERVER, PERFORM_ISAM_LOGIN,
            INVOKE_PROTECTED_RESOURCE, INVOKE_RS_PROTECTED_RESOURCE };
    public static final String[] INVOKE_RS_PROTECTED_RESOURCE_ONLY_ACTIONS = { INVOKE_RS_PROTECTED_RESOURCE };
    public static final String[] INVOKE_RS_PROTECTED_RESOURCE_LOGIN_ACTIONS = { INVOKE_RS_PROTECTED_RESOURCE, PERFORM_LOGIN };

    /* ******************** steps in the RP test process ***************** */
    public static final String SPECIFY_PROVIDER = "specifyProvider";
    public static final String GET_LOGIN_PAGE = "getLoginPage";
    public static final String POST_LOGIN_PAGE = "postLoginPage";
    public static final String RP_LOGIN_PAGE = "rpLoginPage";
    public static final String LOGIN_USER = "loginUser";
    public static final String GET_RP_CONSENT = "getRPConsent";
    public static final String LOGIN_AGAIN = "loginAgain";
    public static final String LOGIN_OP_DIRECTLY = "loginopdirectly";
    public static final String PROCESS_OAUTH = "processOauth";

    // All RP actions/tasks should be included in this list!
    public static final String[] RP_TEST_ACTIONS = { GET_LOGIN_PAGE, SPECIFY_PROVIDER, LOGIN_OP_DIRECTLY, LOGIN_USER, GET_RP_CONSENT, LOGIN_AGAIN, LOGOUT, PERFORM_IDP_LOGIN,
            INVOKE_ACS };

    // public static final List <String> SPECIFY_PROVIDER_ONLY = new
    // List<String>(GET_LOGIN_PAGE, SPECIFY_PROVIDER ) ;
    public static final String[] GET_LOGIN_PAGE_ONLY = { GET_LOGIN_PAGE };
    public static final String[] POST_LOGIN_PAGE_ONLY = { POST_LOGIN_PAGE };
    public static final String[] SPECIFY_PROVIDER_ONLY = { GET_LOGIN_PAGE, SPECIFY_PROVIDER };
    public static final String[] LOGIN_FIRST_ACTIONS = { LOGIN_OP_DIRECTLY, LOGIN_USER, LOGIN_AGAIN };
    public static final String[] GOOD_LOGIN_ACTIONS = { GET_LOGIN_PAGE, SPECIFY_PROVIDER, LOGIN_USER };
    public static final String[] GOOD_LOGIN_AGAIN_ACTIONS = { GET_LOGIN_PAGE, SPECIFY_PROVIDER, LOGIN_USER, LOGIN_AGAIN };
    public static final String[] GOOD_LOGIN_WITH_CONSENT_ACTIONS = { GET_LOGIN_PAGE, SPECIFY_PROVIDER, LOGIN_USER, GET_RP_CONSENT };
    public static final String[] GOOD_LOGIN_WITH_CONSENT_AGAIN_ACTIONS = { GET_LOGIN_PAGE, SPECIFY_PROVIDER, LOGIN_USER, GET_RP_CONSENT, LOGIN_AGAIN };
    public static final String[] STANDARD_FORM_LOGIN_ACTIONS = { GET_LOGIN_PAGE, LOGIN_USER };
    public static final String[] GOOD_OAUTH_LOGIN_ACTIONS = { GET_LOGIN_PAGE, PROCESS_OAUTH, SPECIFY_PROVIDER, LOGIN_USER };
    public static final String[] GOOD_OAUTH_LOGIN_AGAIN_ACTIONS = { GET_LOGIN_PAGE, PROCESS_OAUTH, SPECIFY_PROVIDER, LOGIN_USER, LOGIN_AGAIN };
    public static final String[] GOOD_OAUTH_LOGIN_WITH_CONSENT_ACTIONS = { GET_LOGIN_PAGE, PROCESS_OAUTH, SPECIFY_PROVIDER, LOGIN_USER, GET_RP_CONSENT };
    public static final String[] GOOD_OAUTH_LOGIN_WITH_CONSENT_AGAIN_ACTIONS = { GET_LOGIN_PAGE, PROCESS_OAUTH, SPECIFY_PROVIDER, LOGIN_USER, GET_RP_CONSENT, LOGIN_AGAIN };
    public static final String[] GOOD_OIDC_LOGIN_ACTIONS_CONSENT = { GET_LOGIN_PAGE, LOGIN_USER, GET_RP_CONSENT };
    public static final String[] GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT = { GET_LOGIN_PAGE, LOGIN_USER };
    public static final String[] GOOD_OIDC_POST_LOGIN_ACTIONS_CONSENT = { POST_LOGIN_PAGE, LOGIN_USER, GET_RP_CONSENT };
    public static final String[] GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT = { POST_LOGIN_PAGE, LOGIN_USER };
    public static final String[] GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT_WITH_SAML = { POST_LOGIN_PAGE, PERFORM_IDP_LOGIN }; // with javascript enabled, ACS is automatically invoked.
    public static final String[] GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT_WITH_SAML_SAMESITE = { POST_LOGIN_PAGE, PERFORM_IDP_LOGIN_JAVASCRIPT_DISABLED }; // javascript disabled keeps negative cases from hitting an OOM in htmlunit
    public static final String[] GOOD_OIDC_LOGIN_AGAIN_ACTIONS = { GET_LOGIN_PAGE, LOGIN_USER, LOGIN_AGAIN };
    public static final String[] GOOD_OIDC_LOGIN_AGAIN_ACTIONS_CONSENT = { GET_LOGIN_PAGE, LOGIN_USER, GET_RP_CONSENT, LOGIN_AGAIN, GET_RP_CONSENT };
    public static final String[] GOOD_OIDC_LOGIN_LOGOUT_ACTIONS = { GET_LOGIN_PAGE, LOGIN_USER, LOGOUT };
    public static final String[] GOOD_OIDC_LOGOUT_ONLY_ACTIONS = { LOGOUT };

    /* ********************* End Steps in the process *********************** */

    /* ************** steps in the JWT Builder test process **************** */
    public static final String INVOKE_JWT_BUILDER = "invoke_builder";
    public static final String INVOKE_JWT_BUILDER_AGAIN = "invoke_builder_again";
    public static final String INVOKE_JWT_CONSUMER = "invoke_consumer";
    public static final String INVOKE_JWT_CONSUMER_AGAIN = "invoke_consumer_again";
    public static final String INVOKE_JWK_ENDPOINT_VALIDATION = "invoke_jwk_endpoint_validation";

    public static final String[] BUILDER_TEST_ACTIONS = { INVOKE_JWT_BUILDER, INVOKE_JWT_BUILDER_AGAIN, INVOKE_RS_PROTECTED_RESOURCE, INVOKE_JWK_ENDPOINT_VALIDATION };
    public static final String[] CONSUMER_TEST_ACTIONS = { INVOKE_JWT_CONSUMER, INVOKE_JWT_CONSUMER_AGAIN, INVOKE_RS_PROTECTED_RESOURCE };

    //    public static final String[] GOOD_BUILDER_FLOW = { INVOKE_JWT_BUILDER, INVOKE_RS_PROTECTED_RESOURCE };
    public static final String[] GOOD_BUILDER_FLOW = { INVOKE_JWT_BUILDER };
    public static final String[] GOOD_BUILDER_USE_TOKEN_FLOW = { INVOKE_JWT_BUILDER, INVOKE_RS_PROTECTED_RESOURCE };
    public static final String[] GOOD_BUILDER_AGAIN_FLOW = { INVOKE_JWT_BUILDER, INVOKE_JWT_BUILDER_AGAIN };
    public static final String[] GOOD_CONSUMER_FLOW = { INVOKE_JWT_CONSUMER };
    public static final String[] GOOD_CONSUMER_USE_TOKEN_FLOW = { INVOKE_JWT_CONSUMER, INVOKE_RS_PROTECTED_RESOURCE };
    public static final String[] GOOD_CONSUMER_AGAIN_FLOW = { INVOKE_JWT_CONSUMER, INVOKE_JWT_CONSUMER_AGAIN };
    /* ********************* End Steps in the process *********************** */

    /* ****************** validation data location **************** */
    //    public static final String OP_CONSOLE_LOG = "op_console.log";
    //    public static final String OP_MESSAGES_LOG = "op_messages.log";
    //    public static final String OP_TRACE_LOG = "op_trace.log";
    //    public static final String RP_CONSOLE_LOG = "rp_console.log";
    //    public static final String RP_MESSAGES_LOG = "rp_messages.log";
    //    public static final String RP_TRACE_LOG = "rp_trace.log";
    //    public static final String GENERIC_CONSOLE_LOG = "generic_console.log";
    //    public static final String GENERIC_MESSAGES_LOG = "generic_messages.log";
    //    public static final String GENERIC_TRACE_LOG = "generic_trace.log";
    public static final String RESPONSE_TOKEN = "responseToken";
    public static final String RESPONSE_TOKEN_LENGTH = "response_token_length";
    public static final String RESPONSE_KEY_SIZE = "response_key_size";
    public static final String RESPONSE_ID_TOKEN = "IdToken";
    public static final String RESPONSE_JWT_TOKEN = "JWTToken";
    public static final String JSON_OBJECT = "jsonObject";
    public static final String JSON_OBJECT_COUNT = "jsonCount";
    public static final String RESPONSE_GENERAL = "generalResponse";
    public static final String EXCEPTION_MESSAGE = "exceptionMessage";
    public static final String OAUTH_APP_START_MSG = "OAuth provider ";
    public static final String OIDC_APP_START_MSG = "OAuth provider ";

    /* ***************** ******************* */
    public static final String OAUTH_LOGIN_TITLE = "OAuth 2.0 Authorization Code Grant";
    public static final String NO_REFRESH_TOKEN_MSG = "refresh_token was not found in the token cache.";
    public static final String POSTLOGOUTPAGE = "Redirect To Identity Provider";
    public static final String SAML_TFIM_LOGIN_HEADER = "ITFIM Form Login";
    public static final String SAML_SHIBBOLETH_LOGIN_HEADER = "Web Login Service";
    public static final String SAML_RESPONSE = "SAMLResponse";
    public static final String SAML_REQUEST = "SAMLRequest";
    public static final String CORRECTLY_CAUGHT = "Correctly Caught: ";
    /* ***************** ******************* */
    public static final String ENDPOINT_TYPE = "endpoint";
    public static final String DECLARATIVE_TYPE = "declarativeEndpoint";
    public static final String PROVIDERS_TYPE = "providers";

    public static final String OAUTH_ROOT = "oauth2";
    public static final String OAUTH_TAI_ROOT = "oauth2tai";

    public static final String OIDC_ROOT = "oidc";
    // public static final String OIDC_ROOT = "oauth2" ;
    public static final String OIDC_TAI_ROOT = "oauth2tai";

    public static final String OAUTH2_DEFAULT_CONTEXT_ROOT = "/oauth2/";
    public static final String OIDC_DEFAULT_CONTEXT_ROOT = "/oidc/";
    public static final String OIDC_CLIENT_DEFAULT_CONTEXT_ROOT = "/oidcclient/";
    public static final String JWT_DEFAULT_CONTEXT_ROOT = "/jwt/";

    public static final String AUTHORIZE_ENDPOINT = "authorize";
    public static final String TOKEN_ENDPOINT = "token";
    public static final String INTROSPECTION_ENDPOINT = "introspect";
    public static final String USERINFO_ENDPOINT = "userinfo";
    public static final String JWK_ENDPOINT = "jwk";
    public static final String APP_PASSWORD_ENDPOINT = "app-passwords";
    public static final String APP_TOKEN_ENDPOINT = "app-tokens";
    public static final String END_SESSION = "end_session";
    public static final String END_SESSION_ENDPOINT = "end_session";
    public static final String POST_LOGIN_REDIRECT = "simpleIdP";
    public static final String REVOCATION_ENDPOINT = "revoke";
    public static final String DISCOVERY_ENDPOINT = ".well-known/openid-configuration";
    public static final String COVERAGE_MAP_ENDPOINT = "coverage_map";
    public static final String REGISTRATION_ENDPOINT = "registration";
    public static final String LOGOUT_ENDPOINT = "logout";
    public static final String CLIENTMGMT_ENDPOINT = "clientManagement";
    public static final String PERSONALTOKENMGMT_ENDPOINT = "personalTokenManagement";
    public static final String USERSTOKENMGMT_ENDPOINT = "usersTokenManagement";
    public static final String CLIENTMETATYPE_ENDPOINT = "clientMetatype";
    public static final String CHECKSESSIONIFRAME_ENDPOINT = "check_session_iframe";

    public static final String JSON_USERINFO_DATA = "json_userinfo_data";
    public static final String JWS_USERINFO_DATA = "jws_userinfo_data";
    public static final String JWE_USERINFO_DATA = "jwe_userinfo_data";

    public static final String LOCAL_VALIDATION_METHOD = "";

    public static final String SSODEMO = "ssodemo";

    /* ******************** OP App Info ************************ */
    public static final String CLIENT_JSP = "client.jsp";
    public static final String REFRESH_JSP = "refresh.jsp";
    public static final String REDIRECT_JSP = "redirect.jsp";
    public static final String JWT_JSP = "jwt.jsp";

    public static final String OP_CLIENT_APP = "clientApp";
    public static final String OP_TAI_APP = "taiApp";
    public static final String OP_NOFILTER_APP = "noFilterApp";
    public static final String OP_DERBY_APP = "derbyApp";
    public static final String OP_SAMPLE_APP = "sampleApp";
    public static final String OP_MEDIATOR_APP = "mediatorApp";
    public static final String OP_PUBLIC_APP = "publicApp";

    public static final String OAUTHCLIENT_APP = "oauthclient";
    public static final String OAUTHCONFIGTAI_APP = "OAuthConfigTai";
    public static final String OAUTHCONFIGNOFILTER_APP = "OAuthConfigNoFilter";
    public static final String OAUTHCONFIGDERBY_APP = "OAuthConfigDerby";
    public static final String OAUTHCONFIGMONGO_APP = "OAuthConfigMongo";
    public static final String OAUTHCONFIGSAMPLE_APP = "OAuthConfigSample";
    public static final String OAUTHCONFIGMEDIATOR_APP = "OAuthMediatorProvider";
    public static final String OAUTHCONFIGPUBLIC_APP = "OAuthConfigPublic";

    public static final String OAUTHCLIENT_START_APP = "oauthclient";
    public static final String OAUTHCONFIGTAI_START_APP = "oauthtaidemo";
    public static final String OAUTHCONFIGDERBY_START_APP = "oAuth20DerbySetup";
    public static final String OAUTHCONFIGMONGO_START_APP = "oAuth20MongoSetup";
    public static final String OAUTHCONFIGNOFILTER_START_APP = "OAuthConfigNoFilter";
    public static final String OAUTHCONFIGSAMPLE_START_APP = "OAuthConfigSample";
    public static final String OAUTHCONFIGMEDIATOR_START_APP = "OAuthMediatorProvider";
    public static final String OAUTHCONFIGPUBLIC_START_APP = "OAuthConfigPublic";

    public static final String OIDCCLIENT_APP = "oauthclient";
    public static final String OIDCCONFIGTAI_APP = "OidcConfigTai";
    public static final String OIDCCONFIGNOFILTER_APP = "OidcConfigNoFilter";
    public static final String OIDCCONFIGDERBY_APP = "OidcConfigDerby";
    public static final String OIDCCONFIGSAMPLE_APP = "OidcConfigSample";
    public static final String OIDCCONFIGMEDIATOR_APP = "OidcMediatorProvider";
    public static final String OIDCCONFIGPUBLIC_APP = "OidcConfigPublic";

    public static final String OIDCCLIENT_START_APP = "oauthclient";
    public static final String OIDCCONFIGTAI_START_APP = "oauthtaidemo";
    public static final String OIDCCONFIGDERBY_START_APP = "oAuth20DerbySetup";
    public static final String OIDCCONFIGMONGODB_START_APP = "oAuth20MongoSetup";
    public static final String OIDCCONFIGNOFILTER_START_APP = "OidcConfigNoFilter";
    public static final String OIDCCONFIGSAMPLE_START_APP = "OidcConfigSample";
    public static final String OIDCCONFIGMEDIATOR_START_APP = "OidcMediatorProvider";
    public static final String OIDCCONFIGPUBLIC_START_APP = "OidcConfigPublic";

    /* ******************** RP App Info ************************ */
    public static final String APP_GLOBALFORMLOGIN = "globalformlogin";
    public static final String APP_MIXEDFORMLOGIN = "mixedformlogin";
    public static final String APP_E2E_RP = "formlogine2e";
    public static final String APP_E2E_WEB = "formlogine2e2";
    public static final String E2E_RP_SERVLET = "FormLoginServletE2E1";
    public static final String E2E_WEB_SERVLET = "FormLoginServletE2E2";
    public static final String APP_AUTHZ_PARM = "authzParameter";
    public static final String HELLOWORLD_SERVLET = "helloworld";
    public static final String JWT_BUILDER_SERVLET = "jwtbuilderclient";
    public static final String HELLOWORLD_MSG = "Accessed Hello World!";
    public static final String HELLOWORLD_WITH_HEADER = "Accessed Hello World! Access Token in the header";
    public static final String HELLOWORLD_WITH_PARM = "Accessed Hello World! Access Token as Parm";
    public static final String HELLOWORLD_NO_HEADER_NO_PARM = "Accessed Hello World! Access Token is notSet";
    public static final String HELLOWORLD_PROTECTED_RESOURCE = "/helloworld/rest/helloworld";

    public static final String CHECK_URL = "getRequestURL: ";
    /* OAUTH */
    /* ******************************************************************************************************************* */
    public static final String DERBY_STORE_SERVER = "com.ibm.ws.security.oauth-2.0.derby.fat";
    public static final String LOCAL_STORE_SERVER = "com.ibm.ws.security.oauth-2.0.fat";

    public static final String RECV_AUTH_CODE = "Received authorization code:";
    public static final String RECV_ERROR_CODE = "Error 500: javax.servlet.ServletException: SRVE0207E: Uncaught initialization exception created by servlet";
    // public static final String recvAccessToken =
    // "Received from token endpoint: {\"access_token\":";
    public static final String APP_TITLE = "Snoop Servlet";
    public static final String IMPLICIT_APP_TITLE = "OAuth 2.0 Implicit Flow";
    public static final String APPROVAL_FORM = "javascript";
    public static final String APPROVAL_HEADER = "OAuth authorization form";
    public static final String CUSTOM_APPROVAL_HEADER = "OAuth Custom Consent Form";
    // public static final String autoauthz = "true";
    public static final String REDIRECT_ACCESS_TOKEN = "access_token=";
    public static final String CUSTOM_LOGIN_TITLE = "Custom OAuth Login";
    public static final String CUSTOM_ERROR_TITLE = "Custom Error Page";

    public static final String RECV_FROM_TOKEN_ENDPOINT = "Received from token endpoint: {";
    public static final String REFRESH_TOKEN_UPDATED = "Updated \"Refresh Token\" input field with:";
    public static final String INVALID_TOKEN_ERROR = "error=\"invalid_token\"";
    public static final String CHECK_ACCES_TOKEN = "Check access token";
    public static final String CLIENT_COULD_NOT_BE_FOUND = "The OAuth service provider could not find the client because the client name is not valid. Contact your system administrator to resolve the problem.";
    public static final String AUTHORIZATION_FAILED = "Error 403: AuthenticationFailed";
    public static final String AFTER_REVOKE = "OAuth service failed the request";
    public static final String HTTPS_REQUIRED = "HTTPS is required";
    public static final String NOT_FOUND = "not found";
    public static final String DEFAULT_NONCE = "9874532113";
    public static final String UNAUTHORIZED_REQUEST = "UnauthorizedSessionRequestException: SESN0008E";
    public static final String SERVER_CONFIG_ERROR = "oauth20.grant.types.allowed";
    public static final String UNAUTHORIZED_MESSAGE = "Unauthorized";
    public static final String UNAUTHORIZED_EXCEPTION = "java.io.IOException: Server returned HTTP response code: 401";
    public static final String BASIC_REALM = "BasicRealm";
    public static final String HELLOWORLD_UNAUTHENTICATED = "WSPrincipal:UNAUTHENTICATED";
    public static final String OP_TESTMARKER_APP = "testmarker.war";

    /* Error response codes */
    public static final String ERROR_RESPONSE_PARM = "error";
    public static final String ERROR_CODE_INVALID_REQUEST = "invalid_request";
    public static final String ERROR_CODE_INVALID_CLIENT = "invalid_client";
    public static final String ERROR_CODE_INVALID_GRANT = "invalid_grant";
    public static final String ERROR_CODE_UNAUTHORIZED_CLIENT = "unauthorized_client";
    public static final String ERROR_CODE_UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";
    public static final String ERROR_CODE_INVALID_SCOPE = "invalid_scope";
    public static final String ERROR_RESPONSE_DESCRIPTION = "error_description";
    public static final String ERROR_CODE_INVALID_TOKEN = "invalid_token";
    public static final String ERROR_SERVER_CONFIG = "configuration_error";
    public static final String ERROR_CODE_LOGIN_REQUIRED = "login_required";
    public static final String ERROR_SERVER_ERROR = "server_error";
    public static final String ERROR_RESOURCE_OWNER_BAD_CREDS = "invalid_resource_owner_credential";
    public static final String ERROR_RESPONSE_URI = "error_uri";
    public static final String ERROR_CODE_UNSUPPORTED_TOKEN_TYPE = "unsupported_token_type";

    /* provider types */
    public static final String TFIM_TYPE = "tfim";
    public static final String GOOGLE_TYPE = "google";
    public static final String YAHOO_TYPE = "yahoo";
    public static final String MYOPENID_TYPE = "myopenid";
    public static final String BLUEID_TYPE = "blueid";
    public static final String FREEXRI_TYPE = "freexri";
    public static final String IBMOIDC_TYPE = "ibmoidc";

    /* provider values */
    public static final String BLUEID_PROVIDER = "https://xxx.com/FIM/blueid";
    public static final String BLUEID_USERPARM = "username";
    public static final String BLUEID_PASSPARM = "password";
    public static final String BLUEID_USERNAME = "xxx@xxx.com";
    public static final String BLUEID_USERPASSWORD = "xxxxxxxx";
    public static final String BLUEID_FULLUSERNAME = "xxx@xxx.com";
    public static final String BLUEID_CLIENTIDENTITY = "clientIdentity";
    public static final String BLUEID_LOGINBUTTON = null;
    public static final String BLUEID_CONFIRMBUTTON = null;
    public static final String BLUEID_CONFIRMBUTTONVALUE = null;
    public static final Long BLUEID_CONFIRMSLEEP = null;
    public static final String BLUEID_LOGINTITLE = "somebluething";

    public static final String FREEXRI_PROVIDER = "xri://=xxxx";
    public static final String FREEXRI_USERPARM = null;
    public static final String FREEXRI_PASSPARM = "pass";
    public static final String FREEXRI_USERNAME = "=xxx";
    public static final String FREEXRI_USERPASSWORD = "xxx";
    public static final String FREEXRI_FULLUSERNAME = "=xxxx";
    public static final String FREEXRI_CLIENTIDENTITY = "=xxxx";
    public static final String FREEXRI_LOGINBUTTON = "authenticate";
    public static final String FREEXRI_CONFIRMBUTTON = null;
    public static final String FREEXRI_CONFIRMBUTTONVALUE = null;
    public static final Long FREEXRI_CONFIRMSLEEP = null;
    public static final String FREEXRI_LOGINTITLE = "@freeXRI";

    public static final String GOOGLE_PROVIDER = "https://www.google.com/accounts/o8/id";
    public static final String GOOGLE_USERPARM = "Email";
    public static final String GOOGLE_PASSPARM = "Passwd";
    public static final String GOOGLE_USERNAME = "xxx@yahoo.com";
    public static final String GOOGLE_USERPASSWORD = "xxx";
    public static final String GOOGLE_FULLUSERNAME = "xxx@yahoo.com";
    public static final String GOOGLE_CLIENTIDENTITY = "clientIdentity";
    public static final String GOOGLE_LOGINBUTTON = "signIn";
    public static final String GOOGLE_CONFIRMBUTTON = null;
    public static final String GOOGLE_CONFIRMBUTTONVALUE = null;
    public static final Long GOOGLE_CONFIRMSLEEP = null;
    public static final String GOOGLE_LOGINTITLE = "Google Accounts";

    public static final String MYOPENID_PROVIDER = "http://xxx.myopenid.com/";
    public static final String MYOPENID_USERPARM = null;
    public static final String MYOPENID_PASSPARM = "password";
    public static final String MYOPENID_USERNAME = "xxx";
    public static final String MYOPENID_USERPASSWORD = "xxx";
    public static final String MYOPENID_FULLUSERNAME = "xxx";
    public static final String MYOPENID_CLIENTIDENTITY = "clientIdentity";
    public static final String MYOPENID_LOGINBUTTON = "signin_button";
    public static final String MYOPENID_CONFIRMBUTTON = null;
    public static final String MYOPENID_CONFIRMBUTTONVALUE = null;
    public static final Long MYOPENID_CONFIRMSLEEP = null;
    public static final String MYOPENID_LOGINTITLE = "Sign In";

    public static final String TFIM_PROVIDER = "https://xxx.com:9443/op";
    public static final String TFIM_USERPARM = "j_username";
    public static final String TFIM_PASSPARM = "j_password";
    public static final String TFIM_USERNAME = "xxx";
    public static final String TFIM_USERPASSWORD = "xxx";
    public static final String TFIM_FULLUSERNAME = "xxx@xxx.com";
    public static final String TFIM_CLIENTIDENTITY = "https://xxx.com:9443/op/xxx";
    public static final String TFIM_LOGINBUTTON = null;
    public static final String TFIM_CONFIRMBUTTON = "consent";
    public static final String TFIM_CONFIRMBUTTONVALUE = "permit_once";
    public static final Long TFIM_CONFIRMSLEEP = null;
    public static final String TFIM_LOGINTITLE = "ITFIM Form Login";

    public static final String YAHOO_PROVIDER = "https://me.yahoo.com";
    public static final String YAHOO_USERPARM = "login";
    public static final String YAHOO_PASSPARM = "passwd";
    public static final String YAHOO_USERNAME = "xxx@yahoo.com";
    public static final String YAHOO_USERPASSWORD = "xxx";
    public static final String YAHOO_FULLUSERNAME = "xxx@yahoo.com";
    public static final String YAHOO_CLIENTIDENTITY = "https://me.yahoo.com/a/xxx";
    public static final String YAHOO_LOGINBUTTON = null;
    public static final String YAHOO_CONFIRMBUTTON = null;
    public static final String YAHOO_CONFIRMBUTTONVALUE = null;
    public static final Long YAHOO_CONFIRMSLEEP = null;
    public static final String YAHOO_LOGINTITLE = "Sign in to Yahoo!";

    /* ********************** ID_TOKEN ************************ */
    public static final String ID_TOKEN_KEY = "id_token";
    public static final String IDTOK_ISSUER_KEY = "iss";
    public static final String IDTOK_SUBJECT_KEY = "sub";
    public static final String IDTOK_AUDIENCE_KEY = "aud";
    public static final String IDTOK_EXPIRE_KEY = "exp";
    public static final String IDTOK_ISSUETIME_KEY = "iat";
    public static final String IDTOK_NONCE_KEY = "nonce";
    public static final String IDTOK_AT_HASH_KEY = "at_hash";
    public static final String IDTOK_REALM_KEY = "realmName";
    public static final String IDTOK_UNIQ_SEC_NAME_KEY = "uniqueSecurityName";

    /* ********************** ACC_TOKEN ************************ */

    public static final String ACCTOK_CLIENT_ID = "client_id";
    public static final String ACCTOK_ACTIVE_STATUS = "active";
    public static final String ACCTOK_SCOPE = "scope";
    public static final String ACCTOK_ISSUER_KEY = "iss";
    public static final String ACCTOK_SUBJECT_KEY = "sub";
    public static final String ACCTOK_AUDIENCE_KEY = "aud";
    public static final String ACCTOK_EXPIRE_KEY = "exp";
    public static final String ACCTOK_ISSUETIME_KEY = "iat";
    public static final String ACCTOK_REALM_KEY = "realmName";
    public static final String ACCTOK_UNIQ_SEC_NAME_KEY = "uniqueSecurityName";
    public static final String ACCTOK_GROUP_IDS = "groupIds";
    public static final String ACCTOK_TYPE = "token_type";

    /* ***************** General Response ******************* */
    public static final String ACCESS_TOKEN_KEY = "access_token";
    public static final String REFRESH_TOKEN_KEY = "refresh_token";
    public static final String EXPIRES_IN_KEY = "expires_in";
    public static final String TOKEN_TYPE_KEY = "token_type";
    public static final String STATE_KEY = "state";
    public static final String SCOPE_KEY = "scope";
    public static final String JWT_TOKEN = "jwt_token";
    public static final String MP_JWT_TOKEN = "mpJwt_token";
    public static final String JWT_TOKEN_FORMAT = "jwt";
    public static final String MP_JWT_TOKEN_FORMAT = "mpjwt";
    public static final String OPAQUE_TOKEN_FORMAT = "opaque";
    public static final String JWS_TOKEN_FORMAT = "jws_token";
    public static final String JWE_TOKEN_FORMAT = "jwe_token";
    public static final String APP_PASSWORD_KEY = "app_password";
    public static final String APP_TOKEN_KEY = "app_token";
    public static final String APP_ID_KEY = "app_id";
    public static final String CREATED_AT_KEY = "created_at";
    public static final String EXPIRES_AT_KEY = "expires_at";
    public static final String APP_NAME_KEY = "name";
    public static final String USER_NAME_KEY = "user";

    public static final String OIDC_PROVIDER = "";
    public static final String OIDC_USERPARM = "j_username";
    public static final String OIDC_PASSPARM = "j_password";
    public static final String OIDC_USERNAME = "testuser";
    public static final String OIDC_USERPASSWORD = "testuserpwd";
    public static final String OIDC_FULLUSERNAME = "Mr. testuser";
    public static final String OIDC_CLIENTIDENTITY = "client01";
    public static final String OIDC_LOGINBUTTON = "submitButton";
    public static final String OIDC_CONFIRMBUTTON = null;
    public static final String OIDC_CONFIRMBUTTONVALUE = null;
    public static final Long OIDC_CONFIRMSLEEP = null;
    public static final String OIDC_LOGINTITLE = "Login";
    public static final String OIDC_TOKEN_MANAGER_USER = "tokenmgr";
    public static final String OIDC_TOKEN_MANAGER_PWD = "tokenmgrpwd";

    /* ********************** MESSAGES ************************ */
    public static final String MSG_INVALID_GRANT_TYPE = "CWOAU0025E";
    public static final String MSG_INVALID_HTTP_METHOD = "CWOAU0030E";
    public static final String MSG_CLIENTID_NOT_MATCH_AUTHENTICATED = "CWOAU0031E";
    public static final String MSG_REQUIRED_PARAM_MISSING = "CWOAU0033E";
    public static final String MSG_INVALID_CLIENTID_OR_SECRET = "CWOAU0038E";
    public static final String MSG_OP_INVALID_CLIENTID_OR_SECRET = MSG_INVALID_CLIENTID_OR_SECRET;
    public static final String MSG_RP_REQUEST_INVALID = "CWOAU0039W";
    public static final String MSG_CLIENTID_NOT_MATCH_AUTHENTICATED_LOG = "CWOAU0063E";
    public static final String MSG_PUBLIC_CLIENT_ENDPT_REQUIRES_CONF_CLIENT = "CWOAU0071E";

    public static final String MSG_MISSING_TOKEN_PARAM = "CWWKS1405E";
    public static final String MSG_INVALID_CLIENT_CRED = "CWWKS1406E";
    public static final String MSG_RP_INVALID_CLIENTID_OR_SECRET = MSG_INVALID_CLIENT_CRED;
    public static final String MSG_UNABLE_TO_CONTACT_OP = "CWWKS1708E";

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
    public static final String HEADER_DEFAULT_KEY_ID = "autokeyid";

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
    public static final String PAYLOAD_GROUP = "groupIds";
    public static final String PAYLOAD_GROUPS = "groups";
    public static final String PAYLOAD_USER_PRINCIPAL_NAME = "upn";
    public static final String PAYLOAD_TOKEN_TYPE = "token_type";

    /************************** jwt_bearer request parameters *********************************/
    public static final String JWT_BEARER_TOKEN = "jwt_bearer_token";
    public static final String JWT_TOKEN_ENDPOINT = "token_endpoint";
    public static final String JWT_CLIENT_ID = "client_id";
    public static final String JWT_CLIENT_SECRET = "client_secret";
    public static final String JWT_SCOPE = "scope";
    public static final String JWT_SCOPE2 = "scope2";

    /************************* misc JWT *****************************/
    public static final String JWT_ISSUER_CLAIM = "/ibm/api/jwt/";
    public static final String JWT_DEFAULT_EXPIRY = "2h";
    public static final String BUILT_JWT_TOKEN = "Built JWT Token: ";
    public static final String BUILD_JWT_TOKEN = "build_jwt_token";
    public static final String VALIDATE_JWT_CLAIMS = "validate_jwt_claims";
    public static final String VALIDATE_A_JWT_CLAIM = "validate_a_jwt_claim";
    public static final String REMOVE_JWT_CLAIMS = "remove_jwt_claims";
    // /*********************** GRANT TYPES ************************ */
    // public static final String GRANT_TYPE = "grant_type";
    // public static final String GRANT_CLIENT_CREDENTIALS =
    // "client_credentials";
    // public static final String GRANT_IMPLICIT = "implicit";
    // public static final String GRANT_AUTH_CODE = "authorization_code";
    // public static final String GRANT_REFRESH_TOKEN = "refresh_token";
    // public static final String GRANT_PASSWORD = "password";

    /********************************** grant types ******************************************/
    public static final String GRANT_TYPE = "grant_type";
    public static final String AUTH_CODE_GRANT_TYPE = "authorization_code";
    public static final String IMPLICIT_GRANT_TYPE = "implicit";
    public static final String PASSWORD_GRANT_TYPE = "password";
    public static final String APP_PASSWORD_GRANT_TYPE = "app-password";
    public static final String REFRESH_TOKEN_GRANT_TYPE = "refresh_token";
    public static final String CLIENT_CRED_GRANT_TYPE = "client_credentials";
    public static final String JWT_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer";
    public static final String BEARER = "Bearer";
    public static final String AUTHORIZATION = "Authorization";
    public static final String INVALID_GRANT_TYPE_MSG = "unsupported_grant_type";

    public static final String SAML_CLIENT_APP = "samlclient";

    public static final String IDToken_STR = "IDToken:\\{.*?\"iss\":\"http";
    public static final String IDToken_STR_START = "IDToken:\\{";
    public static final String JWT_STR_START = "JsonWebToken:\\{";

    public static final String CURRENT_VALUE = "currentValue";
    public static final String API_VALUE = "apiValue";
    public static final String TOKEN_CONTENT = "tokenContent";
    public static final String CONTEXT_SET = "contextSet";
    public static final String WHERE = "where";
    public static final String TARGET_APP = "targetApp";
    public static final String PROPAGATE_TOKEN_STRING_TRUE = "propagate_token_string_true";
    public static final String PROPAGATE_TOKEN_BOOLEAN_TRUE = "propagate_token_boolean_true";
    public static final String PROPAGATE_TOKEN_STRING_FALSE = "propagate_token_string__false";
    public static final String PROPAGATE_TOKEN_BOOLEAN_FALSE = "propagate_token_boolean_false";
    public static final String PROPAGATE_JWT_TOKEN_STRING_TRUE = "propagate_jwt_token_string_true";
    public static final String PROPAGATE_JWT_TOKEN_BOOLEAN_TRUE = "propagate_jwt_token_boolean_true";
    public static final String PROPAGATE_JWT_TOKEN_STRING_FALSE = "propagate_jwt_token_string_false";
    public static final String PROPAGATE_JWT_TOKEN_BOOLEAN_FALSE = "propagate_jwt_token_boolean_false";
    public static final String HEADER_NAME = "headerName";
    public static final String JWT_BUILDER_NAME = "jwtBuilder";
    public static final String OAUTH_HANDLER = "com.ibm.ws.jaxrs.client.oauth.sendToken";
    public static final String JWT_HANDLER = "com.ibm.ws.jaxrs.client.oidc.sendJwtToken";

    public static final String RS_COOKIE = "RS_ltpatoken";
    public static final String OP_COOKIE = "OP_ltpatoken";
    public static final String CLIENT_COOKIE = "WASOidcClient_";

    public static final String NONE = "none";
    public static final String REQUIRED = "required";
    public static final String SUPPORTED = "supported";

    /******************************** Certificate settings *******************************/
    public static final String X509_CERT = "x509_cert";
    public static final String JWK_CERT = "jwk_cert";
    public static final String SHARED_SECRET = "shared secret";
    public static final String SHARED = "shared";
    public static final String SECRET = "secret";
    public static final String X509 = "x509";
    public static final String JWK = "jwk";

    /************************************* ffdc strings **********************************/
    public static final String ID_TOKEN_VALIDATION_FFDC = "com.ibm.ws.security.openidconnect.token.IDTokenValidationFailedException";
    public static final String JWT_VALIDATION_FFDC = "org.jose4j.jwt.consumer.InvalidJwtException";

    /********************************* API Client Servlet ********************************/
    public static final String JWT_BUILDER_ACTION_BUILD = "build_token";
    public static final String JWT_BUILDER_ACTION_DEFAULT = "build_default_token";
    public static final String JWT_BUILDER_ACTION_CLAIM_FROM = "claim_from";
    public static final String JWT_BUILDER_ACTION_CLAIM_FROM_JWT_TOKEN = "claim_from_JwtToken";
    public static final String JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING = "claim_from_JwtString";
    public static final String JWT_BUILDER_ACTION_CLAIM_FROM_JWT_TOKEN_NULL = "claim_from_JwtToken_null";
    public static final String JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING_NULL = "claim_from_JwtString_null";
    public static final String JWT_BUILDER_ACTION_CLAIM_FROM_ENCODED_PAYLOAD = "claim_from_EncodedPayload";
    public static final String JWT_BUILDER_ACTION_CLAIM_FROM_DECODED_PAYLOAD = "claim_from_DecodedPayload";
    public static final String JWT_BUILDER_FINAL_TOKEN = "FinalJWTToken: ";
    public static final String JWT_BUILDER_TOKEN_1 = "JWTToken1: ";
    public static final String JWT_BUILDER_BEFORE_REMOVE_TOKEN = "BeforeRemoveJWTToken: ";
    public static final String JWT_BUILDER_HEADER = "Header: ";
    public static final String JWT_BUILDER_PAYLOAD = "Payload: ";
    public static final String JWT_BUILDER_KEYID = "KeyId: ";
    public static final String JWT_BUILDER_ALGORITHM = "Algorithm: ";
    public static final String JWT_BUILDER_ISSUER = "Issuer: ";
    public static final String JWT_BUILDER_SUBJECT = "Subject: ";
    public static final String JWT_BUILDER_AUDIENCE = "Audience: ";
    public static final String JWT_BUILDER_EXPIRATION = "Expiration: ";
    public static final String JWT_BUILDER_NOTBEFORE = "NotBefore: ";
    public static final String JWT_BUILDER_ISSUED_AT = "IssuedAt: ";
    public static final String JWT_BUILDER_JWTID = "JwtId: ";
    public static final String JWT_BUILDER_AUTHORIZEDPARTY = "AuthorizedParty: ";
    public static final String JWT_BUILDER_JSON = "JSON: ";
    public static final String JWT_BUILDER_CLAIM = "Claim: ";
    public static final String JWT_BUILDER_GETCLAIM = "getClaim: ";
    public static final String JWT_BUILDER_GETALLCLAIMS = "getAllClaims: ";
    public static final String JWT_BUILDER_KEY = "Key: ";
    public static final String JWT_BUILDER_VALUE = "Value: ";
    public static final String JWT_BUILDER_NO_CLAIMS = "No Claims";
    public static final String JWT_BUILDER_NO_TOKEN = "No Token";
    public static final String JWT_BUILDER_NOT_SET = "Not Set";
    public static final String JWT_BUILDER_ADD_AUD = "Adding audiences";
    public static final String JWT_BUILDER_ADD_CLAIMS = "Adding claims";
    public static final String JWT_BUILDER_SET_EXP = "Setting Expiration time:";
    public static final String JWT_BUILDER_SET_NBF = "Setting NotBefore time:";
    public static final String JWT_BUILDER_FETCH = "Fetching: ";
    public static final String JWT_BUILDER_SIGN_WITH = "Setting signWith: ";
    public static final String JWT_BUILDER_LOAD_CLAIMS = "Load JWT Token Claims";
    public static final String JWT_BUILDER_SET_JTI = "Setting JTI:";
    public static final String JWT_BUILDER_SET_SUB = "Setting Subject:";
    public static final String JWT_BUILDER_SET_ISS = "Setting Issuer:";
    public static final String JWT_BUILDER_REMOVE = "Removing:";
    public static final String JWT_BUILDER_DEFAULT_ID = "defaultJWT";
    public static final String JWT_BUILDER_NAME_ATTR = "Name";
    public static final String JWT_CONTEXT_NULL = "JsonWebToken from SecurityContext was null";
    // TODO get correct list from Chunlong
    // public static final String[] JWT_BUILDER_REQUIRED_KEYS = {
    // Constants.PAYLOAD_ISSUER, Constants.PAYLOAD_SUBJECT,
    // Constants.PAYLOAD_EXPIRATION_TIME_IN_SECS, Constants.PAYLOAD_TOKEN_TYPE
    // };

    // 20171219 TYP is required for mp-jwt tokens, but we're going to add that in code so we don't have
    // test failures while the OL token change finds its way into CL.
    public static final String[] JWT_BUILDER_REQUIRED_HEADER_KEYS = { HEADER_ALGORITHM };
    public static final String[] JWT_BUILDER_REQUIRED_PAYLOAD_KEYS = { PAYLOAD_ISSUER, PAYLOAD_EXPIRATION_TIME_IN_SECS, PAYLOAD_TOKEN_TYPE, PAYLOAD_ISSUED_AT_TIME_IN_SECS };
    public static final String[] MP_JWT_BUILDER_REQUIRED_PAYLOAD_KEYS = { PAYLOAD_ISSUER, PAYLOAD_EXPIRATION_TIME_IN_SECS, PAYLOAD_TOKEN_TYPE, PAYLOAD_ISSUED_AT_TIME_IN_SECS,
            PAYLOAD_USER_PRINCIPAL_NAME };

    /********************************* JWT Consumer API Servlet ********************************/
    public static final String JWT_CONSUMER_SERVLET = "jwtconsumerclient";
    public static final String JWT_CONSUMER_ENDPOINT = JWT_CONSUMER_SERVLET + "/JwtConsumerClient";
    public static final String JWT_CONSUMER_PARAM_CLIENT_ID = "clientId";
    public static final String JWT_CONSUMER_PARAM_JWT = "jwtString";
    public static final String JWT_CONSUMER_START_SERVLET_OUTPUT = "Start of JwtConsumerClient output";
    public static final String JWT_CONSUMER_SUCCESSFUL_CONSUMER_CREATION = "Successfully created consumer for id [";
    public static final String JWT_CONSUMER_TOKEN_LINE = Constants.BUILT_JWT_TOKEN;
    public static final String JWT_CONSUMER_CLAIM = "JWT Consumer Claim: ";
    public static final String JWT_CONSUMER_TOKEN_HEADER_MALFORMED = "Header malformed: ";
    public static final String JWT_CONSUMER_TOKEN_HEADER_JSON = "JSON Header: ";
    public static final String JWT_REALM_NAME = "realmName";

    public static final String SOCIAL_JWT_TOKEN = "Social JWT Token";

    /*********************************** Same Site **********************************************/
    public static final String SAMESITE_DISABLED = "Disabled";
    public static final String SAMESITE_LAX = "Lax";
    public static final String SAMESITE_NONE = "None";
    public static final String SAMESITE_STRICT = "Strict";

    /*********************************** RP TRacking *******************************************/
    public static final String RP_TRACKING_COOKIE_NAME = "WasOAuthTrackClients";

    /******************************** Test Tooling Servlets ************************************/
    public static final String TOKEN_ENDPOINT_SERVLET = "TokenEndpointServlet";
    public static final String USERINFO_ENDPOINT_SERVLET = "UserinfoEndpointServlet";

}
