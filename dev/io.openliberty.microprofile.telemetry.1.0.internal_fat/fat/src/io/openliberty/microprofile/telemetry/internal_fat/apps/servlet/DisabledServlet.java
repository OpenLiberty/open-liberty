/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.servlet;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/testDisabledHttpTrace")
public class DisabledServlet extends FATServlet {

    public static final String APP_NAME = "TelemetryServletTestApp";
    public static final String SIMPLE_SERVLET = "simple";
    public static final String SIMPLE_ASYNC_SERVLET = "simpleAsync";
    public static final String CONTEXT_ASYNC_SERVLET = "contextAsync";
    public static final String PLACEHOLDER_SERVLET = "placeholder";
    public static final String HELLO_HTML = "hello.html";
    public static final String DICE_JSP = "dice.jsp";
    public static final String INVALID_TRACE_ID = "00000000000000000000000000000000";

    @Inject
    private HttpServletRequest request;

    @Test
    public void testSimpleServlet() throws Exception {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        URL url = new URL(scheme + "://" + serverName + ":" + serverPort + "/" + APP_NAME + "/" + SIMPLE_SERVLET);

        String traceId = httpGet(url); // The servlet outputs the traceId

        // The simple servlet will return the default invalid traceId
        assertEquals(traceId, INVALID_TRACE_ID);

    }

    private String httpGet(URL url) throws IOException {
        HttpURLConnection connection = null;
        StringBuffer content = new StringBuffer();
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            assertEquals(200, connection.getResponseCode());
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return content.toString();
    }

}