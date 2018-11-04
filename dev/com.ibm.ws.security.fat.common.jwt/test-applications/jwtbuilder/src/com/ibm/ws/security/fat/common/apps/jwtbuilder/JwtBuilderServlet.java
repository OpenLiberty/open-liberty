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
package com.ibm.ws.security.fat.common.apps.jwtbuilder;

import java.io.IOException;
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
import com.ibm.ws.security.openidconnect.token.PayloadConstants;

@WebServlet("/build")
public class JwtBuilderServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private Map<String, String[]> parameters = new HashMap<String, String[]>();
    JwtBuilder builder = null;

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

    private JwtToken generateJwt(HttpServletRequest request) throws InvalidBuilderException, InvalidClaimException, JwtException {
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

    private void populateBuilder(HttpServletRequest request) throws InvalidBuilderException, InvalidClaimException, JwtException {

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
                default:
                    // handle unknowns
                    addToClaimMap(claimMap, parameter);
                    break;
            }
        }
        if (!claimMap.isEmpty()) {
            builder.claim(claimMap);
        }
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

//    private JwtToken buildJwt(String builderConfigId) throws InvalidBuilderException, InvalidClaimException, JwtException {
//        JwtBuilder builder = JwtBuilder.create(builderConfigId);
//        //todo - need to figure out how to put upn into the token
//        builder.claim("upn", "testuser");
//        System.out.println("builder content: " + builder.toString());
//        return builder.buildJwt();
//    }

}
