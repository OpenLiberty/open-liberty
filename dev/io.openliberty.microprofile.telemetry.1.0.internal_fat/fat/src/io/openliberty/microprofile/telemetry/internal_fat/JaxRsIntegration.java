/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
import static io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.JaxRsEndpoints.TEST_PASSED;
import static org.junit.Assert.assertEquals;

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

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.JaxRsEndpoints;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.common.PropagationHeaderEndpoint;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.injected.InjectedClientTestClient;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.injected.InjectedClientTestEndpoints;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.injected.InjectedClientTestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.methods.JaxRsMethodTestEndpoints;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.methods.JaxRsMethodTestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.responses.JaxRsResponseCodeTestEndpoints;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.responses.JaxRsResponseCodeTestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.route.JaxRsRouteTestEndpoints;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.route.JaxRsRouteTestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.transports.B3MultiPropagationTestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.transports.B3PropagationTestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.transports.JaegerPropagationTestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.transports.W3CTraceBaggagePropagationTestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.transports.W3CTraceContextPropagatorTestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.transports.W3CTracePropagationTestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.common.TestSpans;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporterProvider;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;
import io.openliberty.microprofile.telemetry.internal_fat.shared.spans.AbstractSpanMatcher;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JaxRsIntegration extends FATServletClient {

    public static final String SERVER_NAME = "Telemetry10Jax";
    public static final String APP_NAME = "JaxPropagation";
    public static final String W3C_TRACE_APP_NAME = "w3cTrace";
    public static final String W3C_TRACE_CONTEXT_PROPAGATOR_APP_NAME = "w3cTraceContextPropagator";
    public static final String W3C_TRACE_BAGGAGE_APP_NAME = "w3cTraceBaggage";
    public static final String B3_APP_NAME = "b3";
    public static final String B3_MULTI_APP_NAME = "b3multi";
    public static final String JAEGER_APP_NAME = "jaeger";
    public static final String ASYNC_SERVER_APP_NAME = "jaxrsAsyncServer";
    public static final String METHODS_APP_NAME = "jaxrsMethods";

    @TestServlets({
                    @TestServlet(contextRoot = W3C_TRACE_APP_NAME, servlet = W3CTracePropagationTestServlet.class),
                    @TestServlet(contextRoot = W3C_TRACE_CONTEXT_PROPAGATOR_APP_NAME, servlet = W3CTraceContextPropagatorTestServlet.class),
                    @TestServlet(contextRoot = W3C_TRACE_BAGGAGE_APP_NAME, servlet = W3CTraceBaggagePropagationTestServlet.class),
                    @TestServlet(contextRoot = B3_APP_NAME, servlet = B3PropagationTestServlet.class),
                    @TestServlet(contextRoot = B3_MULTI_APP_NAME, servlet = B3MultiPropagationTestServlet.class),
                    @TestServlet(contextRoot = JAEGER_APP_NAME, servlet = JaegerPropagationTestServlet.class),
                    //@TestServlet(contextRoot = ASYNC_SERVER_APP_NAME, servlet = JaxRsServerAsyncTestServlet.class),
                    @TestServlet(contextRoot = METHODS_APP_NAME, servlet = JaxRsMethodTestServlet.class),
                    @TestServlet(contextRoot = METHODS_APP_NAME, servlet = JaxRsResponseCodeTestServlet.class),
                    @TestServlet(contextRoot = METHODS_APP_NAME, servlet = JaxRsRouteTestServlet.class),
                    @TestServlet(contextRoot = METHODS_APP_NAME, servlet = InjectedClientTestServlet.class),
    })
    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = TelemetryActions.allMPRepeats(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty("otel.sdk.disabled", "false")
                        .addProperty("otel.traces.exporter", "in-memory")
                        .addProperty("otel.bsp.schedule.delay", "100")
                        .addProperty("feature.version", FATSuite.getTelemetryVersionUnderTest());
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackage(JaxRsEndpoints.class.getPackage())
                        .addPackage(InMemorySpanExporter.class.getPackage())
                        .addPackage(TestSpans.class.getPackage())
                        .addPackage(AbstractSpanMatcher.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        PropertiesAsset w3cTraceAppConfig = new PropertiesAsset()
                        .include(appConfig)
                        .addProperty("otel.propagators", "tracecontext");
        WebArchive w3cTraceApp = ShrinkWrap.create(WebArchive.class, W3C_TRACE_APP_NAME + ".war")
                        .addClass(W3CTracePropagationTestServlet.class)
                        .addPackage(InMemorySpanExporter.class.getPackage())
                        .addPackage(TestSpans.class.getPackage())
                        .addPackage(AbstractSpanMatcher.class.getPackage())
                        .addPackage(PropagationHeaderEndpoint.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(w3cTraceAppConfig, "META-INF/microprofile-config.properties");

        WebArchive w3cTraceContextApp = ShrinkWrap.create(WebArchive.class, W3C_TRACE_CONTEXT_PROPAGATOR_APP_NAME + ".war")
                        .addClass(W3CTraceContextPropagatorTestServlet.class);

        PropertiesAsset w3cTraceBaggageAppConfig = new PropertiesAsset()
                        .include(appConfig)
                        .addProperty("otel.propagators", "tracecontext, baggage");
        WebArchive w3cTraceBaggageApp = ShrinkWrap.create(WebArchive.class, W3C_TRACE_BAGGAGE_APP_NAME + ".war")
                        .addClass(W3CTraceBaggagePropagationTestServlet.class)
                        .addPackage(InMemorySpanExporter.class.getPackage())
                        .addPackage(TestSpans.class.getPackage())
                        .addPackage(AbstractSpanMatcher.class.getPackage())
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
                        .addPackage(AbstractSpanMatcher.class.getPackage())
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
                        .addPackage(AbstractSpanMatcher.class.getPackage())
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
                        .addPackage(AbstractSpanMatcher.class.getPackage())
                        .addPackage(PropagationHeaderEndpoint.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(jaegerAppConfig, "META-INF/microprofile-config.properties");
/*
 * We do not test this one without concurrency-3.0 enabled as it uses a ManagedExecutorService
 *
 * WebArchive asyncServerApp = ShrinkWrap.create(WebArchive.class, ASYNC_SERVER_APP_NAME + ".war")
 * .addPackage(JaxRsServerAsyncTestServlet.class.getPackage())
 * .addPackage(InMemorySpanExporter.class.getPackage())
 * .addPackage(TestSpans.class.getPackage())
 * .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
 * .addAsResource(appConfig, "META-INF/microprofile-config.properties");
 */

        PropertiesAsset methodsAppConfig = new PropertiesAsset()
                        .include(appConfig)
                        .addProperty(InjectedClientTestClient.class.getName() + "/mp-rest/url",
                                     HttpUtils.createURL(server, METHODS_APP_NAME).toString());

        WebArchive methodsApp = ShrinkWrap.create(WebArchive.class, METHODS_APP_NAME + ".war")
                        .addPackage(JaxRsMethodTestEndpoints.class.getPackage())
                        .addPackage(JaxRsResponseCodeTestEndpoints.class.getPackage())
                        .addPackage(JaxRsRouteTestEndpoints.class.getPackage())
                        .addPackage(InjectedClientTestEndpoints.class.getPackage())
                        .addPackage(InMemorySpanExporter.class.getPackage())
                        .addPackage(TestSpans.class.getPackage())
                        .addPackage(AbstractSpanMatcher.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(methodsAppConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, app, SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, w3cTraceApp, SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, w3cTraceContextApp, SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, w3cTraceBaggageApp, SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, b3App, SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, b3MultiApp, SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, jaegerApp, SERVER_ONLY);
        //ShrinkHelper.exportAppToServer(server, asyncServerApp, SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, methodsApp, SERVER_ONLY);
        server.startServer();
    }

    @Test
    public void testIntegrationWithJaxRsClient() throws Exception {
        HttpRequest pokeJax = new HttpRequest(server, "/" + APP_NAME + "/endpoints/jaxrsclient");
        String traceId = readTraceId(pokeJax);
        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspans/" + traceId);
            assertEquals(TEST_PASSED, readspans.run(String.class));
        } else if (TelemetryActions.mpTelemetry20IsActive()) {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspansmptel20/" + traceId);
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
        } else if (TelemetryActions.mpTelemetry20IsActive()) {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspansmptel20/" + traceId);
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
        } else if (TelemetryActions.mpTelemetry20IsActive()) {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspansmptel20/" + traceId);
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
        } else if (TelemetryActions.mpTelemetry20IsActive()) {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspansmptel20/" + traceId);
            assertEquals(TEST_PASSED, readspans.run(String.class));
        } else {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspansmptel11/" + traceId);
            assertEquals(TEST_PASSED, readspans.run(String.class));
        }
    }

    @Test
    public void testIntegrationWithJaxRsClientWithSpan() throws Exception {
        HttpRequest pokeJax = new HttpRequest(server, "/" + APP_NAME + "/endpoints/jaxrsclientwithspan");
        String traceId = readTraceId(pokeJax);
        if (TelemetryActions.mpTelemetry20IsActive()) {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspanswithspanmptel20/" + traceId);
            assertEquals(TEST_PASSED, readspans.run(String.class));
        } else {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspanswithspan/" + traceId);
            assertEquals(TEST_PASSED, readspans.run(String.class));
        }
    }

    @Test
    public void testTraceState() throws Exception {
        HttpRequest pokeJax = new HttpRequest(server, "/" + APP_NAME + "/endpoints/traceState");
        String traceId = readTraceId(pokeJax);

        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspans/" + traceId);
            assertEquals(TEST_PASSED, readspans.run(String.class));
        }else if (TelemetryActions.mpTelemetry20IsActive()) {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspansmptel20/" + traceId);
            assertEquals(TEST_PASSED, readspans.run(String.class));
        } else {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspansmptel11/" + traceId);
            assertEquals(TEST_PASSED, readspans.run(String.class));
        }

    }

    @Test
    public void testTraceStateFromHeaders() throws Exception {
        //Manually creates the tracestate header with the HTTP request header. 
        //Liberty and OpenTelemetry then turn it into a TraceState object on the current span by the time it gets to this method JaxRsEndpoints.testTraceStateFromHeaders
        HttpRequest pokeJax = new HttpRequest(server, "/" + APP_NAME + "/endpoints/traceStateFromHeaders").requestProp("tracestate", "key_1=value.1")
                        .requestProp("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
        String traceId = readTraceId(pokeJax);
        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspans/" + traceId);
            assertEquals(TEST_PASSED, readspans.run(String.class));
        } else if (TelemetryActions.mpTelemetry20IsActive()) {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspansmptel20/" + traceId);
            assertEquals(TEST_PASSED, readspans.run(String.class));
        } else {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspansmptel11/" + traceId);
            assertEquals(TEST_PASSED, readspans.run(String.class));
        }
    }

    @Test
    public void testEmptyTraceState() throws Exception {
        HttpRequest pokeJax = new HttpRequest(server, "/" + APP_NAME + "/endpoints/emptyTraceState");
        String traceId = readTraceId(pokeJax);
        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspans/" + traceId);
            assertEquals(TEST_PASSED, readspans.run(String.class));
        } else if (TelemetryActions.mpTelemetry20IsActive()) {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspansmptel20/" + traceId);
            assertEquals(TEST_PASSED, readspans.run(String.class));
        } else {
            HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspansmptel11/" + traceId);
            assertEquals(TEST_PASSED, readspans.run(String.class));
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
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
