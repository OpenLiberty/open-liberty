/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.timer.persistent.fat.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test ejbPersistentTimer feature when a datasource tables have not been created.
 */
@RunWith(FATRunner.class)
public class NoTablesTest extends FATServletClient {

    private static final String CNTR0218E = "CNTR0218E.* " + "NoDBPersistAutoTimerEJB.jar";
    private static final String CNTR4002E = "CNTR4002E.* " + "NoDBPersistAutoTimerEJB.jar.* " + "NoDBPersistAutoTimerApp";
    private static final String CWWKZ0106E = "CWWKZ0106E.* " + "NoDBPersistAutoTimerApp";
    private static final String CWWKZ0002E = "CWWKZ0002E.* " + "NoDBPersistAutoTimerApp";

    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");

    @Server("com.ibm.ws.ejbcontainer.timer.persistent.fat.NoTablesServer")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = isWindows //
                    ? RepeatTests.with(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.timer.persistent.fat.NoTablesServer")).andWith(FeatureReplacementAction.EE9_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.timer.persistent.fat.NoTablesServer")).andWith(FeatureReplacementAction.EE10_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.timer.persistent.fat.NoTablesServer")) //
                    : RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.timer.persistent.fat.NoTablesServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.timer.persistent.fat.NoTablesServer")).andWith(FeatureReplacementAction.EE9_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.timer.persistent.fat.NoTablesServer")).andWith(FeatureReplacementAction.EE10_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.timer.persistent.fat.NoTablesServer"));

    @After
    public void cleanUp() throws Exception {
        // clean up just in case the test fails and doesn't complete server stop.
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    protected void runTest(String servlet, String testName) throws Exception {
        FATServletClient.runTest(server, servlet, testName);
    }

    /**
     * Test that an application with persistent automatic EJB timers will not start with
     * appropriate errors in the logs when the ejbPersistentTimer feature is enabled, but
     * no tables have been created in the datasource.
     */
    @Test
    @ExpectedFFDC({ "javax.persistence.PersistenceException", "com.ibm.ws.exception.RuntimeWarning", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testNoTablesPersistentAutoTimer() throws Exception {
        // Start the server with no applications
        server.startServer();
        server.setMarkToEndOfLog();

        // Use ShrinkHelper to build the ear and export to server dropins
        JavaArchive NoDBPersistAutoTimerEJB = ShrinkHelper.buildJavaArchive("NoDBPersistAutoTimerEJB.jar", "com.ibm.ws.ejbcontainer.timer.nodb.pauto.ejb.");
        EnterpriseArchive NoDBPersistAutoTimerApp = ShrinkWrap.create(EnterpriseArchive.class, "NoDBPersistAutoTimerApp.ear");
        NoDBPersistAutoTimerApp.addAsModule(NoDBPersistAutoTimerEJB);
        ShrinkHelper.exportToServer(server, "dropins", NoDBPersistAutoTimerApp, DeployOptions.SERVER_ONLY);

        // Verify the application failed to start with correct messages
        assertNotNull(CNTR0218E, server.waitForStringInLogUsingMark(CNTR0218E)); // persistent timers not supported
        assertNotNull(CNTR4002E, server.waitForStringInLogUsingMark(CNTR4002E)); // EJB module failed to start
        assertNotNull(CWWKZ0106E, server.waitForStringInLogUsingMark(CWWKZ0106E)); // App failed to start
        assertNotNull(CWWKZ0002E, server.waitForStringInLogUsingMark(CWWKZ0002E)); // Exception occurred starting app

        // Verify an info message did NOT occur that persistent timers not configured
        assertNull("CNTR4021I", server.verifyStringNotInLogUsingMark("CNTR4021I", 0));

        // Stop the server and remove the application from dropins
        server.stopServer(CNTR0218E, CNTR4002E, CWWKZ0106E, CWWKZ0002E);
        assertTrue("NoDBPersistAutoTimerApp.ear not removed", server.removeDropinsApplications("NoDBPersistAutoTimerApp.ear"));
    }

}
