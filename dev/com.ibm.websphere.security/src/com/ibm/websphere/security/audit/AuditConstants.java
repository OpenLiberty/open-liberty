/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.audit;

import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class AuditConstants {

    static public final String MAX_FILES = "maxFiles";
    static public final String MAX_FILE_SIZE = "maxFileSize";
    static public final String ENCRYPT = "encrypt";
    static public final String SIGN = "sign";
    static public final String ENCRYPT_ALIAS = "encryptAlias";
    static public final String ENCRYPT_KEYSTORE_REF = "encryptKeyStoreRef";
    static public final String SIGNER_ALIAS = "signerAlias";
    static public final String SIGNER_KEYSTORE_REF = "signerKeyStoreRef";
    static public final String SIGNING_ALIAS = "signingAlias";
    static public final String SIGNING_KEYSTORE_REF = "signingKeyStoreRef";
    static public final String WRAP_BEHAVIOR = "wrapBehavior";
    static public final String LOG_DIRECTORY = "logDirectory";
    static public final String EVENTS = "events";
    static public final String COMPACT = "compact";

    static public final String EVENT_NAME = "eventName";
    static public final String IS_CUSTOM_EVENT = "isCustomEvent";
    static public final String AUDIT_DATA = "auditData";
    static public final String OUTCOME = "outcome";
    static public final String EVENT_SEQUENCE_NUMBER = "eventSequenceNumber";
    static public final String LOGGING_SEQUENCE_NUMBER = "loggingSequenceNumber";

    static public final String CONFIG_SNAPSHOT = "CONFIG_SNAPSHOT";
    static public final String SECURITY_AUDIT_MGMT = "SECURITY_AUDIT_MGMT";
    static public final String SECURITY_MEMBER_MGMT = "SECURITY_MEMBER_MGMT";
    static public final String SECURITY_SERVICE_MGMT = "SECURITY_SERVICE_MGMT";
    static public final String SECURITY_SESSION_LOGIN = "SECURITY_SESSION_LOGIN";
    static public final String SECURITY_SESSION_LOGOUT = "SECURITY_SESSION_LOGOUT";
    static public final String SECURITY_SESSION_EXPIRY = "SECURITY_SESSION_EXPIRY";
    static public final String SECURITY_API_AUTHN = "SECURITY_API_AUTHN";
    static public final String SECURITY_API_AUTHN_TERMINATE = "SECURITY_API_AUTHN_TERMINATE";
    static public final String SECURITY_ROLE_MAPPING = "SECURITY_ROLE_MAPPING";
    static public final String SECURITY_AUTHN = "SECURITY_AUTHN";
    static public final String SECURITY_AUTHN_DELEGATION = "SECURITY_AUTHN_DELEGATION";
    static public final String SECURITY_AUTHZ_DELEGATION = "SECURITY_AUTHZ_DELEGATION";
    static public final String SECURITY_AUTHN_TERMINATE = "SECURITY_AUTHN_TERMINATE";
    static public final String SECURITY_AUTHN_FAILOVER = "SECURITY_AUTHN_FAILOVER";
    static public final String SECURITY_AUTHZ = "SECURITY_AUTHZ";
    static public final String SECURITY_SIGNING = "SECURITY_SIGNING";
    static public final String SECURITY_ENCRYPTION = "SECURITY_ENCRYPTION";
    static public final String SECURITY_RESOURCE_ACCESS = "SECURITY_RESOURCE_ACCESS";
    static public final String SECURITY_MGMT_KEY = "SECURITY_MGMT_KEY";
    static public final String SECURITY_RUNTIME_KEY = "SECURITY_RUNTIME_KEY";
    static public final String SECURITY_JMS_AUTHN = "SECURITY_JMS_AUTHN";
    static public final String SECURITY_JMS_AUTHZ = "SECURITY_JMS_AUTHZ";
    static public final String SECURITY_JMS_AUTHN_TERMINATE = "SECURITY_JMS_AUTHN_TERMINATE";
    static public final String SECURITY_JMS_CLOSED_CONNECTION = "SECURITY_JMS_CLOSED_CONNECTION";
    static public final String SECURITY_REST_HANDLER_AUTHZ = "SECURITY_REST_HANDLER_AUTHZ";
    static public final String SECURITY_SAF_AUTHZ = "SECURITY_SAF_AUTHZ";
    static public final String SECURITY_SAF_AUTHZ_DETAILS = "SECURITY_SAF_AUTHZ_DETAILS";
    static public final String JMX_MBEAN = "JMX_MBEAN";
    static public final String JMX_NOTIFICATION = "JMX_NOTIFICATION";
    static public final String JMX_MBEAN_ATTRIBUTES = "JMX_MBEAN_ATTRIBUTES";
    static public final String JMX_MBEAN_REGISTER = "JMX_MBEAN_REGISTER";
    static public final String JMS = "JMS";
    static public final String APPLICATION_TOKEN_MANAGEMENT = "APPLICATION_TOKEN_MANAGEMENT";
    static public final String CUSTOM = "CUSTOM";

    static public final String IS_ADMIN = "isAdmin";
    static public final String USER = "user";
    static public final String TARGET_USER = "targetUser";
    static public final String CLIENT_ID = "clientId";
    static public final String APP_OR_TOKEN_ID = "appOrTokenId";
    static public final String APPLICATION_ID = "applicationId";
    static public final String TOKEN_ID = "tokenId";
    static public final String IS_APP_PASSWORD_REQUEST = "isAppPasswordRequest";
    static public final String PROVIDER = "provider";
    static public final String REQUEST = "request";
    static public final String RESPONSE = "response";
    static public final String ENDPOINT = "endpoint";
    static public final String CREDENTIAL_TYPE = "credentialType";
    static public final String AUDIT_OUTCOME = "auditOutcome";
    static public final String DETAILED_ERROR = "detaileError";
    static public final String AUTH_RESULT = "authResult";
    static public final String INITIATOR_ROLE = "initiatorRole";
    static public final String NUMBER_REVOKED = "numberRevoked";
    static public final String APPNAME = "appName";
    static public final String JWT = "JWT";
    static public final String ACCESS_TOKEN = "AccessToken";
    static public final String TOKEN_MANAGER = "tokenManager";
    static public final String CLIENT_MANAGER = "clientManager";
    static public final String AUTHENTICATED = "authenticated";
    static public final String RESULT = "result";
    static public final String BAD_REQUEST_UNSUPPORTED_METHOD = "bad request: unsupported HTTP method for endpoint";
    static public final String INVALID_CLIENT = "invalid client";
    static public final String BAD_REQUEST = "bad request";
    static public final String INVALID_REQUEST = "invalid request";
    static public final String INTERNAL_ERROR = "internal error";
    static public final String FAILURE_TO_RETURN_REQUEST = "failure to return request";

    static public final List<String> validEventNamesList = Arrays.asList("CONFIG_SNAPSHOT", "SECURITY_AUDIT_MGMT", "SECURITY_MEMBER_MGMT", "SECURITY_SERVICE_MGMT",
                                                                         "SECURITY_SESSION_LOGIN",
                                                                         "SECURITY_SESSION_LOGOUT", "SECURITY_SESSION_EXPIRY", "SECURITY_API_AUTHN",
                                                                         "SECURITY_API_AUTHN_TERMINATE", "SECURITY_ROLE_MAPPING", "SECURITY_AUTHN",
                                                                         "SECURITY_AUTHN_FAILOVER", "SECURITY_AUTHN_DELEGATION", "SECURITY_AUTHZ_DELEGATION",
                                                                         "SECURITY_AUTHN_TERMINATE", "SECURITY_AUTHZ",
                                                                         "SECURITY_SIGNING", "SECURITY_ENCRYPTION", "SECURITY_RESOURCE_ACCESS", "SECURITY_MGMT_KEY",
                                                                         "SECURITY_RUNTIME_KEY", "SECURITY_JMS_AUTHN", "SECURITY_JMS_AUTHZ", "SECURITY_JMS_AUTHN_TERMINATE",
                                                                         "SECURITY_JMS_CLOSED_CONNECTION",
                                                                         "JMX_MBEAN", "JMX_NOTIFICATION", "JMX_MBEAN_ATTRIBUTES", "JMX_MBEAN_REGISTER", "JMS",
                                                                         "SECURITY_SAF_AUTHZ_DETAILS", "SECURITY_SAF_AUTHZ", "APPLICATION_TOKEN_MANAGEMENT", "CUSTOM");

    static public final String SUCCESS = "success";
    static public final String FAILURE = "failure";
    static public final String DENIED = "denied";
    static public final String ERROR = "error";
    static public final String WARNING = "warning";
    static public final String INFO = "info";
    static public final String REDIRECT = "redirect";
    static public final String CHALLENGE = "challenge";

    static public final List<String> validOutcomesList = Arrays.asList("SUCCESS", "FAILURE", "DENIED", "ERROR", "WARNING", "INFO", "REDIRECT", "CHALLENGE");

}
