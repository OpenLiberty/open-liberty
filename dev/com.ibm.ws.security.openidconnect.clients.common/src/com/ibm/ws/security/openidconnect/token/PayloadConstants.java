/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.token;

public class PayloadConstants {

    public static final String EXPIRATION_TIME_IN_SECS = "exp";
    public static final String NOT_BEFORE_TIME_IN_SECS = "nbf";
    public static final String ISSUED_AT_TIME_IN_SECS = "iat";
    public static final String ISSUER = "iss";
    public static final String AUDIENCE = "aud";
    public static final String JWTID = "jti";
    public static final String TYPE = "typ";
    public static final String SUBJECT = "sub";
    public static final String AUTHZ_TIME_IN_SECS = "auth_time";
    public static final String AUTHORIZED_PARTY = "azp";
    public static final String NONCE = "nonce";
    public static final String AT_HASH = "at_hash";
    public static final String CLASS_REFERENCE = "acr";
    public static final String METHODS_REFERENCE = "amr";

}
