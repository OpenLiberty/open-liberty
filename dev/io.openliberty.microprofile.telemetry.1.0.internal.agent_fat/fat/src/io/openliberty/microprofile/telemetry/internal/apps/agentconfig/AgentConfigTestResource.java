/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal.apps.agentconfig;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.WithSpan;

@ApplicationPath("/")
@Path("/")
public class AgentConfigTestResource extends Application {

    @Inject
    private Tracer tracer;

    @Inject
    private WithSpanBean withSpanBean;

    @GET
    public String getTraceId() {
        return Span.current().getSpanContext().getTraceId();
    }

    @GET
    @Path("/withspan")
    public String getWithSpan() {
        return withSpanTarget();
    }

    @GET
    @Path("/manualSpans")
    public String manualSpanCreation() {
        Span span1 = tracer.spanBuilder("span1").startSpan();
        try (Scope scope = span1.makeCurrent()) {
            Span span2 = tracer.spanBuilder("span2").startSpan();
            span2.end();
        } finally {
            span1.end();
        }
        return span1.getSpanContext().getTraceId();
    }

    @WithSpan
    private String withSpanTarget() {
        return Span.current().getSpanContext().getTraceId();
    }

    /**
     * This method should test all of our trace integration points
     */
    @GET
    @Path("/allTraces")
    public String getAllTraces(@Context UriInfo uriInfo) {
        Span manual = tracer.spanBuilder("manual").startSpan();
        try (Scope scope = manual.makeCurrent()) {
            // WithSpan not on bean
            withSpanTarget();

            // WithSpan on bean
            withSpanBean.withSpan();

            // JAX-RS client
            ClientBuilder.newClient()
                         .target(uriInfo.getBaseUri())
                         .path("clientTarget")
                         .request()
                         .get();

            // MP Rest client
            RestClientBuilder.newBuilder()
                             .baseUri(uriInfo.getBaseUri())
                             .build(TestClient.class)
                             .clientTarget();
        } finally {
            manual.end();
        }
        return manual.getSpanContext().getTraceId();
    }

    @GET
    @Path("/clientTarget")
    public String clientTarget() {
        return "OK";
    }
}
