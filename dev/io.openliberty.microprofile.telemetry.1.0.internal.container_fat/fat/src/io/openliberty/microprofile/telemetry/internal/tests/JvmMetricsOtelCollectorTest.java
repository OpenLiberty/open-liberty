/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
import static org.junit.Assert.assertEquals;

import java.io.File;

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

import com.ibm.websphere.simplicity.OperatingSystem;

import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.microprofile.telemetry.internal.apps.spanTest.TestResource;
import io.openliberty.microprofile.telemetry.internal.utils.TestConstants;
import io.openliberty.microprofile.telemetry.internal.utils.otelCollector.OtelCollectorContainer;
import io.openliberty.microprofile.telemetry.internal.utils.otelCollector.OtelCollectorQueryClient;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

/**
 * Test exporting metrics to a OpenTelemetry Collector
 */
@RunWith(FATRunner.class)
public class JvmMetricsOtelCollectorTest {

    private static final String SERVER_NAME = "spanTestServer";

    public static Network network = Network.newNetwork();
    public static OtelCollectorContainer otelCollectorContainer = new OtelCollectorContainer(new File("lib/LibertyFATTestFiles/otel-collector-config-metrics.yaml"), 3131)
                                                                                                                                                                   .withNetwork(network)
                                                                                                                                                                   .withNetworkAliases("otel-collector-metrics")
                                                                                                                                                                   .withLogConsumer(new SimpleLogConsumer(JvmMetricsOtelCollectorTest.class,
                                                                                                                                                                                                          "otelCol"));
    public static RepeatTests repeat = TelemetryActions.telemetry20Repeats(SERVER_NAME);

    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(network)
                                             .around(otelCollectorContainer)
                                             .around(repeat);

    public OtelCollectorQueryClient client = new OtelCollectorQueryClient(otelCollectorContainer);

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        server.addEnvVar(TestConstants.ENV_OTEL_TRACES_EXPORTER, "none");
        server.addEnvVar(TestConstants.ENV_OTEL_LOGS_EXPORTER, "none");
        server.addEnvVar(TestConstants.ENV_OTEL_METRIC_EXPORT_INTERVAL, "1000");
        server.addEnvVar(TestConstants.ENV_OTEL_METRICS_EXPORTER, "otlp");
        server.addEnvVar(TestConstants.ENV_OTEL_EXPORTER_OTLP_ENDPOINT, otelCollectorContainer.getOtlpGrpcUrl()); //Send metrics to OpenTelemetry collector
        server.addEnvVar(TestConstants.ENV_OTEL_SERVICE_NAME, "PrometheusOtelCollectorTest");
        server.addEnvVar(TestConstants.ENV_OTEL_SDK_DISABLED, "false");

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
    public void testBasicTelemetry2() throws Exception {

        HttpRequest request = new HttpRequest(server, "/spanTest/waitForGarbageCollection");
        @SuppressWarnings("unused")
        String notUsed = request.run(String.class);

        //JVM Metrics are not found on AIX. Upstream issue: https://bugs.openjdk.org/browse/JDK-8030957
        OperatingSystem os = server.getMachine().getOperatingSystem();
        if(os == OperatingSystem.AIX){
            return;
        }

        Thread.sleep(10000);
        assertEquals("pass", client.getJVMMetrics());
    }

}
