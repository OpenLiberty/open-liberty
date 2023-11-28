/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.cdi.internal.core;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.DISABLE_VALIDATION;
import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static componenttest.rules.repeater.EERepeatActions.EE10;
import static componenttest.rules.repeater.EERepeatActions.EE11;
import static componenttest.rules.repeater.EERepeatActions.EE7;
import static componenttest.rules.repeater.EERepeatActions.EE8;
import static componenttest.rules.repeater.EERepeatActions.EE9;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.util.List;

import org.jboss.shrinkwrap.api.Archive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import io.openliberty.cdi.internal.core.events.CDIEventTest;
import io.openliberty.cdi.internal.core.injection.CDIInjectionTest;
import io.openliberty.cdi.internal.core.interceptors.CDIInterceptorTest;
import io.openliberty.cdi.internal.core.lookup.CDILookupTest;
import io.openliberty.cdi.internal.core.producers.CDIProducerTest;
import io.openliberty.cdi.internal.core.scopes.CDIScopeTest;

@SuiteClasses({ CDIScopeTest.class,
                CDIProducerTest.class,
                CDIInjectionTest.class,
                CDIInterceptorTest.class,
                CDIEventTest.class,
                CDILookupTest.class
})
@RunWith(Suite.class)
public class FATSuite {

    private static final String APP_STARTED = "CWWKZ0001";
    private static final String APP_STOPPED = "CWWKZ0009";

    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat("cdiCoreServer", EE11, EE10, EE7, EE8, EE9);

    @BeforeClass
    public static void setup() throws Exception {
        server = LibertyServerFactory.getLibertyServer("cdiCoreServer");
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    public static void deployApp(LibertyServer server, Archive<?> archive) throws Exception {
        String appName = getAppName(archive.getName());

        // Deploy the app
        server.setMarkToEndOfLog();
        ShrinkHelper.exportDropinAppToServer(server, archive, SERVER_ONLY, DISABLE_VALIDATION);

        // Wait for the app to either start or fail to start
        String logLine = server.waitForStringInLogUsingMark("CWWKZ000[1,2].*" + appName);
        // Assert that it started successfully
        assertThat("App did not start successfully", logLine, containsString(APP_STARTED));
    }

    public static void removeApp(LibertyServer server, String archiveName) throws Exception {
        String appName = getAppName(archiveName);

        // Look for app started and stopped events
        server.resetLogMarks();
        List<String> logLines = server.findStringsInLogsUsingMark("CWWKZ000[1,9].*" + appName, server.getDefaultLogFile());

        // Assume app is running if the last log line reports that it started successfully
        boolean appIsRunning = !logLines.isEmpty() && logLines.get(logLines.size() - 1).contains(APP_STARTED);

        server.setMarkToEndOfLog();
        server.deleteFileFromLibertyServerRoot("dropins/" + archiveName);
        if (appIsRunning) {
            String logLine = server.waitForStringInLogUsingMark("CWWKZ00(09|10).*" + appName);
            assertThat("App did not stop successfully", logLine, containsString(APP_STOPPED));
        }
    }

    public static String getAppName(String archiveName) {
        String appName = archiveName;
        if (appName.endsWith(".war") || appName.endsWith(".ear")) {
            appName = appName.substring(0, appName.length() - 4);
        }
        return appName;
    }

}
