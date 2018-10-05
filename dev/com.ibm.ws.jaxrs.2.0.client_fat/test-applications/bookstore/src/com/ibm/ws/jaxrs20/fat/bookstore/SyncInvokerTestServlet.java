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
package com.ibm.ws.jaxrs20.fat.bookstore;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
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
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.SyncInvoker;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

@WebServlet("/SyncInvokerTestServlet")
public class SyncInvokerTestServlet extends HttpServlet {

    private static final long serialVersionUID = 2880606295862546001L;

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

            Iterator<String> itr = req.getParameterMap().keySet().iterator();

            while (itr.hasNext()) {
                String key = itr.next();
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

    public void testSyncInvoker_get1(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/bookstore/bookstore2/get1");
        SyncInvoker syncInvoker = t.request();
        Response response = syncInvoker.get();
        ret.append(response.readEntity(String.class));
        c.close();
    }

    public void testSyncInvoker_get2(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/bookstore/bookstore2/get2");
        Builder builder = t.request();
        builder.accept("application/xml");
        Book response = builder.get(Book.class);;
        ret.append(response.getName());
        c.close();
    }

    public void testSyncInvoker_get3(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/bookstore/bookstore2/get3");
        Builder builder = t.request();
        builder.accept("application/xml");
        GenericType<List<Book>> genericResponseType = new GenericType<List<Book>>() {};
        List<Book> response = builder.get(genericResponseType);
        ret.append(response != null);
        c.close();
    }

    public void testSyncInvoker_post1(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/bookstore/bookstore2/post1");
        Builder builder = t.request();
        Book book = new Book("Test book", 100);
        Response response = builder.post(Entity.xml(book));
        ret.append(response.readEntity(String.class));
        c.close();
    }

    public void testSyncInvoker_post2(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/bookstore/bookstore2/post2");
        Builder builder = t.request();
        builder.accept("application/xml");
        Book book = new Book("Test book2", 101);
        Book response = builder.post(Entity.xml(book), Book.class);
        ret.append(response.getName());
        c.close();
    }

    public void testSyncInvoker_post3(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/bookstore/bookstore2/post3");
        Builder builder = t.request();
        builder.accept("application/xml");
        GenericType<List<Book>> genericResponseType = new GenericType<List<Book>>() {};
        Book book = new Book("Test book3", 102);
        List<Book> response = builder.post(Entity.xml(book), genericResponseType);
        ret.append(response.get(response.size() - 1).getName());
        c.close();
    }
}
