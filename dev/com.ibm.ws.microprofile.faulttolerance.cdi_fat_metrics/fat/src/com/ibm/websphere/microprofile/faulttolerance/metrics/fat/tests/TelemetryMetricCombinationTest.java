/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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
package com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.isolation.IsolationServlet;
import com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.isolation.PullExporterAutoConfigurationCustomizerProvider;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;

/*
 * This test ensures that the FaultTolerence can export to OpenTelemetry and Microprofile Metrics at the same time without issues.
 */
@RunWith(FATRunner.class)
public class TelemetryMetricCombinationTest {

    private static final String SERVER_NAME = "CDIFaultToleranceMetricsAndTelemetry";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME, MicroProfileActions.MP71_EE10, MicroProfileActions.MP71_EE11, MicroProfileActions.MP70_EE10, MicroProfileActions.MP70_EE11);

    @BeforeClass
    public static void setup() throws Exception {
        //We may as well reuse a pre-existing app since we're only testing a single metric, any metric
        WebArchive combinationTestApp = ShrinkWrap.create(WebArchive.class, "CombinationTestAppOne.war")
                        .addPackage(IsolationServlet.class.getPackage())
                        .addAsManifestResource(MetricRemovalTest.class.getResource("removal/permissions.xml"), "permissions.xml")
                        .addAsResource(new StringAsset("otel.sdk.disabled=false"),
                                       "META-INF/microprofile-config.properties")
                        .addAsServiceProvider(AutoConfigurationCustomizerProvider.class, PullExporterAutoConfigurationCustomizerProvider.class);

        ShrinkHelper.exportAppToServer(server, combinationTestApp, SERVER_ONLY);

        server.startServer();
    }

    @Test
    public void metricTelemetryCombinationTest() throws Exception {
        //This line does a basic test of FT exporting to Open Telemetry, test one passes if there are no FT metrics before calling
        //any methods with FT annotations. Test two checks we get some metrics after calling a method with FT annotations.
        HttpUtils.findStringInUrl(server, "CombinationTestAppOne/isolationtest", "Test one passed Test two passed");

        //This line tests that FT also exported to MPMetrics. Because we've called the method in the first test we just check the results.
        assertThat(getMetricsPage(),
                   containsString("ft_retry_calls_total{method=\"com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.isolation.IsolationBean.doWorkWithRetry\",mp_scope=\"base\",retried=\"false\",retryResult=\"valueReturned\",} 1.0"));
    }

    /**
     * Retrieve the list of registered metrics from the /metrics endpoint
     */
    private String getMetricsPage() throws IOException {
        HttpURLConnection con = HttpUtils.getHttpConnection(server, "metrics");
        BufferedReader reader = HttpUtils.getResponseBody(con, "UTF-8");

        StringBuilder b = new StringBuilder();
        char[] cbuf = new char[1024];
        int charCount;

        while ((charCount = reader.read(cbuf)) > 0) {
            b.append(cbuf, 0, charCount);
        }

        return b.toString();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
