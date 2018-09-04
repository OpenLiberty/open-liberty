/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import java.security.KeyStoreException;

import javax.net.ssl.SSLSocketFactory;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.ssl.SSLSupport;

public class HttpClientUtil {

    private static final TraceComponent tc = Tr.register(HttpClientUtil.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    protected JSSEHelper getJSSEHelper(SSLSupport sslSupport) throws SSLException {
        if (sslSupport != null) {
            return sslSupport.getJSSEHelper();
        }
        return null;
    }

    protected SSLSocketFactory getSSLSocketFactory(String requestUrl, String sslConfigurationName, SSLSupport sslSupport, String clientId) throws SSLException {
        SSLSocketFactory sslSocketFactory = null;

        if (sslSupport != null) {
            try {
                sslSocketFactory = sslSupport.getSSLSocketFactory(sslConfigurationName);
            } catch (javax.net.ssl.SSLException e) {
                throw new SSLException(e.getMessage());
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "sslSocketFactory (" + ") get: " + sslSocketFactory);
            }
        }

        if (sslSocketFactory == null) {
            if (requestUrl != null && requestUrl.startsWith("https")) {
                throw new SSLException(Tr.formatMessage(tc, "OIDC_CLIENT_HTTPS_WITH_SSLCONTEXT_NULL", new Object[] { "Null ssl socket factory", clientId }));
            }
        }
        return sslSocketFactory;
    }

    @FFDCIgnore({ KeyStoreException.class })
    public String getHTTPRequestAsString(HttpClient httpClient, String url, String jwkClientId, @Sensitive String jwkClientSecret) throws Exception {

        String json = null;
        try {
            HttpGet request = new HttpGet(url);
            request.addHeader("content-type", "application/json");
            if (jwkClientId != null && jwkClientSecret != null) {
                String userpass = jwkClientId + ":" + jwkClientSecret;
                String basicAuth = "Basic " + Base64Coder.base64Encode(userpass);
                request.addHeader(ClientConstants.AUTHORIZATION, basicAuth);
            }
            HttpResponse result = httpClient.execute(request);
            StatusLine statusLine = result.getStatusLine();
            int iStatusCode = statusLine.getStatusCode();
            if (iStatusCode == 200) {
                json = EntityUtils.toString(result.getEntity(), "UTF-8");
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Response: ", json);
                }
                if (json == null || json.isEmpty()) { // NO JWK returned
                    String message = Tr.formatMessage(tc, "JWK_RETRIEVE_FAILED", new Object[] { url, iStatusCode, json });
                    Tr.error(tc, message);
                    throw new Exception(message);
                }
            } else {
                String errMsg = EntityUtils.toString(result.getEntity(), "UTF-8");
                // error in getting JWK
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "status:" + iStatusCode + " errorMsg:" + errMsg);
                }
                String message = Tr.formatMessage(tc, "JWK_RETRIEVE_FAILED", new Object[] { url, iStatusCode, errMsg });
                Tr.error(tc, message);
                throw new Exception(message);
            }
        } catch (KeyStoreException e) {
            throw e;
        }

        return json;
    }

}
