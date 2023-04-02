/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.OpenShift;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Scanner;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class OpenShiftTokenReview extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // getInvocationSubject for RunAs tests
    Subject runAsSubject = null;
    static StringBuffer sb = null;

    public OpenShiftTokenReview() {
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleRequest("GET", req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleRequest("POST", req, resp);
    }

    public void handleRequest(String type, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        PrintWriter writer = resp.getWriter();
        logLine("HandleRequest Request type: " + type);
        String body = null;
        String passedResponse = null;
        try {
            logLine("Got into the stub");
            Enumeration<String> headerList = req.getHeaderNames();
            while (headerList.hasMoreElements()) {
                String key = headerList.nextElement();
                String val = req.getHeader(key);
                logLine("Header element: " + key + " value: " + val);
            }
            Enumeration<String> params = req.getParameterNames();
            while (params.hasMoreElements()) {
                String paramName = params.nextElement();
                logLine("Parameter Name - " + paramName + ", Value - " + req.getParameter(paramName));
            }

            if ("POST".equalsIgnoreCase(req.getMethod())) {
                Scanner s = null;
                try {
                    s = new Scanner(req.getInputStream(), "UTF-8").useDelimiter("\\A");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                body = s.hasNext() ? s.next() : "";
                logLine("Body: " + body);

                JSONObject bodyJson = new JSONObject().parse(body);
                printJson(bodyJson);

                passedResponse = getActionFlag(bodyJson);
                logLine("Found value for token: " + passedResponse);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        String response = null;
        switch (passedResponse) {
        case "badServiceAccountToken":
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response = "403";
            break;
        default:
            resp.setStatus(HttpServletResponse.SC_CREATED);
            response = passedResponse;
        }
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        writer.write(response);

        writer.flush();
        writer.close();

    }

    public static void logLine(String msg) {
        System.out.println(msg);
    }

    private void printJson(JSONObject jObject) {
        printJson("", jObject);

    }

    private void printJson(String indent, JSONObject jObject) {

        for (Object key : jObject.keySet()) {

            String keyStr = (String) key;
            Object keyvalue = jObject.get(keyStr);

            if (keyvalue instanceof JSONObject) {
                logLine(indent + "Key: " + key + " Value: " + keyvalue.toString());
                printJson(indent + "  ", (JSONObject) keyvalue);
            } else if (keyvalue instanceof JSONArray) {
                // skipping for now
            } else {
                logLine(indent + "Key: " + key + " Value: " + keyvalue);
            }
        }

    }

    private String getActionFlag(JSONObject body) {

        Object spec = body.get("spec");
        if (spec != null) {
            Object action = ((JSONObject) spec).get("token");
            if (action instanceof String) {
                return (String) action;
            }
        }
        return null;
    }

}