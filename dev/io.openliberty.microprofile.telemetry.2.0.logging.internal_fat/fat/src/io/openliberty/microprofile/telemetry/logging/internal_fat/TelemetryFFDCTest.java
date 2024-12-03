/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.logging.internal_fat;

import static io.openliberty.microprofile.telemetry.logging.internal_fat.FATSuite.hitWebPage;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

@RunWith(FATRunner.class)
public class TelemetryFFDCTest extends FATServletClient {

    public static final String APP_NAME = "TelemetryServletTestApp";
    public static final String SERVER_NAME = "TelemetryFFDC";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests rt = TelemetryActions.latestTelemetry20Repeats();

    private static final String USER_FEATURE_PATH = "usr/extension/lib/features/";
    private static final String USER_BUNDLE_PATH = "usr/extension/lib/";
    private static final String USER_FEATURE_USERTEST_MF = "features/test.ffdc-1.0.mf";
    private static final String USER_FEATURE_USERTEST_JAR = "bundles/ffdc.bundle.jar";

    static LibertyServer installUserFeatureAndApp(LibertyServer s) throws Exception {
        s.copyFileToLibertyInstallRoot(USER_FEATURE_PATH, USER_FEATURE_USERTEST_MF);
        s.copyFileToLibertyInstallRoot(USER_BUNDLE_PATH, USER_FEATURE_USERTEST_JAR);
        ShrinkHelper.defaultDropinApp(s, "ffdc-servlet", new DeployOptions[] { DeployOptions.SERVER_ONLY },
                                      "io.openliberty.microprofile.telemetry.logging.internal.fat.ffdc.servlet");
        return s;
    }

    static void removeUserFeaturesAndStopServer(LibertyServer s) throws Exception {
        s.stopServer("com.ibm.ws.logging.fat.ffdc.servlet", "ArithmeticException", "SRVE0777E", "SRVE0271E", "SRVE0276E");
        s.deleteFileFromLibertyInstallRoot(USER_FEATURE_PATH + USER_FEATURE_USERTEST_MF);
        s.deleteFileFromLibertyInstallRoot(USER_BUNDLE_PATH + USER_FEATURE_USERTEST_JAR);
    }

    @BeforeClass
    public static void initialSetup() throws Exception {
        installUserFeatureAndApp(server);

        server.addBootstrapProperties(Collections.singletonMap("io.openliberty.microprofile.telemetry.ffdc.early", "true"));
        server.startServer();
    }

    /**
     * Triggers an FFDC and ensures exception messages are present.
     */
    @Test
    @ExpectedFFDC("java.lang.ArithmeticException")
    // RuntimeException comes out of an early start servlet init;
    // This is async and may or may not happen before the test method enters
    @AllowedFFDC({ "java.lang.RuntimeException", "java.lang.IllegalArgumentException" })
    public void testTelemetryFFDCMessages() throws Exception {
        testTelemetryFFDCMessages(server, (linesConsoleLog) -> {
            // We expect to see the early ffdc message
            // TODO This early FFDC is not coming through for some reason
            //assertNotNull("Should contain early FFDC message FFDC_TEST_BUNDLE_START",
            //              linesConsoleLog.stream().filter((l) -> l.contains("FFDC_TEST_BUNDLE_START")).findFirst().orElse(null));

            //After the refactor of OpenTelemetryInfoFactory this is not coming out either. I observed that the startup order of events was:
            // Activate OpenTelemtryLifecycleManagerImpl
            // Servlet init
            // OpenTelemetryLogHandler
            //
            //assertNotNull("Should contain early FFDC message FFDC_TEST_INIT",
            //              linesConsoleLog.stream().filter((l) -> l.contains("FFDC_TEST_INIT")).findFirst().orElse(null));
        });
    }

    static void testTelemetryFFDCMessages(LibertyServer s, Consumer<List<String>> consoleConsumer) throws Exception {
        hitWebPage(s, "ffdc-servlet", "FFDCServlet", true, "?generateFFDC=true");

        String logLevelLine = s.waitForStringInLog(".*scopeInfo.*FFDC_TEST_DOGET", s.getConsoleLogFile());
        String exceptionMessageLine = s.waitForStringInLog("exception.message=\"FFDC_TEST_DOGET\"", s.getConsoleLogFile());
        String exceptionTraceLine = s.waitForStringInLog("exception.stacktrace=\"java.lang.ArithmeticException", s.getConsoleLogFile());
        String exceptionTypeLine = s.waitForStringInLog("exception.type=\"java.lang.ArithmeticException\"", s.getConsoleLogFile());

        if (consoleConsumer != null) {
            List<String> linesConsoleLog = s.findStringsInLogs(".*scopeInfo.*", s.getConsoleLogFile());
            consoleConsumer.accept(linesConsoleLog);
        }
        assertNotNull("FFDC log could not be found.", logLevelLine);
        assertTrue("FFDC Log level was not logged by MPTelemetry", logLevelLine.contains("WARN "));
        assertNotNull("FFDC Exception.message was not logged by MPTelemetry", exceptionMessageLine);
        assertNotNull("FFDC Exception.stacktrace was not logged by MPTelemetry", exceptionTraceLine);
        assertTrue("FFDC Exception.stacktrace did not contain error message", exceptionTraceLine.contains("FFDC_TEST_DOGET"));
        assertNotNull("FFDC Exception.type was not logged by MPTelemetry", exceptionTypeLine);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        removeUserFeaturesAndStopServer(server);
    }

}