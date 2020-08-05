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
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.AfterClass;
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
    private static final long SHORT_TIMEOUT = 3000;
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("appPrereqTestServer");

    @Rule
    public TestName testName = new TestName();

    @Override
    protected Class<?> getLogClass() {
        return AppPrereqTest.class;
    }

    @Override
    protected LibertyServer getServer() {
        return AppPrereqTest.server;
    }

    @BeforeClass
    public static void installTestFeature() throws Exception {
        server.installSystemFeature("test.app.prereq");
        server.installSystemBundle("test.app.prereq");
    }

    @AfterClass
    public static void uninstallTestFeature() throws Exception {
        server.uninstallSystemFeature("test.app.prereq");
        server.uninstallSystemBundle("test.app.prereq");
    }

    @Before
    public void setupServer() throws Exception {
        server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, SNOOP_WAR);
    }

    @After
    public void stopServer() throws Exception {
        server.stopServer();
    }

    @Test
    public void testAppWaitsForDeclaredPrereq() throws Exception {
        // The test server has a configuration that requires that the ApplicationManager waits for the test feature.
        // Start the server with the Snoop app but without the feature configured.
        server.setServerConfigurationFile("/appPrereq/declared-unsatisfied-prereq.xml");
        server.startServer(testName.getMethodName() + ".log");

        // Show that the Snoop application has not started.
        assertNotNull(server.waitForStringInLog("TE9900A"));
        assertNull(server.verifyStringNotInLogUsingMark("CWWKZ0018I.* snoop", SHORT_TIMEOUT));

        // Add the missing test feature to the server configuration.
        // This SHOULD allow the Snoop application to start.
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("/appPrereq/declared-satisfied-prereq.xml");

        // Wait for CWWKG0017I: The server configuration was successfully updated...
        assertNotNull(server.waitForStringInLogUsingMark("CWWKG0017I:"));
        // After config processing has completed, the Snoop app should start.
        assertNotNull(server.waitForStringInLogUsingMark("CWWKZ0018I.* snoop"));
    }
}

