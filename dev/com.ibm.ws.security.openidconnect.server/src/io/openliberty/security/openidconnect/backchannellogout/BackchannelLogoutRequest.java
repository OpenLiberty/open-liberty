/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.openidconnect.backchannellogout;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.net.ssl.SSLSocketFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.http.HttpUtils;
import com.ibm.ws.security.openidconnect.server.internal.OidcServerConfigImpl;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;
import com.ibm.wsspi.ssl.SSLSupport;

public class BackchannelLogoutRequest implements Callable<BackchannelLogoutRequest> {

    private static TraceComponent tc = Tr.register(BackchannelLogoutRequest.class);

    public static final String LOGOUT_TOKEN_PARAM_NAME = "logout_token";

    private final OidcServerConfigImpl oidcServerConfig;
    private final String url;
    private final String logoutToken;
    private HttpResponse response = null;

    private final HttpUtils httpUtils = new HttpUtils();

    public BackchannelLogoutRequest(OidcServerConfig oidcServerConfig, String url, String logoutToken) {
        this.oidcServerConfig = (OidcServerConfigImpl) oidcServerConfig;
        this.url = url;
        this.logoutToken = logoutToken;
    }

    public String getUrl() {
        return url;
    }

    public String getLogoutToken() {
        return logoutToken;
    }

    public HttpResponse getResponse() {
        return response;
    }

    @Override
    public BackchannelLogoutRequest call() throws Exception {
        try {
            HttpClient httpClient = createHttpClient();
            if (httpClient == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed to create an HttpClient for back-channel logout request to " + url + " with logout token " + logoutToken);
                }
                return this;
            }
            HttpPost httpPost = createHttpPost();
            response = httpClient.execute(httpPost);
        } catch (Exception e) {
            Tr.error(tc, "ERROR_BUILDING_OR_SENDING_BACKCHANNEL_LOGOUT_REQUEST", oidcServerConfig.getProviderId(), url, e);
            throw e;
        }
        return this;
    }

    HttpClient createHttpClient() {
        SSLSocketFactory sslSocketFactory = null;
        if (url.startsWith("https:")) {
            SSLSupport sslSupportService = oidcServerConfig.getSSLSupportService();
            if (sslSupportService == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "SSLSupport service cannot be found");
                }
                return null;
            }
            sslSocketFactory = sslSupportService.getSSLSocketFactory();
        }
        // TODO - host name verification?
        return httpUtils.createHttpClient(sslSocketFactory, url, false);
    }

    HttpPost createHttpPost() {
        HttpPost httpPost = httpUtils.createHttpPostMethod(url, null);
        HttpEntity entity = getHttpEntity();
        httpPost.setEntity(entity);
        return httpPost;
    }

    private HttpEntity getHttpEntity() {
        List<NameValuePair> parameters = new ArrayList<>();
        parameters.add(new BasicNameValuePair(LOGOUT_TOKEN_PARAM_NAME, logoutToken));
        return new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
    }

}