/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.application.manager.test;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test that shows the configured AppPrereq delays app startup until the prerequisites are satisfied.
 */
@RunWith(FATRunner.class)
public class AppPrereqTest extends AbstractAppManagerTest {

    private static final long LONG_TIMEOUT = 120000;

    private static final long SHORT_TIMEOUT = 10000;

    private final Class<?> c = AppPrereqTest.class;

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("appPrereqTestServer");

    @Rule
    public TestName testName = new TestName();

    @Override
    protected Class<?> getLogClass() {
        return c;
    }

    @Override
    protected LibertyServer getServer() {
        return AppPrereqTest.server;
    }

    @Test
    public void testAppPrereq() throws Exception {
        final String method = testName.getMethodName();

        try {
            // Install test feature and bundle.
            server.installSystemFeature("test.app.prereq");
            server.installSystemBundle("test.app.prereq");
            server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, SNOOP_WAR);

            // The test server has a configuration that requires that the ApplicationManager waits for the test feature.
            // Start the server with the Snoop app but without the feature configured.
            server.startServer(method + ".log");

            // Show that the Snoop application has not started.
            server.verifyStringNotInLogUsingMark("CWWKZ0001I.* snoop", SHORT_TIMEOUT);

            // Add the missing test feature to the server configuration, this will allow the Snoop application to start.
            server.setMarkToEndOfLog();
            server.changeFeatures(Arrays.asList("servlet-4.0", "test.app.prereq"));

            // Wait for CWWKG0017I: The server configuration was successfully updated...
            server.waitForStringInLogUsingMark("CWWKG0017I:");
            // After config procesing has completed, the Snoop should start.
            assertNotNull(server.waitForStringInLogUsingMark("CWWKZ0001I.* snoop"));

        } finally {
            server.stopServer("CWWKZ0005E");
            server.uninstallSystemFeature("test.app.prereq");
            server.uninstallSystemBundle("test.app.prereq");
        }

    }

}
