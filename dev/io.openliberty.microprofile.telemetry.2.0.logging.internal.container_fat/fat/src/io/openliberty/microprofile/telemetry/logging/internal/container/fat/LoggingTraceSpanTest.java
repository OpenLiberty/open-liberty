/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.logging.internal.container.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class LoggingTraceSpanTest {

    private static Class<?> c = LoggingTraceSpanTest.class;

    @Server("TelemetryLogsTraceServer")
    public static LibertyServer server;

    private static final String[] EXPECTED_FAILURES = { "CWMOT5005W", "SRVE0315E", "SRVE0777E" };

    @ClassRule
    public static GenericContainer<?> container = new GenericContainer<>(new ImageFromDockerfile()
                    .withDockerfileFromBuilder(builder -> builder.from(TestUtils.IMAGE_NAME)
                                    .copy("/etc/otelcol-contrib/config.yaml", "/etc/otelcol-contrib/config.yaml"))
                    .withFileFromFile("/etc/otelcol-contrib/config.yaml", new File(TestUtils.PATH_TO_AUTOFVT_TESTFILES + "configMetrics.yaml"), 0644))
                    .withLogConsumer(new SimpleLogConsumer(LoggingTraceSpanTest.class, "opentelemetry-collector-contrib"))
                    .withExposedPorts(4317);

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.trustAll();
        WebArchive telemetryLogApp = ShrinkWrap
                        .create(WebArchive.class, "MpTelemetryLogApp.war")
                        .addPackage(
                                    "io.openliberty.microprofile.telemetry.logging.internal.container.fat.MpTelemetryLogApp")
                        .addAsManifestResource(new File("publish/resources/META-INF/microprofile-config.properties"),
                                               "microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, telemetryLogApp,
                                             DeployOptions.SERVER_ONLY);

        server.addEnvVar("OTEL_EXPORTER_OTLP_ENDPOINT", "http://" + container.getHost() + ":" + container.getMappedPort(4317));

        server.startServer();

        //Read to run a smarter planet
        server.waitForStringInLogUsingMark("CWWKF0011I");
        server.setMarkToEndOfLog();
    }

    /*
     * Ensures that the span/trace IDs from the app correlates to the metrics exemplar span/trace IDs
     */
    @Test
    public void testTraceSpanMetricsLogCorrelation() throws Exception {
        assertTrue("The server was not started successfully.", server.isStarted());

        TestUtils.runApp(server, "SingleLogURL");

        //Allow time for the collector to receive and bridge logs.
        TimeUnit.SECONDS.sleep(5);

        final String logs = container.getLogs();

        Log.info(c, "testTraceSpanMetricsLogCorrelation", logs);

        assertTrue("Info message log could not be found.", logs.contains("Body: Str(Single info message)"));

        //Check for "Span ID" or "Trace ID" followed by 16/32 characters which is the number of characters of span/trace IDs
        String spanID = findSpanID(logs);
        String traceID = findTraceID(logs);

        //There should be 3 occurances of span/trace IDs. The metrics exemplar, app Initialization successfull message,
        //and the "single info message" from our app.
        if (spanID != null) {
            int bridgedSpanCount = logs.split(spanID).length - 1;
            assertEquals("Span ID occurance was not correct.", bridgedSpanCount, 3);
        } else {
            fail("Span ID was not found.");
        }

        if (traceID != null) {
            int bridgedTraceCount = logs.split(traceID).length - 1;
            assertEquals("Trace ID occurance was not correct.", bridgedTraceCount, 3);
        } else {
            fail("Trace ID was not found.");
        }
    }

    //Parse the container logs for Span ID's that have 16 characters.
    public static String findSpanID(String logsInput) {
        Pattern pattern = Pattern.compile("Span ID:\\s*([a-zA-Z0-9]{16})");
        Matcher matcher = pattern.matcher(logsInput);

        if (matcher.find()) {
            return matcher.group(1);
        } else
            return null;
    }

    //Parse the container logs for Trace ID's that have 32 characters.
    public static String findTraceID(String logsInput) {
        Pattern pattern = Pattern.compile("Trace ID:\\s*([a-zA-Z0-9]{32})");
        Matcher matcher = pattern.matcher(logsInput);

        if (matcher.find()) {
            return matcher.group(1);
        } else
            return null;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        //catch if a server is still running.
        if (server != null && server.isStarted()) {
            server.stopServer(EXPECTED_FAILURES);
        }
    }

    private static String setConfig(String fileName, RemoteFile logFile, LibertyServer server) throws Exception {
        server.setMarkToEndOfLog(logFile);
        server.setServerConfigurationFile(fileName);
        return server.waitForStringInLogUsingMark("CWWKG0017I.*|CWWKG0018I.*");
    }
}
