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
package com.ibm.ws.microprofile.rest.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;

/**
 *
 */
public class RestClientBuilderImplTest {

    @Path("/")
    private static interface TestClient {
        @GET
        String get();
    }

    @Provider
    public static class TestFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext crc) throws IOException {
            System.out.println("executing TestFilter.filter(crc)");
            crc.abortWith(Response.ok("TestFilter").build());
        }
    }

    @Test
    public void builderIsExpectedImpl() {
        assertTrue(RestClientBuilder.newBuilder() instanceof RestClientBuilderImpl);
    }

    @Test
    public void simpleBuildNoException() throws Throwable {
        assertNotNull(RestClientBuilder.newBuilder()
                      .baseUrl(new URL("http://localhost:9080"))
                      .build(TestClient.class));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsIllegalStateExceptionIfNoBaseUrlSet() {
        RestClientBuilder.newBuilder().build(TestClient.class);
    }

    @Test
    public void propertiesAreSet() throws MalformedURLException {
        RestClientBuilder builder = RestClientBuilder.newBuilder()
                        .baseUrl(new URL("http://localhost:9080"))
                        .property("myProperty", "myValue");
        assertEquals("myValue", builder.getConfiguration().getProperty("myProperty"));
    }

    @Test
    public void registerClass() throws MalformedURLException {
        RestClientBuilder builder = RestClientBuilder.newBuilder()
                        .baseUrl(new URL("http://localhost:9080"))
                        .register(TestFilter.class);
        assertTrue(builder.getConfiguration().isRegistered(TestFilter.class));

        //TODO: enable after configuration takes effect in client
        //TestClient client = builder.build(TestClient.class);
        //assertEquals("TestFilter", client.get());
    }
    
    @Test
    public void registerInstance() throws MalformedURLException {
        TestFilter filter = new TestFilter();
        RestClientBuilder builder = RestClientBuilder.newBuilder()
                        .baseUrl(new URL("http://localhost:9080"))
                        .register(filter);
        assertTrue(builder.getConfiguration().isRegistered(filter));

        //TODO: enable after configuration takes effect in client
        //TestClient client = builder.build(TestClient.class);
        //assertEquals("TestFilter", client.get());
    }
}
