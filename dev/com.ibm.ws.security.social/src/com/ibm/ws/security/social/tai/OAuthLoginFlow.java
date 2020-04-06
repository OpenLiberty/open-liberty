/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.tai;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.social.Constants;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.utils.ClientConstants;
import com.ibm.ws.security.social.internal.utils.SocialUtil;
import com.ibm.ws.security.social.web.utils.SocialWebUtils;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.wsspi.security.tai.TAIResult;

public class OAuthLoginFlow {

    public static final TraceComponent tc = Tr.register(OAuthLoginFlow.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    ReferrerURLCookieHandler referrerURLCookieHandler = null;
    TAIWebUtils taiWebUtils = new TAIWebUtils();
    SocialWebUtils webUtils = new SocialWebUtils();

    public OAuthLoginFlow() {
        referrerURLCookieHandler = taiWebUtils.getCookieHandler();
    }

    TAIResult handleOAuthRequest(HttpServletRequest request, HttpServletResponse response, SocialLoginConfig clientConfig) throws WebTrustAssociationFailedException {
        if (SocialUtil.useAccessTokenFromRequest(clientConfig) && isBeforeSSO(request)) {
            TAIResult result = handleAccessTokenFlow(request, response, clientConfig);
            if (clientConfig.isAccessTokenRequired() || (result != null && result.getSubject() != null)) {
                return result;
            }
            // see if we have a valid LTPA cookie to handle before continue with regular oauth login
            return TAIResult.create(HttpServletResponse.SC_CONTINUE);
        }
        String code = webUtils.getAndClearCookie(request, response, ClientConstants.COOKIE_NAME_STATE_KEY);
        if (code == null) {
            return handleRedirectToServer(request, response, clientConfig);
        } else {
            return handleAuthorizationCode(request, response, code, clientConfig);
        }
    }

    private boolean isBeforeSSO(HttpServletRequest request) {
        if (request.getAttribute(Constants.ATTRIBUTE_TAI_BEFORE_SSO_REQUEST) != null) {
            request.removeAttribute(Constants.ATTRIBUTE_TAI_BEFORE_SSO_REQUEST);
            return true;
        }
        return false;
    }

    private TAIResult handleAccessTokenFlow(HttpServletRequest request, HttpServletResponse response, SocialLoginConfig clientConfig) throws WebTrustAssociationFailedException {
        TAIResult result = null;
        //request should have token
        String tokenFromRequest = taiWebUtils.getBearerAccessToken(request, clientConfig);
        if (requestShouldHaveToken(clientConfig)) {
            if (isAccessTokenNullOrEmpty(tokenFromRequest)) {
                Tr.error(tc, "ACCESS_TOKEN_MISSING_FROM_HEADERS", clientConfig.getUniqueId());
                return taiWebUtils.sendToErrorPage(response, TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED));
            }
            return handleAccessToken(tokenFromRequest, request, response, clientConfig);
        } else if (!isAccessTokenNullOrEmpty(tokenFromRequest)) {
            // request may have token
            try {
                return handleAccessToken(tokenFromRequest, request, response, clientConfig);
            } catch (Exception e) {

            }
        }
        return result;
    }

    private boolean isAccessTokenNullOrEmpty(@Sensitive String tokenFromRequest) {
        if (tokenFromRequest == null || tokenFromRequest.isEmpty()) {
            return true;
        }
        return false;
    }

    @FFDCIgnore(SocialLoginException.class)
    private TAIResult handleAccessToken(@Sensitive String tokenFromRequest, HttpServletRequest request, HttpServletResponse response, SocialLoginConfig clientConfig) throws WebTrustAssociationFailedException {
        AuthorizationCodeAuthenticator authzCodeAuthenticator = new AuthorizationCodeAuthenticator(request, response, clientConfig, tokenFromRequest, true);
        try {
            authzCodeAuthenticator.generateJwtAndTokensFromAccessOrServiceAccountToken();
        } catch (SocialLoginException e) {
            if (!clientConfig.isAccessTokenRequired() && clientConfig.isAccessTokenSupported()) {
                taiWebUtils.restorePostParameters(request); //TODO: make sure that we really need to do this here.
                return null;
            }
            // Error should have already been logged; simply send to error page
            return taiWebUtils.sendToErrorPage(response, TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED));
        }
        return buildTAIResultFromAuthzCodeAuthenticator(request, response, clientConfig, authzCodeAuthenticator);

    }

    private boolean requestShouldHaveToken(SocialLoginConfig clientConfig) {
        return clientConfig.isAccessTokenRequired();
    }

    @FFDCIgnore(SocialLoginException.class)
    TAIResult handleRedirectToServer(HttpServletRequest req, HttpServletResponse res, SocialLoginConfig clientConfig) throws WebTrustAssociationFailedException {
        try {
            req.getSession(true);
        } catch (Exception e) {
            // ignore it. Session exists
        }
        String stateValue = taiWebUtils.createStateCookie(req, res);
        String redirect_url = taiWebUtils.getRedirectUrl(req, clientConfig);
        try {
            taiWebUtils.savePostParameters(req);

            String acr_values = req.getParameter("acr_values");
            String authzEndPointUrlWithQuery = buildAuthorizationUrlWithQuery(stateValue, clientConfig, redirect_url, acr_values);

            // Redirect to OP
            // If clientSideRedirect is true (default is true) then do the redirect
            if (clientConfig.isClientSideRedirectSupported()) {
                webUtils.doClientSideRedirect(res, ClientConstants.COOKIE_NAME_REQ_URL_PREFIX + stateValue.hashCode(), authzEndPointUrlWithQuery);
                return TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
            } else {
                String cookieName = ClientConstants.COOKIE_NAME_REQ_URL_PREFIX + stateValue.hashCode();
                Cookie c = referrerURLCookieHandler.createCookie(cookieName, webUtils.getRequestUrlWithEncodedQueryString(req), req);
                res.addCookie(c);
                res.sendRedirect(authzEndPointUrlWithQuery);
                return TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
            }
        } catch (SocialLoginException e) {
            Tr.error(tc, "FAILED_TO_REDIRECT_TO_AUTHZ_ENDPOINT", new Object[] { clientConfig.getUniqueId(), e.getLocalizedMessage() });
        } catch (IOException e) {
            Tr.error(tc, "FAILED_TO_REDIRECT_TO_AUTHZ_ENDPOINT", new Object[] { clientConfig.getUniqueId(), e.getLocalizedMessage() });
        }
        return taiWebUtils.sendToErrorPage(res, TAIResult.create(HttpServletResponse.SC_FORBIDDEN));
    }

    String buildAuthorizationUrlWithQuery(String state, SocialLoginConfig clientConfig, String redirectUrl, String acr_values) throws SocialLoginException {
        if (state == null) {
            throw new SocialLoginException("STATE_IS_NULL", null, new Object[] { clientConfig.getUniqueId() });
        }
        if (redirectUrl == null) {
            throw new SocialLoginException("REDIRECT_URL_IS_NULL", null, new Object[] { clientConfig.getUniqueId() });
        }

        String authzEndpoint = taiWebUtils.getAuthorizationEndpoint(clientConfig);

        String strResponse_type = clientConfig.getResponseType();

        String clientId = clientConfig.getClientId();
        if (clientId == null) {
            Tr.warning(tc, "OUTGOING_REQUEST_MISSING_PARAMETER", new Object[] { authzEndpoint, ClientConstants.CLIENT_ID });
            clientId = "";
        }
        String scope = clientConfig.getScope();
        if (scope == null) {
            scope = "";
        }
        String query = "";
        try {
            query = String.format(ClientConstants.RESPONSE_TYPE + "=%s&" + ClientConstants.CLIENT_ID + "=%s&" + ClientConstants.STATE + "=%s&" + ClientConstants.REDIRECT_URI + "=%s&" + ClientConstants.SCOPE + "=%s",
                    URLEncoder.encode(strResponse_type, ClientConstants.CHARSET),
                    URLEncoder.encode(clientId, ClientConstants.CHARSET),
                    URLEncoder.encode(state, ClientConstants.CHARSET),
                    URLEncoder.encode(redirectUrl, ClientConstants.CHARSET),
                    URLEncoder.encode(scope, ClientConstants.CHARSET));

            if (clientConfig.createNonce()) {
                String nonceValue = SocialUtil.generateRandom();
                // TODO: cache it for later checking
                query = String.format("%s&nonce=%s", query, URLEncoder.encode(nonceValue, ClientConstants.CHARSET));
            }
            if (acr_values != null && !acr_values.isEmpty()) {
                query = String.format("%s&acr_values=%s", query, URLEncoder.encode(acr_values, ClientConstants.CHARSET));

            }
            query = addResponseModeToQuery(query, clientConfig);
            if (!strResponse_type.equals(ClientConstants.CODE)) {
                String resources = clientConfig.getResource();
                if (resources != null) {
                    query = String.format("%s&%s", query, URLEncoder.encode(resources, ClientConstants.CHARSET));
                }
            }
        } catch (UnsupportedEncodingException e) {
            // Shouldn't happen, so deciding not to create an NLS message for this case
            throw new SocialLoginException(e);
        }
        String s = authzEndpoint + "?" + query;
        return s;
    }

    String addResponseModeToQuery(String query, SocialLoginConfig config) throws UnsupportedEncodingException {
        String responseMode = config.getResponseMode();
        String responseType = config.getResponseType();
        if (!responseType.equals(ClientConstants.CODE)) {
            // Implicit types will always use the form_post response mode
            responseMode = ClientConstants.FORM_POST;
        }
        if (responseMode != null) {
            query = String.format("%s&" + ClientConstants.RESPONSE_MODE + "=%s", query, URLEncoder.encode(responseMode, ClientConstants.CHARSET));
        }
        return query;
    }

    TAIResult handleAuthorizationCode(HttpServletRequest req, HttpServletResponse res, String authzCode, SocialLoginConfig clientConfig) throws WebTrustAssociationFailedException {
        AuthorizationCodeAuthenticator authzCodeAuthenticator = getAuthorizationCodeAuthenticator(req, res, authzCode, clientConfig);
        try {
            authzCodeAuthenticator.generateJwtAndTokenInformation();
        } catch (SocialLoginException e) {
            // Error should have already been logged; simply send to error page
            return taiWebUtils.sendToErrorPage(res, TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED));
        }
        return buildTAIResultFromAuthzCodeAuthenticator(req, res, clientConfig, authzCodeAuthenticator);
    }

    AuthorizationCodeAuthenticator getAuthorizationCodeAuthenticator(HttpServletRequest req, HttpServletResponse res, String authzCode, SocialLoginConfig clientConfig) {
        return new AuthorizationCodeAuthenticator(req, res, authzCode, clientConfig);
    }

    TAIResult buildTAIResultFromAuthzCodeAuthenticator(HttpServletRequest request, HttpServletResponse response, SocialLoginConfig clientConfig, AuthorizationCodeAuthenticator authzCodeAuthenticator) throws WebTrustAssociationFailedException {
        TAIResult authnResult = null;
        try {
            TAISubjectUtils subjectUtils = getTAISubjectUtils(authzCodeAuthenticator);
            authnResult = subjectUtils.createResult(response, clientConfig);
        } catch (Exception e) {
            Tr.error(tc, "AUTH_CODE_ERROR_CREATING_RESULT", new Object[] { clientConfig.getUniqueId(), e.getLocalizedMessage() });
            return taiWebUtils.sendToErrorPage(response, TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED));
        }
        taiWebUtils.restorePostParameters(request);
        return authnResult;
    }

    TAISubjectUtils getTAISubjectUtils(AuthorizationCodeAuthenticator authzCodeAuthenticator) {
        return new TAISubjectUtils(authzCodeAuthenticator);
    }

}
