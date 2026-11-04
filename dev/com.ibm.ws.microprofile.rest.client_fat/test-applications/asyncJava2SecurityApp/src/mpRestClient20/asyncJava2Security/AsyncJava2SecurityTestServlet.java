/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package mpRestClient20.asyncJava2Security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;

import componenttest.app.FATServlet;

// Test servlet for GitHub issue #26810: MP Rest Client 2.0 hangs when performing an async request with Java 2 Security enabled.
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/AsyncJava2SecurityTestServlet")
public class AsyncJava2SecurityTestServlet extends FATServlet {

    // Test async MP Rest Client requests work with Java 2 Security enabled. Reproduces #26810 
    @Test
    public void testAsyncRequestWithJava2Security(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String baseUri = "http://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
        
        TestClientInterface client = RestClientBuilder.newBuilder()
                                                      .baseUri(URI.create(baseUri))
                                                      .build(TestClientInterface.class);
        
        String result = client.asyncGetTarget().toCompletableFuture().get(10, TimeUnit.SECONDS);
        
        assertNotNull("Response should not be null", result);
        assertEquals("Response should match expected value", "test string", result);
    }
    
    // Rest client interface to call the target endpoint
    public static interface TestClientInterface {
        @Path("/target")
        @GET
        CompletionStage<String> asyncGetTarget();
    }
}