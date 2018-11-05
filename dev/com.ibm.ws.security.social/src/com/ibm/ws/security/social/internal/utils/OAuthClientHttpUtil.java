/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;

/**
 *
 */
public class OAuthClientHttpUtil {
    private static final TraceComponent tc = Tr.register(OAuthClientHttpUtil.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    static OAuthClientHttpUtil instance = null;

    OAuthClientHttpUtil() {

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

        HttpPost postMethod = new HttpPost(url);
        if (commonHeaders != null) {
            for (Iterator<NameValuePair> i = commonHeaders.iterator(); i.hasNext();) {
                NameValuePair nvp = i.next();
                postMethod.addHeader(nvp.getName(), nvp.getValue());
            }
        }
        return postMethod;
    }

    HttpGet createHttpGetMethod(String url, final List<NameValuePair> commonHeaders) throws SocialLoginException {

        SocialUtil.validateEndpointWithQuery(url);

        HttpGet getMethod = new HttpGet(url);
        if (commonHeaders != null) {
            for (Iterator<NameValuePair> i = commonHeaders.iterator(); i.hasNext();) {
                NameValuePair nvp = i.next();
                getMethod.addHeader(nvp.getName(), nvp.getValue());
            }
        }
        return getMethod;
    }

    HttpResponse executeRequest(SSLSocketFactory sslSocketFactory, String url, boolean isHostnameVerification, HttpUriRequest httpUriRequest, boolean useJvmProps) throws SocialLoginException {
        HttpClient httpClient = createHTTPClient(sslSocketFactory, url, isHostnameVerification, useJvmProps);
        HttpResponse response = null;
        try {
            response = httpClient.execute(httpUriRequest);
        } catch (Exception e) {
            throw new SocialLoginException("ERROR_EXECUTING_REQUEST", e, new Object[] { url, e.getLocalizedMessage() });
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
        throw new SocialLoginException("RESPONSE_STATUS_UNSUCCESSFUL", null, new Object[] { url, errorMsg });
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

        debugPostToEndPoint(url, params, baUsername, baPassword, accessToken, commonHeaders);

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

        debugPostToEndPoint(url, params, baUsername, baPassword, accessToken, commonHeaders);

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

        SocialUtil.validateEndpointWithQuery(url);

        debugPostToEndPoint(url, params, baUsername, baPassword, accessToken, commonHeaders);

        HttpPost postMethod = createPostMethod(url, commonHeaders);
        postMethod = setPostParameters(postMethod, params);

        return commonEndpointInvocation(postMethod, url, baUsername, baPassword, accessToken, sslSocketFactory, isHostnameVerification, authMethod, useJvmProps);
    }

    HttpPost setPostParameters(HttpPost postMethod, List<NameValuePair> params) {
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

    void debugPostToEndPoint(String url, @Sensitive List<NameValuePair> params, String baUsername, @Sensitive String baPassword, String accessToken, final List<NameValuePair> commonHeaders) {
        if (!tc.isDebugEnabled()) {
            // Trace isn't enabled, so don't bother executing the method
            return;
        }

        // Trace the cURL command that will be used using the provided arguments

        Tr.debug(tc, "postToEndpoint: url: " + url + " headers: " + commonHeaders + " params: " + "*****" + " baUsername: " + baUsername + " baPassword: " + (baPassword != null ? "****" : null) + " accessToken: " + accessToken);
        StringBuffer sb = new StringBuffer();
        sb.append("curl -k -v");
        if (commonHeaders != null) {
            for (Iterator<NameValuePair> i = commonHeaders.iterator(); i.hasNext();) {
                NameValuePair nvp = i.next();
                sb.append(" -H \"");
                sb.append(nvp.getName());
                sb.append(": ");
                sb.append(nvp.getValue());
                sb.append("\"");
            }
        }
        if (params != null && params.size() > 0) {
            sb.append(" -d \"");
            for (Iterator<NameValuePair> i = params.iterator(); i.hasNext();) {
                NameValuePair nvp = i.next();
                String name = nvp.getName();
                sb.append(name);
                sb.append("=");
                if (name.equals("client_secret")) {
                    sb.append("*****");
                } else {
                    sb.append(nvp.getValue());
                }

                if (i.hasNext()) {
                    sb.append("&");
                }
            }
            sb.append("\"");
        }
        if (baUsername != null && baPassword != null) {
            sb.append(" -u \"");
            sb.append(baUsername);
            sb.append(":");
            sb.append("****");
            sb.append("\"");
        }
        if (accessToken != null) {
            sb.append(" -H \"Authorization: bearer ");
            sb.append(accessToken);
            sb.append("\"");
        }
        sb.append(" ");
        sb.append(url);

        Tr.debug(tc, "CURL Command: " + sb.toString());
    }

    public HttpClient createHTTPClient(SSLSocketFactory sslSocketFactory, String url, boolean isHostnameVerification, boolean useJvmProps) {

        HttpClient client = null;

        if (url != null && url.startsWith("http:")) {
            client = getBuilder(useJvmProps).build();
        } else {
            SSLConnectionSocketFactory connectionFactory = null;
            if (!isHostnameVerification) {
                connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new AllowAllHostnameVerifier());
            } else {
                connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new StrictHostnameVerifier());
            }
            client = getBuilder(useJvmProps).setSSLSocketFactory(connectionFactory).build();
        }

        return client;
    }

    public HttpClient createHTTPClient(SSLSocketFactory sslSocketFactory, String url, boolean isHostnameVerification, String baUser, @Sensitive String baPassword, boolean useJvmProps) {

        HttpClient client = null;

        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(baUser, baPassword));

        if (url != null && url.startsWith("http:")) {
            client = getBuilder(useJvmProps).setDefaultCredentialsProvider(credentialsProvider).build();
        } else {
            SSLConnectionSocketFactory connectionFactory = null;
            if (!isHostnameVerification) {
                connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new AllowAllHostnameVerifier());
            } else {
                connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new StrictHostnameVerifier());
            }
            client = getBuilder(useJvmProps).setDefaultCredentialsProvider(credentialsProvider).setSSLSocketFactory(connectionFactory).build();
        }

        return client;
    }
    
    private HttpClientBuilder getBuilder(boolean useJvmProps){
         return useJvmProps ? HttpClientBuilder.create().useSystemProperties() : HttpClientBuilder.create();
    }

    public static OAuthClientHttpUtil getInstance() {
        if (instance == null) {
            instance = new OAuthClientHttpUtil();
        }
        return instance;
    }

}
