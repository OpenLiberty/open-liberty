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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
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
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.microprofile.telemetry.internal.apps.metricTest.TestResource;
import io.openliberty.microprofile.telemetry.internal.utils.TestConstants;
import io.openliberty.microprofile.telemetry.internal.utils.otelCollector.OtelCollectorContainer;
import io.openliberty.microprofile.telemetry.internal.utils.otelCollector.OtelCollectorQueryClient;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

/**
 * Test exporting metrics to a OpenTelemetry Collector
 */
@RunWith(FATRunner.class)
public class MetricsApiOtelCollectorTest {

    private static final String SERVER_NAME = "spanTestServer";

    private static final Class<?> c = MetricsApiOtelCollectorTest.class;

    public static Network network = Network.newNetwork();
    public static OtelCollectorContainer otelCollectorContainer = new OtelCollectorContainer(new File("lib/LibertyFATTestFiles/otel-collector-config-metrics.yaml"), 3131)
                                                                                                                                                                          .withNetwork(network)
                                                                                                                                                                          .withNetworkAliases("otel-collector-metrics")
                                                                                                                                                                          .withLogConsumer(new SimpleLogConsumer(MetricsApiOtelCollectorTest.class,
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
    public void testLongUpDownCounter() throws Exception {
        HttpRequest request = new HttpRequest(server, "/spanTest/longUpDownCounterCreated");
        String response = request.run(String.class);

        Log.info(c, "testLongUpDownCounter", "response: " + response);
        Thread.sleep(3000);
        getApiMetrics("testLongUpDownCounter", "gauge", "-20");
    }

    @Test
    public void testCounter() throws Exception {
        HttpRequest request = new HttpRequest(server, "/spanTest/longCounterCreated");
        String response = request.run(String.class);
        Log.info(c, "testCounter", "response: " + response);
        Thread.sleep(3000);
        getApiMetrics("testLongCounter", "counter", "20");
    }

    @Test
    public void testLongHistogram() throws Exception {
        HttpRequest request = new HttpRequest(server, "/spanTest/longHistogramCreated");
        String response = request.run(String.class);
        Thread.sleep(3000);
        Log.info(c, "testLongHistogram", "response: " + response);
        getApiMetrics("testLongHistogram", "histogram", "20");
    }

    @Test
    public void testDoubleCounter() throws Exception {
        HttpRequest request = new HttpRequest(server, "/spanTest/doubleCounterCreated");
        String response = request.run(String.class);
        Thread.sleep(3000);
        Log.info(c, "testDoubleCounter", "response: " + response);
        getApiMetrics("testDoubleCounter", "counter", "40");
    }

    @Test
    public void testDoubleUpDownCounter() throws Exception {
        HttpRequest request = new HttpRequest(server, "/spanTest/doubleUpDownCounterCreated");
        String response = request.run(String.class);
        Thread.sleep(3000);
        Log.info(c, "testDoubleUpDownCounter", "response: " + response);
        getApiMetrics("testDoubleUpDownCounter", "counter", "-40");
    }
    /**
     * Gets metrics from otelcollector:3131/metrics in the prometheus format.
     * For more info on the Prometheus metrics format: 
     * https://github.com/prometheus/docs/blob/main/content/docs/instrumenting/exposition_formats.md#text-format-details
     */
    
    public void getApiMetrics(String name, String type, String value) throws Exception {
        String result = client.dumpMetrics();
        
        // Add null check for result before using it
        if (result == null) {
            fail("No metrics data returned from collector");
            return;
        }
        
        List<String> splits = Arrays.asList(result.split("((?=# HELP))"));
        for (String s : splits) {
            if (s.contains(name)) {
                System.out.println(s);
                assertTrue(s.contains(type));
                assertTrue(s.contains(value));
                return;
            }
        }
        fail(name + " not found in OpenTelemetry Collector output");
    }
}