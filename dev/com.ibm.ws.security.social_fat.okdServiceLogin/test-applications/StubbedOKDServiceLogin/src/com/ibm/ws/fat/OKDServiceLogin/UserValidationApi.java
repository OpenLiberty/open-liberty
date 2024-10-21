/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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
package com.ibm.ws.fat.OKDServiceLogin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class UserValidationApi extends HttpServlet {
    private static final long serialVersionUID = 1L;

    Subject runAsSubject = null;
    static StringBuffer sb = null;

    public UserValidationApi() {
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
        System.out.println("Got into server side test app");
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
                if ("Authorization".equalsIgnoreCase(key)) {

                    passedResponse = val.substring("Bearer ".length());
                    printJson(passedResponse);
                    logLine("Found value for token: " + passedResponse);
                }
            }
            Enumeration<String> params = req.getParameterNames();
            while (params.hasMoreElements()) {
                String paramName = params.nextElement();
                logLine("Parameter Name - " + paramName + ", Value - " + req.getParameter(paramName));
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String response = null;
        switch (passedResponse) {
        case "badServiceAccountToken":
            System.out.println("badServiceAccountToken");
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response = "{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"metadata\":{},\"status\":\"Failure\",\"message\":\"Unauthorized\",\"reason\":\"Unauthorized\",\"code\":401}";
            break;
        case " ":
            System.out.println("blankServiceAccountToken");
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response = "{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"metadata\":{},\"status\":\"Failure\",\"message\":\"users.user.openshift.io \\\"~\\\" is forbidden: User \\\"system:anonymous\\\" cannot get users.user.openshift.io at the cluster scope: no RBAC policy matched\",\"reason\":\"Forbidden\",\"details\":{\"name\":\"~\",\"group\":\"user.openshift.io\",\"kind\":\"users\"},\"code\":403}";
            break;
        case "":
            System.out.println("emptyServiceAccountToken");
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response = "{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"metadata\":{},\"status\":\"Failure\",\"message\":\"users.user.openshift.io \\\"~\\\" is forbidden: User \\\"system:anonymous\\\" cannot get users.user.openshift.io at the cluster scope: no RBAC policy matched\",\"reason\":\"Forbidden\",\"details\":{\"name\":\"~\",\"group\":\"user.openshift.io\",\"kind\":\"users\"},\"code\":403}";
            break;
        default:
            System.out.println("Print what was passed");
            resp.setStatus(HttpServletResponse.SC_OK);
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

    private void printJson(String tokenString) {

        try {
            JSONObject jObject = new JSONObject().parse(tokenString);

            printJson("", jObject);
        } catch (Exception e) {

        }

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
