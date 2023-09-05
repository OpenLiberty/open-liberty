/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal.tests;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static io.openliberty.microprofile.telemetry.internal.utils.TestUtils.findOneFrom;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerQueryClient.convertByteString;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.SpanMatcher.hasKind;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.SpanMatcher.hasName;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.SpanMatcher.hasNoParent;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.SpanMatcher.hasParentSpanId;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.SpanMatcher.hasServiceName;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.SpanMatcher.span;
import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;
import io.jaegertracing.api_v2.Model.Span;
import io.openliberty.microprofile.telemetry.internal.apps.agent.AgentTestResource;
import io.openliberty.microprofile.telemetry.internal.utils.TestConstants;
import io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerContainer;
import io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerQueryClient;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

/**
 * Test mpTelemetry running with the OpenTelemetry Java Agent enabled
 */
@RunWith(FATRunner.class)
@MaximumJavaLevel(javaLevel = 20)
public class AgentTest {

    private static final Class<AgentTest> c = AgentTest.class;
    private static final String SERVICE_NAME = "Test service";

    @Server("TelemetryAgent")
    public static LibertyServer server;

    @ClassRule
    public static JaegerContainer jaegerContainer = new JaegerContainer().withLogConsumer(new SimpleLogConsumer(JaegerBaseTest.class, "jaeger"));

    public static JaegerQueryClient client;

    private static Set<String> traceIdsUsed;

    @BeforeClass
    public static void setUp() throws Exception {

        client = new JaegerQueryClient(jaegerContainer);

        server.copyFileToLibertyServerRoot("opentelemetry-javaagent.jar");

        server.addEnvVar(TestConstants.ENV_OTEL_TRACES_EXPORTER, "otlp");
        server.addEnvVar(TestConstants.ENV_OTEL_EXPORTER_OTLP_ENDPOINT, jaegerContainer.getOltpGrpcUrl());
        server.addEnvVar("OTEL_METRICS_EXPORTER", "none");
        server.addEnvVar("OTEL_LOGS_EXPORTER", "none");

        server.addEnvVar(TestConstants.ENV_OTEL_SERVICE_NAME, SERVICE_NAME);
        server.addEnvVar(TestConstants.ENV_OTEL_BSP_SCHEDULE_DELAY, "100"); // Wait no more than 100ms to send traces to the server
        server.addEnvVar(TestConstants.ENV_OTEL_SDK_DISABLED, "false"); //Enable tracing

        // Construct the test application
        WebArchive jaegerTest = ShrinkWrap.create(WebArchive.class, "agentTest.war")
                                          .addClass(AgentTestResource.class)
                                          .addAsLibraries(new File("lib/com.ibm.websphere.org.reactivestreams.reactive-streams.1.0.jar"))
                                          .addAsLibraries(new File("lib/com.ibm.ws.io.reactivex.rxjava.2.2.jar"));
        ShrinkHelper.exportAppToServer(server, jaegerTest, SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @BeforeClass
    public static void initializeTraceIdCollection() {
        traceIdsUsed = new HashSet<>();
    }

    @AfterClass
    public static void checkAllTraceIdsAccountedFor() {
        Log.info(c, "checkAllTraceIdsAccountedFor", "Trace IDs used: " + traceIdsUsed);
        List<Span> unexpectedSpans;
        unexpectedSpans = client.getServices().stream() // Get all the services
                                .map(client::getSpansForServiceName) // and a list of spans for each service
                                .flatMap(s -> s.stream()) // merge the lists
                                .filter(span -> {
                                    // Filter out any traceIds which were used in tests
                                    String traceId = convertByteString(span.getTraceId());
                                    Log.info(c, "checkAllTraceIdsAccountedFor", "Trace ID found: " + traceId);
                                    return !traceIdsUsed.contains(traceId);
                                })
                                .collect(toList());

        assertThat("Spans created that don't belong to any test", unexpectedSpans, is(empty()));
    }

    /**
     * Test we get the expected span for a basic JAX-RS request
     */
    @Test
    public void testBasic() throws Exception {
        HttpRequest request = new HttpRequest(server, "/agentTest");
        String traceId = request.run(String.class);
        traceIdsUsed.add(traceId);
        Log.info(c, "testBasic", "TraceId is " + traceId);

        List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(1));
        Log.info(c, "testBasic", "Spans returned: " + spans);

        Span span = spans.get(0);

        assertThat(span, span().withTraceId(traceId)
                               .withTag(SemanticAttributes.HTTP_ROUTE.getKey(), "/agentTest/")
                               .withTag(SemanticAttributes.HTTP_METHOD.getKey(), "GET"));

        // We shouldn't have any additional spans
        List<String> services = client.getServices();
        assertThat(services, contains(SERVICE_NAME));
    }

    /**
     * Test we can manually create spans
     */
    @Test
    public void testCreateSubspan() throws Exception {
        HttpRequest request = new HttpRequest(server, "/agentTest/subspan");
        String traceId = request.run(String.class);
        traceIdsUsed.add(traceId);

        List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(2));

        Span root = findOneFrom(spans, hasNoParent());
        Span subspan = findOneFrom(spans, hasParentSpanId(root.getSpanId()));
        assertThat(root, hasServiceName(SERVICE_NAME));
        assertThat(root, hasKind(SERVER));
        assertThat(subspan, hasServiceName(SERVICE_NAME));
        assertThat(subspan, hasKind(INTERNAL));
    }

    /**
     * Test we can manually create several nested spans
     */
    @Test
    public void testCreatedNestedSpans() throws Exception {
        HttpRequest request = new HttpRequest(server, "/agentTest/nestedspans");
        String traceId = request.run(String.class);
        traceIdsUsed.add(traceId);

        List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(4));
        Span root = findOneFrom(spans, hasNoParent());
        assertThat(root, hasServiceName(SERVICE_NAME));
        assertThat(root, hasKind(SERVER));

        Span child1 = findOneFrom(spans, hasParentSpanId(root.getSpanId()));
        assertThat(child1, hasServiceName(SERVICE_NAME));
        assertThat(child1, hasKind(INTERNAL));

        Span child2 = findOneFrom(spans, hasParentSpanId(child1.getSpanId()));
        assertThat(child2, hasServiceName(SERVICE_NAME));
        assertThat(child2, hasKind(INTERNAL));

        Span child3 = findOneFrom(spans, hasParentSpanId(child2.getSpanId()));
        assertThat(child3, hasServiceName(SERVICE_NAME));
        assertThat(child3, hasKind(INTERNAL));
    }

    /**
     * Test annotating a bean method with {@code @WithSpan}
     */
    @Test
    public void testWithSpanBean() throws Exception {
        HttpRequest request = new HttpRequest(server, "/agentTest/withspanbean");
        String traceId = request.run(String.class);
        traceIdsUsed.add(traceId);

        List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(2));

        Span root = findOneFrom(spans, hasNoParent());
        assertThat(root, hasServiceName(SERVICE_NAME));
        assertThat(root, hasKind(SERVER));

        Span child = findOneFrom(spans, hasParentSpanId(root.getSpanId()));
        assertThat(child, hasServiceName(SERVICE_NAME));
        assertThat(child, hasKind(INTERNAL));
    }

    /**
     * Test annotating a non-bean method with {@code @WithSpan}
     */
    @Test
    public void testWithSpanNonBean() throws Exception {
        HttpRequest request = new HttpRequest(server, "/agentTest/withspannonbean");
        String traceId = request.run(String.class);
        traceIdsUsed.add(traceId);

        List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(2));

        Span root = findOneFrom(spans, hasNoParent());
        assertThat(root, hasServiceName(SERVICE_NAME));
        assertThat(root, hasKind(SERVER));

        Span child = findOneFrom(spans, hasParentSpanId(root.getSpanId()));
        assertThat(child, hasServiceName(SERVICE_NAME));
        assertThat(child, hasKind(INTERNAL));
    }

    /**
     * Test that the agent instruments RxJava (context should propagate to async tasks)
     */
    @Test
    public void testRxJava() throws Exception {
        HttpRequest request = new HttpRequest(server, "/agentTest/rxjava");
        String traceId = request.run(String.class);
        traceIdsUsed.add(traceId);

        List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(3));

        // There should be three spans, one from the rest request and two from async tasks
        Span restSpan = findOneFrom(spans, hasNoParent());
        Span fooSpan = findOneFrom(spans, hasName("foo"));
        Span barSpan = findOneFrom(spans, hasName("bar"));

        // The agent should propagate the context to async tasks so that their spans are children of the rest request
        assertThat(fooSpan, hasParentSpanId(restSpan.getSpanId()));
        assertThat(barSpan, hasParentSpanId(restSpan.getSpanId()));
    }

    /**
     * Test that the agent instruments the Java 11 HTTP client, creating a client span and propagating the context
     */
    @Test
    public void testHttpClient() throws Exception {
        HttpRequest request = new HttpRequest(server, "/agentTest/httpclient");
        String traceId = request.run(String.class);
        traceIdsUsed.add(traceId);

        // Expect 3 spans:
        //   server receiving request from test
        //   HTTP client sending request
        //   server receiving request from HTTP client
        List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(3));

        Span root = findOneFrom(spans, hasNoParent());
        assertThat(root, span().withKind(SERVER)
                               .withTag(SemanticAttributes.HTTP_ROUTE.getKey(), "/agentTest/httpclient"));

        Span child1 = findOneFrom(spans, hasParentSpanId(root.getSpanId()));
        assertThat(child1, hasKind(CLIENT));

        Span child2 = findOneFrom(spans, hasParentSpanId(child1.getSpanId()));
        assertThat(child2, span().withKind(SERVER)
                                 .withTag(SemanticAttributes.HTTP_ROUTE.getKey(), "/agentTest/httpclient/target"));
    }

    /**
     * Test that instrumentation of JAX-RS client calls still occurs
     */
    @Test
    public void testJaxRSClient() throws Exception {
        HttpRequest request = new HttpRequest(server, "/agentTest/jaxrsclient");
        String traceId = request.run(String.class);
        traceIdsUsed.add(traceId);

        // Expect 3 spans:
        //   server receiving request from test
        //   JAX-RS client sending request
        //   server receiving request from HTTP client
        List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(3));

        Span root = findOneFrom(spans, hasNoParent());
        assertThat(root, span().withKind(SERVER)
                               .withTag(SemanticAttributes.HTTP_ROUTE.getKey(), "/agentTest/jaxrsclient"));

        Span child1 = findOneFrom(spans, hasParentSpanId(root.getSpanId()));
        assertThat(child1, hasKind(CLIENT));

        Span child2 = findOneFrom(spans, hasParentSpanId(child1.getSpanId()));
        assertThat(child2, span().withKind(SERVER)
                                 .withTag(SemanticAttributes.HTTP_ROUTE.getKey(), "/agentTest/httpclient/target"));
    }

    /**
     * Test that instrumentation of MP Rest Client calls still occurs
     */
    @Test
    public void testMPRestClient() throws Exception {
        HttpRequest request = new HttpRequest(server, "/agentTest/mprestclient");
        String traceId = request.run(String.class);
        traceIdsUsed.add(traceId);

        // Expect 3 spans:
        //   server receiving request from test
        //   JAX-RS client sending request
        //   server receiving request from HTTP client
        List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(3));

        Span root = findOneFrom(spans, hasNoParent());
        assertThat(root, span().withKind(SERVER)
                               .withTag(SemanticAttributes.HTTP_ROUTE.getKey(), "/agentTest/mprestclient"));

        Span child1 = findOneFrom(spans, hasParentSpanId(root.getSpanId()));
        assertThat(child1, hasKind(CLIENT));

        Span child2 = findOneFrom(spans, hasParentSpanId(child1.getSpanId()));
        assertThat(child2, span().withKind(SERVER)
                                 .withTag(SemanticAttributes.HTTP_ROUTE.getKey(), "/agentTest/httpclient/target"));
    }
}
