/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.jwt;

import org.jose4j.jwt.ReservedClaimNames;

public class ClaimConstants extends ReservedClaimNames {

    public static final String SCOPE = "scope";
    public static final String REALM_NAME = "realmName";
    public static final String TOKEN_TYPE = "token_type";
    public static final String NONCE = "nonce";
    public static final String AT_HASH = "at_hash";
    public static final String TYPE = "typ";
    public static final String AUTHORIZED_PARTY = "azp";

}
