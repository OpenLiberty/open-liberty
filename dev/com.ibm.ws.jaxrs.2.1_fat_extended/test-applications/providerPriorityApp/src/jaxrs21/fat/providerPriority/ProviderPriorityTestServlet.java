/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.providerPriority;

import static org.junit.Assert.assertEquals;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/ProviderPriorityTestServlet")
public class ProviderPriorityTestServlet extends FATServlet {

    private Client client;

    @Override
    public void init() throws ServletException {
        client = ClientBuilder.newBuilder().build().register(ClientSideMBRW.class);
    }

    @Override
    public void destroy() {
        client.close();
    }

    @Test
    public void testEntityProviderPriorities(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        MyObject requestObject = new MyObject();
        requestObject.setMyString("hello");
        requestObject.setMyInt(5);
        Response response = target(req, "providerPriorityApp/rest/test")
                        .request(MediaType.TEXT_PLAIN)
                        .put(Entity.text(requestObject));
        response.bufferEntity();
        System.out.println("testEntityProviderPriorities responseEntity = " + response.readEntity(String.class));
        MyObject responseObject = response.readEntity(MyObject.class);

        // check that the response object was not altered during request
        assertEquals("Response object contains unexpected values",
                     requestObject.getMyString(), responseObject.getMyString());
        assertEquals("Response object contains unexpected values",
                     requestObject.getMyInt(), responseObject.getMyInt());

        // check that the expected providers processed the response object
        assertEquals("The higher priority MessageBodyReader was not selected", 2,
                     responseObject.getMbrVersion());
        assertEquals("The higher priority ContextResolver in the MessageBodyReader was not selected",
                     2, responseObject.getContextResolverVersionFromReader());
        assertEquals("The higher priority MessageBodyWriter was not selected", 2,
                     responseObject.getMbwVersion());
        assertEquals("The higher priority ContextResolver in the MessageBodyWriter was not selected",
                     2, responseObject.getContextResolverVersionFromWriter());

    }

    @Test
    public void testExceptionMapperPriorities(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        // first try an exception mapper for an app-specific exception
        Response response = target(req, "providerPriorityApp/rest/test/exception/" + MyException.class.getName())
                        .request(MediaType.TEXT_PLAIN)
                        .get();
        response.bufferEntity();
        System.out.println("testExceptionMapperPriorities status code = " + response.getStatus());
        System.out.println("testExceptionMapperPriorities responseEntity = " + response.readEntity(String.class));
        assertEquals("The higher priority ExceptionMapper was not selected", 418, response.getStatus());
        assertEquals("The response from the ExceptonMapper did not contain the expected entity",
                     MyHighPriorityMyExceptionMapper.class.getSimpleName(), response.readEntity(String.class));

        // now try an "unexpected" exception
        response = target(req, "providerPriorityApp/rest/test/exception/" + NullPointerException.class.getName())
                        .request(MediaType.TEXT_PLAIN)
                        .get();
        response.bufferEntity();
        System.out.println("testExceptionMapperPriorities status code = " + response.getStatus());
        System.out.println("testExceptionMapperPriorities responseEntity = " + response.readEntity(String.class));
        assertEquals("The higher priority ExceptionMapper was not selected", 409, response.getStatus());
        assertEquals("The response from the ExceptonMapper did not contain the expected entity",
                     MyHighPriorityThrowableMapper.class.getSimpleName(), response.readEntity(String.class));

    }

    private WebTarget target(HttpServletRequest request, String path) {
        String base = "http://" + request.getServerName() + ':' + request.getServerPort() + '/';
        return client.target(base + path);
    }

}