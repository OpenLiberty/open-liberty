/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
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
import static io.openliberty.microprofile.telemetry.internal.utils.TestUtils.findOneFrom;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerSpanMatcher.hasKind;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerSpanMatcher.hasName;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerSpanMatcher.hasNoParent;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerSpanMatcher.hasParentSpanId;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerSpanMatcher.hasServiceName;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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

import com.ibm.websphere.simplicity.LocalFile;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

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
public class AgentConfigTest {

    private static final String SERVER_NAME = "TelemetryAgentConfig";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    private static KeyPairs keyPairs = new KeyPairs(server);

    public static JaegerContainer jaegerContainer = new JaegerContainer(keyPairs.getCertificate(),
                                                                        keyPairs.getKey()).withLogConsumer(new SimpleLogConsumer(AgentConfigTest.class, "jaeger"));
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

        // Construct the test application
        WebArchive agentTest = ShrinkWrap.create(WebArchive.class, "agentTest.war")
                                         .addPackage(AgentConfigTestResource.class.getPackage());
        ShrinkHelper.exportAppToServer(server, agentTest, SERVER_ONLY);
    }

    @Before
    public void resetServer() throws Exception {
        // Replace any test files with their original versions
        deleteFromServer("agent-config.properties");
        deleteFromServer("jvm.options");
        deleteFromServer("bootstrap.properties");
        copyToServer("jvm.options-original", "jvm.options");
        copyToServer("bootstrap.properties-original", "bootstrap.properties");

        // Reset the expected apps
        server.removeAllInstalledAppsForValidation();
        server.addInstalledAppForValidation("agentTest");

        // Env vars are cleared when the server starts, so we need to set the core ones up again
        server.addEnvVar(TestConstants.ENV_OTEL_TRACES_EXPORTER, "otlp");
        server.addEnvVar(TestConstants.ENV_OTEL_EXPORTER_OTLP_ENDPOINT, jaegerContainer.getOtlpGrpcUrl());
        //The default OTLP protocol has been changed from grpc to http/protobuf in the Java Agent v2.5.0
        server.addEnvVar(TestConstants.ENV_OTEL_EXPORTER_OTLP_PROTOCOL, "grpc");
        server.addEnvVar("OTEL_METRICS_EXPORTER", "none");
        server.addEnvVar("OTEL_LOGS_EXPORTER", "none");

        //Required for Java Agent 2.0.0+ to create Jax-Rs spans
        server.addEnvVar("OTEL_INSTRUMENTATION_COMMON_EXPERIMENTAL_CONTROLLER_TELEMETRY_ENABLED", "true"); //otel.instrumentation.common.experimental.controller-telemetry.enabled=true)

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
    public void testConfigFromAgentFile() throws Exception {
        copyToServer("agent-config.properties", "agent-config.properties");
        server.addEnvVar("OTEL_JAVAAGENT_CONFIGURATION_FILE", "agent-config.properties");

        server.startServer();

        String traceId = new HttpRequest(server, "/agentTest").run(String.class);

        Span span = null;

        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            // MP6.0 uses JavaAgent 1.19 therefore there is no extra span created for JAX-RS
            span = client.waitForSpansForTraceId(traceId, hasSize(1)).get(0);
        } else {
            // All other repeats are using Java Agent 1.29 therefore there is an extra span for JAX-RS.
            List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(2));
            span = findOneFrom(spans, hasNoParent());
        }
        assertThat(span, hasServiceName("agent-config-test-service"));
    }

    @Test
    public void testConfigFromJvmOptions() throws Exception {
        deleteFromServer("jvm.options");
        copyToServer("jvm.options-test", "jvm.options");

        server.startServer();

        String traceId = new HttpRequest(server, "/agentTest").run(String.class);
        Span span = null;

        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            // MP6.0 uses JavaAgent 1.19 therefore there is no extra span created for JAX-RS
            span = client.waitForSpansForTraceId(traceId, hasSize(1)).get(0);
        } else {
            // All other repeats are using Java Agent 1.29 therefore there is an extra span for JAX-RS.
            List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(2));
            span = findOneFrom(spans, hasNoParent());
        }

        assertThat(span, hasServiceName("jvm-options-test-service"));
    }

    @Test
    public void testConfigNotReadFromBootstrap() throws Exception {
        deleteFromServer("bootstrap.properties");
        copyToServer("bootstrap.properties-test", "bootstrap.properties");

        server.startServer();

        String traceId = new HttpRequest(server, "/agentTest").run(String.class);
        Span span = null;

        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            // MP6.0 uses JavaAgent 1.19 therefore there is no extra span created for JAX-RS
            span = client.waitForSpansForTraceId(traceId, hasSize(1)).get(0);
        } else {
            // All other repeats are using Java Agent 1.29 therefore there is an extra span for JAX-RS.
            List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(2));
            span = findOneFrom(spans, hasNoParent());
        }

        // bootstrap.properties is not read by the agent
        // It's processed as one of the first things liberty does on startup,
        // but that's still too late for the agent to see its changes
        assertThat(span, not(hasServiceName("multi-app-1")));
    }

    @Test
    public void testDisableInstrumentation() throws Exception {
        // Disable @WithSpan instrumentation
        server.addEnvVar("OTEL_INSTRUMENTATION_OPENTELEMETRY_INSTRUMENTATION_ANNOTATIONS_ENABLED", "false");

        server.startServer();

        String traceId = new HttpRequest(server, "/agentTest/withspan").run(String.class);
        Span span = null;

        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            // MP6.0 uses JavaAgent 1.19 therefore there is no extra span created for JAX-RS
            span = client.waitForSpansForTraceId(traceId, hasSize(1)).get(0);
        } else {
            // All other repeats are using Java Agent 1.29 therefore there is an extra span for JAX-RS.
            List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(2));
            span = findOneFrom(spans, hasNoParent());
        }

        // We should only have one span, it should be the server span
        // No span was created for the WithSpan annotation
        assertThat(span, hasKind(SERVER));
    }

    @Test
    /*
     * Skipping for 1.4 and 4.1 as JavaAgent 1.29 currently will not return a span for methods annotated with @withSpan
     * (https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/10159)
     */
    @SkipForRepeat({ TelemetryActions.MP14_MPTEL11_ID, TelemetryActions.MP41_MPTEL11_ID, TelemetryActions.MP14_MPTEL20_ID, TelemetryActions.MP41_MPTEL20_ID })
    public void testEnableSpecificInstrumentation() throws Exception {
        // Enable only @WithSpan instrumentation
        server.addEnvVar("OTEL_INSTRUMENTATION_COMMON_DEFAULT_ENABLED", "false");
        server.addEnvVar("OTEL_INSTRUMENTATION_OPENTELEMETRY_INSTRUMENTATION_ANNOTATIONS_ENABLED", "true");
        server.addEnvVar("OTEL_INSTRUMENTATION_OPENTELEMETRY_API_ENABLED", "true");

        server.startServer();

        String traceId = new HttpRequest(server, "/agentTest/withspan").run(String.class);;

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
        server.addEnvVar("OTEL_INSTRUMENTATION_HTTP_URL_CONNECTION_ENABLED", "false"); //For JaxRs 2.0 and 2.1
        server.addEnvVar("OTEL_INSTRUMENTATION_SERVLET_ENABLED", "false");
        server.addEnvVar("OTEL_INSTRUMENTATION_NETTY_ENABLED", "false");


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

        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            assertThat(spans.get(0), hasName("/agentTest/"));
        } else {
            assertThat(spans.get(0), hasName("GET /agentTest/"));
        }
        // We should still be able to manually create spans with the API
        String traceId2 = new HttpRequest(server, "/agentTest/manualSpans").run(String.class);

        List<Span> spans2 = client.waitForSpansForTraceId(traceId2, hasSize(3));

        String requestSpanName = "";
        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            requestSpanName = "/agentTest/manualSpans";
        } else {
            requestSpanName = "GET /agentTest/manualSpans";
        }

        Span requestSpan = TestUtils.findOneFrom(spans2, hasName(requestSpanName));
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

    private void copyToServer(String src, String dst) throws Exception {
        RemoteFile serverRoot = server.getFileFromLibertyServerRoot("");
        RemoteFile dstFile = server.getMachine().getFile(serverRoot, dst);
        LocalFile srcFile = new LocalFile(server.pathToAutoFVTTestFiles + "/TelemetryAgentConfig/" + src);
        boolean result = dstFile.copyFromSource(srcFile);
        assertTrue("Failed to copy " + src + " to " + dst, result);
    }

    private void deleteFromServer(String filename) throws Exception {
        RemoteFile serverRoot = server.getFileFromLibertyServerRoot("");
        RemoteFile file = server.getMachine().getFile(serverRoot, filename);
        boolean result = file.delete();
        assertTrue("Failed to delete " + filename, result);
    }
}
