/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation;

import static io.openliberty.microprofile.telemetry.internal_fat.common.SpanDataMatcher.isSpan;
import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_SCHEME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_URL;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_NAME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_PORT;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_PEER_NAME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_PEER_PORT;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

/**
 * Tests MP Telemetry integration with restfulWS and mpRestClient.
 * <p>
 * Whenever a request is made or received by one of these features, MP Telemetry should create a span.
 * Trace ID information should be sent via the HTTP headers so that the traces from both the client and server can be joined up.
 * Baggage information should also be sent via the HTTP headers so that contextual information from the first service can be included in trace messages in the second service.
 * <p>
 * This class covers:
 * <p>
 * Creation of Spans in
 * <ul>
 * <li>JAX-RS Client {1}
 * <li>JAX-RS Server {2}
 * <li>MP Rest Client {3}
 * </ul>
 * <p>
 * Propagation of Spans from
 * <ul>
 * <li>JAX-RS Server to JAX-RS Client {4}
 * <li>JAX-RS Server to MP Client {5}
 * <li>JAX-RS Server to JAX-RS Client Async {6}
 * <li>JAX-RS Server to MP Client Async {7}
 * </ul>
 * <p>
 * Baggage is correctly Propagated:
 * <ul>
 * <li>from JAX-RS Client to JAX-RS Server {8}
 * <li>from MP Client to JAX-RS Server {9}
 * <li>from JAX-RS Client async to JAX-RS Server {10}
 * <li>from MP Client async to JAX-RS Server {11}
 * </ul>
 * <p>
 * Correct application of Semantic convention attributes:
 * <ul>
 * <li>TCK RestClientSpanTest only checks HTTP_STATUS, HTTP_METHOD, HTTP_SCHEME, HTTP_TARGET, HTTP_URL, see if there are any others which should be present
 * <li>There are constants defined for each of the attributes in the spec
 * </ul>
 * <p>
 * <strong>NOTE</strong>: Some of the tests in this class could be re-written more simply using {@code FATServlet} and {@code TestSpans}.
 * However, we've sometimes seen slightly different behaviour for JAX-RS client and MP Rest client depending on whether or not they're called via a JAX-RS resource method.
 * Therefore we're leaving these tests as they are so that we have some test coverage of calling JAX-RS client via a JAX-RS resource method.
 */
@ApplicationPath("")
@Path("endpoints")
public class JaxRsEndpoints extends Application {

    private static final Logger LOGGER = Logger.getLogger(JaxRsEndpoints.class.getName());

    public static final String TEST_PASSED = "Test Passed";

    @Inject
    private InMemorySpanExporter spanExporter;

    @Inject
    private HttpServletRequest request;

    @Inject
    private InjectableBean injectableBean;

    private Client client;

    @PostConstruct
    private void openClient() {
        LOGGER.info("Creating JAX-RS client");
        client = ClientBuilder.newClient();
    }

    @PreDestroy
    private void closeClient() {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    //Gets a list of spans created by open telemetry when a test was running and confirms the spans are what we expected and IDs are propagated correctly
    //spanExporter.reset() should be called at the start of each new test.
    @GET
    @Path("/readspans/{traceId}")
    public Response readSpans(@Context UriInfo uriInfo, @PathParam("traceId") String traceId) {
        List<SpanData> spanData = spanExporter.getFinishedSpanItems(3, traceId);

        SpanData firstURL = spanData.get(0);
        SpanData httpGet = spanData.get(1);
        SpanData secondURL = spanData.get(2);

        assertEquals(SERVER, firstURL.getKind());
        assertEquals(CLIENT, httpGet.getKind());
        assertEquals(SERVER, secondURL.getKind());

        assertEquals(firstURL.getSpanId(), httpGet.getParentSpanId());
        assertEquals(httpGet.getSpanId(), secondURL.getParentSpanId());

        assertEquals(HTTP_OK, firstURL.getAttributes().get(HTTP_STATUS_CODE).intValue());
        assertEquals(HttpMethod.GET, firstURL.getAttributes().get(HTTP_METHOD));
        assertEquals("http", firstURL.getAttributes().get(HTTP_SCHEME));
        assertThat(httpGet.getAttributes().get(HTTP_URL), containsString("endpoints")); //There are many different URLs that will end up here. But all should contain "endpoints"

        // The request used to call /readspans should have the same hostname and port as the test request
        URI requestUri = uriInfo.getRequestUri();
        assertEquals(requestUri.getHost(), firstURL.getAttributes().get(NET_HOST_NAME));
        assertEquals(Long.valueOf(requestUri.getPort()), firstURL.getAttributes().get(NET_HOST_PORT));

        assertEquals(CLIENT, httpGet.getKind());
        assertEquals("HTTP GET", httpGet.getName());
        assertEquals(HTTP_OK, httpGet.getAttributes().get(HTTP_STATUS_CODE).intValue());
        assertEquals(HttpMethod.GET, httpGet.getAttributes().get(HTTP_METHOD));
        assertEquals(requestUri.getHost(), httpGet.getAttributes().get(NET_PEER_NAME));
        assertEquals(Long.valueOf(requestUri.getPort()), httpGet.getAttributes().get(NET_PEER_PORT));
        assertThat(httpGet.getAttributes().get(HTTP_URL), containsString("endpoints"));

        return Response.ok(TEST_PASSED).build();
    }

    //Gets a list of spans created by open telemetry when a test was running and confirms the spans are what we expected and IDs are propagated correctly
    //spanExporter.reset() should be called at the start of each new test.
    @GET
    @Path("/readspansmptel11/{traceId}")
    public Response readSpansMpTel11(@Context UriInfo uriInfo, @PathParam("traceId") String traceId) {
        List<SpanData> spanData = spanExporter.getFinishedSpanItems(3, traceId);

        SpanData firstURL = spanData.get(0);
        SpanData httpGet = spanData.get(1);
        SpanData secondURL = spanData.get(2);

        assertEquals(SERVER, firstURL.getKind());
        assertEquals(CLIENT, httpGet.getKind());
        assertEquals(SERVER, secondURL.getKind());

        assertEquals(firstURL.getSpanId(), httpGet.getParentSpanId());
        assertEquals(httpGet.getSpanId(), secondURL.getParentSpanId());

        assertEquals(HTTP_OK, firstURL.getAttributes().get(HTTP_STATUS_CODE).intValue());
        assertEquals(HttpMethod.GET, firstURL.getAttributes().get(HTTP_METHOD));
        assertEquals("http", firstURL.getAttributes().get(HTTP_SCHEME));
        assertThat(httpGet.getAttributes().get(HTTP_URL), containsString("endpoints")); //There are many different URLs that will end up here. But all should contain "endpoints"

        // The request used to call /readspans should have the same hostname and port as the test request
        URI requestUri = uriInfo.getRequestUri();
        assertEquals(requestUri.getHost(), firstURL.getAttributes().get(NET_HOST_NAME));
        assertEquals(Long.valueOf(requestUri.getPort()), firstURL.getAttributes().get(NET_HOST_PORT));

        assertEquals(CLIENT, httpGet.getKind());
        assertEquals("GET", httpGet.getName());
        assertEquals(HTTP_OK, httpGet.getAttributes().get(HTTP_STATUS_CODE).intValue());
        assertEquals(HttpMethod.GET, httpGet.getAttributes().get(HTTP_METHOD));
        assertEquals(requestUri.getHost(), httpGet.getAttributes().get(NET_PEER_NAME));
        assertEquals(Long.valueOf(requestUri.getPort()), httpGet.getAttributes().get(NET_PEER_PORT));
        assertThat(httpGet.getAttributes().get(HTTP_URL), containsString("endpoints"));

        return Response.ok(TEST_PASSED).build();
    }

    //Gets a list of spans created by open telemetry when a test was running and confirms the spans are what we expected and IDs are propagated correctly
    //spanExporter.reset() should be called at the start of each new test.
    @GET
    @Path("/readspanswithspan/{traceId}")
    public Response readSpansWithSpan(@Context UriInfo uriInfo, @PathParam("traceId") String traceId) {
        List<SpanData> spanData = spanExporter.getFinishedSpanItems(2, traceId);

        SpanData firstURL = spanData.get(0);
        SpanData withSpan = spanData.get(1);

        assertThat(firstURL, isSpan()
                        .withKind(SpanKind.SERVER)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                        .withAttribute(SemanticAttributes.HTTP_SCHEME, "http")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L));

        assertThat(withSpan, isSpan()
                        .withKind(SpanKind.INTERNAL)
                        .withParentSpanId(firstURL.getSpanId()));

        return Response.ok(TEST_PASSED).build();
    }

    //This URL is called by the test framework to trigger testing both JAX-RS server and JAX-RS client
    //Tests {2} automatically as this method triggers span creation.
    @GET
    @Path("/jaxrsclient")
    public Response getJax(@Context UriInfo uriInfo) {
        LOGGER.info(">>> getJax");
        assertNotNull(Span.current());

        try (Scope s = Baggage.builder().put("foo", "bar").build().makeCurrent()) {
            Baggage baggage = Baggage.current();
            assertEquals("bar", baggage.getEntryValue("foo"));

            String url = new String(uriInfo.getAbsolutePath().toString());
            url = url.replace("jaxrsclient", "jaxrstwo"); //The jaxrsclient will use the URL as given so it needs the final part to be provided.

            String result = client.target(url)
                            .request(MediaType.TEXT_PLAIN)
                            .get(String.class);
            assertEquals(TEST_PASSED, result);
        } finally {
            LOGGER.info("<<< getJax");
        }
        return Response.ok(Span.current().getSpanContext().getTraceId()).build();
    }

    //This URL is called by the test framework to trigger testing for calling JAX-RS client in Asnyc.
    //Tests {2} automatically as this method triggers span creation.
    @GET
    @Path("/jaxrsclientasync")
    public Response getJaxAsync(@Context UriInfo uriInfo) {
        LOGGER.info(">>> getJaxAsync");
        assertNotNull(Span.current());

        try (Scope s = Baggage.builder().put("foo", "bar").build().makeCurrent()) {
            Baggage baggage = Baggage.current();
            assertEquals("bar", baggage.getEntryValue("foo"));

            String url = new String(uriInfo.getAbsolutePath().toString());
            url = url.replace("jaxrsclientasync", "jaxrstwo"); //The jaxrsclient will use the URL as given so it needs the final part to be provided.

            Client client = ClientBuilder.newClient();
            Future<String> result = client.target(url)
                            .request(MediaType.TEXT_PLAIN)
                            .async()
                            .get(String.class);

            try {
                String resultValue = result.get(10, SECONDS);
                assertEquals(TEST_PASSED, resultValue);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                client.close();
            }

        } finally {
            LOGGER.info("<<< getJax");
        }
        return Response.ok(Span.current().getSpanContext().getTraceId()).build();
    }

    //A method to be called by JAX Clients
    //Tests {1} Automatically as this method triggers span creation. Tests {4} and {6} (depending on which method called by the framework calls this one) as span propagation is automatic.
    @GET
    @Path("/jaxrstwo")
    public Response getJaxRsTwo() {
        LOGGER.info(">>> getJaxRsTwo");
        assertNotNull(Span.current());
        Baggage baggage = Baggage.current();
        assertEquals("bar", baggage.getEntryValue("foo")); //Assert that Baggage is propagated from Jax Server to Jax Client. Tests {8} and {10} (depending on which entry method calls this one)
        LOGGER.info("<<< getJaxRsTwo");
        return Response.ok(TEST_PASSED).build();
    }

    ////// MP code below //////

    //This URL is called by the test framework to trigger testing mpclient
    //Tests {1} automatically as this method triggers span creation.
    @GET
    @Path("/mpclient")
    public Response getMP(@Context UriInfo uriInfo) {
        LOGGER.info(">>> getMP");
        assertNotNull(Span.current());

        try (Scope s = Baggage.builder().put("foo", "bar").build().makeCurrent()) {
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
            assertEquals(TEST_PASSED, result);
        } finally {
            LOGGER.info("<<< getMP");
        }
        return Response.ok(Span.current().getSpanContext().getTraceId()).build();

    }

    //This method is called via mpClient from the entry methods.
    //Tests {3} automatically as this method triggers span creation. Tests {5} and {7} (depending on which method called by the framework calls this one) as span propagation is automatic.
    @GET
    @Path("/mptwo")
    public Response getMPTwo() {
        LOGGER.info(">>> getMPTwo");

        assertNotNull(Span.current());
        Baggage baggage = Baggage.current();
        assertEquals("bar", baggage.getEntryValue("foo")); //Assert that Baggage is propagated from Jax Server to MPClient. Tests {9} or {11} depending on which method called by the framework calls this one

        LOGGER.info("<<< getMPTwo");
        return Response.ok(TEST_PASSED).build();
    }

    @RegisterRestClient
    public interface MPTwo {

        @GET
        @Path("/mptwo")
        public String getMPTwo();

    }

    //This URL is called by the test framework to trigger testing mpclient with async
    //Tests {1} automatically as this method triggers span creation.
    @GET
    @Path("/mpclientasync")
    public Response getMPAsync(@Context UriInfo uriInfo) {
        LOGGER.info(">>> getMPAsync");
        assertNotNull(Span.current());

        try (Scope s = Baggage.builder().put("foo", "bar").build().makeCurrent()) {

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
            assertEquals(TEST_PASSED, result);

            LOGGER.info("<<< getMPAsync");
        }
        return Response.ok(Span.current().getSpanContext().getTraceId()).build();
    }

    @GET
    @Path("/jaxrsclientwithspan")
    public Response getJaxWithSpan(@Context UriInfo uriInfo) {
        LOGGER.info(">>> getJaxWithSpans");
        injectableBean.methodWithSpan();

        LOGGER.info("<<< getJaxWithSpans");

        return Response.ok(Span.current().getSpanContext().getTraceId()).build();
    }

    @RegisterRestClient
    public interface MPTwoAsync {

        @GET
        @Path("/mptwo")
        public CompletionStage<String> getMPTwo();

    }

}