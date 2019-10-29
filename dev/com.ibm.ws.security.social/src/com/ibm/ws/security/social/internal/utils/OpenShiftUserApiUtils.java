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

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.common.http.HttpUtils;
import com.ibm.ws.security.common.jwk.utils.JsonUtils;
import com.ibm.ws.security.social.internal.OpenShiftLoginConfigImpl;

public class OpenShiftUserApiUtils {

    OpenShiftLoginConfigImpl config = null;

    HttpUtils httpUtils = new HttpUtils();

    public OpenShiftUserApiUtils(OpenShiftLoginConfigImpl config) {
        this.config = config;
    }

    public String getUserApiResponse(@Sensitive String accessToken, SSLSocketFactory sslSocketFactory) throws IOException, JoseException{
        String response = null;
        try {
            HttpURLConnection connection = httpUtils.createConnection(HttpUtils.RequestMethod.POST, config.getUserApi(), sslSocketFactory);
            connection = httpUtils.setHeaders(connection, getUserApiRequestHeaders());
            connection.setDoOutput(true);

            OutputStream outputStream = connection.getOutputStream();
            OutputStreamWriter streamWriter = new OutputStreamWriter(outputStream, "UTF-8");

            String bodyString = createUserApiRequestBody(accessToken);
            System.out.println("AYOHO Writing body [" + bodyString + "]");
            streamWriter.write(bodyString);
            // TODO
            streamWriter.close();
            outputStream.close();
            connection.connect();

            int responseCode = connection.getResponseCode();
            response = httpUtils.readConnectionResponse(connection);
            // System.out.println("AYOHO Response [" + responseCode + "]: [" + response + "]");
            if (responseCode != HttpServletResponse.SC_CREATED) {
                // TODO - error condition
            }
            // response = response.replaceFirst("^\\{", "{\"username\":\"ayoho-edited-username\",");
            response = modifyExistingResponseToJSON(response);
            // System.out.println("AYOHO response after formatting : [" + response + "]");
        } catch (IOException e) {
            // TODO - error logging
            throw e;
        }  catch (JoseException e) {
            // TODO - error logging
            throw e;
        }
        return response;
    }

    @Sensitive
    private Map<String, String> getUserApiRequestHeaders() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + config.getServiceAccountToken());
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        return headers;
    }

    private String createUserApiRequestBody(@Sensitive String accessToken) {
        JsonObjectBuilder bodyBuilder = Json.createObjectBuilder();
        bodyBuilder.add("kind", "TokenReview");
        bodyBuilder.add("apiVersion", "authentication.k8s.io/v1");
        bodyBuilder.add("spec", Json.createObjectBuilder().add("token", accessToken));
        return bodyBuilder.build().toString();
    }
    
    private String modifyExistingResponseToJSON(String response) throws JoseException{

        String jsonFormatResponse = JsonUtils.toJson(response);

        Map<?, ?> firstMap = JsonUtils.claimsFromJsonObject(jsonFormatResponse);

        Map<?, ?> statusInnerMap = (LinkedHashMap<?, ?>)firstMap.get("status");

        Map<?, ?> userInnerMap = (LinkedHashMap<?, ?>)statusInnerMap.get("user");

       

        List<?> groupList = (ArrayList<?>) userInnerMap.get("groups");

        StringBuilder correct = new StringBuilder("{\"username\":\"" + userInnerMap.get("username")+ "\",");

        StringBuilder buildArray = new StringBuilder("\"groups\":[");

        for(int i=0;i<groupList.size();i++) {

            

            if(i==groupList.size()-1) {

                buildArray.append("\"" + groupList.get(i)+ "\""+ "]}");

            }

            else {

                buildArray.append("\""  + groupList.get(i)+ "\""+ ",");

            }

        }

        String current = correct.append(buildArray).toString();

       

        return current;

    }

}
