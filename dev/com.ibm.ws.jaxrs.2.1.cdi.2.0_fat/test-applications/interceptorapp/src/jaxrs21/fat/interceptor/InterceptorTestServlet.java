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
package jaxrs21.fat.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/InterceptorTestServlet")
public class InterceptorTestServlet extends FATServlet {

    private static final int HTTP_PORT = Integer.getInteger("bvt.prop.HTTP_default", 8010);
    private static final String SCOPE_REQUEST = "requestScoped";
    private static final String SCOPE_APP = "applicationScoped";

    private Client client;

    @Override
    public void init() throws ServletException {
        client = ClientBuilder.newBuilder().build();
    }

    @Override
    public void destroy() {
        client.close();
    }

    @Test
    public void testCDIInterceptorsInvokedOnResourceMethodWhenInterceptorAppliedToClass_RequestScoped(
                    HttpServletRequest req, HttpServletResponse resp) throws Exception {
        
        testCDIInterceptorsInvokedOnResourceMethodWhenInterceptorAppliedToClass(req, resp, SCOPE_REQUEST);
    }
    @Test
    public void testCDIInterceptorsInvokedOnResourceMethodWhenInterceptorAppliedToClass_ApplicationScoped(
                    HttpServletRequest req, HttpServletResponse resp) throws Exception {
        
        testCDIInterceptorsInvokedOnResourceMethodWhenInterceptorAppliedToClass(req, resp, SCOPE_APP);
    }

    public void testCDIInterceptorsInvokedOnResourceMethodWhenInterceptorAppliedToClass(
        HttpServletRequest req, HttpServletResponse resp, String scope) throws Exception {

        String response = invoke(req, "interceptorapp/" + scope + "/justOne");
        String[] interceptorsInvoked = response.split(" ");
        assertTrue("Unexpected number of businessInterceptors invoked (expected exactly one), got " + interceptorsInvoked.length,
                   interceptorsInvoked.length == 1);
        assertEquals("Unexpected interceptor invoked", "InterceptorOne", response);
    }

    @Test
    public void testCDIInterceptorsInvokedOnResourceMethodWhenInterceptorAppliedToClassAndMethod_RequestScoped(
                    HttpServletRequest req, HttpServletResponse resp) throws Exception {

        testCDIInterceptorsInvokedOnResourceMethodWhenInterceptorAppliedToClassAndMethod(req, resp, SCOPE_REQUEST);
    }

    @Test
    public void testCDIInterceptorsInvokedOnResourceMethodWhenInterceptorAppliedToClassAndMethod_ApplicationScoped(
                    HttpServletRequest req, HttpServletResponse resp) throws Exception {

        testCDIInterceptorsInvokedOnResourceMethodWhenInterceptorAppliedToClassAndMethod(req, resp, SCOPE_APP);
    }

    public void testCDIInterceptorsInvokedOnResourceMethodWhenInterceptorAppliedToClassAndMethod(
        HttpServletRequest req, HttpServletResponse resp, String scope) throws Exception {

        String response = invoke(req, "interceptorapp/" + scope + "/oneAndThree");
        String[] interceptorsInvoked = response.split(" ");
        assertTrue("Unexpected number of businessInterceptors invoked (expected exactly two), got " + interceptorsInvoked.length,
                   interceptorsInvoked.length == 2);
        assertTrue("Class level interceptor not invoked", response.contains("InterceptorOne"));
        assertTrue("Method level interceptor not invoked", response.contains("InterceptorThree"));
    }

    @Test
    public void testAllCDIInterceptorsInvokedOnResourceMethod_RequestScoped(
                    HttpServletRequest req, HttpServletResponse resp) throws Exception {

        testAllCDIInterceptorsInvokedOnResourceMethod_ApplicationScoped(req, resp, SCOPE_REQUEST);
    }
    @Test
    public void testAllCDIInterceptorsInvokedOnResourceMethod_ApplicationScoped(
                    HttpServletRequest req, HttpServletResponse resp) throws Exception {

        testAllCDIInterceptorsInvokedOnResourceMethod_ApplicationScoped(req, resp, SCOPE_REQUEST);
    }

    public void testAllCDIInterceptorsInvokedOnResourceMethod_ApplicationScoped(
                    HttpServletRequest req, HttpServletResponse resp, String scope) throws Exception {

        String response = invoke(req, "interceptorapp/" + scope + "/all");
        String[] interceptorsInvoked = response.split(" ");
        assertTrue("Unexpected number of businessInterceptors invoked (expected exactly three) got " + interceptorsInvoked.length,
                   interceptorsInvoked.length == 3);
        assertTrue("Class level interceptor not invoked", response.contains("InterceptorOne"));
        assertTrue("Method level interceptor not invoked", response.contains("InterceptorTwo"));
        assertTrue("Method level interceptor not invoked", response.contains("InterceptorThree"));
    }

    @Test
    public void testCDIInterceptorsInvokedOnResourcePostConstructMethodWhenInterceptorAppliedToClassAndMethod_RequestScoped(
                    HttpServletRequest req, HttpServletResponse resp) throws Exception {

        testCDIInterceptorsInvokedOnResourcePostConstructMethodWhenInterceptorAppliedToClassAndMethod(req, resp, SCOPE_REQUEST);
    }

    @Test
    public void testCDIInterceptorsInvokedOnResourcePostConstructMethodWhenInterceptorAppliedToClassAndMethod_ApplicationScoped(
                    HttpServletRequest req, HttpServletResponse resp) throws Exception {

        testCDIInterceptorsInvokedOnResourcePostConstructMethodWhenInterceptorAppliedToClassAndMethod(req, resp, SCOPE_APP);
    }

    public void testCDIInterceptorsInvokedOnResourcePostConstructMethodWhenInterceptorAppliedToClassAndMethod(
                    HttpServletRequest req, HttpServletResponse resp, String scope) throws Exception {

        String response = invoke(req, "interceptorapp/" + scope + "/postConstruct");
        String[] interceptorsInvoked = response.split(" ");
        assertTrue("Unexpected number of businessInterceptors invoked (expected exactly one), got " + interceptorsInvoked.length,
                   interceptorsInvoked.length == 1);
        assertTrue("Unexpected interceptor invoked: " + response, response.equals("LifecycleInterceptorTwo"));
    }

    private String invoke(HttpServletRequest request, String path) {

        String base = "http://" + request.getServerName() + ':' + HTTP_PORT + '/';
        WebTarget target = client.target(base + path);
        String response = target.request().get(String.class).trim();
        System.out.println("Response = " + response);
        return response;
    }

}