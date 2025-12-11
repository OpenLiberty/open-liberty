/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
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
import static io.openliberty.microprofile.telemetry.internal.utils.zipkin.ZipkinSpanMatcher.hasAnnotation;
import static io.openliberty.microprofile.telemetry.internal.utils.zipkin.ZipkinSpanMatcher.hasNoParent;
import static io.openliberty.microprofile.telemetry.internal.utils.zipkin.ZipkinSpanMatcher.hasParentSpanId;
import static io.openliberty.microprofile.telemetry.internal.utils.zipkin.ZipkinSpanMatcher.hasTag;
import static io.openliberty.microprofile.telemetry.internal.utils.zipkin.ZipkinSpanMatcher.span;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_ROUTE;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import com.ibm.websphere.simplicity.log.Log;

import java.io.File;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Network;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.microprofile.telemetry.internal.apps.spanTest.TestResource;
import io.openliberty.microprofile.telemetry.internal.utils.TestConstants;
import io.openliberty.microprofile.telemetry.internal.utils.otelCollector.OtelCollectorContainer;
import io.openliberty.microprofile.telemetry.internal.utils.zipkin.ZipkinContainer;
import io.openliberty.microprofile.telemetry.internal.utils.zipkin.ZipkinQueryClient;
import io.openliberty.microprofile.telemetry.internal.utils.zipkin.ZipkinSpan;
import io.openliberty.microprofile.telemetry.internal.utils.zipkin.ZipkinSpanMatcher;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

/**
 * Test exporting traces to a Zipkin server with the OpenTelemetry Collector
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ZipkinOtelCollectorTest {

    private static final Class<?> c = ZipkinOtelCollectorTest.class;
    private static final String SERVER_NAME = "spanTestServer";

    public static Network network = Network.newNetwork();
    public static ZipkinContainer zipkinContainer = new ZipkinContainer()
                                                                         .withLogConsumer(new SimpleLogConsumer(ZipkinTest.class, "zipkin"))
                                                                         .withNetwork(network)
                                                                         .withNetworkAliases("zipkin-all-in-one");

    public static OtelCollectorContainer otelCollectorContainer = new OtelCollectorContainer(
                                                                                             new File("lib/LibertyFATTestFiles/otel-collector-config-zipkin.yaml"))
                                                                                                                                                                   .withNetwork(network)
                                                                                                                                                                   .withNetworkAliases("otel-collector-zipkin")
                                                                                                                                                                   .withLogConsumer(new SimpleLogConsumer(ZipkinOtelCollectorTest.class,
                                                                                                                                                                                                          "otelCol"));

    public static RepeatTests repeat = TelemetryActions.latestTelemetryRepeats(SERVER_NAME);

    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(network)
                                             .around(zipkinContainer)
                                             .around(otelCollectorContainer)
                                             .around(repeat);

    public ZipkinQueryClient client = new ZipkinQueryClient(zipkinContainer);

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        // Configure exporter → collector (gRPC)
        server.addEnvVar(TestConstants.ENV_OTEL_TRACES_EXPORTER, "otlp");
        server.addEnvVar(TestConstants.ENV_OTEL_EXPORTER_OTLP_ENDPOINT, otelCollectorContainer.getOtlpGrpcUrl());
        server.addEnvVar(TestConstants.ENV_OTEL_EXPORTER_OTLP_PROTOCOL, "grpc");
        // Some older agents only honor the per-signal endpoint:
        server.addEnvVar("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", otelCollectorContainer.getOtlpGrpcUrl());

        // Service + BSP batching config
        server.addEnvVar(TestConstants.ENV_OTEL_SERVICE_NAME, "Test service");
        server.addEnvVar(TestConstants.ENV_OTEL_BSP_SCHEDULE_DELAY, "100");
        server.addEnvVar(TestConstants.ENV_OTEL_BSP_MAX_EXPORT_BATCH_SIZE, "1");
        server.addEnvVar(TestConstants.ENV_OTEL_SDK_DISABLED, "false");
        server.addEnvVar(TestConstants.ENV_OTEL_TRACES_SAMPLER, "always_on");

        // Deploy app
        WebArchive spanTest = ShrinkWrap.create(WebArchive.class, "spanTest.war")
                                        .addPackage(TestResource.class.getPackage());
        ShrinkHelper.exportAppToServer(server, spanTest, SERVER_ONLY);
        server.startServer();

        // Readiness probe (prevents 404 + cold pipeline flakes)
        // Up to ~10s, sending a warm-up request until the endpoint responds Ok
        int attempts = 0;
        while (attempts < 20) { // 20 * 500ms = 10s max
            try {
                new HttpRequest(server, "/spanTest").run(String.class);
                break; // success, app is ready and first span emitted
            } catch (Exception e) {
                Thread.sleep(500);
                attempts++;
            }
        }

        // Additional warm-up for older stacks
        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP70_EE10_ID)
            || RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP61_ID)
            || RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)
            || RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP70_EE11_ID)) {
            try {
                Thread.sleep(1000);
                new HttpRequest(server, "/spanTest").run(String.class);
                Thread.sleep(400);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @Test
    @SkipForRepeat({ TelemetryActions.MP14_MPTEL20_ID, TelemetryActions.MP41_MPTEL20_ID, TelemetryActions.MP50_MPTEL20_ID,
                     TelemetryActions.MP50_MPTEL20_JAVA8_ID, TelemetryActions.MP61_MPTEL20_ID,
                     MicroProfileActions.MP70_EE10_ID, MicroProfileActions.MP70_EE11_ID })
    public void testBasicTelemetry1() throws Exception {
        HttpRequest request = new HttpRequest(server, "/spanTest");
        String traceId = request.run(String.class);
        Log.info(c, "testBasic", "TraceId is " + traceId);

        List<ZipkinSpan> spans = waitForSpansWithGrace(traceId, 1);
        Log.info(c, "testBasic", "Spans returned: " + spans);
        ZipkinSpan span = spans.get(0);

        assertThat(span, allOf(
                               span().withTraceId(traceId),
                               hasTag(HTTP_ROUTE.getKey(), "/spanTest/"),
                               anyOf(
                                     hasTag("http.method", "GET"), // legacy key
                                     hasTag(HTTP_REQUEST_METHOD.getKey(), "GET") // current key
                               )));
    }

    @Test
    @SkipForRepeat({ MicroProfileActions.MP60_ID, TelemetryActions.MP14_MPTEL11_ID, TelemetryActions.MP41_MPTEL11_ID,
                     TelemetryActions.MP50_MPTEL11_ID, MicroProfileActions.MP61_ID })
    public void testBasicTelemetry2() throws Exception {
        HttpRequest request = new HttpRequest(server, "/spanTest");
        String traceId = request.run(String.class);
        Log.info(c, "testBasic", "TraceId is " + traceId);

        List<ZipkinSpan> spans = waitForSpansWithGrace(traceId, 1);
        Log.info(c, "testBasic", "Spans returned: " + spans);
        ZipkinSpan span = spans.get(0);

        assertThat(span, span().withTraceId(traceId)
                               .withTag(HTTP_ROUTE.getKey(), "/spanTest/")
                               .withTag(HTTP_REQUEST_METHOD.getKey(), "GET"));
    }

    @Test
    public void testEventAdded() throws Exception {
        HttpRequest request = new HttpRequest(server, "/spanTest/eventAdded");
        String traceId = request.run(String.class);

        List<ZipkinSpan> spans = waitForSpansWithGrace(traceId, 1);
        ZipkinSpan span = spans.get(0);
        assertThat(span, hasAnnotation(TestResource.TEST_EVENT_NAME));
    }

    @Test
    public void testSubspan() throws Exception {
        HttpRequest request = new HttpRequest(server, "/spanTest/subspan");
        String traceId = request.run(String.class);

        List<ZipkinSpan> spans = waitForSpansWithGrace(traceId, 2);

        ZipkinSpan parent, child;
        if (hasParent(spans.get(0))) {
            child = spans.get(0);
            parent = spans.get(1);
        } else {
            child = spans.get(1);
            parent = spans.get(0);
        }

        assertThat(parent, hasNoParent());
        assertThat(child, hasParentSpanId(parent.getId()));

        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            assertThat(parent, hasProperty("name", equalToIgnoringCase("/spanTest/subspan")));
        } else {
            assertThat(parent, hasProperty("name", equalToIgnoringCase("get /spanTest/subspan")));
        }
        assertThat(child, hasProperty("name", equalToIgnoringCase(TestResource.TEST_OPERATION_NAME)));
    }

    @Test
    public void testExceptionRecorded() throws Exception {
        HttpRequest request = new HttpRequest(server, "/spanTest/exception");
        String traceId = request.run(String.class);

        List<ZipkinSpan> spans = waitForSpansWithGrace(traceId, 1);
        ZipkinSpan span = spans.get(0);
        assertThat(span, ZipkinSpanMatcher.hasAnnotation(containsString("exception")));
    }

    @Test
    public void testAttributeAdded() throws Exception {
        HttpRequest request = new HttpRequest(server, "/spanTest/attributeAdded");
        String traceId = request.run(String.class);

        List<ZipkinSpan> spans = waitForSpansWithGrace(traceId, 1);
        ZipkinSpan span = spans.get(0);
        assertThat(span, ZipkinSpanMatcher.hasTag(TestResource.TEST_ATTRIBUTE_KEY.getKey(), TestResource.TEST_ATTRIBUTE_VALUE));
    }

    private boolean hasParent(ZipkinSpan span) {
        return span.getParentId() != null;
    }

    private List<ZipkinSpan> waitForSpansWithGrace(String traceId, int expectedSize) throws Exception {
        long graceMs = 750; // default

        // Old stacks: more time
        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            graceMs = 2500;
        } else if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP61_ID)
                   || RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP70_EE10_ID)) {
            graceMs = 1500;
        } else if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP70_EE11_ID)) {
            graceMs = 2000;
        }

        Thread.sleep(graceMs);
        return client.waitForSpansForTraceId(traceId, hasSize(expectedSize));
    }
}
