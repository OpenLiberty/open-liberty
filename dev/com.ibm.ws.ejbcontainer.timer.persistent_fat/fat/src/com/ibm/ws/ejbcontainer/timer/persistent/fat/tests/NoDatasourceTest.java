/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.timer.persistent.fat.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test ejbPersistentTimer feature when a datasource has not been configured.
 */
@RunWith(FATRunner.class)
public class NoDatasourceTest extends FATServletClient {

    private static final String START_NON_PERSIST_AUTO = "CWWKZ0001I.* " + "NoDBNonPersistAutoTimerApp";
    private static final String START_PROGRAMMATIC_AUTO = "CWWKZ0001I.* " + "NoDBProgrammaticTimerApp";

    private static final String CNTR4020E = "CNTR4020E.* " + "PAutoTimerBean.* " + "NoDBPersistAutoTimerEJB.jar.* " + "NoDBPersistAutoTimerApp";
    private static final String CNTR4002E = "CNTR4002E.* " + "NoDBPersistAutoTimerEJB.jar.* " + "NoDBPersistAutoTimerApp";
    private static final String CWWKZ0106E = "CWWKZ0106E.* " + "NoDBPersistAutoTimerApp";
    private static final String CWWKZ0002E = "CWWKZ0002E.* " + "NoDBPersistAutoTimerApp";

    @Server("com.ibm.ws.ejbcontainer.timer.persistent.fat.NoDatasourceServer")
    public static LibertyServer server;

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
     * Test that non-persistent automatic EJB timers function properly without errors
     * or warnings in the logs when the ejbPersistentTimer feature is enabled, but
     * no datasource has been configured.
     */
    @Test
    public void testNoDatasourceNonPersistentAutoTimer() throws Exception {
        // Start the server with no applications
        server.startServer();
        server.setMarkToEndOfLog();

        // Use ShrinkHelper to build the ear and export to server dropins
        JavaArchive NoDBNonPersistAutoTimerEJB = ShrinkHelper.buildJavaArchive("NoDBNonPersistAutoTimerEJB.jar", "com.ibm.ws.ejbcontainer.timer.nodb.npauto.ejb.");
        WebArchive NoDBNonPersistAutoTimerWeb = ShrinkHelper.buildDefaultApp("NoDBNonPersistAutoTimerWeb.war", "com.ibm.ws.ejbcontainer.timer.nodb.npauto.web.");
        EnterpriseArchive NoDBNonPersistAutoTimerApp = ShrinkWrap.create(EnterpriseArchive.class, "NoDBNonPersistAutoTimerApp.ear");
        NoDBNonPersistAutoTimerApp.addAsModule(NoDBNonPersistAutoTimerEJB).addAsModule(NoDBNonPersistAutoTimerWeb);
        ShrinkHelper.exportToServer(server, "dropins", NoDBNonPersistAutoTimerApp);

        // Wait for the application to start
        assertNotNull("START_NON_PERSIST_AUTO", server.waitForStringInLogUsingMark(START_NON_PERSIST_AUTO));

        // Verify the application with non-persistent timers works just fine
        runTest("NoDBNonPersistAutoTimerWeb/NoDBNonPersistAutoTimerServlet", "testNoDatasourceNonPersistentAutoTimer");

        // Verify an info message did NOT occur that persistent timers not configured
        assertNull("CNTR4021I", server.verifyStringNotInLogUsingMark("CNTR4021I", 0));

        // Stop the server and remove the application from dropins
        server.stopServer();
        assertTrue("NoDBNonPersistAutoTimerApp.ear not removed", server.removeDropinsApplications("NoDBNonPersistAutoTimerApp.ear"));
    }

    /**
     * Test that an application with persistent automatic EJB timers will not start with
     * appropriate errors in the logs when the ejbPersistentTimer feature is enabled, but
     * no datasource has been configured.
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testNoDatasourcePersistentAutoTimer() throws Exception {
        // Start the server with no applications
        server.startServer();
        server.setMarkToEndOfLog();

        // Use ShrinkHelper to build the ear and export to server dropins
        JavaArchive NoDBPersistAutoTimerEJB = ShrinkHelper.buildJavaArchive("NoDBPersistAutoTimerEJB.jar", "com.ibm.ws.ejbcontainer.timer.nodb.pauto.ejb.");
        EnterpriseArchive NoDBPersistAutoTimerApp = ShrinkWrap.create(EnterpriseArchive.class, "NoDBPersistAutoTimerApp.ear");
        NoDBPersistAutoTimerApp.addAsModule(NoDBPersistAutoTimerEJB);
        ShrinkHelper.exportToServer(server, "dropins", NoDBPersistAutoTimerApp);

        // Verify the application failed to start with correct messages
        assertNotNull(CNTR4020E, server.waitForStringInLogUsingMark(CNTR4020E)); // persistent timers not supported
        assertNotNull(CNTR4002E, server.waitForStringInLogUsingMark(CNTR4002E)); // EJB module failed to start
        assertNotNull(CWWKZ0106E, server.waitForStringInLogUsingMark(CWWKZ0106E)); // App failed to start
        assertNotNull(CWWKZ0002E, server.waitForStringInLogUsingMark(CWWKZ0002E)); // Exception occurred starting app

        // Verify an info message did NOT occur that persistent timers not configured
        assertNull("CNTR4021I", server.verifyStringNotInLogUsingMark("CNTR4021I", 0));

        // Stop the server and remove the application from dropins
        server.stopServer(CNTR4020E, CNTR4002E, CWWKZ0106E, CWWKZ0002E);
        assertTrue("NoDBPersistAutoTimerApp.ear not removed", server.removeDropinsApplications("NoDBPersistAutoTimerApp.ear"));
    }

    /**
     * Test that non-persistent programmatic EJB timers function properly without errors
     * or warnings in the logs when the ejbPersistentTimer feature is enabled, but
     * no datasource has been configured, and that an attempt to create a persistent timer
     * will fail with an appropriate exception. The test verifies that an informational
     * message does occur when the application starts, indicating that the persistent
     * timer feature has been enabled but not configured
     */
    @Test
    public void testNoDatasourceProgrammaticTimer() throws Exception {
        // Start the server with no applications
        server.startServer();
        server.setMarkToEndOfLog();

        // Use ShrinkHelper to build the ear and export to server dropins
        JavaArchive NoDBProgrammaticTimerEJB = ShrinkHelper.buildJavaArchive("NoDBProgrammaticTimerEJB.jar", "com.ibm.ws.ejbcontainer.timer.nodb.programmatic.ejb.");
        WebArchive NoDBProgrammaticTimerWeb = ShrinkHelper.buildDefaultApp("NoDBProgrammaticTimerWeb.war", "com.ibm.ws.ejbcontainer.timer.nodb.programmatic.web.");
        EnterpriseArchive NoDBProgrammaticTimerApp = ShrinkWrap.create(EnterpriseArchive.class, "NoDBProgrammaticTimerApp.ear");
        NoDBProgrammaticTimerApp.addAsModule(NoDBProgrammaticTimerEJB).addAsModule(NoDBProgrammaticTimerWeb);
        ShrinkHelper.exportToServer(server, "dropins", NoDBProgrammaticTimerApp);

        // Wait for the application to start
        assertNotNull(START_PROGRAMMATIC_AUTO, server.waitForStringInLogUsingMark(START_PROGRAMMATIC_AUTO));

        // Verify the application with programmatic non-persistent timers works just fine
        runTest("NoDBProgrammaticTimerWeb/NoDBProgrammaticTimerServlet", "testNoDatasourceProgrammaticTimerNonPersistent");

        // Verify an info message occurred that persistent timers not configured
        assertNotNull("CNTR4021I", server.waitForStringInLogUsingMark("CNTR4021I"));

        // Verify that a failure occurs if a persistent timer create is attempted
        runTest("NoDBProgrammaticTimerWeb/NoDBProgrammaticTimerServlet", "testNoDatasourceProgrammaticTimerPersistent");

        // Stop the server and remove the application from dropins
        server.stopServer();
        assertTrue("NoDBProgrammaticTimerApp.ear not removed", server.removeDropinsApplications("NoDBProgrammaticTimerApp.ear"));
    }

}
