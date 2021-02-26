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
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import javax.security.auth.Subject;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20AccessDeniedException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20DuplicateParameterException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.OAuth20Util;
import com.ibm.oauth.core.internal.oauth20.OAuthResultImpl;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.common.claims.UserClaims;
import com.ibm.ws.security.oauth20.ProvidersService;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.api.OAuth20EnhancedTokenCache;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.error.impl.OAuth20TokenRequestExceptionHandler;
import com.ibm.ws.security.oauth20.exception.OAuth20BadParameterException;
import com.ibm.ws.security.oauth20.plugins.OAuth20TokenImpl;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.util.ConfigUtils;
import com.ibm.ws.security.oauth20.util.Nonce;
import com.ibm.ws.security.oauth20.util.OAuth20ProviderUtils;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;
import com.ibm.ws.security.oauth20.web.OAuth20Request.EndpointType;
import com.ibm.ws.webcontainer.security.CookieHelper;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.oauth20.JwtAccessTokenMediator;
import com.ibm.wsspi.security.oauth20.TokenIntrospectProvider;

@Component(service = OAuth20EndpointServices.class, name = "com.ibm.ws.security.oauth20.web.OAuth20EndpointServices", immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.vendor=IBM")
public class OAuth20EndpointServices {
    private static TraceComponent tc = Tr.register(OAuth20EndpointServices.class);
    private static TraceComponent tc2 = Tr.register(OAuth20EndpointServices.class, // use this one when bundle is the usual bundle.
            TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);

    protected static final String MESSAGE_BUNDLE = "com.ibm.ws.security.oauth20.internal.resources.OAuthMessages";
    protected static final String MSG_RESOURCE_BUNDLE = "com.ibm.ws.security.oauth20.resources.ProviderMsgs";

    public static final String KEY_SERVICE_PID = "service.pid";

    public static final String KEY_SECURITY_SERVICE = "securityService";
    protected final AtomicServiceReference<SecurityService> securityServiceRef = new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);

    public static final String KEY_TOKEN_INTROSPECT_PROVIDER = "tokenIntrospectProvider";
    private final ConcurrentServiceReferenceMap<String, TokenIntrospectProvider> tokenIntrospectProviderRef = new ConcurrentServiceReferenceMap<String, TokenIntrospectProvider>(KEY_TOKEN_INTROSPECT_PROVIDER);

    public static final String KEY_JWT_MEDIATOR = "jwtAccessTokenMediator";
    private final ConcurrentServiceReferenceMap<String, JwtAccessTokenMediator> jwtAccessTokenMediatorRef = new ConcurrentServiceReferenceMap<String, JwtAccessTokenMediator>(KEY_JWT_MEDIATOR);

    public static final String KEY_OAUTH_CLIENT_METATYPE_SERVICE = "oauth20ClientMetatypeService";
    private static final AtomicServiceReference<OAuth20ClientMetatypeService> oauth20ClientMetatypeServiceRef = new AtomicServiceReference<OAuth20ClientMetatypeService>(KEY_OAUTH_CLIENT_METATYPE_SERVICE);

    private static final String ATTR_NONCE = "consentNonce";
    public static final String AUTHENTICATED = "authenticated";

    protected volatile ClientAuthentication clientAuthentication = new ClientAuthentication();
    protected volatile ClientAuthorization clientAuthorization = new ClientAuthorization();
    protected volatile UserAuthentication userAuthentication = new UserAuthentication();
    protected volatile CoverageMapEndpointServices coverageMapServices = new CoverageMapEndpointServices();
    protected volatile RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
    protected volatile Consent consent = new Consent();
    protected volatile TokenExchange tokenExchange = new TokenExchange();

    @Reference(service = SecurityService.class, name = KEY_SECURITY_SERVICE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.setReference(reference);
    }

    protected void unsetSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.unsetReference(reference);
    }

    @Reference(service = TokenIntrospectProvider.class, name = KEY_TOKEN_INTROSPECT_PROVIDER, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)
    protected void setTokenIntrospectProvider(ServiceReference<TokenIntrospectProvider> ref) {
        synchronized (tokenIntrospectProviderRef) {
            tokenIntrospectProviderRef.putReference((String) ref.getProperty(KEY_SERVICE_PID), ref);
        }
    }

    protected void unsetTokenIntrospectProvider(ServiceReference<TokenIntrospectProvider> ref) {
        synchronized (tokenIntrospectProviderRef) {
            tokenIntrospectProviderRef.removeReference((String) ref.getProperty(KEY_SERVICE_PID), ref);
        }
    }

    @Reference(service = JwtAccessTokenMediator.class, name = KEY_JWT_MEDIATOR, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    protected void setJwtAccessTokenMediator(ServiceReference<JwtAccessTokenMediator> ref) {
        synchronized (jwtAccessTokenMediatorRef) {
            jwtAccessTokenMediatorRef.putReference((String) ref.getProperty(KEY_SERVICE_PID), ref);
        }
    }

    protected void unsetJwtAccessTokenMediator(ServiceReference<JwtAccessTokenMediator> ref) {
        synchronized (jwtAccessTokenMediatorRef) {
            jwtAccessTokenMediatorRef.removeReference((String) ref.getProperty(KEY_SERVICE_PID), ref);
        }
    }

    @Reference(service = OAuth20ClientMetatypeService.class, name = KEY_OAUTH_CLIENT_METATYPE_SERVICE, policy = ReferencePolicy.DYNAMIC)
    protected void setOAuth20ClientMetatypeService(ServiceReference<OAuth20ClientMetatypeService> ref) {
        oauth20ClientMetatypeServiceRef.setReference(ref);
    }

    protected void unsetOAuth20ClientMetatypeService(ServiceReference<OAuth20ClientMetatypeService> ref) {
        oauth20ClientMetatypeServiceRef.unsetReference(ref);
    }

    @Activate
    protected void activate(ComponentContext cc) {

        securityServiceRef.activate(cc);
        tokenIntrospectProviderRef.activate(cc);
        jwtAccessTokenMediatorRef.activate(cc);
        oauth20ClientMetatypeServiceRef.activate(cc);
        ConfigUtils.setJwtAccessTokenMediatorService(jwtAccessTokenMediatorRef);
        TokenIntrospect.setTokenIntrospect(tokenIntrospectProviderRef);

        // The TraceComponent object was not initialized with the message bundle containing this message, so we cannot use
        // Tr.info(tc, "OAUTH_ENDPOINT_SERVICE_ACTIVATED"). Eventually these messages will be merged into one file, making this infoMsg variable unnecessary
        String infoMsg = TraceNLS.getFormattedMessage(this.getClass(),
                MESSAGE_BUNDLE,
                "OAUTH_ENDPOINT_SERVICE_ACTIVATED",
                null,
                "CWWKS1410I: The OAuth endpoint service is activated.");
        Tr.info(tc, infoMsg);

    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        securityServiceRef.deactivate(cc);
        tokenIntrospectProviderRef.deactivate(cc);
        jwtAccessTokenMediatorRef.deactivate(cc);
        oauth20ClientMetatypeServiceRef.deactivate(cc);
    }

    protected void handleOAuthRequest(HttpServletRequest request,
            HttpServletResponse response,
            ServletContext servletContext) throws ServletException, IOException {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Checking if OAuth20 Provider should process the request.");
            Tr.debug(tc, "Inbound request " + com.ibm.ws.security.common.web.WebUtils.getRequestStringForTrace(request, "client_secret"));
        }
        OAuth20Request oauth20Request = getAuth20Request(request, response);
        OAuth20Provider oauth20Provider = null;
        if (oauth20Request != null) {
            EndpointType endpointType = oauth20Request.getType();
            oauth20Provider = getProvider(response, oauth20Request);
            if (oauth20Provider != null) {
                AttributeList optionalParams = new AttributeList();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "OAUTH20 _SSO OP PROCESS IS STARTING.");
                    Tr.debug(tc, "OAUTH20 _SSO OP inbound URL " + com.ibm.ws.security.common.web.WebUtils.getRequestStringForTrace(request, "client_secret"));
                }
                handleEndpointRequest(request, response, servletContext, oauth20Provider, endpointType, optionalParams);
            }
        }
        if (tc.isDebugEnabled()) {
            if (oauth20Provider != null) {
                Tr.debug(tc, "OAUTH20 _SSO OP PROCESS HAS ENDED.");
            } else {
                Tr.debug(tc, "OAUTH20 _SSO OP WILL NOT PROCESS THE REQUEST");
            }
        }
    }

    @FFDCIgnore({ OidcServerException.class })
    protected void handleEndpointRequest(HttpServletRequest request,
            HttpServletResponse response,
            ServletContext servletContext,
            OAuth20Provider oauth20Provider,
            EndpointType endpointType,
            AttributeList oidcOptionalParams) throws ServletException, IOException {

        checkHttpsRequirement(request, response, oauth20Provider);
        if (response.isCommitted()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Response has already been committed, so likely did not pass HTTPS requirement");
            }
            return;
        }
        boolean isBrowserWithBasicAuth = false;
        UIAccessTokenBuilder uitb = null;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "endpointType[" + endpointType + "]");
        }
        try {
            switch (endpointType) {
            case authorize:
                OAuthResult result = processAuthorizationRequest(oauth20Provider, request, response, servletContext, oidcOptionalParams);

                if (result != null) {
                    if (result.getStatus() == OAuthResult.TAI_CHALLENGE) { // SPNEGO negotiate
                        break;
                    } else if (result.getStatus() != OAuthResult.STATUS_OK) {
                        userAuthentication.renderErrorPage(oauth20Provider, request, response, result);
                    }
                }
                break;
            case token:
                if (clientAuthentication.verify(oauth20Provider, request, response, endpointType)) {
                    processTokenRequest(oauth20Provider, request, response);
                }
                break;
            case introspect:
                if (clientAuthentication.verify(oauth20Provider, request, response, endpointType)) {
                    introspect(oauth20Provider, request, response);
                }
                break;
            case revoke:
                if (clientAuthentication.verify(oauth20Provider, request, response, endpointType)) {
                    revoke(oauth20Provider, request, response);
                }
                break;
            case coverage_map: // non-spec extension
                coverageMapServices.handleEndpointRequest(oauth20Provider, request, response);
                break;
            case registration:
                secureEndpointServices(oauth20Provider, request, response, servletContext, RegistrationEndpointServices.ROLE_REQUIRED, true);
                registrationEndpointServices.handleEndpointRequest(oauth20Provider, request, response);
                break;
            case logout:
                // no need to authenticate
                logout(oauth20Provider, request, response);
                break;
            case app_password:
                tokenExchange.processAppPassword(oauth20Provider, request, response);
                break;
            case app_token:
                tokenExchange.processAppToken(oauth20Provider, request, response);
                break;

            // these next 3 are for UI pages
            case clientManagement:
                if (!authenticateUI(request, response, servletContext, oauth20Provider, oidcOptionalParams, RegistrationEndpointServices.ROLE_REQUIRED)) {
                    break;
                }
                // new MockUiPage(request, response).render(); // TODO: replace with hook to real ui.
                RequestDispatcher rd = servletContext.getRequestDispatcher("WEB-CONTENT/clientAdmin/index.jsp");
                rd.forward(request, response);
                break;

            case personalTokenManagement:
                if (!authenticateUI(request, response, servletContext, oauth20Provider, oidcOptionalParams, null)) {
                    break;
                }
                checkUIConfig(oauth20Provider, request);
                // put auth header and access token and feature enablement state into request attributes for ui to use
                uitb = new UIAccessTokenBuilder(oauth20Provider, request);
                uitb.createHeaderValuesForUI();
                // new MockUiPage(request, response).render(); // TODO: replace with hook to real ui.
                RequestDispatcher rd2 = servletContext.getRequestDispatcher("WEB-CONTENT/accountManager/index.jsp");
                rd2.forward(request, response);
                break;

            case usersTokenManagement:
                if (!authenticateUI(request, response, servletContext, oauth20Provider, oidcOptionalParams, OAuth20Constants.TOKEN_MANAGER_ROLE)) {
                    break;
                }
                checkUIConfig(oauth20Provider, request);
                // put auth header and access token and feature enablement state into request attributes for ui to use
                uitb = new UIAccessTokenBuilder(oauth20Provider, request);
                uitb.createHeaderValuesForUI();
                // new MockUiPage(request, response).render(); // TODO: replace with hook to real ui.
                RequestDispatcher rd3 = servletContext.getRequestDispatcher("WEB-CONTENT/tokenManager/index.jsp");
                rd3.forward(request, response);
                break;

            case clientMetatype:
                serveClientMetatypeRequest(request, response);
                break;

            default:
                break;
            }
        } catch (OidcServerException e) {
            if (!isBrowserWithBasicAuth) {
                // we don't want routine browser auth challenges producing ffdc's.
                // (but if a login is invalid in that case, we will still get a CWIML4537E from base sec.)
                // however for non-browsers we want ffdc's like we had before, so generate manually

                if (!e.getErrorDescription().contains("CWWKS1424E")) { // no ffdc for nonexistent clients
                    com.ibm.ws.ffdc.FFDCFilter.processException(e,
                            "com.ibm.ws.security.oauth20.web.OAuth20EndpointServices", "324", this);
                }
            }
            boolean suppressBasicAuthChallenge = isBrowserWithBasicAuth; // ui must NOT log in using basic auth, so logout function will work.
            WebUtils.sendErrorJSON(response, e.getHttpStatus(), e.getErrorCode(), e.getErrorDescription(request.getLocales()), suppressBasicAuthChallenge);
        }

    }

    // return true if clear to go, false otherwise. Log message and/or throw exception if unsuccessful
    private boolean authenticateUI(HttpServletRequest request, HttpServletResponse response,
            ServletContext servletContext, OAuth20Provider provider, AttributeList options, String requiredRole)
            throws ServletException, IOException, OidcServerException {

        OAuthResult result = handleUIUserAuthentication(request, response, servletContext, provider, options);
        if (!isUIAuthenticationComplete(request, response, provider, result, requiredRole)) {
            return false;
        }

        return true;
    }

    private boolean isUIAuthenticationComplete(HttpServletRequest request, HttpServletResponse response, OAuth20Provider provider, OAuthResult result, String requiredRole) throws OidcServerException {
        if (result == null) { // sent to login page
            return false;
        }
        if (result.getStatus() == OAuthResult.TAI_CHALLENGE) { // SPNEGO negotiate
            return false;
        }
        if (result.getStatus() != OAuthResult.STATUS_OK) {
            try {
                userAuthentication.renderErrorPage(provider, request, response, result);
            } catch (Exception e) {
                // ffdc
            }
            return false;
        }
        if (requiredRole != null && !request.isUserInRole(requiredRole)) {
            throw new OidcServerException("403", OIDCConstants.ERROR_ACCESS_DENIED, HttpServletResponse.SC_FORBIDDEN);
        }
        return true;

    }

    void serveClientMetatypeRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        OAuth20ClientMetatypeService metatypeService = oauth20ClientMetatypeServiceRef.getService();
        if (metatypeService == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        metatypeService.sendClientMetatypeData(request, response);
    }

    private boolean checkUIConfig(OAuth20Provider provider, HttpServletRequest request) {
        String uri = request.getRequestURI();
        String id = provider.getInternalClientId();
        String secret = provider.getInternalClientSecret();
        OidcBaseClient client = null;
        boolean result = false;
        try {
            client = provider.getClientProvider().get(id);
        } catch (OidcServerException e) {
            // ffdc
        }
        if (client != null) {
            result = secret != null && client.isEnabled() && (client.isAppPasswordAllowed() || client.isAppTokenAllowed());
        }
        if (!result) {
            Tr.warning(tc2, "OAUTH_UI_ENDPOINT_NOT_ENABLED", uri);
        }
        return result;
    }

    /**
     * Perform logout.  call base security logout to clear ltpa cookie.
     * Then redirect to a configured logout page if available, else a default.
     *
     * This does NOT implement
     * OpenID Connect Session Management 1.0 draft 28. as of Nov. 2017
     * https://openid.net/specs/openid-connect-session-1_0.html#RedirectionAfterLogout
     *
     * Instead it is a simpler approach that just deletes the ltpa (sso) cookie
     * and sends a simple error page if things go wrong.
     *
     * @param provider
     * @param request
     * @param response
     */
    public void logout(OAuth20Provider provider,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Processing logout");
        }
        try {
            request.logout(); // ltpa cookie removed if present. No exception if not.
        } catch (ServletException e) {
            FFDCFilter.processException(e,
                    this.getClass().getName(), "logout",
                    new Object[] {});
            new LogoutPages().sendDefaultErrorPage(request, response);
            return;
        }

        // not part of spec: logout url defined in config, not client-specific
        String logoutRedirectURL = provider.getLogoutRedirectURL();
        try {
            if (logoutRedirectURL != null) {
                String encodedURL = URLEncodeParams(logoutRedirectURL);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "OAUTH20 _SSO OP redirecting to [" + logoutRedirectURL + "], url encoded to [" + encodedURL + "]");
                }
                response.sendRedirect(encodedURL);
                return;
            } else {
                // send default logout page
                new LogoutPages().sendDefaultLogoutPage(request, response);
            }
        } catch (IOException e) {
            FFDCFilter.processException(e,
                    this.getClass().getName(), "logout",
                    new Object[] {});
            new LogoutPages().sendDefaultErrorPage(request, response);
        }
    }

    String URLEncodeParams(String UrlStr) {
        String sep = "?";
        String encodedURL = UrlStr;
        int index = UrlStr.indexOf(sep);
        // if encoded url in server.xml, don't encode it again.
        boolean alreadyEncoded = UrlStr.contains("%");
        if (index > -1 && !alreadyEncoded) {
            index++; // don't encode ?
            String prefix = UrlStr.substring(0, index);
            String suffix = UrlStr.substring(index);
            try {
                encodedURL = prefix + java.net.URLEncoder.encode(suffix, StandardCharsets.UTF_8.toString());
                // shouldn't encode = in queries, so flip those back
                encodedURL = encodedURL.replace("%3D", "=");
            } catch (UnsupportedEncodingException e) {
                // ffdc
            }
        }
        return encodedURL;
    }

    public OAuthResult processAuthorizationRequest(OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response,
            ServletContext servletContext, AttributeList options)
            throws ServletException, IOException, OidcServerException {
        OAuthResult oauthResult = checkForError(request);
        if (oauthResult != null) {
            return oauthResult;
        }
        boolean autoAuthz = clientAuthorization.isClientAutoAuthorized(provider, request);
        String reqConsentNonce = getReqConsentNonce(request);
        boolean afterLogin = isAfterLogin(request); // we've been to login.jsp or it's replacement.

        if (reqConsentNonce == null) { // validate request for initial authorization request only
            oauthResult = clientAuthorization.validateAuthorization(provider, request, response);
            if (oauthResult.getStatus() != OAuthResult.STATUS_OK) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Status is OK, returning result");
                }
                return oauthResult;
            }
        }

        oauthResult = handleUserAuthentication(oauthResult, request, response, servletContext,
                provider, reqConsentNonce, options, autoAuthz, afterLogin);

        return oauthResult;
    }

    /**
     * Adds the id_token_hint_status, id_token_hint_username, and id_token_hint_clientid attributes from the options list
     * into attrList, if those attributes exist.
     *
     * @param options
     * @param attrList
     */
    private void setTokenHintAttributes(AttributeList options, AttributeList attrList) {
        String value = options.getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS);
        if (value != null) {
            attrList.setAttribute(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS, OAuth20Constants.ATTRTYPE_REQUEST, new String[] { value });
        }
        value = options.getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_USERNAME);
        if (value != null) {
            attrList.setAttribute(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_USERNAME, OAuth20Constants.ATTRTYPE_REQUEST, new String[] { value });
        }
        value = options.getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_CLIENTID);
        if (value != null) {
            attrList.setAttribute(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_CLIENTID, OAuth20Constants.ATTRTYPE_REQUEST, new String[] { value });
        }
    }

    /**
     * @param attrs
     * @param request
     * @return OAuthResultImpl if validation failed, null otherwise.
     */
    private OAuthResultImpl validateIdTokenHintIfPresent(AttributeList attrs, HttpServletRequest request) {
        if (attrs != null) {
            Principal user = request.getUserPrincipal();
            String username = null;
            if (user != null) {
                username = user.getName();
            }
            try {
                userAuthentication.validateIdTokenHint(username, attrs);
            } catch (OAuth20Exception oe) {
                return new OAuthResultImpl(OAuthResult.STATUS_FAILED, attrs, oe);
            }
        }
        return null;
    }

    /**
     * Creates a 401 STATUS_FAILED result due to the token limit being reached.
     *
     * @param attrs
     * @param request
     * @param clientId
     * @return
     */
    private OAuthResult createTokenLimitResult(AttributeList attrs, HttpServletRequest request, String clientId) {
        if (attrs == null) {
            attrs = new AttributeList();
            String responseType = request.getParameter(OAuth20Constants.RESPONSE_TYPE);
            attrs.setAttribute(OAuth20Constants.RESPONSE_TYPE, OAuth20Constants.RESPONSE_TYPE, new String[] { responseType });
            attrs.setAttribute(OAuth20Constants.CLIENT_ID, OAuth20Constants.CLIENT_ID, new String[] { clientId });
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Attribute responseType:" + responseType + " client_id:" + clientId);
            }
        }
        OAuth20AccessDeniedException e = new OAuth20AccessDeniedException("security.oauth20.token.limit.external.error");
        e.setHttpStatusCode(HttpServletResponse.SC_BAD_REQUEST);
        OAuthResult oauthResultWithExcep = new OAuthResultImpl(OAuthResult.STATUS_FAILED, attrs, e);

        return oauthResultWithExcep;
    }

    private OAuthResult handleUIUserAuthentication(HttpServletRequest request, HttpServletResponse response,
            ServletContext servletContext, OAuth20Provider provider, AttributeList options) throws IOException, ServletException, OidcServerException {
        OAuthResult oauthResult = null;
        Prompt prompt = new Prompt();
        if (request.getUserPrincipal() == null) {
            // authenticate user if not done yet. Send to login page.
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Authenticate user if not done yet");
            }
            oauthResult = userAuthentication.handleAuthenticationWithOAuthResult(provider, request, response, prompt, securityServiceRef, servletContext, OAuth20EndpointServices.AUTHENTICATED, oauthResult);
        }

        if (request.getUserPrincipal() == null) { // must be redirect
            return oauthResult;

        } else if (CookieHelper.getCookieValue(request.getCookies(), ReferrerURLCookieHandler.CUSTOM_RELOGIN_URL_COOKIENAME) != null) {
            ReferrerURLCookieHandler handler = WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig().createReferrerURLCookieHandler(); // GM 2017.05.31
            // ReferrerURLCookieHandler handler = new ReferrerURLCookieHandler(WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig());
            handler.invalidateReferrerURLCookie(request, response, ReferrerURLCookieHandler.CUSTOM_RELOGIN_URL_COOKIENAME);
        }

        if (!request.isUserInRole(AUTHENTICATED)) { // must be authorized, we'll check userInRole later.
            Tr.audit(tc, "security.oauth20.error.authorization", request.getUserPrincipal().getName());
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return oauthResult;
        }
        return new OAuthResultImpl(OAuthResult.STATUS_OK, new AttributeList());

    }

    @FFDCIgnore({ OAuth20BadParameterException.class })
    private OAuthResult handleUserAuthentication(OAuthResult oauthResult, HttpServletRequest request, HttpServletResponse response,
            ServletContext servletContext, OAuth20Provider provider,
            String reqConsentNonce, AttributeList options, boolean autoauthz, boolean afterLogin) throws IOException, ServletException, OidcServerException {
        Prompt prompt = null;
        String[] scopesAttr = null;
        AttributeList attrs = null;
        if (oauthResult != null) {
            attrs = oauthResult.getAttributeList();
            scopesAttr = attrs.getAttributeValuesByName(OAuth20Constants.SCOPE);
            if (options != null) {
                setTokenHintAttributes(options, attrs);
            }
            String[] validResources = attrs.getAttributeValuesByName(OAuth20Constants.RESOURCE);
            if (validResources != null) {
                options.setAttribute(OAuth20Constants.RESOURCE, OAuth20Constants.ATTRTYPE_PARAM_OAUTH, validResources);
            }
        }

        // Per section 4.1.2.1 of the OAuth 2.0 spec (RFC6749), the state parameter must be included in any error response if it was
        // originally provided in the request. Adding it to the attribute list here will ensure it is propagated to any failure response.
        String[] stateParams = request.getParameterValues(OAuth20Constants.STATE);
        if (stateParams != null) {
            if (attrs == null) {
                attrs = new AttributeList();
            }
            attrs.setAttribute(OAuth20Constants.STATE, OAuth20Constants.ATTRTYPE_PARAM_QUERY, stateParams);
        }

        boolean isOpenId = false;
        if (scopesAttr != null) {
            for (String scope : scopesAttr) {
                if (OIDCConstants.SCOPE_OPENID.equals(scope)) {
                    isOpenId = true;
                    break;
                }
            }
        }
        if (isOpenId) {
            // if id_token_hint exists and user is already logged in, compare...
            OAuthResultImpl result = validateIdTokenHintIfPresent(attrs, request);
            if (result != null) {
                return result;
            }
        }

        prompt = new Prompt(request);

        if (request.getUserPrincipal() == null || (prompt.hasLogin() && !afterLogin)) {
            // authenticate user if not done yet
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Authenticate user if not done yet");
            }
            oauthResult = userAuthentication.handleAuthenticationWithOAuthResult(provider, request, response, prompt, securityServiceRef, servletContext, OAuth20EndpointServices.AUTHENTICATED, oauthResult);
        }

        if (request.getUserPrincipal() == null) { // must be redirect
            return oauthResult;
        } else if (CookieHelper.getCookieValue(request.getCookies(), ReferrerURLCookieHandler.CUSTOM_RELOGIN_URL_COOKIENAME) != null) {
            ReferrerURLCookieHandler handler = WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig().createReferrerURLCookieHandler(); // GM 2017.05.31
            // ReferrerURLCookieHandler handler = new ReferrerURLCookieHandler(WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig());
            handler.invalidateReferrerURLCookie(request, response, ReferrerURLCookieHandler.CUSTOM_RELOGIN_URL_COOKIENAME);
        }

        if (!request.isUserInRole(AUTHENTICATED) && !request.isUserInRole(OAuth20Constants.TOKEN_MANAGER_ROLE)) { // must be authorized
            Tr.audit(tc, "security.oauth20.error.authorization", request.getUserPrincipal().getName());
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return oauthResult;
        }

        if (reqConsentNonce != null && !consent.isNonceValid(request, reqConsentNonce)) { // nonce must be valid if has one
            consent.handleNonceError(request, response);
            return oauthResult;
        }

        String clientId = getClientId(request);

        String[] reducedScopes = null;
        try {
            reducedScopes = clientAuthorization.getReducedScopes(provider, request, clientId, true);
        } catch (Exception e1) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception, so setting reduced scopes to null. Exception was: " + e1);
            }
            reducedScopes = null;
        }

        boolean preAuthzed = false;
        if (reqConsentNonce == null) {
            try {
                preAuthzed = clientAuthorization.isPreAuthorizedScope(provider, clientId, reducedScopes);
            } catch (Exception e) {
                preAuthzed = false;
            }
        }

        // Handle consent
        if (!autoauthz && !preAuthzed && reqConsentNonce == null && !consent.isCachedAndValid(oauthResult, provider, request, response)) {
            if (prompt.hasNone()) {
                // Prompt includes "none," however authorization has not been obtained or cached; return error
                oauthResult = prompt.errorConsentRequired(attrs);
            } else {
                // ask user for approval if not auto authorized, or not approved
                Nonce nonce = consent.setNonce(request);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "_SSO OP redirecting for consent");
                }
                consent.renderConsentForm(request, response, provider, clientId, nonce, oauthResult.getAttributeList(), servletContext);
            }
            return oauthResult;
        }

        if (reachedTokenLimit(provider, request)) {
            return createTokenLimitResult(attrs, request, clientId);
        }

        if (request.getAttribute(OAuth20Constants.OIDC_REQUEST_OBJECT_ATTR_NAME) != null) {
            // Ensure that the reduced scopes list is not empty
            oauthResult = clientAuthorization.checkForEmptyScopeSetAfterConsent(reducedScopes, oauthResult, request, provider, clientId);
            if (oauthResult != null && oauthResult.getStatus() != OAuthResult.STATUS_OK) {
                response.setStatus(HttpServletResponse.SC_FOUND);
                return oauthResult;
            }
        }

        // getBack the resource. better double check it
        OidcBaseClient client;
        try {
            client = OAuth20ProviderUtils.getOidcOAuth20Client(provider, clientId);
            OAuth20ProviderUtils.validateResource(request, options, client);
        } catch (OAuth20BadParameterException e) { // some exceptions need to handled separately
            WebUtils.throwOidcServerException(request, e);
        } catch (OAuth20Exception e) {
            WebUtils.throwOidcServerException(request, e);
        }

        if (options != null) {
            options.setAttribute(OAuth20Constants.SCOPE, OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE, reducedScopes);
        }

        if (provider.isTrackOAuthClients()) {
            OAuthClientTracker clientTracker = new OAuthClientTracker(request, response, provider);
            clientTracker.trackOAuthClient(clientId);
        }

        consent.handleConsent(provider, request, prompt, clientId);
        getExternalClaimsFromWSSubject(request, options);
        oauthResult = provider.processAuthorization(request, response, options);

        return oauthResult;
    }

    /**
     * Secure registration services with BASIC Auth and validating against the required role.
     *
     * @param provider
     * @param request
     * @param response
     * @param servletContext
     * @param requiredRole - user must be in this role.
     * @param fallbacktoBasicAuth - if false, if there is no cookie on the request, then no basic auth challenge will be sent back to browser.
     * @throws OidcServerException
     */
    @FFDCIgnore({ OidcServerException.class })
    private void secureEndpointServices(OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response, ServletContext servletContext, String requiredRole, boolean fallbackToBasicAuth) throws OidcServerException {
        try {
            userAuthentication.handleBasicAuthenticationWithRequiredRole(provider, request, response, securityServiceRef, servletContext, requiredRole, fallbackToBasicAuth);
        } catch (OidcServerException e) {
            if (fallbackToBasicAuth) {
                if (e.getHttpStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
                    response.setHeader(RegistrationEndpointServices.HDR_WWW_AUTHENTICATE, RegistrationEndpointServices.UNAUTHORIZED_HEADER_VALUE);
                }
            }
            throw e;
        }
    }

    @FFDCIgnore({ OAuth20BadParameterException.class })
    public void processTokenRequest(OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response) throws ServletException, OidcServerException {
        String clientId = (String) request.getAttribute("authenticatedClient");
        try {
            // checking resource
            OidcBaseClient client = OAuth20ProviderUtils.getOidcOAuth20Client(provider, clientId);
            if (client == null || !client.isEnabled()) {
                throw new OidcServerException("security.oauth20.error.invalid.client", OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);
            }
            OAuth20ProviderUtils.validateResource(request, null, client);
        } catch (OAuth20BadParameterException e) { // some exceptions need to handled separately
            WebUtils.throwOidcServerException(request, e);
        } catch (OAuth20Exception e) {
            WebUtils.throwOidcServerException(request, e);
        }

        OAuthResult result = clientAuthorization.validateAndHandle2LegsScope(provider, request, response, clientId);
        if (result.getStatus() == OAuthResult.STATUS_OK) {
            result = provider.processTokenRequest(clientId, request, response);

        }
        if (result.getStatus() != OAuthResult.STATUS_OK) {
            OAuth20TokenRequestExceptionHandler handler = new OAuth20TokenRequestExceptionHandler();
            handler.handleResultException(request, response, result);
        }
    }

    /**
     * Get the access token from the request's token parameter and look it up in
     * the token cache.
     *
     * If the access token is found in the cache return status 200 and a JSON object.
     *
     * If the token is not found or the request had errors return status 400.
     *
     * @param provider
     * @param request
     * @param response
     * @throws OidcServerException
     * @throws IOException
     */
    public void introspect(OAuth20Provider provider,
            HttpServletRequest request,
            HttpServletResponse response) throws OidcServerException, IOException {
        TokenIntrospect tokenIntrospect = new TokenIntrospect();
        tokenIntrospect.introspect(provider, request, response);
    }

    /**
     * Revoke the provided token by removing it from the cache
     *
     * If the access token is found in the cache remove it from the cache
     * and return status 200.
     *
     * @param provider
     * @param request
     * @param response
     */
    public void revoke(OAuth20Provider provider,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            String tokenString = request.getParameter(com.ibm.ws.security.oauth20.util.UtilConstants.TOKEN);
            if (tokenString == null) {
                // send 400 per OAuth Token revocation spec
                WebUtils.sendErrorJSON(response, HttpServletResponse.SC_BAD_REQUEST,
                        Constants.ERROR_CODE_INVALID_REQUEST, null); // invalid_request
                return;
            }
            String tokenLookupStr = tokenString;
            OAuth20Token token = null;
            boolean isAppPasswordOrToken = false;
            if (OidcOAuth20Util.isJwtToken(tokenString)) {
                tokenLookupStr = com.ibm.ws.security.oauth20.util.HashUtils.digest(tokenString);
            } else if (tokenString.length() == (provider.getAccessTokenLength() + 2)) {
                // app-token
                String encode = provider.getAccessTokenEncoding();
                if (OAuth20Constants.PLAIN_ENCODING.equals(encode)) { // must be app-password or app-token
                    tokenLookupStr = EndpointUtils.computeTokenHash(tokenString);
                } else {
                    tokenLookupStr = EndpointUtils.computeTokenHash(tokenString, encode);
                }
                isAppPasswordOrToken = true;
            }
            if (isAppPasswordOrToken) {
                token = provider.getTokenCache().getByHash(tokenLookupStr);
            } else {
                token = provider.getTokenCache().get(tokenLookupStr);
            }

            boolean isAppPassword = false;
            if (token != null && OAuth20Constants.APP_PASSWORD.equals(token.getGrantType())) {
                isAppPassword = true;
                Tr.error(tc, "security.oauth20.apppwtok.revoke.disallowed", new Object[] {});
            }
            if (token == null) {
                // send 200 per OAuth Token revocation spec
                response.setStatus(HttpServletResponse.SC_OK);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "token " + tokenString + " not in cache or wrong token type, return");
                }
                return;
            }
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "token type: " + token.getType());
            }
            ClientAuthnData clientAuthData = new ClientAuthnData(request, response);
            if (clientAuthData.hasAuthnData() &&
                    clientAuthData.getUserName().equals(token.getClientId()) == true) {

                if (!isAppPassword && ((token.getType().equals(OAuth20Constants.TOKENTYPE_AUTHORIZATION_GRANT) &&
                        token.getSubType().equals(OAuth20Constants.SUBTYPE_REFRESH_TOKEN)) ||
                        token.getType().equals(OAuth20Constants.TOKENTYPE_ACCESS_TOKEN))) {
                    // Revoke the token by removing it from the cache
                    if (tc.isDebugEnabled()) {
                        OAuth20Token theToken = provider.getTokenCache().get(tokenLookupStr);
                        String buf = (theToken != null) ? "is in the cache" : "is not in the cache";
                        Tr.debug(tc, "token " + tokenLookupStr + " " + buf + ", calling remove");
                    }
                    if (isAppPasswordOrToken) {
                        provider.getTokenCache().removeByHash(tokenLookupStr);
                    } else {
                        provider.getTokenCache().remove(tokenLookupStr);
                    }
                    if (token.getSubType().equals(OAuth20Constants.SUBTYPE_REFRESH_TOKEN)) {
                        removeAssociatedAccessTokens(request, provider, token);
                    }
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    // Unsupported token type, send 400 per RFC7009 OAuth Token revocation spec
                    WebUtils.sendErrorJSON(response, HttpServletResponse.SC_BAD_REQUEST,
                            Constants.ERROR_CODE_UNSUPPORTED_TOKEN_TYPE, null);
                }
            } else {
                // client is not authorized. send 400 per RFC6749 5.2 OAuth Token revocation spec
                String defaultMsg = "CWWKS1406E: The revoke request had an invalid client credential. The request URI was {" + request.getRequestURI() + "}.";
                String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                        MESSAGE_BUNDLE,
                        "OAUTH_INVALID_CLIENT",
                        new Object[] { "revoke", request.getRequestURI() },
                        defaultMsg);

                WebUtils.sendErrorJSON(response, HttpServletResponse.SC_BAD_REQUEST,
                        Constants.ERROR_CODE_INVALID_CLIENT, errorMsg);
            }
        } catch (OAuth20DuplicateParameterException e) {
            // Duplicate parameter found in request
            WebUtils.sendErrorJSON(response, HttpServletResponse.SC_BAD_REQUEST, Constants.ERROR_CODE_INVALID_REQUEST, e.getMessage());
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Internal error processing token revoke request", e);
            }
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (IOException ioe) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Internal error process token introspect revoke error", ioe);
                }
            }
        }
    }

    /**
     * For OpenidConnect, when a refresh token is revoked, also delete all access tokens that have become associated with it.
     * (Each time a refresh token is submitted for a new one, the prior access tokens become associated with the new refresh token)
     * @param request
     * @param provider
     * @param refreshToken
     * @throws Exception
     */
    private void removeAssociatedAccessTokens(HttpServletRequest request, OAuth20Provider provider, OAuth20Token refreshToken) throws Exception {
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.equals("/oidc")) {
            // if this is for oauth, return. Oauth's persistence code doesn't support this token association.
            // and we only wanted revocation for oidc, we wanted to leave oauth alone.
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "not oidc, returning");
            }
            return;
        }
        if (!provider.getRevokeAccessTokensWithRefreshTokens()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "provider prop revokeAccessTokensWithRefreshTokens is false, returning");
            }
            return;
        }
        String username = refreshToken.getUsername();
        String clientId = refreshToken.getClientId();
        String refreshTokenId = refreshToken.getId();

        OAuth20EnhancedTokenCache cache = provider.getTokenCache();
        Collection<OAuth20Token> ctokens = cache.getAllUserTokens(username);
        for (OAuth20Token ctoken : ctokens) {
            boolean nullGuard = (cache != null && ctoken.getType() != null && clientId != null && ctoken.getClientId() != null && ctoken.getId() != null);

            if (nullGuard && OAuth20Constants.TOKENTYPE_ACCESS_TOKEN.equals(ctoken.getType()) && clientId.equals(ctoken.getClientId())
                    && refreshTokenId.equals(((OAuth20TokenImpl) ctoken).getRefreshTokenKey())) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "removing token: " + ctoken.getId());
                }
                cache.remove(ctoken.getId());
            }
        }

    }

    protected void checkHttpsRequirement(HttpServletRequest request, HttpServletResponse response, OAuth20Provider provider) throws IOException {
        String url = request.getRequestURL().toString();
        if (provider.isHttpsRequired()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Checking if URL starts with https: " + url);
            }
            if (url != null && !url.startsWith("https")) {
                Tr.error(tc, "security.oauth20.error.wrong.http.scheme", new Object[] { url });
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                        Tr.formatMessage(tc, "security.oauth20.error.wrong.http.scheme",
                                new Object[] { url }));
            }
        }
    }

    /**
     * Determines if this user hit the token limit for the user / client combination
     *
     * @param provider
     * @param request
     * @return
     */
    protected boolean reachedTokenLimit(OAuth20Provider provider, HttpServletRequest request) {
        String userName = getUserName(request);
        String clientId = getClientId(request);
        long limit = provider.getClientTokenCacheSize();
        if (limit > 0) {
            long numtokens = provider.getTokenCache().getNumTokens(userName, clientId);
            if (numtokens >= limit) {
                Tr.error(tc, "security.oauth20.token.limit.error", new Object[] { userName, clientId, limit });
                return true;
            }
        }
        return false;
    }

    private OAuth20Request getAuth20Request(HttpServletRequest request, HttpServletResponse response) throws IOException {
        OAuth20Request oauth20Request = (OAuth20Request) request.getAttribute(OAuth20Constants.OAUTH_REQUEST_OBJECT_ATTR_NAME);
        if (oauth20Request == null) {
            String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                    MESSAGE_BUNDLE,
                    "OAUTH_REQUEST_ATTRIBUTE_MISSING",
                    new Object[] { request.getRequestURI(), OAuth20Constants.OAUTH_REQUEST_OBJECT_ATTR_NAME },
                    "CWWKS1412E: The request endpoint {0} does not have attribute {1}.");
            Tr.error(tc, errorMsg);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
        return oauth20Request;
    }

    private OAuth20Provider getProvider(HttpServletResponse response, OAuth20Request oauth20Request) throws IOException {
        OAuth20Provider provider = ProvidersService.getOAuth20Provider(oauth20Request.getProviderName());
        if (provider == null) {
            String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                    MESSAGE_BUNDLE,
                    "OAUTH_PROVIDER_OBJECT_NULL",
                    new Object[] { oauth20Request.getProviderName(), OAuth20Constants.OAUTH_REQUEST_OBJECT_ATTR_NAME },
                    "CWWKS1413E: The OAuth20Provider object is null for OAuth provider {0}.");
            Tr.error(tc, errorMsg);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
        return provider;
    }

    private String getReqConsentNonce(HttpServletRequest request) {
        return request.getParameter(ATTR_NONCE);
    }

    private String getUserName(HttpServletRequest request) {
        return request.getUserPrincipal().getName();
    }

    private String getClientId(HttpServletRequest request) {
        return request.getParameter(OAuth20Constants.CLIENT_ID);
    }

    /* returns whether login form had been presented */
    protected boolean isAfterLogin(HttpServletRequest request) {
        boolean output = false;
        HttpSession session = request.getSession(false);
        if (session != null) {
            if (session.getAttribute(Constants.ATTR_AFTERLOGIN) != null) {
                session.removeAttribute(Constants.ATTR_AFTERLOGIN);
                output = true;
            }
        }
        return output;
    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    public Map<String, String[]> getExternalClaimsFromWSSubject(HttpServletRequest request, AttributeList options) {
        final String methodName = "getExternalClaimsFromWSSubject";
        try {
            String externalClaimNames = options.getAttributeValueByName(OAuth20Constants.EXTERNAL_CLAIM_NAMES);
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName + " externalClamiNames:" + externalClaimNames);
            if (externalClaimNames != null) {

                Map<String, String[]> map2 = (Map<String, String[]>) getFromWSSubject(OAuth20Constants.EXTERNAL_MEDIATION);
                if (map2 != null && map2.size() > 0) {
                    Set<Entry<String, String[]>> entries = map2.entrySet();
                    for (Entry<String, String[]> entry : entries) {
                        options.setAttribute(entry.getKey(), OAuth20Constants.EXTERNAL_MEDIATION, entry.getValue());
                    }
                }

                // get the external claims
                Map<String, String[]> map = (Map<String, String[]>) getFromWSSubject(OAuth20Constants.EXTERNAL_CLAIMS);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName + " externalClaims:" + map);
                if (map == null)
                    return null;

                // filter properties by externalClaimNames
                StringTokenizer strTokenizer = new StringTokenizer(externalClaimNames, ", ");
                while (strTokenizer.hasMoreTokens()) {
                    String key = strTokenizer.nextToken();
                    String[] values = map.get(key);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, methodName + " key:" + key + " values:'" + OAuth20Util.arrayToSpaceString(values) + "'");
                    if (values != null && values.length > 0) {
                        options.setAttribute(OAuth20Constants.EXTERNAL_CLAIMS_PREFIX + key, OAuth20Constants.EXTERNAL_CLAIMS, values);
                    }
                }

                return map;
            }
        } catch (WSSecurityException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName + " failed. Nothing changed. WSSecurityException:" + e);
        }
        return null;
    }

    /**
     * @param externalClaims
     * @return
     */
    private Object getFromWSSubject(String externalClaims) throws WSSecurityException {
        Subject runAsSubject = WSSubject.getRunAsSubject();
        Object obj = null;
        try {
            Set<Object> publicCreds = runAsSubject.getPublicCredentials();
            if (publicCreds != null && publicCreds.size() > 0) {
                Iterator<Object> publicCredIterator = publicCreds.iterator();

                while (publicCredIterator.hasNext()) {
                    Object cred = publicCredIterator.next();
                    if (cred != null && cred instanceof Hashtable) {
                        @SuppressWarnings("rawtypes")
                        Hashtable userCred = (Hashtable) cred;
                        obj = userCred.get(externalClaims);
                        if (obj != null) {
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, "getFromWSSubject found:" + obj);
                            }
                            break;
                        }
                    }
                }
            }

        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to match predefined cache key." + e);
            }
        }
        return obj;
    }

    /**
     * Check if the request contains an "error" parameter. If one is present and equals the OAuth 2.0 error type "access_denied", a message indicating the user likely canceled the request will be
     * logged and a failure result will be returned.
     *
     * @param request
     * @return A {@code OAuthResult} object initialized with AuthResult.FAILURE and 403 status code if an "error" parameter is present and equals "access_denied." Returns null otherwise.
     */
    private OAuthResult checkForError(HttpServletRequest request) {
        OAuthResult result = null;

        String error = request.getParameter("error");
        if (error != null && error.length() > 0 && OAuth20Exception.ACCESS_DENIED.equals(error)) {
            // User likely canceled the request
            String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                    MESSAGE_BUNDLE,
                    "security.oauth20.request.denied",
                    new Object[] {},
                    "CWOAU0067E: The request has been denied by the user, or another error occurred that resulted in denial of the request.");
            Tr.error(tc, errorMsg);
            OAuth20AccessDeniedException e = new OAuth20AccessDeniedException("security.oauth20.request.denied");
            e.setHttpStatusCode(HttpServletResponse.SC_FORBIDDEN);
            AttributeList attrs = new AttributeList();
            String value = request.getParameter(OAuth20Constants.RESPONSE_TYPE);
            if (value != null && value.length() > 0) {
                attrs.setAttribute(OAuth20Constants.RESPONSE_TYPE, OAuth20Constants.RESPONSE_TYPE, new String[] { value });
            }
            value = getClientId(request);
            if (value != null && value.length() > 0) {
                attrs.setAttribute(OAuth20Constants.CLIENT_ID, OAuth20Constants.CLIENT_ID, new String[] { value });
            }
            result = new OAuthResultImpl(OAuthResult.STATUS_FAILED, attrs, e);
        }
        return result;
    }

    /**
     * @param provider
     * @param responseJSON
     * @param accessToken
     * @param groupsOnly
     * @throws IOException
     */
    protected Map<String, Object> getUserClaimsMap(UserClaims userClaims, boolean groupsOnly) throws IOException {
        // keep this method for OidcEndpointServices
        return TokenIntrospect.getUserClaimsMap(userClaims, groupsOnly);
    }

    protected UserClaims getUserClaimsObj(OAuth20Provider provider, OAuth20Token accessToken) throws IOException {
        // keep this method for OidcEndpointServices
        return TokenIntrospect.getUserClaimsObj(provider, accessToken);
    }
}
