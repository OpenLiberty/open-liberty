/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.HttpServletResponse;

import org.jose4j.lang.JoseException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.common.http.HttpUtils;
import com.ibm.ws.security.common.jwk.utils.JsonUtils;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.Oauth2LoginConfigImpl;

public class OpenShiftUserApiUtils {

    public static final TraceComponent tc = Tr.register(OpenShiftUserApiUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    Oauth2LoginConfigImpl config = null;

    HttpUtils httpUtils = new HttpUtils();

    public OpenShiftUserApiUtils(Oauth2LoginConfigImpl config) {
        this.config = config;
    }

    public String getUserApiResponse(@Sensitive String accessToken, SSLSocketFactory sslSocketFactory) throws SocialLoginException {
        String response = null;
        try {
            HttpURLConnection connection = sendUserApiRequest(accessToken, sslSocketFactory);
            response = readUserApiResponse(connection);
        } catch (Exception e) {
            throw new SocialLoginException("OPENSHIFT_ERROR_GETTING_USER_INFO", e, new Object[] { e });
        }
        return response;
    }

    HttpURLConnection sendUserApiRequest(@Sensitive String accessToken, SSLSocketFactory sslSocketFactory) throws IOException, SocialLoginException {
        HttpURLConnection connection = httpUtils.createConnection(HttpUtils.RequestMethod.POST, config.getUserApi(), sslSocketFactory);
        connection = httpUtils.setHeaders(connection, getUserApiRequestHeaders());
        connection.setDoOutput(true);

        OutputStream outputStream = connection.getOutputStream();
        OutputStreamWriter streamWriter = new OutputStreamWriter(outputStream, "UTF-8");

        String bodyString = createUserApiRequestBody(accessToken);
        streamWriter.write(bodyString);
        streamWriter.close();
        outputStream.close();
        connection.connect();
        return connection;
    }

    @Sensitive
    Map<String, String> getUserApiRequestHeaders() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + config.getUserApiToken());
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        return headers;
    }

    String createUserApiRequestBody(@Sensitive String accessToken) throws SocialLoginException {
        if (accessToken == null) {
            throw new SocialLoginException("OPENSHIFT_ACCESS_TOKEN_MISSING", null, null);
        }
        JsonObjectBuilder bodyBuilder = Json.createObjectBuilder();
        bodyBuilder.add("kind", "TokenReview");
        bodyBuilder.add("apiVersion", "authentication.k8s.io/v1");
        bodyBuilder.add("spec", Json.createObjectBuilder().add("token", accessToken));
        return bodyBuilder.build().toString();
    }

    String readUserApiResponse(HttpURLConnection connection) throws IOException, SocialLoginException, JoseException {
        int responseCode = connection.getResponseCode();
        String response = httpUtils.readConnectionResponse(connection);
        if (responseCode != HttpServletResponse.SC_CREATED) {
            throw new SocialLoginException("OPENSHIFT_USER_API_BAD_STATUS", null, new Object[] { responseCode, response });
        }
        return modifyExistingResponseToJSON(response);
    }

    private String modifyExistingResponseToJSON(String response) throws JoseException {

        String jsonFormatResponse = JsonUtils.toJson(response);

        Map<?, ?> firstMap = JsonUtils.claimsFromJsonObject(jsonFormatResponse);

        Map<?, ?> statusInnerMap = (LinkedHashMap<?, ?>) firstMap.get("status");

        Map<?, ?> userInnerMap = (LinkedHashMap<?, ?>) statusInnerMap.get("user");

        List<?> groupList = (ArrayList<?>) userInnerMap.get("groups");

        StringBuilder correct = new StringBuilder("{\"username\":\"" + userInnerMap.get("username") + "\",");

        StringBuilder buildArray = new StringBuilder("\"groups\":[");

        for (int i = 0; i < groupList.size(); i++) {

            if (i == groupList.size() - 1) {

                buildArray.append("\"" + groupList.get(i) + "\"" + "]}");

            }

            else {

                buildArray.append("\"" + groupList.get(i) + "\"" + ",");

            }

        }

        String current = correct.append(buildArray).toString();

        return current;

    }

}
