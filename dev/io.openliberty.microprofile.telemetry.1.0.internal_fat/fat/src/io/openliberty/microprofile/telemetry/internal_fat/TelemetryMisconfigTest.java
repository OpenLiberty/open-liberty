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
import static org.junit.Assert.assertFalse;


import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
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

    @BeforeClass
    public static void setUp() throws Exception {
        //Invalid exporter name (Should allow Jaeger, Zipkin, OTLP, Logging)
        WebArchive invalidExporterApp = ShrinkWrap.create(WebArchive.class, INVALID_EXPORTER_APP_NAME + ".war")
                        .addPackage(JaxRsMisConfigEndpoints.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false\notel.traces.exporter=" + INVALID_EXPORTER),
                                       "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, invalidExporterApp, SERVER_ONLY);

        //Invalid Jaeger endpoint (Should allow URL as http://HOST:PORT)

        WebArchive invalidJaegerEndpointApp = ShrinkWrap.create(WebArchive.class, INVALID_JAEGER_ENDPOINT_APP_NAME + ".war")
                        .addPackage(JaxRsMisConfigEndpoints.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false\notel.traces.exporter=jaeger\notel.exporter.jaeger.endpoint=" + INVALID_JAEGER_ENDPOINT),
                                       "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, invalidJaegerEndpointApp, SERVER_ONLY);

        //Invalid Zipkin endpoint (Should allow URL as http://HOST:PORT)

        WebArchive invalidZipkinEndpointApp = ShrinkWrap.create(WebArchive.class, INVALID_ZIPKIN_ENDPOINT_APP_NAME + ".war")
                        .addPackage(JaxRsMisConfigEndpoints.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false\notel.traces.exporter=zipkin\notel.exporter.zipkin.endpoint=" + INVALID_ZIPKIN_ENDPOINT),
                                       "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, invalidZipkinEndpointApp, SERVER_ONLY);

        //Invalid OTLP endpoint (Should allow URL as http://HOST:PORT)

        WebArchive invalidOtlpEndpointApp = ShrinkWrap.create(WebArchive.class, INVALID_OTLP_ENDPOINT_APP_NAME + ".war")
                        .addPackage(JaxRsMisConfigEndpoints.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false\notel.traces.exporter=otlp\notel.exporter.otlp.endpoint=" + INVALID_OTLP_ENDPOINT),
                                       "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, invalidOtlpEndpointApp, SERVER_ONLY);

        //Invalid Jaeger timeout amount (Should allow number of milliseconds e.g. 1000)

        WebArchive invalidJaegerTimeoutApp = ShrinkWrap.create(WebArchive.class, INVALID_JAEGER_TIMEOUT_APP_NAME + ".war")
                        .addPackage(JaxRsMisConfigEndpoints.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false\notel.traces.exporter=jaeger\notel.exporter.jaeger.timeout=" + INVALID_TIMEOUT),
                                       "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, invalidJaegerTimeoutApp, SERVER_ONLY);

        //Server name in the endpoint is not known
        WebArchive notKnownEndpointApp = ShrinkWrap.create(WebArchive.class, NOT_KNOWN_ENDPOINT_APP_NAME + ".war")
                        .addPackage(JaxRsMisConfigEndpoints.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false\notel.traces.exporter=jaeger\notel.exporter.jaeger.endpoint=" + NOT_KNOWN_ENDPOINT),
                                       "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, notKnownEndpointApp, SERVER_ONLY);

        //The endpoint does not exist
        WebArchive doesNotExistEndpointApp = ShrinkWrap.create(WebArchive.class, DOES_NOT_EXIST_ENDPOINT_APP_NAME + ".war")
                        .addPackage(JaxRsMisConfigEndpoints.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false\notel.traces.exporter=jaeger\notel.exporter.jaeger.endpoint=" + DOES_NOT_EXIST_ENDPOINT),
                                       "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, doesNotExistEndpointApp, SERVER_ONLY);

        server.startServer();
    }

    @Test
    @ExpectedFFDC({ "org.jboss.resteasy.spi.UnhandledException" })
    public void testInvalidExporter() throws Exception {
        server.setMarkToEndOfLog();

        new HttpRequest(server, "/" + INVALID_EXPORTER_APP_NAME + "/misconfig/jaxrsclient")
                        .expectCode(500)
                        .run(String.class);

        assertFalse(server.waitForStringInLogUsingMark("Unrecognized value for otel.traces.exporter: " + INVALID_EXPORTER).isEmpty());
    }

    @Test
    @ExpectedFFDC({ "org.jboss.resteasy.spi.UnhandledException" })
    public void testInvalidJaegerExporterEndpoint() throws Exception {
        server.setMarkToEndOfLog();

        new HttpRequest(server, "/" + INVALID_JAEGER_ENDPOINT_APP_NAME + "/misconfig/jaxrsclient")
                        .expectCode(500)
                        .run(String.class);

        assertFalse(server.waitForStringInLogUsingMark("Invalid endpoint, must start with http:// or https://: " + INVALID_JAEGER_ENDPOINT).isEmpty());

    }

    @Test
    @ExpectedFFDC({ "org.jboss.resteasy.spi.UnhandledException" })
    public void testInvalidZipkinExporterEndpoint() throws Exception {
        server.setMarkToEndOfLog();

        new HttpRequest(server, "/" + INVALID_ZIPKIN_ENDPOINT_APP_NAME + "/misconfig/jaxrsclient")
                        .expectCode(500)
                        .run(String.class);

        assertFalse(server.waitForStringInLogUsingMark("invalid POST url: " + INVALID_ZIPKIN_ENDPOINT).isEmpty());
    }

    @Test
    @ExpectedFFDC({ "org.jboss.resteasy.spi.UnhandledException" })
    public void testInvalidOtlpExporterEndpoint() throws Exception {
        server.setMarkToEndOfLog();

        new HttpRequest(server, "/" + INVALID_OTLP_ENDPOINT_APP_NAME + "/misconfig/jaxrsclient")
                        .expectCode(500)
                        .run(String.class);

        assertFalse(server.waitForStringInLogUsingMark("OTLP endpoint must be a valid URL: " + INVALID_OTLP_ENDPOINT).isEmpty());
    }

    @Test
    public void testNotKnownEndpoint() throws Exception {
        server.setMarkToEndOfLog();

        new HttpRequest(server, "/" + NOT_KNOWN_ENDPOINT_APP_NAME + "/misconfig/jaxrsclient")
                        .expectCode(200)
                        .run(String.class);

        assertFalse(server.waitForStringInLogUsingMark("Failed to export spans. The request could not be executed. Full error message:.*" + INVALID_JAEGER_ENDPOINT.toLowerCase())
                        .isEmpty());
    }

    @Test
    public void testDoesNotExistEndpoint() throws Exception {
        server.setMarkToEndOfLog();

        new HttpRequest(server, "/" + DOES_NOT_EXIST_ENDPOINT_APP_NAME + "/misconfig/jaxrsclient")
                        .expectCode(200)
                        .run(String.class);

        assertFalse(server.waitForStringInLogUsingMark("Failed to export spans. The request could not be executed. Full error message: Failed to connect to.*" + ":10000")
                        .isEmpty());
    }

    @Test
    @ExpectedFFDC({ "org.jboss.resteasy.spi.UnhandledException" })
    public void testInvalidJaegerTimeout() throws Exception {
        server.setMarkToEndOfLog();

        new HttpRequest(server, "/" + INVALID_JAEGER_TIMEOUT_APP_NAME + "/misconfig/jaxrsclient")
                        .expectCode(500)
                        .run(String.class);

        assertFalse(server.waitForStringInLogUsingMark("Invalid duration property otel.exporter.jaeger.timeout").isEmpty());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("SRVE0777E", // Exception thrown by application class
                          "SRVE0315E: .*INVALID_EXPORTER", // An exception occurred: ... Unrecognized value for otel.traces.exporter: INVALID_EXPORTER
                          "SRVE0315E: .*INVALID_TIMEOUT", // An exception occurred: ... Invalid duration property otel.exporter.jaeger.timeout: INVALID_TIMEOUT
                          "SRVE0315E: .*ENDPOINT"); //An exception occurred: ... Endpoint must be a valid URL: INVALID_ENDPOINT
    }
}
