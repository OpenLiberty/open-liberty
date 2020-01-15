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
package com.ibm.ws.security.oauth20.web;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;

import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OauthConsentStore;
import com.ibm.ws.security.oauth20.api.OidcOAuth20Client;
import com.ibm.ws.security.oauth20.form.FormRenderer;
import com.ibm.ws.security.oauth20.util.BoundedConsentCache;
import com.ibm.ws.security.oauth20.util.ConsentCacheKey;
import com.ibm.ws.security.oauth20.util.Nonce;
import com.ibm.ws.security.oauth20.util.TemplateRetriever;

public class Consent {

    private static TraceComponent tc = Tr.register(Consent.class);

    public static final Pattern FORWARD_TEMPLATE_PATTERN = Pattern.compile("\\{(/[\\w-/]+)\\}(/.+)");
    public static final String PARAM_AUTHZ_FORM_TEMPLATE = Constants.PARAM_AUTHZ_FORM_TEMPLATE;
    private static final String ATTR_CONSENT_CACHE = "consentCache";
    private static final String ATTR_NONCE = "consentNonce";
    private static final String ATTR_RESOURCE = "consentResource";
    private static final String ATTR_OAUTH_CLIENT = "oauthClient";
    public static final String HEADER_ACCEPT_LANGUAGE = TemplateRetriever.HEADER_ACCEPT_LANGUAGE;

    /**
     * Caches approval for the scopes included in the given response for the
     * given client, if consent is not explicitly requested by {@code prompt}.
     * Cached scopes are stored in an object stored as an attribute in the
     * HTTP session associated with the request.
     *
     * @param provider
     * @param request
     * @param prompt
     * @param clientId
     */
    protected void handleConsent(OAuth20Provider provider, HttpServletRequest request, Prompt prompt, String clientId) {

        if (prompt.hasPrompt() && !prompt.hasConsent()) {
            String redirectUri = request.getParameter(OAuth20Constants.REDIRECT_URI);
            String resourceId = request.getParameter(OAuth20Constants.RESOURCE);// null; // TODO
            String scopeString = request.getParameter(OAuth20Constants.SCOPE);
            String[] scope = getScope(scopeString);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caching consent response for scopes: " + scopeString);
            }
            int keyLifetime = (int) provider.getConsentCacheEntryLifetime();
            if (provider.isLocalStoreUsed()) {
                BoundedConsentCache consentCache = getConsentCacheFromSession(request, provider);
                for (String s : scope) {
                    ConsentCacheKey newKey = new ConsentCacheKey(clientId, redirectUri, scopeString, resourceId, keyLifetime);
                    if (!consentCache.contains(newKey)) {
                        consentCache.put(newKey);
                    }
                }
                request.getSession().setAttribute(ATTR_CONSENT_CACHE, consentCache);
            } else {
                // Add the consent to the consent store
                OauthConsentStore consentStore = provider.getConsentCache();
                consentStore.addConsent(clientId, request.getUserPrincipal().getName(), scopeString, resourceId, provider.getID(), keyLifetime);
            }

        }
    }

    /**
     * Determine if affirmative consent has been cached, and is still valid,
     * for this client for the requested scopes. If any of the scopes were
     * cached and have since expired, they are removed from the cache.
     *
     * @param oauthResult
     * @param provider
     * @param request
     * @param response
     * @return {@code true} if consent has been granted by the client for the
     * requested scope(s) and has not yet expired
     */
    public boolean isCachedAndValid(OAuthResult oauthResult, OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response) {
        // TODO: oauthResult and scopes check??
        if (oauthResult == null) {
            return false;
        }
        String[] scopes = null;
        if (oauthResult.getAttributeList() != null) {
            scopes = oauthResult.getAttributeList().getAttributeValuesByName(OAuth20Constants.SCOPE);
        }
        if (scopes == null || scopes.length == 0) {
            return false;
        }
        String clientId = oauthResult.getAttributeList().getAttributeValueByName(OAuth20Constants.CLIENT_ID);
        String redirectUri = oauthResult.getAttributeList().getAttributeValueByName(OAuth20Constants.REDIRECT_URI);
        String resourceId = oauthResult.getAttributeList().getAttributeValueByName(OAuth20Constants.RESOURCE);

        // TODO: The duration from metatype is always a long as converted by the config. Investigate changing cache lifetime to a long.
        int keyLifetime = (int) provider.getConsentCacheEntryLifetime();
        boolean isValid = true;
        if (provider.isLocalStoreUsed()) {
            BoundedConsentCache consentCache = getConsentCacheFromSession(request, provider);

            for (String scope : scopes) {
                ConsentCacheKey consentCacheKey = new ConsentCacheKey(clientId, redirectUri, scope, resourceId, keyLifetime);
                synchronized (consentCache) {
                    ConsentCacheKey cacheKey = consentCache.get(consentCacheKey);
                    isValid = isCacheKeyValid(consentCache, cacheKey, scope, keyLifetime);
                    if (!isValid) {
                        consentCache.remove(consentCacheKey);
                        isValid = false;
                    }
                }
            }

            request.getSession().setAttribute(ATTR_CONSENT_CACHE, consentCache);
        } else {
            // get the entry from consent store
            OauthConsentStore consentStore = provider.getConsentCache();
            isValid = consentStore.validateConsent(clientId, request.getUserPrincipal().getName(), provider.getID(), scopes, resourceId);
        }

        return isValid;
    }

    public boolean isCacheKeyValid(BoundedConsentCache consentCache, ConsentCacheKey consentCacheKey, String scope, int keyLifetime) {
        if (consentCacheKey == null) {
            return false;
        }
        if (!consentCacheKey.isValid() || consentCacheKey.getLifetime() != keyLifetime) {
            return false;
        }
        return true;
    }

    /**
     * Attempt to get the authorization consent cache from the servlet
     * session.
     *
     * @param request
     * @param provider
     * @return The cache object stored in the session. If no cache object
     *         was in the session, a new cache object is returned
     */
    public BoundedConsentCache getConsentCacheFromSession(HttpServletRequest request, OAuth20Provider provider) {
        int cacheCapacity = (int) provider.getConsentCacheSize();
        BoundedConsentCache consentCache = (BoundedConsentCache) request.getSession().getAttribute(ATTR_CONSENT_CACHE);
        if (consentCache == null) {
            consentCache = new BoundedConsentCache(cacheCapacity);
        } else if (cacheCapacity != consentCache.getCapacity()) {
            consentCache.updateCapacity(cacheCapacity);
        }
        return consentCache;
    }

    public void renderConsentForm(HttpServletRequest request,
            HttpServletResponse response, OAuth20Provider provider,
            String clientId, Nonce nonce, AttributeList attributes, ServletContext servletContext)
            throws IOException, ServletException, OidcServerException {
        renderConsentForm(request, response, provider, clientId, nonce, attributes, servletContext, null);

    }

    protected void renderConsentForm(HttpServletRequest request,
            HttpServletResponse response, OAuth20Provider provider,
            String clientId, Nonce nonce, AttributeList attributes, ServletContext servletContext, FormRenderer renderer)
            throws IOException, ServletException, OidcServerException {

        OidcOAuth20Client client = provider.getClientProvider().get(clientId);
        String templateUrl = provider.getAuthorizationFormTemplate();
        // default is "template.html" already

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "templateUrl from configuration is " + templateUrl);
        }

        // 242891 - don't load default value thru an http connection, doesn't work behind a proxy.
        byte[] defaultAuthorizationFormTemplatecontent = provider.getDefaultAuthorizationFormTemplateContent();
        boolean useDefaultTemplateValue = (defaultAuthorizationFormTemplatecontent != null);

        Matcher m = FORWARD_TEMPLATE_PATTERN.matcher(templateUrl);
        if (m.matches()) {
            String contextPath = m.group(1);
            String path = m.group(2);
            request.setAttribute(ATTR_OAUTH_CLIENT, client);
            request.setAttribute(ATTR_NONCE, nonce);
            RequestDispatcher dispatcher = getDispatcher(servletContext, contextPath, path);
            if (dispatcher != null) {
                dispatcher.forward(request, response);
            } else {
                Tr.error(tc,
                        "security.oauth20.endpoint.template.forward.error",
                        new Object[] { PARAM_AUTHZ_FORM_TEMPLATE, contextPath,
                                path });
            }
        } else {
            if (useDefaultTemplateValue) {
                templateUrl = null;
            } else {
                templateUrl = TemplateRetriever.normallizeTemplateUrl(request,
                        templateUrl);
            }
            String acceptLanguage = request.getHeader(HEADER_ACCEPT_LANGUAGE);
            if (renderer == null) {
                renderer = new FormRenderer();
            }
            String contextPath = request.getContextPath();
            // TODO REVISIT: in the case of reverse proxy, the authorization url
            // doesn't reflect the real url and
            // reverse proxy is not able to rewrite the value in JavasSript
            // code.
            String authorizationUrl = request.getRequestURL().toString();
            // PM85131: This ApAR solves no Cache Problems on OAUTH
            // HTTP 1.1.
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private, max-age=0");
            // HTTP 1.0.
            response.setHeader("Pragma", "no-cache");
            // Proxies.
            response.setDateHeader("Expires", 0);
            renderer.renderForm(client, templateUrl, contextPath,
                    authorizationUrl, nonce, attributes, acceptLanguage,
                    response, defaultAuthorizationFormTemplatecontent);
        }
    }

    private RequestDispatcher getDispatcher(ServletContext servletContext, String contextPath, String path) {
        RequestDispatcher retVal = null;
        ServletContext ctx = servletContext.getContext(contextPath);
        if (ctx != null) {
            retVal = ctx.getRequestDispatcher(path);
        }
        return retVal;
    }

    protected String[] getScope(String scopeString) {
        String[] scope = null;

        if (scopeString != null) {
            scope = scopeString.split(" ");
        }
        return scope;
    }

    public void handleNonceError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (isNonceExpired(request)) {
            response.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT);
        } else {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Return true if the Nonce from the HttpSession is valid,
     * i.e. it has the same value as the nonce from the HttpServletRequest
     * and is not expired
     *
     * @param request the HttpServletRequest
     * @param requestNonce the nonce String from the HttpServletRequest
     * @return true if expired
     */
    public boolean isNonceValid(HttpServletRequest request, String requestNonce) {
        Nonce sessionNonce = getSessionNonce(request);
        if (sessionNonce != null) {
            return sessionNonce.isValid(requestNonce);
        }
        return false;
    }

    /**
     * Return true if the Nonce from the HttpSession is expired
     *
     * @param request the HttpServletRequest
     * @return true if expired
     */
    boolean isNonceExpired(HttpServletRequest request) {
        Nonce sessionNonce = getSessionNonce(request);
        if (sessionNonce != null) {
            return sessionNonce.isExpired();
        }
        return false;
    }

    public Nonce setNonce(HttpServletRequest request) {
        Nonce nonce = Nonce.getInstance();
        request.getSession(true).setAttribute(ATTR_NONCE, nonce);
        request.setAttribute(ATTR_NONCE, nonce.getValue());
        return nonce;
    }

    private Nonce getSessionNonce(HttpServletRequest request) {
        Nonce sessionNonce = (Nonce) request.getSession().getAttribute(ATTR_NONCE);
        return sessionNonce;
    }
}
