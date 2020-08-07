/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.utils;

public class Constants {

    public static final String UTF_8 = "UTF-8";

    public static final String SIGNATURE_ALG_HS256 = "HS256";
    public static final String SIGNATURE_ALG_RS256 = "RS256";
    public static final String SIGNATURE_ALG_ES256 = "ES256";

    public static final String SIGNING_KEY_X509 = "x509";
    public static final String SIGNING_KEY_JWK = "jwk";
    public static final String SIGNING_KEY_SECRET = "shared secret";

    public static final String ALL_AUDIENCES = "ALL_AUDIENCES";
    public static final String ALL_ISSUERS = "ALL_ISSUERS";

}
