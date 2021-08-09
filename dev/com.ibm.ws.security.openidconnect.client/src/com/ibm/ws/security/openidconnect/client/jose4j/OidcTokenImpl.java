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
package com.ibm.ws.security.openidconnect.client.jose4j;

import org.jose4j.jwt.JwtClaims;

import com.ibm.websphere.security.openidconnect.token.IdToken;
import com.ibm.ws.security.openidconnect.client.jose4j.util.OidcTokenImplBase;

/**
 * hides IdToken interface from common code
 */
public class OidcTokenImpl extends OidcTokenImplBase implements IdToken {
    public OidcTokenImpl(JwtClaims jwtClaims, String access_token, String refresh_token, String client_id, String tokenTypeNoSpace) {
        super(jwtClaims, access_token, refresh_token, client_id, tokenTypeNoSpace);
    }

    public OidcTokenImpl(OidcTokenImplBase token) {
        super(token.getJwtClaims(), token.getAccessToken(), token.getRefreshToken(), token.getClientId(), token.getTokenTypeNoSpace());
    }
}
