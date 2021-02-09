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
package com.ibm.ws.security.openidconnect.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.json.java.JSONObject;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20BadParameterFormatException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.api.oauth20.token.OAuth20TokenCache;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.oauth20.AuthnContext;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.claims.UserClaims;
import com.ibm.ws.security.oauth20.ProvidersService;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20Client;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.internal.AuthnContextImpl;
import com.ibm.ws.security.oauth20.plugins.BaseClient;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.plugins.jose4j.OidcUserClaims;
import com.ibm.ws.security.oauth20.util.CacheUtil;
import com.ibm.ws.security.oauth20.util.ConfigUtils;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;
import com.ibm.ws.security.oauth20.web.EndpointUtils;
import com.ibm.ws.security.oauth20.web.OAuth20EndpointServices;
import com.ibm.ws.security.oauth20.web.OAuth20Request.EndpointType;
import com.ibm.ws.security.oauth20.web.OAuthClientTracker;
import com.ibm.ws.security.oauth20.web.WebUtils;
import com.ibm.ws.security.openidconnect.server.internal.HashUtils;
import com.ibm.ws.security.openidconnect.server.internal.HttpUtils;
import com.ibm.ws.security.openidconnect.token.IDTokenValidationFailedException;
import com.ibm.ws.security.openidconnect.token.JWT;
import com.ibm.ws.security.openidconnect.token.JWTPayload;
import com.ibm.ws.security.openidconnect.token.JsonTokenUtil;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.openidconnect.IDTokenMediator;
import com.ibm.wsspi.security.openidconnect.UserinfoProvider;

@Component(service = { OidcEndpointServices.class }, name = "com.ibm.ws.security.openidconnect.web.OidcEndpointServices", immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.vendor=IBM")
public class OidcEndpointServices extends OAuth20EndpointServices {

    private static TraceComponent tc = Tr.register(OidcEndpointServices.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);

    public static final String KEY_OAUTH20_ENDPOINT_SERVICES = "oauth20EndpointServices";
    public static final String KEY_ID = "id";
    public static final String KEY_SERVICE_PID = "service.pid";
    public static final String KEY_OIDC_SERVER_CONFIG = "oidcServerConfig";
    private final ConcurrentServiceReferenceMap<String, OidcServerConfig> oidcServerConfigRef = new ConcurrentServiceReferenceMap<String, OidcServerConfig>(KEY_OIDC_SERVER_CONFIG);
    private boolean bOidcUpdated = false;
    private HashMap<String, OidcServerConfig> oidcMap = new HashMap<String, OidcServerConfig>();

    public static final String KEY_USER_INFO_PROVIDER = "userinfoProvider";
    private final ConcurrentServiceReferenceMap<String, UserinfoProvider> userinfoProviderConfigRef = new ConcurrentServiceReferenceMap<String, UserinfoProvider>(KEY_USER_INFO_PROVIDER);

    public static final String KEY_IDTOKEN_MEDIATOR = "idTokenMediator";
    private final ConcurrentServiceReferenceMap<String, IDTokenMediator> idTokenMediatorRef = new ConcurrentServiceReferenceMap<String, IDTokenMediator>(KEY_IDTOKEN_MEDIATOR);

    protected final AtomicServiceReference<OAuth20EndpointServices> oauth20EndpointServicesRef = new AtomicServiceReference<OAuth20EndpointServices>(KEY_OAUTH20_ENDPOINT_SERVICES);
    private volatile BrowserState browserState = null;
    private volatile Discovery discovery = null;
    private volatile OidcOptionalParams optionalParameters = null;
    private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
    ConfigUtils configUtils = new ConfigUtils();

    @Reference(service = OAuth20EndpointServices.class, name = KEY_OAUTH20_ENDPOINT_SERVICES, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setOAuth20EndpointServices(ServiceReference<OAuth20EndpointServices> reference) {
        oauth20EndpointServicesRef.setReference(reference);
    }

    protected void unsetOAuth20EndpointServices(ServiceReference<OAuth20EndpointServices> reference) {
        oauth20EndpointServicesRef.unsetReference(reference);
    }

    @Reference(service = UserinfoProvider.class, name = KEY_USER_INFO_PROVIDER, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)
    protected void setUserinfoProvider(ServiceReference<UserinfoProvider> ref) {
        synchronized (userinfoProviderConfigRef) {
            userinfoProviderConfigRef.putReference((String) ref.getProperty(KEY_SERVICE_PID), ref);
        }
    }

    protected void unsetUserinfoProvider(ServiceReference<UserinfoProvider> ref) {
        synchronized (userinfoProviderConfigRef) {
            userinfoProviderConfigRef.removeReference((String) ref.getProperty(KEY_SERVICE_PID), ref);
        }
    }

    @Reference(service = IDTokenMediator.class, name = KEY_IDTOKEN_MEDIATOR, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    protected void setIdTokenMediator(ServiceReference<IDTokenMediator> ref) {
        synchronized (idTokenMediatorRef) {
            idTokenMediatorRef.putReference((String) ref.getProperty(KEY_SERVICE_PID), ref);
        }
    }

    protected void unsetIdTokenMediator(ServiceReference<IDTokenMediator> ref) {
        synchronized (idTokenMediatorRef) {
            idTokenMediatorRef.removeReference((String) ref.getProperty(KEY_SERVICE_PID), ref);
        }
    }

    @Reference(service = OidcServerConfig.class, name = KEY_OIDC_SERVER_CONFIG, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)
    protected void setOidcServerConfig(ServiceReference<OidcServerConfig> ref) {
        synchronized (oidcServerConfigRef) {
            oidcServerConfigRef.putReference((String) ref.getProperty(KEY_ID), ref);
            bOidcUpdated = true;
        }
    }

    protected void unsetOidcServerConfig(ServiceReference<OidcServerConfig> ref) {
        synchronized (oidcServerConfigRef) {
            oidcServerConfigRef.removeReference((String) ref.getProperty(KEY_ID), ref);
            bOidcUpdated = true;
        }
    }

    @Override
    @Activate
    protected void activate(ComponentContext cc) {
        securityServiceRef.activate(cc);
        oauth20EndpointServicesRef.activate(cc);
        synchronized (oidcServerConfigRef) {
            oidcServerConfigRef.activate(cc);
            bOidcUpdated = true;
        }
        userinfoProviderConfigRef.activate(cc);
        idTokenMediatorRef.activate(cc);
        ConfigUtils.setIdTokenMediatorService(idTokenMediatorRef);
        browserState = new BrowserState();
        discovery = new Discovery();
        optionalParameters = new OidcOptionalParams();
        Tr.info(tc, "OIDC_ENDPOINT_SERVICE_ACTIVATED");
    }

    @Override
    @Deactivate
    protected void deactivate(ComponentContext cc) {
        securityServiceRef.deactivate(cc);
        oauth20EndpointServicesRef.deactivate(cc);
        synchronized (oidcServerConfigRef) {
            oidcServerConfigRef.deactivate(cc);
            bOidcUpdated = true;
        }
        userinfoProviderConfigRef.deactivate(cc);
        idTokenMediatorRef.deactivate(cc);
    }

    @Trivial
    protected void logExitMsgFinished() {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "OIDC _SSO OP PROCESS HAS ENDED.");
        }
    }

    @Trivial
    protected void logExitMsgNo() {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "OIDC _SSO OP WILL NOT PROCESS THE REQUEST");
        }
    }

    protected void handleOidcRequest(HttpServletRequest request,
                                     HttpServletResponse response,
                                     ServletContext servletContext) throws ServletException, IOException {

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Checking if OIDC Provider should process the request.");
            Tr.debug(tc, "Inbound request " + com.ibm.ws.security.common.web.WebUtils.getRequestStringForTrace(request, "client_secret"));
        }
        OidcRequest oidcRequest = getOidcRequest(request, response);
        if (oidcRequest == null) {
            logExitMsgNo();
            return;
        }
        String oidcProviderName = oidcRequest.getProviderName();
        OidcServerConfig oidcServerConfig = getOidcServerConfig(response, oidcProviderName);
        if (oidcServerConfig == null) {
            logExitMsgNo();
            return;
        }
        OAuth20Provider oauth20provider = getOAuthProvider(response, oidcServerConfig);
        if (oauth20provider == null) {
            logExitMsgNo();
            return;
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "OIDC _SSO OP PROCESS IS STARTING.");
            Tr.debug(tc, "OIDC _SSO OP inbound URL " + com.ibm.ws.security.common.web.WebUtils.getRequestStringForTrace(request, "client_secret"));
        }
        EndpointType endpointType = oidcRequest.getType();

        AttributeList optionalParams = null;
        try {
            optionalParams = optionalParameters.getParameters(request);
        } catch (OAuth20BadParameterFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        if (endpointType == EndpointType.authorize) {
            String externalClaimNames = oidcServerConfig.getExternalClaimNames();
            if (externalClaimNames != null && externalClaimNames.length() > 0) {
                optionalParams.setAttribute(OAuth20Constants.EXTERNAL_CLAIM_NAMES,
                                            OAuth20Constants.EXTERNAL_CLAIM_NAMES,
                                            new String[] { externalClaimNames });
            }
            handleIdTokenHint(oauth20provider, oidcServerConfig, optionalParams);
            if (oidcServerConfig.isSessionManaged()) {
                browserState.processSession(request, response);
                browserState.generateState(request, optionalParams);
            }
        }
        handleEndpointRequest(request, response, servletContext, oauth20provider, endpointType, optionalParams);
        if (response.isCommitted()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Response has already been committed, will not continue processing the request");
            }
            logExitMsgFinished();
            return;
        }

        switch (endpointType) {
            case discovery:
                discovery.processRequest(oidcServerConfig, request, response);
                logExitMsgFinished();
                return;
            case userinfo:
                userinfo(oauth20provider, oidcServerConfig, request, response);
                logExitMsgFinished();
                return;
            case end_session:
                processEndSession(oauth20provider, oidcServerConfig, request, response);
                logExitMsgFinished();
                return;
            case check_session_iframe:
                processCheckSessionRequest(response, oidcServerConfig);
                logExitMsgFinished();
                return;
            case jwk:
                processJWKRequest(response, oidcServerConfig);
                logExitMsgFinished();
                return;
            default:
                break;
        }
        logExitMsgFinished();
    }

    private OidcRequest getOidcRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        OidcRequest oidcRequest = (OidcRequest) request.getAttribute(OAuth20Constants.OIDC_REQUEST_OBJECT_ATTR_NAME);
        if (oidcRequest == null) {
            String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                           TraceConstants.MESSAGE_BUNDLE,
                                                           "OIDC_REQUEST_ATTRIBUTE_MISSING",
                                                           new Object[] { request.getRequestURI(), OAuth20Constants.OIDC_REQUEST_OBJECT_ATTR_NAME },
                                                           "CWWKS1634E: The request endpoint {0} does not have attribute {1}.");
            Tr.error(tc, errorMsg);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, errorMsg);
        }
        return oidcRequest;
    }

    private OAuth20Provider getOAuthProvider(HttpServletResponse response, OidcServerConfig oidcServerConfig) throws IOException {
        String oidcProviderName = oidcServerConfig.getProviderId();
        String oauthProviderName = oidcServerConfig.getOauthProviderName();
        if (oauthProviderName == null) {
            String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                           TraceConstants.MESSAGE_BUNDLE,
                                                           "OIDC_OAUTH_PROVIDER_NAME_NOT_FOUND",
                                                           new Object[] { oidcProviderName },
                                                           "CWWKS1632E: The OAuth provider name referenced by the OpenID Connect provider {0} was not found.");
            Tr.error(tc, errorMsg);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, errorMsg);
            return null;
        }

        OAuth20Provider oauth20provider = ProvidersService.getOAuth20Provider(oauthProviderName);
        if (oauth20provider == null) {
            String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                           TraceConstants.MESSAGE_BUNDLE,
                                                           "OIDC_OAUTH_PROVIDER_OBJECT_NULL",
                                                           new Object[] { oidcProviderName },
                                                           "CWWKS1630E: OAuth20Provider object is null for the OpenID Connect provider {0}");

            Tr.error(tc, errorMsg);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, errorMsg);
        }
        return oauth20provider;
    }

    /**
     * @param oidcProviderName
     * @return
     */
    public OidcServerConfig getOidcServerConfig(HttpServletResponse response, String oidcProviderName) throws IOException {
        return getOidcServerConfig(response, oidcProviderName, true);
    }

    public OidcServerConfig getOidcServerConfig(HttpServletResponse response, String oidcProviderName, boolean sendErrorIfProviderNotFound) throws IOException {
        synchronized (oidcServerConfigRef) {
            if (bOidcUpdated) {
                oidcMap = configUtils.checkDuplicateOAuthProvider(oidcServerConfigRef);
                bOidcUpdated = false;
            }
        }
        OidcServerConfig oidcServerConfig = oidcMap.get(oidcProviderName);
        if (oidcServerConfig == null && sendErrorIfProviderNotFound) {
            Tr.error(tc, "OIDC_SERVER_CONFIG_SERVICE_NOT_AVAILABLE", oidcProviderName);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "OpenID Connect configuration service is not avaliable for OpenID Connect provider name " + oidcProviderName);
        }
        return oidcServerConfig;
    }

    /**
     * process end session task which includes:
     * - delete LTPAToken cookie.
     * - delete refresh token from tokencache if id_token_hint is present.
     * - redirect a request to a URL which is specified by post_logout_redirect_uri
     *
     * @param oauth20provider  extracted from the request
     * @param oidcServerConfig is the object of oidc server configuration object
     * @param request          is the incoming HttpServletRequest
     * @param response         WAS OIDC response for a given provider
     *
     * @throws IOException
     */
    @FFDCIgnore(IDTokenValidationFailedException.class)
    protected void processEndSession(OAuth20Provider oauth20provider, OidcServerConfig oidcServerConfig, HttpServletRequest request,
                                     HttpServletResponse response) throws ServletException, IOException {
        Principal user = request.getUserPrincipal();
        String idTokenString = request.getParameter(OIDCConstants.OIDC_LOGOUT_ID_TOKEN_HINT);
        String redirectUri = request.getParameter(OIDCConstants.OIDC_LOGOUT_REDIRECT_URI);
        OAuth20Token cachedIdToken = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "id_token_hint : " + idTokenString + " post_logout_redirect_uri : " + redirectUri);
        }
        if (idTokenString != null && idTokenString.length() == 0) {
            idTokenString = null;
        }
        boolean continueLogoff = true;

        // lookup idtoken cache first.
        OAuth20TokenCache tokenCache = null;
        if (idTokenString != null) {
            tokenCache = oauth20provider.getTokenCache();
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
        String clientId = ((cachedIdToken == null) ? null : cachedIdToken.getClientId());

        if (idTokenString != null && cachedIdToken == null && continueLogoff) {
            // if it's not there parse the idTokenString and validate signature.
            JWT jwt = null;
            try {
                jwt = createJwt(idTokenString, oauth20provider, oidcServerConfig);
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
            if (user != null) {
                // logout deletes ltpatoken cookie and oidc_bsc cookie.
                request.logout();
            }
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
                    String[] uris = getPostLogoutRedirectUris(oauth20provider, clientId);
                    if (!containUri(redirectUri, uris)) {
                        // post_logout_redirect_uri is not a member of post_logout_redirect_uris, force to redirect to the default logout page.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            if (clientId == null) {
                                Tr.debug(tc,
                                         "postLogoutRedirectUri value cannot be identified because client id is not set. Most likely this is because the id_token_hint parameter is not set or invalid.");
                            }
                        }
                        Tr.error(tc, "OIDC_SERVER_LOGOUT_REDIRECT_URI_MISMATCH", new Object[] { redirectUri, printArray(uris), clientId });
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
        if (oauth20provider.isTrackOAuthClients()) {
            redirectUri = updateRedirectUriWithTrackedOAuthClients(request, response, oauth20provider, redirectUri);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "OIDC _SSO OP redirecting to [" + redirectUri + "]");
        }
        response.sendRedirect(redirectUri);
    }

    String updateRedirectUriWithTrackedOAuthClients(HttpServletRequest request, HttpServletResponse response, OAuth20Provider provider, String redirectUri) {
        OAuthClientTracker clientTracker = new OAuthClientTracker(request, response, provider);
        return clientTracker.updateLogoutUrlAndDeleteCookie(redirectUri);
    }

    /**
     * Process a request for the check_session_iframe endpoint by redirecting
     * the response to the appropriate session management iframe page.
     *
     * @param response
     * @throws IOException
     */
    private void processCheckSessionRequest(HttpServletResponse response, OidcServerConfig oidcServerConfig) throws IOException {
        String methodName = "processCheckSessionRequest";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName);
        }

        String iframeUrl = oidcServerConfig.getCheckSessionIframeEndpointUrl();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "OIDC _SSO OP redirecting to [" + iframeUrl + "]");
        }
        response.sendRedirect(iframeUrl);

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName);
        }
    }

    /**
     * construct JWT which is a super class of IDToken from IdTokenString.
     * JWT class is used in order to just perform signature validation.
     *
     * @param oauth20provider  extracted from the request
     * @param oidcServerConfig is the object of oidc server configurations
     * @throws OidcServerException
     *
     */
    JWT createJwt(String tokenString, OAuth20Provider oauth20provider, OidcServerConfig oidcServerConfig) throws OidcServerException {
        String aud = null;
        String issuer = null;
        JWTPayload payload = JsonTokenUtil.getPayload(tokenString);
        if (payload != null) {
            aud = JsonTokenUtil.getAud(payload);
            issuer = JsonTokenUtil.getIss(payload);
        }
        // TODO support RS256 by resolving key issue.
        Object key = ((aud == null) ? null : getSharedKey(oauth20provider, aud));
        // signatureAlgorithm
        String signatureAlgorithm = oidcServerConfig.getSignatureAlgorithm();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "clientId : " + aud + " key : " + ((key == null) ? "null" : "<removed>") + " issuer : " + issuer + " signatureAlgorithm : " + signatureAlgorithm);
        }

        return new JWT(tokenString, key, aud, issuer, signatureAlgorithm);
    }

    /**
     * get Shared key
     *
     * @param oauth20provider extracted from the request
     * @param clientId
     * @throws OidcServerException
     *
     */
    @Sensitive
    Object getSharedKey(OAuth20Provider oauth20provider, String clientId) throws OidcServerException {
        String sharedKey = null;
        OidcOAuth20ClientProvider clientProvider = oauth20provider.getClientProvider();
        OidcOAuth20Client oauth20Client = clientProvider.get(clientId);
        if (oauth20Client instanceof BaseClient) {
            BaseClient baseClient = (BaseClient) oauth20Client;
            sharedKey = baseClient.getClientSecret();
        }
        return sharedKey;
    }

    /**
     * get PostLogoutRedirectUris
     *
     * @param oauth20provider extracted from the request
     * @param clientId
     * @throws OidcServerException
     *
     */
    String[] getPostLogoutRedirectUris(OAuth20Provider oauth20provider, String clientId) throws OidcServerException {
        String[] uris = null;
        if (clientId != null) {
            OidcOAuth20ClientProvider clientProvider = oauth20provider.getClientProvider();
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
     * @param uri  String.
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

    /**
     * Return a JSON object with the claims for the scopes in the provided access token.
     *
     * If the access token is valid return status 200 and a JSON object
     * containing the claims associated with the scopes in the access token.
     *
     * If the token is not valid or the request had other errors return status and error
     * info per IETF RFC 6750: OAuth 2.0 Authorization Framework: Bearer Token Usage
     *
     * @param oauth20provider
     * @param oidcServerConfig
     * @param request
     * @param response
     * @throws IOException
     */
    void userinfo(OAuth20Provider oauth20provider,
                  OidcServerConfig oidcServerConfig,
                  HttpServletRequest request,
                  HttpServletResponse response) throws IOException {

//        // Must have VMM
//        VMMService vmmService = ConfigUtils.getVMMService();
//        if (vmmService == null) {
//            String errorMsg =
//                            TraceNLS.getFormattedMessage(this.getClass(),
//                                                         TraceConstants.MESSAGE_BUNDLE,
//                                                         "OIDC_SERVER_USERINFO_INTERNAL_ERROR_NO_VMM",
//                                                         new Object[] {request.getRequestURI()},
//                                                         "CWWKS1627E: An internal server error occurred while processing a userinfo request. The federated repository service was not available. The request URI was {0}.");
//            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMsg);
//            Tr.error(tc, "OIDC_SERVER_USERINFO_INTERNAL_ERROR_NO_VMM", new Object[] { request.getRequestURI() });
//            return;
//        }
        Enumeration<String> params = request.getParameterNames();
        if (params != null) {
            while (params.hasMoreElements()) {
                String param = params.nextElement();
                if (!param.equals(com.ibm.ws.security.oauth20.util.UtilConstants.ACCESS_TOKEN)) {
                    String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                                   TraceConstants.MESSAGE_BUNDLE,
                                                                   "OIDC_SERVER_USERINFO_UNSUPPORTED_PARAMETER",
                                                                   new Object[] { param, request.getRequestURI() },
                                                                   "CWWKS1633E: A userinfo request was made with unsupported parameter {0}. The request URI was {1}.");
                    setWWWAuthenticateHeaderResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                                                     Constants.ERROR_CODE_INVALID_REQUEST, errorMsg, null);
                    Tr.error(tc, "OIDC_SERVER_USERINFO_UNSUPPORTED_PARAMETER", new Object[] { param, request.getRequestURI() });
                    return;
                }
            }
        }
        String accessTokenString = request.getParameter(com.ibm.ws.security.oauth20.util.UtilConstants.ACCESS_TOKEN);
        String reqParamAccessTokenString = accessTokenString;
        String authzHeader = request.getHeader(com.ibm.ws.security.oauth20.util.UtilConstants.AUTHORIZATION_HEADER_NAME);
        if (authzHeader != null) {
            String hdrAccessTokenString = com.ibm.oauth.core.util.WebUtils.getBearerTokenFromAuthzHeader(authzHeader);
            if (accessTokenString != null && hdrAccessTokenString != null) {
                String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                               TraceConstants.MESSAGE_BUNDLE,
                                                               "OIDC_SERVER_USERINFO_MULTIPLE_ACCESS_TOKENS",
                                                               new Object[] { request.getRequestURI() },
                                                               "CWWKS1621E: A userinfo request was made with an access token in the access_token request parameter and also the authorization header. Only one access token is allowed. The request URI was {0}.");
                setWWWAuthenticateHeaderResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                                                 Constants.ERROR_CODE_INVALID_REQUEST, errorMsg, null);
                Tr.error(tc, "OIDC_SERVER_USERINFO_MULTIPLE_ACCESS_TOKENS", new Object[] { request.getRequestURI() });
                return;
            }
            if (accessTokenString == null) {
                accessTokenString = hdrAccessTokenString;
            }
        }
        //  if no token
        //     return 400 error=invalid_request,error_description=no access token
        if (accessTokenString == null) {
            String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                           TraceConstants.MESSAGE_BUNDLE,
                                                           "OIDC_SERVER_USERINFO_NO_ACCESS_TOKEN",
                                                           new Object[] { request.getRequestURI() },
                                                           "CWWKS1616E: A userinfo request was made with no access token. The request URI was {0}.");
            setWWWAuthenticateHeaderResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                                             Constants.ERROR_CODE_INVALID_REQUEST, errorMsg, null);
            Tr.error(tc, "OIDC_SERVER_USERINFO_NO_ACCESS_TOKEN", new Object[] { request.getRequestURI() });
            return;
        }

        //  if not found in cache
        //     return 401 "invalid_token"
        // Check whether the token is opaque or jwt type
        // change the lookup based on the token type
        String tokenLookupStr = accessTokenString;
        boolean isAppPasswordOrToken = false;
        if (OidcOAuth20Util.isJwtToken(accessTokenString)) {
            tokenLookupStr = HashUtils.digest(accessTokenString);
        } else if (tokenLookupStr.length() == (oauth20provider.getAccessTokenLength() + 2)) {
            // app-token or app-password
            String encode = oauth20provider.getAccessTokenEncoding();
            if (OAuth20Constants.PLAIN_ENCODING.equals(encode)) { // must be app-password or app-token
                tokenLookupStr = EndpointUtils.computeTokenHash(accessTokenString);
            } else {
                tokenLookupStr = EndpointUtils.computeTokenHash(accessTokenString, encode);
            }
            isAppPasswordOrToken = true;
        }
        OAuth20Token accessToken = null;//oauth20provider.getTokenCache().get(tokenLookupStr);
        if (isAppPasswordOrToken) {
            accessToken = oauth20provider.getTokenCache().getByHash(tokenLookupStr);
        } else {
            accessToken = oauth20provider.getTokenCache().get(tokenLookupStr);
        }

        boolean isAppPassword = false;
        if (accessToken != null) {
            isAppPassword = accessToken.getGrantType() != null && accessToken.getGrantType().equals(OAuth20Constants.APP_PASSWORD);
        }
        if ((accessToken == null) || (!(oauth20provider.isLocalStoreUsed()) && isAppPassword)) {
            String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                           TraceConstants.MESSAGE_BUNDLE,
                                                           "OIDC_SERVER_USERINFO_BAD_ACCESS_TOKEN",
                                                           new Object[] { request.getRequestURI() },
                                                           "CWWKS1617E: A userinfo request was made with an access token that was not recognized. The request URI was {0}.");
            setWWWAuthenticateHeaderResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                                             Constants.ERROR_CODE_INVALID_TOKEN, errorMsg, null);
            Tr.error(tc, "OIDC_SERVER_USERINFO_BAD_ACCESS_TOKEN", new Object[] { request.getRequestURI() });
            return;
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "token type: " + accessToken.getType() + " grant type: " + accessToken.getGrantType());
        }

        //  if not access token or is app_password,  return 401 "invalid_token"
        boolean isAccessToken = accessToken.getType().equals(OAuth20Constants.TOKENTYPE_ACCESS_TOKEN);
        //boolean isAppPassword = accessToken.getGrantType() != null && accessToken.getGrantType().equals(OAuth20Constants.APP_PASSWORD);
        if (isAppPassword || !isAccessToken) {
            String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                           TraceConstants.MESSAGE_BUNDLE,
                                                           "OIDC_SERVER_USERINFO_BAD_TOKEN_TYPE",
                                                           new Object[] { request.getRequestURI() },
                                                           "CWWKS1622E: A userinfo request was made with a token that was not an access token. The request URI was {0}.");
            setWWWAuthenticateHeaderResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                                             Constants.ERROR_CODE_INVALID_TOKEN, errorMsg, null);
            Tr.error(tc, "OIDC_SERVER_USERINFO_BAD_TOKEN_TYPE", new Object[] { request.getRequestURI() });
            return;
        }
        //  if expired
        //     return 401 "invalid_token"
        if (OAuth20TokenHelper.isTokenExpired(accessToken)) {
            String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                           TraceConstants.MESSAGE_BUNDLE,
                                                           "OIDC_SERVER_USERINFO_EXPIRED_ACCESS_TOKEN",
                                                           new Object[] { request.getRequestURI() },
                                                           "CWWKS1623E: A userinfo request was made with an expired access token. The request URI was {0}.");
            setWWWAuthenticateHeaderResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                                             Constants.ERROR_CODE_INVALID_TOKEN, errorMsg, null);
            Tr.error(tc, "OIDC_SERVER_USERINFO_EXPIRED_ACCESS_TOKEN", new Object[] { request.getRequestURI() });
            return;
        }
        if (oidcServerConfig == null) {
            String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                           TraceConstants.MESSAGE_BUNDLE,
                                                           "OIDC_SERVER_USERINFO_INVALID_REQUEST",
                                                           new Object[] { request.getRequestURI() },
                                                           "CWWKS1618E: A userinfo request URI was not valid. The request URI was {0}.");
            setWWWAuthenticateHeaderResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                                             Constants.ERROR_CODE_INVALID_REQUEST, errorMsg, null);
            Tr.error(tc, "OIDC_SERVER_USERINFO_INVALID_REQUEST", new Object[] { request.getRequestURI() });
//            Tr.error(tc, "OIDC_SERVER_USERINFO_PROVIDER_NOT_FOUND", new Object[] {oidcProviderName, request.getRequestURI() });
            return;
        }
        // get scopes and claims
        boolean requireOpenidScope = oidcServerConfig.isOpenidScopeRequiredForUserInfo();
        String[] tokenScopes = accessToken.getScope();
        Properties scopeToClaims = oidcServerConfig.getScopeToClaimMap();
        HashSet<String> claims = new HashSet<String>();
        boolean hasOpenidScope = false;
        for (String scope : tokenScopes) {
            if (scope.equals(OIDCConstants.SCOPE_OPENID)) {
                hasOpenidScope = true;
            }
            String[] scopeClaims = (String[]) scopeToClaims.get(scope);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "scope: " + scope + "  has claims: " + Arrays.toString(scopeClaims));
            }
            if (scopeClaims != null && scopeClaims.length > 0) {
                claims.addAll(Arrays.asList(scopeClaims));
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "claims: " + claims);
        }
        //  if no openid scope
        //     return 403 error=insufficient_scope, scope=openid
        if (!hasOpenidScope && requireOpenidScope) {
            String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                           TraceConstants.MESSAGE_BUNDLE,
                                                           "OIDC_SERVER_USERINFO_NOT_OIDC_ACCESS_TOKEN",
                                                           new Object[] { request.getRequestURI() },
                                                           "CWWKS1619E: A userinfo request was made with an access token that did not have the required 'openid' scope. The request URI was {0}.");
            setWWWAuthenticateHeaderResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                                             Constants.ERROR_CODE_INVALID_REQUEST, errorMsg, OIDCConstants.SCOPE_OPENID);
            Tr.error(tc, "OIDC_SERVER_USERINFO_NOT_OIDC_ACCESS_TOKEN", new Object[] { request.getRequestURI() });
            return;
        }

        // per IETF RFC 6750
        if (reqParamAccessTokenString != null) {
            response.setHeader(HttpUtils.CACHE_CONTROL, HttpUtils.PRIVATE);
        }

        if (!userinfoProviderConfigRef.isEmpty()) {
            UserinfoProvider userinfoProvider = null;
            Iterator<UserinfoProvider> it = userinfoProviderConfigRef.getServices();
            JSONObject customJSON = null;
            int providersInstalled = userinfoProviderConfigRef.size();

            while (it.hasNext()) {
                userinfoProvider = it.next();
                try {
                    customJSON = getUserinfoFromCustomProvider(accessToken, userinfoProvider, request, response);
                } catch (IOException ioe) {
                    // customJSON is an invalid JSONObject
                    String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                                   TraceConstants.MESSAGE_BUNDLE,
                                                                   "OIDC_SERVER_USERINFO_INVALID_JSONOBJECT",
                                                                   new Object[] { accessToken.getUsername(), userinfoProvider.getClass().getName() },
                                                                   "CWWKS1639E: The userinfo for {0} returned by Liberty user feature {1} is invalid.");
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMsg);
                    Tr.error(tc, "OIDC_SERVER_USERINFO_INVALID_JSONOBJECT", new Object[] { accessToken.getUsername(), userinfoProvider.getClass().getName() });
                    return;
                }

                if (customJSON != null) {
                    if (providersInstalled > 1) // if there is more than one UserinfoProvider configured, log a warning
                        Tr.info(tc, "OIDC_SERVER_MULTIPLE_USERINFO_PROVIDER_CONFIGURED");
                    break;
                }
            }

            if (customJSON == null) {
                //Log error and exit method
                String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                               TraceConstants.MESSAGE_BUNDLE,
                                                               "OIDC_SERVER_USERINFO_PROVIDER_INTERNAL_ERROR",
                                                               new Object[] { accessToken.getUsername(), userinfoProvider.getClass().getName() },
                                                               "CWWKS1637E: The userinfo for {0} returned by Liberty user feature {1} is null.");
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMsg);
                Tr.error(tc, "OIDC_SERVER_USERINFO_PROVIDER_INTERNAL_ERROR", new Object[] { accessToken.getUsername(), userinfoProvider.getClass().getName() });
                return;
            }

            WebUtils.setJSONResponse(response, HttpServletResponse.SC_OK, customJSON.toString());
        } else {
            JSONObject responseJSON = getUserinfoFromRegistry(oauth20provider, oidcServerConfig, request, response, accessToken, claims);
            WebUtils.setJSONResponse(response, HttpServletResponse.SC_OK, responseJSON);
        }
    }

    /**
     * Return the JSONObject that will be returned for the userinfo endpoint, this method invokes the userinfo provider SPI
     * that has being installed on liberty
     *
     * @param accessToken      the OAuth20Token used to get authentication context
     * @param userinfoProvider the implementation of userinfoProvider which is installed at runtime
     * @param request          the HTTPRequest for the userinfo endpoint
     * @param response         the response for the userinfo endpoint request.
     * @return The JsonObject for userinfo endpoint
     * @throws IOException
     */
    private JSONObject getUserinfoFromCustomProvider(OAuth20Token accessToken,
                                                     UserinfoProvider userinfoProvider,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response) throws IOException {
        AuthnContext authnContext = new AuthnContextImpl(request, response, accessToken.getTokenString(), accessToken.getScope(), accessToken.getCreatedAt(), accessToken.getLifetimeSeconds(), accessToken.getUsername(), accessToken.getExtensionProperties());
        String strJsonObject = userinfoProvider.getUserInfo(authnContext);
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getUserInfo:'" + strJsonObject + "'");
        if (strJsonObject != null) {
            return JSONObject.parse(strJsonObject);
        } else {
            return null;
        }

    }

    private String getCalculatedIssuerId(HttpServletRequest request) {
        String hostname = request.getServerName();
        String scheme = request.getScheme();
        int port = request.getLocalPort();
        String path = request.getRequestURI();
        int lastSlashIndex = path.lastIndexOf("/");
        String issuerIdentifier = scheme + "://" + hostname + ":" + port + path.substring(0, lastSlashIndex);

        return issuerIdentifier;
    }

    /**
     * Get the JSONObject that will be returned for userinfo endpoint from the user registry
     *
     * @param oauth20provider  The OAuth20Provider
     * @param oidcServerConfig The OidcServerConfig
     * @param request          The HttpServletRequest
     * @param response         The HttpServletResponse
     * @param accessToken      The OAuth20Token
     * @param claims           The claims for this granted access
     * @param response
     * @throws IOException
     *
     */
    protected JSONObject getUserinfoFromRegistry(OAuth20Provider oauth20provider,
                                                 OidcServerConfig oidcServerConfig,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response,
                                                 OAuth20Token accessToken,
                                                 HashSet<String> claims) throws IOException {
        JSONObject responseJSON = new JSONObject();
        // Always return sub claim
        String userName = accessToken.getUsername();
        responseJSON.put(Constants.USERINFO_CLAIM_SUB, userName);
        String issuer = oidcServerConfig.getIssuerIdentifier();
        if (issuer == null) {
            issuer = getCalculatedIssuerId(request);
        }
        responseJSON.put(Constants.INTROSPECT_CLAIM_ISS, issuer);
        // Get the groups from the user claims (the same ones that go in the idtoken)
        UserClaims oauthClaims = getUserClaimsObj(oauth20provider, accessToken);
        Map<String, Object> userClaimsMap = getUserClaimsMap(oauthClaims, true);
        if (userClaimsMap != null) {
            responseJSON.putAll(userClaimsMap);
        }
        if (oauthClaims != null && oauthClaims.isEnabled()) { // userName == null?
            OidcUserClaims oidcClaims = new OidcUserClaims(oauthClaims);
            responseJSON = oidcClaims.getUserinfoFromRegistry(oidcServerConfig, responseJSON, request, response, claims);
        }

        return responseJSON;
    }

    /**
     * Set the WWW-Authenticate header in the response, using the given
     * error, error description, status and scope.
     *
     * @param response
     * @param status
     * @param error
     * @param errorDescription
     * @param scope
     */
    private void setWWWAuthenticateHeaderResponse(HttpServletResponse response,
                                                  int status,
                                                  String error,
                                                  String errorDescription,
                                                  String scope) {
        String header = "Bearer error=" + error + ",";
        header += " error_description=" + errorDescription;
        if (scope != null) {
            header += ", scope=" + scope;
        }
        response.setHeader(WWW_AUTHENTICATE_HEADER, header);
        response.setStatus(status);
    }

    /**
     * Convert the String array to a String.
     *
     * @param value
     * @return
     */
    @Trivial
    private String printArray(String[] value) {
        String result = null;
        if (value != null && value.length > 0) {
            StringBuffer buf = null;
            for (int i = 0; i < value.length; i++) {
                if (buf == null) {
                    buf = new StringBuffer("[ ");
                } else {
                    buf.append(", ");
                }
                buf.append(value[i]);
            }
            buf.append(" ]");
            result = buf.toString();
        }
        return result;
    }

    @FFDCIgnore({ IDTokenValidationFailedException.class, IllegalStateException.class })
    protected void handleIdTokenHint(OAuth20Provider oauth20provider, OidcServerConfig oidcServerConfig, AttributeList attrs) {
        String idTokenHint = attrs.getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT);
        if (idTokenHint != null) {
            OAuth20Token cachedIdToken = null;
            // lookup idtoken cache first.
            OAuth20TokenCache tokenCache = null;
            tokenCache = oauth20provider.getTokenCache();
            if (tokenCache != null) {
                String hash = HashUtils.digest(idTokenHint);
                if (hash != null) {
                    cachedIdToken = tokenCache.get(hash);
                    // if idToken is found, this is valid.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "idToken : " + cachedIdToken);
                    }
                } else {
                    // this should not happen.
                    Tr.error(tc, "OIDC_SERVER_IDTOKEN_VERIFY_ERR", new Object[] { "IDTokenValidatonFailedException" });
                    attrs.setAttribute(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS, OAuth20Constants.ATTRTYPE_REQUEST,
                                       new String[] { OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS_FAIL_INVALID_ID_TOKEN });
                    return;
                }
            }
            String tokenUsername = null;
            String clientId = null;

            if (cachedIdToken != null) {
                tokenUsername = cachedIdToken.getUsername();
                clientId = cachedIdToken.getClientId();
            } else {
                // if it's not there parse the idTokenString and validate signature.
                JWT jwt = null;
                try {
                    jwt = createJwt(idTokenHint, oauth20provider, oidcServerConfig);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "JWT : " + jwt);
                    }
                    //if (jwt.verify()) {
                    if (jwt.verifySignatureOnly()) {
                        tokenUsername = JsonTokenUtil.getSub(jwt.getPayload());
                        clientId = JsonTokenUtil.getAud(jwt.getPayload());
                    } else {
                        Tr.error(tc, "OIDC_SERVER_IDTOKEN_VERIFY_ERR", new Object[] { "IDTokenValidatonFailedException" });
                        attrs.setAttribute(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS, OAuth20Constants.ATTRTYPE_REQUEST,
                                           new String[] { OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS_FAIL_INVALID_ID_TOKEN });
                        return;
                    }
                } catch (IDTokenValidationFailedException ivfe) {
                    // if this exception is due to exp, iat expiration, the value can be extracted.
                    // otherwise, return error.
                    Throwable cause = ivfe.getCause();
                    if (cause != null && cause instanceof IllegalStateException) {
                        try {
                            JWTPayload payload = JsonTokenUtil.getPayload(idTokenHint);
                            if (payload != null) {
                                tokenUsername = JsonTokenUtil.getSub(payload);
                                clientId = JsonTokenUtil.getAud(payload);
                            }
                        } catch (Exception e) {
                            Tr.error(tc, "OIDC_SERVER_IDTOKEN_VERIFY_ERR", new Object[] { e });
                            attrs.setAttribute(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS, OAuth20Constants.ATTRTYPE_REQUEST,
                                               new String[] { OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS_FAIL_INVALID_ID_TOKEN });
                            return;
                        }
                    } else {
                        Tr.error(tc, "OIDC_SERVER_IDTOKEN_VERIFY_ERR", new Object[] { ivfe });
                        attrs.setAttribute(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS, OAuth20Constants.ATTRTYPE_REQUEST,
                                           new String[] { OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS_FAIL_INVALID_ID_TOKEN });
                        return;
                    }
                } catch (IllegalStateException ise) {
                    try {
                        JWTPayload payload = JsonTokenUtil.getPayload(idTokenHint);
                        if (payload != null) {
                            tokenUsername = JsonTokenUtil.getSub(payload);
                            clientId = JsonTokenUtil.getAud(payload);
                        }
                    } catch (IllegalStateException e) {
                        Tr.error(tc, "OIDC_SERVER_IDTOKEN_VERIFY_ERR", new Object[] { e });
                        attrs.setAttribute(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS, OAuth20Constants.ATTRTYPE_REQUEST,
                                           new String[] { OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS_FAIL_INVALID_ID_TOKEN });
                        return;
                    }
                } catch (Throwable e) {
                    Tr.error(tc, "OIDC_SERVER_IDTOKEN_VERIFY_ERR", new Object[] { e });
                    attrs.setAttribute(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS, OAuth20Constants.ATTRTYPE_REQUEST,
                                       new String[] { OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS_FAIL_INVALID_ID_TOKEN });
                    return;
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "id_token_hint username : " + tokenUsername + " client id: " + clientId);
            }

            if (tokenUsername != null || clientId != null) {
                attrs.setAttribute(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS, OAuth20Constants.ATTRTYPE_REQUEST,
                                   new String[] { OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS_SUCCESS });
                attrs.setAttribute(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_USERNAME, OAuth20Constants.ATTRTYPE_REQUEST, new String[] { tokenUsername });
                attrs.setAttribute(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_CLIENTID, OAuth20Constants.ATTRTYPE_REQUEST, new String[] { clientId });
            }
        }
        return;
    }

    private void processJWKRequest(HttpServletResponse response, OidcServerConfig oidcServerConfig) throws IOException {
        String methodName = "processJWKRequest";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName);
        }

        String jwkString = oidcServerConfig.getJwkJsonString();
        try {
            String cacheControlValue = response.getHeader(OAuth20Constants.HEADER_CACHE_CONTROL);
            if (cacheControlValue != null &&
                !cacheControlValue.isEmpty()) {
                cacheControlValue = cacheControlValue + ", " + OAuth20Constants.HEADERVAL_CACHE_CONTROL;
            } else {
                cacheControlValue = OAuth20Constants.HEADERVAL_CACHE_CONTROL;
            }
            response.setHeader(OAuth20Constants.HEADER_CACHE_CONTROL, cacheControlValue);
            response.setHeader(OAuth20Constants.HEADER_PRAGMA,
                               OAuth20Constants.HEADERVAL_PRAGMA);
            response.setStatus(200);
            if (jwkString != null) {
                response.setHeader(OAuth20Constants.HTTP_HEADER_CONTENT_TYPE,
                                   OAuth20Constants.HTTP_CONTENT_TYPE_JSON);
                PrintWriter pw;
                pw = response.getWriter();
                pw.write(jwkString);
                pw.flush();
            }
        } catch (IOException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Internal error processing JWK request", e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (IOException ioe) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Internal error process JWK request error", ioe);
            }
        }
    }

}
