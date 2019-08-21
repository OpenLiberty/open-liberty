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
import com.ibm.ws.security.jwt.fat.builder.JWTApplicationUtils;
import com.ibm.ws.security.jwt.fat.builder.JWTBuilderConstants;

/**
 * Test application to run the jwtBuilder Apis.
 * This app will create a jwtBuilder, and then invoke all of the methods on the api's that it supports.
 * The test case invoking the app will validate that the specific values processed by the api's are correct.
 * (ie: <claims>.toJsonString() and <claims>.getIssuer() contain that value that the test set)
 */
public class JwtBuilderCreateClient extends HttpServlet {

    protected JWTApplicationUtils appUtils = new JWTApplicationUtils();
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

            appUtils.outputHeader(pw, JWTBuilderConstants.JWT_BUILDER_HEADER, newJwtToken);
            appUtils.outputClaims(pw, JWTBuilderConstants.JWT_BUILDER_CLAIM, newJwtToken);

            if (newJwtToken != null) {
                jwtTokenString = newJwtToken.compact();
            } else {
                jwtTokenString = "token was null";
            }

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

    /******************************************* Helper methods *******************************************/

    //    private void outputParameters(HttpServletRequest request) {
    //        Map<String, String[]> params = request.getParameterMap();
    //        for (Entry<String, String[]> entry : params.entrySet()) {
    //            System.out.println("Parm: " + entry.getKey() + "=" + Arrays.toString(entry.getValue()));
    //        }
    //    }

    //    /***
    //     * Log information to both the server log and add it to the response that will be returned to the caller.
    //     *
    //     * @param msg
    //     *            message to record
    //     */
    //    //    protected void logIt(String msg) {
    //    //        System.out.println(msg);
    //    //        pw.print(msg + newLine);
    //    //    }
    //
    //    private void handleException(HttpServletResponse response, Exception e) throws IOException {
    //
    //        System.out.println(e.getMessage());
    //        appUtils.logIt(pw, "Caught an exception calling external App: " + e.toString()); // this is probably expected
    //        //pw.close(); // we cannot close it here since it affects the following. Instead of getting 500, we will end up receiving 200.
    //        //        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    //
    //    }
    //
    //    private void outputEntry(HttpServletResponse response) throws IOException {
    //        pw = response.getWriter();
    //        response.setContentType("text/plain");
    //        appUtils.logIt(pw, "");
    //        appUtils.logIt(pw, "*******************  Start of JwtBuilderClient output  ******************* ");
    //    }
    //
    //    private void outputExit() {
    //        appUtils.logIt(pw, "*******************  End of JwtBuilderClient output  ******************* ");
    //        pw.close();
    //    }
    //
    //    /**
    //     * Since we can't really pass null and "" through the interface, we can pass values that
    //     * imply null and "". If we are passed one of those, we need to translate it to the real value.
    //     *
    //     * @param specialString
    //     *            - the passed string
    //     * @return
    //     */
    //    private String getSpecialValue(String specialString) {
    //        System.out.println("getSpecialValue: " + specialString);
    //        if (specialString == null) {
    //            return specialString;
    //        }
    //        if (specialString.equals(nullString)) {
    //            System.out.println("getSpecialValue: return null");
    //            return null;
    //        } else if (specialString.equals(emptyString)) {
    //            System.out.println("getSpecialValue: return empty");
    //            return emptyValue;
    //        } else {
    //            System.out.println("getSpecialValue: return passed in value");
    //            return specialString;
    //        }
    //    }
}
