/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt11.fat.utils;

import com.ibm.ws.security.fat.common.MessageConstants;

public class MpJwtMessageConstants extends MessageConstants {

    //Unsupported JDK Expected Set of messages start
    public static final String MIN_JDK_FEATURE_REQUIREMENT = "CWWKF0032E";

    public static final String UNRESOLVED_BUNDLE = "CWWKE0702E";
    public static final String ANNOTATION_FIELD_WARNING = "CWNEN0047W";
    public static final String ANNOTATION_METHOD_WARNING = "CWNEN0049W";
    public static final String MICROPROFILE_APP_DID_NOT_START_EXCEPTION = "SRVE0271E";
    public static final String MICROPROFILE_INITIALIZATION_EXCEPTION = "SRVE0276E";
    public static final String MICROPROFILE_INITIALIZATION_EXCEPTION_2 = " SRVE0207E";
    public static final String SERVLET_DID_NOT_START = "SRVE0242I";
    public static final String MICROPROFILE_NOCLASSDEFFOUND = "CWNEN0048W";

    public static final String CWWKG0058E_CONFIG_MISSING_REQUIRED_ATTRIBUTE = "CWWKG0058E";

    public static final String CWWKS1106A_AUTHENTICATION_FAILED = "CWWKS1106A";

    public static final String CWWKS5506E_USERNAME_NOT_FOUND = "CWWKS5506E";
    public static final String CWWKS5508E_ERROR_CREATING_RESULT = "CWWKS5508E";
    public static final String CWWKS5519E_PRINCIPAL_MAPPING_MISSING_ATTR = "CWWKS5519E";
    public static final String CWWKS5522E_MPJWT_TOKEN_NOT_FOUND = "CWWKS5522E";
    public static final String CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ = "CWWKS5523E";
    public static final String CWWKS5524E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ = "CWWKS5524E";
    public static final String CWWKS5526W_WRONG_AUTH_TYPE = "CWWKS5526W";
    public static final String CWWKS5528W_BAD_HEADER_VALUE_IN_MP_CONFIG = "CWWKS5528W";

    public static final String CWWKS5603E_CLAIM_CANNOT_BE_INJECTED = "CWWKS5603E";

    public static final String CWWKS6007E_BAD_KEY_ALIAS = "CWWKS6007E";
    public static final String CWWKS6022E_ISSUER_NOT_TRUSTED = "CWWKS6022E";
    public static final String CWWKS6023E_AUDIENCE_NOT_TRUSTED = "CWWKS6023E";
    public static final String CWWKS6025E_TOKEN_EXPIRED = "CWWKS6025E";
    public static final String CWWKS6028E_SIG_ALG_MISMATCH = "CWWKS6028E";
    public static final String CWWKS6029E_SIGNING_KEY_CANNOT_BE_FOUND = "CWWKS6029E";
    public static final String CWWKS6031E_CAN_NOT_PROCESS_TOKEN = "CWWKS6031E";
    public static final String CWWKS6033E_JWT_CONSUMER_PUBLIC_KEY_NOT_RETRIEVED = "CWWKS6033E";
    public static final String CWWKS6041E_JWT_SIGNATURE_INVALID = "CWWKS6041E";
    public static final String CWWKS6045E_JTI_REUSED = "CWWKS6045E";
    public static final String CWWKS6055W_BETA_SIGNATURE_ALGORITHM_USED = "CWWKS6055W";

    public static final String CWWKW1001W_CDI_RESOURCE_SCOPE_MISMATCH = "CWWKW1001W";

    public static final String CWWKZ0002E_EXCEPTION_WHILE_STARTING_APP = "CWWKZ0002E";
}
