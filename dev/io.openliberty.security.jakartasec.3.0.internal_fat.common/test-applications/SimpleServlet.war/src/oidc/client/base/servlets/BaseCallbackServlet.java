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
package oidc.client.base.servlets;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import jakarta.json.JsonObject;
import jakarta.security.enterprise.identitystore.openid.AccessToken;
import jakarta.security.enterprise.identitystore.openid.IdentityToken;
import jakarta.security.enterprise.identitystore.openid.OpenIdClaims;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;
import jakarta.security.enterprise.identitystore.openid.RefreshToken;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

//@WebServlet("/Callback")
public class BaseCallbackServlet extends HttpServlet {

    private static final long serialVersionUID = -417476984908088827L;

//    @Inject
    private OpenIdContext context;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        ServletOutputStream ps = response.getOutputStream();

//        logContext(ps);

        System.out.println("got here");
        ps.println("got here");
    }

    protected void logContext(ServletOutputStream ps) throws IOException {

        String contextSub = context.getSubject();

        AccessToken accessToken = context.getAccessToken();
        logAccessTokenClaims(ps, accessToken);

        IdentityToken idToken = context.getIdentityToken();
        logIdTokenClaims(ps, idToken);

        Optional<RefreshToken> refreshToken = context.getRefreshToken();
        logRefreshTokenClaims(ps, refreshToken);

        OpenIdClaims claims = context.getClaims();
        String claimsSub = claims.getSubject();

        JsonObject claimsJson = context.getClaimsJson();
        logJsonClaims(ps, claimsJson);

        Optional<Long> expiresIn = context.getExpiresIn();
        logExpiresIn(ps, expiresIn);

        ps.println("Subject string: " + contextSub);
        ps.println("Token Type: " + context.getTokenType());

        ps.println("Claims Subject: " + claimsSub);
        // compare context subject to cliams subject???
        if (contextSub == null && claimsSub == null) {

        }
        // TODO compare atClaims to jsonClaims - should they be the same?

        // TODO what can we validate with getStoredValue???
        //context.getStoredValue(null, null, claimsSub);
    }

    protected void logAccessTokenClaims(ServletOutputStream ps, AccessToken token) throws IOException {

        if (token != null) {
            Map<String, Object> atClaims = token.getClaims();
            ps.println("Access Token: " + token.toString());
            for (Map.Entry<String, Object> entry : atClaims.entrySet()) {
                ps.println("Access Token: Claim: Key: " + entry.getKey() + " Value: " + entry.getValue());
            }
        } else {
            ps.println("Access Token: null");
        }
    }

    protected void logIdTokenClaims(ServletOutputStream ps, IdentityToken token) throws IOException {

        if (token != null) {
            Map<String, Object> atClaims = token.getClaims();
            ps.println("Identity Token: " + token.toString());
            for (Map.Entry<String, Object> entry : atClaims.entrySet()) {
                ps.println("Identity Token: Claim: Key: " + entry.getKey() + " Value: " + entry.getValue());
            }
        } else {
            ps.println("Identity Token: null");
        }
    }

    protected void logRefreshTokenClaims(ServletOutputStream ps, Optional<RefreshToken> token) throws IOException {

        if (token != null) {
            RefreshToken tokenContent = token.get();
            ps.println("Refresh Token (raw): " + tokenContent.toString());
            ps.println("Refresh Token: " + tokenContent.getToken());
        } else {
            ps.println("Refresh Token: null");
        }
    }

    protected void logJsonClaims(ServletOutputStream ps, JsonObject claims) throws IOException {

        if (claims != null) {
            ps.println("Json Claims: " + claims.toString());
            // TODO update once I can see what the data really looks like
            //for (String key : claims.get)
        } else {
            ps.println("Json Claims: null");
        }
    }

    protected void logExpiresIn(ServletOutputStream ps, Optional<Long> expiresIn) throws IOException {

        if (expiresIn != null) {
            ps.println("Expires In: " + expiresIn.toString());
            // TODO update once we see what're we're getting and what else we could to with the value.
        } else {
            ps.println("Expires In: null");
        }

    }
}