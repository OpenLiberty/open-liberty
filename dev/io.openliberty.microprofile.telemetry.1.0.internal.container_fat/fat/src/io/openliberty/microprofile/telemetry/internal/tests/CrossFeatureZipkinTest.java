/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.tests;

import static io.openliberty.microprofile.telemetry.internal.utils.TestUtils.findOneFrom;
import static io.openliberty.microprofile.telemetry.internal.utils.zipkin.ZipkinSpanMatcher.hasTag;
import static io.openliberty.microprofile.telemetry.internal.utils.zipkin.ZipkinSpanMatcher.span;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

// In MpTelemetry-2.0 SemanticAttributes was moved to a new package, so we use import static to allow both versions to coexist
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.SemanticAttributes.URL_FULL;

import io.opentracing.tag.Tags;

import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.TELEMETRY_SDK_NAME;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
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
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.microprofile.telemetry.internal.utils.TestConstants;
import io.openliberty.microprofile.telemetry.internal.utils.zipkin.ZipkinContainer;
import io.openliberty.microprofile.telemetry.internal.utils.zipkin.ZipkinQueryClient;
import io.openliberty.microprofile.telemetry.internal.utils.zipkin.ZipkinSpan;
import io.opentelemetry.api.trace.SpanKind;
import componenttest.rules.repeater.MicroProfileActions;
/**
 * Tests propagating traces between liberty servers running mpOpenTracing-3.0 and mpTelemetry-1.0, feeding traces to the same trace server.
 * Spans are exported to Zipkin
 */
@RunWith(FATRunner.class)
public class CrossFeatureZipkinTest {

    private static final String CROSS_FEATURE_TELEMETRY_SERVER = "crossFeatureTelemetryServer";
    private static final String APP_NAME = "crossFeature";
    private static final Class<?> c = CrossFeatureZipkinTest.class;

    public static ZipkinContainer zipkinContainer = new ZipkinContainer().withLogConsumer(new SimpleLogConsumer(ZipkinTest.class, "zipkin"));
    public static RepeatTests repeat = TelemetryActions.latestTelemetryRepeats(CROSS_FEATURE_TELEMETRY_SERVER);

    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(zipkinContainer).around(repeat);

    public static ZipkinQueryClient client;

    @Server("crossFeatureOpenTracingZipkinServer")
    public static LibertyServer opentracingServer;

    @Server(CROSS_FEATURE_TELEMETRY_SERVER)
    public static LibertyServer telemetryServer;

    @BeforeClass
    public static void setUp() throws Exception {

        client = new ZipkinQueryClient(zipkinContainer);

        // Inform the test framework that opentracingServer is configured to use the secondary HTTP ports
        opentracingServer.useSecondaryHTTPPort();

        telemetryServer.addEnvVar(TestConstants.ENV_OTEL_TRACES_EXPORTER, "zipkin");
        telemetryServer.addEnvVar(TestConstants.ENV_OTEL_EXPORTER_ZIPKIN_ENDPOINT, zipkinContainer.getApiBaseUrl() + "/spans");
        telemetryServer.addEnvVar(TestConstants.ENV_OTEL_BSP_SCHEDULE_DELAY, "100"); // Wait no more than 100ms to send traces to the server
        telemetryServer.addEnvVar(TestConstants.ENV_OTEL_SDK_DISABLED, "false"); //Enable tracing
        telemetryServer.addEnvVar("OTEL_PROPAGATORS", "tracecontext, b3"); // Include the b3 propagation header for Zipkin
        telemetryServer.addEnvVar("IO_OPENLIBERTY_MICROPROFILE_TELEMETRY_INTERNAL_APPS_CROSSFEATURE_TELEMETRY_CROSSFEATURECLIENT_MP_REST_URL", getUrl(opentracingServer));

        opentracingServer.addEnvVar("zipkinPortName", Integer.toString(zipkinContainer.getHttpPort()));
        opentracingServer.addEnvVar("zipkinHostName", zipkinContainer.getHost());
        opentracingServer.addEnvVar("ZIPKIN_SAMPLER_TYPE", "const"); // Trace every call
        opentracingServer.addEnvVar("ZIPKIN_SAMPLER_PARAM", "1"); // Trace every call
        opentracingServer.addEnvVar("IO_OPENLIBERTY_MICROPROFILE_TELEMETRY_INTERNAL_APPS_CROSSFEATURE_OPENTRACING_CROSSFEATURECLIENT_MP_REST_URL", getUrl(telemetryServer));

        opentracingServer.installUserBundle("com.ibm.ws.io.opentracing.zipkintracer-0.33");
        opentracingServer.installUserFeature("opentracingZipkin-0.33");

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
        List<ZipkinSpan> spans = client.waitForSpansForTraceId(traceId, hasSize(5));

        for (ZipkinSpan span : spans) {
            Log.info(c, "testCrossFeatureFromTelemetry", span.toString());
        }
        //OpenTracing, MpTelemetry-1.0 and MpTelemetry-1.1 use HTTP_URL while MpTelemetry 2.0 uses URL_FULL
        if (RepeatTestFilter.isRepeatActionActive(TelemetryActions.MP14_MPTEL20_ID) || RepeatTestFilter.isRepeatActionActive(TelemetryActions.MP41_MPTEL20_ID) || RepeatTestFilter.isRepeatActionActive(TelemetryActions.MP50_MPTEL20_ID) || RepeatTestFilter.isRepeatActionActive(TelemetryActions.MP61_MPTEL20_ID) || RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP70_EE10_ID) || RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP70_EE11_ID)) {
            ZipkinSpan server1 = findOneFrom(spans, hasTag(HTTP_ROUTE.getKey(), "/crossFeature/1"));
            ZipkinSpan client2 = findOneFrom(spans, span().withKind(SpanKind.CLIENT)
                                                        .withTag(URL_FULL.getKey(), getUrl(opentracingServer) + "/2"));
            ZipkinSpan server2 = findOneFrom(spans, span().withKind(SpanKind.SERVER)
                                                        .withTag(Tags.HTTP_URL.getKey(), getUrl(opentracingServer) + "/2"));

            System.out.println("Server2 from MPTel20 " + server2);
            ZipkinSpan client3 = findOneFrom(spans, hasTag(Tags.HTTP_URL.getKey(), getUrl(telemetryServer) + "/3"));
            ZipkinSpan server3 = findOneFrom(spans, hasTag(HTTP_ROUTE.getKey(), "/crossFeature/3"));

            assertThat(server1, hasTag("otel.scope.name", "io.openliberty.microprofile.telemetry"));
            assertThat(client2, hasTag("otel.scope.name", "Client filter"));
        } else {
            ZipkinSpan server1 = findOneFrom(spans, hasTag(SemanticAttributes.HTTP_ROUTE.getKey(), "/crossFeature/1"));
            ZipkinSpan client2 = findOneFrom(spans, span().withKind(SpanKind.CLIENT)
                                                        .withTag(SemanticAttributes.HTTP_URL.getKey(), getUrl(opentracingServer) + "/2"));
            ZipkinSpan server2 = findOneFrom(spans, span().withKind(SpanKind.SERVER)
                                                        .withTag(SemanticAttributes.HTTP_URL.getKey(), getUrl(opentracingServer) + "/2"));
            System.out.println("Server2 from MPTel10 or 11" + server2);
            ZipkinSpan client3 = findOneFrom(spans, hasTag(SemanticAttributes.HTTP_URL.getKey(), getUrl(telemetryServer) + "/3"));
            ZipkinSpan server3 = findOneFrom(spans, hasTag(SemanticAttributes.HTTP_ROUTE.getKey(), "/crossFeature/3"));

            assertThat(server1, hasTag("otel.scope.name", "io.openliberty.microprofile.telemetry"));
            assertThat(client2, hasTag("otel.scope.name", "Client filter"));
        }
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
        List<ZipkinSpan> spans = client.waitForSpansForTraceId(traceId, hasSize(5));

        for (ZipkinSpan span : spans) {
            Log.info(c, "testCrossFeatureFromOpenTracing", span.toString());
        }
        //OpenTracing, MpTelemetry-1.0 and MpTelemetry-1.1 use HTTP_URL while MpTelemetry 2.0 uses URL_FULL
        if (RepeatTestFilter.isRepeatActionActive(TelemetryActions.MP14_MPTEL20_ID) || RepeatTestFilter.isRepeatActionActive(TelemetryActions.MP41_MPTEL20_ID) || RepeatTestFilter.isRepeatActionActive(TelemetryActions.MP50_MPTEL20_ID) || RepeatTestFilter.isRepeatActionActive(TelemetryActions.MP61_MPTEL20_ID) || RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP70_EE10_ID) || RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP70_EE11_ID)) {
            ZipkinSpan server1 = findOneFrom(spans, span().withTag(Tags.HTTP_URL.getKey(), getUrl(opentracingServer) + "/1"));
            ZipkinSpan client2 = findOneFrom(spans, span().withTag(Tags.HTTP_URL.getKey(), getUrl(telemetryServer) + "/2"));
            ZipkinSpan server2 = findOneFrom(spans, hasTag(HTTP_ROUTE.getKey(), "/crossFeature/2"));
            ZipkinSpan client3 = findOneFrom(spans, span().withKind(SpanKind.CLIENT)
                                                        .withTag(URL_FULL.getKey(), getUrl(opentracingServer) + "/3"));
            ZipkinSpan server3 = findOneFrom(spans, span().withKind(SpanKind.SERVER)
                                                        .withTag(Tags.HTTP_URL.getKey(), getUrl(opentracingServer) + "/3"));

            assertThat(server2, hasTag("otel.scope.name", "io.openliberty.microprofile.telemetry"));
            assertThat(client3, hasTag("otel.scope.name", "Client filter"));
        }else{
            ZipkinSpan server1 = findOneFrom(spans, span().withTag(SemanticAttributes.HTTP_URL.getKey(), getUrl(opentracingServer) + "/1"));
            ZipkinSpan client2 = findOneFrom(spans, span().withTag(SemanticAttributes.HTTP_URL.getKey(), getUrl(telemetryServer) + "/2"));
            ZipkinSpan server2 = findOneFrom(spans, hasTag(SemanticAttributes.HTTP_ROUTE.getKey(), "/crossFeature/2"));
            ZipkinSpan client3 = findOneFrom(spans, span().withKind(SpanKind.CLIENT)
                                                        .withTag(SemanticAttributes.HTTP_URL.getKey(), getUrl(opentracingServer) + "/3"));
            ZipkinSpan server3 = findOneFrom(spans, span().withKind(SpanKind.SERVER)
                                                        .withTag(SemanticAttributes.HTTP_URL.getKey(), getUrl(opentracingServer) + "/3"));

            assertThat(server2, hasTag("otel.scope.name", "io.openliberty.microprofile.telemetry"));
            assertThat(client3, hasTag("otel.scope.name", "Client filter"));
        }
    }


}
