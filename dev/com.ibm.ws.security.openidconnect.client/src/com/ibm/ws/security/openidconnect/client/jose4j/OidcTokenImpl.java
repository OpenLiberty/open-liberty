/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.client.jose4j;

import java.io.Serializable;

import org.jose4j.jwt.JwtClaims;

import com.ibm.websphere.security.openidconnect.token.IdToken;
import com.ibm.ws.security.openidconnect.client.jose4j.util.OidcTokenImplBase;

/**
 * hides IdToken interface from common code
 */
public class OidcTokenImpl extends OidcTokenImplBase implements IdToken, Serializable {

    /*
     * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
     * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
     *
     *
     * WARNING!!!!
     *
     * Carefully consider changes to this class. Serialization across different
     * versions must always be supported. Additionally, any referenced classes
     * must be available to the JCache provider's serialization.
     *
     *
     * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
     * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
     */

    private static final long serialVersionUID = 1L;

    public OidcTokenImpl(JwtClaims jwtClaims, String access_token, String refresh_token, String client_id, String tokenTypeNoSpace) {
        super(jwtClaims, access_token, refresh_token, client_id, tokenTypeNoSpace);
    }

    public OidcTokenImpl(OidcTokenImplBase token) {
        super(token.getJwtClaims(), token.getAccessToken(), token.getRefreshToken(), token.getClientId(), token.getTokenTypeNoSpace());
    }
}
