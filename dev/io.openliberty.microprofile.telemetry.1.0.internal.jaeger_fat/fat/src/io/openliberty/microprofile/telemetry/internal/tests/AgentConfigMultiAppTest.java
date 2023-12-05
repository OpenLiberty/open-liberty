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
import static io.openliberty.microprofile.telemetry.internal.utils.TestConstants.NULL_TRACE_ID;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerSpanMatcher.hasKind;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerSpanMatcher.hasName;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerSpanMatcher.hasNoParent;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerSpanMatcher.hasParentSpanId;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerSpanMatcher.hasServiceName;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.rules.RuleChain;
import org.junit.AfterClass;

import componenttest.custom.junit.runner.RepeatTestFilter;
import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;
import io.jaegertracing.api_v2.Model.Span;
import io.openliberty.microprofile.telemetry.internal.apps.agentconfig.AgentConfigTestResource;
import io.openliberty.microprofile.telemetry.internal.suite.FATSuite;
import io.openliberty.microprofile.telemetry.internal.utils.TestConstants;
import io.openliberty.microprofile.telemetry.internal.utils.TestUtils;
import io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerContainer;
import io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerQueryClient;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

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

    public static JaegerContainer jaegerContainer = new JaegerContainer().withLogConsumer(new SimpleLogConsumer(JaegerBaseTest.class, "jaeger"));
    public static RepeatTests repeat = FATSuite.allMPRepeats(SERVER_NAME);

    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(jaegerContainer).around(repeat);

    public static JaegerQueryClient client;

    @BeforeClass
    public static void setup() throws Exception {
        client = new JaegerQueryClient(jaegerContainer);

        server.copyFileToLibertyServerRoot("opentelemetry-javaagent.jar");
    }

    @Before
    public void resetServer() throws Exception {
        server.addEnvVar(TestConstants.ENV_OTEL_TRACES_EXPORTER, "otlp");
        server.addEnvVar(TestConstants.ENV_OTEL_EXPORTER_OTLP_ENDPOINT, jaegerContainer.getOltpGrpcUrl());
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
        Span span1 = client.waitForSpansForTraceId(traceId, hasSize(1)).get(0);

        // microprofile-config.properties is not read by the agent
        assertThat(span1, hasServiceName("unknown_service:java"));
        assertThat(span1, hasNoParent());
        assertThat(span1, hasKind(SERVER));
        if (RepeatTestFilter.isRepeatActionActive(TelemetryActions.MP41_MPTEL11_ID) || RepeatTestFilter.isRepeatActionActive(TelemetryActions.MP14_MPTEL11_ID)) {
            assertThat(span1, hasName("/multiApp1"));
        }
        else{
            assertThat(span1, hasName("/multiApp1/"));
        }
        // Test we can call app2
        String traceId2 = new HttpRequest(server, "/multiApp2").run(String.class);
        Span span2 = client.waitForSpansForTraceId(traceId2, hasSize(1)).get(0);

        // microprofile-config.properties is not read by the agent
        assertThat(span2, hasServiceName("unknown_service:java"));
        assertThat(span2, hasNoParent());
        assertThat(span2, hasKind(SERVER));
        if (RepeatTestFilter.isRepeatActionActive(TelemetryActions.MP41_MPTEL11_ID) || RepeatTestFilter.isRepeatActionActive(TelemetryActions.MP14_MPTEL11_ID)) {
            assertThat(span2, hasName("/multiApp2"));
        }
        else{
            assertThat(span2, hasName("/multiApp2/"));
        }
    }
}