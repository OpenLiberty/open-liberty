/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.api.oauth20.token.OAuth20TokenCache;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20Client;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.util.CacheUtil;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;
import com.ibm.ws.security.oauth20.web.OAuthClientTracker;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

import io.openliberty.security.openidconnect.backchannellogout.BackchannelLogoutRequestHelper;

/**
 * Handles logic to support OpenID Connect RP-Initiated Logout.
 *
 * See https://openid.net/specs/openid-connect-rpinitiated-1_0.html.
 */
public class OidcRpInitiatedLogout {

    private static TraceComponent tc = Tr.register(OidcRpInitiatedLogout.class);

    private final OidcEndpointServices endpointServices;
    private final OAuth20Provider oauth20Provider;
    private final OidcServerConfig oidcServerConfig;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    /**
     * @param oauth20Provider  extracted from the request
     * @param oidcServerConfig is the object of oidc server configuration object
     * @param request          is the incoming HttpServletRequest
     * @param response         WAS OIDC response for a given provider
     */
    public OidcRpInitiatedLogout(OidcEndpointServices endpointServices, OAuth20Provider oauth20Provider, OidcServerConfig oidcServerConfig, HttpServletRequest request,
                                 HttpServletResponse response) {
        this.endpointServices = endpointServices;
        this.oauth20Provider = oauth20Provider;
        this.oidcServerConfig = oidcServerConfig;
        this.request = request;
        this.response = response;
    }

    /**
     * process end session task which includes:
     * - delete LTPAToken cookie.
     * - delete refresh token from tokencache if id_token_hint is present.
     * - redirect a request to a URL which is specified by post_logout_redirect_uri
     */
    protected void processEndSession() throws ServletException, IOException {
        OidcRpInitiatedLogoutTokenAndRequestData tokenAndRequestData = new OidcRpInitiatedLogoutTokenAndRequestData(request, endpointServices, oauth20Provider, oidcServerConfig);
        tokenAndRequestData.populate();

        boolean isDataValidForLogout = tokenAndRequestData.isDataValidForLogout();
        if (isDataValidForLogout) {
            removeRefreshTokenFromCache(tokenAndRequestData.getCachedIdToken());
        }

        String redirectUri = getPostLogoutRedirectUri(tokenAndRequestData);

        //@AV999-092821
        request.setAttribute("OIDC_END_SESSION_REDIRECT", redirectUri);
        if (isDataValidForLogout) {
            logOutUser(tokenAndRequestData, redirectUri);
        }
        if (request.getAttribute("OIDC_END_SESSION_REDIRECT") != null) {
            sendPostEndSessionRedirect(redirectUri);
        }
    }

    void removeRefreshTokenFromCache(OAuth20Token cachedIdToken) {
        if (cachedIdToken == null) {
            return;
        }
        OAuth20TokenCache tokenCache = oauth20Provider.getTokenCache();
        if (tokenCache != null) {
            CacheUtil cu = new CacheUtil(tokenCache);
            OAuth20Token refreshToken = cu.getRefreshToken(cachedIdToken);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "refreshToken : " + refreshToken);
            }
            if (refreshToken != null) {
                tokenCache.remove(refreshToken.getTokenString());
            }
        }
    }

    String getPostLogoutRedirectUri(OidcRpInitiatedLogoutTokenAndRequestData requestData) {
        String redirectUri = requestData.getPostLogoutRedirectUriParameter();
        if (!requestData.isDataValidForLogout()) {
            // this is an error condition. display an error page.
            redirectUri = request.getContextPath() + "/end_session_error.html";
        } else {
            if (redirectUri == null) {
                // no redirectUri is set, use default.
                redirectUri = request.getContextPath() + "/end_session_logout.html";
            } else {
                redirectUri = verifyPostLogoutRedirectUriMatchesConfiguration(requestData.getClientId(), redirectUri);
            }
        }
        redirectUri = updateRedirectUriWithState(redirectUri, requestData.getState());
        if (oauth20Provider.isTrackOAuthClients()) {
            redirectUri = updateRedirectUriWithTrackedOAuthClients(redirectUri);
        }
        return redirectUri;
    }

    String verifyPostLogoutRedirectUriMatchesConfiguration(String clientId, String redirectUri) {
        try {
            String[] uris = getPostLogoutRedirectUris(clientId);
            if (!containUri(redirectUri, uris)) {
                // post_logout_redirect_uri is not a member of post_logout_redirect_uris, force to redirect to the default logout page.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    if (clientId == null) {
                        Tr.debug(tc,
                                 "postLogoutRedirectUri value cannot be identified because client id is not set. Most likely this is because the id_token_hint parameter is not set or invalid.");
                    }
                }
                Tr.error(tc, "OIDC_SERVER_LOGOUT_REDIRECT_URI_MISMATCH", new Object[] { redirectUri, Arrays.toString(uris), clientId });
                redirectUri = request.getContextPath() + "/end_session_logout.html";

            }
        } catch (OidcServerException ose) {
            // this should not happen.
            Tr.error(tc, "OIDC_SERVER_IDTOKEN_VERIFY_ERR", new Object[] { ose });
            // this is an error condition. display an error page.
            redirectUri = request.getContextPath() + "/end_session_error.html";
        }
        return redirectUri;
    }

    String[] getPostLogoutRedirectUris(String clientId) throws OidcServerException {
        String[] uris = null;
        if (clientId != null) {
            OidcOAuth20ClientProvider clientProvider = oauth20Provider.getClientProvider();
            OidcOAuth20Client oauth20Client = clientProvider.get(clientId);
            if (oauth20Client instanceof OidcBaseClient) {
                OidcBaseClient baseClient = (OidcBaseClient) oauth20Client;
                uris = OidcOAuth20Util.getStringArray(baseClient.getPostLogoutRedirectUris());
            }
        }
        return uris;
    }

    /**
     * Checks whether the given string is contained in the string array.
     */
    boolean containUri(String uri, String[] uris) {
        boolean contain = false;
        if (uris != null && uris.length > 0 && uri != null) {
            for (int i = 0; i < uris.length; i++) {
                if (uri.equals(uris[i])) {
                    contain = true;
                    break;
                }
            }
        }
        return contain;
    }

    /**
     * State is optional, but if it's passed in the logout request, the OP passes the value back when redirecting to the RP.
     */
    @FFDCIgnore(UnsupportedEncodingException.class)
    String updateRedirectUriWithState(String redirectUri, String state) {
        if (state == null) {
            return redirectUri;
        }
        redirectUri = (redirectUri.contains("?")) ? (redirectUri + "&") : (redirectUri + "?");
        try {
            redirectUri += URLEncoder.encode(OIDCConstants.STATE, "UTF-8") + "=" + URLEncoder.encode(state, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Do nothing
        }
        return redirectUri;
    }

    String updateRedirectUriWithTrackedOAuthClients(String redirectUri) {
        OAuthClientTracker clientTracker = new OAuthClientTracker(request, response, oauth20Provider);
        return clientTracker.updateLogoutUrlAndDeleteCookie(redirectUri);
    }

    void logOutUser(OidcRpInitiatedLogoutTokenAndRequestData requestData, String redirectUri) throws ServletException {
        if (requestData.getUserPrincipal() != null) {
            // logout deletes ltpatoken cookie and oidc_bsc cookie.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "save  OIDC_END_SESSION_REDIRECT uri in op end_session : " + redirectUri);
            }
            //if during the servlet request logout, if the other logouts are in play, then we may not want to redirect here in that case
            request.logout();
        } else {
            // request.logout() will send back-channel logout requests via the LogoutService OSGi service. Since request.logout()
            // is only called in the above block if user != null, we need to make sure back-channel logout requests are still sent
            // based on the id_token_hint if a user Principal isn't available
            sendBackchannelLogoutRequests(requestData.getUserPrincipalName(), requestData.getIdTokenHintParameter());
        }
    }

    void sendBackchannelLogoutRequests(String userName, String idTokenString) {
        BackchannelLogoutRequestHelper bclRequestCreator = new BackchannelLogoutRequestHelper(request, oidcServerConfig);
        bclRequestCreator.sendBackchannelLogoutRequests(userName, idTokenString);
    }

    void sendPostEndSessionRedirect(String redirectUri) throws IOException {
        request.removeAttribute("OIDC_END_SESSION_REDIRECT");
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "OIDC _SSO OP redirecting to [" + redirectUri + "]");
        }
        response.sendRedirect(redirectUri);
    }

}
