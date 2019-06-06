/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.jwt;

public class JwtMessageConstants extends com.ibm.ws.security.fat.common.MessageConstants {

    public static final String CWWKS6007E_BAD_KEY_ALIAS = "CWWKS6007E";

    public static final String CWWKS6022E_ISSUER_NOT_TRUSTED = "CWWKS6022E";
    public static final String CWWKS6023E_BAD_AUDIENCE = "CWWKS6023E";
    public static final String CWWKS6024E_IAT_AFTER_EXP = "CWWKS6024E";
    public static final String CWWKS6025E_TOKEN_EXPIRED = "CWWKS6025E";
    public static final String CWWKS6026E_FUTURE_NBF = "CWWKS6026E";

    public static final String CWWKS6028E_BAD_ALGORITHM = "CWWKS6028E";
    public static final String CWWKS6029E_NO_SIGNING_KEY = "CWWKS6029E";
    public static final String CWWKS6030E_JWT_CONSUMER_ID_DOESNT_EXIST = "CWWKS6030E";
    public static final String CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING = "CWWKS6031E";
    public static final String CWWKS6032E_JWT_CONSUMER_SHARED_KEY_NOT_RETRIEVED = "CWWKS6032E";
    public static final String CWWKS6033E_JWT_CONSUMER_PUBLIC_KEY_NOT_RETRIEVED = "CWWKS6033E";
    public static final String CWWKS6034E_JWT_CONSUMER_SHARED_KEY_NOT_FOUND = "CWWKS6034E";

    public static final String CWWKS6040E_JWT_STRING_EMPTY = "CWWKS6040E";
    public static final String CWWKS6041E_JWT_SIGNATURE_INVALID = "CWWKS6041E";

    public static final String CWWKS6043E_MALFORMED_CLAIM = "CWWKS6043E";
    public static final String CWWKS6044E_IAT_AFTER_CURRENT_TIME = "CWWKS6044E";
    public static final String CWWKS6045E_JTI_REUSED = "CWWKS6045E";

    public static final String CWWKS6047E_MULTIKEY_NO_ALIAS = "CWWKS6047E";

    public static final String CWWKS6052E_JWT_TRUSTED_ISSUERS_NULL = "CWWKS6052E";

}
