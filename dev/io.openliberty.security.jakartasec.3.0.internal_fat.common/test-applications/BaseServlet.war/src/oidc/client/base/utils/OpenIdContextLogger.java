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
package oidc.client.base.utils;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import io.openliberty.security.jakartasec.fat.utils.ServletMessageConstants;
import jakarta.json.JsonObject;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;
import jakarta.security.enterprise.identitystore.openid.AccessToken;
import jakarta.security.enterprise.identitystore.openid.IdentityToken;
import jakarta.security.enterprise.identitystore.openid.OpenIdClaims;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;
import jakarta.security.enterprise.identitystore.openid.RefreshToken;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class OpenIdContextLogger {

    protected String caller = null;
    protected OpenIdContext context = null;
    HttpServletRequest req;
    HttpServletResponse resp;

    public OpenIdContextLogger(HttpServletRequest request, HttpServletResponse response, String callingClass, OpenIdContext openidContext) {

        req = request;
        resp = response;
        caller = callingClass;
        context = openidContext;

    }

    public void logContext(ServletOutputStream ps) throws IOException {

        ServletLogger.printSeparator(ps);
        ServletLogger.printLine(ps, caller, "Recording the content of the OpenIdContext");

        if (context == null) {
            ServletLogger.printLine(ps, caller, "OpenIdContext: null");
            return;

        }
        ServletLogger.printLine(ps, caller, "OpenIdContext: " + context);

        /*
         * // Method descriptor #18 ()Ljakarta/json/JsonObject;
         * public abstract jakarta.json.JsonObject getProviderMetadata();
         *
         * // Method descriptor #23
         * (Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;Ljava/lang/String;)Ljava/util/
         * Optional;
         * // Signature:
         * <T:Ljava/lang/Object;>(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;Ljava/lang/
         * String;)Ljava/util/Optional<TT;>;
         * public abstract java.util.Optional getStoredValue(jakarta.servlet.http.HttpServletRequest arg0,
         * jakarta.servlet.http.HttpServletResponse arg1, java.lang.String arg2);
         * }
         */

        logProviderMetadata(ps);

        logSubject(ps);

        logAccessTokenClaims(ps);

        logIdTokenClaims(ps);

        logRefreshTokenClaims(ps);

        logUserinfoClaims(ps);

        // TODO logJsonClaims(ps);

        logExpiresIn(ps);

        logTokenType(ps);

        // TODO compare atClaims to jsonClaims - should they be the same?

        // TODO what can we validate with getStoredValue???
        logStoredValues(ps);

        ServletLogger.printSeparator(ps);
    }

    protected void logSubject(ServletOutputStream ps) throws IOException {

        String claimsSub = null;
        String contextSub = context.getSubject();
        ServletLogger.printLine(ps, caller, ServletMessageConstants.CONTEXT_SUBJECT + contextSub);

        OpenIdClaims claims = context.getClaims();
        if (claims != null) {
            try {
                claimsSub = claims.getSubject();
            } catch (java.lang.IllegalArgumentException e) {
                claimsSub = null;
            }
            ServletLogger.printLine(ps, caller, ServletMessageConstants.CLAIMS_SUBJECT + claimsSub);
        } else {
            ServletLogger.printLine(ps, caller, ServletMessageConstants.SUBS_MISMATCH_NULL);
        }

        // compare context subject to cliams subject???
        if (contextSub == null && claimsSub == null) {
            ServletLogger.printLine(ps, caller, ServletMessageConstants.SUBS_MISMATCH_BOTH_NULL);
        } else {
            if (claimsSub == null) {
                ServletLogger.printLine(ps, caller, ServletMessageConstants.SUBS_CLAIMS_SUB_NULL + contextSub);
            } else {
                if (claimsSub.equals(contextSub)) {
                    ServletLogger.printLine(ps, caller, ServletMessageConstants.SUBS_MATCH);
                } else {
                    ServletLogger.printLine(ps, caller, ServletMessageConstants.SUBS_MISMATCH_PART1 + claimsSub + ServletMessageConstants.SUBS_MISMATCH_PART2 + contextSub);
                }
            }
        }

    }

    protected void logAccessTokenClaims(ServletOutputStream ps) throws IOException {

        AccessToken accessToken = context.getAccessToken();
        if (accessToken != null) {
            Map<String, Object> atClaims = accessToken.getClaims();
            ServletLogger.printLine(ps, caller, ServletMessageConstants.ACCESS_TOKEN + accessToken.getToken());
            for (Map.Entry<String, Object> entry : atClaims.entrySet()) {
                ServletLogger.printLine(ps, caller, ServletMessageConstants.ACCESS_TOKEN + ServletMessageConstants.CLAIM + ServletMessageConstants.KEY + entry.getKey()
                                                    + " " + ServletMessageConstants.VALUE + entry.getValue());
            }
        } else {
            ServletLogger.printLine(ps, caller, ServletMessageConstants.ACCESS_TOKEN + ServletMessageConstants.NULL);
        }
    }

    protected void logIdTokenClaims(ServletOutputStream ps) throws IOException {

        IdentityToken idToken = context.getIdentityToken();
        if (idToken != null) {
            Map<String, Object> idClaims = idToken.getClaims();
            ServletLogger.printLine(ps, caller, ServletMessageConstants.ID_TOKEN + idToken.getToken());
            for (Map.Entry<String, Object> entry : idClaims.entrySet()) {
                ServletLogger.printLine(ps, caller, "Identity Token: Claim: Key: " + entry.getKey() + " " + ServletMessageConstants.VALUE + entry.getValue());
                ServletLogger.printLine(ps, caller, ServletMessageConstants.ID_TOKEN + ServletMessageConstants.CLAIM + ServletMessageConstants.KEY + entry.getKey()
                                                    + " " + ServletMessageConstants.VALUE + entry.getValue());
            }
        } else {
            ServletLogger.printLine(ps, caller, ServletMessageConstants.ID_TOKEN + ServletMessageConstants.NULL);
        }
    }

    protected void logRefreshTokenClaims(ServletOutputStream ps) throws IOException {

        Optional<RefreshToken> refreshToken = context.getRefreshToken();
        if (refreshToken != null && refreshToken.isPresent()) {
//            if (refreshToken != null && refreshToken.get() != null) {
            RefreshToken tokenContent = refreshToken.get();
//            ServletLogger.printLine(ps, caller, ServletMessageConstants.REFRESH_TOKEN + ServletMessageConstants.RAW + tokenContent.toString());
            ServletLogger.printLine(ps, caller, ServletMessageConstants.REFRESH_TOKEN + tokenContent.getToken());
        } else {
            ServletLogger.printLine(ps, caller, ServletMessageConstants.REFRESH_TOKEN + ServletMessageConstants.NULL);
        }
    }

    protected void logJsonClaims(ServletOutputStream ps) throws IOException {

        JsonObject claimsJson = context.getClaimsJson();
        if (claimsJson != null) {
            ServletLogger.printLine(ps, caller, ServletMessageConstants.JSON_CLAIMS + claimsJson.toString());
            // TODO update once I can see what the data really looks like
            //for (String key : claims.get)
        } else {
            ServletLogger.printLine(ps, caller, ServletMessageConstants.JSON_CLAIMS + ServletMessageConstants.NULL);
        }
    }

    protected void logExpiresIn(ServletOutputStream ps) throws IOException {

        Optional<Long> expiresIn = context.getExpiresIn();
        if (expiresIn != null) {
            ServletLogger.printLine(ps, caller, ServletMessageConstants.EXPIRES_IN + expiresIn.toString());
            // TODO update once we see what're we're getting and what else we could to with the value.
        } else {
            ServletLogger.printLine(ps, caller, ServletMessageConstants.EXPIRES_IN + ServletMessageConstants.NULL);
        }

    }

    protected void logTokenType(ServletOutputStream ps) throws IOException {

        ServletLogger.printLine(ps, caller, ServletMessageConstants.TOKEN_TYPE + context.getTokenType());

    }

    protected void logUserinfoClaims(ServletOutputStream ps) throws IOException {

        // TODO - do something with this
        OpenIdClaims claims = context.getClaims();
        if (claims == null) {
            ServletLogger.printLine(ps, caller, ServletMessageConstants.NULL_CLAIMS);
        } else {
            // profile claims
            ServletLogger.printLine(ps, caller,
                                    ServletMessageConstants.USERINFO + ServletMessageConstants.KEY + OpenIdConstant.NAME + " " + ServletMessageConstants.VALUE + claims.getName());
            ServletLogger.printLine(ps, caller, ServletMessageConstants.USERINFO + ServletMessageConstants.KEY + OpenIdConstant.FAMILY_NAME + " " + ServletMessageConstants.VALUE
                                                + claims.getFamilyName());
            ServletLogger.printLine(ps, caller, ServletMessageConstants.USERINFO + ServletMessageConstants.KEY + OpenIdConstant.GIVEN_NAME + " " + ServletMessageConstants.VALUE
                                                + claims.getGivenName());
            ServletLogger.printLine(ps, caller, ServletMessageConstants.USERINFO + ServletMessageConstants.KEY + OpenIdConstant.MIDDLE_NAME + " " + ServletMessageConstants.VALUE
                                                + claims.getMiddleName());
            ServletLogger.printLine(ps, caller, ServletMessageConstants.USERINFO + ServletMessageConstants.KEY + OpenIdConstant.NICKNAME + " " + ServletMessageConstants.VALUE
                                                + claims.getNickname());
            ServletLogger.printLine(ps, caller, ServletMessageConstants.USERINFO + ServletMessageConstants.KEY + OpenIdConstant.PREFERRED_USERNAME + " "
                                                + ServletMessageConstants.VALUE + claims.getPreferredUsername());
            ServletLogger.printLine(ps, caller, ServletMessageConstants.USERINFO + ServletMessageConstants.KEY + OpenIdConstant.PROFILE + " " + ServletMessageConstants.VALUE
                                                + claims.getProfile());
            ServletLogger.printLine(ps, caller, ServletMessageConstants.USERINFO + ServletMessageConstants.KEY + OpenIdConstant.PICTURE + " " + ServletMessageConstants.VALUE
                                                + claims.getPicture());
            ServletLogger.printLine(ps, caller, ServletMessageConstants.USERINFO + ServletMessageConstants.KEY + OpenIdConstant.WEBSITE + " " + ServletMessageConstants.VALUE
                                                + claims.getWebsite());
            ServletLogger.printLine(ps, caller, ServletMessageConstants.USERINFO + ServletMessageConstants.KEY + OpenIdConstant.GENDER + " " + ServletMessageConstants.VALUE
                                                + claims.getGender());
            ServletLogger.printLine(ps, caller, ServletMessageConstants.USERINFO + ServletMessageConstants.KEY + OpenIdConstant.BIRTHDATE + " " + ServletMessageConstants.VALUE
                                                + claims.getBirthdate());
            ServletLogger.printLine(ps, caller, ServletMessageConstants.USERINFO + ServletMessageConstants.KEY + OpenIdConstant.ZONEINFO + " " + ServletMessageConstants.VALUE
                                                + claims.getZoneinfo());
            ServletLogger.printLine(ps, caller, ServletMessageConstants.USERINFO + ServletMessageConstants.KEY + OpenIdConstant.LOCALE + " " + ServletMessageConstants.VALUE
                                                + claims.getLocale());
            ServletLogger.printLine(ps, caller, ServletMessageConstants.USERINFO + ServletMessageConstants.KEY + OpenIdConstant.UPDATED_AT + " " + ServletMessageConstants.VALUE
                                                + claims.getUpdatedAt());

            // email claims
            ServletLogger.printLine(ps, caller, ServletMessageConstants.USERINFO + ServletMessageConstants.KEY + OpenIdConstant.EMAIL + " " + ServletMessageConstants.VALUE
                                                + claims.getEmail());
            ServletLogger.printLine(ps, caller, ServletMessageConstants.USERINFO + ServletMessageConstants.KEY + OpenIdConstant.EMAIL_VERIFIED + " " + ServletMessageConstants.VALUE
                                                + claims.getEmailVerified());

            // address claims
            ServletLogger.printLine(ps, caller, ServletMessageConstants.USERINFO + ServletMessageConstants.KEY + OpenIdConstant.ADDRESS + " " + ServletMessageConstants.VALUE
                                                + claims.getAddress());

            // phone claims
            ServletLogger.printLine(ps, caller, ServletMessageConstants.USERINFO + ServletMessageConstants.KEY + OpenIdConstant.PHONE_NUMBER + " " + ServletMessageConstants.VALUE
                                                + claims.getPhoneNumber());
            ServletLogger.printLine(ps, caller, ServletMessageConstants.USERINFO + ServletMessageConstants.KEY + OpenIdConstant.PHONE_NUMBER_VERIFIED + " "
                                                + ServletMessageConstants.VALUE + claims.getPhoneNumberVerified());
        }
        // TODO - do something with the claims - many individual get methods...

    }

    protected void logStoredValues(ServletOutputStream ps) throws IOException {

        logStoredValue(ps, OpenIdConstant.ORIGINAL_REQUEST);
//        logStoredValue(ps, OpenIdConstant.SUBJECT_IDENTIFIER);
//        logStoredValue(ps, OpenIdConstant.CLIENT_ID);
//        logStoredValue(ps, OpenIdConstant.CLIENT_SECRET);
        // additional values
        // TODO try to use some unknown value to show that we fail gracefully.

    }

    protected void logStoredValue(ServletOutputStream ps, String storedValue) throws IOException {

        System.out.println("req: " + req);
        System.out.println("resp: " + resp);
        System.out.println("storedValue: " + storedValue);

        ServletLogger.printLine(ps, caller, ServletMessageConstants.STORED_VALUE + storedValue + ":" + context.getStoredValue(req, resp, storedValue));

    }

    public void logProviderMetadata(ServletOutputStream ps) throws IOException {

        JsonObject pmd = context.getProviderMetadata();

        if (pmd != null) {
            ServletLogger.printLine(ps, caller, ServletMessageConstants.PROVIDER_METADATA + pmd.toString());
            // TODO update once I can see what the data really looks like
            //for (String key : claims.get)
        } else {
            ServletLogger.printLine(ps, caller, ServletMessageConstants.PROVIDER_METADATA + ServletMessageConstants.NULL);
        }
    }

//        JsonArray pmdArray = pmd.asJsonArray();
//
//        JsonValue boo = pmdArray.get(0);
//
//    }

}
