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
package jaxrs21.fat.atinject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
@WebServlet(urlPatterns = "/AtInjectTestServlet")
public class AtInjectTestServlet extends FATServlet {

    private static final int HTTP_PORT = Integer.getInteger("bvt.prop.HTTP_default", 8010);

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
    public void testResourceConstructorInjection(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        checkInjected(req, "atinjectapp/direct/ctor", "constructor");
    }

    @Test
    public void testResourceFieldInjection(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        checkInjected(req, "atinjectapp/direct/field", "field");
    }

    @Test
    public void testResourceMethodInjectionIn(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        checkInjected(req, "atinjectapp/direct/method", "method");
    }

    @Test
    public void testResourceConstructorInjectionInManagedObject(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        checkInjected(req, "atinjectapp/viaManagedObject/ctor", "constructor");
    }

    @Test
    public void testResourceFieldInjectionInManagedObject(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        checkInjected(req, "atinjectapp/viaManagedObject/field", "field");
    }

    @Test
    public void testResourceMethodInjectionInManagedObject(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        checkInjected(req, "atinjectapp/viaManagedObject/method", "method");
    }

    private void checkInjected(HttpServletRequest req, String path, String expectedTarget) {
        String response = target(req, path).request().get(String.class);
        System.out.println(expectedTarget + " response1: " + response);
        String[] firstInvocationResponse = response.split(":");

        assertEquals("Unexpected injected object: " + firstInvocationResponse[0], expectedTarget, firstInvocationResponse[0]);

        response = target(req, path).request().get(String.class);
        System.out.println(expectedTarget + " response2: " + response);
        String[] secondInvocationResponse = response.split(":");

        assertEquals("Unexpected injected object on second request: " + secondInvocationResponse[0],
                     expectedTarget, secondInvocationResponse[0]);

        assertFalse("Injected same instance into two separate request scoped resources",
                    firstInvocationResponse[1].equals(secondInvocationResponse[1]));
    }

    private WebTarget target(HttpServletRequest request, String path) {
        String base = "http://" + request.getServerName() + ':' + HTTP_PORT + '/';
        return client.target(base + path);
    }

}