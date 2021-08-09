/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.apps.jwtbuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Key;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.jwt.InvalidBuilderException;
import com.ibm.websphere.security.jwt.InvalidClaimException;
import com.ibm.websphere.security.jwt.JwtBuilder;
import com.ibm.websphere.security.jwt.JwtException;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.utils.JWTApiApplicationUtils;
import com.ibm.ws.security.fat.common.utils.KeyTools;
import com.ibm.ws.security.openidconnect.token.PayloadConstants;

@WebServlet("/build")
public class JwtBuilderServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private Map<String, String[]> parameters = new HashMap<String, String[]>();
    JwtBuilder builder = null;
    protected JWTApiApplicationUtils appUtils = new JWTApiApplicationUtils();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doWorker(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doWorker(request, response);
    }

    protected void doWorker(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("************ " + this.getClass().getSimpleName() + " Enter ************");
        try {
            parameters = request.getParameterMap();
            printParameters();

            JwtToken jwt = generateJwt(request);
            response.addCookie(new Cookie(JwtConstants.JWT_COOKIE_NAME, jwt.compact()));
            printTokenParts(response, jwt);

        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
        }
        System.out.println("************ " + this.getClass().getSimpleName() + " Exit  ************");
    }

    private void printParameters() {
        System.out.println("JwtBuilderServlet passed parameters: ");
        for (Entry<String, String[]> parameter : parameters.entrySet()) {
            System.out.println("Parameter [" + parameter.getKey() + "]: " + Arrays.toString(parameter.getValue()));
        }
    }

    private void printTokenParts(HttpServletResponse response, JwtToken jwt) throws Exception {

        PrintWriter pw = appUtils.outputEntry(response, "JwtBuilderServlet");
        appUtils.outputHeader(pw, JwtConstants.JWT_TOKEN_HEADER, jwt);
        appUtils.outputClaims(pw, JwtConstants.JWT_CLAIM, jwt);

    }

    private JwtToken generateJwt(HttpServletRequest request) throws InvalidBuilderException, InvalidClaimException, JwtException, Exception {
        System.out.println("Generating a JWT...");

        // create builder
        builder = createBuilder(request);
        populateBuilder(request);

        JwtToken jwt = builder.buildJwt();
        System.out.println("Built JWT: " + jwt.compact());

        return jwt;
    }

    private JwtBuilder createBuilder(HttpServletRequest request) throws InvalidBuilderException, InvalidClaimException, JwtException {
        String builderConfigId = request.getParameter(JwtConstants.PARAM_BUILDER_ID);
        System.out.println("Got builder config ID: [" + builderConfigId + "]");
        JwtBuilder builder = JwtBuilder.create(builderConfigId);
        return builder;
    }

    private void populateBuilder(HttpServletRequest request) throws InvalidBuilderException, InvalidClaimException, JwtException, Exception {

        String keyMgmtKeyAlg = null;
        String contentEncryptAlg = null;
        String encryptKey = null;

        Map<String, Object> claimMap = new HashMap<String, Object>();
        for (Entry<String, String[]> parameter : parameters.entrySet()) {
            System.out.println("Parameter [" + parameter.getKey() + "]: " + Arrays.toString(parameter.getValue()));

            // only adding code to support what we're currently using in test
            // will continue to enhance this as time goes on.
            switch (parameter.getKey()) {
                case JwtConstants.PARAM_BUILDER_ID:
                    break; // skip as we handle this one in a special way
                case JwtConstants.PARAM_UPN:
                    builder.claim(parameter.getKey(), getOneStringValueFromParm(parameter));
                    break;
                case PayloadConstants.SUBJECT:
                    builder.subject(getOneStringValueFromParm(parameter));
                    break;
                case PayloadConstants.ISSUED_AT_TIME_IN_SECS:
                    builder.claim(PayloadConstants.ISSUED_AT_TIME_IN_SECS, getOneLongValueFromParm(parameter));
                    break;
                case PayloadConstants.EXPIRATION_TIME_IN_SECS:
                    builder.expirationTime(getOneLongValueFromParm(parameter));
                    break;
                case JwtConstants.PARAM_KEY_MGMT_ALG:
                    // allow special handling later
                    keyMgmtKeyAlg = getOneStringValueFromParm(parameter);
                    System.out.println("KeyMgmtKeyAlg: " + keyMgmtKeyAlg);
                    break;
                case JwtConstants.PARAM_ENCRYPT_KEY:
                    // allow special handling later
                    encryptKey = getOneStringValueFromParm(parameter);
                    System.out.println("encryptKey: " + encryptKey);
                    break;
                case JwtConstants.PARAM_CONTENT_ENCRYPT_ALG:
                    // allow special handling later
                    contentEncryptAlg = getOneStringValueFromParm(parameter);
                    System.out.println("contentEncryptAlg: " + contentEncryptAlg);
                    break;
                default:
                    // handle unknowns
                    addToClaimMap(claimMap, parameter);
                    break;
            }
        }
        if (!claimMap.isEmpty()) {
            builder.claim(claimMap);
        }
        // handle encryption outside of loop as we could have up to 3 parms need to perform encryption
        setEncryptWith(keyMgmtKeyAlg, encryptKey, contentEncryptAlg);

    }

    private String getOneStringValueFromParm(Entry<String, String[]> parameter) {

        String[] rawValue = parameter.getValue();
        if (rawValue == null || rawValue.length == 0) {
            return null;
        }

        return rawValue[0];
    }

    private Long getOneLongValueFromParm(Entry<String, String[]> parameter) {

        String[] rawValue = parameter.getValue();
        if (rawValue == null || rawValue.length == 0) {
            return null;
        }

        return Long.valueOf(rawValue[0]);
    }

    private void addToClaimMap(Map<String, Object> claimMap, Entry<String, String[]> parameter) {

        String[] rawValue = parameter.getValue();
        if (rawValue == null || rawValue.length == 0) {
            return;
        }

        if (rawValue.length == 1) {
            claimMap.put(parameter.getKey(), rawValue[0]);
        } else {
            claimMap.put(parameter.getKey(), rawValue);
        }
    }

    protected void setEncryptWith(String keyMgmtAlg, String encryptKeyString, String contentEncryptAlg) throws Exception {

        if (keyMgmtAlg != null || encryptKeyString != null || contentEncryptAlg != null) {

            if (keyMgmtAlg != null || encryptKeyString != null || contentEncryptAlg != null) {
                Key encryptKey = KeyTools.getKeyFromPem(encryptKeyString);
                System.out.println("Calling encryptWith with parms: keyManagementAlg=" + keyMgmtAlg + ", keyManagementKey=" + encryptKey + ", contentEncryptionAlg="
                                   + contentEncryptAlg);
                builder.encryptWith(keyMgmtAlg, encryptKey, contentEncryptAlg);
            }
        }
    }

}
