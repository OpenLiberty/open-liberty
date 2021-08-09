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
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.jaxrs;

import static com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants.WORK_TIME;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;

@ApplicationScoped
@Path("/")
@Produces(TEXT_PLAIN)
public class TestResource {

    private final AtomicInteger testServerCallCount = new AtomicInteger(0);

    /**
     * Test endpoint called by {@link JaxRsTest#testJaxRsServerAsync()}
     */
    @GET
    @Path("test-server-async")
    @Asynchronous
    @Retry(maxRetries = 3)
    public CompletionStage<String> testServer() {
        int callCount = testServerCallCount.incrementAndGet();
        if (callCount >= 3) {
            return CompletableFuture.completedFuture("OK" + callCount);
        } else {
            throw new TestException("Test Exception");
        }
    }

    /**
     * Test endpoint for {@link TestClientBean}
     */
    @GET
    @Path("test-working")
    public String testWorking() {
        return "OK";
    }

    /**
     * Test endpoint for {@link TestClientBean}
     */
    @GET
    @Path("test-failing")
    public String testFailing() {
        throw new TestException("Test Exception");
    }

    /**
     * Test endpoint for {@link TestClientBean}
     */
    @GET
    @Path("test-slow")
    public String testSlow() throws InterruptedException {
        Thread.sleep(WORK_TIME);
        return "OK";
    }

}
