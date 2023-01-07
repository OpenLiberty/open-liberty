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
package io.openliberty.microprofile.telemetry.internal.apps.agent;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.reactivex.Flowable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

@ApplicationPath("/")
@Path("/")
public class AgentTestResource extends Application {

    @Inject
    private Tracer tracer;

    @Inject
    private AgentTestBean bean;

    @GET
    public String getTraceId() {
        return Span.current().getSpanContext().getTraceId();
    }

    @GET
    @Path("/subspan")
    public String createSubspan() {
        Span subspan = tracer.spanBuilder("subspan").startSpan();
        subspan.end();
        return Span.current().getSpanContext().getTraceId();
    }

    @GET
    @Path("/nestedspans")
    public String createNestedSpans() {
        Span first = Span.current();

        Span subspan1 = tracer.spanBuilder("subspan1").startSpan();
        try (Scope scope1 = subspan1.makeCurrent()) {
            Span subspan2 = tracer.spanBuilder("subspan2").startSpan();
            try (Scope scope2 = subspan2.makeCurrent()) {
                Span subspan3 = tracer.spanBuilder("subspan3").startSpan();
                subspan3.end();
            } finally {
                subspan2.end();
            }
        } finally {
            subspan1.end();
        }
        return first.getSpanContext().getTraceId();
    }

    @GET
    @Path("/rxjava")
    public String callRxJava() throws InterruptedException {
        Span span = Span.current();

        CountDownLatch latch = new CountDownLatch(1);

        // Do something async with RxJava
        // The agent should propagate the span context
        Flowable.just("foo", "bar")
                .delay(100, TimeUnit.MILLISECONDS)
                .doOnNext(s -> {
                    // Create a subspan in an async task
                    Span subspan = tracer.spanBuilder(s).startSpan();
                    subspan.end();
                })
                .doOnTerminate(() -> latch.countDown())
                .subscribe();

        latch.await();

        return span.getSpanContext().getTraceId();
    }

    @GET
    @Path("/httpclient")
    public String callHttpClient(@Context UriInfo uriInfo) throws IOException, InterruptedException {
        Span span = Span.current();

        URI targetUri = uriInfo.getBaseUriBuilder().path(AgentTestResource.class, "httpClientTarget").build();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(targetUri).GET().build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

        assertEquals("OK", response.body());

        return span.getSpanContext().getTraceId();
    }

    @GET
    @Path("/httpclient/target")
    public String httpClientTarget() {
        return "OK";
    }

    @GET
    @Path("/withspanbean")
    public String callBeanWithSpan() {
        Span span = Span.current();
        bean.withSpanMethod();
        return span.getSpanContext().getTraceId();
    }

    @GET
    @Path("/withspannonbean")
    public String callNonBeanWithSpan() {
        Span span = Span.current();
        withSpanNonBeanMethod();
        return span.getSpanContext().getTraceId();
    }

    @GET
    @Path("/jaxrsclient")
    public String callJaxRSClient(@Context UriInfo uriInfo) {
        Span span = Span.current();
        ClientBuilder.newClient()
                     .target(uriInfo.getBaseUri())
                     .path("httpclient/target")
                     .request(MediaType.TEXT_PLAIN)
                     .get(String.class);
        return span.getSpanContext().getTraceId();
    }

    @GET
    @Path("/mprestclient")
    public String callMPRestClient(@Context UriInfo uriInfo) {
        Span span = Span.current();
        AgentTestRestClient client = RestClientBuilder.newBuilder()
                                                      .baseUri(uriInfo.getBaseUri())
                                                      .build(AgentTestRestClient.class);
        client.getTarget();
        return span.getSpanContext().getTraceId();
    }

    @WithSpan
    private String withSpanNonBeanMethod() {
        return Span.current().getSpanContext().getSpanId();
    }

    @ApplicationScoped
    public static class AgentTestBean {
        @WithSpan
        public String withSpanMethod() {
            return Span.current().getSpanContext().getSpanId();
        }
    }

    @Path("/")
    public static interface AgentTestRestClient {
        @GET
        @Path("/httpclient/target")
        public String getTarget();
    }

}
