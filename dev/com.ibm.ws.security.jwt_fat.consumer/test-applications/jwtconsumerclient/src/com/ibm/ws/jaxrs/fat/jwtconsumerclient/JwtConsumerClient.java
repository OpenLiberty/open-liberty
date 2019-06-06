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
package com.ibm.ws.jaxrs.fat.jwtconsumerclient;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jose4j.json.JsonUtil;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.InvalidConsumerException;
import com.ibm.websphere.security.jwt.JwtBuilder;
import com.ibm.websphere.security.jwt.JwtConsumer;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.jwt.fat.consumer.JWTConsumerConstants;

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
        outputParameters(request);

        runConsumer(request, response);
    }

    private void runConsumer(HttpServletRequest request, HttpServletResponse response) throws IOException {

        configId = getSpecialValue(request.getParameter(JWTConsumerConstants.JWT_CONSUMER_PARAM_CLIENT_ID));
        String tokenParam = getSpecialValue(request.getParameter(JWTConsumerConstants.JWT_CONSUMER_PARAM_JWT));

        outputEntry(response);

        try {
            myJwtConsumer = createConsumer(configId);
            if (myJwtConsumer == null) {
                throw new Exception("Could not create JwtConsumer object for id [" + configId + "].");
            }
            logIt("Successfully created consumer for id [" + configId + "]");

            outputTokenJson(tokenParam);

            JwtToken token = myJwtConsumer.createJwt(tokenParam);
            logIt(JWTConsumerConstants.BUILT_JWT_TOKEN + ((token == null) ? null : token.compact()));

            outputHeader(JWTConsumerConstants.JWT_BUILDER_HEADER, token);
            outputClaims(JWTConsumerConstants.JWT_CONSUMER_CLAIM, token);

        } catch (Exception e) {
            handleException(response, e);
        }
        outputExit();
    }

    /**
     * Output the Base64-decoded header from the provided token. Each key and value within the header is individually output to
     * aid validation.
     *
     * @param prefixMsg
     * @param token
     * @throws IOException
     */
    protected void outputHeader(String prefixMsg, JwtToken token) throws IOException {
        if (token == null) {
            logIt(prefixMsg + null);
            return;
        }
        String tokenString = token.compact();
        String[] tokenParts = tokenString.split("\\.");
        if (tokenParts == null) {
            logIt(prefixMsg + JWTConsumerConstants.JWT_CONSUMER_TOKEN_HEADER_MALFORMED + tokenString);
            return;
        }

        String decodedHeader = new String(Base64.getDecoder().decode(tokenParts[0]), "UTF-8");
        logIt(prefixMsg + JWTConsumerConstants.JWT_BUILDER_JSON + decodedHeader);

        JSONObject headerInfo = JSONObject.parse(decodedHeader);
        @SuppressWarnings("unchecked")
        Set<String> jKeys = headerInfo.keySet();
        for (String key : jKeys) {
            logIt(prefixMsg + JWTConsumerConstants.JWT_CONSUMER_TOKEN_HEADER_JSON + JWTConsumerConstants.JWT_BUILDER_KEY + key + " " + JWTConsumerConstants.JWT_BUILDER_VALUE + headerInfo.get(key));
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
    protected void outputClaims(String prefixMsg, JwtToken token) throws Exception {
        if (token == null) {
            logIt(prefixMsg + JWTConsumerConstants.JWT_BUILDER_NO_TOKEN);
            return;
        }

        Claims theClaims = token.getClaims();
        if (theClaims == null) {
            logIt(prefixMsg + JWTConsumerConstants.JWT_BUILDER_NO_CLAIMS);
            return;
        }

        logIt(prefixMsg + JWTConsumerConstants.JWT_BUILDER_ISSUER + theClaims.getIssuer());
        logIt(prefixMsg + JWTConsumerConstants.JWT_BUILDER_SUBJECT + theClaims.getSubject());
        logIt(prefixMsg + JWTConsumerConstants.JWT_BUILDER_AUDIENCE + theClaims.getAudience());
        logIt(prefixMsg + JWTConsumerConstants.JWT_BUILDER_EXPIRATION + theClaims.getExpiration());
        logIt(prefixMsg + JWTConsumerConstants.JWT_BUILDER_NOTBEFORE + theClaims.getNotBefore());
        logIt(prefixMsg + JWTConsumerConstants.JWT_BUILDER_ISSUED_AT + theClaims.getIssuedAt());
        logIt(prefixMsg + JWTConsumerConstants.JWT_BUILDER_JWTID + theClaims.getJwtId());
        logIt(prefixMsg + JWTConsumerConstants.JWT_BUILDER_AUTHORIZEDPARTY + theClaims.getAuthorizedParty());

        // Print everything that is in the payload
        String jString = theClaims.toJsonString();
        logIt(prefixMsg + JWTConsumerConstants.JWT_BUILDER_JSON + jString);

        if (jString != null) {
            Map<String, Object> jObject = JsonUtil.parseJson(jString);
            Set<String> jKeys = jObject.keySet();
            for (String key : jKeys) {
                logIt(prefixMsg + JWTConsumerConstants.JWT_BUILDER_JSON + JWTConsumerConstants.JWT_BUILDER_GETALLCLAIMS + JWTConsumerConstants.JWT_BUILDER_KEY + key + " " + JWTConsumerConstants.JWT_BUILDER_VALUE + jObject.get(key));
            }
        } else {
            logIt(prefixMsg + JWTConsumerConstants.JWT_BUILDER_JSON + null);
        }
    }

    /******************************************* Helper methods *******************************************/

    private void outputParameters(HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        for (Entry<String, String[]> entry : params.entrySet()) {
            System.out.println("Parm: " + entry.getKey() + "=" + Arrays.toString(entry.getValue()));
        }
    }

    private JwtConsumer createConsumer(String configId) throws InvalidConsumerException {
        if (configId != null) {
            logIt("Using configId: " + configId);
            if (configId.equals(nullString)) {
                myJwtConsumer = JwtConsumer.create(null);
            } else {
                myJwtConsumer = JwtConsumer.create(configId);
            }
        } else {
            logIt("Not specifying a configId");
            myJwtConsumer = JwtConsumer.create();
        }
        return myJwtConsumer;
    }

    private void handleException(HttpServletResponse response, Exception e) throws IOException {

        System.out.println(e.getMessage());
        logIt("Caught an exception calling external App: " + e.toString()); // this is probably expected
        //pw.close(); // we cannot close it here since it affects the following. Instead of getting 500, we will end up receiving 200.
        //        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());

    }

    private void outputEntry(HttpServletResponse response) throws IOException {
        pw = response.getWriter();
        response.setContentType("text/plain");
        logIt("");
        logIt("*******************  Start of JwtConsumerClient output  ******************* ");
    }

    private void outputExit() {
        logIt("*******************  End of JwtConsumerClient output  ******************* ");
        pw.close();
    }

    private void outputTokenJson(String token) throws Exception {
        if (token == null || token.isEmpty()) {
            logIt("Token string is null or empty");
            return;
        }
        String[] tokenParts = token.split("\\.");
        for (int i = 0; i < tokenParts.length; i++) {
            try {
                String decodedPart = new String(Base64.getDecoder().decode(tokenParts[i]), "UTF-8");
                logIt("Token part[" + i + "]: " + decodedPart);
            } catch (Exception e) {
                logIt("Decoding token part [" + i + "] failed with" + e.getMessage());
            }
        }
    }

    /***
     * Log information to both the server log and add it to the response that will be returned to the caller.
     *
     * @param msg
     *            message to record
     */
    protected void logIt(String msg) {
        System.out.println(msg);
        pw.print(msg + newLine);
    }

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
