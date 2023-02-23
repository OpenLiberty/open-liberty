/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.utils;

public class PayloadConstants {

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
    public static final String PAYLOAD_EVENTS = "events";
    public static final String PAYLOAD_SESSION_ID = "sid";
    public static final String PAYLOAD_SCOPE = "scope";
    public static final String PAYLOAD_REALMNAME = "realmName";

}
