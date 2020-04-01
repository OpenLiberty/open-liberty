/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonParsingException;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.http.HttpUtils;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.Oauth2LoginConfigImpl;

public class OpenShiftUserApiUtils {

    public static final TraceComponent tc = Tr.register(OpenShiftUserApiUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    SocialLoginConfig config = null;

    HttpUtils httpUtils = new HttpUtils();

    public OpenShiftUserApiUtils(SocialLoginConfig config) {
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

    public String getUserApiResponseForServiceAccountToken(@Sensitive String serviceAccountToken, SSLSocketFactory sslSocketFactory) throws SocialLoginException {
        try {
            HttpURLConnection connection = sendServiceAccountIntrospectRequest(serviceAccountToken, sslSocketFactory);
            return readServiceAccountIntrospectResponse(connection);
        } catch (Exception e) {
            throw new SocialLoginException("ERROR_INTROSPECTING_SERVICE_ACCOUNT", e, new Object[] { e });
        }
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

    HttpURLConnection sendServiceAccountIntrospectRequest(@Sensitive String serviceAccountToken, SSLSocketFactory sslSocketFactory) throws IOException {
        HttpURLConnection connection = httpUtils.createConnection(HttpUtils.RequestMethod.GET, config.getUserApi(), sslSocketFactory);
        connection = httpUtils.setHeaders(connection, getServiceAccountIntrospectRequestHeaders(serviceAccountToken));
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

    @Sensitive
    Map<String, String> getServiceAccountIntrospectRequestHeaders(@Sensitive String serviceAccountToken) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + serviceAccountToken);
        headers.put("Accept", "application/json");
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

    String readUserApiResponse(HttpURLConnection connection) throws IOException, SocialLoginException {
        int responseCode = connection.getResponseCode();
        String response = httpUtils.readConnectionResponse(connection);
        if (responseCode != HttpServletResponse.SC_CREATED) {
            throw new SocialLoginException("KUBERNETES_USER_API_BAD_STATUS", null, new Object[] { responseCode, response });
        }
        return modifyExistingResponseToJSON(response);
    }

    String readServiceAccountIntrospectResponse(HttpURLConnection connection) throws IOException, SocialLoginException {
        int responseCode = connection.getResponseCode();
        String response = httpUtils.readConnectionResponse(connection);
        if (responseCode != HttpServletResponse.SC_OK) {
            throw new SocialLoginException("USER_API_RESPONSE_BAD_STATUS", null, new Object[] { responseCode, response });
        }
        return processServiceAccountIntrospectResponse(response);
    }

    String modifyExistingResponseToJSON(String response) throws SocialLoginException {
        JsonObject jsonResponse = getJsonResponseIfValid(response);
        JsonObject statusInnerMap = getStatusJsonObjectFromResponse(jsonResponse);
        JsonObject userInnerMap = getUserJsonObjectFromResponse(statusInnerMap);
        return createModifiedResponse(userInnerMap);
    }

    private JsonObject getJsonResponseIfValid(String response) throws SocialLoginException {
        if (response == null || response.isEmpty()) {
            throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_NULL_EMPTY", null, null);
        }
        try {
            return Json.createReader(new StringReader(response)).readObject();
        } catch (JsonParsingException e) {
            throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_NOT_JSON", null, new Object[] { response, e });
        }
    }

    String createModifiedResponse(JsonObject userInnerMap) throws SocialLoginException {
        JsonObjectBuilder modifiedResponse = Json.createObjectBuilder();
        if ("email".equals(config.getUserNameAttribute())) {
            addUserAttributeToResponseWithEmail(userInnerMap, modifiedResponse);
        } else {
            addUserToResponseWithoutEmail(userInnerMap, modifiedResponse);
        }
        addGroupNameToResponse(userInnerMap, modifiedResponse);
        return modifiedResponse.build().toString();
    }

    void addGroupNameToResponse(JsonObject userInnerMap, JsonObjectBuilder modifiedResponse) throws SocialLoginException {
        if (userInnerMap.containsKey(config.getGroupNameAttribute())) {
            JsonValue groupsValue = userInnerMap.get(config.getGroupNameAttribute());
            if (groupsValue.getValueType() != ValueType.ARRAY) {
                throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_WRONG_JSON_TYPE", null, new Object[] { config.getGroupNameAttribute(), ValueType.ARRAY, groupsValue.getValueType(), userInnerMap });
            }
            modifiedResponse.add(config.getGroupNameAttribute(), userInnerMap.getJsonArray(config.getGroupNameAttribute()));
        }
    }

    void addUserToResponseWithoutEmail(JsonObject userInnerMap, JsonObjectBuilder modifiedResponse) throws SocialLoginException {
        if (userInnerMap.containsKey(config.getUserNameAttribute())) {
            JsonValue userInnerMapUsername = userInnerMap.get(config.getUserNameAttribute());
            if (userInnerMapUsername.getValueType() != ValueType.STRING) {
                throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_WRONG_JSON_TYPE", null, new Object[] { config.getUserNameAttribute(), ValueType.STRING, userInnerMapUsername.getValueType(), userInnerMap });
            }
            modifiedResponse.add(config.getUserNameAttribute(), userInnerMap.getString(config.getUserNameAttribute()));
        } else {
            throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_MISSING_KEY", null, new Object[] { config.getUserNameAttribute(), userInnerMap });
        }
    }

    void addUserAttributeToResponseWithEmail(JsonObject userInnerMap, JsonObjectBuilder modifiedResponse) throws SocialLoginException {
        if (userInnerMap.containsKey("email")) {
            JsonValue emailJsonString = userInnerMap.get("email");
            if (emailJsonString.getValueType() != ValueType.STRING) {
                throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_WRONG_JSON_TYPE", null, new Object[] { "email", ValueType.STRING, emailJsonString.getValueType(), userInnerMap });
            }
            modifiedResponse.add(config.getUserNameAttribute(), userInnerMap.getString("email"));
        } else {
            String defaultKey = "username";
            Tr.warning(tc, "KUBERNETES_USER_API_RESPONSE_DEFAULT_USER_ATTR_NOT_FOUND", config.getUniqueId(), "email", Oauth2LoginConfigImpl.KEY_userNameAttribute, defaultKey);
            if (userInnerMap.containsKey(defaultKey)) {
                modifiedResponse.add(config.getUserNameAttribute(), userInnerMap.getString(defaultKey));
            } else {
                throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_MISSING_KEY", null, new Object[] { defaultKey, userInnerMap });
            }
        }
    }

    JsonObject getUserJsonObjectFromResponse(JsonObject statusResponse) throws SocialLoginException {
        if (statusResponse.containsKey("error")) {
            throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_ERROR", null, new Object[] { statusResponse.get("error") });
        }
        if (statusResponse.containsKey("user")) {
            JsonValue userInnerMapValue = statusResponse.get("user");
            if (userInnerMapValue.getValueType() != ValueType.OBJECT) {
                throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_WRONG_JSON_TYPE", null, new Object[] { "user", ValueType.OBJECT, userInnerMapValue.getValueType(), statusResponse });
            } else {
                return statusResponse.getJsonObject("user");
            }
        } else {
            throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_MISSING_KEY", null, new Object[] { "user", statusResponse });
        }
    }

    JsonObject getStatusJsonObjectFromResponse(JsonObject currentResponse) throws SocialLoginException {
        if (currentResponse.containsKey("status")) {
            JsonValue statusValue = currentResponse.get("status");
            if (ValueType.STRING == statusValue.getValueType()) {
                if (currentResponse.getString("status").equals("Failure")) {
                    if (currentResponse.containsKey("message") && currentResponse.get("message").getValueType() == ValueType.STRING) {
                        throw new SocialLoginException(currentResponse.getString("message"), null, null);
                    }
                }
            }
            if (statusValue.getValueType() != ValueType.OBJECT) {
                throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_WRONG_JSON_TYPE", null, new Object[] { "status", ValueType.OBJECT, statusValue.getValueType(), currentResponse });
            }
            return currentResponse.getJsonObject("status");
        } else {
            throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_MISSING_KEY", null, new Object[] { "status", currentResponse });
        }

    }

    String processServiceAccountIntrospectResponse(String response) throws SocialLoginException {
        JsonObject jsonResponse = readResponseAsJsonObject(response);
        JsonObject userMetadata = getJsonObjectValueFromJson(jsonResponse, "metadata");
        JsonObject result = modifyUsername(userMetadata);
        result = addProjectNameAsGroup(result);
        return result.toString();
    }

    @FFDCIgnore(JsonParsingException.class)
    JsonObject readResponseAsJsonObject(String response) throws SocialLoginException {
        if (response == null || response.isEmpty()) {
            throw new SocialLoginException("RESPONSE_NOT_JSON", null, new Object[] { response });
        }
        JsonObject jsonResponse;
        try {
            jsonResponse = Json.createReader(new StringReader(response)).readObject();
        } catch (JsonParsingException e) {
            throw new SocialLoginException("RESPONSE_NOT_JSON", null, new Object[] { response, e });
        }
        return jsonResponse;
    }

    JsonObject getJsonObjectValueFromJson(JsonObject json, String key) throws SocialLoginException {
        if (!json.containsKey(key)) {
            throw new SocialLoginException("JSON_MISSING_KEY", null, new Object[] { key, json });
        }
        JsonValue rawValue = json.get(key);
        if (rawValue.getValueType() != ValueType.OBJECT) {
            throw new SocialLoginException("JSON_ENTRY_WRONG_JSON_TYPE", null, new Object[] { key, ValueType.OBJECT, rawValue.getValueType(), json });
        }
        return json.getJsonObject(key);
    }

    String getStringValueFromJson(JsonObject json, String key) throws SocialLoginException {
        if (!json.containsKey(key)) {
            throw new SocialLoginException("JSON_MISSING_KEY", null, new Object[] { key, json });
        }
        JsonValue rawValue = json.get(key);
        if (rawValue.getValueType() != ValueType.STRING) {
            throw new SocialLoginException("JSON_ENTRY_WRONG_JSON_TYPE", null, new Object[] { key, ValueType.STRING, rawValue.getValueType(), json });
        }
        return json.getString(key);
    }

    JsonObject modifyUsername(JsonObject metadataEntry) throws SocialLoginException {
        JsonObject result = metadataEntry;
        String userNameAttribute = config.getUserNameAttribute();
        if (userNameAttribute != null) {
            String username = getStringValueFromJson(metadataEntry, userNameAttribute);
            String serviceAccountPrefix = "system:serviceaccount:";
            if (username.startsWith(serviceAccountPrefix)) {
                username = username.substring(serviceAccountPrefix.length());
            }
            JsonObjectBuilder resultBuilder = copyJsonObject(metadataEntry);
            resultBuilder.add(userNameAttribute, username);
            result = resultBuilder.build();
        }
        return result;
    }
    
    JsonObject addProjectNameAsGroup(JsonObject metadataEntry) throws SocialLoginException {
        JsonObject result = metadataEntry;
        String userNameAttribute = config.getUserNameAttribute();
        String username = getStringValueFromJson(metadataEntry, userNameAttribute);
        int index = username.indexOf(":");        
        if (index >= 0) {
            String group = null;
            group = username.substring(0,index);
            if (group != null && !group.isEmpty()) {
                String groupNameAttribute = config.getGroupNameAttribute();
                if (groupNameAttribute != null) {
                    JsonObjectBuilder resultBuilder = copyJsonObject(metadataEntry);
                    resultBuilder.add(groupNameAttribute, group);
                    result = resultBuilder.build();
                }
            }
        }
        
        return result;
    }

    private JsonObjectBuilder copyJsonObject(JsonObject original) {
        JsonObjectBuilder result = Json.createObjectBuilder();
        for (Entry<String, JsonValue> entry : original.entrySet()) {
            result.add(entry.getKey(), entry.getValue());
        }
        return result;
    }

}
