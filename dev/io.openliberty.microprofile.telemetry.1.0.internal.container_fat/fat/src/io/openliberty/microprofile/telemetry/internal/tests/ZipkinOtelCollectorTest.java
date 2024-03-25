/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
import static io.openliberty.microprofile.telemetry.internal.utils.zipkin.ZipkinSpanMatcher.span;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

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
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
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
import io.openliberty.microprofile.telemetry.internal.suite.FATSuite;
import io.openliberty.microprofile.telemetry.internal.utils.TestConstants;
import io.openliberty.microprofile.telemetry.internal.utils.otelCollector.OtelCollectorContainer;
import io.openliberty.microprofile.telemetry.internal.utils.zipkin.ZipkinContainer;
import io.openliberty.microprofile.telemetry.internal.utils.zipkin.ZipkinQueryClient;
import io.openliberty.microprofile.telemetry.internal.utils.zipkin.ZipkinSpan;
import io.openliberty.microprofile.telemetry.internal.utils.zipkin.ZipkinSpanMatcher;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

/**
 * Test exporting traces to a Zipkin server with the OpenTelemetry Collector
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ZipkinOtelCollectorTest {

    private static final Class<?> c = ZipkinTest.class;
    private static final String SERVER_NAME = "spanTestServer";

    public static Network network = Network.newNetwork();
    public static ZipkinContainer zipkinContainer = new ZipkinContainer()
                                                                         .withLogConsumer(new SimpleLogConsumer(ZipkinTest.class, "zipkin"))
                                                                         .withNetwork(network)
                                                                         .withNetworkAliases("zipkin-all-in-one");
    public static OtelCollectorContainer otelCollectorContainer = new OtelCollectorContainer(new File("lib/LibertyFATTestFiles/otel-collector-config-zipkin.yaml"))
                                                                                                                                                                   .withNetwork(network)
                                                                                                                                                                   .withNetworkAliases("otel-collector-zipkin")
                                                                                                                                                                   .withLogConsumer(new SimpleLogConsumer(ZipkinOtelCollectorTest.class,
                                                                                                                                                                                                          "otelCol"));
    public static RepeatTests repeat = FATSuite.allMPRepeats(SERVER_NAME);

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

        server.addEnvVar(TestConstants.ENV_OTEL_TRACES_EXPORTER, "otlp");
        server.addEnvVar(TestConstants.ENV_OTEL_EXPORTER_OTLP_ENDPOINT, otelCollectorContainer.getOtlpGrpcUrl());
        server.addEnvVar(TestConstants.ENV_OTEL_SERVICE_NAME, "Test service");
        server.addEnvVar(TestConstants.ENV_OTEL_BSP_SCHEDULE_DELAY, "100"); // Wait no more than 100ms to send traces to the server
        server.addEnvVar(TestConstants.ENV_OTEL_SDK_DISABLED, "false"); //Enable tracing

        // Construct the test application
        WebArchive spanTest = ShrinkWrap.create(WebArchive.class, "spanTest.war")
                                        .addPackage(TestResource.class.getPackage());
        ShrinkHelper.exportAppToServer(server, spanTest, SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testBasic() throws Exception {
        HttpRequest request = new HttpRequest(server, "/spanTest");

        String traceId = request.run(String.class);
        Log.info(c, "testBasic", "TraceId is " + traceId);

        List<ZipkinSpan> spans = client.waitForSpansForTraceId(traceId, hasSize(1));
        Log.info(c, "testBasic", "Spans returned: " + spans);

        ZipkinSpan span = spans.get(0);

        assertThat(span, span().withTraceId(traceId)
                               .withTag(SemanticAttributes.HTTP_ROUTE.getKey(), "/spanTest/")
                               .withTag(SemanticAttributes.HTTP_METHOD.getKey(), "GET"));
    }

    @Test
    public void testEventAdded() throws Exception {
        HttpRequest request = new HttpRequest(server, "/spanTest/eventAdded");
        String traceId = request.run(String.class);
        List<ZipkinSpan> spans = client.waitForSpansForTraceId(traceId, hasSize(1));

        ZipkinSpan span = spans.get(0);

        assertThat(span, hasAnnotation(TestResource.TEST_EVENT_NAME));
    }

    @Test
    public void testSubspan() throws Exception {
        HttpRequest request = new HttpRequest(server, "/spanTest/subspan");
        String traceId = request.run(String.class);
        List<ZipkinSpan> spans = client.waitForSpansForTraceId(traceId, hasSize(2));

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

        // Note that zipkin lowercases the name
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
        List<ZipkinSpan> spans = client.waitForSpansForTraceId(traceId, hasSize(1));
        ZipkinSpan span = spans.get(0);
        assertThat(span, ZipkinSpanMatcher.hasAnnotation(containsString("exception")));
    }

    @Test
    public void testAttributeAdded() throws Exception {
        HttpRequest request = new HttpRequest(server, "/spanTest/attributeAdded");
        String traceId = request.run(String.class);
        List<ZipkinSpan> spans = client.waitForSpansForTraceId(traceId, hasSize(1));

        ZipkinSpan span = spans.get(0);

        assertThat(span, ZipkinSpanMatcher.hasTag(TestResource.TEST_ATTRIBUTE_KEY.getKey(), TestResource.TEST_ATTRIBUTE_VALUE));
    }

    private boolean hasParent(ZipkinSpan span) {
        return span.getParentId() != null;
    }

}
