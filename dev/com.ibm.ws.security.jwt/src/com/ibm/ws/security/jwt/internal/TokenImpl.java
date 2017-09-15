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

import java.util.Map;

import org.jose4j.lang.JoseException;

import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.JwtException;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.jwt.config.JwtConfig;
import com.ibm.ws.security.jwt.utils.JwtCreator;
import com.ibm.ws.security.jwt.utils.JwtData;
import com.ibm.ws.security.jwt.utils.JwtUtils;

public class TokenImpl implements JwtToken {
    private static final String KEY_JWT_SERVICE = "jwtConfig";
    Claims claims;
    String header;
    String payload;
    String compact;

    public TokenImpl(BuilderImpl jwtBuilder, JwtConfig config) throws JwtException {
        // claims = jwtBuilder.getClaims();
        claims = new ClaimsImpl();
        try {
            createToken(jwtBuilder, config);
        } catch (JwtTokenException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            throw new JwtException(e.getMessage(), e);
        }
    }

    private void createToken(BuilderImpl jwtBuilder, JwtConfig config) throws JwtTokenException {
        // TODO Auto-generated method stub

        JwtConfig jwtConfig = config;//jwtBuilder.getTheServiceConfig(jwtBuilder.getClaims().getIssuer());

        JwtData jwtData = new JwtData(jwtBuilder, jwtConfig, JwtData.TYPE_JWT_TOKEN);
        compact = JwtCreator.createJwtAsString(jwtData, jwtBuilder.getClaims());
        String[] parts = JwtUtils.splitTokenString(compact);

        header = JwtUtils.fromBase64ToJsonString(parts[0]); // decoded header in
                                                            // json format
        payload = JwtUtils.fromBase64ToJsonString(parts[1]); // payload - claims
        setClaims();
    }

    private void setClaims() throws JwtTokenException {
        Map claimsMap = null;
        try {
            claimsMap = JwtUtils.claimsFromJsonObject(payload);
        } catch (JoseException e) {
            throw JwtTokenException.newInstance(true, "JWT_CREATE_FAIL", new Object[] { e.getLocalizedMessage() });
        }
        if (claimsMap != null && !claimsMap.isEmpty()) {
            claims.putAll(claimsMap);
        }

    }

    @Override
    public Claims getClaims() {
        // TODO Auto-generated method stub
        return claims;
    }

    @Override
    public String getHeader(String name) {
        Map headerMap = null;
        try {
            headerMap = JwtUtils.claimsFromJsonObject(header);
        } catch (JoseException e) {
            //TODO
        }
        String value = null;
        if (headerMap != null && headerMap.get(name) != null) {
            value = (String) headerMap.get(name);
        }
        return value;
    }

    @Override
    public String compact() {
        // TODO Auto-generated method stub
        return compact;
    }

}
