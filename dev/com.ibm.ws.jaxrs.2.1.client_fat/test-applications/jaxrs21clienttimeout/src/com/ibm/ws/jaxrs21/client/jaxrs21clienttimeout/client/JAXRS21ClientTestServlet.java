/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.client.jaxrs21clienttimeout.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

/**
 *
 */
@WebServlet("/JAXRS21ClientTestServlet")
public class JAXRS21ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = 7188707949976646396L;

    private static final String moduleName = "jaxrs21clienttimeout";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        PrintWriter pw = resp.getWriter();

        String testMethod = req.getParameter("test");
        if (testMethod == null) {
            pw.write("no test to run");
            return;
        }

        runTest(testMethod, pw, req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        this.doGet(req, resp);
    }

    private void runTest(String testMethod, PrintWriter pw, HttpServletRequest req, HttpServletResponse resp) {
        try {
            Method testM = this.getClass().getDeclaredMethod(testMethod, new Class[] { Map.class, StringBuilder.class });
            Map<String, String> m = new HashMap<String, String>();

            Iterator itr = req.getParameterMap().keySet().iterator();

            while (itr.hasNext()) {
                String key = (String) itr.next();
                if (key.indexOf("@") == 0) {
                    m.put(key.substring(1), req.getParameter(key));
                }
            }

            m.put("serverIP", req.getLocalAddr());
            m.put("serverPort", String.valueOf(req.getLocalPort()));

            StringBuilder ret = new StringBuilder();
            testM.invoke(this, m, ret);
            pw.write(ret.toString());

        } catch (Exception e) {
            e.printStackTrace();
            e.printStackTrace(pw);
        }
    }

    public void testTimeout(Map<String, String> param, StringBuilder ret) {

        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        String timeout = param.get("timeout");
        long longTimeout = 0L;
        String res = null;
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = null;

        try {
            longTimeout = Long.parseLong(timeout);
            cb.connectTimeout(longTimeout*2, TimeUnit.MILLISECONDS);
            cb.readTimeout(longTimeout, TimeUnit.MILLISECONDS);

            // cb.property("com.ibm.ws.jaxrs.client.connection.timeout", timeout);
            // cb.property("com.ibm.ws.jaxrs.client.receive.timeout", timeout);
            c = cb.build();
            try {
                res = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/JAXRS21TimeoutClientTest/BasicResource").path("echo").path(param.get("param")).request()
                        .get(String.class);
            } catch (Exception expected) {
                expected.printStackTrace();
                if (expected.toString().contains("SocketTimeoutException")) {
                    res = "[Timeout Error]:" + "SocketTimeoutException";
                } else {
                    res = "[Timeout Error]:" + expected.toString();
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            res = "[Basic Resource]:timeoutValueInvalid";
        } finally {
            if (c != null) {
                c.close();
            }
            ret.append(res);
        }
    }


    public void testTimeoutNonRoutable(Map<String, String> param, StringBuilder ret) {

        // https://stackoverflow.com/a/904609/6575578
        String timeout = param.get("timeout");
        long longTimeout = 0L;
        String res = null;
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = null;

        try {
            longTimeout = Long.parseLong(timeout);
            cb.connectTimeout(longTimeout, TimeUnit.MILLISECONDS);
            cb.readTimeout(longTimeout*2, TimeUnit.MILLISECONDS);
            c = cb.build();
            long startTime = System.currentTimeMillis();
            try {
                res = c.target("http://" + "192.168.0.0" + "/" + moduleName + "/JAXRS21TimeoutClientTest/BasicResource").path("echo").path(param.get("param")).request()
                        .get(String.class);
            } catch (Exception e2) {
                e2.printStackTrace();
                long timeElapsed = System.currentTimeMillis() - startTime;
                long fudgeFactorTime = 4000;
                if (timeElapsed - fudgeFactorTime < longTimeout && timeElapsed + fudgeFactorTime > longTimeout) {
                    res = "[Basic Resource]:testTimeoutNonRoutable";
                } else {
                    res = "[Basic Resource]:testExceptionUnrelatedToConnectTimeout " + e2.toString() + " timeElapsed: " + timeElapsed + " fudgeFactorTime: " + fudgeFactorTime + " longTimeout: " + longTimeout;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            res = "[Timeout Error]:" + e.toString();
        } finally {
            if (c != null) {
                c.close();
            }
            ret.append(res);
        }
    }
}
