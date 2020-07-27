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
package com.ibm.ws.security.mp.jwt11.fat.actions;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.actions.JwtTokenActions;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;

import componenttest.topology.impl.LibertyServer;

public class MpJwtFatActions extends JwtTokenActions {

    protected static Class<?> thisClass = MpJwtFatActions.class;

    protected final String defaultUser = MpJwtFatConstants.TESTUSER;
    protected final String defaultPassword = MpJwtFatConstants.TESTUSERPWD;

    public String getJwtFromTokenEndpoint(String testName, String builderId, String builderUrlBase, String user, String pw) throws MalformedURLException, Exception {
        WebRequest request = buildJwtTokenEndpointRequest(builderId, builderUrlBase, user, pw);

        Log.info(thisClass, "getJwtFromTokenEndpoint", "Request: " + request.toString());
        WebClient wc = new WebClient();
        wc.getOptions().setUseInsecureSSL(true);

        Page response = submitRequest(testName, wc, request);
        Log.info(thisClass, testName, "Response: " + WebResponseUtils.getResponseText(response));

        return extractJwtFromTokenEndpointResponse(response);
    }

    public WebRequest buildJwtTokenEndpointRequest(String builderId, String builderUrlBase, String user, String pw) throws MalformedURLException {
        String jwtTokenEndpoint = "/jwt/ibm/api/" + "%s" + "/token";
        String jwtBuilderUrl = builderUrlBase + String.format(jwtTokenEndpoint, builderId);
        Log.info(thisClass, "buildJwtTokenEndpointRequest", "Request: " + jwtBuilderUrl);

        WebRequest request = new WebRequest(new URL(jwtBuilderUrl));
        // Token endpoint requires authentication, so provide credentials
        request.setAdditionalHeader("Authorization", "Basic " + Base64Coder.base64Encode(user + ":" + pw));
        return request;
    }

    /**
     * JWT /token endpoint should return a JSON object whose only key, "token", stores the JWT built by the builder.
     */
    public String extractJwtFromTokenEndpointResponse(Page response) throws Exception {
        JsonReader reader = Json.createReader(new StringReader(WebResponseUtils.getResponseText(response)));
        JsonObject jsonResponse = reader.readObject();
        return jsonResponse.getString("token");
    }

    public Page invokeUrlWithBearerTokenAndParms(String currentTest, WebClient wc, String url, String token, List<NameValuePair> requestParams) throws Exception {
        String thisMethod = "invokeUrlWithBearerToken";
        loggingUtils.printMethodName(thisMethod);
        try {
            WebRequest request = createPostRequest(url);
            request.setAdditionalHeader("Authorization", "Bearer " + token);
            if (requestParams != null) {
                request.setRequestParameters(requestParams);
            }
            return submitRequest(currentTest, wc, request);
        } catch (Exception e) {
            throw new Exception("An error occurred invoking the URL [" + url + "]: " + e);
        }
    }

    public String getDefaultJwtToken(String testName, LibertyServer server) throws Exception {
        String builtToken = getJwtFromTokenEndpoint(testName, "defaultJWT", SecurityFatHttpUtils.getServerSecureUrlBase(server), defaultUser, defaultPassword);
        Log.info(thisClass, testName, "JWT Token: " + builtToken);
        return builtToken;
    }

    public String getJwtTokenUsingBuilder(String testName, LibertyServer server) throws Exception {

        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair(JwtConstants.PARAM_UPN, defaultUser));
        return getJwtTokenUsingBuilder(testName, server, "defaultJWT_withAudience", extraClaims);
    }

    public String getJwtTokenUsingBuilder(String testName, LibertyServer server, String builderId) throws Exception {
        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair(JwtConstants.PARAM_UPN, defaultUser));
        return getJwtTokenUsingBuilder(testName, server, builderId, extraClaims);
    }

    // anyone calling this method needs to add upn to the extraClaims that it passes in (if they need it)
    public String getJwtTokenUsingBuilder(String testName, LibertyServer server, List<NameValuePair> extraClaims) throws Exception {
        return getJwtTokenUsingBuilder(testName, server, "defaultJWT_withAudience", extraClaims);
    }
}
