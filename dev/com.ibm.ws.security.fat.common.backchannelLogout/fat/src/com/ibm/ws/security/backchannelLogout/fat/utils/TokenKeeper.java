/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.backchannelLogout.fat.utils;

import java.util.List;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonValidationTools;

public class TokenKeeper {

    protected static Class<?> thisClass = TokenKeeper.class;

    public static CommonValidationTools validationTools = new CommonValidationTools();

    private String access_token = null;
    private String refresh_token = null;
    private String id_token = null;
    private JwtClaims headerClaims = null;
    private JwtClaims payloadClaims = null;
    private String opCookie = null;
    private String clientCookie = null;
    private String client2Cookie = null;
    private String opJSessionId = null;
    private String clientJSessionId = null;
    private String client2JSessionId = null;

    public TokenKeeper(String token) throws Exception {

        access_token = null;
        refresh_token = null;
        id_token = token;
        headerClaims = parseHeader(id_token);
        payloadClaims = parseClaims(id_token);
        opCookie = null;
        clientCookie = null;
        client2Cookie = null;
        opJSessionId = null;
        clientJSessionId = null;
        client2JSessionId = null;

    }

    public TokenKeeper(WebClient webClient) throws Exception {

        access_token = null;
        refresh_token = null;
        id_token = null;
        headerClaims = null;
        payloadClaims = null;
        opCookie = getCookieValue(webClient, Constants.opCookieName);
        clientCookie = getCookieValue(webClient, Constants.clientCookieName);
        client2Cookie = getCookieValue(webClient, Constants.client2CookieName);
        opJSessionId = getCookieValue(webClient, Constants.opJSessionIdName);
        clientJSessionId = getCookieValue(webClient, Constants.clientJSessionIdName);
        client2JSessionId = getCookieValue(webClient, Constants.client2JSessionIdName);

    }

    public TokenKeeper(WebClient webClient, Object response, String flowType) throws Exception {

        access_token = getAccessTokenFromResponse(response);
        refresh_token = getRefreshTokenFromResponse(response);
        id_token = getIdTokenFromResponse(response, flowType);
        headerClaims = parseHeader(id_token);
        payloadClaims = parseClaims(id_token);
        opCookie = getCookieValue(webClient, Constants.opCookieName);
        clientCookie = getCookieValue(webClient, Constants.clientCookieName);
        client2Cookie = getCookieValue(webClient, Constants.client2CookieName);
        opJSessionId = getCookieValue(webClient, Constants.opJSessionIdName);
        clientJSessionId = getCookieValue(webClient, Constants.clientJSessionIdName);
        client2JSessionId = getCookieValue(webClient, Constants.client2JSessionIdName);

    }

    private String getRefreshTokenFromResponse(Object response) throws Exception {

        String refreshToken = validationTools.getTokenFromResponse(response, Constants.REFRESH_TOKEN_KEY);
        Log.info(thisClass, "getRefreshTokenFromResponse", "raw Refresh Token: " + refreshToken);
        if (refreshToken != null && refreshToken.endsWith("}")) {
            Log.info(thisClass, "getRefreshTokenFromResponse", "refreshToken length: " + Integer.toString(refreshToken.length()));
            refreshToken = refreshToken.substring(0, refreshToken.length() - 1);
            Log.info(thisClass, "getRefreshTokenFromResponse", "fixed Refresh Token: " + refreshToken);
        }
        return refreshToken;

    }

    private String getIdTokenFromResponse(Object response, String flowType) throws Exception {

        String idTokenKey = Constants.ID_TOKEN_KEY;

        if (!flowType.equals(Constants.RP_FLOW)) {
            idTokenKey = "ID token";
        }
        String idToken = validationTools.getTokenFromResponse(response, idTokenKey);
        Log.info(thisClass, "getIdToken", "id_token:  " + idToken);
        return idToken;

    }

    private String getAccessTokenFromResponse(Object response) throws Exception {

        String accessToken = validationTools.getTokenFromResponse(response, Constants.ACCESS_TOKEN_KEY);
        Log.info(thisClass, "getAccessToken", "access_token:  " + accessToken);
        return accessToken;

    }

    public JwtClaims parseHeader(String jwtTokenString) throws Exception, InvalidJwtException {

        Log.info(thisClass, "getHeaderAlg", "Original JWS Token String: " + jwtTokenString);
        JwtClaims jwtClaims = new JwtClaims();

        try {

            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setSkipAllValidators()
                    .setDisableRequireSignature()
                    .setSkipSignatureVerification()
                    .build();

            JwtContext context = jwtConsumer.process(jwtTokenString);
            List<JsonWebStructure> jsonStructures = context.getJoseObjects();
            if (jsonStructures == null || jsonStructures.isEmpty()) {
                throw new Exception("Invalid JsonWebStructure");
            }
            JsonWebStructure jsonStruct = jsonStructures.get(0);

            jwtClaims.setClaim(Constants.HEADER_ALGORITHM, jsonStruct.getAlgorithmHeaderValue());

            Log.info(thisClass, "getHeaderAlg", "JWT consumer populated succeeded! " + jwtClaims);
        } catch (InvalidJwtException e) {
            // InvalidJwtException will be thrown, if the JWT failed processing or validation in anyway.
            // Hopefully with meaningful explanations(s) about what went wrong.
            Log.info(thisClass, "getHeaderAlg", "Invalid JWT! " + e);
            throw e;
        }

        // debug if ever needed
        //        Map<String, Object> claimMap = jwtClaims.getClaimsMap();
        //        for (Map.Entry<String, Object> entry : claimMap.entrySet()) {
        //            Log.info(thisClass, "getHeaderAlg", "Key = " + entry.getKey() + ", Value = " + entry.getValue());
        //            Log.info(thisClass, "getHeaderAlg", "value of type: " + entry.getValue().getClass().toString());
        //        }

        return jwtClaims;

    }

    public JwtClaims parseClaims(String jwtTokenString) throws Exception, InvalidJwtException {

        JwtClaims jwtClaims = null;

        Log.info(thisClass, "getClaims", "Original JWS Token String: " + jwtTokenString);

        try {

            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setSkipAllValidators()
                    .setDisableRequireSignature()
                    .setSkipSignatureVerification()
                    .build();

            jwtClaims = jwtConsumer.process(jwtTokenString).getJwtClaims();
            Log.info(thisClass, "getClaims", "JWT consumer populated succeeded! " + jwtClaims);
        } catch (InvalidJwtException e) {
            // InvalidJwtException will be thrown, if the JWT failed processing or validation in anyway.
            // Hopefully with meaningful explanations(s) about what went wrong.
            Log.info(thisClass, "getClaims", "Invalid JWT! " + e);
            throw e;
        }

        // debug if ever needed
        //        Map<String, Object> claimMap = jwtClaims.getClaimsMap();
        //        for (Map.Entry<String, Object> entry : claimMap.entrySet()) {
        //            Log.info(thisClass, "getClaims", "Key = " + entry.getKey() + ", Value = " + entry.getValue());
        //            Log.info(thisClass, "getClaims", "value of type: " + entry.getValue().getClass().toString());
        //        }

        return jwtClaims;

    }

    public String getCookieValue(WebClient webClient, String cookieName) throws Exception {

        String cookieValue = null;
        CookieManager cm = webClient.getCookieManager();
        if (cm != null) {
            Cookie cookie = cm.getCookie(cookieName);
            if (cookie != null) {
                cookieValue = cookie.getValue();
            }
        }
        Log.info(thisClass, "getCookieValue", "CookieName: " + cookieName + " CookieValue: " + cookieValue);
        return cookieValue;
    }

    public String getRefreshToken() {
        return refresh_token;
    }

    public String getIdToken() {
        return id_token;
    }

    public String getAccessToken() {
        return access_token;
    }

    public JwtClaims getHeaderClaims() {
        return headerClaims;
    }

    public JwtClaims getPayloadClaims() {
        return payloadClaims;
    }

    public String getSessionId() throws Exception {
        return this.getPayloadClaims().getStringClaimValue(Constants.PAYLOAD_SESSION_ID);
    }

    public List<String> getAudience() throws Exception {
        return this.getPayloadClaims().getAudience();
    }

    public String getOPCookie() {
        return opCookie;
    }

    public String getClientCookie() {
        return clientCookie;
    }

    public String getClient2Cookie() {
        return client2Cookie;
    }

    public String getOPJSessionId() {
        return opJSessionId;
    }

    public String getClientJSessionId() {
        return clientJSessionId;
    }

    public String getClient2JSessionId() {
        return client2JSessionId;
    }

}
