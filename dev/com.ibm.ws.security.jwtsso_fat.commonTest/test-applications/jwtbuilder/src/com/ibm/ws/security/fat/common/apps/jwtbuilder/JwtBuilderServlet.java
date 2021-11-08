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
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatConstants;

@WebServlet("/build")
public class JwtBuilderServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public static final String PARAM_BUILDER_ID = "builder_id";

    private Map<String, String[]> parameters = new HashMap<String, String[]>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("************ " + this.getClass().getSimpleName() + " Enter ************");
        try {
            parameters = request.getParameterMap();
            printParameters();

            JwtToken jwt = generateJwt(request);
            response.addCookie(new Cookie(JwtFatConstants.JWT_COOKIE_NAME, jwt.compact()));

        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
        }
        System.out.println("************ " + this.getClass().getSimpleName() + " Exit  ************");
    }

    private void printParameters() {
        for (Entry<String, String[]> parameter : parameters.entrySet()) {
            System.out.println("Parameter [" + parameter.getKey() + "]: " + Arrays.toString(parameter.getValue()));
        }
    }

    private JwtToken generateJwt(HttpServletRequest request) throws InvalidBuilderException, InvalidClaimException, JwtException {
        System.out.println("Generating a JWT...");

        String builderConfigId = request.getParameter(PARAM_BUILDER_ID);
        System.out.println("Got builder config ID: [" + builderConfigId + "]");

        JwtToken jwt = buildJwt(builderConfigId);
        System.out.println("Built JWT: " + jwt.compact());
        return jwt;
    }

    private JwtToken buildJwt(String builderConfigId) throws InvalidBuilderException, InvalidClaimException, JwtException {
        JwtBuilder builder = JwtBuilder.create(builderConfigId);
        return builder.buildJwt();
    }

}
