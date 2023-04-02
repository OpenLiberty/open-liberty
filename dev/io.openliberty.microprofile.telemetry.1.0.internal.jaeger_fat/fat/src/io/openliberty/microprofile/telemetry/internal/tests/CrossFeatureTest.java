/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.tests;

import static io.openliberty.microprofile.telemetry.internal.utils.TestUtils.findOneFrom;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.SpanMatcher.hasProcessTag;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.SpanMatcher.hasTag;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.SpanMatcher.span;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.TELEMETRY_SDK_NAME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_URL;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;
import io.jaegertracing.api_v2.Model.Span;
import io.openliberty.microprofile.telemetry.internal.utils.TestConstants;
import io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerContainer;
import io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerQueryClient;
import io.opentelemetry.api.trace.SpanKind;

/**
 * Tests propagating traces between liberty servers running mpOpenTracing-3.0 and mpTelemetry-1.0, feeding traces to the same trace server.
 */
@RunWith(FATRunner.class)
public class CrossFeatureTest {

    private static final String APP_NAME = "crossFeature";
    private static final Class<?> c = CrossFeatureTest.class;

    @ClassRule
    public static JaegerContainer jaegerContainer = new JaegerContainer().withLogConsumer(new SimpleLogConsumer(JaegerBaseTest.class, "jaeger"));

    public static JaegerQueryClient client;

    @Server("crossFeatureOpenTracingServer")
    public static LibertyServer opentracingServer;

    @Server("crossFeatureTelemetryServer")
    public static LibertyServer telemetryServer;

    @BeforeClass
    public static void setUp() throws Exception {

        client = new JaegerQueryClient(jaegerContainer);

        // Inform the test framework that telemetryServer is configured to use the secondary HTTP ports
        telemetryServer.useSecondaryHTTPPort();

        telemetryServer.addEnvVar(TestConstants.ENV_OTEL_EXPORTER_OTLP_ENDPOINT, jaegerContainer.getOltpGrpcUrl());
        telemetryServer.addEnvVar(TestConstants.ENV_OTEL_BSP_SCHEDULE_DELAY, "100"); // Wait no more than 100ms to send traces to the server
        telemetryServer.addEnvVar(TestConstants.ENV_OTEL_SDK_DISABLED, "false"); //Enable tracing
        telemetryServer.addEnvVar("OTEL_PROPAGATORS", "tracecontext, baggage, jaeger"); // Include the jaeger propagation headers
        telemetryServer.addEnvVar("TESTCLIENT_MP_REST_URL", getUrl(opentracingServer));

        opentracingServer.addEnvVar("JAEGER_ENDPOINT", jaegerContainer.getJaegerThriftUrl());
        opentracingServer.addEnvVar("JAEGER_SAMPLER_TYPE", "const"); // Trace every call
        opentracingServer.addEnvVar("JAEGER_SAMPLER_PARAM", "1"); // Trace every call
        opentracingServer.addEnvVar("TESTCLIENT_MP_REST_URL", getUrl(telemetryServer));

        // create apps
        WebArchive opentracingWar = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                              .addPackage(io.openliberty.microprofile.telemetry.internal.apps.crossfeature.opentracing.CrossFeatureResource.class.getPackage());

        WebArchive telemetryWar = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                            .addPackage(io.openliberty.microprofile.telemetry.internal.apps.crossfeature.telemetry.CrossFeatureResource.class.getPackage());

        // deploy apps
        ShrinkHelper.exportAppToServer(opentracingServer, opentracingWar, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportAppToServer(telemetryServer, telemetryWar, DeployOptions.SERVER_ONLY);

        opentracingServer.startServer();
        telemetryServer.startServer();
    }

    @AfterClass
    public static void teardownTelemetry() throws Exception {
        telemetryServer.stopServer();
    }

    @AfterClass
    public static void teardownOpentracing() throws Exception {
        opentracingServer.stopServer();
    }

    private static String getUrl(LibertyServer server) {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME;
    }

    /**
     * Test calls between telemetry and opentracing, starting with telemetry
     * <p>
     * Expected: /1 (telemetry) {@literal ->} /2 (opentracing) {@literal ->} /3 (telemetry)
     */
    @Test
    public void testCrossFeatureFromTelemetry() throws Exception {
        HttpRequest request = new HttpRequest(telemetryServer, "/crossFeature/1");
        String response = request.run(String.class);

        Log.info(c, "testCrossFeatureFromTelemetry", "response: " + response);

        String traceId = response.substring(0, response.indexOf(":"));
        List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(5));

        for (Span span : spans) {
            Log.info(c, "testCrossFeatureFromTelemetry", span.toString());
        }

        Span server1 = findOneFrom(spans, hasTag(HTTP_ROUTE.getKey(), "/crossFeature/1"));
        Span client2 = findOneFrom(spans, span().withKind(SpanKind.CLIENT)
                                                .withTag(HTTP_URL.getKey(), getUrl(opentracingServer) + "/2"));
        Span server2 = findOneFrom(spans, span().withKind(SpanKind.SERVER)
                                                .withTag(HTTP_URL.getKey(), getUrl(opentracingServer) + "/2"));
        Span client3 = findOneFrom(spans, hasTag(HTTP_URL.getKey(), getUrl(telemetryServer) + "/3"));
        Span server3 = findOneFrom(spans, hasTag(HTTP_ROUTE.getKey(), "/crossFeature/3"));

        assertThat(server1, hasProcessTag(TELEMETRY_SDK_NAME.getKey(), "opentelemetry"));
        assertThat(client2, hasProcessTag(TELEMETRY_SDK_NAME.getKey(), "opentelemetry"));
        assertThat(server2, hasProcessTag("jaeger.version", "Java-1.6.0"));
        assertThat(client3, hasProcessTag("jaeger.version", "Java-1.6.0"));
        assertThat(server3, hasProcessTag(TELEMETRY_SDK_NAME.getKey(), "opentelemetry"));
    }

    /**
     * Test calls between telemetry and opentracing, starting with opentracing
     * <p>
     * Expected: /1 (opentracing) {@literal ->} /2 (telemetry) {@literal ->} /3 (opentracing)
     */
    @Test
    public void testCrossFeatureFromOpenTracing() throws Exception {
        HttpRequest request = new HttpRequest(opentracingServer, "/crossFeature/1");
        String response = request.run(String.class);

        Log.info(c, "testCrossFeatureFromOpenTracing", "response: " + response);

        String traceId = response.substring(0, response.indexOf(":"));
        List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(5));

        for (Span span : spans) {
            Log.info(c, "testCrossFeatureFromOpenTracing", span.toString());
        }

        Span server1 = findOneFrom(spans, span().withTag(HTTP_URL.getKey(), getUrl(opentracingServer) + "/1"));
        Span client2 = findOneFrom(spans, span().withTag(HTTP_URL.getKey(), getUrl(telemetryServer) + "/2"));
        Span server2 = findOneFrom(spans, hasTag(HTTP_ROUTE.getKey(), "/crossFeature/2"));
        Span client3 = findOneFrom(spans, span().withKind(SpanKind.CLIENT)
                                                .withTag(HTTP_URL.getKey(), getUrl(opentracingServer) + "/3"));
        Span server3 = findOneFrom(spans, span().withKind(SpanKind.SERVER)
                                                .withTag(HTTP_URL.getKey(), getUrl(opentracingServer) + "/3"));

        assertThat(server1, hasProcessTag("jaeger.version", "Java-1.6.0"));
        assertThat(client2, hasProcessTag("jaeger.version", "Java-1.6.0"));
        assertThat(server2, hasProcessTag(TELEMETRY_SDK_NAME.getKey(), "opentelemetry"));
        assertThat(client3, hasProcessTag(TELEMETRY_SDK_NAME.getKey(), "opentelemetry"));
        assertThat(server3, hasProcessTag("jaeger.version", "Java-1.6.0"));
    }

}
