/*******************************************************************************
 * Copyright (c) 2020,2023 IBM Corporation and others.
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
package componenttest.application.manager.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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
        startServer(ServerXml.PREREQ_CONFIG_BUT_NO_INSTANCES);
        assertSnoopNotStarted();
        changeServerConfig(ServerXml.PREREQ_CONFIG_WITH_INSTANCES);
        assertSnoopStarted();
        changeServerConfig(ServerXml.PREREQ_CONFIG_BUT_NO_INSTANCES);
        assertSnoopStopped();
        changeServerConfig(ServerXml.NO_PREREQ_CONFIG_AND_NO_PREREQ_FEATURE);
        assertSnoopStarted();
    }

    @Test
    public void testAppStartsImmediatelyWhenDeclaredPrereqIsSatisfied() throws Exception {
        startServer(ServerXml.PREREQ_CONFIG_WITH_INSTANCES);
        assertSnoopStarted();
        changeServerConfig(ServerXml.PREREQ_CONFIG_AND_NO_PREREQ_FEATURE);
        assertSnoopStopped();
    }

    enum ServerXml {
        NO_PREREQ_CONFIG_AND_NO_PREREQ_FEATURE(true),
        NO_PREREQ_CONFIG_AND_PREREQ_FEATURE(true),
        PREREQ_CONFIG_AND_NO_PREREQ_FEATURE(true),
        PREREQ_CONFIG_BUT_NO_INSTANCES(false),
        PREREQ_CONFIG_WITH_INSTANCES(true);

        ServerXml(boolean appStartExpected) {
            this.appStartExpected = appStartExpected;
        }

        final boolean appStartExpected;
        final String path = "/appPrereq/" + this + ".xml";
    }

    private void startServer(ServerXml config) throws Exception {
        server.setServerConfigurationFile(config.path);
        server.startServer(testName.getMethodName() + ".log");
        // Wait for the timed exit feature to be enabled
        // (a useful observation point we discovered)
        assertNotNull(server.waitForStringInLog("TE9900A"));
    }

    private static void changeServerConfig(ServerXml config) throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(config.path);
        // Wait for CWWKG0017I: The server configuration was successfully updated...
        assertNotNull(server.waitForStringInLogUsingMark("CWWKG0017I:"));
    }

    private void assertSnoopStarted() {
        // After config processing has completed, the Snoop app should start.
        assertNotNull(server.waitForStringInLogUsingMark("CWWKZ0018I:.* snoop"));
    }

    private void assertSnoopStopped() {
        assertNotNull(server.waitForStringInLogUsingMark("CWWKZ0009I:.* snoop"));
    }

    private void assertSnoopNotStarted() {
        // Check that the Snoop application has not started
        assertNull(server.verifyStringNotInLogUsingMark("CWWKZ0018I:.* snoop", SHORT_TIMEOUT));
    }
}
