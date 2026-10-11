/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.rest.client.fat.asyncjava2security;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

// For GitHub issue #26810. Reproduces the hang when performing async requests with Java 2 Security enabled
@Path("/")
public class TestResource {

    /**
     * Entry point for test
     */
    @GET
    public String doTest(@Context UriInfo uriInfo) throws InterruptedException, ExecutionException, TimeoutException {
        TestClientInterface client = RestClientBuilder.newBuilder()
                                                      .baseUri(uriInfo.getBaseUri())
                                                      .build(TestClientInterface.class);
        return client.asyncGetTarget().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    /**
     * Target for rest client
     * @return constant string
     */
    @GET
    @Path("/target")
    public String getTarget() {
        return "test string";
    }
    
    /**
     * Rest client interface to call to target
     */
    public static interface TestClientInterface {
        @Path("/target")
        @GET
        public CompletionStage<String> asyncGetTarget();
    }

}