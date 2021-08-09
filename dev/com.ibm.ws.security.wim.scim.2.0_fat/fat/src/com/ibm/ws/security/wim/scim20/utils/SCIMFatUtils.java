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

package com.ibm.ws.security.wim.scim20.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

public class SCIMFatUtils {

    public static final String USERS_API_URL = "/ibm/api/scim/Users";
    public static final String GROUPS_API_URL = "/ibm/api/scim/Groups";

    public static StringBuilder callURL(String apiURL, int expectedResp, int[] allowedUnexpectedResp, HTTPRequestMethod reqMethod, InputStream body, String conttype,
                                        String userName, String password) throws Exception {
        URL url = new URL(apiURL);

        Map<String, String> reqHeader = new HashMap<String, String>();

        // Put content-Type in HTTP request header
        if (conttype != null) {
            reqHeader.put("Content-Type", conttype);
        } else {
            reqHeader.put("Content-Type", "application/x-www-form-urlencoded");
        }

        // Put Authorization credentials. Use basic authentication.
        if (userName != null && password != null) {
            String userNamePassword = Base64.getEncoder().encodeToString((userName + ":" + password).getBytes());
            reqHeader.put("Authorization", "Basic " + userNamePassword);
        }

        HttpUtils.trustAllCertificates();
        HttpURLConnection conn = null;
        conn = HttpUtils.getHttpConnection(url, expectedResp, allowedUnexpectedResp, 5, reqMethod, reqHeader, body);
        BufferedReader br = null;
        StringBuilder outputBuilder = new StringBuilder();
        if (expectedResp == 200 || expectedResp == 201) {
            br = HttpUtils.getConnectionStream(conn);
        } else if (expectedResp == 403 || expectedResp == 404 || expectedResp == 500 || expectedResp == 409 || expectedResp == 400) {
            try {
                br = HttpUtils.getErrorStream(conn);
            } catch (NullPointerException e) {
            }
        } else if (expectedResp == 401) {
            return outputBuilder.append("401");
        }

        String line;
        while (br != null && (line = br.readLine()) != null) {
            outputBuilder.append(line);
        }

        conn.disconnect();
        return outputBuilder;
    }
}
