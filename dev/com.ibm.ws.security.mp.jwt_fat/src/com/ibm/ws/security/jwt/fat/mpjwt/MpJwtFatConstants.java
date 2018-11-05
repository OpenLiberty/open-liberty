/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.mpjwt;

import com.ibm.ws.security.fat.common.jwt.JwtConstants;

public class MpJwtFatConstants extends JwtConstants {

    public static final String MPJWT_GENERIC_APP_NAME = "MicroProfileApp";

    /* Servers */
    public static String MPJWTBuilderServerName = "com.ibm.ws.security.jwt_fat.mpjwt.builder";
    public static String MPJWTServerName = "com.ibm.ws.security.jwt_fat.mpjwt";
    public static String MPJWTClientName = "com.ibm.ws.security.jwt_fat.mpjwt.client";
    public static String MPJWTServerWithJVMOptionsName = "com.ibm.ws.security.jwt_fat.mpjwt.jvmOptions";

    /* root Contexts */
    public static final String MICROPROFILE_SERVLET = "microProfileApp";
    public static final String COMPLEX_BUILDER_APP_ROOT_CONTEXT = "jwtbuilderclient";
    public static final String LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_BASIC_IN_APP_ROOT_CONTEXT = "microProfileLoginConfig_FormLoginInWebXml_BasicInApp";
    public static final String LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT = "microProfileLoginConfig_FormLoginInWebXml_MpJwtInApp";
    public static final String LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_NOT_IN_APP_ROOT_CONTEXT = "microProfileLoginConfig_FormLoginInWebXml_NotInApp";
    public static final String LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_BASIC_IN_APP_ROOT_CONTEXT = "microProfileLoginConfig_MpJwtInWebXml_BasicInApp";
    public static final String LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT = "microProfileLoginConfig_MpJwtInWebXml_MpJwtInApp";
    public static final String LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_NOT_IN_APP_ROOT_CONTEXT = "microProfileLoginConfig_MpJwtInWebXml_NotInApp";
    public static final String LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_BASIC_IN_APP_ROOT_CONTEXT = "microProfileLoginConfig_NotInWebXml_BasicInApp";
    public static final String LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT = "microProfileLoginConfig_NotInWebXml_MpJwtInApp";
    public static final String LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_NOT_IN_APP_ROOT_CONTEXT = "microProfileLoginConfig_NotInWebXml_NotInApp";
    public static final String LOGINCONFIG_MULTI_LAYER_NOT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT = "microProfileLoginConfig_MultiLayer_NotInWebXml_MpJwtInApp";
    public static final String LOGINCONFIG_PROPAGATION_ROOT_CONTEXT = "microProfilePropagationClient";

    /* test App Names */
    public static final String MPJWT_APP_SEC_CONTEXT_REQUEST_SCOPE = "SecurityContextRequestScoped";
    public static final String MPJWT_APP_CLASS_SEC_CONTEXT_REQUEST_SCOPE = "com.ibm.ws.jaxrs.fat.microProfileApp.SecurityContext.RequestScoped.MicroProfileApp";
    public static final String MPJWT_APP_SEC_CONTEXT_APP_SCOPE = "SecurityContextApplicationScoped";
    public static final String MPJWT_APP_CLASS_SEC_CONTEXT_APP_SCOPE = "com.ibm.ws.jaxrs.fat.microProfileApp.SecurityContext.ApplicationScoped.MicroProfileApp";
    public static final String MPJWT_APP_SEC_CONTEXT_SESSION_SCOPE = "SecurityContextSessionScoped";
    public static final String MPJWT_APP_CLASS_SEC_CONTEXT_SESSION_SCOPE = "com.ibm.ws.jaxrs.fat.microProfileApp.SecurityContext.SessionScoped.MicroProfileApp";
    public static final String MPJWT_APP_SEC_CONTEXT_NO_SCOPE = "SecurityContextNotScoped";
    public static final String MPJWT_APP_CLASS_SEC_CONTEXT_NO_SCOPE = "com.ibm.ws.jaxrs.fat.microProfileApp.SecurityContext.NotScoped.MicroProfileApp";

    public static final String MPJWT_APP_TOKEN_INJECT_REQUEST_SCOPE = "InjectionRequestScoped";
    public static final String MPJWT_APP_CLASS_TOKEN_INJECT_REQUEST_SCOPE = "com.ibm.ws.jaxrs.fat.microProfileApp.Injection.RequestScoped.MicroProfileApp";
    public static final String MPJWT_APP_TOKEN_INJECT_APP_SCOPE = "InjectionApplicationScoped";
    public static final String MPJWT_APP_CLASS_TOKEN_INJECT_APP_SCOPE = "com.ibm.ws.jaxrs.fat.microProfileApp.Injection.ApplicationScoped.MicroProfileApp";
    public static final String MPJWT_APP_TOKEN_INJECT_SESSION_SCOPE = "InjectionSessionScoped";
    public static final String MPJWT_APP_CLASS_TOKEN_INJECT_SESSION_SCOPE = "com.ibm.ws.jaxrs.fat.microProfileApp.Injection.SessionScoped.MicroProfileApp";
    public static final String MPJWT_APP_TOKEN_INJECT_NO_SCOPE = "InjectionNotScoped";
    public static final String MPJWT_APP_CLASS_TOKEN_INJECT_NO_SCOPE = "com.ibm.ws.jaxrs.fat.microProfileApp.Injection.NotScoped.MicroProfileApp";

    public static final String MPJWT_APP_CLAIM_INJECT_REQUEST_SCOPE = "ClaimInjectionRequestScoped";
    public static final String MPJWT_APP_CLASS_CLAIM_INJECT_REQUEST_SCOPE = "com.ibm.ws.jaxrs.fat.microProfileApp.ClaimInjection.RequestScoped.MicroProfileApp";
    public static final String MPJWT_APP_CLAIM_INJECT_APP_SCOPE = "ClaimInjectionApplicationScopedInstance";
    public static final String MPJWT_APP_CLASS_CLAIM_INJECT_APP_SCOPE = "com.ibm.ws.jaxrs.fat.microProfileApp.ClaimInjection.ApplicationScoped.Instance.MicroProfileApp";
    public static final String MPJWT_APP_CLAIM_INJECT_SESSION_SCOPE = "ClaimInjectionSessionScopedInstance";
    public static final String MPJWT_APP_CLASS_CLAIM_INJECT_SESSION_SCOPE = "com.ibm.ws.jaxrs.fat.microProfileApp.ClaimInjection.SessionScoped.Instance.MicroProfileApp";
    public static final String MPJWT_APP_CLAIM_INJECT_NO_SCOPE = "ClaimInjectionNotScoped";
    public static final String MPJWT_APP_CLASS_CLAIM_INJECT_NO_SCOPE = "com.ibm.ws.jaxrs.fat.microProfileApp.ClaimInjection.NotScoped.MicroProfileAp";

    public static final String LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_BASIC_IN_APP = "loginConfig_FormLoginInWebXml_BasicInApp";
    public static final String LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_MP_JWT_IN_APP = "loginConfig_FormLoginInWebXml_MpJwtInApp";
    public static final String LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_NOT_IN_APP = "loginConfig_FormLoginInWebXml_NotInApp";
    public static final String LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_BASIC_IN_APP = "loginConfig_MpJwtInWebXml_BasicInApp";
    public static final String LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP = "loginConfig_MpJwtInWebXml_MpJwtInApp";
    public static final String LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_NOT_IN_APP = "loginConfig_MpJwtInWebXml_NotInApp";
    public static final String LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_BASIC_IN_APP = "loginConfig_NotInWebXml_BasicInApp";
    public static final String LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP = "loginConfig_NotInWebXml_MpJwtInApp";
    public static final String LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_NOT_IN_APP = "loginConfig_NotInWebXml_NotInApp";
    public static final String LOGINCONFIG_MULTI_LAYER_NOT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP = "loginConfig_MultiLayer_NotInWebXml_MpJwtInApp";
    public static final String LOGINCONFIG_PROPAGATION = "propagationClient";

    public static final String GOOD_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodMP-ConfigInMETA-INF";
    //    public static final String GOOD_CONFIG_IN_META_INF_TREE_APP = "microProfileGoodMPConfigInMetaInf";
    //    public static final String MPJWT_APP_CLASS_GOOD_CONFIG_IN_META_INF = "com.ibm.ws.jaxrs.fat.microProfileApp.microProfileGoodMPConfigInMetaInf.MicroProfileApp";
    public static final String GOOD_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodMP-ConfigUnderWEB-INF";
    //    public static final String GOOD_CONFIG_UNDER_WEB_INF_TREE_APP = "microProfileGoodMPConfigUnderWebInf";
    //    public static final String MPJWT_APP_CLASS_GOOD_CONFIG_UNDER_WEB_INF = "com.ibm.ws.jaxrs.fat.microProfileApp.microProfileGoodMPConfigUnderWebInf.MicroProfileApp";

    public static final String MP_CONFIG_IN_META_INF_TREE_APP = "microProfileMPConfigInMetaInf";
    public static final String MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF = "com.ibm.ws.jaxrs.fat.microProfileApp.microProfileMPConfigInMetaInf.MicroProfileApp";
    public static final String MP_CONFIG_UNDER_WEB_INF_TREE_APP = "microProfileMPConfigUnderWebInf";
    public static final String MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF = "com.ibm.ws.jaxrs.fat.microProfileApp.microProfileMPConfigUnderWebInf.MicroProfileApp";

    public static final String BAD_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileBadMP-ConfigInMETA-INF";
    public static final String BAD_CONFIG_IN_META_INF_TREE_APP = "microProfileBadMPConfigInMetaInf";
    public static final String MPJWT_APP_CLASS_BAD_CONFIG_IN_META_INF = "com.ibm.ws.jaxrs.fat.microProfileApp.microProfileBadMPConfigInMetaInf.MicroProfileApp";
    public static final String BAD_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileBadMP-ConfigUnderWEB-INF";
    public static final String BAD_CONFIG_UNDER_WEB_INF_TREE_APP = "microProfileBadMPConfigUnderWebInf";
    public static final String MPJWT_APP_CLASS_BAD_CONFIG_UNDER_WEB_INF = "com.ibm.ws.jaxrs.fat.microProfileApp.microProfileBadMPConfigUnderWebInf.MicroProfileApp";

    public static final String NO_MP_CONFIG_IN_APP_ROOT_CONTEXT = "microProfileMP-ConfigNotInApp";
    public static final String NO_MP_CONFIG_IN_APP_APP = "microProfileMPConfigNotInApp";
    public static final String MPJWT_APP_CLASS_NO_MP_CONFIG_IN_APP = "com.ibm.ws.jaxrs.fat.microProfileApp.microProfileMPConfigNotInApp.MicroProfileApp";

    public static final String GOOD_ISSUER_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodIssuerInMP-ConfigInMETA-INF";
    public static final String GOOD_ISSUER_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodIssuerInMP-ConfigUnderWEB-INF";
    public static final String GOOD_ISSUER_ONLY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodIssuerOnlyInMP-ConfigInMETA-INF";
    public static final String GOOD_ISSUER_ONLY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodIssuerOnlyInMP-ConfigUnderWEB-INF";
    public static final String BAD_ISSUER_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileBadIssuerInMP-ConfigInMETA-INF";
    public static final String BAD_ISSUER_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileBadIssuerInMP-ConfigUnderWEB-INF";
    public static final String BAD_ISSUER_ONLY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileBadIssuerOnlyInMP-ConfigInMETA-INF";
    public static final String BAD_ISSUER_ONLY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileBadIssuerOnlyInMP-ConfigUnderWEB-INF";

    public static final String GOOD_COMPLEX_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodComplexPublicKeyInMP-ConfigInMETA-INF";
    public static final String GOOD_SIMPLE_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodSimplePublicKeyInMP-ConfigInMETA-INF";
    public static final String GOOD_COMPLEX_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodComplexPublicKeyInMP-ConfigUnderWEB-INF";
    public static final String GOOD_SIMPLE_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodSimplePublicKeyInMP-ConfigUnderWEB-INF";
    public static final String BAD_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileBadPublicKeyInMP-ConfigInMETA-INF";
    public static final String BAD_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileBadPublicKeyInMP-ConfigUnderWEB-INF";

    public static final String GOOD_RELATIVE_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodRelativeKeyLocationInMP-ConfigInMETA-INF";
    public static final String GOOD_RELATIVE_COMPLEX_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodRelativeComplexKeyLocationInMP-ConfigInMETA-INF";
    public static final String GOOD_FILE_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodFileKeyLocationInMP-ConfigInMETA-INF";
    public static final String GOOD_URL_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodUrlKeyLocationInMP-ConfigInMETA-INF";
    public static final String GOOD_JWKSURI_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodJwksuriKeyLocationInMP-ConfigInMETA-INF";
    public static final String GOOD_RELATIVE_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodRelativeKeyLocationInMP-ConfigUnderWEB-INF";
    public static final String GOOD_FILE_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodFileKeyLocationInMP-ConfigUnderWEB-INF";
    public static final String GOOD_FILE_COMPLEX_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodFileComplexKeyLocationInMP-ConfigUnderWEB-INF";
    public static final String GOOD_URL_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodUrlKeyLocationInMP-ConfigUnderWEB-INF";
    public static final String GOOD_JWKSURI_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodJwksuriKeyLocationInMP-ConfigUnderWEB-INF";

    public static final String BAD_FILE_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileBadFileKeyLocationInMP-ConfigInMETA-INF";
    public static final String BAD_URL_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileBadUrlKeyLocationInMP-ConfigInMETA-INF";
    public static final String BAD_RELATIVE_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileBadRelativeKeyLocationInMP-ConfigUnderWEB-INF";

    public static final String BAD_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileBadKeyLocationInMP-ConfigInMETA-INF";
    public static final String BAD_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileBadKeyLocationInMP-ConfigUnderWEB-INF";

    /* App class names (used for validation) */
    public static final String MPJWT_APP_CLASS_NO_LOGIN_CONFIG = "com.ibm.ws.jaxrs.fat.microProfileApp.MicroProfileLoginConfigNotInWebXmlNotInApp.MicroProfileApp";
    public static final String MPJWT_APP_CLASS_LOGIN_CONFIG_BASIC = "com.ibm.ws.jaxrs.fat.microProfileApp.MicroProfileLoginConfigNotInWebXmlBasicInApp.MicroProfileApp";
    public static final String MPJWT_APP_CLASS_LOGIN_CONFIG_MP_JWT = "com.ibm.ws.jaxrs.fat.microProfileApp.MicroProfileLoginConfigNotInWebXmlMPJWTInApp.MicroProfileApp";
    public static final String MPJWT_APP_CLASS_PROPAGATION_CLIENT = "com.ibm.ws.jaxrs.fat.microProfileApp.PropagationClient.MicroProfileApp";
    public static final String MPJWT_APP_CLASS_LOGIN_CONFIG_MPJWTNOTINWEBXML_MPJWTINAPP = "com.ibm.ws.jaxrs.fat.microProfileApp.MicroProfileLoginConfigNotInWebXmlMPJWTInApp.MicroProfileApp";
    public static final String MPJWT_APP_CLASS_LOGIN_CONFIG_FORMLOGININWEBXML_NOTINAPP = "com.ibm.ws.jaxrs.fat.microProfileApp.MicroProfileLoginConfigFormLoginInWebXmlNotInApp.MicroProfileApp";
    public static final String MPJWT_APP_CLASS_LOGIN_CONFIG_FORMLOGININWEBXML_BASICINAPP = "com.ibm.ws.jaxrs.fat.microProfileApp.MicroProfileLoginConfigFormLoginInWebXmlBasicInApp.MicroProfileApp";
    public static final String MPJWT_APP_CLASS_LOGIN_CONFIG_FORMLOGININWEBXML_MPJWTINAPP = "com.ibm.ws.jaxrs.fat.microProfileApp.MicroProfileLoginConfigFormLoginInWebXmlMPJWTInApp.MicroProfileApp";
    public static final String MPJWT_APP_CLASS_LOGIN_CONFIG_MPJWTINWEBXML_NOTINAPP = "com.ibm.ws.jaxrs.fat.microProfileApp.MicroProfileLoginConfigMpJwtInWebXmlNotInApp.MicroProfileApp";
    public static final String MPJWT_APP_CLASS_LOGIN_CONFIG_MPJWTINWEBXML_BASICINAPP = "com.ibm.ws.jaxrs.fat.microProfileApp.MicroProfileLoginConfigMpJwtInWebXmlBasicInApp.MicroProfileApp";
    public static final String MPJWT_APP_CLASS_LOGIN_CONFIG_MPJWTINWEBXML_MPJWTINAPP = "com.ibm.ws.jaxrs.fat.microProfileApp.MicroProfileLoginConfigMpJwtInWebXmlMPJWTInApp.MicroProfileApp";
    public static final String MPJWT_APP_CLASS_LOGIN_CONFIG_MULTI_LAYER_MPJWTNOTINWEBXML_MPJWTINAPP = "com.ibm.ws.jaxrs.fat.microProfileApp.MicroProfileLoginConfigMultiLayerNotInWebXmlMPJWTInApp.MicroProfileApp";

    /* Auth Types */
    public static final String AUTH_TYPE_BASIC = "BASIC";
    public static final String AUTH_TYPE_MPJWT = "MP-JWT";
    public static final String AUTH_TYPE_FORM = "FORM";

    /* Misc */
    public static final String EXECUTED_MSG_STRING = "Executed doWorker in ";
    public static final String CLIENT_SEND_TOKEN_PROPERTY = "com.ibm.ws.jaxrs.client.mpjwt.sendToken";

    public static final String PROPAGATE_TOKEN_STRING_TRUE = "propagate_token_string_true";
    public static final String PROPAGATE_TOKEN_BOOLEAN_TRUE = "propagate_token_boolean_true";
    public static final String PROPAGATE_TOKEN_STRING_FALSE = "propagate_token_string__false";
    public static final String PROPAGATE_TOKEN_BOOLEAN_FALSE = "propagate_token_boolean_false";

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
    public static final String JWT_BUILDER_TOKEN = "mpJwt_token: ";
    public static final String PAYLOAD_GROUPS = "groups";
    public static final String MP_JWT_TOKEN = "mpJwt_token";

    public static final String BOOTSTRAP_PROP_FAT_SERVER_HOSTNAME = "fat.server.hostname";
    public static final String BOOTSTRAP_PROP_FAT_SERVER_HOSTIP = "fat.server.hostip";

    public static final String BVT_SERVER_1_PORT_NAME_ROOT = "security_1_HTTP_default";
    public static final String BVT_SERVER_2_PORT_NAME_ROOT = "security_2_HTTP_default";
    public static final String BVT_SERVER_3_PORT_NAME_ROOT = "security_3_HTTP_default";
    public static final String BVT_SERVER_4_PORT_NAME_ROOT = "security_4_HTTP_default";

    public static final String TARGET_APP = "targetApp";
    public static final String WHERE = "where";

}