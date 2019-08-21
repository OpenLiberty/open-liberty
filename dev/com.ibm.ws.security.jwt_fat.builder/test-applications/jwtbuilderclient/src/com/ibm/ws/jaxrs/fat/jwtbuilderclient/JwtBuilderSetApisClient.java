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
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.json.java.JSON;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.security.jwt.JwtBuilder;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.fat.common.jwt.ClaimConstants;
import com.ibm.ws.security.fat.common.jwt.HeaderConstants;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.jwt.fat.builder.JWTApplicationUtils;
import com.ibm.ws.security.jwt.fat.builder.JWTBuilderConstants;

/**
 * Test application to run the jwtBuilder Apis.
 * This app will create a jwtBuilder, and then invoke all of the methods on the api's that it supports.
 * The test case invoking the app will validate that the specific values processed by the api's are correct.
 * (ie: <claims>.toJsonString() and <claims>.getIssuer() contain that value that the test set)
 */
@SuppressWarnings("restriction")
public class JwtBuilderSetApisClient extends HttpServlet {

    protected JWTApplicationUtils appUtils = new JWTApplicationUtils();
    private static final long serialVersionUID = 1L;
    PrintWriter pw = null;
    protected JwtBuilder myJwtBuilder = null;

    protected String jwtTokenString = null;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public JwtBuilderSetApisClient() {
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
            pw = appUtils.outputEntry(response, JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT);

            String configId = request.getParameter(JWTBuilderConstants.JWT_BUILDER_PARAM_BUILDER_ID);
            appUtils.logIt(pw, JWTBuilderConstants.JWT_BUILDER_PARAM_BUILDER_ID + ": " + configId);
            // create a builder
            if (configId != null) {
                myJwtBuilder = JwtBuilder.create(configId);
            } else {
                myJwtBuilder = JwtBuilder.create();
            }

            // now, we need to use the individual set apis to set fields within the builder
            runSetApis(pw, request);

            // we should now have a populated builder - build the token, and log the contents/test the claim apis, ...
            JwtToken newJwtToken = myJwtBuilder.buildJwt();

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

        appUtils.outputExit(pw, JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT);
    }

    protected void runSetApis(PrintWriter pw, HttpServletRequest request) throws Exception {

        String attrs = request.getParameter("attrs");
        if (attrs == null) {
            return;
        }
        appUtils.logIt(pw, "attrs: " + attrs);
        JSONObject attrObject = (JSONObject) JSON.parse(attrs);
        appUtils.logIt(pw, "JsonAttrs: " + attrObject);

        runClaimFrom(pw, attrObject);

        setAudience(pw, attrObject);
        setExpirationTime(pw, attrObject);
        setNotBefore(pw, attrObject);
        setJti(pw, attrObject);
        setSubject(pw, attrObject);
        setIssuer(pw, attrObject);
        setSignWith(pw, attrObject);

        setClaims(pw, request.getParameter(JwtConstants.ADD_CLAIMS_AS), attrObject);

        runFetch(pw, attrObject);

        runRemove(pw, attrObject);

    }

    protected void setAudience(PrintWriter pw, JSONObject attrs) throws Exception {

        if (attrs.containsKey(ClaimConstants.AUDIENCE)) {
            JSONArray audiences = (JSONArray) attrs.get(ClaimConstants.AUDIENCE);
            List<String> audiences_list = new ArrayList<String>();
            for (int i = 0; i < audiences.size(); i++) {
                audiences_list.add((String) audiences.get(i));
            }
            //            appUtils.logIt(pw, "Setting audience to: " + audiences_list.toString());
            myJwtBuilder.audience(audiences_list);
        }

    }

    protected void setExpirationTime(PrintWriter pw, JSONObject attrs) throws Exception {

        if (attrs.containsKey(ClaimConstants.EXPIRATION_TIME)) {
            Long expiration = (Long) attrs.get(ClaimConstants.EXPIRATION_TIME);
            //            appUtils.logIt(pw, "Setting expiration to: " + expiration);
            myJwtBuilder.expirationTime(expiration);
        }
    }

    protected void setNotBefore(PrintWriter pw, JSONObject attrs) throws Exception {

        if (attrs.containsKey(ClaimConstants.NOT_BEFORE)) {
            Long notBefore = (Long) attrs.get(ClaimConstants.NOT_BEFORE);
            //            appUtils.logIt(pw, "Setting not before to: " + notBefore);
            myJwtBuilder.notBefore(notBefore);
        }

    }

    protected void setJti(PrintWriter pw, JSONObject attrs) throws Exception {

        if (attrs.containsKey(ClaimConstants.JWT_ID)) {
            boolean jti = (boolean) attrs.get(ClaimConstants.JWT_ID);
            //            appUtils.logIt(pw, "Setting id to: " + jti);
            myJwtBuilder.jwtId(jti);
        }
    }

    protected void setSubject(PrintWriter pw, JSONObject attrs) throws Exception {

        if (attrs.containsKey(ClaimConstants.SUBJECT)) {
            String subject = (String) attrs.get(ClaimConstants.SUBJECT);
            //            appUtils.logIt(pw, "Setting subject to: " + subject);
            myJwtBuilder.subject(subject);
        }

    }

    protected void setIssuer(PrintWriter pw, JSONObject attrs) throws Exception {

        if (attrs.containsKey(ClaimConstants.ISSUER)) {
            String issuer = (String) attrs.get(ClaimConstants.ISSUER);
            //            appUtils.logIt(pw, "Setting issuer to: " + issuer);
            myJwtBuilder.issuer(issuer);
        }
    }

    protected void setSignWith(PrintWriter pw, JSONObject attrs) throws Exception {

        if (attrs.containsKey(HeaderConstants.ALGORITHM)) {
            String alg = (String) attrs.get(HeaderConstants.ALGORITHM);
            Object key = attrs.get(JwtConstants.SHARED_KEY);
            String keyType = (String) attrs.get(JwtConstants.SHARED_KEY_TYPE);
            appUtils.logIt(pw, "Setting alg/key to: " + alg + " " + key);
            if (JwtConstants.SHARED_KEY_STRING_TYPE.equals(keyType)) {
                appUtils.logIt(pw, "Setting key which is of type String");
                myJwtBuilder.signWith(alg, (String) key);
            } else {
                // if the test case passed a "key", use that value, otherwise,
                // generate one of the type requested.
                if (!attrs.containsKey(JwtConstants.SHARED_KEY)) {
                    if (JwtConstants.SHARED_KEY_PRIVATE_KEY_TYPE.equals(keyType)) {
                        key = generatePrivateKey();
                    } else {
                        key = generatePublicKey();
                    }
                }
                myJwtBuilder.signWith(alg, (Key) key);
            }
        }
    }

    private Key generatePrivateKey() throws Exception {

        int DEFAULT_KEY_SIZE = 2048;

        KeyPair keypair = generateKeyPair(DEFAULT_KEY_SIZE);

        RSAPrivateKey priKey = (RSAPrivateKey) keypair.getPrivate();

        return priKey;
    }

    /**
     * <p>
     * Generates a public key
     *
     * @return - a public key
     * @throws Exception
     */
    private Key generatePublicKey() throws Exception {

        int DEFAULT_KEY_SIZE = 2048;

        KeyPair keypair = generateKeyPair(DEFAULT_KEY_SIZE);

        RSAPublicKey pubKey = (RSAPublicKey) keypair.getPublic();

        return pubKey;
    }

    private KeyPair generateKeyPair(int size) throws Exception {

        KeyPairGenerator keyGenerator = null;
        try {
            keyGenerator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            // This should not happen, since we hardcoded as "RSA"
            return null;
        }

        keyGenerator.initialize(size);
        KeyPair keypair = keyGenerator.generateKeyPair();

        return keypair;
    }

    protected void setClaims(PrintWriter pw, String addClaimAs, JSONObject attrs) throws Exception {

        if (attrs.containsKey(JwtConstants.JWT_BUILDER_CLAIM_API)) {

            Map<String, Object> map = new HashMap<String, Object>();
            JSONObject claimsToAdd = (JSONObject) attrs.get(JwtConstants.JWT_BUILDER_CLAIM_API);
            Set<String> keySet = claimsToAdd.keySet();
            Iterator<String> keys = keySet.iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = claimsToAdd.get(key);
                // need a way for the tests to tell this app to try a bad key value of null
                //                if (JWTBuilderConstants.NULL_STRING.contains(key)) {
                //                    key = JWTBuilderConstants.NULL_VALUE;
                //                }
                //                if (JWTBuilderConstants.EMPTY_STRING.contains(key)) {
                //                    key = JWTBuilderConstants.EMPTY_VALUE;
                //                }
                //                            if (value instanceof JSONArray) {
                //                                value = toList((JSONArray) value);
                //                            } else if (value instanceof JSONObject) {
                //                                value = toMap((JSONObject) value);
                //                            }
                appUtils.logIt(pw, "DEBUG: key: " + key + " value: " + value);
                map.put(key, value);
            }
            appUtils.logIt(pw, "Will be adding claims as: " + addClaimAs);
            if (JwtConstants.AS_COLLECTION.equals(addClaimAs)) {
                if (map.isEmpty()) {
                    myJwtBuilder.claim(null);
                } else {
                    myJwtBuilder.claim(map);
                }
            } else {
                // trick to allow test case to invoke the claim map interface with null for a value
                if (map.isEmpty()) {
                    myJwtBuilder.claim(null, "xxx");
                } else {
                    for (Map.Entry<String, Object> entry : map.entrySet()) {
                        myJwtBuilder.claim(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

    }

    protected void runRemove(PrintWriter pw, JSONObject attrs) throws Exception {

        if (attrs.containsKey(JwtConstants.JWT_BUILDER_REMOVE_API)) {

            JSONArray claimsToRemove = (JSONArray) attrs.get(JwtConstants.JWT_BUILDER_REMOVE_API);
            appUtils.logIt(pw, "Will be removing claims: " + claimsToRemove.toString());
            for (int i = 0; i < claimsToRemove.size(); i++) {
                myJwtBuilder.remove((String) claimsToRemove.get(i));
            }
        }
    }

    protected void runFetch(PrintWriter pw, JSONObject attrs) throws Exception {

        if (attrs.containsKey(JwtConstants.JWT_BUILDER_FETCH_API)) {

            JSONArray claimsToFetch = (JSONArray) attrs.get(JwtConstants.JWT_BUILDER_FETCH_API);
            appUtils.logIt(pw, "Will be fetching claims: " + claimsToFetch.toString());
            for (int i = 0; i < claimsToFetch.size(); i++) {
                appUtils.logIt(pw, "fetching: " + claimsToFetch.get(i));
                myJwtBuilder.fetch((String) claimsToFetch.get(i));
            }
        }
    }

    protected void runClaimFrom(PrintWriter pw, JSONObject attrs) throws Exception {

        // if we passed the jwt token, we want to use claimFrom
        // we'll pass in one of the following:
        // JwtToken,
        // JwtTokenString (all 3 parts)
        // JwtTokenString (payload only encoded)
        // JwtTokenString (payload only decoded)
        // we can process the strings as passed, but, we need to create the JwtToken from the 3 part string if that's what we want to test
        //        if (attrs.containsKey(JWTBuilderConstants.JWT_TOKEN)) {
        //            if (attrs.containsKey(JWTBuilderConstants.PARAM_BUILDER_ID)) {
        //                String jwtTokenString = (String) attrs.get(JWTBuilderConstants.JWT_TOKEN);
        //                String configId = (String) attrs.get(JWTBuilderConstants.PARAM_BUILDER_ID);
        //                JwtConsumer myConsumer = JwtConsumer.create(configId);
        //                JwtToken jwtToken = myConsumer.createJwt(jwtTokenString);
        //                myJwtBuilder.claimFrom(jwtToken);
        //            } else {
        //                String jwtToken = (String) attrs.get(JWTBuilderConstants.JWT_TOKEN);
        //                myJwtBuilder.claimFrom(jwtToken);
        //            }
        //        }

        if (attrs.containsKey(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM)) {

            String action = (String) attrs.get(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM);
            appUtils.logIt(pw, "passing token as: " + action);

            JSONArray claimsToGet = null;
            if (attrs.containsKey(JwtConstants.JWT_BUILDER_CLAIMFROM_API)) {
                claimsToGet = (JSONArray) attrs.get(JwtConstants.JWT_BUILDER_CLAIMFROM_API);
            }
            switch (action) {
            case JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_TOKEN:
                String configId = (String) attrs.get(JWTBuilderConstants.JWT_BUILDER_PARAM_BUILDER_ID);
                appUtils.logIt(pw, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM + " alternate config: " + configId);
                JwtBuilder otherBuilder = JwtBuilder.create(configId);
                JwtToken jwtToken = otherBuilder.buildJwt();
                appUtils.logIt(pw, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM + ": " + jwtToken.compact());
                runClaimFromWithJwtToken(jwtToken, claimsToGet);
                break;
            case JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_TOKEN_NULL:
                JwtToken nullToken = null;
                runClaimFromWithJwtToken(nullToken, claimsToGet);
                break;
            case JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING:
                runClaimFromWithJwtString((String) attrs.get(JWTBuilderConstants.JWT_TOKEN), claimsToGet);
                break;
            }

            /*
             * String configId = (String) attrs.get(JWTBuilderConstants.JWT_BUILDER_PARAM_BUILDER_ID);
             * appUtils.logIt(pw, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM + " alternate config: " + configId);
             * JwtBuilder otherBuilder = JwtBuilder.create(configId);
             * JwtToken jwtToken = otherBuilder.buildJwt();
             * String jwtString = jwtToken.compact();
             * appUtils.logIt(pw, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM + ": " + jwtString);
             * String[] jwtParts = jwtString.split(Pattern.quote("."));
             * String payload = jwtParts[1];
             *
             * String action = (String) attrs.get(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM);
             * appUtils.logIt(pw, "passing token as: " + action);
             *
             * JSONArray claimsToGet = null;
             * if (attrs.containsKey(JwtConstants.JWT_BUILDER_CLAIMFROM_API)) {
             * claimsToGet = (JSONArray) attrs.get(JwtConstants.JWT_BUILDER_CLAIMFROM_API);
             * }
             * switch (action) {
             * case JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_TOKEN:
             * runClaimFromWithJwtToken(jwtToken, claimsToGet);
             * break;
             * case JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING:
             * runClaimFromWithJwtString(jwtString, claimsToGet);
             * break;
             * case JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_ENCODED_PAYLOAD:
             * runClaimFromWithJwtString(payload, claimsToGet);
             * break;
             * case JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_DECODED_PAYLOAD:
             * runClaimFromWithJwtString(new String(Base64.getDecoder().decode(payload), "UTF-8"), claimsToGet);
             * break;
             * case JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_TOKEN_NULL:
             * JwtToken nullToken = null;
             * runClaimFromWithJwtToken(nullToken, claimsToGet);
             * break;
             * case JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING_NULL:
             * String nullString = null;
             * runClaimFromWithJwtString(nullString, claimsToGet);
             * break;
             * case JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING_EMPTY:
             * String emptyString = "";
             * runClaimFromWithJwtString(emptyString, claimsToGet);
             * break;
             * }
             */

        }
    }

    protected void runClaimFromWithJwtToken(JwtToken jwtToken, JSONArray claimsToGet) throws Exception {

        if (claimsToGet == null) {
            myJwtBuilder.claimFrom(jwtToken);
        } else {
            for (int i = 0; i < claimsToGet.size(); i++) {
                myJwtBuilder.claimFrom(jwtToken, (String) claimsToGet.get(i));
            }
        }
    }

    protected void runClaimFromWithJwtString(String jwtToken, JSONArray claimsToGet) throws Exception {

        if (claimsToGet == null) {
            myJwtBuilder.claimFrom(jwtToken);
        } else {
            for (int i = 0; i < claimsToGet.size(); i++) {
                myJwtBuilder.claimFrom(jwtToken, (String) claimsToGet.get(i));
            }
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
