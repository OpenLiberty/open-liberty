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
 * This test ensures that the metrics exported to OpenTelemetry do not leak between two apps,
 * and do not leak between the same application after it has been restarted.
 */
@RunWith(FATRunner.class)
public class TelemetryMetricIsolationTest {

    private static final String SERVER_NAME = "CDIFaultToleranceMetricsIsolation";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME, MicroProfileActions.MP71_EE10, MicroProfileActions.MP71_EE11, MicroProfileActions.MP70_EE10, MicroProfileActions.MP70_EE11);

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive removalTestOne = ShrinkWrap.create(WebArchive.class, "IsolationTestAppOne.war")
                        .addPackage(IsolationServlet.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false"),
                                       "META-INF/microprofile-config.properties")
                        .addAsServiceProvider(AutoConfigurationCustomizerProvider.class, PullExporterAutoConfigurationCustomizerProvider.class);

        WebArchive removalTestTwo = ShrinkWrap.create(WebArchive.class, "IsolationTestAppTwo.war")
                        .addPackage(IsolationServlet.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false"),
                                       "META-INF/microprofile-config.properties")
                        .addAsServiceProvider(AutoConfigurationCustomizerProvider.class, PullExporterAutoConfigurationCustomizerProvider.class);

        ShrinkHelper.exportAppToServer(server, removalTestOne, SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, removalTestTwo, SERVER_ONLY);

        server.startServer();
    }

    @Test
    public void metricIsolationTest() throws Exception {
        //Test one ensures we don't have any metrics exported from FT into Telemetry before calling a method with a FT annotation
        //Test two ensures that we do have metrics after the method
        HttpUtils.findStringInUrl(server, "IsolationTestAppOne/isolationtest", "Test one passed Test two passed");
        HttpUtils.findStringInUrl(server, "IsolationTestAppTwo/isolationtest", "Test one passed Test two passed");

        server.getApplicationMBean("IsolationTestAppOne").restart();
        HttpUtils.findStringInUrl(server, "IsolationTestAppOne/isolationtest", "Test one passed Test two passed");

        //To verify the test, we'll also check that metrics will persist if we don't restart
        HttpUtils.findStringInUrl(server, "IsolationTestAppOne/isolationtest", "There were metrics before calling doWorkWithRetry Test two passed");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
