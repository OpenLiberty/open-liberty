/*******************************************************************************
 * Copyright (c) 2014, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml20.fat.commonTest;

import com.ibm.ws.security.fat.common.Constants;

public class SAMLConstants extends Constants {

    public SAMLConstants() {
        super();
    }

    /* ************************* Port Variables ************************* */
    public static final String SYS_PROP_PORT_IDP_HTTP_DEFAULT = "security_3_HTTP_default";
    public static final String SYS_PROP_PORT_IDP_HTTPS_DEFAULT = "security_3_HTTP_default.secure";

    /* ********************** Test Configuration setups ********************* */
    public static final String SAML_ONLY_SETUP = "saml_only_setup";
    public static final String SAML_OIDC_SAME_MACHINE_SETUP = "saml_oidc_same_machines_setup";
    public static final String SAML_OIDC_DIFF_MACHINES_SETUP = "saml_oidc_diff_machines_setup";

    /* ********************** Test Server Types *********************** */
    public static final String SAML_SERVER_TYPE = "SAML";
    public static final String OIDC_SERVER_TYPE = "OIDC";
    //	public static final String OAUTH_SERVER_TYPE = "OAUTH";
    public static final String SAML_OIDC_SERVER_TYPE = "SAML_OIDC";
    public static final String APP_SERVER_TYPE = "APP_SERVER";
    public static final String SAML_APP_SERVER_TYPE = "SAML_APP_SERVER";
    public static final String IDP_SERVER_TYPE = "IDP_SERVER";

    /* ********************** Test Flow Types ********************* */
    public static final String IDP_INITIATED = "idp_initiated";
    public static final String SOLICITED_SP_INITIATED = "solicited_sp_initiated";
    public static final String UNSOLICITED_SP_INITIATED = "unsolicited_sp_initiated";
    public static final String SP_INITIATED = "sp_initiated";
    public static final String HTTPSERVLET_INITIATED = "httpServletRequest_initiated";
    public static final String IBMSECURITYLOGOUT_INITIATED = "ibmSecurityLogout_initiated";

    /* ******************** steps in the test process ***************** */
    public static final String GET_IDP_ENDPOINT = "getIDPEndPoint";
    public static final String BUILD_POST_IDP_INITIATED_REQUEST = "buildPostIDPInitiatedRequest";
    public static final String BUILD_POST_SP_INITIATED_REQUEST = "buildPostSPInitiatedRequest";
    public static final String PERFORM_IDP_LOGIN = "performIDPLogin";
    public static final String INVOKE_ACS_WITH_SAML_RESPONSE = "invokeACSWithSAMLResponse";
    public static final String INVOKE_ACS_WITH_SAML_RESPONSE_AGAIN = "invokeACSWithSAMLResponseAgain";
    public static final String INVOKE_ACS_WITH_SAML_RESPONSE_BYPASS_APP = "invokeACSWithSAMLResponseBypassApp";
    public static final String INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES = "invokeACSWithSAMLResponseKeepingCookies";
    public static final String PROCESS_LOGIN_CONTINUE = "processLoginContinue";
    public static final String PROCESS_IDP_CONTINUE = "processIdpContinue";
    public static final String PROCESS_LOGOUT_CONTINUE = "processLogoutContinue";
    public static final String PROCESS_LOGOUT_CONTINUE2 = "processLogoutContinue2";
    public static final String PROCESS_LOGOUT_PROPAGATE_YES = "processLogoutPropagateYes";
    public static final String PROCESS_IDP_REQUEST = "processIdpRequest";
    public static final String PROCESS_LOGIN_REQUEST = "processLoginRequest";
    public static final String PROCESS_LOGOUT_REQUEST = "processLogoutRequest";
    public static final String PROCESS_LOGOUT_REDIRECT = "processLogoutRedirect";
    public static final String INVOKE_DEFAULT_APP = "defaultApp";
    public static final String INVOKE_ALTERNATE_APP = "alternateApp";
    public static final String INVOKE_DEFAULT_APP_NEW_CONVERSATION = "defaultAppNewConversation";
    public static final String INVOKE_ALTERNATE_APP_NEW_CONVERSATION = "alternateAppNewConversation";
    public static final String INVOKE_DEFAULT_APP_SAME_CONVERSATION = "defaultAppSameConversation";
    public static final String INVOKE_ALTERNATE_APP_SAME_CONVERSATION = "alternateAppSameConversation";
    public static final String SAML_META_DATA_ENDPOINT = "SAMLMetaDataEndpoint";
    public static final String PROCESS_FORM_LOGIN = "FormLogin";
    public static final String HANDLE_IDPCLIENT_JSP = "handleIdpClientJsp";
    public static final String PROCESS_IDP_JSP = "processIdpJsp";
    public static final String SLEEP_BEFORE_LOGIN = "sleepBeforeLogin";
    public static final String PERFORM_SP_LOGOUT = "performSPLogout";
    public static final String PERFORM_IDP_LOGOUT = "performIDPLogout";
    public static final String INVOKE_JAXRS_GET = "invokeJAXRSGet";
    public static final String INVOKE_JAXRS_GET_OVERRIDEAPP = "invokeJAXRSGetOverrideApp";
    public static final String INVOKE_JAXRS_GET_VIASERVICECLIENT = "invokeJAXRSGetViaServiceClient";
    public static final String GENERIC_INVOKE_PAGE = "genericInvokePage";

    // All SAML actions/tasks should be included in this list!
    //    public static final String[] ALL_TEST_ACTIONS = { GET_IDP_ENDPOINT, BUILD_POST_IDP_INITIATED_REQUEST, BUILD_POST_SOLICITED_SP_INITIATED_REQUEST, BUILD_POST_UNSOLICITED_SP_INITIATED_REQUEST,
    public static final String[] ALL_TEST_ACTIONS = { GET_IDP_ENDPOINT, BUILD_POST_IDP_INITIATED_REQUEST, BUILD_POST_SP_INITIATED_REQUEST,
                                                      PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE, INVOKE_ACS_WITH_SAML_RESPONSE_AGAIN,
                                                      INVOKE_ACS_WITH_SAML_RESPONSE_BYPASS_APP, INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES,
                                                      INVOKE_DEFAULT_APP, INVOKE_ALTERNATE_APP, INVOKE_DEFAULT_APP_NEW_CONVERSATION, INVOKE_ALTERNATE_APP_NEW_CONVERSATION,
                                                      INVOKE_DEFAULT_APP_SAME_CONVERSATION,
                                                      INVOKE_ALTERNATE_APP_SAME_CONVERSATION, SAML_META_DATA_ENDPOINT, PROCESS_FORM_LOGIN, HANDLE_IDPCLIENT_JSP, PROCESS_IDP_JSP,
                                                      SLEEP_BEFORE_LOGIN, PERFORM_SP_LOGOUT, INVOKE_JAXRS_GET,
                                                      PROCESS_LOGIN_CONTINUE, PROCESS_IDP_REQUEST, PROCESS_LOGIN_REQUEST, PROCESS_LOGOUT_REQUEST, PROCESS_IDP_CONTINUE,
                                                      PROCESS_LOGOUT_CONTINUE, PROCESS_LOGOUT_PROPAGATE_YES, PERFORM_IDP_LOGOUT, GENERIC_INVOKE_PAGE };
    /** IDP Initiated Flows **/
    public static final String[] IDP_INITIATED_FLOW_ONLY_IDP = { BUILD_POST_IDP_INITIATED_REQUEST };
    public static final String[] IDP_INITIATED_FLOW = { BUILD_POST_IDP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE };
    public static final String[] IDP_INITIATED_FLOW_KEEPING_COOKIES = { BUILD_POST_IDP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES };
    public static final String[] IDP_INITIATED_FLOW_AGAIN = { BUILD_POST_IDP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE,
                                                              INVOKE_ACS_WITH_SAML_RESPONSE_AGAIN };
    public static final String[] IDP_INITIATED_GET_SAML_TOKEN = { BUILD_POST_IDP_INITIATED_REQUEST, PERFORM_IDP_LOGIN };
    public static final String[] IDP_INITIATED_FLOW_INVOKE_DEF_APP_AGAIN = { BUILD_POST_IDP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE,
                                                                             INVOKE_DEFAULT_APP };
    public static final String[] IDP_INITIATED_FLOW_INVOKE_DEF_APP_AGAIN_KEEPING_COOKIES = { BUILD_POST_IDP_INITIATED_REQUEST, PERFORM_IDP_LOGIN,
                                                                                             INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES, INVOKE_DEFAULT_APP };
    public static final String[] IDP_INITIATED_FLOW_INVOKE_DEF_APP_AGAIN_NEW_CONVERSATION = { BUILD_POST_IDP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE,
                                                                                              INVOKE_DEFAULT_APP_NEW_CONVERSATION };
    public static final String[] IDP_INITIATED_FLOW_MISSING_TARGET_INVOKE_DEF_APP_AGAIN = { BUILD_POST_IDP_INITIATED_REQUEST, PERFORM_IDP_LOGIN,
                                                                                            INVOKE_ACS_WITH_SAML_RESPONSE_BYPASS_APP, INVOKE_DEFAULT_APP };
    public static final String[] IDP_INITIATED_EXTENDED_FLOW = { BUILD_POST_IDP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE, INVOKE_DEFAULT_APP,
                                                                 INVOKE_ALTERNATE_APP };
    public static final String[] IDP_INITIATED_EXTENDED_FLOW_KEEPING_COOKIES = { BUILD_POST_IDP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES,
                                                                                 INVOKE_DEFAULT_APP, INVOKE_ALTERNATE_APP };
    public static final String[] IDP_INITIATED_FLOW_WITH_ALT_APP = { BUILD_POST_IDP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE, INVOKE_ALTERNATE_APP };
    public static final String[] IDP_INITIATED_FLOW_NEED_REAUTH = { BUILD_POST_IDP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE, PERFORM_IDP_LOGIN };
    public static final String[] IDP_INITIATED_NO_MATCH_FORMLOGIN = { BUILD_POST_IDP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE, PROCESS_FORM_LOGIN };
    public static final String[] IDP_INITIATED_FLOW_SLEEP_BEFORE_LOGIN = { BUILD_POST_IDP_INITIATED_REQUEST, SLEEP_BEFORE_LOGIN, PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE };
    public static final String[] IDP_INITIATED_LOGOUT_ONLY = { PERFORM_IDP_LOGOUT };
    //    public static final String[] IDP_INITIATED_LOGOUT = { PERFORM_IDP_LOGOUT, PROCESS_LOGOUT_CONTINUE, PROCESS_LOGOUT_PROPAGATE_YES, PROCESS_LOGOUT_REDIRECT };
    //    public static final String[] IDP_INITIATED_LOGOUT_NO_SESSIONS = { PERFORM_IDP_LOGOUT, PROCESS_LOGOUT_PROPAGATE_YES, PROCESS_LOGOUT_REDIRECT };
    //    public static final String[] IDP_INITIATED_LOGOUT_LOADING_SESSIONS = { PERFORM_IDP_LOGOUT, PROCESS_LOGOUT_CONTINUE, PROCESS_LOGOUT_PROPAGATE_YES, PROCESS_LOGOUT_REDIRECT };
    public static final String[] IDP_INITIATED_LOGOUT = { PERFORM_IDP_LOGOUT, PROCESS_LOGOUT_CONTINUE, PROCESS_LOGOUT_PROPAGATE_YES };
    public static final String[] IDP_INITIATED_LOGOUT_NO_SESSIONS = { PERFORM_IDP_LOGOUT, PROCESS_LOGOUT_PROPAGATE_YES };
    public static final String[] IDP_INITIATED_LOGOUT_LOADING_SESSIONS = { PERFORM_IDP_LOGOUT, PROCESS_LOGOUT_CONTINUE, PROCESS_LOGOUT_PROPAGATE_YES };
    /**
     * IDP_INIT_LOGIN_IDP_INIT_LOGOUT_LOCAL_ONLY_FLOW
     * IDP_INIT_LOGIN_IDP_INIT_LOGOUT_FLOW
     * IDP_LOGOUT_ONLY
     * IDP_INIT_LOGIN_SP_INIT_LOGOUT_LOCAL_ONLY_FLOW
     * IDP_INIT_LOGIN_SP_INIT_LOGOUT_FLOW
     * SP_LOGOUT_ONLY
     *
     */
    //new flow without idp_login
    public static final String[] IDP_INITIATED_ACS_KEEPING_COOKIES = { BUILD_POST_IDP_INITIATED_REQUEST, PROCESS_LOGIN_CONTINUE, INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES };
    public static final String[] IDP_INITIATED_THROUGH_JAXRS_GET = { BUILD_POST_IDP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_JAXRS_GET };
    public static final String[] IDP_INITIATED_THROUGH_JAXRS_GET_OVERRIDEAPP = { BUILD_POST_IDP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_JAXRS_GET_OVERRIDEAPP };
    public static final String[] IDP_INITIATED_THROUGH_JAXRS_GET_SVC_CLIENT = { BUILD_POST_IDP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_JAXRS_GET_VIASERVICECLIENT };

    /** Solicited SP Initiated Flows **/
    public static final String[] SOLICITED_SP_INITIATED_FLOW_ONLY_SP = { BUILD_POST_SP_INITIATED_REQUEST };
    public static final String[] SOLICITED_SP_INITIATED_FLOW = { BUILD_POST_SP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE };
    public static final String[] SOLICITED_SP_INITIATED_FLOW_KEEPING_COOKIES = { BUILD_POST_SP_INITIATED_REQUEST, PERFORM_IDP_LOGIN,
                                                                                 INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES };
    public static final String[] SOLICITED_SP_INITIATED_FLOW_USING_IDP_KEEPING_COOKIES = { BUILD_POST_SP_INITIATED_REQUEST, PROCESS_IDP_REQUEST, PROCESS_IDP_CONTINUE,
                                                                                           PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES };
    public static final String[] SOLICITED_SP_INITIATED_GET_SAML_RESPONSE = { BUILD_POST_SP_INITIATED_REQUEST, PERFORM_IDP_LOGIN };
    public static final String[] SOLICITED_SP_INITIATED_FLOW_AGAIN = { BUILD_POST_SP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE,
                                                                       INVOKE_ACS_WITH_SAML_RESPONSE_AGAIN };
    public static final String[] SOLICITED_SP_INITIATED_FLOW_INVOKE_DEF_APP_AGAIN = { BUILD_POST_SP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE,
                                                                                      INVOKE_DEFAULT_APP };
    public static final String[] SOLICITED_SP_INITIATED_FLOW_INVOKE_DEF_APP_AGAIN_KEEPING_COOKIES = { BUILD_POST_SP_INITIATED_REQUEST, PERFORM_IDP_LOGIN,
                                                                                                      INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES, INVOKE_DEFAULT_APP };
    public static final String[] SOLICITED_SP_INITIATED_FLOW_INVOKE_DEF_APP_AGAIN_NEW_CONVERSATION = { BUILD_POST_SP_INITIATED_REQUEST, PERFORM_IDP_LOGIN,
                                                                                                       INVOKE_ACS_WITH_SAML_RESPONSE, INVOKE_DEFAULT_APP_NEW_CONVERSATION };
    public static final String[] SOLICITED_SP_INITIATED_FLOW_INVOKE_DEF_APP_SAME_CONVERSATION = { BUILD_POST_SP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE,
                                                                                                  INVOKE_DEFAULT_APP_SAME_CONVERSATION };
    public static final String[] SOLICITED_SP_INITIATED_FLOW_INVOKE_ALT_APP_SAME_CONVERSATION = { BUILD_POST_SP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE,
                                                                                                  INVOKE_ALTERNATE_APP_SAME_CONVERSATION };
    public static final String[] SOLICITED_SP_INITIATED_EXTENDED_FLOW = { BUILD_POST_SP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE, INVOKE_DEFAULT_APP,
                                                                          INVOKE_ALTERNATE_APP };
    public static final String[] SOLICITED_SP_INITIATED_EXTENDED_FLOW_KEEPING_COOKIES = { BUILD_POST_SP_INITIATED_REQUEST, PERFORM_IDP_LOGIN,
                                                                                          INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES, INVOKE_DEFAULT_APP, INVOKE_ALTERNATE_APP };
    public static final String[] SOLICITED_SP_INITIATED_FLOW_WITH_ALT_APP = { BUILD_POST_SP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE,
                                                                              INVOKE_ALTERNATE_APP };
    public static final String[] SOLICITED_SP_INITIATED_FLOW_NEED_REAUTH = { BUILD_POST_SP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE, PERFORM_IDP_LOGIN };
    public static final String[] SOLICITED_SP_INITIATED_NO_MATCH_FORMLOGIN = { BUILD_POST_SP_INITIATED_REQUEST, PROCESS_FORM_LOGIN };
    public static final String[] SOLICITED_SP_INITIATED_FLOW_SLEEP_BEFORE_LOGIN = { BUILD_POST_SP_INITIATED_REQUEST, SLEEP_BEFORE_LOGIN, PERFORM_IDP_LOGIN,
                                                                                    INVOKE_ACS_WITH_SAML_RESPONSE };
    public static final String[] SOLICITED_SP_INITIATED_FLOW_LOGOUT_LOCAL_ONLY = { BUILD_POST_SP_INITIATED_REQUEST, PERFORM_IDP_LOGIN,
                                                                                   INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES, PERFORM_SP_LOGOUT };
    public static final String[] SOLICITED_SP_INITIATED_FLOW_LOGOUT = { BUILD_POST_SP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES,
                                                                        PERFORM_SP_LOGOUT, PROCESS_LOGOUT_REQUEST, PROCESS_LOGOUT_PROPAGATE_YES };
    public static final String[] SP_INITIATED_LOGOUT_ONLY = { PERFORM_SP_LOGOUT };
    public static final String[] SP_INITIATED_LOGOUT = { PERFORM_SP_LOGOUT, PROCESS_LOGOUT_REQUEST, PROCESS_LOGOUT_PROPAGATE_YES };
    public static final String[] SP_INITIATED_LOGOUT_LOADING_SESSIONS = { PERFORM_SP_LOGOUT, PROCESS_LOGOUT_REQUEST, PROCESS_LOGOUT_CONTINUE, PROCESS_LOGOUT_PROPAGATE_YES };
    public static final String[] SOLICITED_SP_INITIATED_ACS_KEEPING_COOKIES = { BUILD_POST_SP_INITIATED_REQUEST, PROCESS_LOGIN_REQUEST, PROCESS_LOGIN_CONTINUE,
                                                                                INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES };
    public static final String[] SOLICITED_SP_INITIATED_THROUGH_JAXRS_GET = { BUILD_POST_SP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_JAXRS_GET };
    public static final String[] SOLICITED_SP_INITIATED_THROUGH_JAXRS_GET_SVC_CLIENT = { BUILD_POST_SP_INITIATED_REQUEST, PERFORM_IDP_LOGIN, INVOKE_JAXRS_GET_VIASERVICECLIENT };

    /** Unsolicited SP Initiated Flows **/
    public static final String[] UNSOLICITED_SP_INITIATED_FLOW_ONLY_SP = { BUILD_POST_SP_INITIATED_REQUEST };
    public static final String[] UNSOLICITED_SP_INITIATED_FLOW = { BUILD_POST_SP_INITIATED_REQUEST, HANDLE_IDPCLIENT_JSP, PROCESS_IDP_JSP, PERFORM_IDP_LOGIN,
                                                                   INVOKE_ACS_WITH_SAML_RESPONSE };
    public static final String[] UNSOLICITED_SP_INITIATED_FLOW_KEEPING_COOKIES = { BUILD_POST_SP_INITIATED_REQUEST, HANDLE_IDPCLIENT_JSP, PROCESS_IDP_JSP, PERFORM_IDP_LOGIN,
                                                                                   INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES };
    public static final String[] UNSOLICITED_SP_INITIATED_FLOW_USING_IDP_KEEPING_COOKIES = { BUILD_POST_SP_INITIATED_REQUEST, HANDLE_IDPCLIENT_JSP, PROCESS_IDP_JSP,
                                                                                             PROCESS_IDP_CONTINUE, PERFORM_IDP_LOGIN,
                                                                                             INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES };
    public static final String[] UNSOLICITED_SP_INITIATED_FLOW_AGAIN = { BUILD_POST_SP_INITIATED_REQUEST, HANDLE_IDPCLIENT_JSP, PROCESS_IDP_JSP, PERFORM_IDP_LOGIN,
                                                                         INVOKE_ACS_WITH_SAML_RESPONSE, INVOKE_ACS_WITH_SAML_RESPONSE_AGAIN };
    public static final String[] UNSOLICITED_SP_INITIATED_GET_SAML_TOKEN = { BUILD_POST_SP_INITIATED_REQUEST, HANDLE_IDPCLIENT_JSP, PROCESS_IDP_JSP, PERFORM_IDP_LOGIN };
    public static final String[] UNSOLICITED_SP_INITIATED_FLOW_INVOKE_DEF_APP_AGAIN = { BUILD_POST_SP_INITIATED_REQUEST, HANDLE_IDPCLIENT_JSP, PROCESS_IDP_JSP, PERFORM_IDP_LOGIN,
                                                                                        INVOKE_ACS_WITH_SAML_RESPONSE, INVOKE_DEFAULT_APP };
    public static final String[] UNSOLICITED_SP_INITIATED_FLOW_INVOKE_DEF_APP_AGAIN_KEEPING_COOKIES = { BUILD_POST_SP_INITIATED_REQUEST, HANDLE_IDPCLIENT_JSP, PROCESS_IDP_JSP,
                                                                                                        PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES,
                                                                                                        INVOKE_DEFAULT_APP };
    public static final String[] UNSOLICITED_SP_INITIATED_EXTENDED_FLOW = { BUILD_POST_SP_INITIATED_REQUEST, HANDLE_IDPCLIENT_JSP, PROCESS_IDP_JSP, PERFORM_IDP_LOGIN,
                                                                            INVOKE_ACS_WITH_SAML_RESPONSE, INVOKE_DEFAULT_APP, INVOKE_ALTERNATE_APP };
    public static final String[] UNSOLICITED_SP_INITIATED_EXTENDED_FLOW_KEEPING_COOKIES = { BUILD_POST_SP_INITIATED_REQUEST, HANDLE_IDPCLIENT_JSP, PROCESS_IDP_JSP,
                                                                                            PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES, INVOKE_DEFAULT_APP,
                                                                                            INVOKE_ALTERNATE_APP };
    public static final String[] UNSOLICITED_SP_INITIATED_NO_MATCH_FORMLOGIN = { BUILD_POST_SP_INITIATED_REQUEST, PROCESS_FORM_LOGIN };
    public static final String[] UNSOLICITED_SP_INITIATED_FLOW_SLEEP_BEFORE_LOGIN = { BUILD_POST_SP_INITIATED_REQUEST, SLEEP_BEFORE_LOGIN, PERFORM_IDP_LOGIN,
                                                                                      INVOKE_ACS_WITH_SAML_RESPONSE };
    //    public static final String[] UNSOLICITED_SP_INITIATED_FLOW_LOGOUT_LOCAL_ONLY = { BUILD_POST_SP_INITIATED_REQUEST, HANDLE_IDPCLIENT_JSP, PROCESS_IDP_JSP, PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES, PERFORM_SP_LOGOUT };
    //    public static final String[] UNSOLICITED_SP_INITIATED_FLOW_LOGOUT = { BUILD_POST_SP_INITIATED_REQUEST, HANDLE_IDPCLIENT_JSP, PROCESS_IDP_JSP, PERFORM_IDP_LOGIN, INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES, PERFORM_SP_LOGOUT, PROCESS_LOGOUT_REQUEST, PROCESS_LOGOUT_PROPAGATE_YES };
    public static final String[] UNSOLICITED_SP_INITIATED_LOGOUT_ONLY = { PERFORM_SP_LOGOUT };
    public static final String[] UNSOLICITED_SP_INITIATED_LOGOUT = { PERFORM_SP_LOGOUT, PROCESS_LOGOUT_REQUEST, PROCESS_LOGOUT_PROPAGATE_YES };
    public static final String[] UNSOLICITED_SP_INITIATED_ACS_KEEPING_COOKIES = { BUILD_POST_SP_INITIATED_REQUEST, HANDLE_IDPCLIENT_JSP, PROCESS_IDP_JSP, PROCESS_LOGIN_CONTINUE,
                                                                                  INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES };
    public static final String[] UNSOLICITED_SP_INITIATED_THROUGH_JAXRS_GET = { BUILD_POST_SP_INITIATED_REQUEST, HANDLE_IDPCLIENT_JSP, PROCESS_IDP_JSP, PERFORM_IDP_LOGIN,
                                                                                INVOKE_JAXRS_GET };

    public static final String[] JUST_INVOKE_JAXRS_GET = { INVOKE_JAXRS_GET };
    public static final String[] JUST_INVOKE_DEFAULT_APP = { INVOKE_DEFAULT_APP };

    /** SAML MetaData **/
    public static final String[] SP_SAMLMETADATAENDPOINT_FLOW = { SAML_META_DATA_ENDPOINT };

    /* ********************* End Steps in the process *********************** */

    /* ****************** validation data location **************** */
    public static final String OIDC_CONSOLE_LOG = "oidc_console.log";
    public static final String OIDC_MESSAGES_LOG = "oidc_messages.log";
    public static final String OIDC_TRACE_LOG = "oidc_trace.log";
    public static final String SAML_CONSOLE_LOG = "saml_console.log";
    public static final String SAML_MESSAGES_LOG = "saml_messages.log";
    public static final String SAML_TRACE_LOG = "saml_trace.log";
    public static final String SAMLOIDC_CONSOLE_LOG = "saml_oidc_console.log";
    public static final String SAMLOIDC_MESSAGES_LOG = "saml_oidc_messages.log";
    public static final String SAMLOIDC_TRACE_LOG = "saml_oidc_trace.log";
    public static final String APP_CONSOLE_LOG = "app_console.log";
    public static final String APP_MESSAGES_LOG = "app_messages.log";
    public static final String APP_TRACE_LOG = "app_trace.log";
    public static final String IDP_PROCESS_LOG = "idp-process.log";

    public static final String RESPONSE_TOKEN = "responseToken";
    public static final String RESPONSE_TOKEN_LENGTH = "response_token_length";
    //    public static final String RESPONSE_ID_TOKEN = "SamlToken";
    public static final String RESPONSE_GENERAL = "generalResponse";
    public static final String EXCEPTION_MESSAGE = "exceptionMessage";
    public static final String SAML_METADATA = "saml_metadata";
    public static final String SP_APP_START_MSG = "SP provider "; // not sure what this might be

    /* ***************** ******************* */
    public static final String LOGIN_ERROR_TITLE = "A Form login authentication failure occurred";
    public static final String LOGIN_ERROR_TEXT = "A Form login authentication failure occurred";
    public static final String SUCCESSFUL_LOGOUT_MSG = "You have successfully logged out";
    public static final String SUCCESSFUL_DEFAULT_LOGOUT_TITLE = "Default Logout Exit Page";
    public static final String SUCCESSFUL_DEFAULT_LOGOUT_MSG = "Successful Logout";
    public static final String SUCCESSFUL_SHIBBOLETH_SP_INIT_LOGOUT_MSG = "The logout operation is complete";
    public static final String SUCCESSFUL_SHIBBOLETH_IDP_INIT_LOGOUT_MSG = "This page is displayed when a logout operation at the Identity Provider completes";
    public static final String NO_REFRESH_TOKEN_MSG = "refresh_token was not found in the token cache.";
    public static final String POSTLOGOUTPAGE = "Redirect To Identity Provider";
    public static final String SUCCESSFUL_DEFAULT_SP_LOGOUT_TITLE = "SAML Single Logout (SLO) Post-Logout";
    public static final String SUCCESSFUL_CUSTOM_SP_LOGOUT_TITLE = "Custom SAML Single Logout (SLO) Post Logout";
    public static final String SUCCESSFUL_DEFAULT_SP_LOGOUT_STATUS = "You successfully logged out";
    public static final String FAILED_LOGOUT_MSG = "You might not be completely logged out";
    public static final String IDP_INIT_SP_LOGOUT_SUCCESS = "\"result\":  \"Success\"";
    public static final String IDP_INIT_SP_LOGOUT_FAILURE = "\"result\":  \"Failure\"";
    public static final String HTTP_SERVLET_REQUEST_RESPONSE = "HttpServletRequestApp Logout";
    public static final String INTERNAL_SERVER_ERROR_MSG = "Internal Server Error";

    /* ******************** Test App Info ********************** */
    public static final String SAML_DEMO_APP = "SAML_Demo";
    public static final String SAML_CLIENT_APP = "samlclient";
    public static final String SAML_CXF_CLIENT_APP = "samlcxfclient";
    public static final String SAML_TOKEN_APP = "samltoken";
    public static final String SAML_CXF_CALLER_CLIENT_APP = "samlcallerclient";
    public static final String SAML_CALLER_TOKEN_APP = "samlcallertoken";
    public static final String SSODEMO = "ssodemo";
    public static final String DEFAULT_APP = "default_app";
    public static final String ALTERNATE_APP = "alternate_app";
    public static final String APP1 = "/samlclient/fat/defaultSP/snoop";
    public static final String APP2 = "/samlclient/fat/defaultSP/SimpleServlet";
    public static final String DEFAULT_SERVLET = "snoop";
    public static final String ALTERNATE_SERVLET = "SimpleServlet";
    public static final String FORMLOGIN_APP = "formlogin";
    public static final String FORMLOGINONLY_APP = "formloginonly";
    public static final String HELLO_WORLD_APP = "helloworld";
    public static final String PARTIAL_HELLO_WORLD_URI = "helloworld/rest/helloworld";
    public static final String JAXRS_SVC_CLIENT = "jaxrsclient/JaxRSClient";
    public static final String JAXRS_SP2_SVC_CLIENT = "jaxrsclient/sp2_JaxRSClient";
    public static final String JAXRS_PROTECTED_SVC_CLIENT = "jaxrsclient/Protected_JaxRSClient";

    /* ******************** IDP App Info ************************ */
    public static final String FEDERATION_JSP = "/fimivt/federations.jsp";
    public static final String IDP1_CHALLENGE_URL = "/sps/WlpIdp1Fed/saml20/logininitial";

    /* ******************** SP App Info ************************ */
    public static final String SP_LANDING_JSP = "fimivt/protected/ivtlanding.jsp";

    public static final String CHECK_URL = "getRequestURL: ";

    /* ******************************************************************************************************************* */
    public static final String IDP_SESSION_COOKIE_NAME = "shib_idp_session";
    public static final String LTPA_TOKEN_NAME = "saml20_SP_sso";

    public static final String SAML_TFIM_POST_RESPONSE = "SAML 2.0 POST response";
    public static final String SAML_ADFS_POST_RESPONSE = "Working...";
    public static final String SAML_SHIBBOLETH_POST_RESPONSE = "";
    public static final String SAML_RESPONSE = "SAMLResponse";
    public static final String SAML_REQUEST = "SAMLRequest";
    // Response from first SP request should contain this
    public static final String SP_LANDING_PAGE = "IBM Tivoli Federated Identity Manager Federation Verification Tool Landing Page";
    public static final String SP_ALTERNATE_PAGE = "Hello! This is an Axis2 Web Service!";
    public static final String IDP_CLIENT_JSP_TITLE = "Saml 2.0 - Request SAML IdP";
    public static final String IDP_PROCESS_JSP_TITLE = "Process IDP request";

    public static final String RECV_AUTH_CODE = "Received authorization code:";
    // public static final  String recvAccessToken = "Received from token endpoint: {\"access_token\":";
    public static final String APP1_TITLE = "Snoop Servlet";
    public static final String APP2_TITLE = "FormLoginServlet";
    public static final String IMPLICIT_APP_TITLE = "OAuth 2.0 Implicit Flow";
    public static final String APPROVAL_FORM = "javascript";
    //    public static final String APPROVAL_HEADER = "OAuth authorization form";
    //    public static final String CUSTOM_APPROVAL_HEADER = "OAuth Custom Consent Form";
    // public static final  String autoauthz = "true";
    public static final String REDIRECT_ACCESS_TOKEN = "access_token=";
    public static final String LOGIN_PROMPT = "Enter your username and password to login";
    public static final String LOGIN_ERROR = "Error: username and password doesn't match";
    //    public static final String CUSTOM_LOGIN_TITLE = "Custom OAuth Login";
    //    public static final String CUSTOM_ERROR_TITLE = "Custom Error Page";
    public static final String LOGIN_TITLE = "Login";
    public static final String SAML_TFIM_LOGIN_HEADER = "ITFIM Form Login";
    public static final String SAML_SHIBBOLETH_LOGIN_HEADER = "Web Login Service";
    public static final String SAML_ADFS_LOGIN_HEADER = "Sign In";
    public static final String SAML_ADFS_LOGIN_PROMPT_PAGE = "Sign-In Page";
    public static final String STANDARD_LOGIN_HEADER = "Regular Form Login without SAML";
    public static final String SAML_TFIM_ERROR_HEADER = "ITFIM Form Login Error";
    public static final String SAML_ADFS_ERROR_HEADER = "ITFIM Form Login Error";
    public static final String SAML_SHIBBOLETH_LOGIN_PROMPT_PAGE = "Our Identity Provider";

    public static final String SAML_DEFAULT_AUTHORIZATION_HEADER_NAME = "SAML";
    public static final String RECV_FROM_TOKEN_ENDPOINT = "Received from token endpoint: {";
    public static final String REFRESH_TOKEN_UPDATED = "Updated \"Refresh Token\" input field with:";
    public static final String INVALID_TOKEN_ERROR = "error=\"invalid_token\"";
    public static final String CHECK_ACCES_TOKEN = "Check access token";
    public static final String FORBIDDEN = "Forbidden";
    //    public static final String CLIENT_COULD_NOT_BE_FOUND = "The OAuth service provider could not find the client because the client name is not valid. Contact your system administrator to resolve the problem.";
    public static final String AUTHENTICATION_FAILED = "Error 403: AuthenticationFailed";
    public static final String AUTHORIZATION_FAILED = "Error 403: AuthorizationFailed";
    public static final String HTTPS_REQUIRED = "HTTPS is required";
    public static final String NOT_FOUND = "not found";
    public static final String UNAUTHORIZED_MESSAGE = "Unauthorized";
    public static final String NO_SAML_TOKEN_FOUND_MSG = "No SAMLToken found";
    public static final String NOT_FOUND_UPPERCASE = "Not Found";
    public static final String FILE_NOT_FOUND_EXCEPTION = "java.io.FileNotFoundException";
    public static final String SAML_AUTHENTICATION_FAILED_TITLE = "SAML 2.0 Authentication Failed";
    public static final String CUSTOM_CACHE_KEY = "com.ibm.wsspi.security.cred.cacheKey";
    public static final String ADFS_FILE_NOT_FOUND = "404 - File or directory not found";

    public static final String SP_DEFAULT_ERROR_PAGE_TITLE = "HTTP Error Message";
    public static final String SP1_ERROR_PAGE_TITLE = "An SP1 Server failure occurred";
    public static final String SP2_ERROR_PAGE_TITLE = "An SP2 Server failure occurred";

    public static final Boolean CALL_EXTRA_APP = true;

    public static final String LOCATION_ALL = "all";
    public static final String LOCATION_FIRST = "first";
    public static final String LOCATION_LAST = "last";

    public static final String HARDCODED_TOKEN_1 = "hardcodedToken1";
    public static final String HARDCODED_TOKEN_MISSING_NAMEID = "hardcodedTokenMissingNameId";

    public static final String HELLO_WORLD_STRING = "Hello World!";
    /* Error response codes */
    public static final String ERROR_RESPONSE_PARM = "error";
    //    public static final String ERROR_CODE_INVALID_REQUEST = "invalid_request";
    //    public static final String ERROR_CODE_INVALID_CLIENT = "invalid_client";
    //    public static final String ERROR_CODE_INVALID_GRANT = "invalid_grant";
    //    public static final String ERROR_CODE_UNAUTHORIZED_CLIENT = "unauthorized_client";
    //    public static final String ERROR_CODE_UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";
    //    public static final String ERROR_CODE_INVALID_SCOPE = "invalid_scope";
    public static final String ERROR_RESPONSE_DESCRIPTION = "error_description";
    public static final String ERROR_CODE_INVALID_TOKEN = "invalid_token";
    //    public static final String ERROR_SERVER_CONFIG = "configuration_error";
    public static final String ERROR_CODE_LOGIN_REQUIRED = "login_required";

    public static final String SHIBBOLETH_IDP_ERROR = "Message Security Error";

    public static final String NODE_TYPE = "node";
    public static final String NODE_EXISTS_TYPE = "node_exists";
    public static final String ELEMENT_TYPE = "element";
    public static final String ALG_TYPE = "algorithm";

    public static final String AES128 = "aes128";
    public static final String AES192 = "aes192";
    public static final String AES256 = "aes256";
    public static final String AES512 = "aes512";

    /* ******************** SAML Token Names *************************** */
    public static final String SAML_TOKEN = "samlToken";
    public static final String SAML_TOKEN_ENCRYPTED = "samlTokenEncrypted";
    public static final String SAML_TOKEN_CONTAINS = "samlTokenContains";
    public static final String SAML_POST_TOKEN = "samlPostToken";
    public static final String SAML_TOKEN_RESPONSE = "saml2p:Response";
    public static final String SAML_TOKEN_SUBJECT = "saml2:Subject";
    public static final String SAML_TOKEN_ASSERTION = "saml2:Assertion";
    public static final String SAML_TOKEN_ENCRYPTED_ASSERTION = "saml2:EncryptedAssertion";
    //    public static final String SAML_TOKEN_ENCRYPTED_DATA = "EncryptedData";
    public static final String SAML_TOKEN_ENCRYPTED_DATA = "xenc:EncryptedData";
    public static final String SAML_TOKEN_ENCRYPTION_METHOD = "xenc:EncryptionMethod";
    public static final String SAML_TOKEN_ENCRYPTION_KEY_INFO = "ds:KeyInfo";
    public static final String SAML_TOKEN_ENCRYPTION_KEY_NAME = "ds:KeyName";
    public static final String SAML_TOKEN_ENCRYPTION_X509 = "ds:X509Data";
    public static final String SAML_TOKEN_ENCRYPTION_X509_CERT = "ds:X509Certificate";
    public static final String SAML_TOKEN_ENCRYPTED_KEY = "xenc:EncryptedKey";
    public static final String SAML_TOKEN_STATUS = "saml2p:Status";
    public static final String SAML_TOKEN_NAMEID = "saml2:NameID";
    public static final String SAML_TOKEN_ISSUER = "saml2:Issuer";
    public static final String SAML_TOKEN__AUDIENCE = "saml2:Audience";
    public static final String SAML_TOKEN_SIGNATURE = "saml2:Subject";
    public static final String SAML_TOKEN_STATUS_CODE = "saml2p:StatusCode";
    public static final String SAML_TOKEN_STATUS_DETAIL = "saml2p:StatusDetail";
    public static final String TFIM_TOKEN_STATUS_DETAIL = "fim:FIMStatusDetail";

    public static final String SAML_ISSUE_INSTANT = "IssueInstant";
    public static final String SAML_NOT_ON_OR_AFTER = "NotOnOrAfter";
    public static final String SAML_SESSION_NOT_ON_OR_AFTER = "SessionNotOnOrAfter";
    public static final String SAML_NOT_BEFORE = "NotBefore";

    /*  */
    public static final String OMITTED = "omitted";
    public static final String EXCEPTION = "exception";
    public static final String NAME_ID = "nameId";
    public static final String LOCAL_REGISTRY = "localRegistry";
    public static final String PUBLIC_CREDENTIAL_STRING = "Public Credential";
    public static final String TFIM_REGISTRY = "tfim_registry";
    public static final String LOCAL_REGISTRY_REALM = "BasicRealm";
    public static final String MESSAGE_ID = "MessageID";
    public static final String UNAUTHENTICATED_USER = "unauthenticated_user";
    public static final String INVALID_REQUEST_NAMEID_POLICY = "invalid_request_nameid_policy";
    public static final String UNEXPECTED_EXCEPTION = "unexpected_exception";
    public static final String BAD_TOKEN_EXCHANGE = "could_not_perform_token_exchange";

    /* *********************** Identifier names ********************* */
    public static final String SAML_USER_IDENTIFIER = "com.ibm.wsspi.security.cred.securityName";
    public static final String SAML_USERUNIQUE_IDENTIFIER = "com.ibm.wsspi.security.cred.uniqueId";
    public static final String SAML_GROUP_IDENTIFIER = "com.ibm.wsspi.security.cred.groups";
    public static final String SAML_REALM_IDENTIFIER = "com.ibm.wsspi.security.cred.realm";
    public static final String SAML_ACCESS_ID = "accessId";
    public static final String SAML_MAPTOUSERREGISTRY_NO = "No";
    public static final String SAML_MAPTOUSERREGISTRY_USER = "User";
    public static final String SAML_MAPTOUSERREGISTRY_GROUP = "Group";

    /* provider types */
    public static final String TFIM_TYPE = "tfimidp";
    public static final String ADFS_TYPE = "adidp";
    public static final String BOTH_IDP_TYPE = "both";
    public static final String SHIBBOLETH_TYPE = "shibbolethidp";

    /* Users/passwords */
    public static final String IDP_USER_NAME = "testuser";
    public static final String IDP_USER_PWD = "testuserpwd";
    public static final String SP_USER_NAME = "user1";
    public static final String SP_USER_PWD = "user1pwd";
    public static final String SAML_USER_1_NAME = "user1";
    public static final String SAML_USER_1_PWD = "security";
    public static final String SAML_USER_2_NAME = "user2";
    public static final String SAML_USER_2_PWD = "security";
    public static final String SAML_USER_3_NAME = "user3";
    public static final String SAML_USER_3_PWD = "security";
    public static final String SAML_USER_4_NAME = "user4";
    public static final String SAML_USER_4_PWD = "security";
    public static final String SAML_USER_5_NAME = "user5";
    public static final String SAML_USER_5_PWD = "security";

    /* access token types (sp or ltpa) */
    public static final String SP_ACCESS_TOKEN_TYPE = "SP";
    public static final String LTPA_ACCESS_TOKEN_TYPE = "LTPA";
    public static final String LTPA_ALTERED_ACCESS_TOKEN_TYPE = "ALTERED_LTPA";
    public static final String SP_AND_LTPA_ACCESS_TOKEN_TYPE = "SP_And_LTPA";

    public static final String SP_COOKIE_PREFIX = "WASSamlSP_";
    public static final String COOKIES = "cookies";
    public static final String JSESSIONID = "JSESSIONID";

    public static final String CXF_SAML_TOKEN_SERVLET = "CXF SAML Service Cleint";
    public static final String CXF_SAML_TOKEN_WSS_SERVLET = "CXF WSS Templates Test Service Client";
    public static final String CXF_SAML_TOKEN_SERVICE = "This is WSSECFVT CXF Web Service";
    public static final String CXF_SSL_SAML_TOKEN_SERVICE = "This is WSSECFVT CXF SSL Web Service (using SAML)";
    public static final String CXF_SAML_TOKEN_SYM_SIGN_SERVICE = "This is WSSECFVT CXF Symmetric Signature Web Service (using SAML)";
    public static final String CXF_SAML_TOKEN_SYM_ENCR_SERVICE = "This is WSSECFVT CXF Symmetric Encryption Web Service (using SAML)";
    public static final String CXF_SAML_TOKEN_SYM_SIGN_ENCR_SERVICE = "This is WSSECFVT CXF Symmetric Signature And Encryption Web Service (using SAML)";
    public static final String CXF_SAML_TOKEN_ASYM_SIGN_SERVICE = "This is WSSECFVT CXF Asymmetric Signature Web Service (using SAML)";
    public static final String CXF_SAML_TOKEN_ASYM_ENCR_SERVICE = "This is WSSECFVT CXF Asymmetric Encryption Web Service (using SAML)";
    public static final String CXF_SAML_TOKEN_ASYM_SIGN_ENCR_SERVICE = "This is WSSECFVT CXF Asymmetric Signature And Encryption Web Service (using SAML)";
    public static final String CXF_SAML_TOKEN_ASYNC_ENCR_SERVICE = "This is WSSECFVT CXF Asymmetric Encryption Web Service (using SAML)";
    public static final String CXF_SAML_TOKEN_WSS_TEMPLATE_SERVICE_2 = "This is WSSTemplateWebSvc2 Web Service";
    public static final String CXF_SAML_TOKEN_WSS_TEMPLATE_SERVICE_4 = "This is WSSTemplateWebSvc4 Web Service";
    public static final String CXF_SAML_TOKEN_WSS_TEMPLATE_SERVICE_6 = "This is WSSTemplateWebSvc6 Web Service";
    public static final String CXF_SAML_TOKEN_SERVICE_UNAUTHORIZED = "The security token could not be authenticated or authorized";
    public static final String CXF_SAML_TOKEN_SERVICE_HTTPS_NOT_USED = "HttpsToken could not be asserted: HttpURLConnection is not a HttpsURLConnection";
    public static final String CXF_SAML_TOKEN_SYM_SIGN_SERVICE_CLIENT_NOT_SIGN = "The received token does not match the signed supporting token requirement";
    public static final String CXF_SAML_TOKEN_SYM_ENCR_SERVICE_CLIENT_NOT_ENCR = "The received token does not match the encrypted supporting token requirement";
    public static final String CXF_SAML_TOKEN_SYM_SIGN_ENCR_SERVICE_CLIENT_NOT_SIGN_OR_ENCR = "The received token does not match the signed encrypted supporting token requirement";
    public static final String CXF_SAML_TOKEN_GENERAL_FAILURE_MSG = "General security error (SAML token security failure)";

    public static final String ASSERTION_TEXT_ONLY = "assertion_text_only";
    public static final String ASSERTION_ENCODED = "assertion_encoded";
    public static final String ASSERTION_COMPRESSED_ENCODED = "assertion_compressed_encoded";
    public static final String TOKEN_TEXT_ONLY = "token_text_only";
    // remove TEXT_ONLY from the list - it is not guarenteed to work and now that we're using Shibboleth, it puts new-lines in the SAMLResponse
    // This causes HTTPUrlConnection to blow up - If I strip those out, then the signature checks fail which means that all tests would have to be run
    // unsigned, ... It's better to just have 1 test that focusses on TEXT ONLY
    //    public static final String[] SAML_TOKEN_FORMATS = new String[] { ASSERTION_TEXT_ONLY, ASSERTION_ENCODED, ASSERTION_COMPRESSED_ENCODED };
    public static final String[] SAML_TOKEN_FORMATS = new String[] { ASSERTION_ENCODED, ASSERTION_COMPRESSED_ENCODED };

    public static final String HEADER_FORMAT_AUTHZ_NAME_EQUALS_VALUE = "auth_header_saml_with_equals";
    public static final String HEADER_FORMAT_AUTHZ_NAME_EQUALS_QUOTED_VALUE = "auth_header_saml_with_equals_and_quotes";
    public static final String HEADER_FORMAT_AUTHZ_NAME_SPACE_VALUE = "auth_header_saml_no_equals";
    public static final String HEADER_FORMAT_NAME_EQUALS_VALUE = "header_saml_with_equals";
    public static final String HEADER_FORMAT_PROPAGATE_TOKEN_STRING_TRUE = "propagate_token_string_true";
    public static final String HEADER_FORMAT_PROPAGATE_TOKEN_STRING_FALSE = "propagate_token_string_false";
    public static final String HEADER_FORMAT_PROPAGATE_TOKEN_BOOLEAN_TRUE = "propagate_token_boolean_true";
    public static final String HEADER_FORMAT_PROPAGATE_TOKEN_BOOLEAN_FALSE = "propagate_token_boolean_false";
    public static final String[] SAML_HEADER_FORMATS = new String[] { HEADER_FORMAT_AUTHZ_NAME_EQUALS_VALUE, HEADER_FORMAT_AUTHZ_NAME_EQUALS_QUOTED_VALUE,
                                                                      HEADER_FORMAT_AUTHZ_NAME_SPACE_VALUE,
                                                                      HEADER_FORMAT_NAME_EQUALS_VALUE };

    /* SAML IDPs */
    public static final String[] IDP_SERVER_LIST = { "localhost:8019:8029" };
    public static String[] ADFS_SERVERS = {};
    public static String[] SHIBBOLETH_SERVERS = { "localhost" };
    public static final String[][] IDP_FEDERATION_LISTS = {
                                                            { "WlpTfimIdp1", "WlpTfimIdp1", "WlpTfimIdp1", "WlpTfimIdp1", "sp1", "shibboleth" }, // Federation 1 for server1, server2, server3, ...
                                                            { "WlpTfimIdp2", "WlpTfimIdp2", "WlpTfimIdp2", "WlpTfimIdp2", "sp2", "shibboleth" }, // Federation 2 for server1, server2, server3, ...
                                                            { "WlpTfimIdp3", "WlpTfimIdp3", "WlpTfimIdp3", "WlpTfimIdp3", "defaultSP", "shibboleth" }, // Federation 3 for server1, server2, server3, ...
                                                            { "WlpTfimIdp4", "WlpTfimIdp4", "WlpTfimIdp4", "WlpTfimIdp4", "sp1", "shibboleth" }, // Federation 4 for server1, server2, server3, ...
                                                            { "WlpTfimIdp5", "WlpTfimIdp5", "WlpTfimIdp5", "WlpTfimIdp5", "sp2", "shibboleth" }, // Federation 5 for server1, server2, server3, ...
                                                            { "WlpTfimIdp6", "WlpTfimIdp6", "WlpTfimIdp6", "WlpTfimIdp6", "defaultSP", "shibboleth" }, // Federation 6 for server1, server2, server3, ...
                                                            { null, null, null } // Federation x for server1, server2, ... (placeholder if we won't implement 3
    };

    public static final String[] SAML_SUPPORTED_PORTS = { "8020", "8021", "8022", "8023", "8024", "8025", "8026", "8027", "8028", "8029" };
    public static final String TFIM_SAML_ERROR_OCCURRED = "A SAML error has occurred";
    public static final String TFIM_REGISTRY_REALM = "customRealm";
    public static final String TFIM_TEST_GROUP = "group:customRealm/users";

    /* ****************** Misc Time Settings ************************* */
    public static final Boolean ADD_TIME = true;
    public static final Boolean SUBTRACT_TIME = false;
    public static final Boolean USE_CURRENT_TIME = true;
    public static final Boolean DO_NOT_USE_CURRENT_TIME = false;

    /* ********************* Server variables ********************* */
    public static final String LOGIN_PAGE_URL = "https://localhost:${bvt.prop.HTTP_default.secure}/samlclient/testIDPClient.jsp";

    /* ********************* Misc Settings ************************ */
    public static final String SERVLET_31 = "servlet31";
    public static final String SERVLET_40 = "servlet40";
    public static final String EXAMPLE_CALLBACK = "com.ibm.ws.wssecurity.example.cbh_1.0.0";
    public static final String EXAMPLE_CALLBACK_FEATURE = "wsseccbh-1.0";
    //issue 17687
    public static final String EXAMPLE_CALLBACK_WSS4J = "com.ibm.ws.wssecurity.example.cbhwss4j";
    public static final String EXAMPLE_CALLBACK_FEATURE_WSS4J = "wsseccbh-2.0";
}
