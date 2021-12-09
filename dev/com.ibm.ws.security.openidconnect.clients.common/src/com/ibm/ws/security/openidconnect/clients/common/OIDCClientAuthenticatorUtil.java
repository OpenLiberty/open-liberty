/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
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
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.web.JavaScriptUtils;
import com.ibm.ws.security.openidconnect.client.jose4j.util.Jose4jUtil;
import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.CookieHelper;
import com.ibm.ws.webcontainer.security.PostParameterHelper;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.wsspi.ssl.SSLSupport;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

public class OIDCClientAuthenticatorUtil {
    public static final TraceComponent tc = Tr.register(OIDCClientAuthenticatorUtil.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    SSLSupport sslSupport = null;
    private Jose4jUtil jose4jUtil = null;
    private static int badStateCount = 0;
    public static final String[] OIDC_COOKIES = { ClientConstants.WAS_OIDC_STATE_KEY, ClientConstants.WAS_REQ_URL_OIDC,
            ClientConstants.WAS_OIDC_CODE, ClientConstants.WAS_OIDC_NONCE };

    public OIDCClientAuthenticatorUtil() {
    }

    public OIDCClientAuthenticatorUtil(SSLSupport sslspt) {
        sslSupport = sslspt;
        jose4jUtil = getJose4jUtil(sslSupport);
    }

    protected Jose4jUtil getJose4jUtil(SSLSupport sslSupport) {
        return new Jose4jUtil(sslSupport);
    }

    /**
     * This method handle the redirect to the OpenID Connect server with query parameters
     */
    public ProviderAuthenticationResult handleRedirectToServer(HttpServletRequest req, HttpServletResponse res, ConvergedClientConfig clientConfig) {
        String authorizationEndpoint = clientConfig.getAuthorizationEndpointUrl();

        if (!checkHttpsRequirement(clientConfig, authorizationEndpoint)) {
            Tr.error(tc, "OIDC_CLIENT_URL_PROTOCOL_NOT_HTTPS", authorizationEndpoint);
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }

        if (clientConfig.createSession()) {
            try {
                req.getSession(true);
            } catch (Exception e) {
                // ignore it. Session exists
            }
        }

        String strRandom = OidcUtil.generateRandom(OidcUtil.RANDOM_LENGTH);
        String timestamp = OidcUtil.getTimeStamp();
        String state = timestamp + strRandom;
        if (!req.getMethod().equalsIgnoreCase("GET") && req.getParameter("oidc_client") != null) {
            state = state + req.getParameter("oidc_client");
        }
        String cookieValue = HashUtils.createStateCookieValue(clientConfig, state);
        String cookieName = ClientConstants.WAS_OIDC_STATE_KEY + HashUtils.getStrHashCode(state);
        boolean isHttpsRequest = req.getScheme().toLowerCase().contains("https");
        int cookieLifeTime = (int) clientConfig.getAuthenticationTimeLimitInSeconds();
        Cookie cookie = OidcClientUtil.createCookie(cookieName, cookieValue, cookieLifeTime, req);
        if (clientConfig.isHttpsRequired() == true && isHttpsRequest) {
            cookie.setSecure(true);
        }
        res.addCookie(cookie);

        String redirect_url = setRedirectUrlIfNotDefined(req, clientConfig);
        if (!checkHttpsRequirement(clientConfig, redirect_url)) {
            Tr.error(tc, "OIDC_CLIENT_URL_PROTOCOL_NOT_HTTPS", redirect_url);
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }

        String acr_values = req.getParameter("acr_values");

        String authzEndPointUrlWithQuery = null;
        try {
            boolean openidScopeMissing = !clientConfig.isSocial() && !isOpenIDScopeSpecified(clientConfig); // some social media use nonstandard scope
            boolean scopeMissing = clientConfig.getScope() == null || clientConfig.getScope().length() == 0;
            if (openidScopeMissing || scopeMissing) {
                Tr.error(tc, "OIDC_CLIENT_REQUEST_MISSING_OPENID_SCOPE",
                        clientConfig.getClientId(), clientConfig.getScope()); // CWWKS1713E
                return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
            }

            authzEndPointUrlWithQuery = buildAuthorizationUrlWithQuery(req, (OidcClientRequest) req.getAttribute(ClientConstants.ATTRIB_OIDC_CLIENT_REQUEST), state, clientConfig, redirect_url, acr_values);

            // preserve post param.
            WebAppSecurityConfig webAppSecConfig = WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig();
            PostParameterHelper pph = new PostParameterHelper(webAppSecConfig);
            pph.save(req, res);
            // Redirect to OP
            // If clientSideRedirect is true (default is true) then do the
            // redirect.  If the user agent doesn't support javascript then config can set this to false.
            if (clientConfig.isClientSideRedirect()) {
                String domain = OidcClientUtil.getSsoDomain(req);
                doClientSideRedirect(res, authzEndPointUrlWithQuery, state, domain);
            } else {
                String urlCookieName = ClientConstants.WAS_REQ_URL_OIDC + HashUtils.getStrHashCode(state);
                Cookie c = OidcClientUtil.createCookie(urlCookieName, getReqURL(req), cookieLifeTime, req);
                if (clientConfig.isHttpsRequired() == true && isHttpsRequest) {
                    cookie.setSecure(true);
                }
                res.addCookie(c);
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

    /**
     * Perform OpenID Connect client authenticate for the given web request.
     * Return an OidcAuthenticationResult which contains the status and subject
     *
     * A routine flow can come through here twice. First there's no state and it goes to handleRedirectToServer
     *
     * second time, oidcclientimpl.authenticate sends us here after the browser has been to the OP and
     * come back with a WAS_OIDC_STATE cookie, and an auth code or implicit token.
     */
    public ProviderAuthenticationResult authenticate(HttpServletRequest req,
            HttpServletResponse res,
            ConvergedClientConfig clientConfig) {
        ProviderAuthenticationResult oidcResult = null;

        if (!isAuthorizationEndpointValid(clientConfig)) {
            Tr.error(tc, "OIDC_CLIENT_NULL_AUTH_ENDPOINT", clientConfig.getClientId());
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }

        boolean isImplicit = Constants.IMPLICIT.equals(clientConfig.getGrantType());

        String authzCode = null;
        String responseState = null;

        Hashtable<String, String> reqParameters = new Hashtable<String, String>();

        // the code cookie was set earlier by the code that receives the very first redirect back from the provider.
        String encodedReqParams = CookieHelper.getCookieValue(req.getCookies(), ClientConstants.WAS_OIDC_CODE);
        OidcClientUtil.invalidateReferrerURLCookie(req, res, ClientConstants.WAS_OIDC_CODE);
        if (encodedReqParams != null && !encodedReqParams.isEmpty()) {
            boolean validCookie = validateReqParameters(clientConfig, reqParameters, encodedReqParams);
            if (validCookie) {
                authzCode = reqParameters.get(ClientConstants.CODE);
                responseState = reqParameters.get(ClientConstants.STATE);
            } else {
                // error handling
                oidcResult = new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
                Tr.error(tc, "OIDC_CLIENT_BAD_PARAM_COOKIE", new Object[] { encodedReqParams, clientConfig.getClientId() }); // CWWKS1745E
                return oidcResult;
            }
        }

        if (responseState != null) {
            // if flow was implicit, we'd have a grant_type of implicit and tokens on the params.
            String id_token = req.getParameter(Constants.ID_TOKEN);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "id_token:" + id_token);
            }
            if (id_token != null) {
                reqParameters.put(Constants.ID_TOKEN, id_token);
            }
            String access_token = req.getParameter(Constants.ACCESS_TOKEN);
            if (access_token != null) {
                reqParameters.put(Constants.ACCESS_TOKEN, access_token);
            }
            if (req.getMethod().equals("POST") && req instanceof IExtendedRequest) {
                ((IExtendedRequest) req).setMethod("GET");
            }
        }

        // auth code flow responseState might be invalid if they sat at OP login panel for > authenticationTimeLimit,
        // or otherwise messed up the state.  Rather than 401'ing them when we try to process the auth code,
        // detect it here and just send them back to server to try again.
        boolean stateValid = true;
        if (responseState != null) {
            stateValid = verifyState(req, res, responseState, clientConfig);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Early check of state returns " + stateValid);
            }
            // if we get a bunch of quasi-consecutive bad states, we might be stuck in an endless redirection loop.  Bail out.
            badStateCount = stateValid ? 0 : badStateCount++;
            if (badStateCount > 5) {
                stateValid = true;
                badStateCount = 0;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Got too many bad states, set to true and let the flow fail");
                }
            }
        }

        if (responseState == null || !stateValid) {
            if (tc.isDebugEnabled()) {
                if (!stateValid) {
                    Tr.debug(tc, "*** redirect to server because state is not valid");
                }
                if (responseState == null) {
                    Tr.debug(tc, "*** redirect to server because responseState is null");
                }
            }
            oidcResult = handleRedirectToServer(req, res, clientConfig); // first time through, we go here.
        } else if (isImplicit) {
            oidcResult = handleImplicitFlowTokens(req, res, responseState, clientConfig, reqParameters);

        } else {
            AuthorizationCodeHandler authzCodeHandler = new AuthorizationCodeHandler(sslSupport);

            if (authzCodeHandler.isAuthCodeReused(authzCode)) {
                // somehow a previously used code has been re-submitted, along
                // with valid state query param and state cookie. Rather than having the OP
                // reject the request due to code re-use and fail the entire flow,
                // just go get a new code.
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "*** redirect to server because authcode is reused");
                }
                oidcResult = handleRedirectToServer(req, res, clientConfig);
            } else {
                // confirm the code and go get the tokens if it's good.
                oidcResult = authzCodeHandler.handleAuthorizationCode(req, res, authzCode, responseState, clientConfig);
            }
        }
        if (oidcResult.getStatus() != AuthResult.REDIRECT_TO_PROVIDER) {
            // restore post param.
            // Even the status is bad, it's OK to restore, since it will not
            // have any impact
            WebAppSecurityConfig webAppSecConfig = WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig();
            PostParameterHelper pph = new PostParameterHelper(webAppSecConfig);
            pph.restore(req, res, true);
            OidcClientUtil.invalidateReferrerURLCookies(req, res, OIDC_COOKIES);
        }

        return oidcResult;
    }

    ProviderAuthenticationResult handleImplicitFlowTokens(HttpServletRequest req,
            HttpServletResponse res,
            String responseState,
            ConvergedClientConfig clientConfig,
            Hashtable<String, String> reqParameters) {

        OidcClientRequest oidcClientRequest = (OidcClientRequest) req.getAttribute(ClientConstants.ATTRIB_OIDC_CLIENT_REQUEST);
        ProviderAuthenticationResult oidcResult = null;
        oidcResult = verifyResponseState(req, res, responseState, clientConfig);
        if (oidcResult != null) {
            return oidcResult; //401
        }

        oidcClientRequest.setTokenType(OidcClientRequest.TYPE_ID_TOKEN);

        oidcResult = jose4jUtil.createResultWithJose4J(responseState, reqParameters, clientConfig, oidcClientRequest);

        // get userinfo if configured and available.
        if (clientConfig.getUserInfoEndpointUrl() != null) {
            boolean needHttps = clientConfig.getUserInfoEndpointUrl().toLowerCase().startsWith("https");
            SSLSocketFactory sslSocketFactory = null;
            try {
                sslSocketFactory = new OidcClientHttpUtil().getSSLSocketFactory(clientConfig, sslSupport, false, needHttps);
            } catch (com.ibm.websphere.ssl.SSLException e) {
                //ffdc
            }
            new UserInfoHelper(clientConfig, sslSupport).getUserInfoIfPossible(oidcResult, reqParameters, sslSocketFactory, oidcClientRequest);
        }

        return oidcResult;
    }

    public static boolean checkHttpsRequirement(ConvergedClientConfig clientConfig, String urlStr) {
        boolean metHttpsRequirement = true;
        if (clientConfig.isHttpsRequired()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Checking if URL starts with https: " + urlStr);
            }
            if (urlStr != null && !urlStr.startsWith("https")) {
                metHttpsRequirement = false;
            }
        }
        return metHttpsRequirement;
    }

    public static boolean isAuthorizationEndpointValid(ConvergedClientConfig clientConfig) {
        return clientConfig.getAuthorizationEndpointUrl() != null;
    }

    //todo: avoid call on each request.
    public String setRedirectUrlIfNotDefined(HttpServletRequest req, ConvergedClientConfig clientConfig) {
        //String redirect_url = clientConfig.getRedirectUrlFromServerToClient();
        String redirect_url = null;
        // in oidc case, configimpl completely builds this url, in social we need to finish building it.
        if (clientConfig.isSocial()) {
            redirect_url = getRedirectUrlFromServerToClient(clientConfig.getId(), clientConfig.getContextPath(), clientConfig.getRedirectUrlFromServerToClient());
        } else {
            redirect_url = clientConfig.getRedirectUrlFromServerToClient();
        }

        // in oidc and social case, null unless redirectToRPHostAndPort specified.
        if (redirect_url == null || redirect_url.isEmpty()) {
            String uri = clientConfig.getContextPath() + "/redirect/" + clientConfig.getId();
            redirect_url = new OidcClientUtil().getRedirectUrl(req, uri);
        }
        redirect_url = clientConfig.getRedirectUrlWithJunctionPath(redirect_url);
        return redirect_url;
    }

    // moved from oidcconfigimpl so social can use it.
    public String getRedirectUrlFromServerToClient(String clientId, String contextPath, String redirectToRPHostAndPort) {
        String redirectURL = null;
        if (redirectToRPHostAndPort != null && redirectToRPHostAndPort.length() > 0) {
            try {
                final String fHostPort = redirectToRPHostAndPort;
                @SuppressWarnings({ "unchecked", "rawtypes" })
                URL url = (URL) java.security.AccessController.doPrivileged(new java.security.PrivilegedExceptionAction() {
                    @Override
                    public Object run() throws Exception {
                        return new URL(fHostPort);
                    }
                });
                int port = url.getPort();
                String path = url.getPath();
                if (path == null)
                    path = "";
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                String entryPoint = path + contextPath + "/redirect/" + clientId;
                redirectURL = url.getProtocol() + "://" + url.getHost() + (port > 0 ? ":" + port : "");

                redirectURL = redirectURL + (entryPoint.startsWith("/") ? "" : "/") + entryPoint;
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "the value of redirectToRPHostAndPort might not valid. Please verify that the format is <protocol>://<host>:<port> " + redirectToRPHostAndPort
                            + "\n" + e);
                }
            }
        }
        return redirectURL;
    }

    private boolean isOpenIDScopeSpecified(ConvergedClientConfig clientConfig) {
        String scope = null;
        scope = clientConfig.getScope();
        if (scope.contains("openid")) {
            return true;
        }
        return false;
    }

    String buildAuthorizationUrlWithQuery(HttpServletRequest req, OidcClientRequest oidcClientRequest, String state, ConvergedClientConfig clientConfig, String redirect_url, String acr_values) throws UnsupportedEncodingException {
        String strResponse_type = Constants.RESPONSE_TYPE_CODE; // default is asking for "authorization code
        boolean isImplicit = false;
        if (Constants.IMPLICIT.equals(clientConfig.getGrantType())) {
            // in OidcClientConfigImpl, the grantType and responseType had been
            // clarified. See task 223258
            isImplicit = true;
            strResponse_type = clientConfig.getResponseType();
        }
        String query;
        String clientId = clientConfig.getClientId() == null ? "" : clientConfig.getClientId();
        query = String.format("response_type=%s&client_id=%s&state=%s&redirect_uri=%s&scope=%s",
                URLEncoder.encode(strResponse_type, ClientConstants.CHARSET),
                URLEncoder.encode(clientId, ClientConstants.CHARSET),
                URLEncoder.encode(state, ClientConstants.CHARSET),
                URLEncoder.encode(redirect_url, ClientConstants.CHARSET),
                URLEncoder.encode(clientConfig.getScope(), ClientConstants.CHARSET));

        if (clientConfig.isNonceEnabled() || isImplicit) {
            String nonceValue = OidcUtil.generateRandom(Constants.STATE_LENGTH);
            OidcUtil.createNonceCookie(oidcClientRequest, nonceValue, state, clientConfig);
            query = String.format("%s&nonce=%s", query,
                    URLEncoder.encode(nonceValue, ClientConstants.CHARSET));
        }

        if (acr_values != null && !acr_values.isEmpty()) {
            query = String.format("%s&acr_values=%s", query,
                    URLEncoder.encode(acr_values, ClientConstants.CHARSET));

        } else if (isACRConfigured(clientConfig)) {
            query = String.format("%s&acr_values=%s", query,
                    URLEncoder.encode(clientConfig.getAuthContextClassReference(), ClientConstants.CHARSET));
        }

        if (clientConfig.getPrompt() != null) {
            query = String.format("%s&prompt=%s", query,
                    URLEncoder.encode(clientConfig.getPrompt(), ClientConstants.CHARSET));
        }

        if (isImplicit) {
            query = String.format("%s&response_mode=%s", query,
                    URLEncoder.encode("form_post", ClientConstants.CHARSET));
            // add resource
            String resources = getResourcesParameter(clientConfig);
            if (resources != null) {
                query += resources;
            }
        }
        // look for custom params in the configuration to send to the authorization ep
        query = handleCustomParams(clientConfig, query);

        // check and see if we have any additional params to forward from the request
        query = addForwardLoginParamsToQuery(clientConfig, req, query);

        // in case the AuthorizationEndpoint already has set up its own parameters
        String s = clientConfig.getAuthorizationEndpointUrl();
        String queryMark = "?";
        if (s != null && s.indexOf("?") > 0) {
            queryMark = "&";
        }
        return s + queryMark + query;
    }

    String addForwardLoginParamsToQuery(ConvergedClientConfig clientConfig, HttpServletRequest req, String query) {
        List<String> forwardAuthzParams = clientConfig.getForwardLoginParameter();
        if (forwardAuthzParams == null || forwardAuthzParams.isEmpty()) {
            return query;
        }
        for (String entry : forwardAuthzParams) {
            if (entry != null) {
                String value = req.getParameter(entry);
                if (value != null) {
                    try {
                        query = String.format("%s&%s=%s", query, URLEncoder.encode(entry, ClientConstants.CHARSET), URLEncoder.encode(value, ClientConstants.CHARSET));
                    } catch (UnsupportedEncodingException e) {
                        // Do nothing - UTF-8 encoding will be supported
                    }
                }

            }
        }
        return query;
    }

    /**
     * @param clientConfig
     * @param query
     * @return
     */
    private String handleCustomParams(ConvergedClientConfig clientConfig, String query) {
        HashMap<String, String> customParams = clientConfig.getAuthzRequestParams();
        if (customParams != null && !customParams.isEmpty()) {
            Set<Entry<String, String>> entries = customParams.entrySet();
            for (Entry<String, String> entry : entries) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    try {
                        query = String.format("%s&%s=%s", query, URLEncoder.encode(entry.getKey(), ClientConstants.CHARSET),
                                URLEncoder.encode(entry.getValue(), ClientConstants.CHARSET));
                    } catch (UnsupportedEncodingException e) {

                    }
                }
            }
        }
        return query;
    }

    /**
     * @param clientConfig
     * @return
     */
    private boolean isACRConfigured(ConvergedClientConfig clientConfig) {
        boolean isACR = false;
        String acr_values = null;
        if ((acr_values = clientConfig.getAuthContextClassReference()) != null && !acr_values.isEmpty()) {
            isACR = true;
        }
        return isACR;
    }

    /**
     * @param clientConfig
     * @return
     * @throws UnsupportedEncodingException
     */

    static public String getResourcesParameter(ConvergedClientConfig clientConfig) throws UnsupportedEncodingException {
        String result = null;
        String resources = getResources(clientConfig);
        if (resources != null && !resources.isEmpty()) {
            result = "&resource=" + URLEncoder.encode(resources, ClientConstants.CHARSET);
        }
        return result;
    }

    /**
     * @param clientConfig
     * @return
     * @throws UnsupportedEncodingException
     */
    static public String getResources(ConvergedClientConfig clientConfig) {
        String[] resources = clientConfig.getResources();
        String result = null;
        if (resources != null && resources.length > 0) {
            result = "";
            for (int iI = 0; iI < resources.length; iI++) {
                if (iI > 0) {
                    result = result.concat(" ");
                }
                result = result.concat(resources[iI]);
            }
        }
        return result;
    }

    /*
     * A javascript redirect is preferred over a 302 because it will preserve web fragements in the URL,
     * i.e. foo.com/something#fragment.
     */

    private void doClientSideRedirect(HttpServletResponse response, String loginURL, String state, String domain) throws IOException {

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

        String cookieName = ClientConstants.WAS_REQ_URL_OIDC + HashUtils.getStrHashCode(state);
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

    //todo: remove from oidcca
    String getReqURL(HttpServletRequest req) {
        // due to some longstanding webcontainer strangeness, we have to do
        // some extra things for certain behind-proxy cases to get the right port.
        boolean rewritePort = false;
        Integer realPort = null;
        if (req.getScheme().toLowerCase().contains("https")) {
            realPort = new com.ibm.ws.security.common.web.WebUtils().getRedirectPortFromRequest(req);
        }
        int port = req.getServerPort();
        if (realPort != null && realPort.intValue() != port) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "serverport = " + port + "real port is " + realPort.toString() + ", url will be rewritten to use real port");
            }
            rewritePort = true;
        }

        StringBuffer reqURL = req.getRequestURL();
        if (rewritePort) {
            reqURL = rewritePortInRequestUrl(req, realPort);
        }
        reqURL = appendQueryString(req, reqURL);
        return reqURL.toString();
    }

    StringBuffer rewritePortInRequestUrl(HttpServletRequest req, int realPort) {
        StringBuffer reqURL = new StringBuffer();
        reqURL.append(req.getScheme());
        reqURL.append("://");
        reqURL.append(req.getServerName());
        reqURL.append(":");
        reqURL.append(realPort);
        reqURL.append(req.getRequestURI());
        return reqURL;
    }

    StringBuffer appendQueryString(HttpServletRequest req, StringBuffer reqURL) {
        String queryString = req.getQueryString();
        if (queryString != null) {
            reqURL.append("?");
            reqURL.append(queryString);
        }
        return reqURL;
    }

    /**
     * This gets called after an auth code or implicit token might have been received.
     * This method examines the encodedReqParameters extracted from the WASOidcCode cookie along
     * with the client config and request params, to determine if the params in the cookie are valid.
     *
     * @param clientConfig
     * @param reqParameters
     * @param encodedReqParams
     *            - the encoded params that came in as the value of the WASOidcCode cookie
     * @return
     */
    @FFDCIgnore({ IndexOutOfBoundsException.class })
    public boolean validateReqParameters(ConvergedClientConfig clientConfig, Hashtable<String, String> reqParameters, String cookieValue) {
        boolean validCookie = true;
        String encoded = null;
        String cookieName = "WASOidcCode";
        String requestParameters = null;

        try {
            int lastindex = cookieValue.lastIndexOf("_");
            if (lastindex < 1) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "The cookie may have been tampered with.");
                    if (lastindex < 0) {
                        Tr.debug(tc, "The cookie does not contain an underscore.");
                    }
                    if (lastindex == 0) {
                        Tr.debug(tc, "The cookie does not contain a value before the underscore.");
                    }
                }
                return false;
            }
            encoded = cookieValue.substring(0, lastindex);
            String testCookie = OidcClientUtil.calculateOidcCodeCookieValue(encoded, clientConfig);

            if (!cookieValue.equals(testCookie)) {
                String msg = "The value for the OIDC state cookie [" + cookieName + "] failed validation.";
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, msg);
                }
                validCookie = false;
            }
        } catch (IndexOutOfBoundsException e) {
            // anything wrong indicated the requestParameter cookie is not right or is not in right format
            validCookie = false;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "unexpected exception:", e);
            }
        }

        if (validCookie) {
            requestParameters = Base64Coder.toString(Base64Coder.base64DecodeString(encoded));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "decodedRequestParameters:" + requestParameters);
            }
            JsonParser parser = new JsonParser();
            JsonObject jsonObject = (JsonObject) parser.parse(requestParameters);
            Set<Map.Entry<String, JsonElement>> entries = jsonObject.entrySet();
            for (Map.Entry<String, JsonElement> entry : entries) {
                String key = entry.getKey();
                JsonElement element = entry.getValue();
                if (element.isJsonObject() || element.isJsonPrimitive()) {
                    reqParameters.put(key, element.getAsString());
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "parameterKey:" + key + "  value:" + element.getAsString());
                    }
                } else { // this should not happen
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "unexpected json element:" + element.getClass().getName());
                    }
                    validCookie = false;
                }
            }
        }
        return validCookie;
    }

    /**
     * @param req
     * @param res
     * @param clientId
     *            TODO
     * @param oidcResult
     * @return null if ok, otherwise log error and set 401.
     */
    public ProviderAuthenticationResult verifyResponseState(HttpServletRequest req, HttpServletResponse res,
            String responseState, ConvergedClientConfig clientConfig) {
        Boolean bValidState = false;
        if (responseState != null) {
            bValidState = verifyState(req, res, responseState, clientConfig);
        }
        if (!bValidState) { // CWWKS1744E
            Tr.error(tc, "OIDC_CLIENT_RESPONSE_STATE_ERR", new Object[] { responseState, clientConfig.getClientId() });
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }

        return null;
    }

    /**
     * Determine the name of the state cookie based on the state name key + hashcode of response state.
     * Retrieve that cookie value, then create a check value by hashing the clinet config and resonseState again.
     * If the hash result equals the cookie value, request is valid, proceed to check the clock skew.
     *
     * @param req
     * @param res
     * @return
     */
    public Boolean verifyState(HttpServletRequest req, HttpServletResponse res,
            String responseState, ConvergedClientConfig clientConfig) {
        if (responseState.length() < OidcUtil.STATEVALUE_LENGTH) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "*** verifyState returns false because length is wrong");
            }
            return false; // the state does not even match the length, the verification failed
        }
        long clockSkewMillSeconds = clientConfig.getClockSkewInSeconds() * 1000;

        long allowHandleTimeMillSeconds = (clientConfig.getAuthenticationTimeLimitInSeconds() * 1000) + clockSkewMillSeconds; // allow 7 minutes plust clockSkew

        javax.servlet.http.Cookie[] cookies = req.getCookies();

        String cookieName = ClientConstants.WAS_OIDC_STATE_KEY + HashUtils.getStrHashCode(responseState);
        String stateKey = CookieHelper.getCookieValue(cookies, cookieName); // this could be null if used
        OidcClientUtil.invalidateReferrerURLCookie(req, res, cookieName);
        String cookieValue = HashUtils.createStateCookieValue(clientConfig, responseState);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "stateKey:'" + stateKey + "' cookieValue:'" + cookieValue + "'");
        }
        if (cookieValue.equals(stateKey)) {
            long lNumber = OidcUtil.convertNormalizedTimeStampToLong(responseState);
            long lDate = (new Date()).getTime();
            // lDate can not be earlier than lNumber by clockSkewMillSeconds
            // lDate can not be later than lNumber by clockSkewMllSecond + allowHandleTimeSeconds
            long difference = lDate - lNumber;
            if (difference < 0) {
                difference *= -1;
                if (difference >= clockSkewMillSeconds) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "error current: " + lDate + "  ran at:" + lNumber);
                        Tr.debug(tc, "verifyState returns check against clockSkew: " + (difference < clockSkewMillSeconds));
                    }
                }

                return difference < clockSkewMillSeconds;
            } else {
                if (difference >= allowHandleTimeMillSeconds) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "error current: " + lDate + "  ran at:" + lNumber);
                        Tr.debug(tc, "verifyState returns check against allowHandleTimeMilliseconds: " + (difference < allowHandleTimeMillSeconds));
                    }
                }

                return difference < allowHandleTimeMillSeconds;
            }
        }

        return false;
    }

    public String getIssuerIdentifier(ConvergedClientConfig clientConfig) {
        String issuer = null;
        issuer = clientConfig.getIssuerIdentifier();
        if (issuer == null || issuer.isEmpty()) {
            issuer = extractIssuerFromTokenEndpointUrl(clientConfig);
        }
        return issuer;
    }

    String extractIssuerFromTokenEndpointUrl(ConvergedClientConfig clientConfig) {
        String issuer = null;
        String tokenEndpoint = clientConfig.getTokenEndpointUrl();
        if (tokenEndpoint != null) {
            int endOfSchemeIndex = tokenEndpoint.indexOf("//");
            int lastSlashIndex = tokenEndpoint.lastIndexOf("/");
            boolean urlContainsScheme = endOfSchemeIndex > -1;
            boolean urlContainsSlash = lastSlashIndex > -1;
            if ((!urlContainsScheme && !urlContainsSlash) || (urlContainsScheme && (lastSlashIndex == (endOfSchemeIndex + 1)))) {
                issuer = tokenEndpoint;
            } else {
                issuer = tokenEndpoint.substring(0, lastSlashIndex);
            }
        }
        return issuer;
    }

}
