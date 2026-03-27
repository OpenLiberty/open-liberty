/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.fat.exception;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;
import junit.framework.Assert;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JaxrsExceptionClientTestServlet")
public class ExceptionClientTestServlet extends FATServlet {

    private static final String URI_CONTEXT_ROOT = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/exception/";

    private Client client;

    @Override
    public void before() throws ServletException {
        client = ClientBuilder.newClient();
    }

    @Override
    public void after() {
        client.close();
    }

    @Test
    @AllowedFFDC({"org.jboss.resteasy.spi.UnhandledException","java.lang.ArithmeticException"})
    public void testExceptionFromStandardReq() {
        try {
           Response response = client.target(URI_CONTEXT_ROOT)
                            .path("single")
                            .request(MediaType.TEXT_PLAIN_TYPE)
                            .get();
            assertEquals(500, response.getStatus());
//            assertTrue(response.readEntity(String.class).contains("ArithmeticException"));
            System.out.println("Jim... readEntity = " + response.readEntity(String.class));
        } catch (Throwable t) {
            t.getStackTrace();
            Assert.fail("Caught exception: " + t);
        }
    }

    
    @Test
    @AllowedFFDC({"java.lang.ArithmeticException","org.jboss.resteasy.spi.UnhandledException"})
    public void testExceptionFromInternalAsyncReq() {
        Client client = ClientBuilder.newClient();
        try {
            CompletionStage<String> csResponse = client.target(URI_CONTEXT_ROOT).path("echo").request().rx().get(String.class);

            csResponse.toCompletableFuture().get();
            Assert.fail("Exception should he been thrown");
        } catch (Throwable e) {
            assertTrue("Exception does not contain HTTP 500 Internal Server Error",e.getMessage().contains("HTTP 500 Internal Server Error"));
            Thread.dumpStack();
        }
        client.close();
    }
}