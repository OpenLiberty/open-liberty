/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.resourcealgorithm;

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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

@WebServlet("/TestServlet")
public class ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = 5146292896244309506L;
    private final String war = "resourcealgorithm";
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

    private String getContinuedSearchURI() {
        return "http://localhost:" + serverPort + "/resourcealgorithm/continued";
    }

    private String getNormalSearchURI() {
        return "http://localhost:" + serverPort + "/resourcealgorithm/normal";
    }

    // The reliance on MyOtherRootResource and MyRootResource causes compilation problems,
    // even though they're in the same package as this class. Guess because the they are in
    // separate source directories? Hardcode resource path for now, until we find a better way.

    /**
     * Tests that a regular GET is okay. Technically on MyRootResource but
     * MyOtherRootResource should have the same path value.
     */
    public void testDefaultGETIsOK(Map<String, String> param, StringBuilder ret) {
        Response response = client.target(UriBuilder.fromPath(getContinuedSearchURI()).path("/root")).request().get();

        assertEquals(200, response.getStatus());
        String resp = response.readEntity(String.class);
        assertEquals("MyRootResource.get()", resp);
        ret.append("OK");
    }

    /**
     * Tests that a regular POST is okay. Technically on MyOtherRootResource but
     * MyRootResource should have the same path value.
     */
    public void testDefaultPOSTIsOK(Map<String, String> param, StringBuilder ret) {
        Response response = client.target(UriBuilder.fromPath(getContinuedSearchURI()).path("/root")).request().post(null);

        assertEquals(200, response.getStatus());
        String resp = response.readEntity(String.class);
        assertEquals("MyOtherRootResource.post()", resp);
        ret.append("OK");
    }

    public Response getClientResponseGet() {
        Response response = client.target(UriBuilder.fromPath(getContinuedSearchURI()).path("/root")).request().get();

        return response;
    }

    /**
     * Tests that a subresource method can be reached with the same path as a
     * subresource locator.
     */
    public void testSubresourceMethodGET(Map<String, String> param, StringBuilder ret) {
        Response response = client.target(UriBuilder.fromPath(getContinuedSearchURI()).path("/root").path("subresource").build()).request().get();

        assertEquals(200, response.getStatus());
        String resp = response.readEntity(String.class);
        assertEquals("MyRootResource.getSub()", resp);
        ret.append("OK");
    }

    /**
     * Tests that a subresource locator can be reached with the same path as a
     * subresource method.
     */
    public void testSubresourceLocatorPOST(Map<String, String> param, StringBuilder ret) {
        Response response = client.target(UriBuilder.fromPath(getContinuedSearchURI()).path("/root").path("subresource").build()).request().post(null);

        assertEquals(200, response.getStatus());
        String resp = response.readEntity(String.class);
        assertEquals("MyObject.hello()", resp);
        ret.append("OK");
    }

    /**
     * Tests that a regular GET is okay. Technically on MyRootResource but
     * MyOtherRootResource should have the same path value.
     */
    public void testNormalSearch(Map<String, String> param, StringBuilder ret) {
        Response response = client.target(UriBuilder.fromPath(getNormalSearchURI()).path("/root").build()).request().get();

        assertEquals(200, response.getStatus());
        String resp = response.readEntity(String.class);
        assertEquals("MyRootResource.get()", resp);

        response = client.target(UriBuilder.fromPath(getNormalSearchURI()).path("/root").build()).request().post(null);

        assertEquals(200, response.getStatus());

        response = client.target(UriBuilder.fromPath(getNormalSearchURI()).path("/root").path("subresource").build()).request().get();

        assertEquals(200, response.getStatus());
        resp = response.readEntity(String.class);
        assertEquals("MyRootResource.getSub()", resp);

        response = client.target(UriBuilder.fromPath(getNormalSearchURI()).path("/root").path("subresource").build()).request().post(null);

        assertEquals(200, response.getStatus());
        ret.append("OK");
    }
}
