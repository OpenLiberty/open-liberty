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

import static io.openliberty.microprofile.telemetry.internal.utils.TestUtils.findOneFrom;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerSpanMatcher.hasKind;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerSpanMatcher.hasName;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerSpanMatcher.hasNoParent;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerSpanMatcher.hasParentSpanId;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerSpanMatcher.hasServiceName;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.MaximumJavaLevel;
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
import io.jaegertracing.api_v2.Model.Span;
import io.openliberty.microprofile.telemetry.internal.apps.agentconfig.AgentConfigTestResource;
import io.openliberty.microprofile.telemetry.internal.utils.KeyPairs;
import io.openliberty.microprofile.telemetry.internal.utils.TestConstants;
import io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerContainer;
import io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerQueryClient;
import io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerSpanMatcher;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

/**
 * Test all the ways the agent can be configured
 *
 * Each test starts the server with a different config, making this test a bit slow so it only runs in FULL mode
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@MaximumJavaLevel(javaLevel = 20)
public class AgentConfigMultiAppTest {

    public static final String SERVER_NAME = "TelemetryAgentMultiAppConfig";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    private static KeyPairs keyPairs = new KeyPairs(server);

    public static JaegerContainer jaegerContainer = new JaegerContainer(keyPairs.getCertificate(),
                                                                        keyPairs.getKey()).withLogConsumer(new SimpleLogConsumer(AgentConfigMultiAppTest.class, "jaeger"));
    public static RepeatTests repeat = TelemetryActions.latestTelemetryRepeats(SERVER_NAME);

    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(jaegerContainer).around(repeat);

    public static JaegerQueryClient client;

    @BeforeClass
    public static void setup() throws Exception {

        client = new JaegerQueryClient(jaegerContainer, keyPairs.getCertificate());

        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            server.copyFileToLibertyServerRoot("agent-119/opentelemetry-javaagent.jar");
        } 
        else if(RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP61_ID)){
            server.copyFileToLibertyServerRoot("agent-129/opentelemetry-javaagent.jar");
        }
        else {
            server.copyFileToLibertyServerRoot("agent-250/opentelemetry-javaagent.jar");
        }
    }

    @Before
    public void resetServer() throws Exception {
        server.addEnvVar(TestConstants.ENV_OTEL_TRACES_EXPORTER, "otlp");
        server.addEnvVar(TestConstants.ENV_OTEL_EXPORTER_OTLP_ENDPOINT, jaegerContainer.getOtlpGrpcUrl());
        //The default OTLP protocol has been changed from grpc to http/protobuf in the Java Agent v2.5.0
        server.addEnvVar(TestConstants.ENV_OTEL_EXPORTER_OTLP_PROTOCOL, "grpc");
        server.addEnvVar("OTEL_METRICS_EXPORTER", "none");
        server.addEnvVar("OTEL_LOGS_EXPORTER", "none");
        server.addEnvVar(TestConstants.ENV_OTEL_BSP_SCHEDULE_DELAY, "100"); // Wait no more than 100ms to send traces to the server
        server.addEnvVar(TestConstants.ENV_OTEL_SDK_DISABLED, "false"); //Enable tracing
    }

    @After
    public void ensureStopped() throws Exception {
        server.stopServer();
    }

    @AfterClass
    public static void closeClient() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    @Test
    // Skipping for MP 5.0, 6.1 and 7.0 as JavaAgent 1.29 is sometimes successful and sometimes fails (possible classLoader issue in JavaAgent (BUG))
    @SkipForRepeat({ TelemetryActions.MP50_MPTEL11_ID, MicroProfileActions.MP61_ID, TelemetryActions.MP50_MPTEL20_ID, MicroProfileActions.MP70_EE10_ID, MicroProfileActions.MP70_EE11_ID })
    public void testAgentMultiApp() throws Exception {
        PropertiesAsset app1Config = new PropertiesAsset().addProperty("otel.service.name", "multi-app-1");
        WebArchive app1 = ShrinkWrap.create(WebArchive.class, "multiApp1.war")
                                    .addPackage(AgentConfigTestResource.class.getPackage())
                                    .addAsResource(app1Config, "META-INF/microprofile-config.properties");
        ShrinkHelper.exportAppToServer(server, app1, DeployOptions.SERVER_ONLY);

        PropertiesAsset app2Config = new PropertiesAsset().addProperty("otel.service.name", "multi-app-2");
        WebArchive app2 = ShrinkWrap.create(WebArchive.class, "multiApp2.war")
                                    .addPackage(AgentConfigTestResource.class.getPackage())
                                    .addAsResource(app2Config, "META-INF/microprofile-config.properties");
        ShrinkHelper.exportAppToServer(server, app2, DeployOptions.SERVER_ONLY);

        server.startServer();

        // Test we can call app1
        String traceId = new HttpRequest(server, "/multiApp1").run(String.class);
        Span serverSpan = null;
        Span internalSpan = null;

        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            // MP6.0 uses JavaAgent 1.19 therefore the internal span is not created
            serverSpan = client.waitForSpansForTraceId(traceId, hasSize(1)).get(0);
        } else {
            List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(2));
            serverSpan = findOneFrom(spans, hasNoParent());
            internalSpan = findOneFrom(spans, hasParentSpanId(serverSpan.getSpanId()));
            assertThat(internalSpan, hasKind(INTERNAL));
        }

        // microprofile-config.properties is not read by the agent
        assertThat(serverSpan, not(hasServiceName("multi-app-1")));
        assertThat(serverSpan, hasNoParent());
        assertThat(serverSpan, hasKind(SERVER));
        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            // MP6.0 uses JavaAgent 1.19 therefore the internal span is not created
            assertThat(serverSpan, hasName("/multiApp1/"));
        } else if (TelemetryActions.EE7orEE8Mp20IsActive()) {
            assertThat(serverSpan, hasName("GET /multiApp1"));
            assertThat(serverSpan, JaegerSpanMatcher.isSpan().withTraceId(traceId)
                                                    .withAttribute(SemanticAttributes.HTTP_ROUTE, "/multiApp1")
                                                    .withAttribute(SemanticAttributes.HTTP_TARGET, "/multiApp1")
                                                    .withAttribute(SemanticAttributes.HTTP_METHOD, "GET"));
        } else {
            assertThat(serverSpan, hasName("GET /multiApp1/"));
            assertThat(serverSpan, JaegerSpanMatcher.isSpan().withTraceId(traceId)
                                                    .withAttribute(SemanticAttributes.HTTP_ROUTE, "/multiApp1/")
                                                    .withAttribute(SemanticAttributes.HTTP_TARGET, "/multiApp1")
                                                    .withAttribute(SemanticAttributes.HTTP_METHOD, "GET"));
        }
        // Test we can call app2
        String traceId2 = new HttpRequest(server, "/multiApp2").run(String.class);
        Span serverSpan2 = null;
        Span internalSpan2 = null;

        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            // MP6.0 uses JavaAgent 1.19 therefore the internal span is not created
            serverSpan2 = client.waitForSpansForTraceId(traceId2, hasSize(1)).get(0);
        } else {
            List<Span> spans = client.waitForSpansForTraceId(traceId2, hasSize(2));
            serverSpan2 = findOneFrom(spans, hasNoParent());
            internalSpan2 = findOneFrom(spans, hasParentSpanId(serverSpan2.getSpanId()));
            assertThat(internalSpan2, hasKind(INTERNAL));
        }

        // microprofile-config.properties is not read by the agent
        assertThat(serverSpan2, not(hasServiceName("multi-app-2")));
        assertThat(serverSpan2, hasNoParent());
        assertThat(serverSpan2, hasKind(SERVER));
        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            // MP6.0 uses JavaAgent 1.19 therefore the internal span is not created
            assertThat(serverSpan2, hasName("/multiApp2/"));
            assertThat(serverSpan2, JaegerSpanMatcher.isSpan().withTraceId(traceId2)
                                                     .withAttribute(SemanticAttributes.HTTP_ROUTE, "/multiApp2/")
                                                     .withAttribute(SemanticAttributes.HTTP_TARGET, "/multiApp2")
                                                     .withAttribute(SemanticAttributes.HTTP_METHOD, "GET"));
        } else if (TelemetryActions.EE7orEE8Mp20IsActive()) {
            assertThat(serverSpan2, hasName("GET /multiApp2"));
            assertThat(serverSpan2, JaegerSpanMatcher.isSpan().withTraceId(traceId2)
                                                     .withAttribute(SemanticAttributes.HTTP_ROUTE, "/multiApp2")
                                                     .withAttribute(SemanticAttributes.HTTP_TARGET, "/multiApp2")
                                                     .withAttribute(SemanticAttributes.HTTP_METHOD, "GET"));
        } else {
            assertThat(serverSpan2, hasName("GET /multiApp2/"));
            assertThat(serverSpan2, JaegerSpanMatcher.isSpan().withTraceId(traceId2)
                                                     .withAttribute(SemanticAttributes.HTTP_ROUTE, "/multiApp2/")
                                                     .withAttribute(SemanticAttributes.HTTP_TARGET, "/multiApp2")
                                                     .withAttribute(SemanticAttributes.HTTP_METHOD, "GET"));
        }

    }
}