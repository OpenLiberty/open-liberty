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
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerSpanMatcher.hasAttribute;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerSpanMatcher.hasResourceAttribute;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerSpanMatcher.isSpan;
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
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;
import io.jaegertracing.api_v2.Model.Span;
import io.openliberty.microprofile.telemetry.internal.suite.FATSuite;
import io.openliberty.microprofile.telemetry.internal.utils.TestConstants;
import io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerContainer;
import io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerQueryClient;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;

/**
 * Tests propagating traces between liberty servers running mpOpenTracing-3.0 and mpTelemetry-1.0, feeding traces to the same trace server.
 * Spans are exported to Jaeger
 */
@RunWith(FATRunner.class)
public class CrossFeatureJaegerTest {

    private static final String CROSS_FEATURE_TELEMETRY_SERVER = "crossFeatureTelemetryServer";
    private static final String APP_NAME = "crossFeature";
    private static final Class<?> c = CrossFeatureJaegerTest.class;
    private static final AttributeKey<String> JAEGER_VERSION = AttributeKey.stringKey("jaeger.version");

    public static JaegerContainer jaegerContainer = new JaegerContainer().withLogConsumer(new SimpleLogConsumer(JaegerBaseTest.class, "jaeger"));
    public static RepeatTests repeat = FATSuite.allMPRepeats(CROSS_FEATURE_TELEMETRY_SERVER);

    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(jaegerContainer).around(repeat);

    public static JaegerQueryClient client;

    @Server("crossFeatureOpenTracingServer")
    public static LibertyServer opentracingServer;

    @Server(CROSS_FEATURE_TELEMETRY_SERVER)
    public static LibertyServer telemetryServer;

    @BeforeClass
    public static void setUp() throws Exception {

        client = new JaegerQueryClient(jaegerContainer);

        // Inform the test framework that opentracingServer is configured to use the secondary HTTP ports
        opentracingServer.useSecondaryHTTPPort();

        telemetryServer.addEnvVar(TestConstants.ENV_OTEL_EXPORTER_OTLP_ENDPOINT, jaegerContainer.getOtlpGrpcUrl());
        telemetryServer.addEnvVar(TestConstants.ENV_OTEL_BSP_SCHEDULE_DELAY, "100"); // Wait no more than 100ms to send traces to the server
        telemetryServer.addEnvVar(TestConstants.ENV_OTEL_SDK_DISABLED, "false"); //Enable tracing
        telemetryServer.addEnvVar("OTEL_PROPAGATORS", "tracecontext, baggage, jaeger"); // Include the jaeger propagation headers
        telemetryServer.addEnvVar("IO_OPENLIBERTY_MICROPROFILE_TELEMETRY_INTERNAL_APPS_CROSSFEATURE_TELEMETRY_CROSSFEATURECLIENT_MP_REST_URL", getUrl(opentracingServer));

        opentracingServer.addEnvVar("JAEGER_ENDPOINT", jaegerContainer.getJaegerThriftUrl());
        opentracingServer.addEnvVar("JAEGER_SAMPLER_TYPE", "const"); // Trace every call
        opentracingServer.addEnvVar("JAEGER_SAMPLER_PARAM", "1"); // Trace every call
        opentracingServer.addEnvVar("IO_OPENLIBERTY_MICROPROFILE_TELEMETRY_INTERNAL_APPS_CROSSFEATURE_OPENTRACING_CROSSFEATURECLIENT_MP_REST_URL", getUrl(telemetryServer));

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
        opentracingServer.stopServer();
    }

    @AfterClass
    public static void closeClient() throws Exception {
        if (client != null) {
            client.close();
        }
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

        Span server1 = findOneFrom(spans, hasAttribute(HTTP_ROUTE, "/crossFeature/1"));
        Span client2 = findOneFrom(spans, isSpan().withKind(SpanKind.CLIENT)
                                                  .withAttribute(HTTP_URL, getUrl(opentracingServer) + "/2"));
        Span server2 = findOneFrom(spans, isSpan().withKind(SpanKind.SERVER)
                                                  .withAttribute(HTTP_URL, getUrl(opentracingServer) + "/2"));
        Span client3 = findOneFrom(spans, hasAttribute(HTTP_URL, getUrl(telemetryServer) + "/3"));
        Span server3 = findOneFrom(spans, hasAttribute(HTTP_ROUTE, "/crossFeature/3"));

        assertThat(server1, hasResourceAttribute(TELEMETRY_SDK_NAME, "opentelemetry"));
        assertThat(client2, hasResourceAttribute(TELEMETRY_SDK_NAME, "opentelemetry"));
        assertThat(server2, hasResourceAttribute(JAEGER_VERSION, "Java-1.6.0"));
        assertThat(client3, hasResourceAttribute(JAEGER_VERSION, "Java-1.6.0"));
        assertThat(server3, hasResourceAttribute(TELEMETRY_SDK_NAME, "opentelemetry"));
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

        Span server1 = findOneFrom(spans, isSpan().withAttribute(HTTP_URL, getUrl(opentracingServer) + "/1"));
        Span client2 = findOneFrom(spans, isSpan().withAttribute(HTTP_URL, getUrl(telemetryServer) + "/2"));
        Span server2 = findOneFrom(spans, hasAttribute(HTTP_ROUTE, "/crossFeature/2"));
        Span client3 = findOneFrom(spans, isSpan().withKind(SpanKind.CLIENT)
                                                  .withAttribute(HTTP_URL, getUrl(opentracingServer) + "/3"));
        Span server3 = findOneFrom(spans, isSpan().withKind(SpanKind.SERVER)
                                                  .withAttribute(HTTP_URL, getUrl(opentracingServer) + "/3"));

        assertThat(server1, hasResourceAttribute(JAEGER_VERSION, "Java-1.6.0"));
        assertThat(client2, hasResourceAttribute(JAEGER_VERSION, "Java-1.6.0"));
        assertThat(server2, hasResourceAttribute(TELEMETRY_SDK_NAME, "opentelemetry"));
        assertThat(client3, hasResourceAttribute(TELEMETRY_SDK_NAME, "opentelemetry"));
        assertThat(server3, hasResourceAttribute(JAEGER_VERSION, "Java-1.6.0"));
    }

}
