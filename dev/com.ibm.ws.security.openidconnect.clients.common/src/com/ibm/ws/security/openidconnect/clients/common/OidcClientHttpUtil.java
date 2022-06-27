/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLException;
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
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.http.HttpUtils;
import com.ibm.wsspi.ssl.SSLSupport;
import com.ibm.wsspi.webcontainer.util.ThreadContextHelper;

public class OidcClientHttpUtil {
    private static final TraceComponent tc = Tr.register(OidcClientHttpUtil.class);
    private String clientId;
    static OidcClientHttpUtil instance = null;
    private final HttpUtils httpUtils;

    protected OidcClientHttpUtil() {
        httpUtils = new HttpUtils();

    }

    public void setClientId(String id) {
        this.clientId = id;
    }

    public SSLSocketFactory getSSLSocketFactory(ConvergedClientConfig config, SSLSupport sslSupport,
            boolean throwExceptionIfNull, boolean logErrorIfNull) throws com.ibm.websphere.ssl.SSLException {
        SSLSocketFactory sslSocketFactory = null;

        try {
            if (sslSupport != null) {
                sslSocketFactory = sslSupport.getSSLSocketFactory(config.getSSLConfigurationName());
            }
        } catch (javax.net.ssl.SSLException e) {
            throw new com.ibm.websphere.ssl.SSLException(e);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "sslSocketFactory (" + ") get: " + sslSocketFactory);
        }

        if (sslSocketFactory == null && throwExceptionIfNull) {
            throw new com.ibm.websphere.ssl.SSLException(Tr.formatMessage(tc, "OIDC_CLIENT_HTTPS_WITH_SSLCONTEXT_NULL",
                    new Object[] { "Null ssl socket factory", config.getClientId() }));

        }
        if (sslSocketFactory == null && logErrorIfNull) {
            Tr.error(tc, "OIDC_CLIENT_HTTPS_WITH_SSLCONTEXT_NULL", new Object[] { "Null ssl socket factory", config.getClientId() });
        }
        return sslSocketFactory;
    }

    /**
     * @param postResponseMap
     * @return
     * @throws IOException
     */
    String extractTokensFromResponse(Map<String, Object> postResponseMap) throws Exception {
        HttpResponse response = (HttpResponse) postResponseMap.get(ClientConstants.RESPONSEMAP_CODE);
        HttpEntity entity = response.getEntity();
        if (entity == null)
            return null;
        try {
            return EntityUtils.toString(entity);
        } catch (Exception ioe) {
            // get/set the default error message (indicating we can't get tokens from a returned response)
            String insertMsg = Tr.formatMessage(tc, "OIDC_CLIENT_INVALID_HTTP_RESPONSE_NO_MSG");
            String eMsg = ioe.getMessage();
            // if we got an usable error message in the exception, use it, otherwise, use the more generic error messages
            if (eMsg != null && !eMsg.isEmpty()) {
                insertMsg = eMsg;
            }
            Tr.error(tc, "OIDC_CLIENT_INVALID_HTTP_RESPONSE", new Object[] { insertMsg, this.clientId });
            throw ioe;
        }
    }

    /**
     * @param url
     * @return
     */
    HttpPost createPostMethod(String url, final List<NameValuePair> commonHeaders) {
        return httpUtils.createHttpPostMethod(url, commonHeaders);
    }

    /**
     * @param url
     * @return
     */
    HttpGet createHttpGetMethod(String url, final List<NameValuePair> commonHeaders) {
        return httpUtils.createHttpGetMethod(url, commonHeaders);
    }

    @FFDCIgnore({ SSLException.class })
    Map<String, Object> postToEndpoint(String url,
            @Sensitive List<NameValuePair> params,
            String baUsername,
            @Sensitive String baPassword,
            String accessToken,
            SSLSocketFactory sslSocketFactory,
            final List<NameValuePair> commonHeaders,
            boolean isHostnameVerification,
            String authMethod, boolean useSystemPropertiesForHttpClientConnections) throws Exception {

        debugPostToEndPoint(url, params, baUsername, baPassword, accessToken, commonHeaders);

        HttpPost postMethod = createPostMethod(url, commonHeaders);

        postMethod.setEntity(new UrlEncodedFormEntity(params));

        setAuthorizationHeaderForPostMethod(baUsername, baPassword, accessToken, postMethod, authMethod);

        HttpClient httpClient = OidcClientHttpUtil.getInstance().createHTTPClient(sslSocketFactory, url, isHostnameVerification, useSystemPropertiesForHttpClientConnections);
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

        Map<String, Object> result = new HashMap<String, Object>();
        result.put(ClientConstants.RESPONSEMAP_CODE, response);
        result.put(ClientConstants.RESPONSEMAP_METHOD, postMethod);

        return result;
    }

    @FFDCIgnore({ SSLException.class })
    Map<String, Object> postToIntrospectEndpoint(String url,
            @Sensitive List<NameValuePair> params,
            String baUsername,
            @Sensitive String baPassword,
            String accessToken,
            SSLSocketFactory sslSocketFactory,
            final List<NameValuePair> commonHeaders,
            boolean isHostnameVerification,
            String authMethod, boolean useSystemPropertiesForHttpClientConnections) throws Exception {

        debugPostToEndPoint(url, params, baUsername, baPassword, accessToken, commonHeaders);

        HttpPost postMethod = createPostMethod(url, commonHeaders);

        postMethod.setEntity(new UrlEncodedFormEntity(params));

        setAuthorizationHeaderForPostMethod(baUsername, baPassword, accessToken, postMethod, authMethod);

        HttpClient httpClient = OidcClientHttpUtil.getInstance().createHTTPClient(sslSocketFactory, url, isHostnameVerification, useSystemPropertiesForHttpClientConnections);
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

        Map<String, Object> result = new HashMap<String, Object>();
        result.put(ClientConstants.RESPONSEMAP_CODE, response);
        result.put(ClientConstants.RESPONSEMAP_METHOD, postMethod);

        return result;
    }

    /**
     * @param baUsername
     * @param baPassword
     * @param accessToken
     * @param postMethod
     */
    void setAuthorizationHeaderForPostMethod(String baUsername,
            @Sensitive String baPassword, String accessToken,
            HttpPost postMethod,
            String authMethod) {
        if (authMethod.contains(ClientConstants.METHOD_BASIC)) { // social constant differs
            String userpass = baUsername + ":" + baPassword;
            String basicAuth = "Basic " + Base64Coder.base64Encode(userpass);
            postMethod.setHeader(ClientConstants.AUTHORIZATION, basicAuth);
        }

        if (accessToken != null) {
            postMethod.addHeader(ClientConstants.AUTHORIZATION, ClientConstants.BEARER + accessToken);
        }
    }

    static int getTokenEndPointPort(String url) {
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

    /**
     * @param url
     * @param headers
     * @param params
     * @param baUsername
     * @param baPassword
     * @param accessToken
     */
    void debugPostToEndPoint(String url,
            @Sensitive List<NameValuePair> params,
            String baUsername,
            @Sensitive String baPassword,
            String accessToken,
            final List<NameValuePair> commonHeaders) {
        httpUtils.debugPostToEndPoint(url, params, baUsername, baPassword, accessToken, commonHeaders);
    }

    public HttpClient createHTTPClient(SSLSocketFactory sslSocketFactory, String url, boolean isHostnameVerification, boolean useSystemPropertiesForHttpClientConnections) {
        return httpUtils.createHttpClient(sslSocketFactory, url, isHostnameVerification, useSystemPropertiesForHttpClientConnections);
    }

    public HttpClient createHTTPClient(SSLSocketFactory sslSocketFactory, String url, boolean isHostnameVerification,
            String baUser, @Sensitive String baPassword, boolean useSystemPropertiesForHttpClientConnections) {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(baUser, baPassword));

        return httpUtils.createHttpClient(sslSocketFactory, url.startsWith("httsp:"), isHostnameVerification, useSystemPropertiesForHttpClientConnections, credentialsProvider);
    }

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
