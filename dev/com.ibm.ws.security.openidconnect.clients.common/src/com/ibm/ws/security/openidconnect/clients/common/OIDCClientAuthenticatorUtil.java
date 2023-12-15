/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.ssl.NoSSLSocketFactoryException;
import com.ibm.ws.security.openidconnect.client.jose4j.util.Jose4jUtil;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.CookieHelper;
import com.ibm.ws.webcontainer.security.PostParameterHelper;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.wsspi.ssl.SSLSupport;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

import io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException;
import io.openliberty.security.oidcclientcore.http.OidcClientHttpUtil;
import io.openliberty.security.oidcclientcore.storage.OidcClientStorageConstants;

public class OIDCClientAuthenticatorUtil {
    public static final TraceComponent tc = Tr.register(OIDCClientAuthenticatorUtil.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    SSLSupport sslSupport = null;
    private Jose4jUtil jose4jUtil = null;
    private static int badStateCount = 0;
    public static final String[] OIDC_COOKIES = { OidcClientStorageConstants.WAS_OIDC_STATE_KEY, OidcClientStorageConstants.WAS_REQ_URL_OIDC,
            ClientConstants.WAS_OIDC_CODE, OidcClientStorageConstants.WAS_OIDC_NONCE };

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
        OidcAuthorizationRequest authzRequestHelper = new OidcAuthorizationRequest(req, res, clientConfig);
        return authzRequestHelper.sendRequest();
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

        if (!isAuthorizationEndpointConfigured(clientConfig)) {
            Tr.error(tc, "OIDC_CLIENT_NULL_AUTH_ENDPOINT", clientConfig.getClientId());
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }

        boolean isImplicit = Constants.IMPLICIT.equals(clientConfig.getGrantType());

        String responseState = null;

        Hashtable<String, String> reqParameters = new Hashtable<String, String>();

        // the code cookie was set earlier by the code that receives the very first redirect back from the provider.
        String encodedReqParams = CookieHelper.getCookieValue(req.getCookies(), ClientConstants.WAS_OIDC_CODE);
        OidcClientUtil.invalidateReferrerURLCookie(req, res, ClientConstants.WAS_OIDC_CODE);
        if (encodedReqParams != null && !encodedReqParams.isEmpty()) {
            boolean validCookie = validateReqParameters(clientConfig, reqParameters, encodedReqParams);
            if (validCookie) {
                responseState = reqParameters.get(ClientConstants.STATE);
            } else {
                // error handling
                Tr.error(tc, "OIDC_CLIENT_BAD_PARAM_COOKIE", new Object[] { encodedReqParams, clientConfig.getClientId() }); // CWWKS1745E
                return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
            }
        }

        return processAuthenticateRequest(req, res, clientConfig, isImplicit, responseState, reqParameters);
    }

    ProviderAuthenticationResult processAuthenticateRequest(HttpServletRequest req, HttpServletResponse res, ConvergedClientConfig clientConfig, boolean isImplicit, String responseState, Hashtable<String, String> reqParameters) {
        ProviderAuthenticationResult oidcResult;
        boolean stateValid = true;
        if (responseState != null) {
            addTokensToRequestParameters(req, reqParameters);
            stateValid = verifyState(req, res, clientConfig, responseState);
        }
        if (responseState == null || !stateValid) {
            // auth code flow responseState might be invalid if they sat at OP login panel for > authenticationTimeLimit,
            // or otherwise messed up the state.  Rather than 401'ing them when we try to process the auth code,
            // detect it here and just send them back to server to try again.
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
            String authzCode = reqParameters.get(ClientConstants.CODE);
            oidcResult = redirectToServerOrProcessAuthorizationCode(req, res, clientConfig, authzCode, responseState);
        }
        if (oidcResult.getStatus() != AuthResult.REDIRECT_TO_PROVIDER) {
            restorePostParametersAndInvalidateCookies(req, res);
        }
        return oidcResult;
    }

    void addTokensToRequestParameters(HttpServletRequest req, Hashtable<String, String> reqParameters) {
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

    @FFDCIgnore(AuthenticationResponseException.class)
    boolean verifyState(HttpServletRequest req, HttpServletResponse res, ConvergedClientConfig clientConfig, String responseState) {
        OidcAuthenticationResponseValidator responseValidator = new OidcAuthenticationResponseValidator(req, res);
        boolean stateValid = false;
        try {
            responseValidator.verifyState(responseState, clientConfig.getClientId(), clientConfig.getClientSecret(), clientConfig.getClockSkewInSeconds(), clientConfig.getAuthenticationTimeLimitInSeconds());
            stateValid = true;
        } catch (AuthenticationResponseException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception verifying state: " + e);
            }
            stateValid = false;
        }
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
        return stateValid;
    }

    ProviderAuthenticationResult redirectToServerOrProcessAuthorizationCode(HttpServletRequest req, HttpServletResponse res, ConvergedClientConfig clientConfig, String authzCode, String responseState) {
        ProviderAuthenticationResult oidcResult;
        AuthorizationCodeHandler authzCodeHandler = new AuthorizationCodeHandler(req, res, clientConfig, sslSupport);

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
            oidcResult = authzCodeHandler.handleAuthorizationCode(authzCode, responseState);
        }
        return oidcResult;
    }

    void restorePostParametersAndInvalidateCookies(HttpServletRequest req, HttpServletResponse res) {
        // Even the status is bad, it's OK to restore, since it will not have any impact
        WebAppSecurityConfig webAppSecConfig = WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig();
        PostParameterHelper pph = new PostParameterHelper(webAppSecConfig);
        pph.restore(req, res, true);
        OidcClientUtil.invalidateReferrerURLCookies(req, res, OIDC_COOKIES);
    }

    ProviderAuthenticationResult handleImplicitFlowTokens(HttpServletRequest req,
            HttpServletResponse res,
            String responseState,
            ConvergedClientConfig clientConfig,
            Hashtable<String, String> reqParameters) {

        OidcClientRequest oidcClientRequest = (OidcClientRequest) req.getAttribute(ClientConstants.ATTRIB_OIDC_CLIENT_REQUEST);
        ProviderAuthenticationResult oidcResult = verifyResponseState(req, res, responseState, clientConfig);
        if (oidcResult != null) {
            return oidcResult; //401
        }

        oidcClientRequest.setTokenType(OidcClientRequest.TYPE_ID_TOKEN);
        oidcResult = jose4jUtil.createResultWithJose4J(responseState, reqParameters, clientConfig, oidcClientRequest, getSSLSocketFactory(clientConfig));
        return oidcResult;
    }

    void getUserInfo(ConvergedClientConfig clientConfig, Hashtable<String, String> reqParameters, OidcClientRequest oidcClientRequest, ProviderAuthenticationResult oidcResult) {
        boolean needHttps = clientConfig.getUserInfoEndpointUrl().toLowerCase().startsWith("https");
        SSLSocketFactory sslSocketFactory = null;
        try {
            sslSocketFactory = new OidcClientHttpUtil().getSSLSocketFactory(clientConfig.getSSLConfigurationName(), sslSupport);
        } catch (com.ibm.websphere.ssl.SSLException e) {
            Tr.error(tc, "OIDC_CLIENT_HTTPS_WITH_SSLCONTEXT_NULL", new Object[] { e, clientConfig.getClientId() });
        } catch (NoSSLSocketFactoryException e) {
            if (needHttps) {
                Tr.error(tc, "OIDC_CLIENT_HTTPS_WITH_SSLCONTEXT_NULL", new Object[] { "Null ssl socket factory", clientConfig.getClientId() });
            }
        }
        new UserInfoHelper(clientConfig, sslSupport).getUserInfoIfPossible(oidcResult, reqParameters, sslSocketFactory, oidcClientRequest);
    }

    SSLSocketFactory getSSLSocketFactory(ConvergedClientConfig clientConfig) {
        SSLSocketFactory sslSocketFactory = null;
        try {
            sslSocketFactory = new OidcClientHttpUtil().getSSLSocketFactory(clientConfig.getSSLConfigurationName(), sslSupport);
        } catch (com.ibm.websphere.ssl.SSLException e) {
            Tr.error(tc, "OIDC_CLIENT_HTTPS_WITH_SSLCONTEXT_NULL", new Object[] { e, clientConfig.getClientId() });
        } catch (NoSSLSocketFactoryException e) {
            boolean needHttps = clientConfig.getUserInfoEndpointUrl().toLowerCase().startsWith("https");
            if (needHttps) {
                Tr.error(tc, "OIDC_CLIENT_HTTPS_WITH_SSLCONTEXT_NULL", new Object[] { "Null ssl socket factory", clientConfig.getClientId() });
            }
        }
        return sslSocketFactory;
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

    public boolean isAuthorizationEndpointConfigured(ConvergedClientConfig clientConfig) {
        return clientConfig.getAuthorizationEndpointUrl() != null;
    }

    //todo: avoid call on each request.
    public static String setRedirectUrlIfNotDefined(HttpServletRequest req, ConvergedClientConfig clientConfig) {
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
    public static String getRedirectUrlFromServerToClient(String clientId, String contextPath, String redirectToRPHostAndPort) {
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
        try {
            String encoded = extractEncodedValueFromCodeCookie(clientConfig, cookieValue);
            if (encoded != null) {
                validCookie = populateHashtableFromEncodedCookieValue(reqParameters, encoded);
            }
        } catch (IndexOutOfBoundsException e) {
            // anything wrong indicated the requestParameter cookie is not right or is not in right format
            validCookie = false;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "unexpected exception:", e);
            }
        }
        return validCookie;
    }

    String extractEncodedValueFromCodeCookie(ConvergedClientConfig clientConfig, String cookieValue) {
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
            return null;
        }
        String encoded = cookieValue.substring(0, lastindex);
        String testCookie = OidcClientUtil.addSignatureToStringValue(encoded, clientConfig);

        if (!cookieValue.equals(testCookie)) {
            String cookieName = "WASOidcCode";
            String msg = "The value for the OIDC state cookie [" + cookieName + "] failed validation.";
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, msg);
            }
            return null;
        }
        return encoded;
    }

    boolean populateHashtableFromEncodedCookieValue(Hashtable<String, String> reqParameters, String encoded) {
        String requestParameters = Base64Coder.toString(Base64Coder.base64DecodeString(encoded));
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
                return false;
            }
        }
        return true;
    }

    /**
     * @return null if ok, otherwise log error and set 401.
     */
    @FFDCIgnore(AuthenticationResponseException.class)
    public ProviderAuthenticationResult verifyResponseState(HttpServletRequest req, HttpServletResponse res,
            String responseState, ConvergedClientConfig clientConfig) {
        boolean bValidState = false;
        if (responseState != null) {
            OidcAuthenticationResponseValidator responseValidator = new OidcAuthenticationResponseValidator(req, res);
            try {
                responseValidator.verifyState(responseState, clientConfig.getClientId(), clientConfig.getClientSecret(), clientConfig.getClockSkewInSeconds(), clientConfig.getAuthenticationTimeLimitInSeconds());
                bValidState = true;
            } catch (AuthenticationResponseException e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caught exception verifying state: " + e);
                }
                bValidState = false;
            }
        }
        if (!bValidState) { // CWWKS1744E
            Tr.error(tc, "OIDC_CLIENT_RESPONSE_STATE_ERR", new Object[] { responseState, clientConfig.getClientId() });
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }

        return null;
    }

    public static String getIssuerIdentifier(ConvergedClientConfig clientConfig) {
        String issuer = null;
        issuer = clientConfig.getIssuerIdentifier();
        if (issuer == null || issuer.isEmpty()) {
            issuer = extractIssuerFromTokenEndpointUrl(clientConfig);
        }
        return issuer;
    }

    static String extractIssuerFromTokenEndpointUrl(ConvergedClientConfig clientConfig) {
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
