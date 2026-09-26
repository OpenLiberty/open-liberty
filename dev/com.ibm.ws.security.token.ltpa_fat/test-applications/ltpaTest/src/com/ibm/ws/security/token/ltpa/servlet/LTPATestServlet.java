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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
import com.ibm.wsspi.security.ltpa.Token;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.security.token.SingleSignonToken;

@SuppressWarnings("serial")
public class LTPATestServlet extends HttpServlet {

    private static final String LTPA_COOKIE   = "LtpaToken2";
    private static final String CREATION_TIME = AttributeNameConstants.WSTOKEN_CREATION_TIME;
    private static final String EXPIRATION    = AttributeNameConstants.WSTOKEN_EXPIRATION;

    // Dispatches GET requests: ?action=backdate&offsetSeconds=N backdates the caller's LTPA token
    // (used by LTPATokenRefreshTests); any other GET exercises the TokenManager creation path and
    // returns "Test Passed" (used by FATTest).
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        PrintWriter writer = response.getWriter();
        try {
            if ("backdate".equals(request.getParameter("action"))) {
                handleBackdate(request, response, writer);
            } else {
                testGetTokenManager();
                writer.println("Test Passed");
            }
        } catch (Throwable e) {
            e.printStackTrace(writer);
        } finally {
            writer.flush();
            writer.close();
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
    private void testGetTokenManager() throws Exception {
        BundleContext ctx = getBundleContext();
        ServiceReference<TokenManager> tokenManagerReference = ctx.getServiceReference(TokenManager.class);
        try {
            TokenManager tm = ctx.getService(tokenManagerReference);
            if (tm != null) {
                HashMap<String, Object> tokenData = new HashMap<>();
                tokenData.put("unique_id", "foo");
                tm.createToken("Ltpa2", tokenData);
            }
        } catch (Exception e) {
            throw new Exception("Error creating the token: " + e.getMessage());
        } finally {
            ctx.ungetService(tokenManagerReference);
        }
    }

    // Backdates the LTPA token by offsetSeconds seconds. The token is created fresh, its bytes are
    // passed through TokenManager.recreateTokenFromBytes() with WSTOKEN_CREATION_TIME and
    // WSTOKEN_EXPIRATION stripped, and fresh backdated values are added.
    // Usage: GET /ltpaTest/LTPATestServlet?action=backdate&offsetSeconds=70
    private void handleBackdate(HttpServletRequest request, HttpServletResponse response, PrintWriter writer) {
        BundleContext ctx = getBundleContext();
        ServiceReference<TokenManager> ref = ctx.getServiceReference(TokenManager.class);
        try {
            TokenManager tm = ctx.getService(ref);

            long offsetMs = Long.parseLong(request.getParameter("offsetSeconds")) * 1000L;
            long backdatedCreationTime = System.currentTimeMillis() - offsetMs;

            SingleSignonToken token = createBackdatedToken(tm, request, backdatedCreationTime, writer);
            setLtpaCookieHeader(response, token);
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writer.println("ERROR: " + e.getMessage());
            e.printStackTrace(writer);
        } finally {
            ctx.ungetService(ref);
        }
    }

    /*
    * Creates an SSO token for the authenticated user with creationTime and expire both
    * backdated by offsetSeconds.
    *
    * Strategy:
    *  1. Call createSSOToken() — Liberty stamps creationTime=now and expire=now+duration.
    *  2. Read the original creationTime and expire from the fresh token to compute the duration.
    *  3. Recreate the token from its bytes, stripping WSTOKEN_CREATION_TIME and WSTOKEN_EXPIRATION.
    *  4. Add the backdated creationTime via addAttribute().
    *  5. Backdate the expiration
    *
    * Result: the token contains exactly one WSTOKEN_CREATION_TIME entry and one expire entry,
    * both shifted back by offsetSeconds.
    */
    private SingleSignonToken createBackdatedToken(TokenManager tm, HttpServletRequest request,
                                                   long backdatedCreationTime, PrintWriter writer) throws Exception {
        String accessId = resolveAccessId(request, writer);
        HashMap<String, Object> tokenData = new HashMap<>();
        tokenData.put("unique_id", accessId);
        SingleSignonToken freshToken = tm.createSSOToken(tokenData);

        // Read the original timestamps before stripping so we can preserve the duration.
        String[] originalCreationArr = freshToken.getAttributes(CREATION_TIME);
        String[] originalExpireArr   = freshToken.getAttributes(EXPIRATION);
        
        long originalCreationTime = Long.parseLong(originalCreationArr[originalCreationArr.length - 1]);
        long originalExpire       = Long.parseLong(originalExpireArr[originalExpireArr.length - 1]);
        long duration             = originalExpire - originalCreationTime;
        long backdatedExpire      = backdatedCreationTime + duration;

        // Recreate from bytes with both creationtime and expiration stripped.
        Token strippedToken = tm.recreateTokenFromBytes(freshToken.getBytes(), CREATION_TIME, EXPIRATION);
        SingleSignonToken token = tm.createSSOToken(strippedToken);

        // Add the backdated creationTime and expiration to token
        token.addAttribute(CREATION_TIME, Long.toString(backdatedCreationTime));
        setExpirationFromMilliseconds(token, backdatedExpire);

        return token;
    }

    // Invokes LTPAToken2.setExpirationFromMilliseconds(long) via reflection. That private method
    // atomically sets LTPAToken2.expirationInMilliseconds (read by encrypt() to write field 2 of
    // the wire format) and appends the value to userData.expire (field 1). Both fields must carry
    // the same value so that the two-field consistency check in decrypt() passes on receiving
    // servers, and so that validateExpiration() sees the correct backdated expiry.
    private void setExpirationFromMilliseconds(SingleSignonToken ssoToken, long backdatedExpire) throws Exception {
        // reach the LTPAToken2 through AbstractTokenImpl's private "token" field.
        Field tokenField = ssoToken.getClass().getSuperclass().getDeclaredField("token");
        tokenField.setAccessible(true);
        Object ltpaToken2 = tokenField.get(ssoToken);

        // invoke setExpirationFromMilliseconds(long) on the LTPAToken2 instance.
        Method m = ltpaToken2.getClass().getDeclaredMethod("setExpirationFromMilliseconds", long.class);
        m.setAccessible(true);
        m.invoke(ltpaToken2, backdatedExpire);
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
        }
        return accessId;
    }

    // Encodes the token bytes as Base64 and appends an LtpaToken2 cookie to the response.
    // Using addCookie() places this cookie after Liberty's own entry, so the
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
