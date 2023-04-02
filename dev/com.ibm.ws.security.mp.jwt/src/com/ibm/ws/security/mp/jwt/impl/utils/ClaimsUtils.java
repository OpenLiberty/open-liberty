/*******************************************************************************
 * Copyright (c) 2017,2022 IBM Corporation and others.
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
package com.ibm.ws.security.mp.jwt.impl.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.jose4j.json.JsonUtil;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.lang.JoseException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.jwk.utils.JsonUtils;
import com.ibm.ws.security.mp.jwt.TraceConstants;

/**
 *
 */
public class ClaimsUtils {
    public static final TraceComponent tc = Tr.register(ClaimsUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private static final JsonBuilderFactory builderFactory = Json.createBuilderFactory(null);

    /**
     * Uses the claims from the JwtToken to avoid having to reparse the jwt json string.
     */
    public static JwtClaims getJwtClaims(JwtToken jwtToken) {
        String methodName = "getJwtClaims";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, jwtToken);
        }
        Claims claims = jwtToken.getClaims();
        JwtClaims jwtclaims = createJwtClaims(claims, jwtToken.compact());
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, jwtclaims);
        }
        return jwtclaims;
    }

    /**
     * Parses the provided JWT and returns the claims found within its payload.
     */
    public static JwtClaims getJwtClaims(String jwt) throws JoseException {
        String methodName = "getJwtClaims";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, jwt);
        }
        JwtClaims jwtclaims;
        String payload = getJwtPayload(jwt);
        if (payload != null) {
            Map<String, Object> payloadClaims = JsonUtil.parseJson(payload);
            jwtclaims = createJwtClaims(payloadClaims, jwt);
        } else {
            jwtclaims = new JwtClaims();
        }
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, jwtclaims);
        }
        return jwtclaims;
    }

    static String getJwtPayload(String jwt) {
        String methodName = "getJwtPayload";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, jwt);
        }
        String payload = null;
        if (jwt != null) {
            String[] parts = JsonUtils.splitTokenString(jwt);
            if (parts.length > 0) {
                payload = JsonUtils.fromBase64ToJsonString(parts[1]); // payload - claims
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, payload);
        }
        return payload;
    }

    static JwtClaims createJwtClaims(Map<String, Object> claimsMap, String jwt) {
        String methodName = "getClaimsFromJwtPayload";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, jwt);
        }
        JwtClaims jwtclaims = new JwtClaims();
        for (Entry<String, Object> entry : claimsMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Key : " + key + ", Value: " + value);
            }
            if (key != null && value != null) {
                jwtclaims.setClaim(key, value);
            }
        }
        jwtclaims.setStringClaim(org.eclipse.microprofile.jwt.Claims.raw_token.name(), jwt);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Key : " + "raw_token" + ", Value: " + "raw_token");
        }
        convertJoseTypes(jwtclaims);
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, jwtclaims);
        }
        return jwtclaims;
    }

    /**
     * Convert the types jose4j uses for address, sub_jwk, and jwk
     */
    private static void convertJoseTypes(JwtClaims claimsSet) {
        //        if (claimsSet.hasClaim(Claims.address.name())) {
        //            replaceMap(Claims.address.name());
        //        }
        //        if (claimsSet.hasClaim(Claims.jwk.name())) {
        //            replaceMap(Claims.jwk.name());
        //        }
        //        if (claimsSet.hasClaim(Claims.sub_jwk.name())) {
        //            replaceMap(Claims.sub_jwk.name());
        //        }
        if (claimsSet.hasClaim("address")) {
            replaceMapWithJsonObject("address", claimsSet);
        }
        if (claimsSet.hasClaim("jwk")) {
            replaceMapWithJsonObject("jwk", claimsSet);
        }
        if (claimsSet.hasClaim("sub_jwk")) {
            replaceMapWithJsonObject("sub_jwk", claimsSet);
        }
        if (claimsSet.hasClaim("aud")) {
            convertToList("aud", claimsSet);
        }
        if (claimsSet.hasClaim("groups")) {
            convertToList("groups", claimsSet);
        }
    }

    /**
     * Replace the jose4j Map<String,Object> with a JsonObject
     */
    private static void replaceMapWithJsonObject(String claimName, JwtClaims claimsSet) {
        try {
            Map<String, Object> map = claimsSet.getClaimValue(claimName, Map.class);
            JsonObjectBuilder builder = builderFactory.createObjectBuilder();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                builder.add(entry.getKey(), entry.getValue().toString());
            }
            JsonObject jsonObject = builder.build();
            claimsSet.setClaim(claimName, jsonObject);
        } catch (MalformedClaimException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "The value for the claim [" + claimName + "] could not be convered to a Map: " + e.getLocalizedMessage());
            }
        }
    }

    /**
     * Converts the value for the specified claim into a String list.
     */
    @FFDCIgnore({ MalformedClaimException.class })
    private static void convertToList(String claimName, JwtClaims claimsSet) {
        List<String> list = null;
        try {
            list = claimsSet.getStringListClaimValue(claimName);
        } catch (MalformedClaimException e) {
            try {
                String value = claimsSet.getStringClaimValue(claimName);
                if (value != null) {
                    list = new ArrayList<String>();
                    list.add(value);
                    claimsSet.setClaim(claimName, list);
                }
            } catch (MalformedClaimException e1) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "The value for the claim [" + claimName + "] could not be convered to a string list: " + e1.getLocalizedMessage());
                }
            }
        }
    }

}
