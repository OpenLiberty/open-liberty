/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.jwtconsumerclient;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Base64;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.jwt.InvalidConsumerException;
import com.ibm.websphere.security.jwt.JwtBuilder;
import com.ibm.websphere.security.jwt.JwtConsumer;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.fat.common.jwt.utils.JWTApiApplicationUtils;
import com.ibm.ws.security.jwt.fat.consumer.JwtConsumerConstants;

/**
 * Test application to run the jwtConsumer Apis.
 * This app will create a jwtConsumer, and then invoke all of the methods on the api's that it supports.
 * The test case invoking the app will validate that the specific values processed by the api's are correct.
 * (ie: <claims>.toJsonString() and <claims>.getIssuer() contain that value that the test set)
 */
@SuppressWarnings("restriction")
public class JwtConsumerClient extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String newLine = System.getProperty("line.separator");
    protected JWTApiApplicationUtils appUtils = new JWTApiApplicationUtils();

    protected JwtBuilder myJwtBuilder = null;
    protected JwtConsumer myJwtConsumer = null;
    protected String jwtTokenString = null;
    protected String configId = JwtConsumer.DEFAULT_ID;

    PrintWriter pw = null;

    private final String nullString = "null";
    private final String emptyString = "empty";
    private final String emptyValue = "";

    /**
     * @see HttpServlet#HttpServlet()
     */
    public JwtConsumerClient() {
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
        System.out.println("Got into the JWT Consumer Client");
        appUtils.outputParameters(request);

        runConsumer(request, response);
    }

    private void runConsumer(HttpServletRequest request, HttpServletResponse response) throws IOException {

        configId = getSpecialValue(request.getParameter(JwtConsumerConstants.JWT_CONSUMER_PARAM_CLIENT_ID));
        String tokenParam = getSpecialValue(request.getParameter(JwtConsumerConstants.JWT_CONSUMER_PARAM_JWT));

        pw = appUtils.outputEntry(response, JwtConsumerConstants.JWT_CONSUMER_ENDPOINT);

        try {
            myJwtConsumer = createConsumer(configId);
            if (myJwtConsumer == null) {
                throw new Exception("Could not create JwtConsumer object for id [" + configId + "].");
            }
            appUtils.logIt(pw, "Successfully created consumer for id [" + configId + "]");

            outputTokenJson(tokenParam);

            JwtToken token = myJwtConsumer.createJwt(tokenParam);
            appUtils.logIt(pw, JwtConsumerConstants.BUILT_JWT_TOKEN + ((token == null) ? null : token.compact()));

            appUtils.outputHeader(pw, JwtConsumerConstants.JWT_TOKEN_HEADER, token);
            appUtils.outputClaims(pw, JwtConsumerConstants.JWT_CONSUMER_CLAIM, token);

        } catch (Exception e) {
            appUtils.handleException(pw, response, e);
        }
        appUtils.outputExit(pw, JwtConsumerConstants.JWT_CONSUMER_ENDPOINT);
    }

    /******************************************* Helper methods *******************************************/
    private JwtConsumer createConsumer(String configId) throws InvalidConsumerException {
        if (configId != null) {
            appUtils.logIt(pw, "Using configId: " + configId);
            if (configId.equals(nullString)) {
                myJwtConsumer = JwtConsumer.create(null);
            } else {
                myJwtConsumer = JwtConsumer.create(configId);
            }
        } else {
            appUtils.logIt(pw, "Not specifying a configId");
            myJwtConsumer = JwtConsumer.create();
        }
        return myJwtConsumer;
    }

    private void outputTokenJson(String token) throws Exception {
        if (token == null || token.isEmpty()) {
            appUtils.logIt(pw, "Token string is null or empty");
            return;
        }
        String[] tokenParts = token.split("\\.");
        if (tokenParts.length != 3) {
            // we have a JWE, not a JWS - we can only process the first header since we don't have the key to decode
            String decodedPart = new String(Base64.getDecoder().decode(tokenParts[0]), "UTF-8");
            appUtils.logIt(pw, "JWE Token part[0]: " + decodedPart);
        } else {
            // we have a JWS - we can process all parts
            for (int i = 0; i < tokenParts.length; i++) {
                try {
                    String decodedPart = new String(Base64.getDecoder().decode(tokenParts[i]), "UTF-8");
                    appUtils.logIt(pw, "JWS Token part[" + i + "]: " + decodedPart);
                } catch (Exception e) {
                    appUtils.logIt(pw, "Decoding token part [" + i + "] failed with" + e.getMessage());
                }
            }
        }
    }

    /***
     * Log information to both the server log and add it to the response that will be returned to the caller.
     *
     * @param msg
     *            message to record
     */

    private String getSpecialValue(String specialString) {
        System.out.println("getSpecialValue: " + specialString);
        if (specialString == null) {
            return specialString;
        }
        if (specialString.equals(nullString)) {
            System.out.println("getSpecialValue: return null");
            return null;
        } else if (specialString.equals(emptyString)) {
            System.out.println("getSpecialValue: return empty");
            return emptyValue;
        } else {
            System.out.println("getSpecialValue: return passed in value");
            return specialString;
        }
    }
}
