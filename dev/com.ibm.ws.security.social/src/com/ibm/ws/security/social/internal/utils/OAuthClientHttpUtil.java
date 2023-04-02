/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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
package com.ibm.ws.security.social.internal.utils;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.common.http.HttpUtils;
import com.ibm.wsspi.webcontainer.util.ThreadContextHelper;

/**
 *
 */
public class OAuthClientHttpUtil {
    private static final TraceComponent tc = Tr.register(OAuthClientHttpUtil.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    static OAuthClientHttpUtil instance = null;
    private HttpUtils httpUtils;

    OAuthClientHttpUtil() {
    	httpUtils = new HttpUtils();
    }

    @Sensitive
    public String extractTokensFromResponse(Map<String, Object> postResponseMap) throws SocialLoginException {

        if (postResponseMap == null) {
            return null;
        }
        HttpResponse response = (HttpResponse) postResponseMap.get(ClientConstants.RESPONSEMAP_CODE);
        if (response == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "An HttpResponse object was not found in the map");
            }
            return null;
        }
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return null;
        }
        try {
            return EntityUtils.toString(entity);
        } catch (Exception e) {
            throw new SocialLoginException("ERROR_PARSING_RESPONSE_ENTITY", e, new Object[] { e.getLocalizedMessage() });
        }
    }

    HttpPost createPostMethod(String url, final List<NameValuePair> commonHeaders) throws SocialLoginException {

        SocialUtil.validateEndpointWithQuery(url);

        return httpUtils.createHttpPostMethod(url, commonHeaders);
    }

    HttpGet createHttpGetMethod(String url, final List<NameValuePair> commonHeaders) throws SocialLoginException {

        SocialUtil.validateEndpointWithQuery(url);

        return httpUtils.createHttpGetMethod(url, commonHeaders);
    }

    HttpResponse executeRequest(SSLSocketFactory sslSocketFactory, String url, boolean isHostnameVerification, HttpUriRequest httpUriRequest, boolean useJvmProps) throws SocialLoginException {
        HttpClient httpClient = createHTTPClient(sslSocketFactory, url, isHostnameVerification, useJvmProps);
        HttpResponse response = null;
        
        ClassLoader origCL = ThreadContextHelper.getContextClassLoader();
        ThreadContextHelper.setClassLoader(getClass().getClassLoader());
        try {
            response = httpClient.execute(httpUriRequest);
        } catch (Exception e) {
            throw new SocialLoginException("ERROR_EXECUTING_REQUEST", e, new Object[] { url, e.getLocalizedMessage() });
        } finally {
            ThreadContextHelper.setClassLoader(origCL);
        }
        return response;
    }

    void verifyResponse(String url, HttpResponse response) throws SocialLoginException {
        if (response == null) {
            return;
        }
        StatusLine status = response.getStatusLine();
        if (status != null && status.getStatusCode() == 200) {
            return;
        }
        String statusInsert = (status == null) ? null : (status.getStatusCode() + " " + status.getReasonPhrase());
        String errorMsg = Tr.formatMessage(tc, "RESPONSE_STATUS_MISSING_OR_ERROR", new Object[] { statusInsert });
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            try {
                errorMsg = EntityUtils.toString(entity).trim();
            } catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caught error parsing HttpEntity: " + e);
                }
            }
        }
        throw new SocialLoginException("RESPONSE_STATUS_UNSUCCESSFUL", null, new Object[] { url, statusInsert, errorMsg });
    }

    public Map<String, Object> postToEndpoint(String url,
            @Sensitive List<NameValuePair> params,
            String baUsername,
            @Sensitive String baPassword,
            String accessToken,
            SSLSocketFactory sslSocketFactory,
            final List<NameValuePair> commonHeaders,
            boolean isHostnameVerification,
            String authMethod, boolean useJvmProps) throws SocialLoginException {

        SocialUtil.validateEndpointWithQuery(url);

        httpUtils.debugPostToEndPoint(url, params, baUsername, baPassword, accessToken, commonHeaders);

        HttpPost postMethod = createPostMethod(url, commonHeaders);
        postMethod = setPostParameters(postMethod, params);

        return commonEndpointInvocation(postMethod, url, baUsername, baPassword, accessToken, sslSocketFactory, isHostnameVerification, authMethod, useJvmProps);
    }

    public Map<String, Object> getToEndpoint(String url,
            @Sensitive List<NameValuePair> params,
            String baUsername,
            @Sensitive String baPassword,
            String accessToken,
            SSLSocketFactory sslSocketFactory,
            final List<NameValuePair> commonHeaders,
            boolean isHostnameVerification,
            String authMethod, boolean useJvmProps) throws SocialLoginException {

        SocialUtil.validateEndpointWithQuery(url);

        httpUtils.debugPostToEndPoint(url, params, baUsername, baPassword, accessToken, commonHeaders);

        HttpGet getMethod = createHttpGetMethod(url, commonHeaders);

        if (params != null) {
            for (Iterator<NameValuePair> i = params.iterator(); i.hasNext();) {
                NameValuePair nvp = i.next();
                getMethod.addHeader(nvp.getName(), nvp.getValue());
            }
        }

        return commonEndpointInvocation(getMethod, url, baUsername, baPassword, accessToken, sslSocketFactory, isHostnameVerification, authMethod, useJvmProps);
    }

    Map<String, Object> postToIntrospectEndpoint(String url,
            @Sensitive List<NameValuePair> params,
            String baUsername,
            @Sensitive String baPassword,
            String accessToken,
            SSLSocketFactory sslSocketFactory,
            final List<NameValuePair> commonHeaders,
            boolean isHostnameVerification,
            String authMethod, boolean useJvmProps) throws SocialLoginException {
         return postToEndpoint(url, params, baUsername, baPassword, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, useJvmProps);
    }

    HttpPost setPostParameters(HttpPost postMethod, @Sensitive List<NameValuePair> params) {
        try {
            if (params != null) {
                postMethod.setEntity(new UrlEncodedFormEntity(params));
            }
        } catch (UnsupportedEncodingException e1) {
            // Should not occur
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "The default encoding is not supported; parameters might not be present in the request");
            }
        }
        return postMethod;
    }

    Map<String, Object> commonEndpointInvocation(HttpUriRequest httpUriRequest, String url, String baUsername, @Sensitive String baPassword, String accessToken, SSLSocketFactory sslSocketFactory, boolean isHostnameVerification, String authMethod, boolean useJvmProps) throws SocialLoginException {
        setAuthorizationHeader(baUsername, baPassword, accessToken, httpUriRequest, authMethod);

        HttpResponse response = executeRequest(sslSocketFactory, url, isHostnameVerification, httpUriRequest, useJvmProps);

        // Check the response from the endpoint to see if there was an error
        verifyResponse(url, response);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put(ClientConstants.RESPONSEMAP_CODE, response);
        result.put(ClientConstants.RESPONSEMAP_METHOD, httpUriRequest);

        return result;
    }

    void setAuthorizationHeader(String baUsername, @Sensitive String baPassword, String accessToken, HttpUriRequest httpUriRequest, String authMethod) {
        if (accessToken != null) {
            httpUriRequest.addHeader(ClientConstants.AUTHORIZATION, ClientConstants.BEARER + accessToken);
            return;
        }
        // TODO - what if access token is empty?
        if (authMethod != null && authMethod.equals(ClientConstants.METHOD_client_secret_basic)) {
            String userpass = baUsername + ":" + baPassword;
            String basicAuth = "Basic " + Base64Coder.base64Encode(userpass);

            httpUriRequest.addHeader(ClientConstants.AUTHORIZATION, basicAuth);
        }
    }

    public HttpClient createHTTPClient(SSLSocketFactory sslSocketFactory, String url, boolean isHostnameVerification, boolean useJvmProps) {
        return httpUtils.createHttpClient(sslSocketFactory, url, isHostnameVerification, useJvmProps);
    }

    public HttpClient createHTTPClient(SSLSocketFactory sslSocketFactory, String url, boolean isHostnameVerification, String baUser, @Sensitive String baPassword, boolean useJvmProps) {
        BasicCredentialsProvider credentialsProvider = httpUtils.createCredentialsProvider(baUser, baPassword);
        return httpUtils.createHttpClient(sslSocketFactory, url, isHostnameVerification, useJvmProps, credentialsProvider);
    }

    public static OAuthClientHttpUtil getInstance() {
        if (instance == null) {
            instance = new OAuthClientHttpUtil();
        }
        return instance;
    }

}