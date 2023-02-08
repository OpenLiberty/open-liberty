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
package io.openliberty.microprofile.telemetry.internal_fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.apps.multiapp1.MultiApp1TestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.multiapp2.MultiApp2TargetResource;
import io.openliberty.microprofile.telemetry.internal_fat.apps.multiapp2.MultiApp2TestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.common.TestSpans;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporterProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;

/**
 * Test use of the multiple apps
 */
@RunWith(FATRunner.class)
public class TelemetryMultiAppTest extends FATServletClient {

    public static final String APP1_NAME = "multiapp1";
    public static final String APP2_NAME = "multiapp2";

    @TestServlets({
                    @TestServlet(contextRoot = APP1_NAME, servlet = MultiApp1TestServlet.class),
                    @TestServlet(contextRoot = APP2_NAME, servlet = MultiApp2TestServlet.class),
    })
    @Server("Telemetry10MultiApp")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // InMemorySpanExporter shared library
        PropertiesAsset exporterConfig = new PropertiesAsset()
                        .addProperty("otel.traces.exporter", "in-memory");
        JavaArchive exporterJar = ShrinkWrap.create(JavaArchive.class, "exporter.jar")
                        .addClasses(InMemorySpanExporter.class, InMemorySpanExporterProvider.class)
                        .addPackage(TestSpans.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(exporterConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportToServer(server, "shared", exporterJar, SERVER_ONLY);

        PropertiesAsset app1Config = new PropertiesAsset()
                        .addProperty("otel.service.name", "multiapp1");
        WebArchive multiapp1 = ShrinkWrap.create(WebArchive.class, APP1_NAME + ".war")
                        .addClass(MultiApp1TestServlet.class)
                        .addPackage(TestSpans.class.getPackage())
                        .addAsResource(app1Config, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, multiapp1, SERVER_ONLY);

        PropertiesAsset app2Config = new PropertiesAsset()
                        .addProperty("otel.service.name", "multiapp2");
        WebArchive multiapp2 = ShrinkWrap.create(WebArchive.class, APP2_NAME + ".war")
                        .addClass(MultiApp2TestServlet.class)
                        .addClass(MultiApp2TargetResource.class)
                        .addPackage(TestSpans.class.getPackage())
                        .addAsResource(app2Config, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, multiapp2, SERVER_ONLY);

        server.addEnvVar("OTEL_SDK_DISABLED", "false");
        server.addEnvVar("OTEL_BSP_SCHEDULE_DELAY", "100");
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

}
