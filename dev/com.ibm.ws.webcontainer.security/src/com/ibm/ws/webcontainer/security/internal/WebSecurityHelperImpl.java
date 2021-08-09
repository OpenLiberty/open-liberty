/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.http.Cookie;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.jaas.common.callback.AuthenticationHelper;
import com.ibm.ws.security.jwtsso.token.proxy.JwtSSOTokenHelper;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.ltpa.Token;
import com.ibm.wsspi.security.token.SingleSignonToken;

public class WebSecurityHelperImpl {
    private static final TraceComponent tc = Tr.register(WebSecurityHelperImpl.class);
    private static WebAppSecurityConfig webAppSecConfig = null;
    private static final AtomicServiceReference<TokenManager> tokenManagerRef = new AtomicServiceReference<TokenManager>("tokenManager");

    public static void setWebAppSecurityConfig(WebAppSecurityConfig webAppSecConfig) {
        WebSecurityHelperImpl.webAppSecConfig = webAppSecConfig;
    }

    /**
     * Extracts an LTPA sso cookie from the subject of current thread
     * and builds a ltpa cookie out of it without a list of attributes for use on downstream web invocations.
     * The caller must check for null return value only when not null
     * that getName and getValue can be invoked on the returned Cookie object
     *
     * @param removeAttributes A list of attributes
     * @return an object of type javax.servlet.http.Cookie. When the returned value is not
     *         null use Cookie methods getName() and getValue() to set the Cookie header
     *         on an http request with header value of Cookie.getName()=Cookie.getValue()
     */
    public static Cookie getSSOCookieFromSSOToken(String... removeAttributes) throws Exception {
        Cookie ltpaCookie = null;
        if (webAppSecConfig == null) {
            // if we don't have the config, we can't construct the cookie
            return null;
        }
        try {
            Subject subject = WSSubject.getRunAsSubject();
            if (subject == null) {
                subject = WSSubject.getCallerSubject();
            }
            if (subject != null) {
                ltpaCookie = getLTPACookie(subject, removeAttributes);
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "No subjects on the thread");
                }
            }
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getSSOCookieFromSSOTokenWithOutAttrs caught exception: " + e.getMessage());
            }
            throw e;
        }
        return ltpaCookie;
    }

    /**
     * @return
     */
    public static WebAppSecurityConfig getWebAppSecurityConfig() {
        return webAppSecConfig;
    }

    /**
     * Extracts the JWT cookie name for use on downstream web invocations.
     * Return null when the service is not started or activated.
     *
     * @return a String.
     */
    public static String getJwtCookieName() {
        return JwtSSOTokenHelper.getJwtCookieName();
    }

    /**
     * Extracts the access ID from the SSO LTPA token. Return null if the token is not valid.
     *
     * @return a String.
     */
//    public static String validateToken(byte[] ssoToken) throws Exception {
//        String accessId = null;
//        if (ssoToken != null) {
//            try {
//                Token recreatedToken = recreateTokenFromBytes(ssoToken);
//                if (recreatedToken != null) {
//                    accessId = recreatedToken.getAttributes("u")[0];
//                }
//            } catch (WSSecurityException e) {
//                if (tc.isDebugEnabled()) {
//                    Tr.debug(tc, "getAcessIdFromSSOToken caught exception: " + e.getMessage());
//                }
//                throw e;
//            }
//        }
//        return accessId;
//    }

    /**
     * Extracts the access ID from the SSO LTPA token. Return null if the token is not valid.
     *
     * @return a String.
     */
//    public static String validateToken(String cookieValue) throws Exception {
//        return validateToken(Base64Coder.base64DecodeString(cookieValue));
//    }

    /**
     * builds an LTPACookie object
     **/
    private static Cookie constructLTPACookieObj(SingleSignonToken ssoToken) {
        byte[] ssoTokenBytes = ssoToken.getBytes();
        return createCookie(ssoTokenBytes);
    }

    /**
     * builds an LTPACookie object without a list of attributes
     **/
    private static Cookie constructLTPACookieObj(SingleSignonToken ssoToken, String... removeAttributes) {
        byte[] ssoTokenBytes = ssoToken.getBytes();

        try {
            Token token = recreateTokenFromBytes(ssoTokenBytes, removeAttributes);
            if (token != null)
                ssoTokenBytes = token.getBytes();
        } catch (InvalidTokenException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Token is not valid" + e.getMessage());
            }
            return null;
        } catch (TokenExpiredException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Token is expired" + e.getMessage());
            }
            return null;
        }

        return createCookie(ssoTokenBytes);
    }

    /**
     * @param removeAttributes
     * @param ssoToken
     * @return
     * @throws InvalidTokenException
     * @throws TokenExpiredException
     */
    private static Token recreateTokenFromBytes(byte[] ssoToken, String... removeAttributes) throws InvalidTokenException, TokenExpiredException {
        Token token = null;
        TokenManager tokenManager = tokenManagerRef.getService();
        if (tokenManager != null) {
            byte[] credToken = AuthenticationHelper.copyCredToken(ssoToken);
            if (removeAttributes != null) {
                token = tokenManager.recreateTokenFromBytes(credToken, removeAttributes);
            } else {
                token = tokenManager.recreateTokenFromBytes(credToken);
            }
        }
        return token;

    }

    /**
     * @param ssoTokenBytes
     * @return
     */
    private static Cookie createCookie(byte[] ssoTokenBytes) {
        String ssoCookieString = Base64Coder.base64EncodeToString(ssoTokenBytes);
        Cookie cookie = new Cookie(webAppSecConfig.getSSOCookieName(), ssoCookieString);
        return cookie;
    }

    /**
     * Gets the LTPA cookie from the given subject
     *
     * @param subject
     * @param removeAttributes
     * @return
     * @throws Exception
     */
    static Cookie getLTPACookie(final Subject subject, String... removeAttributes) throws Exception {
        Cookie ltpaCookie = null;
        SingleSignonToken ssoToken = null;
        Set<SingleSignonToken> ssoTokens = subject.getPrivateCredentials(SingleSignonToken.class);
        Iterator<SingleSignonToken> ssoTokensIterator = ssoTokens.iterator();
        if (ssoTokensIterator.hasNext()) {
            ssoToken = ssoTokensIterator.next();
            if (ssoTokensIterator.hasNext()) {
                throw new WSSecurityException("More than one ssotoken found in subject");
            }
        }
        if (ssoToken != null) {
            if (removeAttributes == null) {
                ltpaCookie = constructLTPACookieObj(ssoToken);
            } else {
                ltpaCookie = constructLTPACookieObj(ssoToken, removeAttributes);
            }

        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No ssotoken found for this subject");
            }
        }
        return ltpaCookie;
    }

    protected void setTokenManager(ServiceReference<TokenManager> ref) {
        tokenManagerRef.setReference(ref);
    }

    protected void unsetTokenManager(ServiceReference<TokenManager> ref) {
        tokenManagerRef.unsetReference(ref);
    }

    protected void activate(ComponentContext cc, Map<String, Object> properties) {
        tokenManagerRef.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        tokenManagerRef.deactivate(cc);
    }
}
