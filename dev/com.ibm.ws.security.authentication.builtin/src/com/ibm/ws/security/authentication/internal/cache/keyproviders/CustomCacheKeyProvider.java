/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.internal.cache.keyproviders;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;
import java.util.Map;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.cache.AuthCacheService;
import com.ibm.ws.security.authentication.cache.CacheContext;
import com.ibm.ws.security.authentication.cache.CacheKeyProvider;
import com.ibm.ws.security.authentication.internal.SSOTokenHelper;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.ltpa.Token;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.security.token.SingleSignonToken;

/**
 * Provides the SSO token bytes as the cache key.
 */
public class CustomCacheKeyProvider implements CacheKeyProvider {
    private static final String[] hashtableProperties = { AttributeNameConstants.WSCREDENTIAL_CACHE_KEY };
    private static final AtomicServiceReference<TokenManager> tokenManager = new AtomicServiceReference<TokenManager>("tokenManager");
    private final SubjectHelper subjectHelper = new SubjectHelper();
    private final static String OIDC_ACCESS_TOKEN = "oidc_access_token";
    private static final String LTPA_OID = "oid:1.3.18.0.2.30.2";
    private static final String JWT_OID = "oid:1.3.18.0.2.30.3"; // ?????

    protected void setTokenManager(ServiceReference<TokenManager> ref) {
        tokenManager.setReference(ref);
    }

    protected void unsetTokenManager(ServiceReference<TokenManager> ref) {
        tokenManager.unsetReference(ref);
    }

    protected void activate(ComponentContext cc, Map<String, Object> properties) {
        tokenManager.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        tokenManager.deactivate(cc);
    }

    /** {@inheritDoc} */
    @Override
    public Object provideKey(CacheContext context) {
        return getCustomCacheKey(context.getSubject());
    }

    /**
     * @param authCacheService
     * @param ssoTokenBytes
     * @param authenticationData TODO
     * @return
     * @throws AuthenticationException
     */
    @FFDCIgnore({ InvalidTokenException.class, TokenExpiredException.class })
    public static String getCustomCacheKey(AuthCacheService authCacheService, byte[] ssoTokenBytes, AuthenticationData authenticationData) throws AuthenticationException {
        String customCacheKey = null;
        TokenManager tokenManager = CustomCacheKeyProvider.tokenManager.getService();
        if (tokenManager == null)
            return null;
        try {
            Token recreatedToken = tokenManager.recreateTokenFromBytes(ssoTokenBytes);
            String[] attrs = recreatedToken.getAttributes(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
            if (attrs != null && attrs.length > 0) {
                customCacheKey = attrs[0];
            }
            String[] accessTokens = recreatedToken.getAttributes(OIDC_ACCESS_TOKEN);
            if (accessTokens != null && accessTokens.length > 0) {
                HttpServletRequest req = (HttpServletRequest) authenticationData.get(AuthenticationData.HTTP_SERVLET_REQUEST);
                req.setAttribute(OIDC_ACCESS_TOKEN, accessTokens[0]);
            }

        } catch (InvalidTokenException e) {
            throw new AuthenticationException(e.getMessage());
        } catch (TokenExpiredException e) {
            throw new AuthenticationException(e.getMessage());
        }
        return customCacheKey;
    }

    /**
     * @param subject
     * @return
     */
    private String getCustomCacheKey(final Subject subject) {
        String customCacheKey = null;
        Hashtable<String, ?> customProperties = subjectHelper.getHashtableFromSubject(subject, hashtableProperties);
        if (customProperties != null) {
            customCacheKey = (String) customProperties.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
        }
        //TODO: how's about jwt token
        if (customCacheKey == null) {
            SingleSignonToken ssoToken = AccessController.doPrivileged(new PrivilegedAction<SingleSignonToken>() {

                @Override
                public SingleSignonToken run() {
                    return SSOTokenHelper.getSSOToken(subject);
                }
            });
            if (ssoToken != null) {
                String[] attrs = ssoToken.getAttributes(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
                if (attrs != null)
                    customCacheKey = attrs[0];
            }
        }
        return customCacheKey;
    }

}
