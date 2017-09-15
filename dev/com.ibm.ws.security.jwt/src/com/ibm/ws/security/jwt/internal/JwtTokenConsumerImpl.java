/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.internal;

import java.util.List;

import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.Headers;
import org.jose4j.jwx.JsonWebStructure;

import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.JwtToken;

public class JwtTokenConsumerImpl implements JwtToken {

    Claims claims;
    String payload;
    String compact;
    Headers jwtHeaders;

    public JwtTokenConsumerImpl(JwtContext jwtContext) {
        claims = new ClaimsImpl();
        claims.putAll(jwtContext.getJwtClaims().getClaimsMap());
        compact = jwtContext.getJwt();
        List<JsonWebStructure> jsonStructures = jwtContext.getJoseObjects();

        JsonWebStructure jsonStruct = jsonStructures.get(0);
        jwtHeaders = jsonStruct.getHeaders();

    }

    @Override
    public Claims getClaims() {
        return claims;
    }

    @Override
    public String getHeader(String name) {
        return jwtHeaders.getStringHeaderValue(name);
    }

    @Override
    public String compact() {
        return compact;
    }

}
