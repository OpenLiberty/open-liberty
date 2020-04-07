/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.ComponentContext;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.common.web.CommonWebConstants;
import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.RedirectionEntry;
import com.ibm.ws.security.openidconnect.clients.common.RedirectionProcessor;
import com.ibm.ws.security.social.Constants;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.ErrorHandlerImpl;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.OidcLoginConfigImpl;
import com.ibm.ws.security.social.internal.utils.ClientConstants;
import com.ibm.ws.security.social.internal.utils.SocialLoginRequest;
import com.ibm.ws.security.social.internal.utils.SocialUtil;
import com.ibm.ws.security.social.twitter.TwitterConstants;
import com.ibm.ws.security.social.twitter.TwitterTokenServices;
import com.ibm.ws.security.social.web.utils.ConfigInfoJsonBuilder;
import com.ibm.ws.security.social.web.utils.SocialWebUtils;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

public class EndpointServices {
    private static TraceComponent tc = Tr.register(EndpointServices.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    static ConcurrentServiceReferenceMap<String, SocialLoginConfig> socialLoginConfigRef = null;
    static AtomicServiceReference<SecurityService> securityServiceRef = null;

    SocialWebUtils webUtils = new SocialWebUtils();

    public static void setActivatedSocialLoginConfigRef(ConcurrentServiceReferenceMap<String, SocialLoginConfig> socialLoginConfigRef) {
        EndpointServices.socialLoginConfigRef = socialLoginConfigRef;
    }

    public static void setActivatedSecurityServiceRef(AtomicServiceReference<SecurityService> securityServiceRef) {
        EndpointServices.securityServiceRef = securityServiceRef;
    }

    protected void activate(ComponentContext cc) {
        Tr.info(tc, "SOCIAL_LOGIN_ENDPOINT_SERVICE_ACTIVATED");
    }

    protected void deactivate(ComponentContext cc) {
    }

    protected void handleSocialLoginRequest(HttpServletRequest request, HttpServletResponse response) throws SocialLoginException {
        SocialLoginRequest socialLoginRequest = (SocialLoginRequest) request.getAttribute(Constants.ATTRIBUTE_SOCIALMEDIA_REQUEST);
        if (socialLoginRequest != null) {
            handleSocialLoginRequest(request, response, socialLoginRequest);
        } else {
            // this should not happen, since filter always add a socialLoginRequest
            // Just in case
            throw new SocialLoginException("SOCIAL_LOGIN_INVALID_URL", null, new Object[] { request.getRequestURL().toString() });
        }
    }

    /**
     * @param request
     * @param response
     * @param socialLoginRequest
     * @throws SocialLoginException
     */
    void handleSocialLoginRequest(HttpServletRequest request, HttpServletResponse response, SocialLoginRequest socialLoginRequest) throws SocialLoginException {

        if (socialLoginRequest.isRedirect()) { // we are probably receiving a redirect to the OP from the browser after getting a code.
            try {
                SocialLoginConfig config = socialLoginRequest.getSocialLoginConfig();
                if (config == null) {
                    throw new SocialLoginException("REDIRECT_NO_MATCHING_CONFIG", null, new Object[] { request.getRequestURL().toString() });
                }
                if (config.getClass().getName().contains(TwitterConstants.TWITTER_CONFIG_CLASS)) {
                    // Twitter doesn't follow the OAuth 2.0 flow, so Twitter requests must be handled differently
                    doTwitter(request, response, config);
                } else {
                    doRedirect(request, response, config);
                }
            } catch (Exception e) {
                throw new SocialLoginException("ERROR_PROCESSING_REDIRECT", null, new Object[] { e.getMessage() });
            }

        } else if (socialLoginRequest.isLogout()) {
            // handle logout
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "logout:" + socialLoginRequest.getRequestUrl());
            }
        } else if (socialLoginRequest.isWellknownConfig()) {
            // handle /.well-known/config
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, ".well-known/config:" + socialLoginRequest.getRequestUrl());
            }
            handleSocialLoginAPIRequest(request, response);
            // TODO
        } else if (socialLoginRequest.isUnknown()) {
            // handle /.well-known/config
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "unknown URL:" + socialLoginRequest.getRequestUrl());
            }
            // TODO - NLS revisit - message could be improved
            // SOCIAL_LOGIN_INVALID_URL=CWWKS5406E: The requested endpoint of [{0}] is not supported in this Social Login service provider.
            throw new SocialLoginException("SOCIAL_LOGIN_INVALID_URL", null, new Object[] { socialLoginRequest.getRequestUrl() });
        }

    }

    /**
     * @param SocialLoginRequest
     * @param BasicSocialLoginConfig
     * @return
     */
    protected Map<String, Object> getParameterMap(SocialLoginConfig socialLoginConfig) {
        HashMap<String, Object> results = new HashMap<String, Object>();
        results.put(Constants.KEY_SOCIALLOGIN_SERVICE, socialLoginConfig);
        if (securityServiceRef != null) {
            results.put(Constants.KEY_SECURITY_SERVICE, securityServiceRef.getService());
        }
        return results;
    }

    @FFDCIgnore(SocialLoginException.class)
    protected void doTwitter(HttpServletRequest request, HttpServletResponse response, SocialLoginConfig config) throws IOException {
        TwitterTokenServices ts = getTwitterTokenServices();
        Map<String, Object> result = ts.getAccessToken(request, response, config);
        if (result == null) {
            // Something went wrong, but error has already been logged
            ErrorHandlerImpl.getInstance().handleErrorResponse(response);
            return;
        }
        String token = (String) result.get(TwitterConstants.RESULT_ACCESS_TOKEN);
        String tokenSecret = (String) result.get(TwitterConstants.RESULT_ACCESS_TOKEN_SECRET);

        String state = getStateCookieValue(request, response);
        if (state == null || state.isEmpty()) {
            Tr.error(tc, "TWITTER_STATE_MISSING", new Object[] { config.getUniqueId() });
            ErrorHandlerImpl.getInstance().handleErrorResponse(response);
            return;
        }

        String requestUrl = getRequestUrlCookieValue(request, response, state);
        if (requestUrl == null || requestUrl.isEmpty()) {
            Tr.error(tc, "TWITTER_ORIGINAL_REQUEST_URL_MISSING_OR_EMPTY", new Object[] { config.getUniqueId() });
            ErrorHandlerImpl.getInstance().handleErrorResponse(response);
            return;
        }

        try {
            SocialUtil.validateEndpointWithQuery(requestUrl);
        } catch (SocialLoginException e) {
            Tr.error(tc, "REQUEST_URL_NOT_VALID", new Object[] { requestUrl, e.getMessage() });
            ErrorHandlerImpl.getInstance().handleErrorResponse(response);
            return;
        }
        cacheValueInCookie(request, response, TwitterConstants.COOKIE_NAME_ACCESS_TOKEN, token);
        cacheSensitiveValueInCookie(request, response, TwitterConstants.COOKIE_NAME_ACCESS_TOKEN_SECRET, tokenSecret);

        response.sendRedirect(requestUrl);
    }

    // dump parameter map for trace.
    @Trivial
    private String dumpMap(Map<String, String[]> m) {
        StringBuffer sb = new StringBuffer();
        sb.append(" --- request parameters: ---\n");
        Iterator<String> it = m.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            String[] values = m.get(key);
            sb.append(key + ": ");
            for (String s : values) {
                sb.append("[" + s + "] ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    //@FFDCIgnore(SocialLoginException.class)
    protected void doRedirect(HttpServletRequest request, final HttpServletResponse response, final SocialLoginConfig config) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, dumpMap(request.getParameterMap()));
        }
        // if we got error param, fail fast
        String error = request.getParameter(ClientConstants.ERROR);
        if (error != null) { // CWWKS5495E
            Tr.error(tc, "REDIRECT_REQUEST_CONTAINED_ERROR",
                    new Object[] {
                            request.getParameter(ClientConstants.ERROR),
                            request.getParameter(ClientConstants.ERROR_DESC),
                            request.getParameter(ClientConstants.ERROR_URI), });
            ErrorHandlerImpl.getInstance().handleErrorResponse(response);
            return;
        }

        String state = request.getParameter(ClientConstants.STATE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "state is " + state);
        }

        if (config instanceof OidcLoginConfigImpl) {
            // for oidc, handle redirect consistent with how the rest of oidc does it.
            // Use their cookies, etc.
        	RedirectionProcessor redirectionProcessor = new RedirectionProcessor(request, response, tc);
			redirectionProcessor.processRedirection(new RedirectionEntry() {

				@Override
				public ConvergedClientConfig getConvergedClientConfig(HttpServletRequest request, String clientId) {
					return (ConvergedClientConfig) config;
				}

				@Override
				public void handleNoState(HttpServletRequest request, HttpServletResponse response) throws IOException {
					traceAndSetResponseForNoState(response);
				}

				@Override
				public void sendError(HttpServletRequest request, HttpServletResponse response) throws IOException {
					throw new UnsupportedOperationException();
				}
			});
        } else {
            finishOAuthRedirect(request, response, config);
        }

    }
    
    private void traceAndSetResponseForNoState(HttpServletResponse response) {
    	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "The state is null");
        }
        Tr.error(tc, "STATE_NULL_OR_MISMATCHED", new Object[] {});
        ErrorHandlerImpl.getInstance().handleErrorResponse(response);
    }

    private void finishOAuthRedirect(HttpServletRequest request, HttpServletResponse response, SocialLoginConfig config) throws IOException {
        String state = request.getParameter(ClientConstants.STATE);
        
        if (state == null || state.isEmpty()) {
        	traceAndSetResponseForNoState(response);
            return;
        }
        
        String stateCookie = getStateCookieValue(request, response);
        if (!state.equals(stateCookie)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The state mismatches");
            }
            // state mismatch
            Tr.error(tc, "STATE_NULL_OR_MISMATCHED", new Object[] {});
            ErrorHandlerImpl.getInstance().handleErrorResponse(response);
            return;
        }

        String requestUrl = getRequestUrlCookieValue(request, response, state);

        if (requestUrl == null || requestUrl.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "requestURL is null or empty");
            }
            Tr.error(tc, "REQUEST_URL_NULL_OR_EMPTY", new Object[] {});
            ErrorHandlerImpl.getInstance().handleErrorResponse(response);
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "requestURL is not null or empty");
        }

        try {
            SocialUtil.validateEndpointWithQuery(requestUrl);
        } catch (SocialLoginException e) {
            Tr.error(tc, "REQUEST_URL_NOT_VALID", new Object[] { requestUrl, e.getMessage() });
            ErrorHandlerImpl.getInstance().handleErrorResponse(response);
            return;
        }

        String code = request.getParameter(ClientConstants.CODE);
        if (code == null || code.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "code parameter in request is null or empty, return internal error");
            }
            Tr.error(tc, "CODE_PARAMETER_NULL_OR_EMPTY", new Object[] {});
            ErrorHandlerImpl.getInstance().handleErrorResponse(response);
            return;
        }
        cacheValueInCookie(request, response, ClientConstants.COOKIE_NAME_STATE_KEY, code);

        response.sendRedirect(requestUrl); // send back to protected resource
    }

    protected String getStateCookieValue(HttpServletRequest request, HttpServletResponse response) {
        return webUtils.getAndClearCookie(request, response, ClientConstants.COOKIE_NAME_STATE_KEY);
    }

    protected String getRequestUrlCookieValue(HttpServletRequest request, HttpServletResponse response, String state) {
        String reqUrlCookieName = ClientConstants.COOKIE_NAME_REQ_URL_PREFIX + state.hashCode();
        String requestUrl = webUtils.getAndClearCookie(request, response, reqUrlCookieName);
        requestUrl = decodeAndNormalizeRequestUrl(requestUrl);
        if (tc.isDebugEnabled() && requestUrl != null) {
            Tr.debug(tc, "The restored request Url: " + requestUrl);
        }
        return requestUrl;
    }

    String decodeAndNormalizeRequestUrl(String requestUrl) {
        try {
            requestUrl = java.net.URLDecoder.decode(requestUrl, "UTF-8");
            // Spaces must remain encoded
            requestUrl = requestUrl.replaceAll(" ", "%20");
        } catch (Exception e) {
            // Should not happen since UTF-8 must be supported
        }
        return requestUrl;
    }

    protected void cacheValueInCookie(HttpServletRequest req, HttpServletResponse resp, String cookieName, String value) {
        if (value == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Value to store in cookie is null, so no cookie will be created");
            }
            return;
        }
        ReferrerURLCookieHandler referrerURLCookieHandler = getReferrerUrlCookieHandler();
        Cookie c = referrerURLCookieHandler.createCookie(cookieName, value, req);
        resp.addCookie(c);
    }

    ReferrerURLCookieHandler getReferrerUrlCookieHandler() {
        return WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig().createReferrerURLCookieHandler();
    }

    protected void cacheSensitiveValueInCookie(HttpServletRequest req, HttpServletResponse resp, String cookieName, @Sensitive String value) {
        if (value == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Value to store in cookie is null, so no cookie will be created");
            }
            return;
        }
        ReferrerURLCookieHandler referrerURLCookieHandler = getReferrerUrlCookieHandler();
        Cookie c = referrerURLCookieHandler.createCookie(cookieName, value, req);
        resp.addCookie(c);
    }

    /**
     * @param request
     * @param response
     * @param socialLoginRequest
     */
    protected void handleSocialLoginAPIRequest(HttpServletRequest request, HttpServletResponse response) {
        JSONObject json = getAllSocialLoginConfigs();
        writeToResponse(json, response);
    }

    /**
     * @param json
     * @param response
     */
    private void writeToResponse(JSONObject json, HttpServletResponse response) {

        if (json == null || json.toString() == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Provided JSON object is null");
            }
            ErrorHandlerImpl.getInstance().handleErrorResponse(response);
            return;
        }
        addNoCacheHeaders(response);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "socialLoginConfigs json :" + json.toString());
            // could be null when wellknown
        }
        response.setStatus(200);
        try {
            PrintWriter pw = response.getWriter();
            response.setHeader(CommonWebConstants.HTTP_HEADER_CONTENT_TYPE, CommonWebConstants.HTTP_CONTENT_TYPE_JSON);
            pw.write(json.toString());
            pw.flush();
            pw.close();
        } catch (IOException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught an exception attempting to get the response writer: " + e.getLocalizedMessage());
            }
        }

    }

    /**
     * @param response
     */
    private void addNoCacheHeaders(HttpServletResponse response) {
        String cacheControlValue = response.getHeader(CommonWebConstants.HEADER_CACHE_CONTROL);

        if (cacheControlValue != null && !cacheControlValue.isEmpty()) {
            cacheControlValue = cacheControlValue + ", " + CommonWebConstants.CACHE_CONTROL_NO_STORE;
        } else {
            cacheControlValue = CommonWebConstants.CACHE_CONTROL_NO_STORE;
        }

        response.setHeader(CommonWebConstants.HEADER_CACHE_CONTROL, cacheControlValue);
        response.setHeader(CommonWebConstants.HEADER_PRAGMA, CommonWebConstants.PRAGMA_NO_CACHE);

    }

    /**
     * @param response
     * @param socialLoginRequest
     * @return
     */
    JSONObject getAllSocialLoginConfigs() {
        if (socialLoginConfigRef == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Social login config reference not set");
            }
            return new JSONObject();
        }
        Iterator<SocialLoginConfig> configIt = socialLoginConfigRef.getServices();
        ConfigInfoJsonBuilder configJsonBuilder = new ConfigInfoJsonBuilder(configIt);
        return configJsonBuilder.buildJsonResponse();
    }

    protected TwitterTokenServices getTwitterTokenServices() {
        // Primarily for unit testing assistance
        return new TwitterTokenServices();
    }

}
