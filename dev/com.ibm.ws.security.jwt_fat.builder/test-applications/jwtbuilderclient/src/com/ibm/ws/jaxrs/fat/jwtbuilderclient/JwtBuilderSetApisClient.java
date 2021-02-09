/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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
import java.io.StringReader;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue.ValueType;
import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.websphere.security.jwt.JwtBuilder;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.fat.common.jwt.HeaderConstants;
import com.ibm.ws.security.fat.common.jwt.PayloadConstants;
import com.ibm.ws.security.fat.common.jwt.utils.JWTApiApplicationUtils;
import com.ibm.ws.security.fat.common.utils.KeyTools;
import com.ibm.ws.security.jwt.fat.builder.JWTBuilderConstants;

/**
 * Test application to run the jwtBuilder Apis. This app will create a
 * jwtBuilder, and then invoke all of the methods on the api's that it supports.
 * The test case invoking the app will validate that the specific values
 * processed by the api's are correct. (ie: <claims>.toJsonString() and
 * <claims>.getIssuer() contain that value that the test set)
 */
public class JwtBuilderSetApisClient extends HttpServlet {

    protected JWTApiApplicationUtils appUtils = new JWTApiApplicationUtils();
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
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doWorker(request, response);
        return;
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doWorker(request, response);
        return;
    }

    protected void doWorker(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        System.out.println("Got into the JWT Builder Client");
        appUtils.outputParameters(request);

        runBuilder(request, response);

    }

    private void runBuilder(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {
            pw = appUtils.outputEntry(response, JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT);

            String configId = request.getParameter(JWTBuilderConstants.JWT_BUILDER_PARAM_BUILDER_ID);
            appUtils.logIt(pw, JWTBuilderConstants.JWT_BUILDER_PARAM_BUILDER_ID + ": " + configId);
            // Setting amr in the subject if it is in params
            setAMRInSubject(pw, request);
            // create a builder
            appUtils.logIt(pw, "Calling builder create");
            if (configId != null) {
                myJwtBuilder = JwtBuilder.create(configId);
            } else {
                myJwtBuilder = JwtBuilder.create();
            }

            // now, we need to use the individual set apis to set fields within
            // the builder
            runSetApis(pw, request);

            // we should now have a populated builder - build the token, and log
            // the contents/test the claim apis, ...
            appUtils.logIt(pw, "Calling buildJwt");
            JwtToken newJwtToken = myJwtBuilder.buildJwt();

            appUtils.outputHeader(pw, JWTBuilderConstants.JWT_TOKEN_HEADER, newJwtToken);
            appUtils.outputClaims(pw, JWTBuilderConstants.JWT_CLAIM, newJwtToken);

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

        appUtils.logIt(pw, "Debug: In runSetApis");
        String attrs = request.getParameter("attrs");
        if (attrs == null) {
            return;
        }
        appUtils.logIt(pw, "attrs: " + attrs);
        JsonObject attrObject = Json.createReader(new StringReader(attrs)).readObject();
        appUtils.logIt(pw, "JsonAttrs: " + attrObject);

        runClaimFrom(pw, attrObject);

        setAudience(pw, attrObject);
        setExpirationTime(pw, attrObject);
        setNotBefore(pw, attrObject);
        setJti(pw, attrObject);
        setSubject(pw, attrObject);
        setIssuer(pw, attrObject);
        setSignWith(pw, attrObject);
        setEncryptWith(pw, attrObject);

        setClaims(pw, request.getParameter(JWTBuilderConstants.ADD_CLAIMS_AS), attrObject);

        runFetch(pw, attrObject);

        runRemove(pw, attrObject);

    }

    protected void setAudience(PrintWriter pw, JsonObject attrs) throws Exception {

        appUtils.logIt(pw, "Debug: In setAudience");
        if (attrs.containsKey(PayloadConstants.AUDIENCE)) {
            JsonArray audiences = attrs.getJsonArray(PayloadConstants.AUDIENCE);
            List<String> audiences_list = new ArrayList<String>();
            for (int i = 0; i < audiences.size(); i++) {
                audiences_list.add(getString(audiences, i));
            }
            // appUtils.logIt(pw, "Setting audience to: " +
            // audiences_list.toString());
            myJwtBuilder.audience(audiences_list);
        }

    }

    protected void setExpirationTime(PrintWriter pw, JsonObject attrs) throws Exception {

        appUtils.logIt(pw, "Debug: In setExpiration");
        if (attrs.containsKey(PayloadConstants.EXPIRATION_TIME)) {
            Long expiration = getLong(attrs, PayloadConstants.EXPIRATION_TIME);
            // appUtils.logIt(pw, "Setting expiration to: " + expiration);
            myJwtBuilder.expirationTime(expiration);
        }
    }

    protected void setNotBefore(PrintWriter pw, JsonObject attrs) throws Exception {

        appUtils.logIt(pw, "Debug: In setNotBefore");
        if (attrs.containsKey(PayloadConstants.NOT_BEFORE)) {
            Long notBefore = getLong(attrs, PayloadConstants.NOT_BEFORE);
            // appUtils.logIt(pw, "Setting not before to: " + notBefore);
            myJwtBuilder.notBefore(notBefore);
        }

    }

    protected void setJti(PrintWriter pw, JsonObject attrs) throws Exception {

        appUtils.logIt(pw, "Debug: In setJti");
        if (attrs.containsKey(PayloadConstants.JWT_ID)) {
            boolean jti = attrs.getBoolean(PayloadConstants.JWT_ID);
            // appUtils.logIt(pw, "Setting id to: " + jti);
            myJwtBuilder.jwtId(jti);
        }
    }

    protected void setSubject(PrintWriter pw, JsonObject attrs) throws Exception {

        appUtils.logIt(pw, "Debug: In setSubject");
        if (attrs.containsKey(PayloadConstants.SUBJECT)) {
            String subject = getString(attrs, PayloadConstants.SUBJECT);
            // appUtils.logIt(pw, "Setting subject to: " + subject);
            myJwtBuilder.subject(subject);
        }

    }

    protected void setIssuer(PrintWriter pw, JsonObject attrs) throws Exception {

        appUtils.logIt(pw, "Debug: In setIssuer");
        if (attrs.containsKey(PayloadConstants.ISSUER)) {
            String issuer = getString(attrs, PayloadConstants.ISSUER);
            // appUtils.logIt(pw, "Setting issuer to: " + issuer);
            myJwtBuilder.issuer(issuer);
        }
    }

    protected void setSignWith(PrintWriter pw, JsonObject attrs) throws Exception {

        appUtils.logIt(pw, "Debug: In setSignWith");
        if (attrs.containsKey(HeaderConstants.ALGORITHM)) {
            String alg = getString(attrs, HeaderConstants.ALGORITHM);
            Object key = getObject(attrs, JWTBuilderConstants.SHARED_KEY);
            String keyType = getString(attrs, JWTBuilderConstants.SHARED_KEY_TYPE);
            appUtils.logIt(pw, "Setting alg/key to: " + alg + " " + key);
            if (JWTBuilderConstants.SHARED_KEY_STRING_TYPE.equals(keyType)) {
                appUtils.logIt(pw, "Setting key which is of type String");
                myJwtBuilder.signWith(alg, (String) key);
            } else {
                // if the test case passed a "key", use that value, otherwise,
                // generate one of the type requested.
                if (!attrs.containsKey(JWTBuilderConstants.SHARED_KEY)) {
                    if (JWTBuilderConstants.SHARED_KEY_PRIVATE_KEY_TYPE.equals(keyType)) {
                        key = generatePrivateKey(alg);
                    } else {
                        key = generatePublicKey(alg);
                    }
                }
                myJwtBuilder.signWith(alg, (Key) key);
            }
        }
    }

    private Key generatePrivateKey(String alg) throws Exception {

        int DEFAULT_KEY_SIZE = 2048;
        PrivateKey privKey = null;
        KeyPair keypair = generateKeyPair(DEFAULT_KEY_SIZE, alg);
        if (keypair != null) {
            System.out.println("generatePrivateKey - have keypair");
            privKey = keypair.getPrivate();
        }
        return privKey;
    }

    /**
     * <p>
     * Generates a public key
     *
     * @return - a public key
     * @throws Exception
     */
    private Key generatePublicKey(String alg) throws Exception {

        int DEFAULT_KEY_SIZE = 2048;
        KeyPair keypair = generateKeyPair(DEFAULT_KEY_SIZE, alg);

        if (keypair != null) {
            System.out.println("generatePublicKey - have keypair");
            PublicKey pubKey = keypair.getPublic();
            return pubKey;
        } else {
            return null;
        }
    }

    private KeyPair generateKeyPair(int size, String alg) throws Exception {

        KeyPairGenerator keyGenerator = null;

        try {
            if (alg == null) {
                return null;
            }
            if (alg.startsWith("ES")) {
                keyGenerator = KeyPairGenerator.getInstance("EC");
                String spec = "secp256r1";

                if (alg.contains("384")) {
                    spec = "secp384r1";
                }
                if (alg.contains("512")) {
                    spec = "secp521r1";
                }
                keyGenerator.initialize(new ECGenParameterSpec(spec), new SecureRandom());
            } else {
                keyGenerator = KeyPairGenerator.getInstance("RSA");
                keyGenerator.initialize(size);
            }
        } catch (NoSuchAlgorithmException e) {
            return null;
        }

        KeyPair keypair = keyGenerator.generateKeyPair();

        return keypair;
    }

    protected void setClaims(PrintWriter pw, String addClaimAs, JsonObject attrs) throws Exception {

        appUtils.logIt(pw, "Debug: In setClaims");
        if (attrs.containsKey(JWTBuilderConstants.JWT_BUILDER_CLAIM_API)) {

            Map<String, Object> map = new HashMap<String, Object>();
            JsonObject claimsToAdd = attrs.getJsonObject(JWTBuilderConstants.JWT_BUILDER_CLAIM_API);
            Set<String> keySet = claimsToAdd.keySet();
            Iterator<String> keys = keySet.iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = getObject(claimsToAdd, key);
                appUtils.logIt(pw, "DEBUG: key: " + key + " value: " + value);
                map.put(key, value);
            }
            appUtils.logIt(pw, "Will be adding claims as: " + addClaimAs);
            if (JWTBuilderConstants.AS_COLLECTION.equals(addClaimAs)) {
                if (map.isEmpty()) {
                    myJwtBuilder.claim(null);
                } else {
                    myJwtBuilder.claim(map);
                }
            } else {
                // trick to allow test case to invoke the claim map interface
                // with null for a value
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

    protected void runRemove(PrintWriter pw, JsonObject attrs) throws Exception {

        appUtils.logIt(pw, "Debug: In runRemove");
        if (attrs.containsKey(JWTBuilderConstants.JWT_BUILDER_REMOVE_API)) {

            JsonArray claimsToRemove = attrs.getJsonArray(JWTBuilderConstants.JWT_BUILDER_REMOVE_API);
            appUtils.logIt(pw, "Will be removing claims: " + claimsToRemove.toString());
            for (int i = 0; i < claimsToRemove.size(); i++) {
                myJwtBuilder.remove(getString(claimsToRemove, i));
            }
        }
    }

    protected void runFetch(PrintWriter pw, JsonObject attrs) throws Exception {

        appUtils.logIt(pw, "Debug: In runFetch");
        if (attrs.containsKey(JWTBuilderConstants.JWT_BUILDER_FETCH_API)) {

            JsonArray claimsToFetch = attrs.getJsonArray(JWTBuilderConstants.JWT_BUILDER_FETCH_API);
            appUtils.logIt(pw, "Will be fetching claims: " + claimsToFetch.toString());
            for (int i = 0; i < claimsToFetch.size(); i++) {
                appUtils.logIt(pw, "fetching: " + claimsToFetch.get(i));
                myJwtBuilder.fetch(getString(claimsToFetch, i));
            }
        }
    }

    protected void setAMRInSubject(PrintWriter pw, HttpServletRequest request) throws Exception {

        appUtils.logIt(pw, "Debug: In setAMRInSubject");
        String attrsString = request.getParameter("attrs");
        if (attrsString == null) {
            return;
        }
        JsonObject attrs = Json.createReader(new StringReader(attrsString)).readObject();
        if (attrs.containsKey(PayloadConstants.METHODS_REFERENCE)) {
            String amrAttr = getString(attrs, PayloadConstants.METHODS_REFERENCE);
            Subject callerSubject = WSSubject.getCallerSubject();
            WSCredential callerCredential = getWSCredential(callerSubject);
            if (callerCredential != null) {
                callerCredential.set(amrAttr, "amrTestValue");
                appUtils.logIt(pw, amrAttr + " is set in WSCredential");
            }
        }

    }

    protected WSCredential getWSCredential(Subject subject) {
        WSCredential wsCredential = null;
        if (subject != null) {
            Set<WSCredential> wsCredentials = subject.getPublicCredentials(WSCredential.class);
            Iterator<WSCredential> wsCredentialsIterator = wsCredentials.iterator();
            if (wsCredentialsIterator.hasNext()) {
                wsCredential = wsCredentialsIterator.next();
            }
        }
        return wsCredential;
    }

    protected void runClaimFrom(PrintWriter pw, JsonObject attrs) throws Exception {

        appUtils.logIt(pw, "Debug: In runClaimFrom");
        // if we passed the jwt token, we want to use claimFrom
        // we'll pass in one of the following:
        // JwtToken,
        // JwtTokenString (all 3 parts)
        // JwtTokenString (payload only encoded)
        // JwtTokenString (payload only decoded)

        if (attrs.containsKey(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM)) {

            String action = getString(attrs, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM);
            appUtils.logIt(pw, "passing token as: " + action);

            JsonArray claimsToGet = null;
            if (attrs.containsKey(JWTBuilderConstants.JWT_BUILDER_CLAIMFROM_API)) {
                claimsToGet = attrs.getJsonArray(JWTBuilderConstants.JWT_BUILDER_CLAIMFROM_API);
            }
            switch (action) {
            case JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_TOKEN:
                String configId = getString(attrs, JWTBuilderConstants.JWT_BUILDER_PARAM_BUILDER_ID);
                appUtils.logIt(pw,
                        JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM + " alternate config: " + configId);
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
                runClaimFromWithJwtString(getString(attrs, JWTBuilderConstants.JWT_TOKEN), claimsToGet);
                break;
            }

        }
    }

    protected void runClaimFromWithJwtToken(JwtToken jwtToken, JsonArray claimsToGet) throws Exception {

        appUtils.logIt(pw, "Debug: In runClaimFromWithJwtToken");
        if (claimsToGet == null) {
            myJwtBuilder.claimFrom(jwtToken);
        } else {
            for (int i = 0; i < claimsToGet.size(); i++) {
                myJwtBuilder.claimFrom(jwtToken, getString(claimsToGet, i));
            }
        }
    }

    protected void runClaimFromWithJwtString(String jwtToken, JsonArray claimsToGet) throws Exception {

        appUtils.logIt(pw, "Debug: In runClaimFromWithJwtString");
        if (claimsToGet == null) {
            myJwtBuilder.claimFrom(jwtToken);
        } else {
            for (int i = 0; i < claimsToGet.size(); i++) {
                myJwtBuilder.claimFrom(jwtToken, getString(claimsToGet, i));
            }
        }
    }

    /**
     * invoke encryptWith method on the builder if any of the encryption parms
     * are passed
     *
     * @param pw
     *            - print writer
     * @param attrs
     *            - the caller requested parms
     * @throws Exception
     */
    protected void setEncryptWith(PrintWriter pw, JsonObject attrs) throws Exception {

        appUtils.logIt(pw, "Debug: In setEncryptWith");
        if (attrs.containsKey(JWTBuilderConstants.PARAM_KEY_MGMT_ALG)
                || attrs.containsKey(JWTBuilderConstants.PARAM_ENCRYPT_KEY)
                || attrs.containsKey(JWTBuilderConstants.PARAM_CONTENT_ENCRYPT_ALG)) {
            String keyMgmtAlg = getString(attrs, JWTBuilderConstants.PARAM_KEY_MGMT_ALG);
            String encryptKeyString = getString(attrs, JWTBuilderConstants.PARAM_ENCRYPT_KEY);
            String contentEncryptAlg = getString(attrs, JWTBuilderConstants.PARAM_CONTENT_ENCRYPT_ALG);

            // to allow calling test case to test all possible combinations of
            // good/bad values passed to the encryptWith method,
            // allow caller to pass any subset of parms - we'll pass null for
            // any missing
            // this means that for positive tests, caller needs to pass values
            // for all parms even the "defaults"
            if (keyMgmtAlg != null || encryptKeyString != null || contentEncryptAlg != null) {
                Key encryptKey = KeyTools.getKeyFromPem(encryptKeyString);
                appUtils.logIt(pw, "Calling encryptWith with parms: keyManagementAlg=" + keyMgmtAlg
                        + ", keyManagementKey=" + encryptKey + ", contentEncryptionAlg=" + contentEncryptAlg);
                myJwtBuilder.encryptWith(keyMgmtAlg, encryptKey, contentEncryptAlg);
            }
        }
    }

    /**
     * Get a string from a JsonObject. This is equivalent to the following
     * json4j call: <code>(String) JSONObject.get(key)</code>.
     *
     * @param object
     *            JsonObject to get the string object from.
     * @param key
     *            The JSON key to get the String value for.
     * @return The String value if it exists.
     */
    private static String getString(JsonObject object, String key) {
        return (!object.containsKey(key) || object.isNull(key)) ? null : object.getString(key);
    }

    /**
     * Get a string from a JsonArray. This is equivalent to the following
     * json4j call: <code>(String) JSONArray.get(idx)</code>.
     *
     * @param array
     *            JsonArray to get the string object from.
     * @param idx
     *            The idx to get the String value for.
     * @return The String value if it exists.
     */
    private static String getString(JsonArray array, int idx) {
        return array.isNull(idx) ? null : array.getString(idx);
    }

    /**
     * Get a generic Object from a JsonObject. This is equivalent to the following
     * json4j call: <code>JSONArray.get(idx)</code>.
     *
     * @param object
     *            The JsonObject to get the generic object from.
     * @param key
     *            The JSON key to get the Object value for.
     * @return The generic object if it exists.
     */
    private static Object getObject(JsonObject object, String key) {
        if (!object.containsKey(key) || object.isNull(key)) {
            return null;
        }

        /*
         * Numerical value.
         */
        if (object.get(key).getValueType() == ValueType.NUMBER) {
            return object.isNull(key) ? null : object.getJsonNumber(key).longValue();
        }

        /*
         * Treat everything else as String.
         */
        return getString(object, key);
    }

    /**
     * Get a long from a JsonObject. This is equivalent to the following
     * json4j call: <code>(Long) JSONObject.get(key)</code>.
     *
     * @param object
     *            JsonObject to get the Long object from.
     * @param key
     *            The JSON key to get the Long value for.
     * @return The Long value if it exists.
     */
    private static Long getLong(JsonObject object, String key) {
        if (!object.containsKey(key) || object.isNull(key)) {
            return null;
        }

        /*
         * Numerical value.
         */
        if (object.get(key).getValueType() == ValueType.NUMBER) {
            return object.isNull(key) ? null : object.getJsonNumber(key).longValue();
        }

        /*
         * Convert string value to long value.
         */
        return Long.valueOf(object.getString(key));
    }
}
