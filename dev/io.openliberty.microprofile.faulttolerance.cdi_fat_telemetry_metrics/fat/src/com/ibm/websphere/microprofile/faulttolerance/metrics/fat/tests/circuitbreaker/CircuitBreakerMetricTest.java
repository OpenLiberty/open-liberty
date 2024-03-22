/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.circuitbreaker;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.util.InMemoryMetricExporter;
import com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.util.InMemoryMetricExporterProvider;
import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;

@RunWith(FATRunner.class)
public class CircuitBreakerMetricTest extends FATServletClient {

    private static final String APP_NAME = "ftCircuitBreakerMetrics";
    private static final String SERVER_NAME = "CDIFaultToleranceCircuitBreakerMetricsRemoval";

    @Server(SERVER_NAME)
    @TestServlet(contextRoot = APP_NAME, servlet = CircuitBreakerMetricServlet.class)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE10);

    @BeforeClass
    public static void setup() throws Exception {

        PropertiesAsset exporterConfig = new PropertiesAsset()
                        .addProperty("otel.metrics.exporter", "in-memory")
                        .addProperty("otel.metric.export.interval", "3000");

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackage(CircuitBreakerMetricServlet.class.getPackage())
                        .addPackage(InMemoryMetricExporter.class.getPackage())
                        .addAsResource(exporterConfig, "META-INF/microprofile-config.properties")
                        .addAsServiceProvider(ConfigurableMetricExporterProvider.class, InMemoryMetricExporterProvider.class)
                        .addAsManifestResource(CircuitBreakerMetricTest.class.getResource("permissions.xml"), "permissions.xml");

        ShrinkHelper.exportAppToServer(server, war, DeployOptions.SERVER_ONLY);

        server.addEnvVar("OTEL_SDK_DISABLED", "false");
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
        server.deleteFileFromLibertyServerRoot("dropins/" + APP_NAME + ".war");
    }

}
