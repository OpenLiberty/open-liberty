/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.MessageBodyWebRequest;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.PutMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

/**
 * Test class for app-password and app-token endpoint test tools
 *
 * @author chrisc
 *
 */
public class ClientRegistrationTools {

    private static final Class<?> thisClass = ClientRegistrationTools.class;

    public static EndpointSettings eSettings = new EndpointSettings();
    public static CommonTestTools cttools = new CommonTestTools();
    public static CommonMessageTools msgUtils = new CommonMessageTools();
    public static CommonTestHelpers helpers = new CommonTestHelpers();
    public static ValidationData vData = new ValidationData();

    protected endpointSettings createBasicAuthenticationHeader(String clientID, String clientSecret) throws Exception {
        String basicAuth = cttools.buildBasicAuthCred(clientID, clientSecret);
        return new EndpointSettings().new endpointSettings("Authorization", basicAuth);
    }

    public List<endpointSettings> setRequestHeaders(TestSettings settings) throws Exception {
        List<endpointSettings> headers = new ArrayList<EndpointSettings.endpointSettings>();
        endpointSettings authorization = createBasicAuthenticationHeader(settings.getAdminUser(), settings.getAdminPswd());
        headers = eSettings.addEndpointSettings(headers, "Accept", "application/json");
        headers.add(authorization);
        return headers;
    }

    public JSONObject setRegistrationBody(TestServer opServer, String clientId, String clientSecret, String clientName,
            boolean appPasswordAllowed, boolean appTokenAllowed, String scope, String preauthScope, List<String> grants, List<String> rspTypes, boolean introspect) {
        JSONObject clientData = new JSONObject();

        clientData.put("client_id", clientId);
        clientData.put("client_secret", clientSecret);
        clientData.put("client_name", clientName);

        clientData.put("appPasswordAllowed", appPasswordAllowed);
        clientData.put("appTokenAllowed", appTokenAllowed);

        JSONArray redirectUris = new JSONArray();
        redirectUris.add("http://localhost:" + opServer.getServerHttpPort() + "/oauthclient/redirect.jsp");
        clientData.put("redirect_uris", redirectUris);

        clientData.put("scope", scope);

        JSONArray grantTypes = new JSONArray();
        for (String type : grants) {
            grantTypes.add(type);
        }
        clientData.put("grant_types", grantTypes);

        JSONArray responseTypes = new JSONArray();
        for (String rsp : rspTypes) {
            responseTypes.add(rsp);
        }
        clientData.put("response_types", responseTypes);

        clientData.put("preauthorized_scope", preauthScope);

        clientData.put("introspect_tokens", introspect);

        Log.info(thisClass, "setRegistrationBody", clientData.toString());
        return clientData;

    }

    public void createClientEntries(String registrationEndpt, List<endpointSettings> headers, List<JSONObject> values) throws Exception {

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_REGISTRATION_ENDPOINT, Constants.CREATED_STATUS);

        for (JSONObject value : values) {
            createClientEntry(registrationEndpt, Constants.POSTMETHOD, Constants.INVOKE_REGISTRATION_ENDPOINT, null, headers,
                    expectations, new ByteArrayInputStream(value.serialize().getBytes()), "application/json");
        }
    }

    public void updateClientEntries(String registrationEndpt, List<endpointSettings> headers, List<JSONObject> values) throws Exception {

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_REGISTRATION_ENDPOINT, Constants.CREATED_STATUS);

        for (JSONObject value : values) {
            createClientEntry(registrationEndpt, Constants.PUTMETHOD, Constants.INVOKE_REGISTRATION_ENDPOINT, null, headers,
                    expectations, new ByteArrayInputStream(value.serialize().getBytes()), "application/json");
        }
    }

    public WebResponse createClientEntry(String url, String method, String action, List<endpointSettings> parms, List<endpointSettings> headers, List<validationData> expectations, InputStream source, String contentType) throws Exception {

        WebResponse response = null;
        String thisMethod = "createClientEntry";

        msgUtils.printMethodName(thisMethod);
        msgUtils.printOAuthOidcExpectations(expectations);

        WebConversation wc = new WebConversation();

        MessageBodyWebRequest request = null;
        if (method.equals(Constants.POSTMETHOD)) {
            request = new PostMethodWebRequest(url, source, contentType);
        } else {
            if (method.equals(Constants.PUTMETHOD)) {
                request = new PutMethodWebRequest(url, source, contentType);
            }
        }

        try {

            Log.info(thisClass, thisMethod, "Endpoint URL: " + url);

            if (parms != null) {
                for (endpointSettings parm : parms) {
                    Log.info(thisClass, thisMethod, "Setting request parameter:  key: " + parm.key + " value: " + parm.value);
                    request.setParameter(parm.key, parm.value);
                }
            } else {
                Log.info(thisClass, thisMethod, "No parameters to set");
            }

            if (headers != null) {
                for (endpointSettings header : headers) {
                    Log.info(thisClass, thisMethod, "Setting header field:  key: " + header.key + " value: " + header.value);
                    request.setHeaderField(header.key, header.value);
                }
            } else {
                Log.info(thisClass, thisMethod, "No header fields to add");
            }

            response = wc.getResponse(request);
            msgUtils.printResponseParts(response, thisMethod, "Invoke with Parms and Headers: ");

        } catch (HttpException e) {

            System.err.println("Exception: " + e);

        } catch (Exception e) {
            System.err.println("Exception: " + e);
        }
        return response;
    }
}