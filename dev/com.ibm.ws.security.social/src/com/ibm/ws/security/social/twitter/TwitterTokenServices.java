/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.twitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.error.ErrorHandlerImpl;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.utils.ClientConstants;
import com.ibm.ws.security.social.internal.utils.SocialUtil;
import com.ibm.ws.security.social.web.utils.SocialWebUtils;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;

public class TwitterTokenServices {

    private static TraceComponent tc = Tr.register(TwitterTokenServices.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    SocialWebUtils webUtils = new SocialWebUtils();

    protected TwitterEndpointServices getTwitterEndpointServices() {
        return new TwitterEndpointServices();
    }

    /**
     * Attemps to obtain a request token from the oauth/request_token Twitter API. This request token can later be used to
     * obtain an access token. If a request token is successfully obtained, the request is redirected to the oauth/authorize
     * Twitter API to have Twitter authenticate the user and allow the user to authorize the application to access Twitter data.
     *
     * @param request
     * @param response
     * @param callbackUrl
     *            URL that Twitter should redirect to with the oauth_token and oauth_verifier parameters once the request token is
     *            issued.
     * @param stateValue
     * @param config
     */
    public void getRequestToken(HttpServletRequest request, HttpServletResponse response, String callbackUrl, String stateValue, SocialLoginConfig config) {
        TwitterEndpointServices twitter = getTwitterEndpointServices();
        twitter.setConsumerKey(config.getClientId());
        twitter.setConsumerSecret(config.getClientSecret());

        Map<String, Object> result = twitter.obtainRequestToken(config, callbackUrl);
        if (result == null || result.isEmpty()) {
            Tr.error(tc, "TWITTER_ERROR_OBTAINING_ENDPOINT_RESULT", new Object[] { TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN });
            ErrorHandlerImpl.getInstance().handleErrorResponse(response);
            return;
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN + " result: " + result.toString());
        }

        try {
            if (!isSuccessfulResult(result, TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN)) {
                ErrorHandlerImpl.getInstance().handleErrorResponse(response);
                return;
            }

            // If successful, the result has already been verified to contain a non-empty oauth_token and oauth_token_secret
            String requestToken = (String) result.get(TwitterConstants.RESPONSE_OAUTH_TOKEN);

            // Cache request token to verify against later
            setCookies(request, response, requestToken, stateValue);

            // Redirect to authorization endpoint with the provided request token
            String authzEndpoint = config.getAuthorizationEndpoint();
            try {
                SocialUtil.validateEndpointWithQuery(authzEndpoint);
            } catch (SocialLoginException e) {
                Tr.error(tc, "FAILED_TO_REDIRECT_TO_AUTHZ_ENDPOINT", new Object[] { config.getUniqueId(), e });
                ErrorHandlerImpl.getInstance().handleErrorResponse(response);
                return;
            }
            String queryChar = (authzEndpoint.contains("?")) ? "&" : "?";
            response.sendRedirect(authzEndpoint + queryChar + TwitterConstants.PARAM_OAUTH_TOKEN + "=" + requestToken);

        } catch (IOException e) {
            Tr.error(tc, "TWITTER_REDIRECT_IOEXCEPTION", new Object[] { TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN, e.getLocalizedMessage() });
            ErrorHandlerImpl.getInstance().handleErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
    }

    /**
     * Attemps to obtain an access token from the oauth/access_token Twitter API.
     *
     * @param request
     * @param response
     */
    @Sensitive
    public Map<String, Object> getAccessToken(HttpServletRequest request, HttpServletResponse response, SocialLoginConfig config) {
        TwitterEndpointServices twitter = getTwitterEndpointServices();
        twitter.setConsumerKey(config.getClientId());
        twitter.setConsumerSecret(config.getClientSecret());

        Map<String, String[]> params = request.getParameterMap();

        if (isMissingParameter(params, TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN)) {
            return null;
        }

        // oauth_token provided in request MUST match the request token obtained earlier
        String token = request.getParameter(TwitterConstants.PARAM_OAUTH_TOKEN);
        String cachedRequestToken = webUtils.getAndClearCookie(request, response, TwitterConstants.COOKIE_NAME_REQUEST_TOKEN);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Obtained token from request: [" + token + "], matching against: [" + cachedRequestToken + "]");
        }

        if (!token.equals(cachedRequestToken)) {
            Tr.error(tc, "TWITTER_TOKEN_DOES_NOT_MATCH");
            return null;
        }

        String oauthVerifier = request.getParameter(TwitterConstants.PARAM_OAUTH_VERIFIER);

        Map<String, Object> result = twitter.obtainAccessToken(config, cachedRequestToken, oauthVerifier);
        if (result == null || result.isEmpty()) {
            Tr.error(tc, "TWITTER_ERROR_OBTAINING_ENDPOINT_RESULT", new Object[] { TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN });
            return null;
        }

        if (!isSuccessfulResult(result, TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN)) {
            return null;
        }

        // If result was successful, the result has already been verified to contain a non-empty access_token and access_token_secret
        return result;
    }

    /**
     * Attemps to obtain a value to use for the user subject from the /1.1/account/verify_credentials.json Twitter API.
     * Specifically this is looking to obtain an email address associated with the Twitter user.
     *
     * @param response
     * @param accessToken
     * @param accessTokenSecret
     */
    public Map<String, Object> verifyCredentials(HttpServletResponse response, String accessToken, @Sensitive String accessTokenSecret, SocialLoginConfig config) {
        TwitterEndpointServices twitter = getTwitterEndpointServices();
        twitter.setConsumerKey(config.getClientId());
        twitter.setConsumerSecret(config.getClientSecret());

        Map<String, Object> creds = twitter.verifyCredentials(config, accessToken, accessTokenSecret);
        if (creds == null || creds.isEmpty()) {
            Tr.error(tc, "TWITTER_ERROR_OBTAINING_ENDPOINT_RESULT", new Object[] { TwitterConstants.TWITTER_ENDPOINT_VERIFY_CREDENTIALS });
            return null;
        }

        if (!isSuccessfulResult(creds, TwitterConstants.TWITTER_ENDPOINT_VERIFY_CREDENTIALS)) {
            return null;
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.putAll(creds);
        result.put(ClientConstants.ACCESS_TOKEN, accessToken);

        return result;
    }

    /**
     * Sets cookies for the provided request token and the original request URL. These values can then be verified and used in
     * subsequent requests.
     *
     * @param request
     * @param response
     * @param requestToken
     * @param stateValue
     */
    protected void setCookies(HttpServletRequest request, HttpServletResponse response, String requestToken, String stateValue) {
        ReferrerURLCookieHandler referrerURLCookieHandler = WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig().createReferrerURLCookieHandler();

        // Create cookie for request token
        Cookie requestTokenCookie = referrerURLCookieHandler.createCookie(TwitterConstants.COOKIE_NAME_REQUEST_TOKEN, requestToken, request);
        response.addCookie(requestTokenCookie);

        // Set original request URL in cookie
        String cookieName = ClientConstants.COOKIE_NAME_REQ_URL_PREFIX + stateValue.hashCode();
        Cookie c = referrerURLCookieHandler.createCookie(cookieName, webUtils.getRequestUrlWithEncodedQueryString(request), request);
        response.addCookie(c);

    }

    /**
     * Checks for required parameters depending on the endpoint type.
     * - oauth/access_token: Must have oauth_token and oauth_verifier parameters in order to continue
     *
     * @param requestParams
     * @param endpoint
     * @return
     */
    protected boolean isMissingParameter(Map<String, String[]> requestParams, String endpoint) {
        if (TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN.equals(endpoint)) {
            // Must have oauth_token and oauth_verifier parameters in order to send request to oauth/access_token endpoint
            if (!requestParams.containsKey(TwitterConstants.PARAM_OAUTH_TOKEN)) {
                Tr.error(tc, "TWITTER_REQUEST_MISSING_PARAMETER", new Object[] { TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN, TwitterConstants.PARAM_OAUTH_TOKEN });
                return true;
            }
            if (!requestParams.containsKey(TwitterConstants.PARAM_OAUTH_VERIFIER)) {
                Tr.error(tc, "TWITTER_REQUEST_MISSING_PARAMETER", new Object[] { TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN, TwitterConstants.PARAM_OAUTH_VERIFIER });
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether the result was successful by looking at the response status value contained in the result.
     *
     * @param result
     *            May contain token secrets, so has been annotated as @Sensitive.
     * @param endpoint
     * @return
     */
    protected boolean isSuccessfulResult(@Sensitive Map<String, Object> result, String endpoint) {
        if (result == null) {
            Tr.error(tc, "TWITTER_ERROR_OBTAINING_ENDPOINT_RESULT", new Object[] { endpoint });
            return false;
        }
        String responseStatus = result.containsKey(TwitterConstants.RESULT_RESPONSE_STATUS) ? (String) result.get(TwitterConstants.RESULT_RESPONSE_STATUS) : null;
        String responseMsg = result.containsKey(TwitterConstants.RESULT_MESSAGE) ? (String) result.get(TwitterConstants.RESULT_MESSAGE) : null;

        if (responseStatus == null) {
            Tr.error(tc, "TWITTER_RESPONSE_STATUS_MISSING", new Object[] { endpoint });
            return false;
        }

        if (!responseStatus.equals(TwitterConstants.RESULT_SUCCESS)) {
            Tr.error(tc, "TWITTER_RESPONSE_FAILURE", new Object[] { endpoint, (responseMsg == null) ? "" : responseMsg });
            return false;
        }

        return true;
    }

}
