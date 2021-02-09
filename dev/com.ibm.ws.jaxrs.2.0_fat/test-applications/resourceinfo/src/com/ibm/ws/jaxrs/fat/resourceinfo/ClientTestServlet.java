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
package com.ibm.ws.jaxrs.fat.resourceinfo;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@WebServlet("/TestServlet")
public class ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = 6483141066276996160L;
    private static final String war = "resourceinfo";
    private String serverIP;
    private String serverPort;

    private static Client client;

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
            client = ClientBuilder.newClient();
            Method testM = this.getClass().getDeclaredMethod(testMethod, new Class[] { Map.class, StringBuilder.class });
            Map<String, String> m = new HashMap<String, String>();

            Iterator<String> itr = req.getParameterMap().keySet().iterator();

            while (itr.hasNext()) {
                String key = itr.next();
                if (key.indexOf("@") == 0) {
                    m.put(key.substring(1), req.getParameter(key));
                }
            }

            serverIP = req.getLocalAddr();
            serverPort = String.valueOf(req.getLocalPort());
            m.put("serverIP", serverIP);
            m.put("serverPort", serverPort);

            StringBuilder ret = new StringBuilder();
            testM.invoke(this, m, ret);
            pw.write(ret.toString());

        } catch (Exception e) {
            e.printStackTrace(); // print to the logs too since the test client only reads the first line of the pw output
            if (e instanceof InvocationTargetException) {
                e.getCause().printStackTrace(pw);
            } else {
                e.printStackTrace(pw);
            }
        } finally {
            client.close();
        }
    }

    private String getAddress(String path) {
        return "http://" + serverIP + ":" + serverPort + "/" + war + "/app/" +  path;
    }

    public void testSimpleResource(Map<String, String> param, StringBuilder ret) {
        Response r = client.target(getAddress("simple")).request().accept(MediaType.TEXT_PLAIN).get();
        ret.append(test(r, SimpleResource.class, "get"));
    }

    public void testAbstractAndSubClassResource(Map<String, String> param, StringBuilder ret) {
        Response r = client.target(getAddress("abstract")).request().accept(MediaType.TEXT_PLAIN).get();
        ret.append(test(r, AbstractResourceImpl.class, "abstractGet"));
    }

    public void testInterfaceAndImplClassResource(Map<String, String> param, StringBuilder ret) {
        Response r = client.target(getAddress("interface")).request().accept(MediaType.TEXT_PLAIN).get();
        ret.append(test(r, IResourceImpl.class, "interfaceGet"));
    }

    public void testConcreteSuperAndConcreteSubClassResource(Map<String, String> param, StringBuilder ret) {
        Response r = client.target(getAddress("subclass")).request().accept(MediaType.TEXT_PLAIN).get();
        ret.append(test(r, SubClassResource.class, "get"));
    }

    private String test(Response r, Class<?> resourceClass, String method) {
        assertEquals("NULL", r.getHeaderString("ClassFromPreMatchRequestFilter"));
        assertEquals("NULL", r.getHeaderString("MethodFromPreMatchRequestFilter"));

        assertEquals(resourceClass.getName(), r.getHeaderString("ClassFromRequestFilter"));
        assertEquals(resourceClass.getName() + " " + method, r.getHeaderString("MethodFromRequestFilter"));

        assertEquals(resourceClass.getName(), r.getHeaderString("ClassFromResource"));
        assertEquals(resourceClass.getName() + " " + method, r.getHeaderString("MethodFromResource"));

        assertEquals(resourceClass.getName(), r.getHeaderString("ClassFromResponseFilter"));
        assertEquals(resourceClass.getName() + " " + method, r.getHeaderString("MethodFromResponseFilter"));

        return "OK";
    }
}
