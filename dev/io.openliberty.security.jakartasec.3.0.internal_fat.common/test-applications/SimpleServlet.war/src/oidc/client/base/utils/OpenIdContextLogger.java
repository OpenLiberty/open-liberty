/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package oidc.client.base.utils;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import io.openliberty.security.jakartasec.fat.utils.ServletMessageConstants;
import jakarta.json.JsonObject;
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

        printLine(ps, caller, "Recording the content of the OpenIdContext");

        if (context == null) {
            printLine(ps, caller, "OpenIdContext: null");
            return;

        }

        /*
         * // Method descriptor #18 ()Ljakarta/json/JsonObject;
         * public abstract jakarta.json.JsonObject getProviderMetadata();
         *
         * // Method descriptor #23 (Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;Ljava/lang/String;)Ljava/util/Optional;
         * // Signature: <T:Ljava/lang/Object;>(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;Ljava/lang/String;)Ljava/util/Optional<TT;>;
         * public abstract java.util.Optional getStoredValue(jakarta.servlet.http.HttpServletRequest arg0, jakarta.servlet.http.HttpServletResponse arg1, java.lang.String arg2);
         * }
         */

        logSubject(ps);

        logAccessTokenClaims(ps);

        logIdTokenClaims(ps);

        logRefreshTokenClaims(ps);

        logClaims(ps);

        logJsonClaims(ps);

        logExpiresIn(ps);

        logTokenType(ps);

        // TODO compare atClaims to jsonClaims - should they be the same?

        // TODO what can we validate with getStoredValue???
        logStoredValues(ps);
    }

    protected void logSubject(ServletOutputStream ps) throws IOException {

        String claimsSub = null;
        String contextSub = context.getSubject();
        printLine(ps, caller, ServletMessageConstants.CONTEXT_SUBJECT + contextSub);

        OpenIdClaims claims = context.getClaims();
        if (claims != null) {
            claimsSub = claims.getSubject();
            printLine(ps, caller, ServletMessageConstants.CLAIMS_SUBJECT + claimsSub);
        } else {
            printLine(ps, caller, ServletMessageConstants.SUBS_MISMATCH_NULL);
        }

        // compare context subject to cliams subject???
        if (contextSub == null && claimsSub == null) {
            printLine(ps, caller, ServletMessageConstants.SUBS_MISMATCH_BOTH_NULL);
        } else {
            if (claimsSub == null) {
                printLine(ps, caller, ServletMessageConstants.SUBS_CLAIMS_SUB_NULL + contextSub);
            } else {
                if (claimsSub.equals(contextSub)) {
                    printLine(ps, caller, ServletMessageConstants.SUBS_MATCH);
                } else {
                    printLine(ps, caller, ServletMessageConstants.SUBS_MISMATCH_PART1 + claimsSub + ServletMessageConstants.SUBS_MISMATCH_PART2 + contextSub);
                }
            }
        }

    }

    protected void logAccessTokenClaims(ServletOutputStream ps) throws IOException {

        AccessToken accessToken = context.getAccessToken();
        if (accessToken != null) {
            Map<String, Object> atClaims = accessToken.getClaims();
            printLine(ps, caller, "Access Token: " + accessToken.toString());
            for (Map.Entry<String, Object> entry : atClaims.entrySet()) {
                printLine(ps, caller, "Access Token: Claim: Key: " + entry.getKey() + " Value: " + entry.getValue());
            }
        } else {
            printLine(ps, caller, "Access Token: null");
        }
    }

    protected void logIdTokenClaims(ServletOutputStream ps) throws IOException {

        IdentityToken idToken = context.getIdentityToken();
        if (idToken != null) {
            Map<String, Object> idClaims = idToken.getClaims();
            printLine(ps, caller, "Identity Token: " + idToken.toString());
            for (Map.Entry<String, Object> entry : idClaims.entrySet()) {
                printLine(ps, caller, "Identity Token: Claim: Key: " + entry.getKey() + " Value: " + entry.getValue());
            }
        } else {
            printLine(ps, caller, "Identity Token: null");
        }
    }

    protected void logRefreshTokenClaims(ServletOutputStream ps) throws IOException {

        Optional<RefreshToken> refreshToken = context.getRefreshToken();
        if (refreshToken != null) {
            RefreshToken tokenContent = refreshToken.get();
            printLine(ps, caller, "Refresh Token (raw): " + tokenContent.toString());
            printLine(ps, caller, "Refresh Token: " + tokenContent.getToken());
        } else {
            printLine(ps, caller, "Refresh Token: null");
        }
    }

    protected void logJsonClaims(ServletOutputStream ps) throws IOException {

        JsonObject claimsJson = context.getClaimsJson();
        if (claimsJson != null) {
            printLine(ps, caller, "Json Claims: " + claimsJson.toString());
            // TODO update once I can see what the data really looks like
            //for (String key : claims.get)
        } else {
            printLine(ps, caller, "Json Claims: null");
        }
    }

    protected void logExpiresIn(ServletOutputStream ps) throws IOException {

        Optional<Long> expiresIn = context.getExpiresIn();
        if (expiresIn != null) {
            printLine(ps, caller, "Expires In: " + expiresIn.toString());
            // TODO update once we see what're we're getting and what else we could to with the value.
        } else {
            printLine(ps, caller, "Expires In: null");
        }

    }

    protected void logTokenType(ServletOutputStream ps) throws IOException {

        printLine(ps, caller, "Token Type: " + context.getTokenType());

    }

    protected void logClaims(ServletOutputStream ps) throws IOException {

        // TODO - do something with this
        OpenIdClaims claims = context.getClaims();
        if (claims == null) {
            printLine(ps, caller, "Claims are null");
        }
        // TODO - do something with the claims - many individual get methods...

    }

    protected void logStoredValues(ServletOutputStream ps) throws IOException {

//        logStoredValue(ps, OpenIdConstant.ORIGINAL_REQUEST);
//        logStoredValue(ps, OpenIdConstant.SUBJECT_IDENTIFIER); is throwing an npe
//        logStoredValue(ps, OpenIdConstant.CLIENT_ID);
//        logStoredValue(ps, OpenIdConstant.CLIENT_SECRET);

    }

    protected void logStoredValue(ServletOutputStream ps, String storedValue) throws IOException {

        System.out.println("req: " + req);
        System.out.println("resp: " + resp);
        System.out.println("storedValue: " + storedValue);

        printLine(ps, caller, "StoredValue: " + storedValue + ":" + context.getStoredValue(req, resp, storedValue));

    }

    protected void printLine(ServletOutputStream ps, String caller, String msg) throws IOException {

        printLine(ps, caller + ": " + msg);

    }

    public void printLine(ServletOutputStream ps, String msg) throws IOException {

        System.out.println(msg);
        ps.println(msg);

    }
}