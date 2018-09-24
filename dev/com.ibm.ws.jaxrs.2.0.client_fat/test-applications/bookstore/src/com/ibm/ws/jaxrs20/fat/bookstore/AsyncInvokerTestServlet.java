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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.xml.ws.Holder;

@WebServlet("/AsyncInvokerTestServlet")
public class AsyncInvokerTestServlet extends HttpServlet {

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

    public void testAsyncInvoker_get1(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/bookstore/bookstore2/asyncget1");
        Builder builder = t.request();
        AsyncInvoker asyncInvoker = builder.async();
        Future<Response> future = asyncInvoker.get();
        try {
            Response response = future.get();
            ret.append(response.readEntity(String.class));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();
    }

    public void testAsyncInvoker_get2(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/bookstore/bookstore2/asyncget2");
        Builder builder = t.request();
        builder.accept("application/xml");
        AsyncInvoker asyncInvoker = builder.async();
        Future<Book> future = asyncInvoker.get(Book.class);
        try {
            Book response = future.get();
            ret.append(response.getName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();
    }

    public void testAsyncInvoker_get3(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/bookstore/bookstore2/asyncget3");
        Builder builder = t.request();
        builder.accept("application/xml");
        AsyncInvoker asyncInvoker = builder.async();
        GenericType<List<Book>> genericResponseType = new GenericType<List<Book>>() {};
        Future<List<Book>> future = asyncInvoker.get(genericResponseType);
        try {
            List<Book> response = future.get();
            ret.append(response != null);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();
    }

    public void testAsyncInvoker_get4(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/bookstore/bookstore2/asyncget2");
        Builder builder = t.request();
        builder.accept("application/xml");
        AsyncInvoker asyncInvoker = builder.async();
        final Holder<Book> holder = new Holder<Book>();
        InvocationCallback<Book> callback = createCallback(holder);
        Future<Book> future = asyncInvoker.get(callback);
        try {
            Book response = future.get();
            ret.append(response.getName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();
    }

    public void testAsyncInvoker_post1(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/bookstore/bookstore2/asyncpost1");
        Builder builder = t.request();
        AsyncInvoker asyncInvoker = builder.async();
        Book book = new Book("Test book", 100);
        Future<Response> future = asyncInvoker.post(Entity.xml(book));
        try {
            Response response = future.get();
            ret.append(response.readEntity(String.class));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();
    }

    public void testAsyncInvoker_post2(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/bookstore/bookstore2/asyncpost2");
        Builder builder = t.request();
        builder.accept("application/xml");
        AsyncInvoker asyncInvoker = builder.async();
        Book book = new Book("Test book2", 101);
        Future<Book> future = asyncInvoker.post(Entity.xml(book), Book.class);
        try {
            Book response = future.get();
            ret.append(response.getName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();
    }

    public void testAsyncInvoker_post3(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/bookstore/bookstore2/asyncpost3");
        Builder builder = t.request();
        builder.accept("application/xml");
        AsyncInvoker asyncInvoker = builder.async();
        GenericType<List<Book>> genericResponseType = new GenericType<List<Book>>() {};
        Book book = new Book("Test book3", 102);
        Future<List<Book>> future = asyncInvoker.post(Entity.xml(book), genericResponseType);
        try {
            List<Book> response = future.get();
            ret.append(response.get(response.size() - 1).getName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();
    }

    public void testAsyncInvoker_post4(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/bookstore/bookstore2/asyncpost2");
        Builder builder = t.request();
        builder.accept("application/xml");
        AsyncInvoker asyncInvoker = builder.async();
        Book book = new Book("Test book4", 103);
        final Holder<Book> holder = new Holder<Book>();
        InvocationCallback<Book> callback = createCallback(holder);
        Future<Book> future = asyncInvoker.post(Entity.xml(book), callback);
        try {
            Book response = future.get();
            ret.append(response.getName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();
    }

    private InvocationCallback<Book> createCallback(final Holder<Book> holder) {
        return new InvocationCallback<Book>() {
            @Override
            public void completed(Book response) {
                holder.value = response;
            }

            @Override
            public void failed(Throwable error) {
                error.printStackTrace();
            }
        };
    }
}
