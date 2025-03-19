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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.TimeUnit;

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

import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@MaximumJavaLevel(javaLevel = 20)
public class JULDuplicateTest {

    private static Class<?> c = JULDuplicateTest.class;

    @Server("TelemetryAgentJULMessages")
    public static LibertyServer server;

    public static final String SERVER_XML_ALL_SOURCES = "allJULSources.xml";

    private static final String[] EXPECTED_FAILURES = { "CWMOT5005W", "SRVE0315E", "SRVE0777E" };

    //TODO switch to use ghcr.io/open-telemetry/opentelemetry-collector-releases/opentelemetry-collector-contrib:0.117.0
    //TODO remove withDockerfileFromBuilder and instead create a dockerfile
    @ClassRule
    public static GenericContainer<?> container = new GenericContainer<>(new ImageFromDockerfile()
                    .withDockerfileFromBuilder(builder -> builder.from(TestUtils.IMAGE_NAME)
                                    .copy("/etc/otelcol-contrib/config.yaml", "/etc/otelcol-contrib/config.yaml"))
                    .withFileFromFile("/etc/otelcol-contrib/config.yaml", new File(TestUtils.PATH_TO_AUTOFVT_TESTFILES + "config.yaml"), 0644))
                    .withLogConsumer(new SimpleLogConsumer(JULDuplicateTest.class, "opentelemetry-collector-contrib"))
                    .withExposedPorts(4317, 4318);

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

        server.addEnvVar("OTEL_EXPORTER_OTLP_PROTOCOL", "http/protobuf");
        server.addEnvVar("OTEL_EXPORTER_OTLP_ENDPOINT", "http://" + container.getHost() + ":" + container.getMappedPort(4318));

        server.copyFileToLibertyServerRoot("agent-260/opentelemetry-javaagent.jar");
        server.startServer();

        //Read to run a smarter planet
        server.waitForStringInLogUsingMark("CWWKF0011I");
        server.setMarkToEndOfLog();
    }

    /**
     * Ensure JUL message logs are not duplicated when the OpenTelemetry Java agent is active with mpTelemetry-2.0.
     */
    @Test
    public void testNoDuplicateJULMessageLogsWithOpenTelemetryAgent() throws Exception {
        // Wait for the JUL message (SRVE0250I) to appear in the logs.
        server.waitForStringInLog("SRVE0250I");

        // Wait for the second message (SESN0176I) to arrive, to ensure all the occurrences of the first message (SRVE0250I) has arrived.
        server.waitForStringInLog("SESN0176I");

        TestUtils.isContainerStarted("LogsExporter", container);

        RemoteFile messageLogFile = server.getDefaultLogFile();
        setConfig(SERVER_XML_ALL_SOURCES, messageLogFile, server);

        TestUtils.runApp(server, "logs");

        //Allow time for the collector to receive and bridge logs.
        TimeUnit.SECONDS.sleep(5);

        final String logs = container.getLogs();

        String[] agentMappedJulMsg = logs.split("SRVE0250I");

        // There should only be one instance of the JUL message routed to OpenTelemetry.
        assertTrue("There are duplicate JUL messages.",
                   TestUtils.compareLogSizes("testNoDuplicateJULMessageLogsWithOpenTelemetryAgent", logs, (agentMappedJulMsg.length - 1), 1));

        String[] agentMappedJulAppMsg = logs.split("info message");

        // There should only be one instance of the JUL App message routed to OpenTelemetry.
        assertTrue("There are duplicate JUL App messages.",
                   TestUtils.compareLogSizes("testNoDuplicateJULMessageLogsWithOpenTelemetryAgent", logs, (agentMappedJulAppMsg.length - 1), 1));

        String[] agentMappedJulAppTrace = logs.split("finest trace");

        // There should only be one instance of the JUL Trace message routed to OpenTelemetry.
        assertTrue("There are duplicate JUL Trace messages.",
                   TestUtils.compareLogSizes("testNoDuplicateJULMessageLogsWithOpenTelemetryAgent", logs, (agentMappedJulAppTrace.length - 1), 1));
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
