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
package com.ibm.ws.security.jwt.internal;

import java.util.Map;

import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.JwtException;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.jwt.config.JwtConfig;
import com.ibm.ws.security.jwt.utils.JwtCreator;
import com.ibm.ws.security.jwt.utils.JwtCreator.JwtResult;
import com.ibm.ws.security.jwt.utils.JwtData;

public class TokenImpl implements JwtToken {
    Claims claims;
    Map<String, Object> header;
    String compact;

    public TokenImpl(BuilderImpl jwtBuilder, JwtConfig config) throws JwtException {
        // claims = jwtBuilder.getClaims();
        claims = new ClaimsImpl();
        try {
            createToken(jwtBuilder, config);
        } catch (Exception e) {
            throw new JwtException(e.getMessage(), e);
        }
    }

    private void createToken(BuilderImpl jwtBuilder, JwtConfig config) throws Exception {
        JwtData jwtData = new JwtData(jwtBuilder, config, JwtData.TYPE_JWT_TOKEN);
        JwtResult result = JwtCreator.createJwt(jwtData, jwtBuilder.getClaims());
        compact = result.getCompact();
        header = result.getHeader();
        claims = result.getClaims();
    }

    @Override
    public Claims getClaims() {
        return claims;
    }

    @Override
    public String getHeader(String name) {
        String value = null;
        if (header != null && header.get(name) != null) {
            value = (String) header.get(name);
        }
        return value;
    }

    @Override
    public String compact() {
        return compact;
    }

}
