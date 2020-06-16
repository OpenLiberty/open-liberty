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
package com.ibm.ws.jaxrs.fat.options;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet(urlPatterns = "/OptionsTestServlet")
public class OptionsTestServlet extends FATServlet {

    private Client client;
    private static HttpClient httpClient;

    @Override
    public void init() throws ServletException {
        client = ClientBuilder.newBuilder().build();
        httpClient = new DefaultHttpClient();
    }

    @Override
    public void destroy() {
        client.close();
        httpClient.getConnectionManager().shutdown();
    }


    @Test
    public void testOptions(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        Response response = req(req, "/options/options/test");
        assertEquals(200, response.getStatus());
        System.out.println("testOptions Allowed headers are : " + response.getHeaderString("Allow"));
        assertFalse(response.getHeaderString("Allow").contains("GET"));
        assertFalse(response.getHeaderString("Allow").contains("PUT"));
        assertTrue(response.getHeaderString("Allow").contains("POST"));
        assertTrue(response.getHeaderString("Allow").contains("OPTIONS"));
        assertFalse(response.getHeaderString("Allow").contains("HEAD"));
        assertFalse(response.getHeaderString("Allow").contains("DELETE"));
        assertFalse(response.getHeaderString("Allow").contains("PATCH"));

    }

    @Test
    public void testOptions2(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        Response response = req(req, "/options/options/test2/1");
        assertEquals(200, response.getStatus());
        System.out.println("testOptions2 Allowed headers are : " + response.getHeaderString("Allow"));
        assertFalse(response.getHeaderString("Allow").contains("GET"));
        assertFalse(response.getHeaderString("Allow").contains("PUT"));
        assertFalse(response.getHeaderString("Allow").contains("POST"));
        assertTrue(response.getHeaderString("Allow").contains("DELETE"));
        assertFalse(response.getHeaderString("Allow").contains("PATCH"));
        assertTrue(response.getHeaderString("Allow").contains("OPTIONS"));
        assertFalse(response.getHeaderString("Allow").contains("HEAD"));

    }

    @Test
    public void testOptions3(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        Response response = req(req, "/options/options/test/1");
        assertEquals(200, response.getStatus());
        System.out.println("testOptions3 Allowed headers are : " + response.getHeaderString("Allow"));
        assertTrue(response.getHeaderString("Allow").contains("GET"));
        assertTrue(response.getHeaderString("Allow").contains("PUT"));
        assertFalse(response.getHeaderString("Allow").contains("POST"));
        assertTrue(response.getHeaderString("Allow").contains("OPTIONS"));
        assertTrue(response.getHeaderString("Allow").contains("HEAD"));
        assertFalse(response.getHeaderString("Allow").contains("DELETE"));
        assertFalse(response.getHeaderString("Allow").contains("PATCH"));

    }

    @Test
    public void testOptions4(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        Response response = req(req, "/options/options/test/three/1/2/3");
        assertEquals(200, response.getStatus());
        System.out.println("testOptions4 Allowed headers are : " + response.getHeaderString("Allow"));
        assertTrue(response.getHeaderString("Allow").contains("GET"));
        assertFalse(response.getHeaderString("Allow").contains("PUT"));
        assertFalse(response.getHeaderString("Allow").contains("POST"));
        assertTrue(response.getHeaderString("Allow").contains("OPTIONS"));
        assertTrue(response.getHeaderString("Allow").contains("HEAD"));
        assertFalse(response.getHeaderString("Allow").contains("DELETE"));
        assertFalse(response.getHeaderString("Allow").contains("PATCH"));

    }

    @Test
    public void testOptions5(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        Response response = req(req, "/options/options/test3/all/1");
        assertEquals(200, response.getStatus());
        System.out.println("testOptions5 Allowed headers are : " + response.getHeaderString("Allow"));
        assertTrue(response.getHeaderString("Allow").contains("GET"));
        assertTrue(response.getHeaderString("Allow").contains("PUT"));
        assertTrue(response.getHeaderString("Allow").contains("POST"));
        assertTrue(response.getHeaderString("Allow").contains("OPTIONS"));
        assertTrue(response.getHeaderString("Allow").contains("HEAD"));
        assertTrue(response.getHeaderString("Allow").contains("DELETE"));
        assertFalse(response.getHeaderString("Allow").contains("PATCH"));

    }

    // OPTIONS does not work correctly for sub_resources.  See Issue https://github.com/OpenLiberty/open-liberty/issues/12643
//    @Test
    public void testOptions6(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        Response response = req(req, "/options/options/test4/post");
        assertEquals(200, response.getStatus());
        System.out.println("testOptions6 Allowed headers are : " + response.getHeaderString("Allow"));
        assertFalse(response.getHeaderString("Allow").contains("GET"));
        assertFalse(response.getHeaderString("Allow").contains("PUT"));
        assertTrue(response.getHeaderString("Allow").contains("POST"));
        assertTrue(response.getHeaderString("Allow").contains("OPTIONS"));
        assertFalse(response.getHeaderString("Allow").contains("HEAD"));
        assertFalse(response.getHeaderString("Allow").contains("DELETE"));
        assertFalse(response.getHeaderString("Allow").contains("PATCH"));

    }

    // OPTIONS does not work correctly for sub_resources.  See Issue https://github.com/OpenLiberty/open-liberty/issues/12643
//    @Test
    public void testOptions7(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        Response response = req(req, "/options/options/test5/5");
        assertEquals(200, response.getStatus());
        System.out.println("testOptions7 Allowed headers are : " + response.getHeaderString("Allow"));
        assertTrue(response.getHeaderString("Allow").contains("GET"));
        assertTrue(response.getHeaderString("Allow").contains("PUT"));
        assertFalse(response.getHeaderString("Allow").contains("POST"));
        assertTrue(response.getHeaderString("Allow").contains("OPTIONS"));
        assertTrue(response.getHeaderString("Allow").contains("HEAD"));
        assertFalse(response.getHeaderString("Allow").contains("DELETE"));
        assertFalse(response.getHeaderString("Allow").contains("PATCH"));

    }

    @Test
    public void testOptions10(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        HttpResponse response = httpReq(req, "/options/options/test4/4");
        assertEquals(200, response.getStatusLine().getStatusCode());
        Header[] allowHeaders = response.getHeaders("Allow");
        boolean foundGET = false;
        boolean foundPUT = false;
        boolean foundDELETE = false;
        boolean foundPOST = false;
        boolean foundOPTIONS = false;
        boolean foundHEAD = false;
        boolean foundPATCH = false;
        String headerValues = "";
        for (Header h : allowHeaders) {
            String headerVal = h.getValue();
            if (headerVal.contains("GET")) {
                foundGET = true;
            }
            if (headerVal.contains("PUT")) {
                foundPUT = true;
            }
            if (headerVal.contains("POST")) {
                foundPOST = true;
            }
            if (headerVal.contains("OPTIONS")) {
                foundOPTIONS = true;
            }
            if (headerVal.contains("HEAD")) {
                foundHEAD = true;
            }
            if (headerVal.contains("DELETE")) {
                foundDELETE = true;
            }
            if (headerVal.contains("PATCH")) {
                foundPATCH = true;
            }
            headerValues = headerValues.concat(h + " " );
        }
        System.out.println("testOption10 Allowed headers are : " + headerValues);

        assertTrue(foundGET);
        assertTrue(foundPUT);
        assertFalse(foundPOST);
        assertTrue(foundOPTIONS);
        assertTrue(foundHEAD);
        assertTrue(foundDELETE);
        assertFalse(foundPATCH);

    }

    private Response req(HttpServletRequest request, String path) {
        String base = "http://" + request.getServerName() + ':' + request.getServerPort();
        System.out.println("target : " + base + path);
        return client.target(base + path).request().options();
    }

    private HttpResponse httpReq(HttpServletRequest request, String path) throws Exception {
        String base = "http://" + request.getServerName() + ':' + request.getServerPort();
        HttpOptions optionsMethod = new HttpOptions(base + path);
        System.out.println("httpTarget : " + base + path);
        return httpClient.execute(optionsMethod);
    }

}