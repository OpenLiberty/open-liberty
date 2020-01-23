/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
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

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.EJBContainerElement;
import com.ibm.websphere.simplicity.config.EJBTimerServiceElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.ejbcontainer.timer.persistent.core.web.TimerAccessOperationsServlet;
import com.ibm.ws.ejbcontainer.timer.persistent.core.web.TimerSFOperationsServlet;
import com.ibm.ws.ejbcontainer.timer.persistent.core.web.TimerSLOperationsServlet;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 8)
public class PersistentTimerCoreTest extends FATServletClient {

    public static final String WAR_NAME = "PersistentTimerCoreWeb";

    @Server("com.ibm.ws.ejbcontainer.timer.persistent.fat.PersistentTimerServer")
    @TestServlets({ @TestServlet(servlet = TimerAccessOperationsServlet.class, contextRoot = WAR_NAME),
                    @TestServlet(servlet = TimerSFOperationsServlet.class, contextRoot = WAR_NAME),
                    @TestServlet(servlet = TimerSLOperationsServlet.class, contextRoot = WAR_NAME) })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.timer.persistent.fat.PersistentTimerServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.timer.persistent.fat.PersistentTimerServer"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### InitTxRecoveryLogApp.ear (Automatically initializes transaction recovery logs)
        JavaArchive InitTxRecoveryLogEJBJar = ShrinkHelper.buildJavaArchive("InitTxRecoveryLogEJB.jar", "com.ibm.ws.ejbcontainer.init.recovery.ejb.");

        EnterpriseArchive InitTxRecoveryLogApp = ShrinkWrap.create(EnterpriseArchive.class, "InitTxRecoveryLogApp.ear");
        InitTxRecoveryLogApp.addAsModule(InitTxRecoveryLogEJBJar);

        ShrinkHelper.exportDropinAppToServer(server, InitTxRecoveryLogApp);

        //#################### PersistentTimerCoreApp.ear
        JavaArchive PersistentTimerCoreEJB = ShrinkHelper.buildJavaArchive("PersistentTimerCoreEJB.jar", "com.ibm.ws.ejbcontainer.timer.persistent.core.ejb.");
        PersistentTimerCoreEJB = (JavaArchive) ShrinkHelper.addDirectory(PersistentTimerCoreEJB, "test-applications/PersistentTimerCoreEJB.jar/resources");
        WebArchive PersistentTimerCoreWeb = ShrinkHelper.buildDefaultApp("PersistentTimerCoreWeb.war", "com.ibm.ws.ejbcontainer.timer.persistent.core.web.");

        EnterpriseArchive PersistentTimerCoreApp = ShrinkWrap.create(EnterpriseArchive.class, "PersistentTimerCoreApp.ear");
        PersistentTimerCoreApp.addAsModule(PersistentTimerCoreEJB).addAsModule(PersistentTimerCoreWeb);
        PersistentTimerCoreApp = (EnterpriseArchive) ShrinkHelper.addDirectory(PersistentTimerCoreApp, "test-applications/PersistentTimerCoreApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, PersistentTimerCoreApp);

        // Finally, start server
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CNTR0333W", "CWWKC1500W", "CWWKC1501W", "CWWKC1506E");
        }
    }

    //-----------------------------------------------------
    // --------------TimerLateWarningServlet---------------
    //-----------------------------------------------------

    /**
     * Test PersistentTimerTaskHandlerImpl.skipRun() logging a warning message when a Timer is starting
     * later than the default lateTimerThreshold of 5 minutes
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    //Full because test sleeps for over 5 minutes
    public void testDefaultLateTimerMessage() throws Exception {
        String warningRegExp = "CNTR0333W(?=.*LateWarning)(?=.*PersistentTimerCoreEJB.jar)(?=.*PersistentTimerCoreApp)";
        String timeoutRegExp = "WTRN0006W.*120";
        String servlet = "PersistentTimerCoreWeb/TimerLateWarningServlet";

        setLateTimerThresholdConfiguration(null);
        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, servlet, "testDefaultLateWarningMessageSetup");

        assertNull("Received unexpected message in log 'WTRN0006W:'", server.waitForStringInLogUsingMark(timeoutRegExp, 3 * 60 * 1000));
        assertNotNull("Did not receive expected message in log 'CNTR0333W:'", server.waitForStringInLogUsingMark(warningRegExp, 3 * 60 * 1000));

        FATServletClient.runTest(server, servlet, "testLateWarningMessageTearDown");
    }

    /**
     * Test PersistentTimerTaskHandlerImpl.skipRun() not logging a warning message when a Timer is starting
     * later than the default laterTimerThreshold of 5 minutes and the configured lateTimerThreshold is 0.
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    //Full because test sleeps for over 5 minutes
    public void testDisabledLateTimerMessage() throws Exception {
        String warningRegExp = "CNTR0333W:.*";
        String timeoutRegExp = "WTRN0006W.*120";
        String servlet = "PersistentTimerCoreWeb/TimerLateWarningServlet";

        setLateTimerThresholdConfiguration(0L);
        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, servlet, "testDisabledLateWarningMessageSetup");

        assertNull("Received unexpected message in log 'WTRN0006W:'", server.waitForStringInLogUsingMark(timeoutRegExp, 3 * 60 * 1000));
        assertNull("Received unexpected message in log 'CNTR0333W:'", server.waitForStringInLogUsingMark(warningRegExp, 3 * 60 * 1000));

        FATServletClient.runTest(server, servlet, "testLateWarningMessageTearDown");

        setLateTimerThresholdConfiguration(null);
    }

    /**
     * Test PersistentTimerTaskHandlerImpl.skipRun() logging a warning message when a Timer is starting
     * later than the configured lateTimerThreshold of 1 minute.
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    //Full because test sleeps for over 1 minutes
    public void testConfiguredLateTimerMessage() throws Exception {
        String warningRegExp = "CNTR0333W(?=.*LateWarning)(?=.*PersistentTimerCoreEJB.jar)(?=.*PersistentTimerCoreApp)";
        String servlet = "PersistentTimerCoreWeb/TimerLateWarningServlet";

        setLateTimerThresholdConfiguration(1L);
        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, servlet, "testConfiguredLateWarningMessageSetup");

        assertNotNull("Did not receive expected message in log 'CNTR0333W:'", server.waitForStringInLogUsingMark(warningRegExp, 2 * 60 * 1000));

        FATServletClient.runTest(server, servlet, "testLateWarningMessageTearDown");

        setLateTimerThresholdConfiguration(null);
    }

    /**
     * Change the setting of the timerService lateTimerThreshold to the specified value;
     * nothing is done if the specified value is the existing value.
     *
     * @param lateTimerThreshold late timer threshold in minutes, or null.
     */
    private static void setLateTimerThresholdConfiguration(Long lateTimerThreshold) throws Exception {

        ServerConfiguration config = server.getServerConfiguration();
        EJBContainerElement ejbContainer = config.getEJBContainer();
        EJBTimerServiceElement timerService = ejbContainer.getTimerService();
        Long currentLateTimerThreshold = timerService.getLateTimerThreshold();

        if (lateTimerThreshold != currentLateTimerThreshold) {
            timerService.setLateTimerThreshold(lateTimerThreshold);
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            assertNotNull(server.waitForConfigUpdateInLogUsingMark(null));
        }
    }

}
