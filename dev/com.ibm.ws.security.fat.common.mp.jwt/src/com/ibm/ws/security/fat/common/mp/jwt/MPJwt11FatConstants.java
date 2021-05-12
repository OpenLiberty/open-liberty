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
package com.ibm.ws.security.fat.common.mp.jwt;

public class MPJwt11FatConstants extends MPJwtFatConstants {

    /* root Contexts */
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

    /* App class names for various signature algorithms */
    public static final String GOOD_RS256_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodRS256PublicKeyInMP-ConfigInMETA-INF";
    public static final String GOOD_RS256_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodRS256PublicKeyInMP-ConfigUnderWEB-INF";
    public static final String GOOD_RS384_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodRS384PublicKeyInMP-ConfigInMETA-INF";
    public static final String GOOD_RS384_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodRS384PublicKeyInMP-ConfigUnderWEB-INF";
    public static final String GOOD_RS512_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodRS512PublicKeyInMP-ConfigInMETA-INF";
    public static final String GOOD_RS512_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodRS512PublicKeyInMP-ConfigUnderWEB-INF";

    public static final String GOOD_ES256_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodES256PublicKeyInMP-ConfigInMETA-INF";
    public static final String GOOD_ES256_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodES256PublicKeyInMP-ConfigUnderWEB-INF";
    public static final String GOOD_ES384_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodES384PublicKeyInMP-ConfigInMETA-INF";
    public static final String GOOD_ES384_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodES384PublicKeyInMP-ConfigUnderWEB-INF";
    public static final String GOOD_ES512_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodES512PublicKeyInMP-ConfigInMETA-INF";
    public static final String GOOD_ES512_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodES512PublicKeyInMP-ConfigUnderWEB-INF";

    public static final String GOOD_PS256_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodPS256PublicKeyInMP-ConfigInMETA-INF";
    public static final String GOOD_PS256_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodPS256PublicKeyInMP-ConfigUnderWEB-INF";
    public static final String GOOD_PS384_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodPS384PublicKeyInMP-ConfigInMETA-INF";
    public static final String GOOD_PS384_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodPS384PublicKeyInMP-ConfigUnderWEB-INF";
    public static final String GOOD_PS512_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodPS512PublicKeyInMP-ConfigInMETA-INF";
    public static final String GOOD_PS512_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodPS512PublicKeyInMP-ConfigUnderWEB-INF";

    public static final String GOOD_RELATIVE_ES512_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodRelativeES512KeyLocationInMP-ConfigInMETA-INF";
    public static final String GOOD_FILE_RS256_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodFileRS256KeyLocationInMP-ConfigInMETA-INF";
    public static final String GOOD_FILE_PS384_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodFilePS384KeyLocationInMP-ConfigInMETA-INF";
    public static final String GOOD_URL_RS384_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodUrlRS384KeyLocationInMP-ConfigInMETA-INF";
    public static final String GOOD_JWKSURI_PS256_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodJwksuriPS256KeyLocationInMP-ConfigInMETA-INF";
    public static final String GOOD_JWKSURI_RS512_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileGoodJwksuriRS512KeyLocationInMP-ConfigInMETA-INF";

    public static final String GOOD_RELATIVE_RS256_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodRelativeRS256KeyLocationInMP-ConfigUnderWEB-INF";
    public static final String GOOD_RELATIVE_PS384_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodRelativePS384KeyLocationInMP-ConfigUnderWEB-INF";
    public static final String GOOD_FILE_ES512_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodFileES512KeyLocationInMP-ConfigUnderWEB-INF";
    public static final String GOOD_URL_ES256_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodUrlES256KeyLocationInMP-ConfigUnderWEB-INF";
    public static final String GOOD_URL_PS512_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodUrlPS512KeyLocationInMP-ConfigUnderWEB-INF";
    public static final String GOOD_JWKSURI_ES384_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileGoodJwksuriES384KeyLocationInMP-ConfigUnderWEB-INF";

    public static final String PROPAGATE_TOKEN_STRING_TRUE = "propagate_token_string_true";
    public static final String PROPAGATE_TOKEN_BOOLEAN_TRUE = "propagate_token_boolean_true";
    public static final String PROPAGATE_TOKEN_STRING_FALSE = "propagate_token_string__false";
    public static final String PROPAGATE_TOKEN_BOOLEAN_FALSE = "propagate_token_boolean_false";

}