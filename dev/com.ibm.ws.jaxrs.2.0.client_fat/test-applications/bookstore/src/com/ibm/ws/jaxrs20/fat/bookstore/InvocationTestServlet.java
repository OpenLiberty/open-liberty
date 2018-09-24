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
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.xml.ws.Holder;

@WebServlet("/InvocationTestServlet")
public class InvocationTestServlet extends HttpServlet {

    private static final long serialVersionUID = 2880606295862546001L;
    private static final String moduleName = "invocation";

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

    public void testClientClass(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        String className = c.getClass().getName();
        ret.append(className);
        c.close();
    }

    public void testInvocation_invoke1(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/bookstore/bookstore2/get1");
        Invocation invocation = t.request().buildGet();
        Response response = invocation.invoke();
        ret.append(response.readEntity(String.class));
        c.close();
    }

    public void testInvocation_invoke2(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/bookstore/bookstore2/get2");
        Invocation invocation = t.request().buildGet();
        Book book = invocation.invoke(Book.class);
        ret.append(book.getName());
        c.close();
    }

    public void testInvocation_invoke3(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/bookstore/bookstore2/get3");
        GenericType<List<Book>> genericResponseType = new GenericType<List<Book>>() {};
        Invocation invocation = t.request().buildGet();
        List<Book> bookList = invocation.invoke(genericResponseType);
        ret.append(bookList != null);
        c.close();
    }

    public void testInvocation_submit1(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/bookstore/bookstore2/asyncget1");
        Invocation invocation = t.request().buildGet();
        Future<Response> future = invocation.submit();
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

    public void testInvocation_submit2(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/bookstore/bookstore2/asyncget2");
        Invocation invocation = t.request().buildGet();
        Future<Book> future = invocation.submit(Book.class);
        Book book;
        try {
            book = future.get();
            ret.append(book.getName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();
    }

    public void testInvocation_submit3(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/bookstore/bookstore2/asyncget3");
        GenericType<List<Book>> genericResponseType = new GenericType<List<Book>>() {};
        Invocation invocation = t.request().buildGet();
        Future<List<Book>> future = invocation.submit(genericResponseType);
        List<Book> bookList;
        try {
            bookList = future.get();
            ret.append(bookList != null);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();
    }

    public void testInvocation_submit4(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/bookstore/bookstore2/asyncget2");
        Invocation invocation = t.request().buildGet();
        final Holder<Book> holder = new Holder<Book>();
        InvocationCallback<Book> callback = createCallback(holder);
        Future<Book> future = invocation.submit(callback);
        Book book;
        try {
            book = future.get();
            ret.append(book.getName());
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

    public void testInvocation_property(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/bookstore/bookstore2/get2");
        t.register(InovacationPropertyTestClientRequestFilter.class);
        Invocation invocation = t.request().buildGet();
        Book testBook = new Book();
        testBook.setName("TestBook");
        invocation.property("TestProperty", testBook);
        Book book = invocation.invoke(Book.class);
        ret.append(book.getId());
        c.close();
    }

    public void testInvocationBuilder_property(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/bookstore/bookstore2/get2");
        t.register(InovacationPropertyTestClientRequestFilter.class);
        Builder builder = t.request();
        Book testBook = new Book();
        testBook.setName("TestBook");
        builder.property("TestProperty", testBook);
        Invocation invocation = builder.buildGet();
        Book book = invocation.invoke(Book.class);
        ret.append(book.getId());
        c.close();
    }
}
