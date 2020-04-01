/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.jwtbuilderclient;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.jwt.JwtBuilder;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.fat.common.jwt.utils.JWTApiApplicationUtils;
import com.ibm.ws.security.jwt.fat.builder.JWTBuilderConstants;

/**
 * Test application to run the jwtBuilder create Apis.
 * This app focuses on creating a builder. Different config ids or no config id can be passed.
 * The tests using this app are simply trying to create a jwt token using a builder that is
 * populated from just the specified or default config (we won't use any of the set methods
 * to update any of the attributes)
 * The app creates and populates a builder (based on the config). Then, it creates a jwt from
 * the builder. Finally, it uses the claim get methods to log the values found in the jwt.
 * The calling test cases will validate that the values logged are appropriate for the config/test case.
 */
@SuppressWarnings("restriction")
public class JwtBuilderCreateClient extends HttpServlet {

    protected JWTApiApplicationUtils appUtils = new JWTApiApplicationUtils();
    private static final long serialVersionUID = 1L;
    PrintWriter pw = null;
    protected JwtBuilder myJwtBuilder = null;

    protected String jwtTokenString = null;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public JwtBuilderCreateClient() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doWorker(request, response);
        return;
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doWorker(request, response);
        return;
    }

    protected void doWorker(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("Got into the JWT Builder Client");
        appUtils.outputParameters(request);

        runBuilder(request, response);

    }

    private void runBuilder(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {
            pw = appUtils.outputEntry(response, JWTBuilderConstants.JWT_BUILDER_CREATE_ENDPOINT);

            String configId = request.getParameter(JWTBuilderConstants.JWT_BUILDER_PARAM_BUILDER_ID);
            createBuilder(configId);

            // we should now have a populated builder - build the token, and log the contents/test the claim apis, ...
            JwtToken newJwtToken = myJwtBuilder.buildJwt();
            appUtils.logIt(pw, "JwtToken: " + newJwtToken.toString());

            appUtils.outputHeader(pw, JWTBuilderConstants.JWT_TOKEN_HEADER, newJwtToken);
            appUtils.outputClaims(pw, JWTBuilderConstants.JWT_CLAIM, newJwtToken);

            jwtTokenString = newJwtToken.compact();

            appUtils.logIt(pw, JWTBuilderConstants.BUILT_JWT_TOKEN + jwtTokenString);
            System.out.println("exiting the svc client");
            appUtils.logIt(pw, "*******************  End of JWTBuilderClient output  ******************* ");
            pw.close();

        } catch (Exception e) {
            appUtils.handleException(pw, response, e);
        }

        appUtils.outputExit(pw, JWTBuilderConstants.JWT_BUILDER_CREATE_ENDPOINT);
    }

    /**
     * Create a JwtBuilder using the config specified by the configId
     * We'll test with a string, null, or an empty string
     *
     * @param configId
     *            the config to use
     * @throws Exception
     */
    protected void createBuilder(String configId) throws Exception {

        //* we were were passed some value for the configId (could a string, or a keyword indicating null or an empty string)
        if (configId != null) {
            switch (configId) {
            case JWTBuilderConstants.NULL_STRING:
                appUtils.logIt(pw, "Using configId: " + JWTBuilderConstants.NULL_VALUE);
                myJwtBuilder = JwtBuilder.create(JWTBuilderConstants.NULL_VALUE);
                break;
            case JWTBuilderConstants.EMPTY_STRING:
                appUtils.logIt(pw, "Using configId: " + JWTBuilderConstants.EMPTY_VALUE);
                myJwtBuilder = JwtBuilder.create(JWTBuilderConstants.EMPTY_VALUE);
                break;
            default:
                // if we were passed a configId, use it
                appUtils.logIt(pw, "Using configId: " + configId);
                myJwtBuilder = JwtBuilder.create(configId);
            }

        } else {

            appUtils.logIt(pw, "Not specifying a configId");
            myJwtBuilder = JwtBuilder.create();

        }

    }

}
