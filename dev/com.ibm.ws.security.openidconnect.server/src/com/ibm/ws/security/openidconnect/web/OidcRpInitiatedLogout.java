/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.web;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.api.oauth20.token.OAuth20TokenCache;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20Client;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.util.CacheUtil;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;
import com.ibm.ws.security.oauth20.web.OAuthClientTracker;
import com.ibm.ws.security.openidconnect.server.internal.HashUtils;
import com.ibm.ws.security.openidconnect.token.IDTokenValidationFailedException;
import com.ibm.ws.security.openidconnect.token.JWT;
import com.ibm.ws.security.openidconnect.token.JWTPayload;
import com.ibm.ws.security.openidconnect.token.JsonTokenUtil;
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
     * @param oauth20Provider extracted from the request
     * @param oidcServerConfig is the object of oidc server configuration object
     * @param request is the incoming HttpServletRequest
     * @param response WAS OIDC response for a given provider
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
        Principal user = request.getUserPrincipal();
        String idTokenString = request.getParameter(OIDCConstants.OIDC_LOGOUT_ID_TOKEN_HINT);
        String redirectUri = request.getParameter(OIDCConstants.OIDC_LOGOUT_REDIRECT_URI);
        String clientId = request.getParameter(OIDCConstants.OIDC_LOGOUT_CLIENT_ID);
        OAuth20Token cachedIdToken = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "id_token_hint : " + idTokenString + " post_logout_redirect_uri : " + redirectUri + " client_id : " + clientId);
        }
        if (idTokenString != null && idTokenString.length() == 0) {
            idTokenString = null;
        }
        boolean continueLogoff = true;

        // lookup idtoken cache first.
        OAuth20TokenCache tokenCache = null;
        if (idTokenString != null) {
            tokenCache = oauth20Provider.getTokenCache();
            if (tokenCache != null) {
                String hash = HashUtils.digest(idTokenString);
                if (hash != null) {
                    cachedIdToken = tokenCache.get(hash);
                    // if idToken is found, this is valid.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "idToken : " + cachedIdToken);
                    }
                } else {
                    Tr.error(tc, "OIDC_SERVER_IDTOKEN_VERIFY_ERR", new Object[] { "IDTokenValidatonFailedException" });
                    continueLogoff = false;
                }
            }
        }

        String userName = ((user == null) ? null : user.getName());
        String tokenUsername = ((cachedIdToken == null) ? null : cachedIdToken.getUsername());
        clientId = ((cachedIdToken == null) ? clientId : cachedIdToken.getClientId());

        if (idTokenString != null && cachedIdToken == null && continueLogoff) {
            // if it's not there parse the idTokenString and validate signature.
            JWT jwt = null;
            try {
                jwt = endpointServices.createJwt(idTokenString, oauth20Provider, oidcServerConfig);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "JWT : " + jwt);
                }
                //if (jwt.verify()) {
                if (jwt.verifySignatureOnly()) {
                    tokenUsername = JsonTokenUtil.getSub(jwt.getPayload());
                    clientId = JsonTokenUtil.getAud(jwt.getPayload());
                } else {
                    Tr.error(tc, "OIDC_SERVER_IDTOKEN_VERIFY_ERR", new Object[] { "IDTokenValidatonFailedException" });
                    continueLogoff = false;
                }
            } catch (IDTokenValidationFailedException ivfe) {
                Throwable cause = ivfe.getCause();
                if (cause != null && cause instanceof IllegalStateException) {
                    // this error can be ignored, since this is due to exp, iat expiration.
                    // extract sub.
                    try {
                        JWTPayload payload = JsonTokenUtil.getPayload(idTokenString);
                        if (payload != null) {
                            tokenUsername = JsonTokenUtil.getSub(payload);
                            clientId = JsonTokenUtil.getAud(payload);
                        }
                    } catch (Exception e) {
                        Tr.error(tc, "OIDC_SERVER_IDTOKEN_VERIFY_ERR", new Object[] { e });
                        continueLogoff = false;
                    }
                } else {
                    Tr.error(tc, "OIDC_SERVER_IDTOKEN_VERIFY_ERR", new Object[] { ivfe });
                    continueLogoff = false;
                }
            } catch (Exception e) {
                Tr.error(tc, "OIDC_SERVER_IDTOKEN_VERIFY_ERR", new Object[] { e });
                continueLogoff = false;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "login username : " + userName + " IDToken username : " + tokenUsername);
        }

        if (userName != null && tokenUsername != null && !userName.equals(tokenUsername)) {
            // user mismatch, abort
            Tr.error(tc, "OIDC_SERVER_USERNAME_MISMATCH_ERR", new Object[] { userName, tokenUsername });
            continueLogoff = false;
        }

        if (continueLogoff) {
            if (cachedIdToken != null && tokenCache != null) {
                // delete refreshtoken.
                CacheUtil cu = new CacheUtil(tokenCache);
                OAuth20Token refreshToken = cu.getRefreshToken(cachedIdToken);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "refreshToken : " + refreshToken);
                }
                if (refreshToken != null) {
                    tokenCache.remove(refreshToken.getTokenString());
                }
            }
            //@AV999-092821
//            if (user != null) {
//                // logout deletes ltpatoken cookie and oidc_bsc cookie.
//                request.logout();
//            }
        }

        if (!continueLogoff) {
            // this is an error condition. display an error page.
            redirectUri = request.getContextPath() + "/end_session_error.html";
        } else {
            if (redirectUri == null) {
                // no redirectUri is set, use default.
                redirectUri = request.getContextPath() + "/end_session_logout.html";
            } else {
                try {
                    String[] uris = getPostLogoutRedirectUris(oauth20Provider, clientId);
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
            }
        }
        if (oauth20Provider.isTrackOAuthClients()) {
            redirectUri = updateRedirectUriWithTrackedOAuthClients(request, response, oauth20Provider, redirectUri);
        }
        //@AV999-092821
        request.setAttribute("OIDC_END_SESSION_REDIRECT", redirectUri);
        if (continueLogoff) {
            if (user != null) {
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
                sendBackchannelLogoutRequests(request, oidcServerConfig, userName, idTokenString);
            }
        }
        if (request.getAttribute("OIDC_END_SESSION_REDIRECT") != null) {
            request.removeAttribute("OIDC_END_SESSION_REDIRECT");
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "OIDC _SSO OP redirecting to [" + redirectUri + "]");
            }
            response.sendRedirect(redirectUri);
        }
    }

    /**
     * get PostLogoutRedirectUris
     *
     * @param oauth20Provider extracted from the request
     * @param clientId
     * @throws OidcServerException
     *
     */
    String[] getPostLogoutRedirectUris(OAuth20Provider oauth20Provider, String clientId) throws OidcServerException {
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
     * get check whether the given string contains in the given JsonArray.
     *
     * @param uri String.
     * @param uris String[]
     *
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

    String updateRedirectUriWithTrackedOAuthClients(HttpServletRequest request, HttpServletResponse response, OAuth20Provider provider, String redirectUri) {
        OAuthClientTracker clientTracker = new OAuthClientTracker(request, response, provider);
        return clientTracker.updateLogoutUrlAndDeleteCookie(redirectUri);
    }

    void sendBackchannelLogoutRequests(HttpServletRequest request, OidcServerConfig oidcServerConfig, String userName, String idTokenString) {
        BackchannelLogoutRequestHelper bclRequestCreator = new BackchannelLogoutRequestHelper(request, oidcServerConfig);
        bclRequestCreator.sendBackchannelLogoutRequests(userName, idTokenString);
    }

}