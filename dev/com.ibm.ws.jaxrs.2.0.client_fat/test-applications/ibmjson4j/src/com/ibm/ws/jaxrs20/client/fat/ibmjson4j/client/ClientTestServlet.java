/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.fat.ibmjson4j.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

//import com.ibm.json.java.JSONArray;
//import com.ibm.json.java.JSONObject;
//import com.ibm.websphere.jaxrs.providers.json4j.JSON4JArrayProvider;
//import com.ibm.websphere.jaxr//import com.ibm.websphere.jaxrs.providers.json4j.JSON4JArrayProvider;
//import com.ibm.websphere.jaxrs.providers.json4j.JSON4JJAXBProvider;
//import com.ibm.websphere.jaxrs.providers.json4j.JSON4JObjectProvider;s.providers.json4j.JSON4JJAXBProvider;
//import com.ibm.websphere.jaxrs.providers.json4j.JSON4JObjectProvider;
import com.ibm.ws.jaxrs20.client.fat.ibmjson4j.service.Book;

@WebServlet("/ClientTestServlet")
public class ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = 7188707949976646396L;
    private static final String moduleName = "ibmjson4j";

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
            e.printStackTrace(pw);
        }
    }

    public void testNewClientBuilder_ClientIBMJson4JProviders(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        ret.append("OK");
    }

    public void testNewClient_ClientIBMJson4JProviders(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.newClient();
        c.close();
        ret.append("OK");
    }

    public void testNewWebTarget_ClientIBMJson4JProviders(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.newClient();
        WebTarget t = c.target("http://localhost:8010/dumpservice/test");
        c.close();
        ret.append("OK");
    }

    public void testNewInvocationBuilder_ClientIBMJson4JProviders(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.newClient();
        WebTarget t = c.target("http://localhost:8010/dumpservice/test");
        Builder ib = t.request();
        c.close();
        ret.append("OK");
    }

    public void testNewInvocation_ClientIBMJson4JProviders(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.newClient();
        WebTarget t = c.target("http://localhost:8010/dumpservice/test");
        Builder ib = t.request();
        Invocation iv = ib.buildGet();
        c.close();
        ret.append("OK");
    }

    public void testFlowProgram_ClientIBMJson4JProviders_JSONObject_Get(Map<String, String> param, StringBuilder ret) {

        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.newClient();
//        c.register(JSON4JObjectProvider.class);
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/user/listusers");
        Builder ib = t.request();
        JsonObject res = ib.accept(MediaType.APPLICATION_JSON).get(JsonObject.class);
        c.close();
        ret.append(res);
    }

    public void testFlowProgram_ClientIBMJson4JProviders_JSONObject_Post(Map<String, String> param, StringBuilder ret) {

        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

//        JsonObject newJsonObj = new JsonObject();
//        newJsonObj.put("Ellen", "30");
        JsonObject newJsonObj = Json.createObjectBuilder().add("Ellen", "30").build();

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.newClient();
//        c.register(JSON4JObjectProvider.class);
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/user/listusers");
        Builder ib = t.request();
        JsonObject res = ib.accept(MediaType.APPLICATION_JSON).post(Entity.json(newJsonObj), JsonObject.class);
        c.close();
        ret.append(res);
    }

    public void testFlowProgram_ClientIBMJson4JProviders_JSONArray_Get(Map<String, String> param, StringBuilder ret) {

        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.newClient();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/user/listusers/array");
//        t.register(JSON4JArrayProvider.class);
//        t.register(JSON4JObjectProvider.class);
        Builder ib = t.request();
        JsonArray res = ib.accept(MediaType.APPLICATION_JSON).get(JsonArray.class);
        c.close();
        ret.append(res);
    }

    public void testFlowProgram_ClientIBMJson4JProviders_JSONArray_Post(Map<String, String> param, StringBuilder ret) {

        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

//        JsonArray newJsonArr = new JsonArray();
//        JsonObject obj1 = new JsonObject();
//        obj1.put("Apache", "10");
//        obj1.put("CXF", "3");
        JsonObject obj1 = Json.createObjectBuilder()
                        .add("Apache", "10")
                        .add("CXF", "3").build();
//        newJsonArr.add(0, obj1);
        JsonArray newJsonArr = Json.createArrayBuilder()
                        .add(obj1)
                        .build();

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.newClient();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/user/listusers/array");
//        t.register(JSON4JArrayProvider.class);
//        t.register(JSON4JObjectProvider.class);
//        t.register(JSON4JJAXBProvider.class);
        Builder ib = t.request();
        JsonArray res = ib.accept(MediaType.APPLICATION_JSON).post(Entity.json(newJsonArr), JsonArray.class);
        c.close();
        ret.append(res);
    }

    public void testFlowProgram_ClientIBMJson4JProviders_JAXB_Get(Map<String, String> param, StringBuilder ret) {

        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.newClient();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/user/listusers/book");
        Builder ib = t.request();
        Book res = ib.accept(MediaType.APPLICATION_XML).get(Book.class);
        c.close();
        ret.append(res.getName());
    }

    public void testFlowProgram_ClientIBMJson4JProviders_JAXB_Post(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        Book book = new Book("JAXRS", "2.0");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.newClient();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/user/listusers/book");
        Builder ib = t.request();
        Book res = ib.accept(MediaType.APPLICATION_XML).post(Entity.json(book), Book.class);
        c.close();
        ret.append(res.getName());
    }
}
