/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.JAXRSClientSSL.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;



@WebServlet("/ClientTestServlet")
public class ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = 7188707949976646396L;
    private static final String moduleName = "jaxrsclientssl";

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
            m.put("context", req.getContextPath());

            StringBuilder ret = new StringBuilder();
            testM.invoke(this, m, ret);
            pw.write(ret.toString());

        } catch (Exception e) {
            e.printStackTrace();
            e.printStackTrace(pw);
        }
    }

    public void testClientBasicSSL_ClientBuilder(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("hostname");
        String serverPort = param.get("secport");

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.property("com.ibm.ws.jaxrs.client.ssl.config", "mySSLConfig");
        Client c = cb.build();
        String res = c.target("https://" + serverIP + ":" + serverPort + "/" + moduleName
                              + "/Test/BasicResource").path("echo").path(param.get("param")).request().get(String.class);
        c.close();
        ret.append(res);
    }

    public void testClientBasicSSLDefault(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("hostname");
        String serverPort = param.get("secport");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        try {
            String res = c.target("https://" + serverIP + ":" + serverPort + "/" + moduleName
                              + "/Test/BasicResource").path("echo").path(param.get("param")).request().get(String.class);
            c.close();
            ret.append(res);
        } finally {
            c.close();
        }
    }

    public void testClientBasicSSL_Client(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("hostname");
        String serverPort = param.get("secport");

        ClientBuilder cb = ClientBuilder.newBuilder();

        Client c = cb.build();
        c.property("com.ibm.ws.jaxrs.client.ssl.config", "mySSLConfig");
        String res = c.target("https://" + serverIP + ":" + serverPort + "/" + moduleName
                              + "/Test/BasicResource").path("echo").path(param.get("param")).request().get(String.class);
        c.close();
        ret.append(res);
    }

    public void testClientBasicSSL_WebTarget(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("hostname");
        String serverPort = param.get("secport");

        ClientBuilder cb = ClientBuilder.newBuilder();

        Client c = cb.build();

        WebTarget wt = c.target("https://" + serverIP + ":" + serverPort + "/" + moduleName + "/Test/BasicResource");
        wt.property("com.ibm.ws.jaxrs.client.ssl.config", "mySSLConfig");

        String res = wt.path("echo").path(param.get("param")).request().get(String.class);
        c.close();
        ret.append(res);
    }

/*
 * public void testClientBasicSSL_CustomizedSSLContext(Map<String, String> param, StringBuilder ret) {
 * String res = "";
 *
 * String serverIP = param.get("hostname");
 * String serverPort = param.get("secport");
 *
 * ClientBuilder cb = ClientBuilder.newBuilder();
 *
 * //set a invalid Customized SSL context, then the access should fail as using Customized SSL context instead of Liberty SSL config "mySSLConfig"
 * KeyStore ts;
 * try {
 * ts = KeyStore.getInstance("jceks");
 * } catch (KeyStoreException e1) {
 * ret.append("new KeyStore fails");
 * return;
 * }
 * String keyStorePath = param.get("SERVER_CONFIG_DIR") + "/resources/security/clientInvalidTrust.jks";
 *
 * try {
 * ts.load(new FileInputStream(keyStorePath), "passw0rd".toCharArray());
 * } catch (NoSuchAlgorithmException e1) {
 * ret.append("load KeyStore " + keyStorePath + " fails");
 * return;
 * } catch (CertificateException e1) {
 * ret.append("load KeyStore " + keyStorePath + " fails");
 * return;
 * } catch (FileNotFoundException e1) {
 * ret.append("load KeyStore " + keyStorePath + " fails");
 * return;
 * } catch (IOException e1) {
 * ret.append("load KeyStore " + keyStorePath + " fails");
 * return;
 * }
 *
 * cb.trustStore(ts);
 * cb.keyStore(ts, "passw0rd");
 *
 * Client c = cb.build();
 *
 * c.property("com.ibm.ws.jaxrs.client.ssl.config", "mySSLConfig");
 *
 * try {
 * WebTarget wt = c.target("https://" + serverIP + ":" + serverPort + "/" + moduleName + "/Test/BasicResource");
 *
 * res = wt.path("echo").path(param.get("param")).request().get(String.class);
 * } catch (Exception e) {
 * res = e.getMessage();
 * } finally {
 * c.close();
 * ret.append(res);
 * }
 * }
 */
    public void testClientBasicSSL_InvalidSSLRef(Map<String, String> param, StringBuilder ret) {
        String res = "";

        String serverIP = param.get("hostname");
        String serverPort = param.get("secport");

        ClientBuilder cb = ClientBuilder.newBuilder();

        Client c = cb.build();

        try {
            WebTarget wt = c.target("https://" + serverIP + ":" + serverPort + "/" + moduleName + "/Test/BasicResource");
            wt.property("com.ibm.ws.jaxrs.client.ssl.config", "invalidSSLConfig");

            res = wt.path("echo").path(param.get("param")).request().get(String.class);
        } catch (Exception e) {
            res = e.getMessage();
        } finally {
            c.close();
            ret.append(res);
        }
    }

    public void testClientLtpaHander_ClientNoTokenWithSSL(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("hostname");
        String serverPort = param.get("secport");

        ClientBuilder cb = ClientBuilder.newBuilder();

        Client c = cb.build();
        c.property("com.ibm.ws.jaxrs.client.ssl.config", "mySSLConfig");
        c.property("com.ibm.ws.jaxrs.client.ltpa.handler", "true");
        String res = c.target("https://" + serverIP + ":" + serverPort + "/" + moduleName
                              + "/Test/BasicResource").path("echo").path(param.get("param")).request().get(String.class);
        c.close();
        ret.append(res);
    }

    public void testClientLtpaHander_ClientNoTokenWithSSLDefault(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("hostname");
        String serverPort = param.get("secport");

        ClientBuilder cb = ClientBuilder.newBuilder();

        Client c = cb.build();
        c.property("com.ibm.ws.jaxrs.client.ltpa.handler", "true");
        String res = c.target("https://" + serverIP + ":" + serverPort + "/" + moduleName
                              + "/Test/BasicResource").path("echo").path(param.get("param")).request().get(String.class);
        c.close();
        ret.append(res);
    }
}
