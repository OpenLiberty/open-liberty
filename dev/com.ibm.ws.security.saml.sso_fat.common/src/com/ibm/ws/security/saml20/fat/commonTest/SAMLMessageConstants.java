/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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

import com.ibm.ws.security.fat.common.MessageConstants;

public class SAMLMessageConstants extends MessageConstants {

    public static final String CWWKS5000I_SAML_CONFIG_PROCESSED = "CWWKS5000I";
    public static final String CWWKS5002I_SAML_SERVICE_ACTIVATED = "CWWKS5002I";
    public static final String CWWKS5003E_ENDPOINT_NOT_SUPPORTED = "CWWKS5003E";
    public static final String CWWKS5004E_SP_NOT_CONFIGURED = "CWWKS5004E";
    public static final String CWWKS5006E_SP_ID_EMPTY = "CWWKS5006E";
    public static final String CWWKS5007E_INTERNAL_SERVER_ERROR = "CWWKS5007E";
    public static final String CWWKS5008E_STATUS_CODE_NOT_SUCCESS = "CWWKS5008E";

    public static final String CWWKS5011E_ISSUE_INSTANT_OUT_OF_RANGE = "CWWKS5011E";
    public static final String CWWKS5013E_MISSING_SAML_ASSERTION_ERROR = "CWWKS5013E: The header named as ";
    public static final String CWWKS5014W_INVALID_URL = "CWWKS5014W";
    public static final String CWWKS5018E_SAML_RESPONSE_CANNOT_BE_DECODED = "CWWKS5018E";

    public static final String CWWKS5021E_IDP_METADATA_MISSING_ISSUER = "CWWKS5021E";
    public static final String CWWKS5023E_IDP_METADATA_NOT_VALID = "CWWKS5023E";
    public static final String CWWKS5025E_IDP_METADATA_DOES_NOT_EXIST = "CWWKS5025E";
    public static final String CWWKS5029E_RELAY_STATE_NOT_RECOGNIZED = "CWWKS5029E";

    public static final String CWWKS5041E_RELAY_STATE_PARAM_MISSING = "CWWKS5041E";
    public static final String CWWKS5045E_INVALID_ISSUER = "CWWKS5045E";
    public static final String CWWKS5048E_ERROR_VERIFYING_SIGNATURE = "CWWKS5048E";
    public static final String CWWKS5049E_SIGNATURE_NOT_TRUSTED_OR_VALID = "CWWKS5049E";
    public static final String CWWKS5023E_NO_IDPSSODESCRIPTOR = "CWWKS5023E";

    public static final String CWWKS5053E_NOT_ON_OR_AFTER_OUT_OF_RANGE = "CWWKS5053E";
    public static final String CWWKS5057E_NOT_BEFORE_OUT_OF_RANGE = "CWWKS5057E";

    public static final String CWWKS5060E_AUDIENCE_NOT_VALID = "CWWKS5060E";
    public static final String CWWKS5062E_SESSION_NOT_ON_OR_AFTER_OUT_OF_RANGE = "CWWKS5062E";
    public static final String CWWKS5063E_SAML_END_USER_ERROR_MESSAGE = "CWWKS5063E";
    public static final String CWWKS5067E_INVALID_IN_RESPONSE_TO = "CWWKS5067E";
    public static final String CWWKS5068E_MISSING_ATTRIBUTE = "CWWKS5068E";

    public static final String CWWKS5072E_AUTHN_UNSUCCESSFUL = "CWWKS5072E";
    public static final String CWWKS5073E_CANNOT_FIND_PRIVATE_KEY = "CWWKS5073E";
    public static final String CWWKS5076E_USERCREDENTIALRESOLVER_FAILED = "CWWKS5076E";
    public static final String CWWKS5077E_CANNOT_SELECT_SAML_PROVIDER = "CWWKS5077E";
    public static final String CWWKS5079E_CANNOT_FIND_IDP_URL_IN_METATDATA = "CWWKS5079E";

    public static final String CWWKS5080E_IDP_MEDATA_MISSING_IN_CONFIG = "CWWKS5080E";
    public static final String CWWKS5081E_SAML_REQUEST_EXPIRED = "CWWKS5081E";
    public static final String CWWKS5082E_ASSERTION_ALREADY_PROCESSED = "CWWKS5082E";
    public static final String CWWKS5083E_HTTPS_REQUIRED_FOR_REQUEST = "CWWKS5083E";

    public static final String CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES = "CWWKS5207W";
    public static final String CWWKS5208E_SAML_ASSERTION_NOT_VALID = "CWWKS5208E";
    public static final String CWWKS5215E_NO_AVAILABLE_SP = "CWWKS5215E";
    public static final String CWWKS5214E_LOGOUT_ENDPOINT_MISSING = "CWWKS5214E";
    public static final String CWWKS5218E_USER_SESSION_NOT_FOUND = "CWWKS5218E";

    public static final String CWWKS5251W_SAML_TOKEN_NOT_IN_SUBJECT = "CWWKS5251W";

    // Non-SAML messages
    public static final String CWPKI0033E_KEYSTORE_DID_NOT_LOAD = "CWPKI0033E";
    public static final String CWPKI0807W_KEYSTORE_NOT_FOUND = "CWPKI0807W";
    public static final String CWPKI0809W_FAILURE_LOADING_KEYSTORE = "CWPKI0809W";
    public static final String CWPKI0812E_ERROR_GETTING_KEY = "CWPKI0812E";

    public static final String CWWKS4358I_AUTH_FILTER_PROCESSED = "CWWKS4358I: The authentication filter ";
    public static final String CWWKS3005E_NO_USER_REGISTRY = "CWWKS3005E";
    public static final String CWWKG0033W_REF_VALUE_NOT_FOUND_IN_CONFIG = "CWWKG0033W";

    public static final String CWWKW0210E_CANNOT_CREATE_SUBJECT = "CWWKW0210E";
    public static final String CWWKW0217E_CLOCK_SKEW_ERROR = "CWWKW0217E";
    public static final String CWWKW0228E_SAML_ASSERTION_MISSING = "CWWKW0228E";
    public static final String CWWKW0232E_CANNOT_CREATE_SUBJECT_FOR_USER = "CWWKW0232E";
    public static final String CWWKS3107W_GROUP_USER_MISMATCH = "CWWKS3107W";

    public static final String CWWKO0801E_HTTPS_REQUIRED = "CWWKO0801E";
    public static final String CWWKF0001E_FEATURE_MISSING = "CWWKF0001E";

    public static final String SRVE0190E_FILE_NOT_FOUND = "SRVE0190E";

    public static final String CWWKG0101W_CONFIG_NOT_VISIBLE_TO_OTHER_BUNDLES = "CWWKG0101W";
}
