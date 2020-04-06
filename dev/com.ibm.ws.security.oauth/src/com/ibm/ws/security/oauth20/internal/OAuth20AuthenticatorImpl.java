/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.internal;

import java.io.UnsupportedEncodingException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidScopeException;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.claims.UserClaims;
import com.ibm.ws.security.common.claims.UserClaimsRetrieverService;
import com.ibm.ws.security.oauth20.ProvidersService;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.token.impl.WSOAuth20TokenHelper;
import com.ibm.ws.security.oauth20.util.ConfigUtils;
import com.ibm.ws.security.oauth20.util.UtilConstants;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.oauth20.OAuth20Authenticator;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.oauth20.token.WSOAuth20Token;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 * This class handles OAuth authentication for incoming web requests.
 * The logic is a consolidation/modification of the tWAS OAuth TAI.
 */
public class OAuth20AuthenticatorImpl implements OAuth20Authenticator {

    private static final String MESSAGE_BUNDLE = "com.ibm.ws.security.oauth20.resources.ProviderMsgs";
    private static final TraceComponent tc = Tr.register(OAuth20AuthenticatorImpl.class, "OAuth20Provider", MESSAGE_BUNDLE);
    private static final String DEFAULT_GROUP_IDENTIFIER = "groupIds";
    private static final String Authorization_Header = "Authorization";

    /**
     * Perform OAuth authentication for the given web request. Return an
     * ProviderAuthenticationResult which contains the status and subject
     *
     * @param HttpServletRequest
     * @param HttpServletResponse
     * @return ProviderAuthenticationResult
     */
    @Override
    public ProviderAuthenticationResult authenticate(HttpServletRequest req, HttpServletResponse res) {
        return authenticate(req, res, null);
    }

    @Override
    public ProviderAuthenticationResult authenticate(HttpServletRequest req,
            HttpServletResponse res, ConcurrentServiceReferenceMap<String, OidcServerConfig> oidcServerConfigRef) {
        boolean isOauthProtected = false;
        ProviderAuthenticationResult result = new ProviderAuthenticationResult(AuthResult.CONTINUE, HttpServletResponse.SC_OK);
        List<OAuth20Provider> providers = getProviders(req);
        if (providers != null && !providers.isEmpty()) {
            if (providers.size() == 1) {
                OAuth20Provider provider = providers.get(0);
                String encoding = provider.getCharacterEncoding();
                if (req.getCharacterEncoding() == null && encoding != null) {
                    try {
                        req.setCharacterEncoding(encoding);
                    } catch (UnsupportedEncodingException e) {
                        if (tc.isWarningEnabled()) {
                            Tr.warning(tc, e.getMessage());
                        }
                    }
                }
                boolean oauthOnly = provider.isOauthOnly();
                if (oauthOnly) {
                    if (!isTokenRequest(req)) {
                        isOauthProtected = true;
                    }
                } else {
                    if (isProtectedResourceRequest(req)) {
                        isOauthProtected = true;
                    } else {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "There is no access token, falling back to available authentication.");
                        }
                    }
                }
                if (isOauthProtected) {
                    result = checkAccess(req, res, provider);
                }
            } else {
                // duplicate entry, return an internal error.
                result = new ProviderAuthenticationResult(AuthResult.FAILURE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                StringBuffer list = null;
                Iterator<OAuth20Provider> oauthProvidersIterator = providers.iterator();
                while (oauthProvidersIterator.hasNext()) {
                    OAuth20Provider provider = oauthProvidersIterator.next();
                    if (list == null) {
                        list = new StringBuffer(provider.getID());
                    } else {
                        list.append(", ").append(provider.getID());
                    }
                }
                Tr.error(tc, "security.oauth20.error.filter.multiple.matching", list.toString());
            }
        }
        return result;
    }

    private ProviderAuthenticationResult checkAccess(HttpServletRequest req, HttpServletResponse res, OAuth20Provider provider) {
        ProviderAuthenticationResult result = null;
        String access_token = getBearerAccessTokenToken(req);
        boolean isAppPasswordOrTokenRequest = false;
        if (access_token == null || access_token.trim().length() == 0) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "There is no OAuth token in the request.");
            }
            result = new ProviderAuthenticationResult(AuthResult.FAILURE, HttpServletResponse.SC_UNAUTHORIZED);
            // throw new WebTrustAssociationFailedException(getMsg("notoken"));
        } else {
            OAuthResult oResult = provider.processResourceRequest(req);
            if (oResult.getStatus() == OAuthResult.STATUS_FAILED) {
                // Token rejected due to timeout, bad token or scope mismatch
                // Response codes detailed at
                // http://tools.ietf.org/html/draft-ietf-oauth-v2-bearer
                int responseCode = -1;
                if (oResult.getCause() instanceof OAuth20InvalidScopeException) {
                    responseCode = HttpServletResponse.SC_FORBIDDEN;
                } else { // bad token
                    responseCode = HttpServletResponse.SC_UNAUTHORIZED;
                }
                result = new ProviderAuthenticationResult(AuthResult.FAILURE, responseCode);
                String message = "Bearer realm=\"OAuth\",";
                message += " error=\"invalid_token\",";
                message += " error_description=\"Check access token\"";
                res.setHeader(UtilConstants.WWW_AUTHENTICATE, message);
                if (tc.isDebugEnabled()) {
                    if (oResult.getCause() != null) {
                        Tr.debug(tc, "OAuth Token validation fails: " + oResult.getCause().getMessage());
                    } else {
                        Tr.debug(tc, "OAuth Token with null cause! " + oResult);
                    }
                }
            } else { // success
                result = createResult(req, res, oResult, provider);
            }
        }
        if (AuthResult.FAILURE == result.getStatus() && isAppPasswordOrTokenRequest(req)) {
            result = new ProviderAuthenticationResult(AuthResult.CONTINUE, HttpServletResponse.SC_OK); // we are handling errors with app-token and app-password requests in the new rest api token exchange
        }
        return result;
    }

    /**
     * @param req
     * @return
     */
    private boolean isAppPasswordOrTokenRequest(HttpServletRequest req) {
        if (req.getRequestURI() == null) {
            return false;
        }
        if (req.getRequestURI().endsWith(OAuth20Constants.APP_PASSWORD_URI) || req.getRequestURI().endsWith(OAuth20Constants.APP_TOKEN_URI)) {
            return true;
        }
        final String apw = "/" + OAuth20Constants.APP_PASSWORD_URI + "/";
        final String atk = "/" + OAuth20Constants.APP_TOKEN_URI + "/";
        if (req.getRequestURI().contains(apw) || req.getRequestURI().contains(atk)) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private ProviderAuthenticationResult createResult(HttpServletRequest req,
            HttpServletResponse res,
            OAuthResult oResult,
            OAuth20Provider provider) {
        Subject subject = new Subject();
        WSOAuth20Token token = WSOAuth20TokenHelper.createToken(req, res, oResult, provider.getID());
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "OAuth Token is " + token);
        }
        String cacheKey = token.getCacheKey();

        // include access token in Subject
        if (provider.isIncludeTokenInSubject()) {
            addToSubjectAsPrivateCredential(subject, token);
        }

        // custom propagation attributes
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        customProperties.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, cacheKey);
        customProperties.put(com.ibm.ws.security.oauth20.util.UtilConstants.OAUTH_PROVIDER_NAME, provider.getID());
        String user_name = token.getUser();
        UserClaimsRetrieverService ucrService = ConfigUtils.getUserClaimsRetrieverService();
        if (ucrService != null) {
            UserClaims userClaims = ucrService.getUserClaims(user_name, DEFAULT_GROUP_IDENTIFIER);
            if (userClaims != null) {
                Map<String, Object> claimsMap = userClaims.asMap();
                List<String> groups = (List<String>) claimsMap.get(DEFAULT_GROUP_IDENTIFIER);
                if (groups != null && groups.size() > 0) {
                    customProperties.put(AttributeNameConstants.WSCREDENTIAL_GROUPS, groups);
                }
            }
        }

        return new ProviderAuthenticationResult(AuthResult.SUCCESS, HttpServletResponse.SC_OK, user_name, subject, customProperties, null);
    }

    private void addToSubjectAsPrivateCredential(final Subject subj, final Object token) {
        if (token != null) {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    subj.getPrivateCredentials().add(token);
                    return null;
                }
            });
        }
    }

    protected List<OAuth20Provider> getProviders(HttpServletRequest req) {
        return ProvidersService.getProvidersMatchingRequest(req);
    }

    private boolean isTokenRequest(HttpServletRequest req) {
        boolean isToken = false;
        if (UtilConstants.AUTHORIZATION_CODE.equals(req.getParameter(UtilConstants.GRANT_TYPE)) ||
                UtilConstants.AUTHORIZATION_CODE.equals(req.getHeader(UtilConstants.GRANT_TYPE)) ||
                UtilConstants.TOKEN.equals(req.getParameter(UtilConstants.RESPONSE_TYPE)) ||
                UtilConstants.TOKEN.equals(req.getHeader(UtilConstants.RESPONSE_TYPE)) ||
                UtilConstants.PASSWORD.equals(req.getParameter(UtilConstants.GRANT_TYPE)) ||
                UtilConstants.PASSWORD.equals(req.getHeader(UtilConstants.GRANT_TYPE)) ||
                UtilConstants.CLIENT_CREDENTIALS.equals(req.getParameter(UtilConstants.GRANT_TYPE)) ||
                UtilConstants.CLIENT_CREDENTIALS.equals(req.getHeader(UtilConstants.GRANT_TYPE))) {
            isToken = true;
        }
        return isToken;
    }

    private boolean isProtectedResourceRequest(HttpServletRequest req) {
        boolean isResourceReq = false;
        if (hasOAuthToken(req) && !isTokenRequest(req)) {
            isResourceReq = true;
        }
        return isResourceReq;
    }

    private boolean hasOAuthToken(HttpServletRequest req) {
        boolean hasToken = false;
        String hdrValue = getBearerAccessTokenToken(req);
        if (hdrValue != null && hdrValue.trim().length() != 0) {
            hasToken = true;
        }
        return hasToken;
    }

    private String getBearerAccessTokenToken(HttpServletRequest req) {
        String hdrValue = req.getHeader(Authorization_Header);
        if (hdrValue != null && hdrValue.startsWith("Bearer ")) {

            hdrValue = hdrValue.substring(7);
        } else {
            hdrValue = req.getHeader(UtilConstants.ACCESS_TOKEN);
            if (hdrValue == null || hdrValue.trim().length() == 0) {
                hdrValue = req.getParameter(UtilConstants.ACCESS_TOKEN);
            }

        }
        return hdrValue;
    }

}
