/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.openidconnect.backchannellogout;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.http.HttpUtils;

public class BackchannelLogoutRequest implements Callable<BackchannelLogoutRequest> {

    private static TraceComponent tc = Tr.register(BackchannelLogoutRequest.class);

    public static final String LOGOUT_TOKEN_PARAM_NAME = "logout_token";

    private final String url;
    private final String logoutToken;
    private CloseableHttpResponse response;

    private final HttpUtils httpUtils = new HttpUtils();

    public BackchannelLogoutRequest(String url, String logoutToken) {
        this.url = url;
        this.logoutToken = logoutToken;
    }

    public String getUrl() {
        return url;
    }

    public String getLogoutToken() {
        return logoutToken;
    }

    public CloseableHttpResponse getResponse() {
        return response;
    }

    @Override
    public BackchannelLogoutRequest call() throws Exception {
        CloseableHttpClient httpClient = null;
        try {
            HttpPost httpPost = httpUtils.createHttpPostMethod(url, null);
            HttpEntity entity = getHttpEntity();
            httpPost.setEntity(entity);
            // TODO
            httpClient = HttpClients.createDefault();
            response = httpClient.execute(httpPost);
        } catch (Exception e) {
            // TODO
            // ERROR_BUILDING_OR_SENDING_BACKCHANNEL_LOGOUT_REQUEST=The OpenID Connect server encountered an error while building or sending a back-channel logout request to {0}: {1}
            throw e;
        } finally {
            if (httpClient != null) {
                httpClient.close();
            }
        }
        return this;
    }

    private HttpEntity getHttpEntity() throws UnsupportedEncodingException {
        List<NameValuePair> parameters = new ArrayList<>();
        parameters.add(new BasicNameValuePair(LOGOUT_TOKEN_PARAM_NAME, logoutToken));
        return new UrlEncodedFormEntity(parameters);
    }

}