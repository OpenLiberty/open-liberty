/*******************************************************************************
 * Copyright (c) 2013, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.http.HttpUtils;
import com.ibm.ws.security.common.ssl.NoSSLSocketFactoryException;
import com.ibm.ws.security.common.ssl.SecuritySSLUtils;
import com.ibm.ws.security.common.web.WebUtils;
import com.ibm.wsspi.ssl.SSLSupport;
import com.ibm.wsspi.webcontainer.util.ThreadContextHelper;

import io.openliberty.security.oidcclientcore.token.TokenConstants;

public class OidcClientHttpUtil {

    public static final List<NameValuePair> commonHeaders;

    private static final TraceComponent tc = Tr.register(OidcClientHttpUtil.class);
    private final HttpUtils httpUtils;

    static {
        commonHeaders = new ArrayList<NameValuePair>();
        commonHeaders.add(new BasicNameValuePair(HttpConstants.ACCEPT, HttpConstants.APPLICATION_JSON));
    }

    public OidcClientHttpUtil() {
        httpUtils = new HttpUtils();
    }

    public SSLSocketFactory getSSLSocketFactory(String sslConfigurationName, SSLSupport sslSupport) throws com.ibm.websphere.ssl.SSLException, NoSSLSocketFactoryException {
        SSLSocketFactory sslSocketFactory = null;
        try {
            sslSocketFactory = SecuritySSLUtils.getSSLSocketFactory(sslSupport, sslConfigurationName);
        } catch (javax.net.ssl.SSLException e) {
            throw new com.ibm.websphere.ssl.SSLException(e);
        }
        return sslSocketFactory;
    }

    public String extractEntityFromTokenResponse(Map<String, Object> postResponseMap) throws Exception {
        HttpResponse response = (HttpResponse) postResponseMap.get(HttpConstants.RESPONSEMAP_CODE);
        HttpEntity entity = response.getEntity();
        if (entity == null)
            return null;

        return EntityUtils.toString(entity);
    }

    public HttpPost setupPost(String url,
                              @Sensitive List<NameValuePair> params,
                              String baUsername,
                              @Sensitive String baPassword,
                              String accessToken,
                              String authMethod) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "OIDC _SSO RP POST TO URL [" + WebUtils.stripSecretFromUrl(url, "client_secret") + "]");
            httpUtils.debugPostToEndPoint(url, params, baUsername, baPassword, accessToken, commonHeaders);
        }
        HttpPost postMethod = httpUtils.createHttpPostMethod(url, commonHeaders);
        if (!TokenConstants.METHOD_PRIVATE_KEY_JWT.equals(authMethod)) {
            setAuthorizationHeaderForPostMethod(baUsername, baPassword, accessToken, postMethod, authMethod);
        }
        postMethod.setEntity(new UrlEncodedFormEntity(params));
        return postMethod;
    }

    @FFDCIgnore({ SSLException.class })
    HttpResponse setupResponse(SSLSocketFactory sslSocketFactory,
                               String url,
                               boolean isHostnameVerification,
                               boolean useSystemPropertiesForHttpClientConnections,
                               HttpPost postMethod) throws Exception {
        HttpClient httpClient = createHTTPClient(sslSocketFactory, url, isHostnameVerification, useSystemPropertiesForHttpClientConnections);
        HttpResponse response = null;

        ClassLoader origCL = ThreadContextHelper.getContextClassLoader();
        ThreadContextHelper.setClassLoader(getClass().getClassLoader());
        try {
            response = httpClient.execute(postMethod);
        } catch (SSLException ex) {
            throw ex;
        } catch (Exception ioe) {
            throw ioe; // keep it for unit testing for now.
        } finally {
            ThreadContextHelper.setClassLoader(origCL);
        }
        return response;
    }

    Map<String, Object> finishPost(HttpResponse response, HttpPost postMethod) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(HttpConstants.RESPONSEMAP_CODE, response);
        result.put(HttpConstants.RESPONSEMAP_METHOD, postMethod);
        return result;
    }

    public Map<String, Object> postToEndpoint(String url,
                                              @Sensitive List<NameValuePair> params,
                                              String baUsername,
                                              @Sensitive String baPassword,
                                              String accessToken,
                                              SSLSocketFactory sslSocketFactory,
                                              boolean isHostnameVerification,
                                              String authMethod, boolean useSystemPropertiesForHttpClientConnections) throws Exception {

        HttpPost postMethod = setupPost(url, params, baUsername, baPassword, accessToken, authMethod);
        return postToEndpoint(url, postMethod, sslSocketFactory, isHostnameVerification, useSystemPropertiesForHttpClientConnections);
    }

    public Map<String, Object> postToEndpoint(String url,
                                              HttpPost postMethod,
                                              SSLSocketFactory sslSocketFactory,
                                              boolean isHostnameVerification,
                                              boolean useSystemPropertiesForHttpClientConnections) throws Exception {

        HttpResponse response = setupResponse(sslSocketFactory, url, isHostnameVerification, useSystemPropertiesForHttpClientConnections, postMethod);

        // Check the response from the endpoint to see if there was an error
        StatusLine status = response.getStatusLine();
        if (status == null || status.getStatusCode() != 200) {
            String errorMsg = "Could not get the status of the response, or the response returned an error.";
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                errorMsg = EntityUtils.toString(entity).trim();
            }
            if (status != null && status.getStatusCode() == 400) {

                throw new BadPostRequestException(errorMsg, 400);
            }
            throw new IOException("Failed to reach endpoint " + url + " because of the following error: " + errorMsg);
        }

        return finishPost(response, postMethod);
    }

    public Map<String, Object> postToIntrospectEndpoint(String url,
                                                        @Sensitive List<NameValuePair> params,
                                                        String baUsername,
                                                        @Sensitive String baPassword,
                                                        String accessToken,
                                                        SSLSocketFactory sslSocketFactory,
                                                        boolean isHostnameVerification,
                                                        String authMethod, boolean useSystemPropertiesForHttpClientConnections) throws Exception {

        HttpPost postMethod = setupPost(url, params, baUsername, baPassword, accessToken, authMethod);
        HttpResponse response = setupResponse(sslSocketFactory, url, isHostnameVerification, useSystemPropertiesForHttpClientConnections, postMethod);

        return finishPost(response, postMethod);
    }

    public void setAuthorizationHeaderForPostMethod(String baUsername,
                                                    @Sensitive String baPassword, String accessToken,
                                                    HttpPost postMethod,
                                                    String authMethod) {
        if (authMethod.contains(HttpConstants.METHOD_BASIC)) { // social constant differs
            String userpass = baUsername + ":" + baPassword;
            String encodedUserpass = Base64.getEncoder().encodeToString(userpass.getBytes());
            postMethod.setHeader(HttpConstants.AUTHORIZATION, HttpConstants.BASIC + encodedUserpass);
        }

        if (accessToken != null) {
            postMethod.addHeader(HttpConstants.AUTHORIZATION, HttpConstants.BEARER + accessToken);
        }
    }

    public static int getTokenEndPointPort(String url) {
        int port = 0;
        int first = url.indexOf(":"); // https://hostname:8011/openidconnect
        int last = url.lastIndexOf(":"); // https://hostname:8011/openidconnect
        if (first != last) {
            int end = -1;
            int begin = last;
            if (begin > 0) {
                end = url.substring(begin).indexOf("/"); // :8011/openidconnect
                if (end > 0) {
                    String s = url.substring(begin + 1, begin + end);
                    port = Integer.valueOf(s);
                }
            }
        } else if (url.startsWith("https:")) {
            port = 443;
        } else if (url.startsWith("http:")) {
            port = 80;
        }

        return port;

    }

    public Map<String, Object> getFromEndpoint(String url,
                                               List<NameValuePair> params,
                                               String baUsername,
                                               @Sensitive String baPassword,
                                               String accessToken,
                                               SSLSocketFactory sslSocketFactory,
                                               boolean isHostnameVerification,
                                               boolean useSystemPropertiesForHttpClientConnections) throws HttpException, IOException {

        String query = null;
        if (params != null) {
            query = URLEncodedUtils.format(params, HttpConstants.UTF_8);
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
            request.setHeader(HttpConstants.AUTHORIZATION, HttpConstants.BEARER + accessToken);
        }

        BasicCredentialsProvider credentialsProvider = null;
        if (baUsername != null) {
            credentialsProvider = httpUtils.createCredentialsProvider(baUsername, baPassword);
        }

        HttpClient httpClient = createHttpClient(sslSocketFactory, url, isHostnameVerification, useSystemPropertiesForHttpClientConnections, credentialsProvider);

        HttpResponse response = null;

        ClassLoader origCL = ThreadContextHelper.getContextClassLoader();
        ThreadContextHelper.setClassLoader(getClass().getClassLoader());
        try {
            response = httpClient.execute(request);
        } finally {
            ThreadContextHelper.setClassLoader(origCL);
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put(HttpConstants.RESPONSEMAP_CODE, response);
        result.put(HttpConstants.RESPONSEMAP_METHOD, request);

        return result;
    }

    public HttpClient createHTTPClient(SSLSocketFactory sslSocketFactory, String url, boolean isHostnameVerification, boolean useSystemPropertiesForHttpClientConnections) {
        return httpUtils.createHttpClient(sslSocketFactory, url, isHostnameVerification, useSystemPropertiesForHttpClientConnections);
    }

    public HttpClient createHTTPClient(SSLSocketFactory sslSocketFactory, String url, boolean isHostnameVerification,
                                       String baUser, @Sensitive String baPassword, boolean useSystemPropertiesForHttpClientConnections) {
        BasicCredentialsProvider credentialsProvider = httpUtils.createCredentialsProvider(baUser, baPassword);
        return httpUtils.createHttpClientWithCookieSpec(sslSocketFactory, url, isHostnameVerification, useSystemPropertiesForHttpClientConnections, credentialsProvider);
    }

    public HttpClient createHttpClient(SSLSocketFactory sslSocketFactory, String url, boolean isHostnameVerification,
                                       boolean useSystemPropertiesForHttpClientConnections, BasicCredentialsProvider credentialsProvider) {
        return httpUtils.createHttpClient(sslSocketFactory, url, isHostnameVerification, useSystemPropertiesForHttpClientConnections, credentialsProvider);
    }

    static OidcClientHttpUtil instance = null;;

    /**
     * @return
     */
    public static OidcClientHttpUtil getInstance() {
        if (instance == null) {
            instance = new OidcClientHttpUtil();
        }
        return instance;
    }

}