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
import com.ibm.ws.common.internal.encoder.Base64Coder;

public class OpenShiftUserApiUtilsIntrospect {

    public static final TraceComponent tc = Tr.register(OpenShiftUserApiUtilsIntrospect.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    Oauth2LoginConfigImpl config = null;

    HttpUtils httpUtils = new HttpUtils();

    public OpenShiftUserApiUtilsIntrospect(Oauth2LoginConfigImpl config) {
        this.config = config;
    }

    public String getUserApiResponse(@Sensitive String accessToken, SSLSocketFactory sslSocketFactory) throws SocialLoginException {
        String response = null;
        try {
            HttpURLConnection connection = sendUserApiRequest(accessToken, sslSocketFactory);
            response = readUserApiResponse(connection);
        } catch (Exception e) {
//            throw new SocialLoginException("KUBERNETES_ERROR_GETTING_USER_INFO", e, new Object[] { e });
        }
        return response;
    }

    HttpURLConnection sendUserApiRequest(@Sensitive String accessToken, SSLSocketFactory sslSocketFactory) throws IOException, SocialLoginException {
        HttpURLConnection connection = httpUtils.createConnection(HttpUtils.RequestMethod.POST, config.getUserApi(), sslSocketFactory);
        connection = httpUtils.setHeaders(connection, getUserApiRequestHeaders());
        connection.setDoOutput(true);
        
        OutputStream outputStream = connection.getOutputStream();
        String postData = "token=" + accessToken;
        outputStream.write(postData.getBytes());
        outputStream.close();
        connection.connect();
        return connection;
    }

    @Sensitive
    Map<String, String> getUserApiRequestHeaders() {
        Map<String, String> headers = new HashMap<String, String>();
        String idAndSecretEncoded = Base64Coder.base64Encode(config.getClientId() + ":" + config.getClientSecret());
                //b64encoder.encodeToString((config.getClientId() + ":" + config.getClientSecret()).getBytes());  
        headers.put("Authorization", "Basic " + idAndSecretEncoded);
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        return headers;
    }

    String readUserApiResponse(HttpURLConnection connection) throws IOException, SocialLoginException, JoseException {
        int responseCode = connection.getResponseCode();
        String response = httpUtils.readConnectionResponse(connection);
        if (responseCode != HttpServletResponse.SC_OK) {
            throw new SocialLoginException("KUBERNETES_USER_API_BAD_STATUS", null, new Object[] { responseCode, response });
        }
        return modifyExistingResponseToJSON(response);
    }

    String modifyExistingResponseToJSON(String response) throws JoseException, SocialLoginException {
        JsonObject jsonResponse = getJsonResponseIfValid(response);
        if(jsonResponse.getBoolean("active")) {
            return jsonResponse.toString();
        }
        else {
            throw new SocialLoginException("INTROSPECT_USER_API_ACTIVE_NOT_TRUE",null, null);
        }
//        JsonObject statusInnerMap = getActiveJsonObjectFromResponse(jsonResponse);
//        JsonObject userInnerMap = getUserJsonObjectFromResponse(statusInnerMap);
        
      
    }

    private JsonObject getJsonResponseIfValid(String response) throws SocialLoginException {
        if (response == null || response.isEmpty()) {
            throw new SocialLoginException("INTROSPECT_USER_API_RESPONSE_NULL_EMPTY", null, null);
        }
        try {
            return Json.createReader(new StringReader(response)).readObject();
        } catch (JsonParsingException e) {
            throw new SocialLoginException("INTROSPECT_USER_API_RESPONSE_NOT_JSON", null, new Object[] { response, e });
        }
    }

//    String createModifiedResponse(JsonObject activeInnerMap) throws SocialLoginException {
//        JsonObjectBuilder modifiedResponse = Json.createObjectBuilder();
////        if ("email".equals(config.getUserNameAttribute())) {
////            addUserAttributeToResponseWithEmail(userInnerMap, modifiedResponse);
////        } else {
////            addUserToResponseWithoutEmail(userInnerMap, modifiedResponse);
////        }
////        addGroupNameToResponse(userInnerMap, modifiedResponse);
//        modifiedResponse.add("active", activeInnerMap.get("active"));
//        return modifiedResponse.build().toString();
//    }

//    void addGroupNameToResponse(JsonObject userInnerMap, JsonObjectBuilder modifiedResponse) throws SocialLoginException {
//        if (userInnerMap.containsKey(config.getGroupNameAttribute())) {
//            JsonValue groupsValue = userInnerMap.get(config.getGroupNameAttribute());
//            if (groupsValue.getValueType() != ValueType.ARRAY) {
//                throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_WRONG_JSON_TYPE", null, new Object[] { config.getGroupNameAttribute(), ValueType.ARRAY, groupsValue.getValueType(), userInnerMap });
//            }
//            modifiedResponse.add(config.getGroupNameAttribute(), userInnerMap.getJsonArray(config.getGroupNameAttribute()));
//        }
//    }

//    void addUserToResponseWithoutEmail(JsonObject userInnerMap, JsonObjectBuilder modifiedResponse) throws SocialLoginException {
//        if (userInnerMap.containsKey(config.getUserNameAttribute())) {
//            JsonValue userInnerMapUsername = userInnerMap.get(config.getUserNameAttribute());
//            if (userInnerMapUsername.getValueType() != ValueType.STRING) {
//                throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_WRONG_JSON_TYPE", null, new Object[] { config.getUserNameAttribute(), ValueType.STRING, userInnerMapUsername.getValueType(), userInnerMap });
//            }
//            modifiedResponse.add(config.getUserNameAttribute(), userInnerMap.getString(config.getUserNameAttribute()));
//        } else {
//            throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_MISSING_KEY", null, new Object[] { config.getUserNameAttribute(), userInnerMap });
//        }
//    }

//    void addUserAttributeToResponseWithEmail(JsonObject userInnerMap, JsonObjectBuilder modifiedResponse) throws SocialLoginException {
//        if (userInnerMap.containsKey("email")) {
//            JsonValue emailJsonString = userInnerMap.get("email");
//            if (emailJsonString.getValueType() != ValueType.STRING) {
//                throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_WRONG_JSON_TYPE", null, new Object[] { "email", ValueType.STRING, emailJsonString.getValueType(), userInnerMap });
//            }
//            modifiedResponse.add(config.getUserNameAttribute(), userInnerMap.getString("email"));
//        } else {
//            String defaultKey = "username";
//            Tr.warning(tc, "KUBERNETES_USER_API_RESPONSE_DEFAULT_USER_ATTR_NOT_FOUND", config.getUniqueId(), "email", Oauth2LoginConfigImpl.KEY_userNameAttribute, defaultKey);
//            if (userInnerMap.containsKey(defaultKey)) {
//                modifiedResponse.add(config.getUserNameAttribute(), userInnerMap.getString(defaultKey));
//            } else {
//                throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_MISSING_KEY", null, new Object[] { defaultKey, userInnerMap });
//            }
//        }
//    }

//    JsonObject getUserJsonObjectFromResponse(JsonObject statusResponse) throws SocialLoginException {
//        if (statusResponse.containsKey("error")) {
//            throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_ERROR", null, new Object[] { statusResponse.get("error") });
//        }
//        if (statusResponse.containsKey("user")) {
//            JsonValue userInnerMapValue = statusResponse.get("user");
//            if (userInnerMapValue.getValueType() != ValueType.OBJECT) {
//                throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_WRONG_JSON_TYPE", null, new Object[] { "user", ValueType.OBJECT, userInnerMapValue.getValueType(), statusResponse });
//            } else {
//                return statusResponse.getJsonObject("user");
//            }
//        } else {
//            throw new SocialLoginException("KUBERNETES_USER_API_RESPONSE_MISSING_KEY", null, new Object[] { "user", statusResponse });
//        }
//    }

    JsonObject getActiveJsonObjectFromResponse(JsonObject currentResponse) throws SocialLoginException {
        if (currentResponse.containsKey("active")) {
            JsonValue statusValue = currentResponse.get("active");
            if (ValueType.FALSE == statusValue.getValueType()) {
                throw new SocialLoginException(""+ currentResponse.getBoolean("active"), null, null);
            }
            if (statusValue.getValueType() != ValueType.OBJECT) {
                throw new SocialLoginException("INTROSPECT_USER_API_RESPONSE_WRONG_JSON_TYPE", null, new Object[] { "status", ValueType.OBJECT, statusValue.getValueType(), currentResponse });
            }
            return currentResponse.getJsonObject("active");
        } else {
            throw new SocialLoginException("INTROSPECT_USER_API_RESPONSE_MISSING_KEY", null, new Object[] { "status", currentResponse });
        }

    }

}
