/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.async;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Application;

/**
 * This endpoint is used to test MP Telemetry integration with async JAX-RS resource methods.
 * <p>
 * It does the following:
 * <ul>
 * <li>Creates a span
 * <li>Reads the baggage entry with key {@link #BAGGAGE_KEY} and if present sets span attribute {@link #BAGGAGE_VALUE_ATTR} to the entry value
 * <li>Submits a subtask to a managed executor with the context propagated and returns a CompletableFuture representing the result of the subtask
 * <li>In the subtask it:
 * <ul><li>Creates a span
 * <li>Sleeps three seconds (to ensure that there is no chance of the subtask completing before the resource method returns)
 * <li>Reads the baggage entry with key {@link #BAGGAGE_KEY} and if present sets span attribute {@link #BAGGAGE_VALUE_ATTR} to the entry value
 * <li>Returns "OK"
 * </ul></ul>
 *
 */
@ApplicationPath("/")
@Path("JaxRsServerAsyncTestEndpoint")
public class JaxRsServerAsyncTestEndpoint extends Application {

    public static final String BAGGAGE_KEY = "test.baggage.key";
    public static final AttributeKey<String> BAGGAGE_VALUE_ATTR = AttributeKey.stringKey("test.baggage");

    @Resource
    private ManagedExecutorService managedExecutor;

    @Inject
    private Tracer tracer;

    @GET
    @Path("completionstage")
    public CompletionStage<String> getCompletionStage() {
        Span span = Span.current();

        // Retrieve the test baggage value (if present) and store in the span
        String baggageValue = Baggage.current().getEntryValue(BAGGAGE_KEY);
        if (baggageValue != null) {
            span.setAttribute(BAGGAGE_VALUE_ATTR, baggageValue);
        }

        // Call a subtask, propagating the context
        ExecutorService contextExecutor = Context.taskWrapping(managedExecutor);
        CompletableFuture<String> result = CompletableFuture.supplyAsync(this::subtask, contextExecutor);

        // Return the async result
        return result;
    }

    @GET
    @Path("suspend")
    public void getSuspend(@Suspended AsyncResponse async) {
        Span span = Span.current();

        // Retrieve the test baggage value (if present) and store in the span
        String baggageValue = Baggage.current().getEntryValue(BAGGAGE_KEY);
        if (baggageValue != null) {
            span.setAttribute(BAGGAGE_VALUE_ATTR, baggageValue);
        }

        // Call a subtask, propagating the context
        ExecutorService contextExecutor = Context.taskWrapping(managedExecutor);
        contextExecutor.execute(() -> {
            // Ensure we call resume, either with the result or a thrown exception
            try {
                async.resume(subtask());
            } catch (Throwable t) {
                async.resume(t);
            }
        });
    }

    private String subtask() {
        Span span = tracer.spanBuilder("subtask").startSpan();
        try (Scope scope = span.makeCurrent()) {
            // Sleep a while to ensure that this is running after get() has returned
            Thread.sleep(3000);

            // Retrieve the test baggage value (if present) and store in the span
            String baggageValue = Baggage.current().getEntryValue(BAGGAGE_KEY);
            if (baggageValue != null) {
                span.setAttribute(BAGGAGE_VALUE_ATTR, baggageValue);
            }

            return "OK";

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            span.end();
        }
    }

    public static URI getBaseUri(HttpServletRequest request) {
        try {
            URI originalUri = URI.create(request.getRequestURL().toString());
            URI targetUri = new URI(originalUri.getScheme(), originalUri.getAuthority(), request.getContextPath(), null, null);
            System.out.println("Using URI: " + targetUri);
            return targetUri;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}