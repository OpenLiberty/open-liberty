/*******************************************************************************
 * Copyright (c) 2013, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpException;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.google.gson.JsonObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.genericbnf.PasswordNullifier;
import com.ibm.ws.security.common.crypto.HashUtils;
import com.ibm.ws.security.common.http.HttpUtils;
import com.ibm.ws.security.openidconnect.token.IDToken;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

import io.openliberty.security.oidcclientcore.http.OidcClientHttpUtil;
import io.openliberty.security.oidcclientcore.storage.CookieBasedStorage;
import io.openliberty.security.oidcclientcore.storage.CookieStorageProperties;

@SuppressWarnings("restriction")
public class OidcClientUtil {
    @SuppressWarnings("unused")
    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(OidcClientUtil.class);
    OidcClientHttpUtil oidcHttpUtil = null;
    public HttpUtils httpUtils;

    public OidcClientUtil() {
        init(OidcClientHttpUtil.getInstance());
        httpUtils = new HttpUtils();
    }

    void init(OidcClientHttpUtil oidcHttpUtil) {
        this.oidcHttpUtil = oidcHttpUtil;
    }

    /**
     * @param params
     * @param customParams
     */
    public void handleCustomParams(@Sensitive List<NameValuePair> params, HashMap<String, String> customParams) {
        //HashMap<String, String> customParams = clientConfig.getAuthzRequestParams();
        if (customParams != null && !customParams.isEmpty()) {
            Set<Entry<String, String>> entries = customParams.entrySet();
            for (Entry<String, String> entry : entries) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    params.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                }
            }
        }
    }

    public Map<String, Object> checkToken(String tokenInfor, String clientId, @Sensitive String clientSecret,
            String accessToken, boolean isHostnameVerification, String authMethod, SSLSocketFactory sslSocketFactory, boolean useSystemPropertiesForHttpClientConnections) throws Exception {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("token", accessToken));

        if (authMethod.equals(ClientConstants.METHOD_POST)) {
            params.add(new BasicNameValuePair(Constants.CLIENT_ID, clientId));
            params.add(new BasicNameValuePair(Constants.CLIENT_SECRET, clientSecret));
        }

        Map<String, Object> postResponseMap = postToCheckTokenEndpoint(tokenInfor,
                params, clientId, clientSecret, isHostnameVerification, authMethod, sslSocketFactory, useSystemPropertiesForHttpClientConnections);

        // String tokenResponse =
        // oidcHttpUtil.extractTokensFromResponse(postResponseMap);

        // return tokenResponse;
        return postResponseMap;
    }

    public Map<String, Object> getUserinfo(String userInfor, String accessToken, SSLSocketFactory sslSocketFactory,
            boolean isHostnameVerification, boolean useSystemPropertiesForHttpClientConnections) throws Exception {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        // params.add(new BasicNameValuePair("access_token", accessToken));

        Map<String, Object> getResponseMap = getFromUserinfoEndpoint(userInfor, params, accessToken, sslSocketFactory, isHostnameVerification, useSystemPropertiesForHttpClientConnections);
        return getResponseMap;
        // String userinfoResponse =
        // oidcHttpUtil.extractTokensFromResponse(getResponseMap);

        // return userinfoResponse;
    }

    Map<String, Object> postToCheckTokenEndpoint(String tokenEnpoint,
            @Sensitive List<NameValuePair> params,
            String baUsername,
            @Sensitive String baPassword,
            boolean isHostnameVerification,
            String authMethod,
            SSLSocketFactory sslSocketFactory,
            boolean useSystemPropertiesForHttpClientConnections)
            throws Exception {
        return oidcHttpUtil.postToIntrospectEndpoint(tokenEnpoint, params,
                baUsername, baPassword, null, sslSocketFactory, isHostnameVerification, authMethod, useSystemPropertiesForHttpClientConnections);
    }

    Map<String, Object> getFromUserinfoEndpoint(String userInforEndpoint,
            List<NameValuePair> params,
            String accessToken,
            SSLSocketFactory sslSocketFactory,
            boolean isHostnameVerification,
            boolean useSystemPropertiesForHttpClientConnections)
            throws HttpException, IOException {
        return oidcHttpUtil.getFromEndpoint(userInforEndpoint, params, null, null, accessToken, sslSocketFactory, isHostnameVerification, useSystemPropertiesForHttpClientConnections);
    }

    // This assumes the oidcClient is using the same httpEndpoint settings as
    // the application
    // If not, either users has to set their own redirect URL or we need to do
    // extra handling
    public String getRedirectUrl(HttpServletRequest req, String uri) {
        String hostName = req.getServerName();
        Integer httpsPort = new com.ibm.ws.security.common.web.WebUtils().getRedirectPortFromRequest(req);
        String entryPoint = uri;
        if (httpsPort == null && req.isSecure()) {
            // TODO: need to specify SSL_PORT_IS_NULL message
            // Tr.error(tc, "SSL_PORT_IS_NULL");
            int port = req.getServerPort();
            // return whatever in the req
            String httpSchema = req.getScheme();
            return httpSchema + "://" + hostName + (port > 0 && port != 443 ? ":" + port : "") + entryPoint;
        } else {
            return "https://" + hostName + (httpsPort == null ? "" : ":" + httpsPort) + entryPoint;
        }
    }

    public IDToken createIDToken(String tokenString, @Sensitive Object key, String clientId, String issuer,
            String signingAlgorithm, String accessToken) {
        return new IDToken(tokenString, key, clientId, issuer,
                signingAlgorithm, accessToken);
    }

    //static ReferrerURLCookieHandler referrerURLCookieHandler = null; // this will be updated when the referrerURLCookieHandler is changed
    //static WebAppSecurityConfig webAppSecurityConfig = null; // this will be updated when the referrerURLCookieHandler is changed
    static AtomicReference<ReferrerURLCookieHandler> referrerURLCookieHandlerRef = new AtomicReference<ReferrerURLCookieHandler>();
    static AtomicReference<WebAppSecurityConfig> webAppSecurityConfigRef = new AtomicReference<WebAppSecurityConfig>();

    // 240540: rather than call webcontainer code, replace the cookie with an empty one having same cookie and domain name and add to response.
    public static void invalidateReferrerURLCookie(HttpServletRequest req, HttpServletResponse res, String cookieName) {
        CookieBasedStorage store = new CookieBasedStorage(req, res, getReferrerURLCookieHandler());
        store.remove(cookieName);
    }

    public static void invalidateReferrerURLCookies(HttpServletRequest req, HttpServletResponse res, String[] cookieNames) {
        //240540  getReferrerURLCookieHandler().invalidateReferrerURLCookies(req, res, cookieNames);
        if (cookieNames == null || req == null || res == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "invalidateReferrerURLCookies param is null, return");
            }
            return;
        }
        CookieBasedStorage store = new CookieBasedStorage(req, res, getReferrerURLCookieHandler());
        for (String name : cookieNames) {
            store.remove(name);
        }
    }

    static WebAppSecurityConfig getWebAppSecurityConfig() {
        if (webAppSecurityConfigRef.get() == null) {
            webAppSecurityConfigRef.set(WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig());
        }
        return webAppSecurityConfigRef.get();
    }

    public static ReferrerURLCookieHandler getReferrerURLCookieHandler() {
        //240331 - never return a null even if another thread clears the handler.
        ReferrerURLCookieHandler handler = OidcClientUtil.referrerURLCookieHandlerRef.get();
        if (handler == null) {
            handler = getWebAppSecurityConfig().createReferrerURLCookieHandler();
            OidcClientUtil.referrerURLCookieHandlerRef.set(handler);
        }
        return handler;
    }

    /*
     * In case, the ReferrerURLCookieHandler is dynamic in every request, then we will need to make this into
     * OidcClientRequest. And do not do static.
     */
    public static void setReferrerURLCookieHandler(ReferrerURLCookieHandler referrerURLCookieHandler) {
        // in case, the webAppSecurityConfig is dynamically changed
        if (getReferrerURLCookieHandler() != referrerURLCookieHandler) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Old and new CookieHandler", getReferrerURLCookieHandler(), referrerURLCookieHandler);
            }
            webAppSecurityConfigRef.set(WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig());
            referrerURLCookieHandlerRef.set(referrerURLCookieHandler);
        }
    }

    @FFDCIgnore(Exception.class)
    public static void verifyReferrerHostIsValid(HttpServletRequest req, @Sensitive String requestUrl, String cookieNameOrPrefix) throws Exception {
        try {
            // Get the redirection domain names from the global security configuration, <webAppSecurity wasReqURLRedirectDomainNames="mydomain"/>
            ReferrerURLCookieHandler.isReferrerHostValid(PasswordNullifier.nullifyParams(req.getRequestURL().toString()), PasswordNullifier.nullifyParams(requestUrl),
                    getWebAppSecurityConfig().getWASReqURLRedirectDomainNames());
        } catch (Exception re) {
            String errorMsg = Tr.formatMessage(tc, "MALFORMED_URL_IN_ORIGIN_REQUEST_URL_COOKIE", req.getRequestURL(), cookieNameOrPrefix, (new URL(requestUrl)).getHost());
            throw new Exception(errorMsg, re);
        }
    }

    // for FAT
    public static void setWebAppSecurityConfig(WebAppSecurityConfig webAppSecurityConfig) {
        OidcClientUtil.webAppSecurityConfigRef.set(webAppSecurityConfig);
    }

    // set the code cookie during authentication
    public static void setCookieForRequestParameter(HttpServletRequest request, HttpServletResponse response, String id, String state, boolean isHttpsRequest, ConvergedClientConfig clientCfg) {
        //OidcClientConfigImpl clientCfg = activatedOidcClientImpl.getOidcClientConfig(request, id);
        Map<String, String[]> map = request.getParameterMap(); // at least it gets state parameter
        JsonObject jsonObject = new JsonObject();
        Set<Map.Entry<String, String[]>> entries = map.entrySet();
        for (Map.Entry<String, String[]> entry : entries) {
            String key = entry.getKey();
            if (Constants.ACCESS_TOKEN.equals(key) || Constants.ID_TOKEN.equals(key)) {
                continue;
            }
            String[] strs = entry.getValue();
            if (strs != null && strs.length > 0) {
                jsonObject.addProperty(key, strs[0]);
            }
        }
        String requestParameters = jsonObject.toString();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "requestParameters:" + requestParameters);
        }
        String encodedReqParams = Base64Coder.toString(Base64Coder.base64Encode(requestParameters.getBytes(StandardCharsets.UTF_8)));

        String encodedHash = null;
        if (encodedReqParams != null) {
            encodedHash = addSignatureToStringValue(encodedReqParams, clientCfg);
        }
        CookieBasedStorage store = new CookieBasedStorage(request, response, getReferrerURLCookieHandler());
        CookieStorageProperties cookieProps = new CookieStorageProperties();
        if (clientCfg.isHttpsRequired() && isHttpsRequest) {
            cookieProps.setSecure(true);
        }
        store.store(ClientConstants.WAS_OIDC_CODE, encodedHash, cookieProps);
    }

    public static String addSignatureToStringValue(String encoded, ConvergedClientConfig clientCfg) {

        String retVal = new String(encoded);
        String uniqueSecretValue = clientCfg.getClientSecret();
        if (uniqueSecretValue == null) {
            uniqueSecretValue = clientCfg.toString();
        }

        String signatureValue = (new String(encoded)) + "_" + uniqueSecretValue;
        retVal = retVal + "_" + HashUtils.digest(signatureValue); // digest encoded request params and client_secret/client-specific value

        return retVal;
    }
}
