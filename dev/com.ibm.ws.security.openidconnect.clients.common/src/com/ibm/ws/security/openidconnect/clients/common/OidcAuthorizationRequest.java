/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.security.common.web.JavaScriptUtils;
import com.ibm.ws.security.common.web.WebSSOUtils;
import com.ibm.ws.security.openidconnect.pkce.ProofKeyForCodeExchangeHelper;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.PostParameterHelper;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

import io.openliberty.security.oidcclientcore.authentication.AuthorizationRequest;
import io.openliberty.security.oidcclientcore.authentication.AuthorizationRequestParameters;
import io.openliberty.security.oidcclientcore.exceptions.OidcUrlNotHttpsException;
import io.openliberty.security.oidcclientcore.storage.CookieBasedStorage;
import io.openliberty.security.oidcclientcore.storage.CookieStorageProperties;
import io.openliberty.security.oidcclientcore.storage.OidcStorageUtils;
import io.openliberty.security.oidcclientcore.storage.StorageProperties;

public class OidcAuthorizationRequest extends AuthorizationRequest {

    public static final TraceComponent tc = Tr.register(OidcAuthorizationRequest.class);

    ConvergedClientConfig clientConfig;
    WebSSOUtils webSsoUtils = new WebSSOUtils();

    public OidcAuthorizationRequest(HttpServletRequest request, HttpServletResponse response, ConvergedClientConfig clientConfig) {
        super(request, response, clientConfig.getClientId());
        this.clientConfig = clientConfig;
        this.storage = new CookieBasedStorage(request, response);
    }

    @Override
    @FFDCIgnore({ OidcUrlNotHttpsException.class, Exception.class })
    public ProviderAuthenticationResult sendRequest() {
        try {
            return super.sendRequest();
        } catch (OidcUrlNotHttpsException e) {
            Tr.error(tc, "OIDC_CLIENT_URL_PROTOCOL_NOT_HTTPS", e.getUrl());
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        } catch (Exception e) {
            Tr.error(tc, "ERROR_SENDING_AUTHORIZATION_REQUEST", clientId, e.getMessage());
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @Override
    protected String getAuthorizationEndpoint() throws OidcUrlNotHttpsException {
        String authorizationEndpoint = clientConfig.getAuthorizationEndpointUrl();
        if (!OIDCClientAuthenticatorUtil.checkHttpsRequirement(clientConfig, authorizationEndpoint)) {
            throw new OidcUrlNotHttpsException(authorizationEndpoint, clientId);
        }
        return authorizationEndpoint;
    }

    @Override
    protected String getRedirectUrl() throws OidcUrlNotHttpsException {
        String redirectUrl = OIDCClientAuthenticatorUtil.setRedirectUrlIfNotDefined(request, clientConfig);
        if (!OIDCClientAuthenticatorUtil.checkHttpsRequirement(clientConfig, redirectUrl)) {
            throw new OidcUrlNotHttpsException(redirectUrl, clientId);
        }
        return redirectUrl;
    }

    @Override
    protected boolean shouldCreateSession() {
        return clientConfig.createSession();
    }

    @Override
    protected String createStateValueForStorage(String state) {
        return OidcStorageUtils.createStateStorageValue(state, clientConfig.getClientSecret());
    }

    @Override
    protected String createNonceValueForStorage(String nonce, String state) {
        return OidcStorageUtils.createNonceStorageValue(nonce, state, clientConfig.getClientSecret());
    }

    @Override
    protected StorageProperties getStateStorageProperties() {
        CookieStorageProperties props = new CookieStorageProperties();
        props.setStorageLifetimeSeconds((int) clientConfig.getAuthenticationTimeLimitInSeconds());
        if (shouldCookiesBeSecure()) {
            props.setSecure(true);
        }
        return props;
    }

    @Override
    protected StorageProperties getNonceStorageProperties() {
        CookieStorageProperties props = new CookieStorageProperties();
        return props;
    }

    @Override
    protected StorageProperties getOriginalRequestUrlStorageProperties() {
        CookieStorageProperties props = new CookieStorageProperties();
        props.setStorageLifetimeSeconds((int) clientConfig.getAuthenticationTimeLimitInSeconds());
        if (shouldCookiesBeSecure()) {
            props.setSecure(true);
        }
        return props;
    }

    private boolean shouldCookiesBeSecure() {
        boolean isHttpsRequest = request.getScheme().toLowerCase().contains("https");
        return (clientConfig.isHttpsRequired() && isHttpsRequest);
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
                String domain = webSsoUtils.getSsoDomain(request);
                doClientSideRedirect(authzEndPointUrlWithQuery, state, domain);
            } else {
                storeOriginalRequestUrl(state);
            }

        } catch (UnsupportedEncodingException e) {
            Tr.error(tc, "OIDC_CLIENT_AUTHORIZE_ERR", new Object[] { clientId, e.getLocalizedMessage(), ClientConstants.CHARSET });
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        } catch (IOException ioe) {
            Tr.error(tc, "OIDC_CLIENT_AUTHORIZE_ERR", new Object[] { clientId, ioe.getLocalizedMessage(), ClientConstants.CHARSET });
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);

        }
        return new ProviderAuthenticationResult(AuthResult.REDIRECT_TO_PROVIDER, HttpServletResponse.SC_OK, null, null, null, authzEndPointUrlWithQuery);
    }

    ProviderAuthenticationResult checkIfOpenIdScopeIsMissing() {
        boolean openidScopeMissing = !clientConfig.isSocial() && !isOpenIDScopeSpecified(); // some social media use nonstandard scope
        boolean scopeMissing = clientConfig.getScope() == null || clientConfig.getScope().length() == 0;
        if (openidScopeMissing || scopeMissing) {
            Tr.error(tc, "OIDC_CLIENT_REQUEST_MISSING_OPENID_SCOPE",
                    clientId, clientConfig.getScope()); // CWWKS1713E
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
        String clientIdParam = clientId == null ? "" : clientId;

        AuthorizationRequestParameters authzParameters = new AuthorizationRequestParameters(clientConfig.getAuthorizationEndpointUrl(), clientConfig.getScope(), strResponse_type, clientIdParam, redirect_url, state);

        addOptionalParameters(authzParameters, oidcClientRequest, state, acr_values, isImplicit);

        return authzParameters.buildRequestUrl();
    }

    void addOptionalParameters(AuthorizationRequestParameters authzParameters, OidcClientRequest oidcClientRequest, String state, String acr_values, boolean isImplicit) throws UnsupportedEncodingException {
        if (clientConfig.isNonceEnabled() || isImplicit) {
            String nonceValue = OidcUtil.generateRandom(Constants.STATE_LENGTH);
            storeNonceValue(nonceValue, state);
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

        String pkceCodeChallengeMethod = clientConfig.getPkceCodeChallengeMethod();
        if (pkceCodeChallengeMethod != null && !ClientConstants.PKCE_CODE_CHALLENGE_DISABLED.equals(pkceCodeChallengeMethod)) {
            addPkceParameters(pkceCodeChallengeMethod, state, authzParameters);
        }

        if (isImplicit) {
            addImplicitParameters(authzParameters);
        }
        String resources = getResourcesParameter();
        if (resources != null) {          
            authzParameters.addParameter("resource", resources);
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

    void addPkceParameters(String codeChallengeMethod, String state, AuthorizationRequestParameters authzParameters) {
        ProofKeyForCodeExchangeHelper pkceHelper = new ProofKeyForCodeExchangeHelper();
        pkceHelper.generateAndAddPkceParametersToAuthzRequest(codeChallengeMethod, state, authzParameters);
    }

    void addImplicitParameters(AuthorizationRequestParameters authzParameters) throws UnsupportedEncodingException {
        authzParameters.addParameter("response_mode", "form_post");
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

        String cookieName = OidcStorageUtils.getOriginalReqUrlStorageKey(state);
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

}
