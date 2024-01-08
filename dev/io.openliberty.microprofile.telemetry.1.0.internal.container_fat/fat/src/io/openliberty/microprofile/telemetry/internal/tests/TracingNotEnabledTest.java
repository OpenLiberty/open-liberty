/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

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
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;
import io.jaegertracing.api_v2.Model.Span;
import io.openliberty.microprofile.telemetry.internal.apps.spanTest.TestResource;
import io.openliberty.microprofile.telemetry.internal.suite.FATSuite;
import io.openliberty.microprofile.telemetry.internal.utils.TestConstants;
import io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerContainer;
import io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerQueryClient;

/**
 * Test logging to Jaeger without tracing enabled
 * <p>
 * This is a full mode test because it requires a longer wait to assert a lack of functionality
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class TracingNotEnabledTest {

    private static final String SERVER_NAME = "spanTestServer";
    private static final String SERVICE_NAME = "Test service";

    private static final Class<TracingNotEnabledTest> c = TracingNotEnabledTest.class;

    public static JaegerContainer jaegerContainer = new JaegerContainer().withLogConsumer(new SimpleLogConsumer(TracingNotEnabledTest.class, "jaeger"));
    public static RepeatTests repeat = FATSuite.allMPRepeats(SERVER_NAME);

    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(jaegerContainer).around(repeat);

    public static JaegerQueryClient client;

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        client = new JaegerQueryClient(jaegerContainer);

        server.addEnvVar(TestConstants.ENV_OTEL_TRACES_EXPORTER, "otlp");
        server.addEnvVar(TestConstants.ENV_OTEL_EXPORTER_OTLP_ENDPOINT, jaegerContainer.getOtlpGrpcUrl());

        server.addEnvVar(TestConstants.ENV_OTEL_SERVICE_NAME, SERVICE_NAME);
        server.addEnvVar(TestConstants.ENV_OTEL_BSP_SCHEDULE_DELAY, "100"); // Wait no more than 100ms to send traces to the server
        // server.addEnvVar(TestConstants.ENV_OTEL_SDK_DISABLED, "false"); // Do not enable tracing

        // Construct the test application
        WebArchive jaegerTest = ShrinkWrap.create(WebArchive.class, "spanTest.war")
                                          .addPackage(TestResource.class.getPackage());
        ShrinkHelper.exportAppToServer(server, jaegerTest, SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @AfterClass
    public static void closeClient() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    @Test
    public void testTracingNotEnabled() throws Exception {
        HttpRequest request = new HttpRequest(server, "/spanTest");
        String traceId = request.run(String.class);

        Log.info(c, "testBasic", "TraceId is " + traceId);

        // We still expect a blank traceId when tracing is disabled
        assertThat(traceId, equalTo(NULL_TRACE_ID));

        // Wait 12 seconds to give a chance for any traces to arrive
        // With BSP_SCHEDULE_DELAY, traces should be sent after 100ms,
        // however the default is to send only every 10 seconds.
        Thread.sleep(12_000);

        // With tracing disabled, we expect a traceId of 00000000000000000000000000000000, which Jaeger reports is an invalid ID.
        // Instead, we check for all traces for the service. As we start a clean Jaeger instance for this test, there should be none.
        List<Span> spans = client.getSpansForServiceName(SERVICE_NAME);
        assertThat(spans, is(empty()));
    }

}
