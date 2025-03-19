/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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
import java.util.Collections;
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

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

@RunWith(FATRunner.class)
public class LoggingServletTest {

    private static Class<?> c = LoggingServletTest.class;

    @Server("TelemetryLogsServer")
    public static LibertyServer server;

    public static final String APP_NAME = "MpTelemetryLogApp";

    public static final String SERVER_XML_MSG_SOURCES = "msgSourceServer.xml";
    public static final String SERVER_XML_TRACE_SOURCE = "traceSourceServer.xml";
    public static final String SERVER_XML_FFDC_SOURCE = "FFDCSourceServer.xml";
    public static final String SERVER_XML_AUDIT_SOURCE = "auditSourceServer.xml";

    private static final String[] EXPECTED_FAILURES = { "CWMOT5005W", "SRVE0315E", "SRVE0777E" };

    public static final int WAIT_TIMEOUT = 5; // 5 seconds

    //TODO switch to use ghcr.io/open-telemetry/opentelemetry-collector-releases/opentelemetry-collector-contrib:0.117.0
    //TODO remove withDockerfileFromBuilder and instead create a dockerfile
    @ClassRule
    public static GenericContainer<?> container = new GenericContainer<>(new ImageFromDockerfile()
                    .withDockerfileFromBuilder(builder -> builder.from(TestUtils.IMAGE_NAME)
                                    .copy("/etc/otelcol-contrib/config.yaml", "/etc/otelcol-contrib/config.yaml"))
                    .withFileFromFile("/etc/otelcol-contrib/config.yaml", new File(TestUtils.PATH_TO_AUTOFVT_TESTFILES + "config.yaml"), 0644))
                    .withLogConsumer(new SimpleLogConsumer(LoggingServletTest.class, "opentelemetry-collector-contrib"))
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

        server.startServer();

        //Read to run a smarter planet
        server.waitForStringInLogUsingMark("CWWKF0011I");
        server.setMarkToEndOfLog();
    }

    /*
     * Ensures that an info message from a Liberty application are bridged over to the otlp container.
     */
    @Test
    public void testMessageLogs() throws Exception {
        assertTrue("The server was not started successfully.", server.isStarted());

        TestUtils.isContainerStarted("LogsExporter", container);

        RemoteFile messageLogFile = server.getDefaultLogFile();
        setConfig(SERVER_XML_MSG_SOURCES, messageLogFile, server);

        TestUtils.runApp(server, "logs");

        //Allow time for the collector to receive and bridge logs.
        TimeUnit.SECONDS.sleep(WAIT_TIMEOUT);

        final String logs = container.getLogs();

        assertTrue("Info message log could not be found.", TestUtils.assertLogContains("testMessageLogs", logs, "Body: Str(info message)"));
        assertTrue("Extension appName could not be found.", TestUtils.assertLogContains("testMessageLogs", logs, "io.openliberty.ext.app_name: Str(MpTelemetryLogApp)"));
        assertTrue("Module could not be found.",
                   TestUtils.assertLogContains("testMessageLogs", logs,
                                               "io.openliberty.module: Str(io.openliberty.microprofile.telemetry.logging.internal.container.fat.MpTelemetryLogApp.MpTelemetryServlet)"));
        assertTrue("SeverityText message could not be found.", TestUtils.assertLogContains("testMessageLogs", logs, "SeverityText: I"));
        assertTrue("SeverityNumber message could not be found.", TestUtils.assertLogContains("testMessageLogs", logs, "SeverityNumber: Info"));
        assertTrue("Squence message could not be found.", TestUtils.assertLogContains("testMessageLogs", logs, "io.openliberty.sequence: Str"));
        assertTrue("Log type message could not be found.", TestUtils.assertLogContains("testMessageLogs", logs, "io.openliberty.type: Str(liberty_message)"));
        assertTrue("Thread ID message could not be found.", TestUtils.assertLogContains("testMessageLogs", logs, "thread.id: Int"));
    }

    /*
     * Ensures that a trace message from a Liberty application are bridged over to the otlp container.
     */
    @Test
    public void testTraceLogs() throws Exception {

        assertTrue("The server was not started successfully.", server.isStarted());

        TestUtils.isContainerStarted("LogsExporter", container);

        RemoteFile messageLogFile = server.getDefaultLogFile();
        setConfig(SERVER_XML_TRACE_SOURCE, messageLogFile, server);

        TestUtils.runApp(server, "logs");

        //Allow time for the collector to receive and bridge logs.
        TimeUnit.SECONDS.sleep(WAIT_TIMEOUT);

        final String logs = container.getLogs();

        assertTrue("Trace message log could not be found.", TestUtils.assertLogContains("testTraceLogs", logs, "Body: Str(finest trace)"));
        assertTrue("Extension appName could not be found", TestUtils.assertLogContains("testTraceLogs", logs, "io.openliberty.ext.app_name: Str(MpTelemetryLogApp)"));
        assertTrue("Module could not be found.",
                   TestUtils.assertLogContains("testTraceLogs", logs,
                                               "io.openliberty.module: Str(io.openliberty.microprofile.telemetry.logging.internal.container.fat.MpTelemetryLogApp.MpTelemetryServlet)"));
        assertTrue("SeverityText message could not be found.", TestUtils.assertLogContains("testTraceLogs", logs, "SeverityText: 3"));
        assertTrue("SeverityNumber message could not be found.", TestUtils.assertLogContains("testTraceLogs", logs, "SeverityNumber: Trace(1)"));
        assertTrue("Sequence message could not be found.", TestUtils.assertLogContains("testTraceLogs", logs, "io.openliberty.sequence: Str"));
        assertTrue("Log type message could not be found.", TestUtils.assertLogContains("testTraceLogs", logs, "io.openliberty.type: Str(liberty_trace)"));
        assertTrue("Thread ID message could not be found.", TestUtils.assertLogContains("testTraceLogs", logs, "thread.id: Int"));
    }

    /*
     * Ensures that an FFDC message from a Liberty application are bridged over to the otlp container.
     */
    @Test
    @ExpectedFFDC({ "java.lang.ArithmeticException" })
    public void testFFDCLogs() throws Exception {

        assertTrue("The server was not started successfully.", server.isStarted());

        TestUtils.isContainerStarted("LogsExporter", container);

        RemoteFile messageLogFile = server.getDefaultLogFile();
        setConfig(SERVER_XML_FFDC_SOURCE, messageLogFile, server);

        TestUtils.runApp(server, "ffdc1");

        //Allow time for the collector to receive and bridge logs.
        TimeUnit.SECONDS.sleep(WAIT_TIMEOUT);

        final String logs = container.getLogs();

        assertTrue("FFDC message log could not be found.", TestUtils.assertLogContains("testFFDCLogs", logs, "Body: Str(FFDC_TEST_DOGET"));
        assertTrue("Exception message could not be found.", TestUtils.assertLogContains("testFFDCLogs", logs, "exception.message: Str(FFDC_TEST_DOGET"));
        assertTrue("Exception Stacktrace  could not be found.", TestUtils.assertLogContains("testFFDCLogs", logs, "exception.stacktrace: Str(java.lang.ArithmeticException"));
        assertTrue("Exception type could not be found.", TestUtils.assertLogContains("testFFDCLogs", logs, "exception.type: Str(java.lang.ArithmeticException)"));
        assertTrue("Probe ID could not be found.", TestUtils.assertLogContains("testFFDCLogs", logs, "io.openliberty.probe_id"));
        assertTrue("SeverityText message could not be found.", TestUtils.assertLogContains("testFFDCLogs", logs, "SeverityText:"));
        assertTrue("SeverityNumber message could not be found.", TestUtils.assertLogContains("testFFDCLogs", logs, "SeverityNumber: Warn(13)"));
        assertTrue("Sequence message could not be found.", TestUtils.assertLogContains("testFFDCLogs", logs, "io.openliberty.sequence: Str"));
        assertTrue("Log type message could not be found.", TestUtils.assertLogContains("testFFDCLogs", logs, "io.openliberty.type: Str(liberty_ffdc)"));
        assertTrue("Thread ID message could not be found.", TestUtils.assertLogContains("testFFDCLogs", logs, "thread.id: Int"));

        //These older repeats cause the class name and object details to display different class names.
        if (TelemetryActions.mpTelemetry20BelowEE10IsActive()) {
            assertTrue("Class name could not be found.", TestUtils.assertLogContains("testFFDCLogs", logs, "io.openliberty.class_name"));
            assertTrue("Object details could not be found.", TestUtils.assertLogContains("testFFDCLogs", logs, "io.openliberty.object_details"));
        } else {
            assertTrue("Class name could not be found.",
                       TestUtils.assertLogContains("testFFDCLogs", logs, "io.openliberty.class_name: Str(com.ibm.ws.webcontainer.filter.WebAppFilterManager.invokeFilters)"));
            assertTrue("Object details could not be found.",
                       TestUtils.assertLogContains("testFFDCLogs", logs,
                                                   "io.openliberty.object_details: Str(Object type = com.ibm.ws.webcontainer.osgi.filter.WebAppFilterManagerImpl"));
        }
    }

    /*
     * Ensures that audit events generated by a Liberty application are bridged over to the OTLP container.
     */
    @Test
    public void testAuditEventLogs() throws Exception {
        assertTrue("The server was not started successfully.", server.isStarted());

        TestUtils.isContainerStarted("LogsExporter", container);

        RemoteFile messageLogFile = server.getDefaultLogFile();
        setConfig(SERVER_XML_AUDIT_SOURCE, messageLogFile, server);

        // Hit the application to trigger an audit event.
        TestUtils.runApp(server, "logs");

        //Allow time for the collector to receive and bridge logs.
        TimeUnit.SECONDS.sleep(WAIT_TIMEOUT);

        final String logs = container.getLogs();

        // Verify audit event attributes generated by an application's audit event.
        assertTrue("Audit type message could not be found.", TestUtils.assertLogContains("testAuditEventLogs", logs, "io.openliberty.type: Str(liberty_audit)"));
        assertTrue("The Audit Event can not be found in the body.", TestUtils.assertLogContains("testAuditEventLogs", logs, "Body: Str(SECURITY_AUTHN)"));
        assertTrue("The Audit event name attribute can not be found.",
                   TestUtils.assertLogContains("testAuditEventLogs", logs, "io.openliberty.audit.event_name: Str(SECURITY_AUTHN)"));
        assertTrue("The Audit observer name attribute can not be found.",
                   TestUtils.assertLogContains("testAuditEventLogs", logs, "io.openliberty.audit.observer.name: Str(SecurityService)"));
        assertTrue("The Audit observer type URI attribute can not be found.",
                   TestUtils.assertLogContains("testAuditEventLogs", logs, "io.openliberty.audit.observer.type_uri: Str(service/server)"));
        assertTrue("The Audit outcome attribute can not be found.", TestUtils.assertLogContains("testAuditEventLogs", logs, "io.openliberty.audit.outcome: Str(success)"));
        assertTrue("The Audit reason code attribute can not be found.",
                   TestUtils.assertLogContains("testAuditEventLogs", logs, "io.openliberty.audit.reason.reason_code: Str(200)"));
        assertTrue("The Audit reason type attribute can not be found.",
                   TestUtils.assertLogContains("testAuditEventLogs", logs, "io.openliberty.audit.reason.reason_type: Str(HTTP)"));
        assertTrue("The Audit target app name attribute can not be found.", TestUtils
                        .assertLogContains("testAuditEventLogs", logs,
                                           "io.openliberty.audit.target.appname: Str(io.openliberty.microprofile.telemetry.logging.internal.container.fat.MpTelemetryLogApp.LogServlet)"));
        assertTrue("The Audit target method attribute can not be found.", TestUtils.assertLogContains("testAuditEventLogs", logs, "io.openliberty.audit.target.method: Str(GET)"));
        assertTrue("The Audit target name attribute can not be found.",
                   TestUtils.assertLogContains("testAuditEventLogs", logs, "io.openliberty.audit.target.name: Str(/MpTelemetryLogApp/LogURL)"));
        assertTrue("The Audit target realm attribute can not be found.",
                   TestUtils.assertLogContains("testAuditEventLogs", logs, "io.openliberty.audit.target.realm: Str(defaultRealm)"));
        assertTrue("The Audit target type URI attribute can not be found.",
                   TestUtils.assertLogContains("testAuditEventLogs", logs, "io.openliberty.audit.target.type_uri: Str(service/application/web)"));

        // Verify common Logging attributes
        assertTrue("SeverityNumber message could not be found.", TestUtils.assertLogContains("testAuditEventLogs", logs, "SeverityNumber: Info2(10)"));
        assertTrue("Sequence message could not be found.", TestUtils.assertLogContains("testAuditEventLogs", logs, "io.openliberty.sequence: Str"));
        assertTrue("Thread ID message could not be found.", TestUtils.assertLogContains("testAuditEventLogs", logs, "thread.id: Int"));

    }

    @AfterClass
    public static void tearDown() throws Exception {
        //catch if a server is still running.
        if (server != null && server.isStarted()) {
            server.stopServer(EXPECTED_FAILURES);
        }
    }

    private static void setConfig(String fileName, RemoteFile logFile, LibertyServer server) throws Exception {
        server.setMarkToEndOfLog(logFile);
        server.setServerConfigurationFile(fileName);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), new String[] {});
    }

}
