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
import java.util.List;
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

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@MaximumJavaLevel(javaLevel = 20)
public class JULLogServletTest {

    private static Class<?> c = JULLogServletTest.class;

    @Server("TelemetryAgentJULMessages")
    public static LibertyServer server;

    private static final String[] EXPECTED_FAILURES = { "CWMOT5005W", "SRVE0315E", "SRVE0777E" };

    //TODO switch to use ghcr.io/open-telemetry/opentelemetry-collector-releases/opentelemetry-collector-contrib:0.117.0
    //TODO remove withDockerfileFromBuilder and instead create a dockerfile
    @ClassRule
    public static GenericContainer<?> container = new GenericContainer<>(new ImageFromDockerfile()
                    .withDockerfileFromBuilder(builder -> builder.from(TestUtils.IMAGE_NAME)
                                    .copy("/etc/otelcol-contrib/config.yaml", "/etc/otelcol-contrib/config.yaml"))
                    .withFileFromFile("/etc/otelcol-contrib/config.yaml", new File(TestUtils.PATH_TO_AUTOFVT_TESTFILES + "config.yaml"), 0644))
                    .withLogConsumer(new SimpleLogConsumer(JULLogServletTest.class, "opentelemetry-collector-contrib"))
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
    public void testMatchingJULMessageLogsWithContainerViaOpenTelemetryAgent() throws Exception {
        assertTrue("The server was not started successfully.", server.isStarted());

        TestUtils.isContainerStarted("LogsExporter", container);

        TimeUnit.SECONDS.sleep(5);

        final String logs = container.getLogs();

        List<String> linesMessagesLog = server.findStringsInLogs("^(?!.*scopeInfo).*\\[.*$", server.getDefaultLogFile());
        int bridgedLogsCount = logs.split("LogRecord #").length - 1;

        assertTrue("Messages.log and Telemetry console logs don't match.",
                   TestUtils.compareLogSizes("testMatchingJULMessageLogsWithContainerViaOpenTelemetryAgent", logs, linesMessagesLog.size(), bridgedLogsCount));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        //catch if a server is still running.
        if (server != null && server.isStarted()) {
            server.stopServer(EXPECTED_FAILURES);
        }
    }
}
