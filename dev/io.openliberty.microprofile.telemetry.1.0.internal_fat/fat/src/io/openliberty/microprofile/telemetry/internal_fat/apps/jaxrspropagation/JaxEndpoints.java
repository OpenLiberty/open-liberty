/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.jaxpropagation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Scope;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.trace.data.SpanData;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@ApplicationPath("")
@Path("endpoints")
public class JaxEndpoints extends Application {

    @Inject
    InMemorySpanExporter spanExporter;

    @GET
    @Path("/readspans")
    public Response readSpans() {
        List<SpanData> spanData = spanExporter.getFinishedSpanItems(3);

        SpanData firstURL = spanData.get(0);
        SpanData httpGet = spanData.get(1);
        SpanData secondURL = spanData.get(2);

        assertEquals(firstURL.getKind(), SERVER);
        assertEquals(httpGet.getKind(), CLIENT);
        assertEquals(secondURL.getKind(), SERVER);

        assertEquals(httpGet.getParentSpanId(), firstURL.getSpanId());
        assertEquals(secondURL.getParentSpanId(), httpGet.getSpanId());

        return Response.ok("Test Passed").build();
    }

    @GET
    @Path("/jaxclient")
    public Response getJax(@Context UriInfo uriInfo) {
        assertNotNull(Span.current());
        try {
            Thread.sleep(3000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        spanExporter.reset();

        Baggage.builder().put("foo", "bar").build().makeCurrent();
        Baggage baggage = Baggage.current();
        assertEquals("bar", baggage.getEntryValue("foo"));

        Client client = ClientBuilder.newClient();
        String url = new String(uriInfo.getAbsolutePath().toString());
        url = url.replace("jaxclient", "jaxtwo"); //The jaxclient will use the URL as given so it needs the final part to be provided.

        String result = client.target(url)
            .request(MediaType.TEXT_PLAIN)
            .get(String.class);

        client.close();

        return Response.ok(result).build();
    }

    @GET
    @Path("/jaxclientasync")
    public Response getJaxAsync(@Context UriInfo uriInfo) {
        assertNotNull(Span.current());
        try {
            Thread.sleep(3000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        spanExporter.reset();

        Baggage.builder().put("foo", "bar").build().makeCurrent();
        Baggage baggage = Baggage.current();
        assertEquals("bar", baggage.getEntryValue("foo"));

        Client client = ClientBuilder.newClient();
        String url = new String(uriInfo.getAbsolutePath().toString());
        url = url.replace("jaxclientasync", "jaxtwo"); //The jaxclient will use the URL as given so it needs the final part to be provided.

        Future<String> result = client.target(url)
            .request(MediaType.TEXT_PLAIN)
            .async()
            .get(String.class);

	try {
            return Response.ok(result.get()).build(); 
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            client.close(); //This needs tobe after result.get()
        }
    }

    @GET
    @Path("/jaxtwo")
    public Response getJaxTwo() {
        assertNotNull(Span.current());
        Baggage baggage = Baggage.current();
        assertEquals("bar", baggage.getEntryValue("foo"));
        return Response.ok("Test Passed").build();
    }

    ////// MP code below //////

    @GET
    @Path("/mpclient")
    public Response getMP(@Context UriInfo uriInfo) {
        assertNotNull(Span.current());
        try {
            Thread.sleep(3000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        spanExporter.reset();

        Baggage.builder().put("foo", "bar").build().makeCurrent();
        Baggage baggage = Baggage.current();
        assertEquals("bar", baggage.getEntryValue("foo"));

        String baseUrl = uriInfo.getAbsolutePath().toString().replace("/mpclient", ""); //The mpclient will add the final part of the URL for you, so we remove the final part.
        URI baseUri = null;
        try {
            baseUri = new URI(baseUrl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertNotNull(Span.current());
        MPTwo two = RestClientBuilder.newBuilder()
                         .baseUri(baseUri)
                         .build(MPTwo.class);

        String result = two.getMPTwo();
        return Response.ok(result).build();
    }

    @GET
    @Path("/mptwo")
    public Response getMPTwo() {
        assertNotNull(Span.current());
        Baggage baggage = Baggage.current();
        assertEquals("bar", baggage.getEntryValue("foo"));

        return Response.ok("Test Passed").build();
    }

    @RegisterRestClient
    public interface MPTwo {

        @GET
        @Path("/mptwo")
        public String getMPTwo();

    }

    @GET
    @Path("/mpclientasync")
    public Response getMPAsync	(@Context UriInfo uriInfo) {
        assertNotNull(Span.current());
        try {
            Thread.sleep(3000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        spanExporter.reset();

        Baggage.builder().put("foo", "bar").build().makeCurrent();
        Baggage baggage = Baggage.current();
        assertEquals("bar", baggage.getEntryValue("foo"));

        String baseUrl = uriInfo.getAbsolutePath().toString().replace("/mpclientasync", ""); //The mpclient will add the final part of the URL for you, so we remove the final part.
        URI baseUri = null;
        try {
            baseUri = new URI(baseUrl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertNotNull(Span.current());
        MPTwoAsync two = RestClientBuilder.newBuilder()
                         .baseUri(baseUri)
                         .build(MPTwoAsync.class);

        String result = two.getMPTwo().toCompletableFuture().join();
        return Response.ok(result).build();
    }

    @RegisterRestClient
    public interface MPTwoAsync {

        @GET
        @Path("/mptwo")
        public CompletionStage<String> getMPTwo();

    }

}
