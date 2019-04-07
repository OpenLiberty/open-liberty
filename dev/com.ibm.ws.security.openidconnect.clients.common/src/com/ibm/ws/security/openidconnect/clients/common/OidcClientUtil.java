/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import com.google.gson.JsonObject;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.security.openidconnect.token.IDToken;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.SSOCookieHelper;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

public class OidcClientUtil {
    @SuppressWarnings("unused")
    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(OidcClientUtil.class);
    private final List<NameValuePair> commonHeaders = new ArrayList<NameValuePair>();
    OidcClientHttpUtil oidcHttpUtil = null;

    public OidcClientUtil() {
        commonHeaders.add(new BasicNameValuePair("Accept", "application/json"));
        // commonHeaders.add(new BasicNameValuePair("Accept-Encoding",
        // "gzip, deflate"));
        init(OidcClientHttpUtil.getInstance());
    }

    void init(OidcClientHttpUtil oidcHttpUtil) {
        this.oidcHttpUtil = oidcHttpUtil;
    }

    /*
     * get CommonHeaders
     */
    final List<NameValuePair> getCommonHeaders() {
        return commonHeaders;
    }

    public HashMap<String, String> getTokensFromAuthzCode(String tokenEnpoint,
            String clientId,
            @Sensitive String clientSecret,
            String redirectUri,
            String code,
            String grantType,
            SSLSocketFactory sslSocketFactory,
            boolean isHostnameVerification,
            String authMethod,
            String resources,
            HashMap<String, String> customParams,
            boolean useSystemPropertiesForHttpClientConnections) throws Exception {

        // List<String> result = new ArrayList<String>();
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(ClientConstants.GRANT_TYPE, grantType));
        if (resources != null) {
            params.add(new BasicNameValuePair("resource", resources));
        }
        params.add(new BasicNameValuePair(ClientConstants.REDIRECT_URI, redirectUri));
        params.add(new BasicNameValuePair(Constants.CODE, code));
        oidcHttpUtil.setClientId(clientId);
        if (authMethod.equals(ClientConstants.METHOD_POST) || authMethod.equals(ClientConstants.METHOD_CLIENT_SECRET_POST)) {
            params.add(new BasicNameValuePair(Constants.CLIENT_ID, clientId));
            params.add(new BasicNameValuePair(Constants.CLIENT_SECRET, clientSecret));
        }

        handleCustomParams(params, customParams); // custom token ep params
        Map<String, Object> postResponseMap = postToTokenEndpoint(tokenEnpoint, params, clientId, clientSecret, sslSocketFactory, isHostnameVerification, authMethod, useSystemPropertiesForHttpClientConnections);

        String tokenResponse = oidcHttpUtil.extractTokensFromResponse(postResponseMap);

        JSONObject jobject = JSONObject.parse(tokenResponse);
        // result.add(0, (String) jobject.get(Constants.ID_TOKEN));
        // result.add(1, (String) jobject.get(Constants.ACCESS_TOKEN));
        @SuppressWarnings({ "rawtypes", "unchecked" })
        Iterator<Entry> it = jobject.entrySet().iterator();
        HashMap<String, String> tokens = new HashMap<String, String>();
        while (it.hasNext()) {
            @SuppressWarnings("rawtypes")
            Entry obj = it.next();
            if (obj.getKey() instanceof String) {
                Object value = obj.getValue();
                if (value == null) {
                    value = "";
                }
                tokens.put((String) obj.getKey(), value.toString());
            }
        }

        return tokens;
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

    Map<String, Object> postToTokenEndpoint(String tokenEnpoint,
            @Sensitive List<NameValuePair> params,
            String baUsername,
            @Sensitive String baPassword,
            SSLSocketFactory sslSocketFactory,
            boolean isHostnameVerification,
            String authMethod, boolean useSystemPropertiesForHttpClientConnections)
            throws Exception {
        return oidcHttpUtil.postToEndpoint(tokenEnpoint, params, baUsername, baPassword, null, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, useSystemPropertiesForHttpClientConnections);
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
                baUsername, baPassword, null, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, useSystemPropertiesForHttpClientConnections);
    }

    Map<String, Object> getFromUserinfoEndpoint(String userInforEndpoint,
            List<NameValuePair> params,
            String accessToken,
            SSLSocketFactory sslSocketFactory,
            boolean isHostnameVerification,
            boolean useSystemPropertiesForHttpClientConnections)
            throws HttpException, IOException {
        return getFromEndpoint(userInforEndpoint, params, null, null, accessToken, sslSocketFactory, isHostnameVerification, useSystemPropertiesForHttpClientConnections);
    }

    Map<String, Object> getFromEndpoint(String url,
            List<NameValuePair> params,
            String baUsername,
            @Sensitive String baPassword,
            String accessToken,
            SSLSocketFactory sslSocketFactory,
            boolean isHostnameVerification,
            boolean useSystemPropertiesForHttpClientConnections)
            throws HttpException, IOException {

        String query = null;
        if (params != null) {
            query = URLEncodedUtils.format(params, Constants.UTF_8);
        }

        if (query != null) {
            if (!url.endsWith("?")) {
                url += "?";
            }
            url += query;
        }
        HttpGet request = new HttpGet(url);
        for (NameValuePair nameValuePair : commonHeaders) {
            request.addHeader(nameValuePair.getName(), nameValuePair.getValue());
        }
        if (accessToken != null) {
            request.setHeader(ClientConstants.AUTHORIZATION, ClientConstants.BEARER + accessToken);
        }

        HttpClient httpClient = baUsername != null ? oidcHttpUtil.createHTTPClient(sslSocketFactory, url, isHostnameVerification, baUsername, baPassword, useSystemPropertiesForHttpClientConnections) : oidcHttpUtil.createHTTPClient(sslSocketFactory, url, isHostnameVerification, useSystemPropertiesForHttpClientConnections);

        HttpResponse responseCode = httpClient.execute(request);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put(ClientConstants.RESPONSEMAP_CODE, responseCode);
        result.put(ClientConstants.RESPONSEMAP_METHOD, request);

        return result;
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
            String httpSchema = ((javax.servlet.ServletRequest) req).getScheme();
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

    /*
     * In case, the ReferrerURLCookieHandler is dynamic in every request, then we will need to make this into
     * OidcClientRequest. And do not do static.
     */
    public static Cookie createCookie(String cookieName, @Sensitive String cookieValue, HttpServletRequest req) {
        return createCookie(cookieName, cookieValue, -1, req);
    }

    public static Cookie createCookie(String cookieName, @Sensitive String cookieValue, int maxAge, HttpServletRequest req) {
        Cookie cookie = getReferrerURLCookieHandler().createCookie(cookieName, cookieValue, req);
        String domainName = getSsoDomain(req);
        if (domainName != null && !domainName.isEmpty()) {
            cookie.setDomain(domainName);
        }
        cookie.setMaxAge(maxAge);
        return cookie;
    }

    // 240540: rather than call webcontainer code, replace the cookie with an empty one having same cookie and domain name and add to response.
    public static void invalidateReferrerURLCookie(HttpServletRequest req, HttpServletResponse res, String cookieName) {
        // 240540 getReferrerURLCookieHandler().invalidateReferrerURLCookie(req, res, cookieName); // need to have domain here too
        if (cookieName == null || req == null || res == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "invalidateReferrerURLCookie param is null, return");
            }
            return;
        }
        Cookie c = createCookie(cookieName, "", req);
        String domainName = getSsoDomain(req);
        if (domainName != null && !domainName.isEmpty()) {
            c.setDomain(domainName);
        }
        c.setMaxAge(0);
        res.addCookie(c);
    }

    public static void invalidateReferrerURLCookies(HttpServletRequest req, HttpServletResponse res, String[] cookieNames) {
        //240540  getReferrerURLCookieHandler().invalidateReferrerURLCookies(req, res, cookieNames);
        if (cookieNames == null || req == null || res == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "invalidateReferrerURLCookies param is null, return");
            }
            return;
        }
        for (String name : cookieNames) {
            invalidateReferrerURLCookie(req, res, name);
        }
    }

    /**
     * @param cookie
     * @param req
     */
    public static String getSsoDomain(HttpServletRequest req) {
        SSOCookieHelper ssoCookieHelper = getWebAppSecurityConfig().createSSOCookieHelper();
        String domainName = ssoCookieHelper.getSSODomainName(req,
                getWebAppSecurityConfig().getSSODomainList(),
                getWebAppSecurityConfig().getSSOUseDomainFromURL());
        // config.getSSOUseDomainFromURL() false: if host domainname does not match/exist in domainList, do not use it. Otherwise use it
        return domainName;
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
            handler = OidcClientUtil.getWebAppSecurityConfig().createReferrerURLCookieHandler();
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
        String encodedReqParams = null;
        try {
            encodedReqParams = Base64Coder.toString(Base64Coder.base64Encode(requestParameters.getBytes(ClientConstants.CHARSET)));
        } catch (UnsupportedEncodingException e) {
            //This should not happen, we are using UTF-8
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "get unexpected exception", e);
            }
        }

        String encodedHash = null;
        if (encodedReqParams != null) {
            encodedHash = calculateOidcCodeCookieValue(encodedReqParams, clientCfg);
        }
        Cookie c = OidcClientUtil.createCookie(ClientConstants.WAS_OIDC_CODE, encodedHash, request);
        if (clientCfg.isHttpsRequired() && isHttpsRequest) {
            c.setSecure(true);
        }
        response.addCookie(c);
    }

    /**
     * @param encodedReqParams
     * @param clientSecret
     * @return
     */
    public static String calculateOidcCodeCookieValue(String encoded, ConvergedClientConfig clientCfg) {

        String retVal = new String(encoded);
        String clientidsecret = clientCfg.toString();
        if (clientCfg.getClientSecret() != null) {
            clientidsecret = clientidsecret.concat(clientCfg.getClientSecret());
        }

        String tmpStr = new String(encoded);
        tmpStr = tmpStr.concat("_").concat(clientidsecret);
        retVal = retVal.concat("_").concat(HashUtils.digest(tmpStr)); // digest encoded request params and clientid+client_secret

        return retVal;
    }
}
