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

import static com.ibm.ws.ejbcontainer.timer.persistent.fat.tests.PersistentTimerTestHelper.expectedFailures;
import static org.junit.Assert.assertFalse;
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
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.EJBContainerElement;
import com.ibm.websphere.simplicity.config.EJBTimerServiceElement;
import com.ibm.websphere.simplicity.config.PersistentExecutor;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.ejbcontainer.timer.persistent.core.web.TimerAccessOperationsServlet;
import com.ibm.ws.ejbcontainer.timer.persistent.core.web.TimerSFOperationsServlet;
import com.ibm.ws.ejbcontainer.timer.persistent.core.web.TimerSLOperationsServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class PersistentTimerCoreTest extends FATServletClient {

    public static final String CORE_WAR_NAME = "PersistentTimerCoreWeb";
    public static final String MISSED_ACTION_WAR_NAME = "MissedTimerActionWeb";

    @Server("com.ibm.ws.ejbcontainer.timer.persistent.fat.PersistentTimerServer")
    @TestServlets({ @TestServlet(servlet = TimerAccessOperationsServlet.class, contextRoot = CORE_WAR_NAME),
                    @TestServlet(servlet = TimerSFOperationsServlet.class, contextRoot = CORE_WAR_NAME),
                    @TestServlet(servlet = TimerSLOperationsServlet.class, contextRoot = CORE_WAR_NAME) })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.timer.persistent.fat.PersistentTimerServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.timer.persistent.fat.PersistentTimerServer")).andWith(new JakartaEE9Action().forServers("com.ibm.ws.ejbcontainer.timer.persistent.fat.PersistentTimerServer"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### InitTxRecoveryLogApp.ear (Automatically initializes transaction recovery logs)
        JavaArchive InitTxRecoveryLogEJBJar = ShrinkHelper.buildJavaArchive("InitTxRecoveryLogEJB.jar", "com.ibm.ws.ejbcontainer.init.recovery.ejb.");

        EnterpriseArchive InitTxRecoveryLogApp = ShrinkWrap.create(EnterpriseArchive.class, "InitTxRecoveryLogApp.ear");
        InitTxRecoveryLogApp.addAsModule(InitTxRecoveryLogEJBJar);

        ShrinkHelper.exportDropinAppToServer(server, InitTxRecoveryLogApp, DeployOptions.SERVER_ONLY);

        //#################### MissedTimerActionApp.ear
        JavaArchive MissedTimerActionEJB = ShrinkHelper.buildJavaArchive("MissedTimerActionEJB.jar", "com.ibm.ws.ejbcontainer.timer.persistent.missed.ejb.");
        WebArchive MissedTimerActionWeb = ShrinkHelper.buildDefaultApp("MissedTimerActionWeb.war", "com.ibm.ws.ejbcontainer.timer.persistent.missed.web.");

        EnterpriseArchive MissedTimerActionApp = ShrinkWrap.create(EnterpriseArchive.class, "MissedTimerActionApp.ear");
        MissedTimerActionApp.addAsModule(MissedTimerActionEJB).addAsModule(MissedTimerActionWeb);

        ShrinkHelper.exportDropinAppToServer(server, MissedTimerActionApp, DeployOptions.SERVER_ONLY);

        //#################### PersistentTimerCoreApp.ear
        JavaArchive PersistentTimerCoreEJB = ShrinkHelper.buildJavaArchive("PersistentTimerCoreEJB.jar", "com.ibm.ws.ejbcontainer.timer.persistent.core.ejb.");
        PersistentTimerCoreEJB = (JavaArchive) ShrinkHelper.addDirectory(PersistentTimerCoreEJB, "test-applications/PersistentTimerCoreEJB.jar/resources");
        WebArchive PersistentTimerCoreWeb = ShrinkHelper.buildDefaultApp("PersistentTimerCoreWeb.war", "com.ibm.ws.ejbcontainer.timer.persistent.core.web.");

        EnterpriseArchive PersistentTimerCoreApp = ShrinkWrap.create(EnterpriseArchive.class, "PersistentTimerCoreApp.ear");
        PersistentTimerCoreApp.addAsModule(PersistentTimerCoreEJB).addAsModule(PersistentTimerCoreWeb);
        PersistentTimerCoreApp = (EnterpriseArchive) ShrinkHelper.addDirectory(PersistentTimerCoreApp, "test-applications/PersistentTimerCoreApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, PersistentTimerCoreApp, DeployOptions.SERVER_ONLY);

        // Finally, start server
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // CNTR0333W  : test*LateTimer - Late Timer Warning
        // CWWKC1501W : testSLTimerServiceEJBTimeoutSessionContextCMT - PersistentExecutor rolled back a task
        // CWWKC1506E : testSLTimerServiceEJBTimeoutSessionContextCMT - Transaction is marked for rollback
        // CWWKG0032W : testMissedTimerActionBadValueNoFailover - Unexpected value [Blah]
        // CWWKG0014E - intermittently caused by server.xml being momentarily missing during server reconfig
        if (server != null && server.isStarted()) {
            server.stopServer(expectedFailures("CNTR0333W", "CWWKC1500W", "CWWKC1501W", "CWWKC1506E", "CWWKG0032W.*Blah", "CWWKG0014E"));
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

    //-----------------------------------------------------
    // --------------MissedTimerActionServlet--------------
    //-----------------------------------------------------

    /**
     * Test Persistent Timer missed action default behavior when failover has not been enabled.
     * The default behavior without failover should be ALL. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will run all expirations despite a delay that causes several to be missed.
     * <li> Timer.getNextTimeout() will return values in the past for missed expirations.
     * </ol>
     */
    @Test
    public void testMissedTimerActionDefaultNoFailover() throws Exception {
        // Default when no failover is ALL
        testMissedTimerAction(null, false);
    }

    /**
     * Test Persistent Timer missed action default behavior when failover has been enabled.
     * The default behavior with failover should be ONCE. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will skip expirations missed because of a delay.
     * <li> Timer.getNextTimeout() will return values in the future; skipping missed expirations.
     * </ol>
     */
    @Test
    public void testMissedTimerActionDefaultWithFailover() throws Exception {
        // Default when failover is enabled is ONCE
        testMissedTimerAction(null, true);
    }

    /**
     * Test Persistent Timer missed action "ALL" behavior when failover has not been enabled. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will run all expirations despite a delay that causes several to be missed.
     * <li> Timer.getNextTimeout() will return values in the past for missed expirations.
     * </ol>
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testMissedTimerActionAllNoFailover() throws Exception {
        testMissedTimerAction("ALL", false);
    }

    /**
     * Test Persistent Timer missed action "ALL" behavior when failover has been enabled. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will run all expirations despite a delay that causes several to be missed.
     * <li> Timer.getNextTimeout() will return values in the past for missed expirations.
     * </ol>
     */
    @Test
    public void testMissedTimerActionAllWithFailover() throws Exception {
        testMissedTimerAction("ALL", true);
    }

    /**
     * Test Persistent Timer missed action "ONCE" behavior when failover has not been enabled. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will skip expirations missed because of a delay.
     * <li> Timer.getNextTimeout() will return values in the future; skipping missed expirations.
     * </ol>
     */
    @Test
    public void testMissedTimerActionOnceNoFailover() throws Exception {
        testMissedTimerAction("ONCE", false);
    }

    /**
     * Test Persistent Timer missed action "ONCE" behavior when failover has been enabled. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will skip expirations missed because of a delay.
     * <li> Timer.getNextTimeout() will return values in the future; skipping missed expirations.
     * </ol>
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testMissedTimerActionOnceWithFailover() throws Exception {
        testMissedTimerAction("ONCE", true);
    }

    /**
     * Test Persistent Timer missed action "Once" (mixed case) behavior when failover has not been enabled.
     * The value is case insensitive and will be treated as "ONCE". <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will skip expirations missed because of a delay.
     * <li> Timer.getNextTimeout() will return values in the future; skipping missed expirations.
     * </ol>
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testMissedTimerActionMixedCaseNoFailover() throws Exception {
        testMissedTimerAction("Once", false);
    }

    /**
     * Test Persistent Timer missed action "Blah" (bad value) behavior when failover has not been enabled.
     * A warning will be logged, and the value will be treated as unspecified, so default to "ALL". <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will run all expirations despite a delay that causes several to be missed.
     * <li> Timer.getNextTimeout() will return values in the past for missed expirations.
     * </ol>
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testMissedTimerActionBadValueNoFailover() throws Exception {
        // Change to non-default configuration before setting bad configuration value.
        setMissedPersistentTimerActionConfiguration("ONCE", false);
        server.setMarkToEndOfLog();

        // Change to bad value "Blah" configuration.
        setMissedPersistentTimerActionConfiguration("Blah", false);
        assertFalse("Expected CWWKG0032W message for Blah did not occur", server.findStringsInLogs("CWWKG0032W.*missedPersistentTimerAction.*Blah").isEmpty());
        server.setMarkToEndOfLog();

        // Run test and verify default, ALL, is used rather than prior value, "ONCE"
        try {
            String servlet = MISSED_ACTION_WAR_NAME + "/MissedTimerActionServlet";
            FATServletClient.runTest(server, servlet, getTestMethodSimpleName());
        } finally {
            setMissedPersistentTimerActionConfiguration(null, false);
        }
    }

    private void testMissedTimerAction(String missedTimerAction, boolean failover) throws Exception {
        setMissedPersistentTimerActionConfiguration(missedTimerAction, failover);
        server.setMarkToEndOfLog();

        try {
            String servlet = MISSED_ACTION_WAR_NAME + "/MissedTimerActionServlet";
            FATServletClient.runTest(server, servlet, getTestMethodSimpleName());
        } finally {
            setMissedPersistentTimerActionConfiguration(null, false);
        }
    }

    /**
     * Change the setting of the timerService missedPersistentTimerAction to the specified value;
     * and set persistentExecutor missedTaskThreshold to the minimum value if failover should
     * be enabled.
     *
     * @param missedPersistentTimerAction missed persistent timer action or null
     * @param failover true if failover should be enabled; otherwise false
     */
    private static void setMissedPersistentTimerActionConfiguration(String missedPersistentTimerAction, boolean failover) throws Exception {

        ServerConfiguration config = server.getServerConfiguration();
        EJBContainerElement ejbContainer = config.getEJBContainer();
        EJBTimerServiceElement timerService = ejbContainer.getTimerService();
        timerService.setMissedPersistentTimerAction(missedPersistentTimerAction);

        PersistentExecutor persistentExecutor = config.getPersistentExecutors().getById("Howdy");
        if (failover) {
            persistentExecutor.setMissedTaskThreshold("100s");
            persistentExecutor.setRetryInterval(null); // mutually exclusive with missedTaskThreshold
        } else {
            persistentExecutor.setMissedTaskThreshold(null);
            persistentExecutor.setRetryInterval("300s");
        }

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        assertNotNull(server.waitForConfigUpdateInLogUsingMark(null));
    }
}
