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

import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

/**
 * Bean with FT annotations which calls JAX-RS client asynchronously and passes back the CompletionStage.
 * <p>
 * These test the FT 2.0 behaviour of applying the fault tolerance logic when the returned CompletionStage completes.
 */
@RequestScoped
public class TestClientBean {

    private final WebTarget target;

    @Inject
    public TestClientBean(Client client, HttpServletRequest req) {
        System.out.println("Request URL: " + req.getRequestURL());
        System.out.println("Request context path: " + req.getContextPath());
        System.out.println("Servlet context path: " + req.getServletContext().getContextPath());
        URI baseUri = URI.create(req.getRequestURL().toString()).resolve(req.getContextPath());
        System.out.println("Base URI: " + baseUri);
        target = client.target(baseUri);
    }

    // No-arg constructor to allow bean to be proxied
    protected TestClientBean() {
        target = null;
    }

    @Asynchronous
    @Retry
    public CompletionStage<String> callWorkingEndpoint() {
        return target.path("test-working")
                        .request(TEXT_PLAIN_TYPE)
                        .rx()
                        .get(String.class);
    }

    @Asynchronous
    @Fallback(fallbackMethod = "failingEndpointFallback")
    public CompletionStage<String> callFailingEndpoint() {
        return target.path("test-failing")
                        .request(TEXT_PLAIN_TYPE)
                        .rx()
                        .get(String.class);
    }

    @SuppressWarnings("unused")
    private CompletionStage<String> failingEndpointFallback() {
        return CompletableFuture.completedFuture("FALLBACK");
    }

    @Asynchronous
    @Timeout(500)
    public CompletionStage<String> callSlowEndpoint() {
        return target.path("test-slow")
                        .request(TEXT_PLAIN_TYPE)
                        .rx()
                        .get(String.class);
    }
}
