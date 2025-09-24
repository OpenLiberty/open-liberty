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
package io.openliberty.microprofile.telemetry.internal_fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.RuntimeInstanceServlet;
import io.openliberty.microprofile.telemetry.internal_fat.common.TestSpans;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporterProvider;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;
import io.openliberty.microprofile.telemetry.internal_fat.shared.spans.AbstractSpanMatcher;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;

@RunWith(FATRunner.class)
public class TelemetryRuntimeInstanceTest extends FATServletClient {

    public static final String SERVER_NAME = "Telemetry10ResourceAttributes";
    public static final String APP_NAME = "TelemetryResourcesApp";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = RuntimeInstanceServlet.class, contextRoot = APP_NAME),
    })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = TelemetryActions.telemetry21andLatest20Repeats(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsResource(new StringAsset("otel.traces.exporter=in-memory\notel.bsp.schedule.delay=100"),
                                       "META-INF/microprofile-config.properties")
                        .addClasses(InMemorySpanExporter.class, InMemorySpanExporterProvider.class, RuntimeInstanceServlet.class)
                        .addPackage(TestSpans.class.getPackage())
                        .addPackage(AbstractSpanMatcher.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class);

        ShrinkHelper.exportAppToServer(server, app, SERVER_ONLY);
        server.addEnvVar("OTEL_SDK_DISABLED", "false");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}