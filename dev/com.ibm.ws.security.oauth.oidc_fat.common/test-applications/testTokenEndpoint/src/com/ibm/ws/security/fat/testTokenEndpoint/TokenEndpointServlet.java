/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.testTokenEndpoint;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.security.Key;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.JwtBuilder;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.fat.common.utils.KeyTools;

public class TokenEndpointServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final String servletName = "TokenEndpointServlet";
    private String token = null;

    public TokenEndpointServlet() {
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleSaveTokenRequest(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleReturnTokenRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleReturnTokenRequest(req, resp);
    }

    /**
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    protected void handleSaveTokenRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        PrintWriter writer = resp.getWriter();
        JwtBuilder builder = null;
        JwtToken builtToken = null;

        Map<String, String[]> foo = req.getParameterMap();
        foo.entrySet().iterator();
        Iterator<Entry<String, String[]>> itr = req.getParameterMap().entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, String[]> entry = itr.next();
            System.out.println("Parm: " + entry.getKey() + " with Value: " + req.getParameter(entry.getKey()));
        }

        String builderId = req.getParameter("builderId");
        System.out.println("Using builderId: " + builderId);
        try {
            token = req.getParameter("overrideToken");
            if (token == null) { // if the calling test hacked up a token that we want to use, skip creating a new token
                if (builderId == null) {
                    // just use the default builder
                    builder = JwtBuilder.create();
                } else {
                    builder = JwtBuilder.create(builderId);
                }
                builder.claim("test", "token for testing");
                builder.claim("at_hash", "dummy_hash_value");
                builder.claim("uniqueSecurityName", "testuser");
                builder.claim("realmName", "BasicRealm");
                builder.subject("testuser");
                //System.out.println("Token value: " + builder.toString());
                setEncryptWith(builder, req);
                builtToken = builder.buildJwt();
                token = builtToken.compact();
                //            token = hackHeader(writer, token, req);
            }
        } catch (Exception e) {
            writer.println(e);
            throw new ServletException(e.toString());
        }
        // save the token value passed for the next get call
        // test cases will save a token and the RP will
        //        token = req.getParameter("tokenToSave");
        System.out.println("Saving token: " + token);

        writer.println("ServletName: " + servletName);
        writer.println("builderId: " + builderId);
        writer.println("token to save: " + token);
        if (builder != null) {
            dumpTokenContents(writer, builtToken);
        }
        writer.flush();
        writer.close();
    }

    /**
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    protected void handleReturnTokenRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("Returning token: " + token);

        JSONObject theResponse = new JSONObject();
        theResponse.put("access_token", token);
        theResponse.put("token_type", "Bearer");
        theResponse.put("expires_in", 7199);
        theResponse.put("scope", "openid profile");
        theResponse.put("refresh_token", "21MhoIC95diaQo9tb5UpFBDFlHh45NixhcKkCwRipszH6WIzKz");
        theResponse.put("id_token", token);

        PrintWriter writer = resp.getWriter();
        //        writer.println("ServletName: " + servletName);
        writer.println(theResponse);
        writer.flush();
        writer.close();

    }

    public void dumpTokenContents(PrintWriter pw, JwtToken jwtToken) throws IOException {
        if (token == null) {
            pw.println("Token was null");
            return;
        }

        String[] tokenParts = token.split("\\.");
        if (tokenParts == null) {
            pw.println("Token array is null");
            return;
        }

        String decodedHeader = new String(Base64.getDecoder().decode(tokenParts[0]), "UTF-8");
        pw.println("Decoded header: " + decodedHeader);

        JsonObject headerInfo = Json.createReader(new StringReader(decodedHeader)).readObject();//JsonObject.parse(decodedHeader);
        Set<String> headerKeys = headerInfo.keySet();
        for (String key : headerKeys) {
            pw.println("Header: Key: " + key + " value: " + headerInfo.get(key));
        }

        Claims theClaims = jwtToken.getClaims();
        if (theClaims == null) {
            pw.println("Token contained no claims");
            return;
        }

        // Print everything that is in the payload
        String jString = theClaims.toJsonString();
        pw.println("JSON String: " + jString);

        if (jString != null) {
            JsonObject jObject = Json.createReader(new StringReader(jString)).readObject();
            Set<String> claimKeys = jObject.keySet();
            for (String key : claimKeys) {
                pw.println("Claim: Key: " + key + " value: " + jObject.get(key));
            }
        } else {
            pw.println("Claim json string is null");
        }
    }

    /**
     * Some of the negative test cases need to create a token with values that the builder configs do not support.
     * We need some extra code to build these tokens.
     * The builder will accept values via the set methods that the metadata does not currently allow such as:
     * keyManagementKeyAlgorithm=RSA-AOEP-256
     * contentEncryptionAlgorithm=A192GCM
     *
     * @param builder
     * @param keyMgmtAlg
     * @param encryptKeyString
     * @param contentEncryptAlg
     * @throws Exception
     */
    protected void setEncryptWith(JwtBuilder builder, HttpServletRequest req) throws Exception {

        String keyMgmtAlg = null;
        String contentEncryptAlg = null;
        String encryptKeyString = null;
        Key encryptKey = null;

        keyMgmtAlg = req.getParameter("keyManagementAlg");
        contentEncryptAlg = req.getParameter("contentEncryptionAlg");
        encryptKeyString = req.getParameter("encrypt_key");

        if (keyMgmtAlg != null || encryptKeyString != null || contentEncryptAlg != null) {

            //            if (keyMgmtAlg != null || encryptKeyString != null || contentEncryptAlg != null) {
            if (encryptKeyString != null) {
                encryptKey = KeyTools.getKeyFromPem(encryptKeyString);
            }
            System.out.println("Calling encryptWith with parms: keyManagementAlg=" + keyMgmtAlg + ", keyManagementKey=" + encryptKey + ", contentEncryptionAlg="
                               + contentEncryptAlg);
            // encryptWith will use default values for all but the actual key - that must be passed
            builder.encryptWith(keyMgmtAlg, encryptKey, contentEncryptAlg);
            //            }
        } else {
            System.out.println("Not explicitly updating the encryption settings");
        }

    }

    //    protected String hackHeader(PrintWriter pw, String origToken, HttpServletRequest req) throws Exception {
    //
    //        String type = req.getParameter("type");
    //        String contentType = req.getParameter("contentType");
    //
    //        if (type != null || contentType != null) {
    //            if (token == null) {
    //                pw.println("Token was null");
    //                throw new Exception("Null token was passed");
    //            }
    //
    //            String[] tokenParts = token.split("\\.");
    //            if (tokenParts == null) {
    //                pw.println("Token array is null");
    //                throw new Exception("Split token was null Null");
    //            }
    //
    //            //            String decodedHeader = new String(Base64.getDecoder().decode(tokenParts[0]), "UTF-8");
    //            //            pw.println("Decoded header: " + decodedHeader);
    //            //
    //            //            JsonObject headerInfo = Json.createReader(new StringReader(decodedHeader)).readObject();
    //            JsonWebEncryption _origjwe = (JsonWebEncryption) JsonWebEncryption.fromCompactSerialization(origToken);
    //            JsonWebEncryption _jwe = new JsonWebEncryption();
    //            //            Headers foo = _origjwe.getHeaders();
    //
    //            //Set<String> headerKeys = headerInfo.keySet();
    //            if (type != null) {
    //                //                headerInfo.put("typ", JsonValue.class.cast(type)) ;
    //                _jwe.setHeader("typ", type);
    //                //                foo.setStringHeaderValue("typ", type);
    //            }
    //            if (contentType != null) {
    //                //                headerInfo.put("cty", JsonValue.class.cast(contentType));
    //                _jwe.setHeader("cty", contentType);
    //                //                foo.setStringHeaderValue("cty", contentType);
    //            }
    //            //            String encryptKeyString = req.getParameter("encrypt_key");
    //            //            if (encryptKeyString != null) {
    //            //                Key key = KeyTools.getKeyFromPem(encryptKeyString);
    //            //                _jwe.setKey(key);
    //            //            }
    //            _jwe.setPayload(_origjwe.getPayload());
    //            _jwe.setKey(_origjwe.getKey());
    //            return _jwe.getCompactSerialization();
    //        } else {
    //            return origToken;
    //        }
    //    }

}
