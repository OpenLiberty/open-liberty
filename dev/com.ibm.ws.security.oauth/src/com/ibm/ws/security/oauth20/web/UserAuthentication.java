/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.Subject;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.JsonArray;
import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20AccessDeniedException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.oauth.core.api.oauth20.client.OAuth20Client;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.OAuthResultImpl;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.krb5.SpnegoUtil;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.error.impl.OAuth20AuthorizeRequestExceptionHandler;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.util.TemplateRetriever;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebAuthenticator;
import com.ibm.ws.webcontainer.security.WebAuthenticatorFactory;
import com.ibm.ws.webcontainer.security.WebAuthenticatorProxy;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorProxy;
import com.ibm.ws.webcontainer.security.internal.CertificateLoginAuthenticator;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

public class UserAuthentication {
    private static TraceComponent tc = Tr.register(UserAuthentication.class,
            TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);

    public static final String KEY_SECURITY_SERVICE = "securityService";
    protected AtomicServiceReference<SecurityService> securityServiceRef = new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);

    private static final Pattern FORWARD_TEMPLATE_PATTERN = Pattern.compile("\\{(/[\\w-/]+)\\}(/.+)");
    private static final String BASIC_AUTH_HEADER_NAME = "Authorization";

    public static final String PARAM_AUTHZ_FORM_TEMPLATE = Constants.PARAM_AUTHZ_FORM_TEMPLATE;
    public static final String PARAM_AUTHZ_LOGIN_URL = Constants.PARAM_AUTHZ_LOGIN_URL;
    public static final String PARAM_AUTHZ_ERROR_TEMPLATE = Constants.PARAM_AUTHZ_ERROR_TEMPLATE;

    private static final String ATTR_OAUTH_RESULT = "oauthResult";
    private final SubjectManager subjectManager = new SubjectManager();
    private final SubjectHelper subjectHelper = new SubjectHelper();
    private ServletContext servletContext = null;
    private final AuthenticationResult SPNEGO_CONT = new AuthenticationResult(AuthResult.CONTINUE, "OIDC -> SPNEGO said continue...");
    private final SpnegoUtil spnegoUtil = new SpnegoUtil();

    /**
     * @param provider
     * @param request
     * @param response
     * @param result
     * @param prompt
     * @return
     * @throws IOException
     */
    public OAuthResult handleAuthentication(OAuth20Provider provider, HttpServletRequest request,
            HttpServletResponse response, Prompt prompt,
            AtomicServiceReference<SecurityService> securityServiceRef,
            ServletContext servletContext, String requiredRole) throws IOException, ServletException {
        this.securityServiceRef = securityServiceRef;
        this.servletContext = servletContext;
        boolean loggedout = false;

        AuthType authnType = hasAuthenticationData(request);
        if (authnType != AuthType.NONE) {
            if (prompt.hasLogin()) {
                LogoutIfRequired(request);
                loggedout = true;
            }
            AuthenticationResult authResult = loginWithAuthenticationData(request, response, authnType, provider);
            if (authResult.getStatus() == AuthResult.SUCCESS) {
                postAuthentication(request, authResult.getSubject());
                if (!request.isUserInRole(requiredRole)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return new OAuthResultImpl(OAuthResult.STATUS_FAILED, null);
                }
                return new OAuthResultImpl(OAuthResult.STATUS_OK, null);
            } else if (authResult.getStatus() == AuthResult.FAILURE || // client certificate authentication
                    authResult.getStatus() == AuthResult.SEND_401) { // Authorization head: Basic
                if (authnType == AuthType.CERT || provider.isCertAuthentication()) {
                    Tr.error(tc, "OAUATH_CLIENT_CERT_AUTH_FAIL", new Object[] {});
                } else {
                    Tr.error(tc, "OAUATH_BASIC_AUTH_FAIL", new Object[] {});
                }
                response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
                // do not do user login but return login_required error
                return prompt.errorLoginRequired();
            }
        }
        if (prompt.hasNone()) {
            return prompt.errorLoginRequired();
        } else if (prompt.hasLogin()) {
            if (!loggedout) {
                LogoutIfRequired(request);
            }
        }
        sendForLogin(provider, request, response);
        return null;
    }

    /**
     * @param provider
     * @param request
     * @param response
     * @param result
     * @param prompt
     * @param oauthResult
     * @return
     * @throws IOException
     */
    public OAuthResult handleAuthenticationWithOAuthResult(OAuth20Provider provider, HttpServletRequest request,
            HttpServletResponse response, Prompt prompt,
            AtomicServiceReference<SecurityService> securityServiceRef,
            ServletContext servletContext, String requiredRole, OAuthResult result) throws IOException, ServletException {
        this.securityServiceRef = securityServiceRef;
        this.servletContext = servletContext;
        boolean loggedout = false;
        AuthenticationResult authResult = null;
        AttributeList al = getAttributeList(result);

        AuthType authnType = hasAuthenticationData(request);
        if (authnType != AuthType.NONE) {
            if (prompt.hasLogin()) {
                LogoutIfRequired(request);
                loggedout = true;
            }
            authResult = loginWithAuthenticationData(request, response, authnType, provider);

            if (authResult.getStatus() == AuthResult.SUCCESS) {
                return processPostAuthentication(request, response, requiredRole, al, authResult);
            } else if (authResult.getStatus() == AuthResult.FAILURE || authResult.getStatus() == AuthResult.SEND_401) {
                setAuthTypeAndPrintError(provider, al, authResult, authnType);
                boolean bOAuth = request.getAttribute(OAuth20Constants.OIDC_REQUEST_OBJECT_ATTR_NAME) == null;
                if (bOAuth || !prompt.hasNone()) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return new OAuthResultImpl(OAuthResult.STATUS_FAILED, al,
                            new OAuth20AccessDeniedException("security.oauth20.error.access.denied"));
                }
            }
        }

        authResult = processSpnego(request, response, provider);

        if (authResult.getStatus() == AuthResult.TAI_CHALLENGE) {
            return new OAuthResultImpl(OAuthResult.TAI_CHALLENGE, al);
        } else if (authResult.getStatus() == AuthResult.SUCCESS) {
            return processPostAuthentication(request, response, requiredRole, al, authResult);
        }

        if (prompt.hasNone()) {
            return prompt.errorLoginRequired(al);
        } else if (prompt.hasLogin()) {
            if (!loggedout) {
                LogoutIfRequired(request);
            }
        }
        sendForLogin(provider, request, response);
        return null;
    }

    /**
     * @param result
     * @return
     */
    private AttributeList getAttributeList(OAuthResult result) {
        AttributeList al;
        if (result != null) {
            al = result.getAttributeList();
        } else {
            al = new AttributeList();
        }
        return al;
    }

    /**
     * @param provider
     * @param al
     * @param authResult
     * @param authnType
     */
    private void setAuthTypeAndPrintError(OAuth20Provider provider, AttributeList al, AuthenticationResult authResult, AuthType authnType) {
        if (authnType == AuthType.CERT || provider.isCertAuthentication()) {
            if (al != null) {
                al.setAttribute(Constants.WWW_AUTHENTICATE, Constants.WWW_AUTHENTICATE, new String[] { "Client Certificate Authentication" });
            }
            Tr.error(tc, "OAUATH_CLIENT_CERT_AUTH_FAIL", authResult.getReason());
        } else {
            if (al != null) {
                al.setAttribute(Constants.WWW_AUTHENTICATE, Constants.WWW_AUTHENTICATE, new String[] { "Basic" });
            }
            Tr.error(tc, "OAUATH_BASIC_AUTH_FAIL", new Object[] {});
        }
    }

    /**
     * @param request
     * @param response
     * @param requiredRole
     * @param al
     * @param authResult
     * @return
     * @throws ServletException
     * @throws IOException
     */
    private OAuthResult processPostAuthentication(HttpServletRequest request, HttpServletResponse response, String requiredRole, AttributeList al, AuthenticationResult authResult) throws ServletException, IOException {
        if (al != null) {
            try {
                validateIdTokenHint(authResult.getSubject().getPrincipals(WSPrincipal.class).iterator().next().getName(), al);
            } catch (OAuth20Exception oe) {
                if (OIDCConstants.ERROR_LOGIN_REQUIRED.equals(oe.getError())) {
                    request.logout();
                }
                return new OAuthResultImpl(OAuthResult.STATUS_FAILED, al, oe);
            }
        }

        postAuthentication(request, authResult.getSubject());
        if (!request.isUserInRole(requiredRole)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return new OAuthResultImpl(OAuthResult.STATUS_FAILED, al);
        }

        return new OAuthResultImpl(OAuthResult.STATUS_OK, al);
    }

    private AuthenticationResult processSpnego(HttpServletRequest request, HttpServletResponse response, OAuth20Provider provider) {
        AuthenticationResult authResult = SPNEGO_CONT;

        if (!provider.isAllowSpnegoAuthentication()) {
            return authResult;
        }
        java.util.HashMap<String, Object> props = new java.util.HashMap<String, Object>();
        props.put("authType", "com.ibm.ws.security.spnego");
        WebAuthenticatorFactory webAuthenticatorFactory = WebAppSecurityCollaboratorImpl.getWebAuthenticatorFactory();
        if (webAuthenticatorFactory != null) {
            try {
                WebProviderAuthenticatorProxy webProviderAuthenticatorProxy = webAuthenticatorFactory.getWebProviderAuthenticatorProxy();
                if (webProviderAuthenticatorProxy != null) {
                    authResult = webProviderAuthenticatorProxy.authenticate(request, response, props);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return authResult;
    }

    public void handleBasicAuthenticationWithRequiredRole(OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response,
            AtomicServiceReference<SecurityService> securityServiceRef,
            ServletContext servletContext, String requiredRole) throws OidcServerException {
        handleBasicAuthenticationWithRequiredRole(provider, request, response, securityServiceRef, servletContext, requiredRole, true);
    }

    public void handleBasicAuthenticationWithRequiredRole(OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response,
            AtomicServiceReference<SecurityService> securityServiceRef,
            ServletContext servletContext, String requiredRole, boolean fallbackToBasicAuth) throws OidcServerException {
        this.securityServiceRef = securityServiceRef;
        this.servletContext = servletContext;

        String description = "The user is not authenticated, or is not in the role that is required to complete this request";

        if (request.getUserPrincipal() == null) {
            AuthType authnType = hasAuthenticationData(request);
            if (authnType == AuthType.NONE) { // needs to be AUthType.BASIC or AuthType.CERT
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Request does not contain Basic Authentication header or Client Certificate");
                }
                throw new OidcServerException(description, OIDCConstants.ERROR_ACCESS_DENIED, HttpServletResponse.SC_UNAUTHORIZED);
            }

            if (fallbackToBasicAuth) {
                AuthenticationResult authResult = loginWithAuthenticationData(request, response, authnType, provider);
                if (authResult.getStatus() != AuthResult.SUCCESS) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Authentication failure...");
                    }
                    throw new OidcServerException(description, OIDCConstants.ERROR_ACCESS_DENIED, HttpServletResponse.SC_UNAUTHORIZED);
                }
                postAuthentication(request, authResult.getSubject());
            } else {
                throw new OidcServerException(description, OIDCConstants.ERROR_ACCESS_DENIED, HttpServletResponse.SC_UNAUTHORIZED);
            }
        }
        if (requiredRole != null) {
            boolean isClientAdminCheck = requiredRole.equals(RegistrationEndpointServices.ROLE_REQUIRED);

            if (!(request.isUserInRole(requiredRole) || (isClientAdminCheck && isUserProviderClientAdmin(provider, request.getUserPrincipal())))) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, description);
                }
                throw new OidcServerException(description, OIDCConstants.ERROR_ACCESS_DENIED, HttpServletResponse.SC_FORBIDDEN);
            }
        }
    }

    /**
     * @param authResult
     */
    private void postAuthentication(HttpServletRequest request, Subject subject) {
        // delete session if it's owned by different user.
        Subject current = subjectManager.getInvocationSubject();
        if (current != null && !subjectHelper.isUnauthenticated(current) && !subject.equals(current)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "invalidating existing HTTP Session");
                }
                session.invalidate();
            }
        }
        subjectManager.setInvocationSubject(subject);
        subjectManager.setCallerSubject(subject);
    }

    /**
     * @param provider
     * @param principal
     * @return
     */
    private boolean isUserProviderClientAdmin(OAuth20Provider provider, Principal principal) {
        String clientManager = provider == null ? null : provider.getClientAdmin();
        String userName = principal == null ? null : principal.getName();
        if (userName == null || clientManager == null) {
            return false;
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Provider clientManager:" + clientManager + " userName:" + userName);
        }
        return clientManager.equals(userName);
    }

    public AuthType hasAuthenticationData(HttpServletRequest request) {
        String authzHeader = request.getHeader(BASIC_AUTH_HEADER_NAME);
        if (authzHeader != null) {
            if (spnegoUtil.isSpnegoOrKrb5Token(authzHeader)) {
                return AuthType.SPNEGO;
            } else {
                return AuthType.BASIC;
            }
        }

        X509Certificate certChain[] = (X509Certificate[]) request.getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
        if (certChain != null && certChain.length > 0) {
            return AuthType.CERT;
        }

        return AuthType.NONE;
    }

    private AuthenticationResult loginWithAuthenticationData(HttpServletRequest request,
            HttpServletResponse response,
            AuthType authType,
            OAuth20Provider provider) {
        AuthenticationResult authResult = null;
        WebAuthenticator authenticator = null;

        WebAppSecurityConfig webAppSecurityConfig = WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig();
        WebAuthenticatorProxy authenticatorProxy = webAppSecurityConfig.createWebAuthenticatorProxy();

        // WebAuthenticatorFactory webAuthenticatorFactory = WebAppSecurityCollaboratorImpl.getWebAuthenticatorFactory();
        // WebAuthenticatorProxy authenticatorProxy = null;
        // if (webAuthenticatorFactory != null && webAuthenticatorFactory instanceof WebAuthenticatorFactoryExtended) {
        // authenticatorProxy = webAuthenticatorFactory.getWebAuthenticatorProxy();
        // }

        if (provider.isCertAuthentication()) {
            if (authType == AuthType.CERT) {
                authenticator = authenticatorProxy.createCertificateLoginAuthenticator();
            } else {
                String message = Tr.formatMessage(tc, "OAUATH_CERT_AUTH_WITH_NO_CERT");
                Tr.error(tc, "OAUATH_CERT_AUTH_WITH_NO_CERT", new Object[] {});
                return new AuthenticationResult(AuthResult.FAILURE, message);
            }
        } else {
            switch (authType) {
            case BASIC:
                authenticator = authenticatorProxy.getBasicAuthAuthenticator();
                // authResult = authenticator.authenticate(request, response, null);
                break;
            case CERT:
                if (provider.isAllowCertAuthentication()) {
                    authenticator = authenticatorProxy.createCertificateLoginAuthenticator();
                    // authResult = authenticator.authenticate(request, response, null);
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "The login of the request failed because the request has authType CERT but allowCertAuthentication is false.");
                    }
                    return new AuthenticationResult(AuthResult.CONTINUE, "The login of the request failed because the request has authType CERT but allowCertAuthentication is false.");
                }

                break;
            case SPNEGO:
                authResult = processSpnego(request, response, provider);
                break;
            default:
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The login of the request failed because the request has an unknown authentication type.");
                }
                return new AuthenticationResult(AuthResult.CONTINUE, "The login of the request failed because the request has an unknown authentication type.");
            }
        }

        if (authType == AuthType.BASIC || authType == AuthType.CERT) {
            try {
                authResult = authenticator.authenticate(request, response, null);
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The direct login of http request failed because of exception: " + e);
                }
                authResult = new AuthenticationResult(AuthResult.FAILURE, e.getMessage());
            }
        }

        return authResult;
    }

    /**
     * Create an instance of the BasicAuthAuthenticator.
     * <p>
     * Protected so it can be overridden in unit tests.
     */
    public void renderErrorPage(OAuth20Provider provider, HttpServletRequest request,
            HttpServletResponse response, OAuthResult result) throws ServletException, IOException {
        String templateUrl = provider.getAuthorizationErrorTemplate();

        Matcher m = FORWARD_TEMPLATE_PATTERN.matcher(templateUrl);
        if (m.matches()) {
            String contextPath = m.group(1);
            String path = m.group(2);
            request.setAttribute(ATTR_OAUTH_RESULT, result);
            RequestDispatcher dispatcher = getDispatcher(contextPath, path);
            if (dispatcher != null) {
                dispatcher.forward(request, response);
            } else {
                Tr.error(tc, "security.oauth20.endpoint.template.forward.error",
                        new Object[] { PARAM_AUTHZ_ERROR_TEMPLATE, contextPath, path });
            }
        } else {
            templateUrl = TemplateRetriever.normallizeTemplateUrl(request,
                    templateUrl);
            AttributeList attrs = result.getAttributeList();
            String responseType = attrs.getAttributeValueByName(OAuth20Constants.RESPONSE_TYPE);
            String redirectUri = attrs.getAttributeValueByName(OAuth20Constants.REDIRECT_URI);
            if (redirectUri == null || redirectUri.length() == 0) {
                redirectUri = getRegisteredRedirectUri(provider, result.getAttributeList());
            }
            OAuth20AuthorizeRequestExceptionHandler handler = new OAuth20AuthorizeRequestExceptionHandler(
                    responseType, redirectUri, templateUrl);
            handler.handleResultException(request, response, result);
        }
    }

    private void sendForLogin(OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String loginURL = provider.getCustomLoginURL(); // default is "login.jsp" already

        Integer realPort = null;
        if (request.getScheme().toLowerCase().contains("https")) {
            realPort = new com.ibm.ws.security.common.web.WebUtils().getRedirectPortFromRequest(request);
        }
        int port = request.getServerPort();
        // if we are behind proxy, might need to rewrite url's to get the port number right.
        boolean rewriteURLs = false;
        if (realPort != null && realPort.intValue() != port) {
            rewriteURLs = true;
        }

        boolean isAbsoluteLoginURL = loginURL.toLowerCase().startsWith("http://") || loginURL.toLowerCase().startsWith("https://");
        String requestURI = request.getRequestURI();
        if (request.getQueryString() != null) {
            requestURI = requestURI + "?" + request.getQueryString();
        }

        if (!loginURL.startsWith("/") && !loginURL.toLowerCase().startsWith("http://")
                && !loginURL.toLowerCase().startsWith("https://")) {
            loginURL = request.getContextPath() + "/" + loginURL; // i.e. oidc/login.jsp
        }

        if (rewriteURLs) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "rewriting urls and changing port from " + port + " to: " + realPort);
            }
            HttpServletRequest r = request;
            if (!isAbsoluteLoginURL) {
                // convert to absolute URL otherwise base security will attempt to do that
                // and will hit same "wrong port" problem being addressed here.
                String buf = r.getScheme() + "://" + r.getServerName() + ":" + realPort;
                if (!loginURL.startsWith("/")) {
                    buf = buf + ("/");
                }
                loginURL = buf + loginURL;

            }

            requestURI = r.getScheme() + "://" + r.getServerName() + ":" + realPort + requestURI;
        }
        ReferrerURLCookieHandler handler = WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig().createReferrerURLCookieHandler();
        Cookie c = handler.createCookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, requestURI, request);
        if (provider.isHttpsRequired()) {
            c.setSecure(true);
        }
        response.addCookie(c);

        c = handler.createCookie(ReferrerURLCookieHandler.CUSTOM_RELOGIN_URL_COOKIENAME, loginURL, request);
        if (provider.isHttpsRequired()) {
            c.setSecure(true);
        }
        response.addCookie(c);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "_SSO OP redirecting to login page [" + loginURL + "]");
        }
        request.getSession(true).setAttribute(Constants.ATTR_AFTERLOGIN, Boolean.TRUE);
        response.sendRedirect(loginURL);
    }

    private RequestDispatcher getDispatcher(String contextPath, String path) {
        RequestDispatcher retVal = null;
        ServletContext ctx = servletContext.getContext(contextPath);
        if (ctx != null) {
            retVal = ctx.getRequestDispatcher(path);
        }
        return retVal;
    }

    private void LogoutIfRequired(HttpServletRequest request) throws ServletException {
        if (request.getUserPrincipal() != null) {
            request.logout();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "login is set as a prompt parameter, thus the current user is forced to be logged out.");
            }
        }
    }

    /**
     * get the one registered redirect uri.
     * return null if there are none, or more than one, or if the only one is a regexp uri.
     * @param provider
     * @param attrs
     * @return
     */
    @FFDCIgnore(com.ibm.oauth.core.api.error.OidcServerException.class)
    protected String getRegisteredRedirectUri(OAuth20Provider provider, AttributeList attrs) {
        String uri = null;
        String clientId = attrs.getAttributeValueByName(OAuth20Constants.CLIENT_ID);
        if (clientId != null && clientId.length() != 0) {
            OAuth20Client client = null;
            try {
                OidcOAuth20ClientProvider clientProvider = provider.getClientProvider();
                if (clientProvider != null) {
                    client = clientProvider.get(clientId);
                }
            } catch (OidcServerException e) {
                // this should not happen.
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception is caught" + e);
                }
            }
            if (client != null) {
                JsonArray redirectUris = client.getRedirectUris();
                if (redirectUris != null && redirectUris.size() == 1) {
                    uri = redirectUris.get(0).getAsString();
                    if (uri.startsWith(OIDCConstants.REGEXP_PREFIX)) {
                        uri = null;
                    }
                }
            }
        }
        return uri;
    }

    /**
     * Validates that, if present, the id_token_hint_status indicates success, and that the id_token_hint_username and
     * id_token_hint_clientid attributes match the provided username and the client_id attribute in the provided attribute
     * list, respectively.
     *
     * @param username
     * @param attrs
     * @return
     * @throws OAuth20Exception
     */
    boolean validateIdTokenHint(String username, AttributeList attrs) throws OAuth20Exception {
        if (username != null && attrs != null) {
            String status = attrs.getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS);
            if (status != null) {
                if (OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS_SUCCESS.equals(status)) {
                    String hint_username = attrs.getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_USERNAME);
                    String hint_clientId = attrs.getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_CLIENTID);
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "username in id_token_hint : " + hint_username + " current username : " + username);
                        Tr.debug(tc, "clientid in id_token_hint : " + hint_clientId + " current clientid : " + attrs.getAttributeValueByName(OAuth20Constants.CLIENT_ID));
                    }

                    if (((hint_username != null) && !hint_username.equals(username)) || ((hint_clientId != null) && !hint_clientId.equals(attrs.getAttributeValueByName(OAuth20Constants.CLIENT_ID)))) {
                        throw new OAuth20Exception(OIDCConstants.ERROR_LOGIN_REQUIRED, OIDCConstants.MESSAGE_LOGIN_REQUIRED_ID_TOKEN_HINT_MISMATCH, null);
                    }
                } else if (OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS_FAIL_INVALID_ID_TOKEN.equals(status)) {
                    throw new OAuth20Exception(OAuth20Exception.INVALID_REQUEST, OIDCConstants.MESSAGE_LOGIN_REQUIRED_ID_TOKEN_HINT_INVALID, null);
                }
            }
        }
        return true;
    }
}
