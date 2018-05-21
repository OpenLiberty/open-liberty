/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.tai;

import java.util.ArrayList;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jose4j.lang.JoseException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.JwtConsumer;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.mp.jwt.TraceConstants;
import com.ibm.ws.security.mp.jwt.error.MpJwtProcessingException;
import com.ibm.ws.security.mp.jwt.impl.DefaultJsonWebTokenImpl;

public class TAIJwtUtils {

    private static TraceComponent tc = Tr.register(TAIJwtUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    public static final String TYPE_JWT_TOKEN = "Json Web Token";

    public TAIJwtUtils() {
    }

    public JwtToken createJwt(@Sensitive String idToken, String jwtConfigId) throws MpJwtProcessingException {
        String methodName = "createJwt";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, idToken, jwtConfigId);
        }
        try {
            JwtToken token = JwtConsumer.create(jwtConfigId).createJwt(idToken);
            if (tc.isDebugEnabled()) {
                Tr.exit(tc, methodName, token);
            }
            return token;
        } catch (Exception e) {
            String msg = Tr.formatMessage(tc, "ERROR_CREATING_JWT", new Object[] { jwtConfigId, e.getLocalizedMessage() });
            Tr.error(tc, msg);
            throw new MpJwtProcessingException(msg, e);
        }
    }

    /**
     * @param username
     * @param groups
     * @param jwtToken
     * @return
     * @throws JoseException
     */
    @Sensitive
    public JsonWebToken createJwtPrincipal(String username, ArrayList<String> groups, @Sensitive JwtToken jwtToken) {
        String methodName = "createJwtPrincipal";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, username, groups, jwtToken);
        }
        // TODO Auto-generated method stub

        String compact = null;
        String type = TYPE_JWT_TOKEN;
        if (jwtToken != null) {
            compact = jwtToken.compact();
            type = (String) jwtToken.getClaims().get(Claims.TOKEN_TYPE);
        }
        //        String payload = null;
        //        if (compact != null) {
        //            String[] parts = JsonUtils.splitTokenString(compact);
        //            if (parts.length > 0) {
        //                payload = JsonUtils.fromBase64ToJsonString(parts[1]); // payload - claims
        //            }
        //        }
        //        JwtClaims jwtclaims = new JwtClaims();
        //
        //        if (payload != null) {
        //            Map<String, Object> payloadClaims = org.jose4j.json.JsonUtil.parseJson(payload);
        //            Set<Entry<String, Object>> entries = payloadClaims.entrySet();
        //            Iterator<Entry<String, Object>> it = entries.iterator();
        //            while (it.hasNext()) {
        //                Entry<String, Object> entry = it.next();
        //
        //                String key = entry.getKey();
        //                Object value = entry.getValue();
        //                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
        //                    Tr.debug(tc, "Key : " + key + ", Value: " + value);
        //                }
        //                if (key != null && value != null) {
        //                    jwtclaims.setClaim(key, value);
        //                }
        //            }
        //        }
        //        jwtclaims.setStringClaim(org.eclipse.microprofile.jwt.Claims.raw_token.name(), compact);
        //        fixJoseTypes(jwtclaims);
        DefaultJsonWebTokenImpl token = new DefaultJsonWebTokenImpl(compact, type, username);
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, token);
        }
        return token;
    }

}
