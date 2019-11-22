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
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonParsingException;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.HttpServletResponse;

import org.jose4j.lang.JoseException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.common.http.HttpUtils;
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
            throw new SocialLoginException("KUBERNETES_ERROR_GETTING_USER_INFO", e, new Object[] { e });
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
            throw new SocialLoginException("KUBERNETES_ACCESS_TOKEN_MISSING", null, null);
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
            throw new SocialLoginException("KUBERNETES_USER_API_BAD_STATUS", null, new Object[] { responseCode, response });
        }
        return modifyExistingResponseToJSON(response);
    }

    String modifyExistingResponseToJSON(String response) throws JoseException, SocialLoginException {

        if (response == null || response.isEmpty()) {
            throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_NULL_EMPTY", null, null);
        }

        JsonObject jsonResponse;
        try {
            jsonResponse = Json.createReader(new StringReader(response)).readObject();
        } catch (JsonParsingException e) {
            throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_NOT_JSON", null, new Object[] { response, e });
        }

        JsonObject statusInnerMap, userInnerMap;
        JsonObjectBuilder modifiedResponse = Json.createObjectBuilder();
        if (jsonResponse.containsKey("status")) {
            JsonValue statusValue = jsonResponse.get("status");
            if (ValueType.STRING == statusValue.getValueType()) {
                if (jsonResponse.getString("status").equals("Failure")) {
                    throw new SocialLoginException(jsonResponse.getString("message"), null, null);
                }
            }
            statusInnerMap = jsonResponse.getJsonObject("status");
        } else {
            throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_MISSING_KEY", null, new Object[] { "status", jsonResponse });
        }

        if (statusInnerMap.containsKey("user")) {

            JsonValue userInnerMapValue = statusInnerMap.get("user");
            if (userInnerMapValue.getValueType() != ValueType.OBJECT) {
                throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_WRONG_JSON_TYPE", null, new Object[] { ValueType.OBJECT, userInnerMapValue.getValueType(), jsonResponse });
            } else {
                if (statusInnerMap.getJsonObject("user").isEmpty()) {
                    throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_USER_EMPTY", null, null);
                }
                userInnerMap = statusInnerMap.getJsonObject("user");
            }
        } else {
            throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_MISSING_KEY", null, new Object[] { "user", jsonResponse });
        }

        if (config.getUserNameAttribute().equals("email")) {
            if (userInnerMap.containsKey("email")) {
                JsonValue emailJsonString = userInnerMap.get("email");
                if (emailJsonString.getValueType() != ValueType.STRING) {
                    throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_WRONG_JSON_TYPE", null, new Object[] { ValueType.STRING, emailJsonString.getValueType(), jsonResponse });
                }
                modifiedResponse.add(config.getUserNameAttribute(), userInnerMap.getString("email"));
            } else {
                Tr.warning(tc, "KUBERNETES_DEFAULT_ENTRY_NOT_FOUND");
                if (userInnerMap.containsKey("username")) {
                    modifiedResponse.add(config.getUserNameAttribute(), userInnerMap.getString("username"));
                } else {
                    throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_MISSING_KEY", null, new Object[] { "username", jsonResponse });
                }
            }
        } else {
            if (userInnerMap.containsKey(config.getUserNameAttribute())) {
                JsonValue userInnerMapUsername = userInnerMap.get(config.getUserNameAttribute());
                if (userInnerMapUsername.getValueType() != ValueType.STRING) {
                    throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_WRONG_JSON_TYPE", null, new Object[] { ValueType.STRING, userInnerMapUsername.getValueType(), jsonResponse });
                }
                modifiedResponse.add(config.getUserNameAttribute(), userInnerMap.getString(config.getUserNameAttribute()));
            } else {
                throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_MISSING_KEY", null, new Object[] { config.getUserNameAttribute(), jsonResponse });
            }
        }

        if (statusInnerMap.containsKey("error")) {
            throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_ERROR", null, new Object[] { jsonResponse });
        }

        if (userInnerMap.containsKey(config.getGroupNameAttribute())) {
            JsonValue groupsValue = userInnerMap.get(config.getGroupNameAttribute());
            if (groupsValue.getValueType() != ValueType.ARRAY) {
                throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_WRONG_JSON_TYPE", null, new Object[] { ValueType.ARRAY, groupsValue.getValueType(), jsonResponse });
            }
            modifiedResponse.add(config.getGroupNameAttribute(), userInnerMap.getJsonArray("groups"));

        }
        return modifiedResponse.build().toString();
    }

}
