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
package com.ibm.ws.security.fat.common.jwt.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jose4j.json.JsonUtil;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.PayloadConstants;

public class JWTApiApplicationUtils {

    private static final String newLine = System.getProperty("line.separator");

    /*** JWT application helper methods ***/

    /***
     * <p>
     * Log information to both the server log and add it to the response that will be returned to the caller
     *
     * @param msg
     *            - message to record
     * @throws Exception
     */
    public void logIt(PrintWriter pw, String msg) {
        System.out.println(msg);
        pw.print(msg + newLine);
    }

    /***
     * Log information to both the server log and add it to the response that will be returned to the caller.
     *
     * @param msg
     *            message to record
     */
    public void handleException(PrintWriter pw, HttpServletResponse response, Exception e) throws IOException {

        System.out.println(e.getMessage());
        e.printStackTrace();
        logIt(pw, "Caught an exception calling external App: " + e.toString()); // this is probably expected
        //pw.close(); // we cannot close it here since it affects the following. Instead of getting 500, we will end up receiving 200.
        //        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());

    }

    public PrintWriter outputEntry(HttpServletResponse response, String appName) throws IOException {
        PrintWriter pw = response.getWriter();
        response.setContentType("text/plain");
        logIt(pw, "");
        logIt(pw, "*******************  Start of " + appName + " output  ******************* ");
        return pw;
    }

    public void outputExit(PrintWriter pw, String appName) {
        logIt(pw, "*******************  End of " + appName + " output  ******************* ");
        pw.close();
    }

    public void outputParameters(HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        for (Entry<String, String[]> entry : params.entrySet()) {
            System.out.println("Parm: " + entry.getKey() + "=" + Arrays.toString(entry.getValue()));
        }
    }

    /**
     * Outputs all of the claims contained in the provided token. The individual claims are output in addition to the full JSON
     * representation of the token claims.
     *
     * @param prefixMsg
     * @param token
     * @throws Exception
     */
    public void outputClaims(PrintWriter pw, String prefixMsg, JwtToken token) throws Exception {
        if (token == null) {
            logIt(pw, prefixMsg + JwtConstants.NO_JWT_TOKEN);
            return;
        }

        Claims theClaims = token.getClaims();
        if (theClaims == null) {
            logIt(pw, prefixMsg + JwtConstants.NO_JWT_CLAIMS);
            return;
        }

        logIt(pw, prefixMsg + PayloadConstants.ISSUER + ": " + theClaims.getIssuer());
        logIt(pw, prefixMsg + PayloadConstants.SUBJECT + ": " + theClaims.getSubject());
        logIt(pw, prefixMsg + PayloadConstants.AUDIENCE + ": " + theClaims.getAudience());
        logIt(pw, prefixMsg + PayloadConstants.EXPIRATION_TIME + ": " + theClaims.getExpiration());
        logIt(pw, prefixMsg + PayloadConstants.NOT_BEFORE + ": " + theClaims.getNotBefore());
        logIt(pw, prefixMsg + PayloadConstants.ISSUED_AT + ": " + theClaims.getIssuedAt());
        logIt(pw, prefixMsg + PayloadConstants.JWT_ID + ": " + theClaims.getJwtId());
        logIt(pw, prefixMsg + PayloadConstants.AUTHORIZED_PARTY + ": " + theClaims.getAuthorizedParty());

        // Print everything that is in the payload
        String jString = theClaims.toJsonString();
        logIt(pw, prefixMsg + JwtConstants.JWT_JSON + jString);

        if (jString != null) {
            Map<String, Object> jObject = JsonUtil.parseJson(jString);
            Set<String> jKeys = jObject.keySet();
            for (String key : jKeys) {
                logIt(pw, prefixMsg + JwtConstants.JWT_JSON + JwtConstants.JWT_GETALLCLAIMS + JwtConstants.JWT_CLAIM_KEY + key + " "
                          + JwtConstants.JWT_CLAIM_VALUE + jObject.get(key));
            }
        } else {
            logIt(pw, prefixMsg + JwtConstants.JWT_JSON + null);
        }
    }

    /**
     * Output the Base64-decoded header from the provided token. Each key and value within the header is individually output to
     * aid validation.
     *
     * @param prefixMsg
     * @param token
     * @throws IOException
     */
    public void outputHeader(PrintWriter pw, String prefixMsg, JwtToken token) throws IOException {
        if (token == null) {
            logIt(pw, prefixMsg + null);
            return;
        }
        String tokenString = token.compact();
        String[] tokenParts = tokenString.split("\\.");
        if (tokenParts == null) {
            logIt(pw, prefixMsg + JwtConstants.JWT_MALFORMED_TOKEN_HEADER + tokenString);
            return;
        }

        String decodedHeader = new String(Base64.getDecoder().decode(tokenParts[0]), "UTF-8");
        logIt(pw, prefixMsg + JwtConstants.JWT_JSON + decodedHeader);

        JSONObject headerInfo = JSONObject.parse(decodedHeader);
        @SuppressWarnings("unchecked")
        Set<String> jKeys = headerInfo.keySet();
        for (String key : jKeys) {
            logIt(pw, prefixMsg + JwtConstants.JWT_TOKEN_HEADER_JSON + JwtConstants.JWT_CLAIM_KEY + key + " " + JwtConstants.JWT_CLAIM_VALUE + headerInfo.get(key));
        }
    }
}