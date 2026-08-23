/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.token.ltpa.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Base64;
import java.util.HashMap;
import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.security.token.SingleSignonToken;

@SuppressWarnings("serial")
public class LTPATestServlet extends HttpServlet {

    private static final String LTPA_COOKIE   = "LtpaToken2";
    private static final String CREATION_TIME = AttributeNameConstants.WSTOKEN_CREATION_TIME;

    // Dispatches GET requests: ?action=backdate&offsetSeconds=N backdates the caller's LTPA token
    // (used by LTPATokenRefreshTests); any other GET exercises the TokenManager creation path and
    // returns "Test Passed" (used by FATTest).
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if ("backdate".equals(request.getParameter("action"))) {
            handleBackdate(request, response);
        } else {
            PrintWriter writer = response.getWriter();
            BundleContext ctx = getBundleContext();
            try {
                testGetTokenManager(ctx);
                writer.println("Test Passed");
            } catch (Throwable e) {
                e.printStackTrace(writer);
            } finally {
                writer.flush();
                writer.close();
            }
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().print("use GET method");
    }

    // Looks up the TokenManager OSGi service and creates a test Ltpa2 token with a dummy
    // unique_id. This exercises the full token creation path (key loading, encryption, signing)
    // and triggers generation of the LTPA keys file on disk if it does not yet exist.
    private void testGetTokenManager(BundleContext ctx) throws Exception {
        ServiceReference<TokenManager> tokenManagerReference = ctx.getServiceReference(TokenManager.class);
        TokenManager tm = ctx.getService(tokenManagerReference);

        try {
            if (tm != null) {
                HashMap<String, Object> tokenData = new HashMap<>();
                tokenData.put("unique_id", "foo");
                tm.createToken("Ltpa2", tokenData);
            }
        } catch (Exception e) {
            throw new Exception("Error creating the token: " + e.getMessage());
        }
    }

    // Backdates the LTPA token by offsetSeconds seconds by appending a backdated WSTOKEN_CREATION_TIME
    // to the SSO token bytes so LTPAToken2.checkRefreshNeeded() and validateExpiration() see the older age.
    // Usage: GET /ltpaTest/LTPATestServlet?action=backdate&offsetSeconds=70
    private void handleBackdate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain");
        PrintWriter writer = response.getWriter();
        try {
            long offsetMs = parseOffsetMs(request.getParameter("offsetSeconds"));
            BundleContext ctx = getBundleContext();
            if (ctx == null) {
                throw new IllegalStateException("FrameworkUtil.getBundle returned null for HttpServlet.class");
            }

            ServiceReference<TokenManager> ref = ctx.getServiceReference(TokenManager.class);
            TokenManager tm = ctx.getService(ref);
            try {
                long backdatedCreationTime = System.currentTimeMillis() - offsetMs;
                SingleSignonToken token = createBackdatedToken(tm, request, backdatedCreationTime, writer);
                setLtpaCookieHeader(response, token);
                response.setStatus(HttpServletResponse.SC_OK);
            } finally {
                ctx.ungetService(ref);
            }
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writer.println("ERROR: " + e.getMessage());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writer.println("ERROR: " + e.getMessage());
            e.printStackTrace(writer);
        } finally {
            writer.flush();
            writer.close();
        }
    }

    // Parses the offsetSeconds query parameter into milliseconds. Throws IllegalArgumentException on parse failure.
    private long parseOffsetMs(String offsetParam) {
        try {
            return Long.parseLong(offsetParam) * 1000L;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("offsetSeconds must be a number, got: " + offsetParam);
        }
    }

    // Creates an SSO token for the authenticated user with creationTime backdated to backdatedCreationTime.
    //
    // Strategy:
    //  1. Call createSSOToken() — Liberty stamps creationTime=now and expire=now+duration.
    //  2. Append backdated creationTime (second value wins in validateExpiration() and checkRefreshNeeded()).
    private SingleSignonToken createBackdatedToken(TokenManager tm, HttpServletRequest request,
                                                   long backdatedCreationTime, PrintWriter writer) throws Exception {
        String accessId = resolveAccessId(request, writer);
        writer.println("accessId=" + accessId);

        HashMap<String, Object> tokenData = new HashMap<>();
        tokenData.put("unique_id", accessId);
        SingleSignonToken token = tm.createSSOToken(tokenData);

        token.addAttribute(CREATION_TIME, Long.toString(backdatedCreationTime));
        writer.println("backdatedCreationTime=" + backdatedCreationTime);
        return token;
    }

    // Reads the access ID from the caller's WSCredential so the realm is included (e.g. "user:BasicRealm/user1").
    // Falls back to the servlet principal name when no WSCredential is present.
    private String resolveAccessId(HttpServletRequest request, PrintWriter writer) {
        String accessId = null;
        try {
            Subject callerSubject = WSSubject.getCallerSubject();
            if (callerSubject != null) {
                for (Object cred : callerSubject.getPublicCredentials()) {
                    if (cred instanceof WSCredential) {
                        accessId = ((WSCredential) cred).getAccessId();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            writer.println("WARN: could not read accessId from WSCredential: " + e.getMessage());
        }
        if (accessId == null) {
            accessId = "user:" + (request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "user1");
            writer.println("WARN: accessId fallback used: " + accessId);
        }
        return accessId;
    }

    // Encodes the token bytes as Base64 and appends an LtpaToken2 cookie to the response.
    // Using addCookie() (not setHeader) places this cookie after Liberty's own entry, so the
    // test always reads the last LtpaToken2 value, which is this backdated one.
    private void setLtpaCookieHeader(HttpServletResponse response, SingleSignonToken token) throws Exception {
        String value = Base64.getEncoder().encodeToString(token.getBytes());
        Cookie cookie = new Cookie(LTPA_COOKIE, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

    // Returns the OSGi BundleContext via the servlet container bundle, or null if unavailable.
    private BundleContext getBundleContext() {
        Bundle bundle = FrameworkUtil.getBundle(HttpServlet.class);
        return bundle != null ? bundle.getBundleContext() : null;
    }
}
