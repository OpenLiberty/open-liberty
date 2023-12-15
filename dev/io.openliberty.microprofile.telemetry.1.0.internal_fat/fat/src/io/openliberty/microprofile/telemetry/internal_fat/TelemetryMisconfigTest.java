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
import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrsmisconfig.JaxRsMisConfigEndpoints;

//Tests behaviour when placing an invalid value into the OpenTelemetry configuration options
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class TelemetryMisconfigTest extends FATServletClient {

    public static final String SERVER_NAME = "Telemetry10MisConfig";
    public static final String INVALID_EXPORTER_APP_NAME = "InvalidExporterApp";
    public static final String INVALID_JAEGER_ENDPOINT_APP_NAME = "InvalidJaegerEndpointApp";
    public static final String INVALID_ZIPKIN_ENDPOINT_APP_NAME = "InvalidZipkinEndpointApp";
    public static final String INVALID_OTLP_ENDPOINT_APP_NAME = "InvalidOtlpEndpointApp";
    public static final String INVALID_JAEGER_TIMEOUT_APP_NAME = "InvalidJaegerTimeoutApp";
    public static final String NOT_KNOWN_ENDPOINT_APP_NAME = "NotKnownEndpointApp";
    public static final String DOES_NOT_EXIST_ENDPOINT_APP_NAME = "DoesNotExistEndpointApp";
    public static final String INVALID_EXPORTER = "INVALID_EXPORTER";
    public static final String INVALID_TIMEOUT = "INVALID_TIMEOUT";
    public static final String INVALID_OTLP_ENDPOINT = "INVALID_OTLP_ENDPOINT";
    public static final String INVALID_JAEGER_ENDPOINT = "INVALID_JAEGER_ENDPOINT";
    public static final String INVALID_ZIPKIN_ENDPOINT = "INVALID_ZIPKIN_ENDPOINT";
    public static final String NOT_KNOWN_ENDPOINT = "http://" + INVALID_JAEGER_ENDPOINT;
    public static final String DOES_NOT_EXIST_ENDPOINT = "http://localhost:10000";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.allMPRepeats(SERVER_NAME);

    private static WebArchive invalidExporterApp = null;
    private static WebArchive invalidJaegerEndpointApp = null;
    private static WebArchive invalidZipkinEndpointApp = null;
    private static WebArchive invalidOtlpEndpointApp = null;
    private static WebArchive invalidJaegerTimeoutApp = null;
    private static WebArchive notKnownEndpointApp = null;
    private static WebArchive doesNotExistEndpointApp = null;

    @BeforeClass
    public static void setUp() throws Exception {
        invalidExporterApp = ShrinkWrap.create(WebArchive.class, INVALID_EXPORTER_APP_NAME + ".war")
                        .addPackage(JaxRsMisConfigEndpoints.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false\notel.traces.exporter=" + INVALID_EXPORTER),
                                       "META-INF/microprofile-config.properties");

        //Invalid Jaeger endpoint (Should allow URL as http://HOST:PORT)

        invalidJaegerEndpointApp = ShrinkWrap.create(WebArchive.class, INVALID_JAEGER_ENDPOINT_APP_NAME + ".war")
                        .addPackage(JaxRsMisConfigEndpoints.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false\notel.traces.exporter=jaeger\notel.exporter.jaeger.endpoint=" + INVALID_JAEGER_ENDPOINT),
                                       "META-INF/microprofile-config.properties");

        //Invalid Zipkin endpoint (Should allow URL as http://HOST:PORT)

        invalidZipkinEndpointApp = ShrinkWrap.create(WebArchive.class, INVALID_ZIPKIN_ENDPOINT_APP_NAME + ".war")
                        .addPackage(JaxRsMisConfigEndpoints.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false\notel.traces.exporter=zipkin\notel.exporter.zipkin.endpoint=" + INVALID_ZIPKIN_ENDPOINT),
                                       "META-INF/microprofile-config.properties");

        //Invalid OTLP endpoint (Should allow URL as http://HOST:PORT)

        invalidOtlpEndpointApp = ShrinkWrap.create(WebArchive.class, INVALID_OTLP_ENDPOINT_APP_NAME + ".war")
                        .addPackage(JaxRsMisConfigEndpoints.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false\notel.traces.exporter=otlp\notel.exporter.otlp.endpoint=" + INVALID_OTLP_ENDPOINT),
                                       "META-INF/microprofile-config.properties");

        //Invalid Jaeger timeout amount (Should allow number of milliseconds e.g. 1000)

        invalidJaegerTimeoutApp = ShrinkWrap.create(WebArchive.class, INVALID_JAEGER_TIMEOUT_APP_NAME + ".war")
                        .addPackage(JaxRsMisConfigEndpoints.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false\notel.traces.exporter=jaeger\notel.exporter.jaeger.timeout=" + INVALID_TIMEOUT),
                                       "META-INF/microprofile-config.properties");

        notKnownEndpointApp = ShrinkWrap.create(WebArchive.class, NOT_KNOWN_ENDPOINT_APP_NAME + ".war")
                        .addPackage(JaxRsMisConfigEndpoints.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false\notel.traces.exporter=jaeger\notel.exporter.jaeger.endpoint=" + NOT_KNOWN_ENDPOINT),
                                       "META-INF/microprofile-config.properties");

        doesNotExistEndpointApp = ShrinkWrap.create(WebArchive.class, DOES_NOT_EXIST_ENDPOINT_APP_NAME + ".war")
                        .addPackage(JaxRsMisConfigEndpoints.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false\notel.traces.exporter=jaeger\notel.exporter.jaeger.endpoint=" + DOES_NOT_EXIST_ENDPOINT),
                                       "META-INF/microprofile-config.properties");

        //Don't validate the apps because they have not been deployed yet.
        server.startServerAndValidate(LibertyServer.DEFAULT_PRE_CLEAN, LibertyServer.DEFAULT_CLEANSTART, false);
    }

    @Test
    @ExpectedFFDC({ "io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException" })
    public void testInvalidExporter() throws Exception {
        deployAndWaitForApp(invalidExporterApp, INVALID_EXPORTER_APP_NAME);
        new HttpRequest(server, "/" + INVALID_EXPORTER_APP_NAME + "/misconfig/jaxrsclient")
                        .expectCode(200)
                        .run(String.class);

        assertNotNull(server.waitForStringInLogUsingMark("Unrecognized value for otel.traces.exporter: " + INVALID_EXPORTER));
    }

    @Test
    @ExpectedFFDC(repeatAction = MicroProfileActions.MP60_ID, value = { "java.lang.IllegalArgumentException" })
    @ExpectedFFDC(repeatAction = { MicroProfileActions.MP61_ID, MicroProfileActions.MP50_ID, MicroProfileActions.MP41_ID, MicroProfileActions.MP14_ID },
                  value = { "io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException" })
    public void testInvalidJaegerExporterEndpoint() throws Exception {
        deployAndWaitForApp(invalidJaegerEndpointApp, INVALID_JAEGER_ENDPOINT_APP_NAME);
        new HttpRequest(server, "/" + INVALID_JAEGER_ENDPOINT_APP_NAME + "/misconfig/jaxrsclient")
                        .expectCode(200)
                        .run(String.class);
        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            assertNotNull(server.waitForStringInLogUsingMark("Invalid endpoint, must start with http:// or https://: " + INVALID_JAEGER_ENDPOINT));
        } else {
            assertNotNull(server.waitForStringInLogUsingMark("Unexpected configuration error."));
        }
    }

    @Test
    @ExpectedFFDC(repeatAction = MicroProfileActions.MP60_ID, value = { "java.lang.IllegalArgumentException" })
    @ExpectedFFDC(repeatAction = { MicroProfileActions.MP61_ID, MicroProfileActions.MP50_ID, MicroProfileActions.MP41_ID, MicroProfileActions.MP14_ID },
                  value = { "io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException" })
    public void testInvalidZipkinExporterEndpoint() throws Exception {
        deployAndWaitForApp(invalidZipkinEndpointApp, INVALID_ZIPKIN_ENDPOINT_APP_NAME);
        new HttpRequest(server, "/" + INVALID_ZIPKIN_ENDPOINT_APP_NAME + "/misconfig/jaxrsclient")
                        .expectCode(200)
                        .run(String.class);

        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            assertNotNull(server.waitForStringInLogUsingMark("invalid POST url: " + INVALID_ZIPKIN_ENDPOINT));
        } else {
            assertNotNull(server.waitForStringInLogUsingMark("Unexpected configuration error."));
        }
    }

    @Test
    @ExpectedFFDC({ "io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException" })
    public void testInvalidOtlpExporterEndpoint() throws Exception {
        deployAndWaitForApp(invalidOtlpEndpointApp, INVALID_OTLP_ENDPOINT_APP_NAME);
        new HttpRequest(server, "/" + INVALID_OTLP_ENDPOINT_APP_NAME + "/misconfig/jaxrsclient")
                        .expectCode(200)
                        .run(String.class);

        assertNotNull(server.waitForStringInLogUsingMark("OTLP endpoint must be a valid URL: " + INVALID_OTLP_ENDPOINT));
    }

    @Test
    public void testNotKnownEndpoint() throws Exception {
        deployAndWaitForApp(notKnownEndpointApp, NOT_KNOWN_ENDPOINT_APP_NAME);
        new HttpRequest(server, "/" + NOT_KNOWN_ENDPOINT_APP_NAME + "/misconfig/jaxrsclient")
                        .expectCode(200)
                        .run(String.class);

        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            assertNotNull(server.waitForStringInLogUsingMark("Failed to export spans. The request could not be executed. Full error message:.*"
                                                             + INVALID_JAEGER_ENDPOINT.toLowerCase()));
        } else {
            assertNotNull(server.waitForStringInLogUsingMark("Failed to export spans. Server responded with gRPC status code 2. Error message:.*"
                                                             + INVALID_JAEGER_ENDPOINT.toLowerCase()));
        }
    }

    @Test
    public void testDoesNotExistEndpoint() throws Exception {
        deployAndWaitForApp(doesNotExistEndpointApp, DOES_NOT_EXIST_ENDPOINT_APP_NAME);
        new HttpRequest(server, "/" + DOES_NOT_EXIST_ENDPOINT_APP_NAME + "/misconfig/jaxrsclient")
                        .expectCode(200)
                        .run(String.class);

        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            assertNotNull(server.waitForStringInLogUsingMark("Failed to export spans. The request could not be executed. Full error message: Failed to connect to.*" + ":10000"));
        } else {
            assertNotNull(server.waitForStringInLogUsingMark("Failed to export spans. Server responded with gRPC status code 2. Error message: Failed to connect to.*" + ":10000"));
        }
    }

    @Test
    @ExpectedFFDC({ "io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException" })
    public void testInvalidJaegerTimeout() throws Exception {
        deployAndWaitForApp(invalidJaegerTimeoutApp, INVALID_JAEGER_TIMEOUT_APP_NAME);
        new HttpRequest(server, "/" + INVALID_JAEGER_TIMEOUT_APP_NAME + "/misconfig/jaxrsclient")
                        .expectCode(200)
                        .run(String.class);

        assertNotNull(server.waitForStringInLogUsingMark("Invalid duration property otel.exporter.jaeger.timeout"));
    }

    private static void deployAndWaitForApp(WebArchive app, String appName) throws Exception {
        server.setMarkToEndOfLog();
        ShrinkHelper.exportAppToServer(server, app, SERVER_ONLY);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWMOT5002E", "CWWKZ0014W"); //CWMOT5002E thrown in OpenTelemetryProducer. CWWKZ0014W thrown because apps defined in server.xml will be added dynamically
    }
}
