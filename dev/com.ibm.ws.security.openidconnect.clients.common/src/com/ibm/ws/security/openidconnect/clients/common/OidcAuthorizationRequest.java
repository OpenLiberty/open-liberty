/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.web.JavaScriptUtils;
import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.PostParameterHelper;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

import io.openliberty.security.oidcclientcore.authentication.AuthorizationRequest;
import io.openliberty.security.oidcclientcore.authentication.AuthorizationRequestParameters;
import io.openliberty.security.oidcclientcore.exceptions.OidcUrlNotHttpsException;
import io.openliberty.security.oidcclientcore.utils.Utils;

public class OidcAuthorizationRequest extends AuthorizationRequest {

    public static final TraceComponent tc = Tr.register(OidcAuthorizationRequest.class);

    ConvergedClientConfig clientConfig;

    public OidcAuthorizationRequest(HttpServletRequest request, HttpServletResponse response, ConvergedClientConfig clientConfig) {
        super(request, response);
        this.clientConfig = clientConfig;
    }

    @Override
    @FFDCIgnore(OidcUrlNotHttpsException.class)
    public ProviderAuthenticationResult sendRequest() {
        try {
            return super.sendRequest();
        } catch (OidcUrlNotHttpsException e) {
            Tr.error(tc, "OIDC_CLIENT_URL_PROTOCOL_NOT_HTTPS", e.getUrl());
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @Override
    protected String getAuthorizationEndpoint() throws OidcUrlNotHttpsException {
        String authorizationEndpoint = clientConfig.getAuthorizationEndpointUrl();
        if (!OIDCClientAuthenticatorUtil.checkHttpsRequirement(clientConfig, authorizationEndpoint)) {
            throw new OidcUrlNotHttpsException(authorizationEndpoint);
        }
        return authorizationEndpoint;
    }

    @Override
    protected String getRedirectUrl() throws OidcUrlNotHttpsException {
        String redirectUrl = OIDCClientAuthenticatorUtil.setRedirectUrlIfNotDefined(request, clientConfig);
        if (!OIDCClientAuthenticatorUtil.checkHttpsRequirement(clientConfig, redirectUrl)) {
            throw new OidcUrlNotHttpsException(redirectUrl);
        }
        return redirectUrl;
    }

    @Override
    protected boolean shouldCreateSession() {
        return clientConfig.createSession();
    }

    @Override
    protected void storeStateValue(String state) {
        cookieUtils.createAndAddStateCookie(state, clientConfig.getClientSecret(), (int) clientConfig.getAuthenticationTimeLimitInSeconds(), clientConfig.isHttpsRequired());
    }

    void createAndAddWasReqUrlCookie(String state) {
        String urlCookieName = ClientConstants.WAS_REQ_URL_OIDC + Utils.getStrHashCode(state);
        String cookieValue = getReqURL();
        cookieUtils.createAndAddCookie(urlCookieName, cookieValue, (int) clientConfig.getAuthenticationTimeLimitInSeconds(), clientConfig.isHttpsRequired());
    }

    @Override
    protected ProviderAuthenticationResult redirectToAuthorizationEndpoint(String state, String redirectUrl) {
        String authzEndPointUrlWithQuery = null;
        try {
            ProviderAuthenticationResult result = checkIfOpenIdScopeIsMissing();
            if (result != null) {
                return result;
            }

            String acr_values = request.getParameter("acr_values");

            authzEndPointUrlWithQuery = buildAuthorizationUrlWithQuery((OidcClientRequest) request.getAttribute(ClientConstants.ATTRIB_OIDC_CLIENT_REQUEST), state, redirectUrl, acr_values);

            savePostParameters();

            // Redirect to OP
            // If clientSideRedirect is true (default is true) then do the
            // redirect.  If the user agent doesn't support javascript then config can set this to false.
            if (clientConfig.isClientSideRedirect()) {
                String domain = OidcClientUtil.getSsoDomain(request);
                doClientSideRedirect(authzEndPointUrlWithQuery, state, domain);
            } else {
                createAndAddWasReqUrlCookie(state);
            }

        } catch (UnsupportedEncodingException e) {
            Tr.error(tc, "OIDC_CLIENT_AUTHORIZE_ERR", new Object[] { clientConfig.getClientId(), e.getLocalizedMessage(), ClientConstants.CHARSET });
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        } catch (IOException ioe) {
            Tr.error(tc, "OIDC_CLIENT_AUTHORIZE_ERR", new Object[] { clientConfig.getClientId(), ioe.getLocalizedMessage(), ClientConstants.CHARSET });
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);

        }
        return new ProviderAuthenticationResult(AuthResult.REDIRECT_TO_PROVIDER, HttpServletResponse.SC_OK, null, null, null, authzEndPointUrlWithQuery);
    }

    ProviderAuthenticationResult checkIfOpenIdScopeIsMissing() {
        boolean openidScopeMissing = !clientConfig.isSocial() && !isOpenIDScopeSpecified(); // some social media use nonstandard scope
        boolean scopeMissing = clientConfig.getScope() == null || clientConfig.getScope().length() == 0;
        if (openidScopeMissing || scopeMissing) {
            Tr.error(tc, "OIDC_CLIENT_REQUEST_MISSING_OPENID_SCOPE",
                    clientConfig.getClientId(), clientConfig.getScope()); // CWWKS1713E
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }
        return null;
    }

    private boolean isOpenIDScopeSpecified() {
        String scope = null;
        scope = clientConfig.getScope();
        if (scope.contains("openid")) {
            return true;
        }
        return false;
    }

    String buildAuthorizationUrlWithQuery(OidcClientRequest oidcClientRequest, String state, String redirect_url, String acr_values) throws UnsupportedEncodingException {
        String strResponse_type = Constants.RESPONSE_TYPE_CODE; // default is asking for authorization code
        boolean isImplicit = false;
        if (Constants.IMPLICIT.equals(clientConfig.getGrantType())) {
            // in OidcClientConfigImpl, the grantType and responseType had been clarified. See task 223258
            isImplicit = true;
            strResponse_type = clientConfig.getResponseType();
        }
        String clientId = clientConfig.getClientId() == null ? "" : clientConfig.getClientId();

        AuthorizationRequestParameters authzParameters = new AuthorizationRequestParameters(clientConfig.getAuthorizationEndpointUrl(), clientConfig.getScope(), strResponse_type, clientId, redirect_url, state);

        addOptionalParameters(authzParameters, oidcClientRequest, state, acr_values, isImplicit);

        return authzParameters.buildRequestUrl();
    }

    void addOptionalParameters(AuthorizationRequestParameters authzParameters, OidcClientRequest oidcClientRequest, String state, String acr_values, boolean isImplicit) throws UnsupportedEncodingException {
        if (clientConfig.isNonceEnabled() || isImplicit) {
            String nonceValue = OidcUtil.generateRandom(Constants.STATE_LENGTH);
            OidcUtil.createNonceCookie(oidcClientRequest, nonceValue, state, clientConfig);
            authzParameters.addParameter("nonce", nonceValue);
        }

        if (acr_values != null && !acr_values.isEmpty()) {
            authzParameters.addParameter("acr_values", acr_values);
        } else if (isACRConfigured()) {
            authzParameters.addParameter("acr_values", clientConfig.getAuthContextClassReference());
        }

        if (clientConfig.getPrompt() != null) {
            authzParameters.addParameter("prompt", clientConfig.getPrompt());
        }

        if (isImplicit) {
            addImplicitParameters(authzParameters);
        }
        // look for custom params in the configuration to send to the authorization ep
        addCustomParams(authzParameters);

        // check and see if we have any additional params to forward from the request
        addForwardLoginParams(authzParameters);
    }

    private boolean isACRConfigured() {
        boolean isACR = false;
        String acr_values = null;
        if ((acr_values = clientConfig.getAuthContextClassReference()) != null && !acr_values.isEmpty()) {
            isACR = true;
        }
        return isACR;
    }

    void addImplicitParameters(AuthorizationRequestParameters authzParameters) throws UnsupportedEncodingException {
        authzParameters.addParameter("response_mode", "form_post");
        // add resource
        String resources = getResourcesParameter();
        if (resources != null) {
            authzParameters.addParameter("resource", resources);
        }
    }

    String getResourcesParameter() throws UnsupportedEncodingException {
        String resources = OIDCClientAuthenticatorUtil.getResources(clientConfig);
        if (resources != null && !resources.isEmpty()) {
            return resources;
        }
        return null;
    }

    private void addCustomParams(AuthorizationRequestParameters authzParameters) {
        HashMap<String, String> customParams = clientConfig.getAuthzRequestParams();
        if (customParams != null && !customParams.isEmpty()) {
            Set<Entry<String, String>> entries = customParams.entrySet();
            for (Entry<String, String> entry : entries) {
                authzParameters.addParameter(entry.getKey(), entry.getValue());
            }
        }
    }

    void addForwardLoginParams(AuthorizationRequestParameters authzParameters) {
        List<String> forwardAuthzParams = clientConfig.getForwardLoginParameter();
        if (forwardAuthzParams == null || forwardAuthzParams.isEmpty()) {
            return;
        }
        for (String entry : forwardAuthzParams) {
            if (entry != null) {
                String value = request.getParameter(entry);
                if (value != null) {
                    authzParameters.addParameter(entry, value);
                }
            }
        }
    }

    void savePostParameters() {
        // preserve post param.
        WebAppSecurityConfig webAppSecConfig = WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig();
        PostParameterHelper pph = new PostParameterHelper(webAppSecConfig);
        pph.save(request, response);
    }

    /*
     * A javascript redirect is preferred over a 302 because it will preserve web fragements in the URL,
     * i.e. foo.com/something#fragment.
     */
    private void doClientSideRedirect(String loginURL, String state, String domain) throws IOException {

        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter pw = response.getWriter();
        pw.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
        pw.println("<head>");

        pw.println(createJavaScriptForRedirect(loginURL, state, domain));

        pw.println("<title>Redirect To OP</title> ");
        pw.println("</head>");
        pw.println("<body></body>");
        pw.println("</html>");

        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private, max-age=0");
        // HTTP 1.0.
        response.setHeader("Pragma", "no-cache");
        // Proxies.
        response.setDateHeader("Expires", 0);
        response.setContentType("text/html; charset=UTF-8");

        pw.close();

    }

    private String createJavaScriptForRedirect(String loginURL, String state, String domain) {

        String cookieName = ClientConstants.WAS_REQ_URL_OIDC + Utils.getStrHashCode(state);
        StringBuilder sb = new StringBuilder();

        String strDomain = "";
        if (domain != null && !domain.isEmpty()) {
            strDomain = "domain=" + domain + ";";
        }
        sb.append("<script type=\"text/javascript\" language=\"javascript\">")
                .append("var loc=window.location.href;")
                .append("document.cookie=\"").append(cookieName).append("=\"").append("+loc+").append("\";" + strDomain + " path=/;");

        JavaScriptUtils jsUtils = new JavaScriptUtils();
        String cookieProps = jsUtils.createHtmlCookiePropertiesString(jsUtils.getWebAppSecurityConfigCookieProperties());
        sb.append(cookieProps);
        sb.append("\"</script>");

        sb.append("<script type=\"text/javascript\" language=\"javascript\">")
                .append("window.location.replace(\"" + loginURL + "\")")
                .append("</script>");

        String js = sb.toString();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "createJavaScriptForRedirect returns [" + js + "]");
        }
        return js;
    }

    String getReqURL() {
        // due to some longstanding webcontainer strangeness, we have to do some extra things for certain behind-proxy cases to get the right port.
        boolean rewritePort = false;
        Integer realPort = null;
        if (request.getScheme().toLowerCase().contains("https")) {
            realPort = new com.ibm.ws.security.common.web.WebUtils().getRedirectPortFromRequest(request);
        }
        int port = request.getServerPort();
        if (realPort != null && realPort.intValue() != port) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "serverport = " + port + "real port is " + realPort.toString() + ", url will be rewritten to use real port");
            }
            rewritePort = true;
        }

        StringBuffer requestURL = request.getRequestURL();
        if (rewritePort) {
            requestURL = rewritePortInRequestUrl(realPort);
        }
        requestURL = appendQueryString(requestURL);
        return requestURL.toString();
    }

    StringBuffer rewritePortInRequestUrl(int realPort) {
        StringBuffer requestURL = new StringBuffer();
        requestURL.append(request.getScheme());
        requestURL.append("://");
        requestURL.append(request.getServerName());
        requestURL.append(":");
        requestURL.append(realPort);
        requestURL.append(request.getRequestURI());
        return requestURL;
    }

    StringBuffer appendQueryString(StringBuffer requestURL) {
        String queryString = request.getQueryString();
        if (queryString != null) {
            requestURL.append("?");
            requestURL.append(queryString);
        }
        return requestURL;
    }

}
