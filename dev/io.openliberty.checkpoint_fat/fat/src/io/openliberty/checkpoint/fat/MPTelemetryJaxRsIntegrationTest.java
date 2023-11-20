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
package io.openliberty.checkpoint.fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static io.openliberty.checkpoint.fat.FATSuite.getTestMethod;
import static io.openliberty.checkpoint.fat.FATSuite.configureEnvVariable;
import static jaxrspropagation.JaxRsEndpoints.TEST_PASSED;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureSet;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import jaxrspropagation.JaxRsEndpoints;
import jaxrspropagation.common.PropagationHeaderEndpoint;
import jaxrspropagation.methods.JaxRsMethodTestEndpoints;
import jaxrspropagation.methods.JaxRsMethodTestServlet;
import jaxrspropagation.responses.JaxRsResponseCodeTestEndpoints;
import jaxrspropagation.responses.JaxRsResponseCodeTestServlet;
import jaxrspropagation.transports.B3MultiPropagationTestServlet;
import jaxrspropagation.transports.B3PropagationTestServlet;
import jaxrspropagation.transports.JaegerPropagationTestServlet;
import jaxrspropagation.transports.W3CTraceBaggagePropagationTestServlet;
import jaxrspropagation.transports.W3CTracePropagationTestServlet;
import jaxrspropagation.spanexporter.TestSpans;
import jaxrspropagation.spanexporter.InMemorySpanExporter;
import jaxrspropagation.spanexporter.InMemorySpanExporterProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.openliberty.checkpoint.fat.MPTelemetryTest.TestMethod;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class MPTelemetryJaxRsIntegrationTest extends FATServletClient {

    public static final String SERVER_NAME = "Telemetry10Jax";
    public static final String APP_NAME = "JaxPropagation";
    public static final String W3C_TRACE_APP_NAME = "w3cTrace";
    public static final String W3C_TRACE_BAGGAGE_APP_NAME = "w3cTraceBaggage";
    public static final String B3_APP_NAME = "b3";
    public static final String B3_MULTI_APP_NAME = "b3multi";
    public static final String JAEGER_APP_NAME = "jaeger";
    public static final String METHODS_APP_NAME = "jaxrsMethods";
    public static final String MP50_MPTEL11_ID = MicroProfileActions.MP50_ID + "_MPTEL11";
    public static final FeatureSet MP50_MPTEL11 = MicroProfileActions.MP50
                    .addFeature("mpTelemetry-1.1")
                    .build(MP50_MPTEL11_ID);

    @TestServlets({
                    @TestServlet(contextRoot = W3C_TRACE_APP_NAME, servlet = W3CTracePropagationTestServlet.class),
                    @TestServlet(contextRoot = W3C_TRACE_BAGGAGE_APP_NAME, servlet = W3CTraceBaggagePropagationTestServlet.class),
                    @TestServlet(contextRoot = B3_APP_NAME, servlet = B3PropagationTestServlet.class),
                    @TestServlet(contextRoot = B3_MULTI_APP_NAME, servlet = B3MultiPropagationTestServlet.class),
                    @TestServlet(contextRoot = JAEGER_APP_NAME, servlet = JaegerPropagationTestServlet.class),
                    @TestServlet(contextRoot = METHODS_APP_NAME, servlet = JaxRsMethodTestServlet.class),
                    @TestServlet(contextRoot = METHODS_APP_NAME, servlet = JaxRsResponseCodeTestServlet.class),
    })
    @Server(SERVER_NAME)
    public static LibertyServer server;
    
    @ClassRule
    public static RepeatTests repeatTest = MicroProfileActions.repeat(SERVER_NAME,
                                                                      MicroProfileActions.MP60, // first test in LITE mode
                                                                      MicroProfileActions.MP61, // rest are FULL mode
                                                                      MP50_MPTEL11); 

    @BeforeClass
    public static void setUp() throws Exception {
        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty("otel.sdk.disabled", "true")
                        .addProperty("otel.traces.exporter", "oltp")
                        .addProperty("otel.bsp.schedule.delay", "100");
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackage(JaxRsEndpoints.class.getPackage())
                        .addPackage(InMemorySpanExporter.class.getPackage())
                        .addPackage(TestSpans.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        PropertiesAsset w3cTraceAppConfig = new PropertiesAsset()
                        .include(appConfig)
                        .addProperty("otel.propagators", "tracecontext");
        WebArchive w3cTraceApp = ShrinkWrap.create(WebArchive.class, W3C_TRACE_APP_NAME + ".war")
                        .addClass(W3CTracePropagationTestServlet.class)
                        .addPackage(InMemorySpanExporter.class.getPackage())
                        .addPackage(TestSpans.class.getPackage())
                        .addPackage(PropagationHeaderEndpoint.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(w3cTraceAppConfig, "META-INF/microprofile-config.properties");

        PropertiesAsset w3cTraceBaggageAppConfig = new PropertiesAsset()
                        .include(appConfig)
                        .addProperty("otel.propagators", "tracecontext, baggage");
        WebArchive w3cTraceBaggageApp = ShrinkWrap.create(WebArchive.class, W3C_TRACE_BAGGAGE_APP_NAME + ".war")
                        .addClass(W3CTraceBaggagePropagationTestServlet.class)
                        .addPackage(InMemorySpanExporter.class.getPackage())
                        .addPackage(TestSpans.class.getPackage())
                        .addPackage(PropagationHeaderEndpoint.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(w3cTraceBaggageAppConfig, "META-INF/microprofile-config.properties");

        PropertiesAsset b3AppConfig = new PropertiesAsset()
                        .include(appConfig)
                        .addProperty("otel.propagators", "b3");
        WebArchive b3App = ShrinkWrap.create(WebArchive.class, B3_APP_NAME + ".war")
                        .addClass(B3PropagationTestServlet.class)
                        .addPackage(InMemorySpanExporter.class.getPackage())
                        .addPackage(TestSpans.class.getPackage())
                        .addPackage(PropagationHeaderEndpoint.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(b3AppConfig, "META-INF/microprofile-config.properties");

        PropertiesAsset b3MultiAppConfig = new PropertiesAsset()
                        .include(appConfig)
                        .addProperty("otel.propagators", "b3multi");
        WebArchive b3MultiApp = ShrinkWrap.create(WebArchive.class, B3_MULTI_APP_NAME + ".war")
                        .addClass(B3MultiPropagationTestServlet.class)
                        .addPackage(InMemorySpanExporter.class.getPackage())
                        .addPackage(TestSpans.class.getPackage())
                        .addPackage(PropagationHeaderEndpoint.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(b3MultiAppConfig, "META-INF/microprofile-config.properties");

        PropertiesAsset jaegerAppConfig = new PropertiesAsset()
                        .include(appConfig)
                        .addProperty("otel.propagators", "jaeger");
        WebArchive jaegerApp = ShrinkWrap.create(WebArchive.class, JAEGER_APP_NAME + ".war")
                        .addClass(JaegerPropagationTestServlet.class)
                        .addPackage(InMemorySpanExporter.class.getPackage())
                        .addPackage(TestSpans.class.getPackage())
                        .addPackage(PropagationHeaderEndpoint.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(jaegerAppConfig, "META-INF/microprofile-config.properties");

        WebArchive methodsApp = ShrinkWrap.create(WebArchive.class, METHODS_APP_NAME + ".war")
                        .addPackage(JaxRsMethodTestEndpoints.class.getPackage())
                        .addPackage(JaxRsResponseCodeTestEndpoints.class.getPackage())
                        .addPackage(InMemorySpanExporter.class.getPackage())
                        .addPackage(TestSpans.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, app, SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, w3cTraceApp, SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, w3cTraceBaggageApp, SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, b3App, SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, b3MultiApp, SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, jaegerApp, SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, methodsApp, SERVER_ONLY);
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, true,
                             server -> {
                                 assertNotNull("'SRVE0169I: Loading Web Module: " + APP_NAME + "' message not found in log before rerstore",
                                               server.waitForStringInLogUsingMark("SRVE0169I: .*" + APP_NAME, 0));
                                 assertNotNull("'CWWKZ0001I: Application " + APP_NAME + " started' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I: .*" + APP_NAME, 0));
                                 configureBeforeRestore();
                             });
         
        server.startServer("JaxRsIntegrationTest.log");
    }
    
    private static void configureBeforeRestore() {
        try {
            server.saveServerConfiguration();
            Map<String, String> config = new HashMap<>();
            config.put("OTEL_SDK_DISABLED", "false");
            config.put("OTEL_TRACES_EXPORTER", "in-memory");
            configureEnvVariable(server, config);
        } catch (Exception e) {
            throw new AssertionError("Unexpected error configuring test.", e);
        }
    }
    
    @Test
    public void testIntegrationWithJaxRsClient() throws Exception {
        HttpRequest pokeJax = new HttpRequest(server, "/" + APP_NAME + "/endpoints/jaxrsclient");
        String traceId = readTraceId(pokeJax);
        
        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspans/" + traceId);
            assertEquals(TEST_PASSED, readspans.run(String.class));           
        } else {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspansmptel11/" + traceId);
            assertEquals(TEST_PASSED, readspans.run(String.class));
        }           
    }

    @Test
    public void testIntegrationWithJaxRsClientAsync() throws Exception {
        HttpRequest pokeJax = new HttpRequest(server, "/" + APP_NAME + "/endpoints/jaxrsclientasync");
        String traceId = readTraceId(pokeJax);

        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspans/" + traceId);
            assertEquals(TEST_PASSED, readspans.run(String.class));
        } else {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspansmptel11/" + traceId);
            assertEquals(TEST_PASSED, readspans.run(String.class));
        } 
    }

    @Test
    public void testIntegrationWithMpClient() throws Exception {
        HttpRequest pokeMp = new HttpRequest(server, "/" + APP_NAME + "/endpoints/mpclient");
        String traceId = readTraceId(pokeMp);

        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspans/" + traceId);
            assertEquals(TEST_PASSED, readspans.run(String.class));
        } else {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspansmptel11/" + traceId);
            assertEquals(TEST_PASSED, readspans.run(String.class));
        } 
    }

    @Test
    public void testIntegrationWithMpClientAsync() throws Exception {
        HttpRequest pokeMp = new HttpRequest(server, "/" + APP_NAME + "/endpoints/mpclientasync");
        String traceId = readTraceId(pokeMp);

        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspans/" + traceId);
            assertEquals(TEST_PASSED, readspans.run(String.class));
        } else {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspansmptel11/" + traceId);
            assertEquals(TEST_PASSED, readspans.run(String.class));
        } 
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer();
        } finally {
            server.restoreServerConfiguration();
            configureEnvVariable(server, emptyMap());
        }
    }

    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("[0-9a-f]{32}");

    private String readTraceId(HttpRequest httpRequest) throws Exception {
        String response = httpRequest.run(String.class);
        if (!TRACE_ID_PATTERN.matcher(response).matches()) {
            Assert.fail("Request failed, response: " + response);
        }
        return response;
    }
}
