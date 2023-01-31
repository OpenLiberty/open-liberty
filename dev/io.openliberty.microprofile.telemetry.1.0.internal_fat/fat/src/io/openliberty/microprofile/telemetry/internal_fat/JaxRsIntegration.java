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
package io.openliberty.microprofile.telemetry.internal_fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertEquals;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.JaxRsEndpoints;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.common.PropagationHeaderEndpoint;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.transports.B3MultiPropagationTestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.transports.B3PropagationTestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.transports.JaegerPropagationTestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.transports.W3CTraceBaggagePropagationTestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.transports.W3CTracePropagationTestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporterProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;

@RunWith(FATRunner.class)
public class JaxRsIntegration extends FATServletClient {

    public static final String SERVER_NAME = "Telemetry10Jax";
    public static final String APP_NAME = "JaxPropagation";
    public static final String W3C_TRACE_APP_NAME = "w3cTrace";
    public static final String W3C_TRACE_BAGGAGE_APP_NAME = "w3cTraceBaggage";
    public static final String B3_APP_NAME = "b3";
    public static final String B3_MULTI_APP_NAME = "b3multi";
    public static final String JAEGER_APP_NAME = "jaeger";

    @TestServlets({
                    @TestServlet(contextRoot = W3C_TRACE_APP_NAME, servlet = W3CTracePropagationTestServlet.class),
                    @TestServlet(contextRoot = W3C_TRACE_BAGGAGE_APP_NAME, servlet = W3CTraceBaggagePropagationTestServlet.class),
                    @TestServlet(contextRoot = B3_APP_NAME, servlet = B3PropagationTestServlet.class),
                    @TestServlet(contextRoot = B3_MULTI_APP_NAME, servlet = B3MultiPropagationTestServlet.class),
                    @TestServlet(contextRoot = JAEGER_APP_NAME, servlet = JaegerPropagationTestServlet.class),
    })
    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty("otel.sdk.disabled", "false")
                        .addProperty("otel.traces.exporter", "in-memory")
                        .addProperty("otel.bsp.schedule.delay", "100");
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackage(JaxRsEndpoints.class.getPackage())
                        .addPackage(InMemorySpanExporter.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        PropertiesAsset w3cTraceAppConfig = new PropertiesAsset()
                        .include(appConfig)
                        .addProperty("otel.propagators", "tracecontext");
        WebArchive w3cTraceApp = ShrinkWrap.create(WebArchive.class, W3C_TRACE_APP_NAME + ".war")
                        .addClass(W3CTracePropagationTestServlet.class)
                        .addPackage(InMemorySpanExporter.class.getPackage())
                        .addPackage(PropagationHeaderEndpoint.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(w3cTraceAppConfig, "META-INF/microprofile-config.properties");

        PropertiesAsset w3cTraceBaggageAppConfig = new PropertiesAsset()
                        .include(appConfig)
                        .addProperty("otel.propagators", "tracecontext, baggage");
        WebArchive w3cTraceBaggageApp = ShrinkWrap.create(WebArchive.class, W3C_TRACE_BAGGAGE_APP_NAME + ".war")
                        .addClass(W3CTraceBaggagePropagationTestServlet.class)
                        .addPackage(InMemorySpanExporter.class.getPackage())
                        .addPackage(PropagationHeaderEndpoint.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(w3cTraceBaggageAppConfig, "META-INF/microprofile-config.properties");

        PropertiesAsset b3AppConfig = new PropertiesAsset()
                        .include(appConfig)
                        .addProperty("otel.propagators", "b3");
        WebArchive b3App = ShrinkWrap.create(WebArchive.class, B3_APP_NAME + ".war")
                        .addClass(B3PropagationTestServlet.class)
                        .addPackage(InMemorySpanExporter.class.getPackage())
                        .addPackage(PropagationHeaderEndpoint.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(b3AppConfig, "META-INF/microprofile-config.properties");

        PropertiesAsset b3MultiAppConfig = new PropertiesAsset()
                        .include(appConfig)
                        .addProperty("otel.propagators", "b3multi");
        WebArchive b3MultiApp = ShrinkWrap.create(WebArchive.class, B3_MULTI_APP_NAME + ".war")
                        .addClass(B3MultiPropagationTestServlet.class)
                        .addPackage(InMemorySpanExporter.class.getPackage())
                        .addPackage(PropagationHeaderEndpoint.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(b3MultiAppConfig, "META-INF/microprofile-config.properties");

        PropertiesAsset jaegerAppConfig = new PropertiesAsset()
                        .include(appConfig)
                        .addProperty("otel.propagators", "jaeger");
        WebArchive jaegerApp = ShrinkWrap.create(WebArchive.class, JAEGER_APP_NAME + ".war")
                        .addClass(JaegerPropagationTestServlet.class)
                        .addPackage(InMemorySpanExporter.class.getPackage())
                        .addPackage(PropagationHeaderEndpoint.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(jaegerAppConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, app, SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, w3cTraceApp, SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, w3cTraceBaggageApp, SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, b3App, SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, b3MultiApp, SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, jaegerApp, SERVER_ONLY);

        server.startServer();
    }

    @Test
    public void testIntegrationWithJaxRsClient() throws Exception {
        HttpRequest pokeJax = new HttpRequest(server, "/" + APP_NAME + "/endpoints/jaxrsclient");
        assertEquals("Test Passed", pokeJax.run(String.class));

        Thread.sleep(1000);

        HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspans");
        assertEquals("Test Passed", readspans.run(String.class));
    }

    @Test
    public void testIntegrationWithJaxRsClientAsync() throws Exception {
        HttpRequest pokeJax = new HttpRequest(server, "/" + APP_NAME + "/endpoints/jaxrsclientasync");
        assertEquals("Test Passed", pokeJax.run(String.class));

        Thread.sleep(1000);

        HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspans");
        assertEquals("Test Passed", readspans.run(String.class));
    }

    @Test
    public void testIntegrationWithMpClient() throws Exception {
        HttpRequest pokeMp = new HttpRequest(server, "/" + APP_NAME + "/endpoints/jaxrsclient");
        assertEquals("Test Passed", pokeMp.run(String.class));

        Thread.sleep(1000);

        HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspans");
        assertEquals("Test Passed", readspans.run(String.class));
    }

    @Test
    public void testIntegrationWithMpClientAsync() throws Exception {
        HttpRequest pokeMp = new HttpRequest(server, "/" + APP_NAME + "/endpoints/mpclientasync");
        assertEquals("Test Passed", pokeMp.run(String.class));

        Thread.sleep(1000);

        HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspans");
        assertEquals("Test Passed", readspans.run(String.class));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
