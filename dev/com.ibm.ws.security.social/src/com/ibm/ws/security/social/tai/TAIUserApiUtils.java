/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.tai;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.http.HttpUtils;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.UserApiConfig;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.LinkedinLoginConfigImpl;
import com.ibm.ws.security.social.internal.OpenShiftLoginConfigImpl;
import com.ibm.ws.security.social.internal.utils.OAuthClientUtil;

public class TAIUserApiUtils {

    public static final TraceComponent tc = Tr.register(TAIUserApiUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    HttpUtils httpUtils = new HttpUtils();

    @FFDCIgnore(SocialLoginException.class)
    public String getUserApiResponse(OAuthClientUtil clientUtil, SocialLoginConfig clientConfig, @Sensitive String accessToken, SSLSocketFactory sslSocketFactory) {
        UserApiConfig[] userinfoCfg = clientConfig.getUserApis();
        if (userinfoCfg == null || userinfoCfg.length == 0) {
            Tr.warning(tc, "NO_USER_API_CONFIGS_PRESENT", new Object[] { clientConfig.getUniqueId() });
            return null;
        }
        UserApiConfig userApiConfig = userinfoCfg[0];
        String userinfoApi = userApiConfig.getApi();
        try {
            if (clientConfig instanceof OpenShiftLoginConfigImpl) {
                return getUserApiResponseFromOpenShift(userinfoApi, (OpenShiftLoginConfigImpl) clientConfig, accessToken, sslSocketFactory);
            }
            String userApiResp = clientUtil.getUserApiResponse(userinfoApi,
                    accessToken,
                    sslSocketFactory,
                    false,
                    clientConfig.getUserApiNeedsSpecialHeader(),
                    clientConfig.getUseSystemPropertiesForHttpClientConnections());
            if (clientConfig instanceof LinkedinLoginConfigImpl) {
                return convertLinkedinToJson(userApiResp, clientConfig.getUserNameAttribute());
            }
            if (userApiResp != null && userApiResp.startsWith("[") && userApiResp.endsWith("]")) {
                return convertToJson(userApiResp, clientConfig.getUserNameAttribute());
            } else {
                return userApiResp;
            }
        } catch (SocialLoginException e) {
            Tr.warning(tc, "ERROR_GETTING_USER_API_RESPONSE", new Object[] { userinfoApi, clientConfig.getUniqueId(), e.getLocalizedMessage() });
            return null;
        } catch (Exception e) {
            Tr.warning(tc, "ERROR_GETTING_USER_API_RESPONSE", new Object[] { userinfoApi, clientConfig.getUniqueId(), e.getLocalizedMessage() });
            return null;
        }
    }

    private String getUserApiResponseFromOpenShift(String userinfoApi, OpenShiftLoginConfigImpl config, @Sensitive String accessToken, SSLSocketFactory sslSocketFactory) {
        String response = null;
        try {
            HttpURLConnection connection = httpUtils.createConnection(HttpUtils.RequestMethod.POST, userinfoApi, sslSocketFactory);
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Bearer " + config.getServiceAccountToken());
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");

            OutputStream outputStream = connection.getOutputStream();
            OutputStreamWriter streamWriter = new OutputStreamWriter(outputStream, "UTF-8");

            JsonObjectBuilder bodyBuilder = Json.createObjectBuilder();
            bodyBuilder.add("kind", "TokenReview");
            bodyBuilder.add("apiVersion", "authentication.k8s.io/v1");
            bodyBuilder.add("spec", Json.createObjectBuilder().add("token", accessToken));
            String bodyString = bodyBuilder.build().toString();
            System.out.println("AYOHO Writing body [" + bodyString + "]");
            streamWriter.write(bodyString);
            // TODO
            streamWriter.close();
            outputStream.close();
            connection.connect();

            int responseCode = connection.getResponseCode();
            response = httpUtils.readConnectionResponse(connection);
            System.out.println("AYOHO Response [" + responseCode + "]: [" + response + "]");
            if (responseCode != HttpServletResponse.SC_CREATED) {
                // TODO - error condition
            }
            response = response.replaceFirst("^\\{", "{\"username\":\"ayoho-edited-username\",");
            System.out.println("AYOHO Edited response: [" + response + "]");
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
        }
        return response;
    }

    // flatten linkedin's json 
    // in: {"elements":[{"handle~":{"emailAddress":"abcde@gmail.com"},"handle":"urn:li:emailAddress:688645328"}]}
    // out: {"emailAddress":"abcde@gmail.com"};
    private String convertLinkedinToJson(String resp, String usernameattr) {
        int end = 0;
        int begin = resp.indexOf(usernameattr) - 1;
        if (begin > 0) {
            end = resp.indexOf("}", begin);
            return resp.substring(begin - 1, end + 1);
        }
        return null;
    }

    private String convertToJson(String userApiResp, String usernameattr) {
        //String key = userinfoApi.substring(userinfoApi.lastIndexOf("/") + 1);
        StringBuffer sb = new StringBuffer();
        sb.append("{\"").append(usernameattr).append("\":").append(userApiResp).append("}");
        return sb.toString();
    }

}
