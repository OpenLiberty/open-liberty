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
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrsmisconfig.JaxRsMisConfigEndpoints;

@RunWith(FATRunner.class)
public class TelemetryMisconfigTest extends FATServletClient {

    public static final String SERVER_NAME = "Telemetry10MisConfig";
    public static final String INVALID_EXPORTER_APP_NAME = "InvalidExporterApp";
    public static final String INVALID_JAEGER_ENDPOINT_APP_NAME = "InvalidJaegerEndpointApp";
    public static final String INVALID_ZIPKIN_ENDPOINT_APP_NAME = "InvalidZipkinEndpointApp";
    public static final String INVALID_OTLP_ENDPOINT_APP_NAME = "InvalidOtlpEndpointApp";
    public static final String INVALID_JAEGER_TIMEOUT_APP_NAME = "InvalidJaegerTimeoutApp";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive invalidExporterApp = ShrinkWrap.create(WebArchive.class, INVALID_EXPORTER_APP_NAME + ".war")
                        .addPackage(JaxRsMisConfigEndpoints.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false\notel.traces.exporter=INVALID_EXPORTER"),
                                       "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, invalidExporterApp, SERVER_ONLY);

        WebArchive invalidJaegerEndpointApp = ShrinkWrap.create(WebArchive.class, INVALID_JAEGER_ENDPOINT_APP_NAME + ".war")
                        .addPackage(JaxRsMisConfigEndpoints.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false\notel.traces.exporter=jaeger\notel.jaeger.exporter.endpoint=INVALID_ENDPOINT"),
                                       "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, invalidJaegerEndpointApp, SERVER_ONLY);

        WebArchive invalidZipkinEndpointApp = ShrinkWrap.create(WebArchive.class, INVALID_ZIPKIN_ENDPOINT_APP_NAME + ".war")
                        .addPackage(JaxRsMisConfigEndpoints.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false\notel.traces.exporter=zipkin\notel.zipkin.exporter.endpoint=INVALID_ENDPOINT"),
                                       "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, invalidZipkinEndpointApp, SERVER_ONLY);

        WebArchive invalidOtlpEndpointApp = ShrinkWrap.create(WebArchive.class, INVALID_OTLP_ENDPOINT_APP_NAME + ".war")
                        .addPackage(JaxRsMisConfigEndpoints.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false\notel.traces.exporter=otlp\notel.otlp.exporter.endpoint=INVALID_ENDPOINT"),
                                       "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, invalidOtlpEndpointApp, SERVER_ONLY);
        //otel.exporter.jaeger.timeout

        WebArchive invalidJaegerTimeoutApp = ShrinkWrap.create(WebArchive.class, INVALID_JAEGER_TIMEOUT_APP_NAME + ".war")
                        .addPackage(JaxRsMisConfigEndpoints.class.getPackage())
                        .addAsResource(new StringAsset("otel.sdk.disabled=false\notel.traces.exporter=jaeger\notel.exporter.jaeger.timeout=INVALID_TIMEOUT"),
                                       "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, invalidJaegerTimeoutApp, SERVER_ONLY);

        server.startServer();
    }

    @Test
    @AllowedFFDC({ "org.jboss.resteasy.spi.UnhandledException" })
    public void testInvalidExporter() throws Exception {
        String createSpan = new HttpRequest(server, "/" + INVALID_EXPORTER_APP_NAME + "/misconfig/jaxrsclient")
                        .expectCode(500)
                        .run(String.class);
    }

    @Test
    @AllowedFFDC({ "org.jboss.resteasy.spi.UnhandledException" })
    public void testInvalidJaegerExporterEndpoint() throws Exception {
        HttpRequest createSpan = new HttpRequest(server, "/" + INVALID_JAEGER_ENDPOINT_APP_NAME + "/misconfig/jaxrsclient").expectCode(500);
    }

    @Test
    @AllowedFFDC({ "org.jboss.resteasy.spi.UnhandledException" })
    public void testInvalidZipkinExporterEndpoint() throws Exception {
        HttpRequest createSpan = new HttpRequest(server, "/" + INVALID_ZIPKIN_ENDPOINT_APP_NAME + "/misconfig/jaxrsclient").expectCode(500);
    }

    @Test
    @AllowedFFDC({ "org.jboss.resteasy.spi.UnhandledException" })
    public void testInvalidOtlpExporterEndpoint() throws Exception {
        HttpRequest createSpan = new HttpRequest(server, "/" + INVALID_OTLP_ENDPOINT_APP_NAME + "/misconfig/jaxrsclient").expectCode(500);
    }

    @Test
    @AllowedFFDC({ "org.jboss.resteasy.spi.UnhandledException" })
    public void testInvalidJaegerTimeoutEndpoint() throws Exception {
        HttpRequest createSpan = new HttpRequest(server, "/" + INVALID_JAEGER_TIMEOUT_APP_NAME + "/misconfig/jaxrsclient").expectCode(200);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("SRVE0777E", // Exception thrown by application class
                          "SRVE0315E: .*INVALID_EXPORTER"); // An exception occurred: ... Unrecognized value for otel.traces.exporter: INVALID_EXPORTER
    }
}
