/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.beansxml.BeansAsset;
import com.ibm.websphere.simplicity.beansxml.BeansAsset.DiscoveryMode;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.apps.disabledhttptracing.HttpTraceDisabledServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.servlet.HttpTraceTestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.servlet.SimpleServlet;
import io.openliberty.microprofile.telemetry.internal_fat.common.TestSpans;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporterProvider;
import io.openliberty.microprofile.telemetry.internal_fat.shared.spans.AbstractSpanMatcher;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;

/**
 * HTTP request tracing tests
 */
@RunWith(FATRunner.class)
public class TelemetryServletTest extends FATServletClient {

    public static final String APP_NAME = "TelemetryServletTestApp";
    public static final String TRACE_APP_NAME = "HttpTraceDisabledServletTestApp";
    public static final String SERVER_NAME = "Telemetry10Servlet";

    @TestServlets({
                    @TestServlet(contextRoot = APP_NAME, servlet = HttpTraceTestServlet.class),
                    @TestServlet(contextRoot = TRACE_APP_NAME, servlet = HttpTraceDisabledServlet.class),
    })

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.allMPRepeats(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty("otel.sdk.disabled", "false")
                        .addProperty("otel.traces.exporter", "in-memory")
                        .addProperty("otel.bsp.schedule.delay", "100");
        BeansAsset beans = BeansAsset.getBeansAsset(DiscoveryMode.ALL);
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addClass(HttpTraceTestServlet.class)
                        .addPackage(SimpleServlet.class.getPackage())
                        .addPackage(InMemorySpanExporter.class.getPackage())
                        .addPackage(TestSpans.class.getPackage())
                        .addPackage(AbstractSpanMatcher.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsWebResource(new File("test-applications/TelemetryServletTestApp.war/hello.html"))
                        .addAsWebResource(new File("test-applications/TelemetryServletTestApp.war/dice.jsp"))
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties")
                        .addAsResource(beans, "META-INF/beans.xml");

        ShrinkHelper.exportAppToServer(server, app, SERVER_ONLY);

        // Testing that tracing is enabled but http tracing is disabled
        PropertiesAsset httpTraceConfig = new PropertiesAsset()
                        .addProperty("otel.sdk.disabled", "false")
                        // By default otel.trace.http.disabled is false
                        .addProperty("otel.trace.http.disabled", "true")
                        .addProperty("otel.traces.exporter", "in-memory")
                        .addProperty("otel.bsp.schedule.delay", "100");
        WebArchive appForTrace = ShrinkWrap.create(WebArchive.class, TRACE_APP_NAME + ".war")
                        .addClass(SimpleServlet.class)
                        .addPackage(HttpTraceDisabledServlet.class.getPackage())
                        .addPackage(InMemorySpanExporter.class.getPackage())
                        .addPackage(TestSpans.class.getPackage())
                        .addPackage(AbstractSpanMatcher.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsWebResource(new File("test-applications/HttpTraceDisabledServletTestApp.war/dice.jsp"))
                        .addAsResource(httpTraceConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, appForTrace, SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}