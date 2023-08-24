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
import static io.openliberty.microprofile.telemetry.internal.utils.TestConstants.NULL_TRACE_ID;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.SpanMatcher.hasKind;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.SpanMatcher.hasName;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.SpanMatcher.hasNoParent;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.SpanMatcher.hasParentSpanId;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.SpanMatcher.hasServiceName;
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

import com.ibm.websphere.simplicity.LocalFile;
import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;
import io.jaegertracing.api_v2.Model.Span;
import io.openliberty.microprofile.telemetry.internal.apps.agentconfig.AgentConfigTestResource;
import io.openliberty.microprofile.telemetry.internal.utils.TestConstants;
import io.openliberty.microprofile.telemetry.internal.utils.TestUtils;
import io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerContainer;
import io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerQueryClient;

/**
 * Test all the ways the agent can be configured
 *
 * Each test starts the server with a different config, making this test a bit slow so it only runs in FULL mode
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@MaximumJavaLevel(javaLevel = 20)
public class AgentConfigTest {

    @Server("TelemetryAgentConfig")
    public static LibertyServer server;

    @ClassRule
    public static JaegerContainer jaegerContainer = new JaegerContainer().withLogConsumer(new SimpleLogConsumer(JaegerBaseTest.class, "jaeger"));

    public static JaegerQueryClient client;

    @BeforeClass
    public static void setup() throws Exception {
        client = new JaegerQueryClient(jaegerContainer);

        server.copyFileToLibertyServerRoot("opentelemetry-javaagent.jar");

        // Construct the test application
        WebArchive agentTest = ShrinkWrap.create(WebArchive.class, "agentTest.war")
                                         .addPackage(AgentConfigTestResource.class.getPackage());
        ShrinkHelper.exportAppToServer(server, agentTest, SERVER_ONLY);
    }

    @Before
    public void resetServer() throws Exception {
        // Replace any test files with their original versions
        copyToServer("server.xml-original", "server.xml");
        copyToServer("jvm.options-original", "jvm.options");
        copyToServer("bootstrap.properties-original", "bootstrap.properties");
        deleteFromServer("agent-config.properties");

        // Reset the expected apps
        server.removeAllInstalledAppsForValidation();
        server.addInstalledAppForValidation("agentTest");

        // Env vars are cleared when the server starts, so we need to set the core ones up again
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

    @Test
    public void testConfigFromAgentFile() throws Exception {
        copyToServer("agent-config.properties", "agent-config.properties");
        server.addEnvVar("OTEL_JAVAAGENT_CONFIGURATION_FILE", "agent-config.properties");

        server.startServer();

        String traceId = new HttpRequest(server, "/agentTest").run(String.class);
        Span span = client.waitForSpansForTraceId(traceId, hasSize(1)).get(0);

        assertThat(span, hasServiceName("agent-config-test-service"));
    }

    @Test
    public void testConfigFromJvmOptions() throws Exception {
        copyToServer("jvm.options-test", "jvm.options");

        server.startServer();

        String traceId = new HttpRequest(server, "/agentTest").run(String.class);
        Span span = client.waitForSpansForTraceId(traceId, hasSize(1)).get(0);

        assertThat(span, hasServiceName("jvm-options-test-service"));
    }

    @Test
    public void testConfigNotReadFromBootstrap() throws Exception {
        copyToServer("bootstrap.properties-test", "bootstrap.properties");

        server.startServer();

        String traceId = new HttpRequest(server, "/agentTest").run(String.class);
        Span span = client.waitForSpansForTraceId(traceId, hasSize(1)).get(0);

        // bootstrap.properties is not read by the agent
        // It's processed as one of the first things liberty does on startup,
        // but that's still too late for the agent to see its changes
        assertThat(span, hasServiceName("unknown_service:java"));
    }

    @Test
    public void testDisableInstrumentation() throws Exception {
        // Disable @WithSpan instrumentation
        server.addEnvVar("OTEL_INSTRUMENTATION_OPENTELEMETRY_INSTRUMENTATION_ANNOTATIONS_ENABLED", "false");

        server.startServer();

        String traceId = new HttpRequest(server, "/agentTest/withspan").run(String.class);
        Span span = client.waitForSpansForTraceId(traceId, hasSize(1)).get(0);

        // We should only have one span, it should be the server span
        // No span was created for the WithSpan annotation
        assertThat(span, hasKind(SERVER));
    }

    @Test
    public void testEnableSpecificInstrumentation() throws Exception {
        // Enable only @WithSpan instrumentation
        server.addEnvVar("OTEL_INSTRUMENTATION_COMMON_DEFAULT_ENABLED", "false");
        server.addEnvVar("OTEL_INSTRUMENTATION_OPENTELEMETRY_INSTRUMENTATION_ANNOTATIONS_ENABLED", "true");
        server.addEnvVar("OTEL_INSTRUMENTATION_OPENTELEMETRY_API_ENABLED", "true");

        server.startServer();

        String traceId = new HttpRequest(server, "/agentTest/withspan").run(String.class);
        Span span = client.waitForSpansForTraceId(traceId, hasSize(1)).get(0);

        // We should only have one span, it should be the span created by WithSpan
        // No span was created for the JAX-RS call
        assertThat(span, hasKind(INTERNAL));
        assertThat(span, hasNoParent());
    }

    /**
     * If the user configures the agent not to trace Rest or {@code @WithSpan} calls, we should not enable our instrumentation.
     */
    @Test
    public void testAgentRestAndCDIInstrumentationDisabled() throws Exception {
        server.addEnvVar("OTEL_INSTRUMENTATION_OPENTELEMETRY_INSTRUMENTATION_ANNOTATIONS_ENABLED", "false");
        server.addEnvVar("OTEL_INSTRUMENTATION_JAXRS_CLIENT_ENABLED", "false");
        server.addEnvVar("OTEL_INSTRUMENTATION_JAXRS_ENABLED", "false");
        server.addEnvVar("OTEL_INSTRUMENTATION_LIBERTY_ENABLED", "false");
        server.addEnvVar("OTEL_INSTRUMENTATION_APACHE_HTTPCLIENT_ENABLED", "false");
        server.addEnvVar("OTEL_INSTRUMENTATION_SERVLET_ENABLED", "false");

        server.startServer();

        String traceId = new HttpRequest(server, "/agentTest/allTraces").run(String.class);
        Span span = client.waitForSpansForTraceId(traceId, hasSize(1)).get(0);

        // We should only have one span, it should be the span created by WithSpan
        // No span was created for the JAX-RS call
        assertThat(span, hasKind(INTERNAL));
        assertThat(span, hasNoParent());
    }

    @Test
    public void testAgentDisabled() throws Exception {
        server.addEnvVar("OTEL_JAVAAGENT_ENABLED", "false");

        server.startServer();

        // With the agent disabled, we should have no span created for the jax-rs request
        String traceId = new HttpRequest(server, "/agentTest").run(String.class);

        List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(1));

        assertThat(spans.get(0), hasName("/agentTest/"));

        // We should still be able to manually create spans with the API
        String traceId2 = new HttpRequest(server, "/agentTest/manualSpans").run(String.class);

        List<Span> spans2 = client.waitForSpansForTraceId(traceId2, hasSize(3));

        Span requestSpan = TestUtils.findOneFrom(spans2, hasName("/agentTest/manualSpans"));
        assertThat(requestSpan, hasNoParent());

        Span span1 = TestUtils.findOneFrom(spans2, hasName("span1"));
        assertThat(span1, hasParentSpanId(requestSpan.getSpanId()));

        Span span2 = TestUtils.findOneFrom(spans2, hasName("span2"));
        assertThat(span2, hasParentSpanId(span1.getSpanId()));
    }

    @Test
    public void testAgentInstrumentationDisabled() throws Exception {
        server.addEnvVar("OTEL_INSTRUMENTATION_COMMON_DEFAULT_ENABLED", "false");
        server.addEnvVar("OTEL_INSTRUMENTATION_OPENTELEMETRY_API_ENABLED", "true");

        server.startServer();

        // With the all agent instrumentation disabled, we should have no span created for the jax-rs request
        String traceId = new HttpRequest(server, "/agentTest").run(String.class);
        assertThat(traceId, equalTo(NULL_TRACE_ID));

        // We should still be able to manually create spans with the API
        String traceId2 = new HttpRequest(server, "/agentTest/manualSpans").run(String.class);

        List<Span> spans2 = client.waitForSpansForTraceId(traceId2, hasSize(2));

        Span span1 = TestUtils.findOneFrom(spans2, hasName("span1"));
        assertThat(span1, hasNoParent());

        Span span2 = TestUtils.findOneFrom(spans2, hasName("span2"));
        assertThat(span2, hasParentSpanId(span1.getSpanId()));
    }

    @Test
    public void testAgentMultiApp() throws Exception {
        // Deploy two apps and associated server.xml
        server.removeAllInstalledAppsForValidation(); // Not starting the normal app

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

        copyToServer("server.xml-multi-app", "server.xml");

        server.startServer();

        // Test we can call app1
        String traceId = new HttpRequest(server, "/multiApp1").run(String.class);
        Span span1 = client.waitForSpansForTraceId(traceId, hasSize(1)).get(0);

        // microprofile-config.properties is not read by the agent
        assertThat(span1, hasServiceName("unknown_service:java"));
        assertThat(span1, hasNoParent());
        assertThat(span1, hasKind(SERVER));
        assertThat(span1, hasName("/multiApp1/"));

        // Test we can call app2
        String traceId2 = new HttpRequest(server, "/multiApp2").run(String.class);
        Span span2 = client.waitForSpansForTraceId(traceId2, hasSize(1)).get(0);

        // microprofile-config.properties is not read by the agent
        assertThat(span2, hasServiceName("unknown_service:java"));
        assertThat(span2, hasNoParent());
        assertThat(span2, hasKind(SERVER));
        assertThat(span2, hasName("/multiApp2/"));
    }

    private void copyToServer(String src, String dst) throws Exception {
        RemoteFile serverRoot = server.getFileFromLibertyServerRoot("");
        RemoteFile dstFile = new RemoteFile(serverRoot, dst);
        LocalFile srcFile = new LocalFile(server.pathToAutoFVTTestFiles + "/TelemetryAgentConfig/" + src);
        boolean result = dstFile.copyFromSource(srcFile);
        assertTrue("Failed to copy " + src + " to " + dst, result);
    }

    private void deleteFromServer(String filename) throws Exception {
        RemoteFile serverRoot = server.getFileFromLibertyServerRoot("");
        RemoteFile file = new RemoteFile(serverRoot, filename);
        boolean result = file.delete();
        assertTrue("Failed to delete " + filename, result);
    }
}
