/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.fat.jackson.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

import com.ibm.ws.jaxrs20.client.fat.jackson.service.JacksonPOJOResource.Person;

@WebServlet("/ClientTestServlet")
public class ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = 7188707949976646396L;
    private static final String moduleName = "jackson";

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

    public void testNewClientBuilder_ClientJacksonProviders(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        ret.append("OK");
    }

    public void testNewClient_ClientJacksonProviders(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.newClient();
        c.close();
        ret.append("OK");
    }

    public void testNewWebTarget_ClientJacksonProviders(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.newClient();
        WebTarget t = c.target("http://localhost:8010/dumpservice/test");
        c.close();
        ret.append("OK");
    }

    public void testNewInvocationBuilder_ClientJacksonProviders(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.newClient();
        WebTarget t = c.target("http://localhost:8010/dumpservice/test");
        Builder ib = t.request();
        c.close();
        ret.append("OK");
    }

    public void testNewInvocation_ClientJacksonProviders(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.newClient();
        WebTarget t = c.target("http://localhost:8010/dumpservice/test");
        Builder ib = t.request();
        Invocation iv = ib.buildGet();
        c.close();
        ret.append("OK");
    }

    public void testFlowProgram_ClientJacksonProviders_Pojo_Get(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.newClient();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/pojo/person/person");
        Builder ib = t.request();
        Person res = ib.accept(MediaType.APPLICATION_JSON).get(Person.class);
        c.close();
        ret.append(res.getFirst());
    }

    public void testFlowProgram_ClientJacksonProviders_Pojo_Post(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        Person p = new Person();
        p.setFirst("jordan");
        p.setLast("zhang");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.newClient();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/pojo/person/person");
        Builder ib = t.request();
        Person res = ib.accept(MediaType.APPLICATION_JSON).post(Entity.json(p), Person.class);
        c.close();
        ret.append(res.getFirst());
    }

    public void testFlowProgram_ClientJacksonProviders_ListString_Get(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.newClient();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/pojo/person/string");
        Builder ib = t.request();
        List<String> res = ib.accept(MediaType.APPLICATION_JSON).get(List.class);
        c.close();
        ret.append(res.get(0));
    }

    public void testFlowProgram_ClientJacksonProviders_ListString_Post(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        List<String> list = new ArrayList<String>();
        list.add("jordan");
        list.add("ellen");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.newClient();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/pojo/person/string");
        Builder ib = t.request();
        List<String> res = ib.accept(MediaType.APPLICATION_JSON).post(Entity.json(list), List.class);
        c.close();
        ret.append(res.get(1));
    }

    public void testFlowProgram_ClientJacksonProviders_ListPojo_Get(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.newClient();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/pojo/person/personcollect");
        Builder ib = t.request();
        List<Person> res = ib.accept(MediaType.APPLICATION_JSON).get(List.class);
        ret.append(res.get(0));
    }

    public void testFlowProgram_ClientJacksonProviders_ListPojo_Post(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        List<Person> people = new ArrayList<Person>();
        Person p = new Person();
        p.setFirst("jordan");
        p.setLast("zhang");
        people.add(p);
        p = new Person();
        p.setFirst("ellen");
        p.setLast("xiao");
        people.add(p);

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.newClient();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/pojo/person/personcollect");
        Builder ib = t.request();
        List<Person> res = ib.accept(MediaType.APPLICATION_JSON).post(Entity.json(people), List.class);
        c.close();
        ret.append(res.get(0));
    }

    public void testFlowProgram_ClientJacksonProviders_ArrayPojo_Get(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.newClient();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/pojo/person/personarray");
        Builder ib = t.request();
        Person[] res = ib.accept(MediaType.APPLICATION_JSON).get(Person[].class);
        ret.append(res[2].getLast());
    }

    public void testFlowProgram_ClientJacksonProviders_ArrayPojo_Post(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        Person[] people = new Person[2];
        Person p = new Person();
        p.setFirst("jordan");
        p.setLast("zhang");
        people[0] = p;
        p = new Person();
        p.setFirst("ellen");
        p.setLast("xiao");
        people[1] = p;

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.newClient();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/pojo/person/personarray");
        Builder ib = t.request();
        Person[] res = ib.accept(MediaType.APPLICATION_JSON).post(Entity.json(people), Person[].class);
        c.close();
        ret.append(res[1].getFirst());
    }

    public void testFlowProgram_ClientJacksonProviders_Map_Get(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.newClient();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/pojo/person2/map");
        Builder ib = t.request();
        Map res = ib.accept(MediaType.APPLICATION_JSON).get(Map.class);
        ret.append(res);
    }
}
