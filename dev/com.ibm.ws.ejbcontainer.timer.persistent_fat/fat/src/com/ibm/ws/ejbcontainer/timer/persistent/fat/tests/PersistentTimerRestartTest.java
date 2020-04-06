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

package com.ibm.ws.ejbcontainer.timer.persistent.fat.tests;

import static com.ibm.ws.ejbcontainer.timer.persistent.fat.tests.PersistentTimerTestHelper.expectedFailures;

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
import com.ibm.websphere.simplicity.config.PersistentExecutor;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 8)
public class PersistentTimerRestartTest extends FATServletClient {

    public static final String RESTART_MISSED_ACTION_WAR_NAME = "RestartMissedTimerActionWeb";
    public static final String RESTART_MISSED_ACTION_SERVLET = RESTART_MISSED_ACTION_WAR_NAME + "/RestartMissedTimerActionServlet";

    @Server("com.ibm.ws.ejbcontainer.timer.persistent.fat.PersistentTimerRestartServer")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.timer.persistent.fat.PersistentTimerRestartServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.timer.persistent.fat.PersistentTimerRestartServer"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### InitTxRecoveryLogApp.ear (Automatically initializes transaction recovery logs)
        JavaArchive InitTxRecoveryLogEJBJar = ShrinkHelper.buildJavaArchive("InitTxRecoveryLogEJB.jar", "com.ibm.ws.ejbcontainer.init.recovery.ejb.");

        EnterpriseArchive InitTxRecoveryLogApp = ShrinkWrap.create(EnterpriseArchive.class, "InitTxRecoveryLogApp.ear");
        InitTxRecoveryLogApp.addAsModule(InitTxRecoveryLogEJBJar);

        ShrinkHelper.exportDropinAppToServer(server, InitTxRecoveryLogApp);

        //#################### RestartMissedTimerActionApp.ear
        JavaArchive RestartMissedTimerActionEJB = ShrinkHelper.buildJavaArchive("RestartMissedTimerActionEJB.jar", "com.ibm.ws.ejbcontainer.timer.persistent.restart.missed.ejb.");
        WebArchive RestartMissedTimerActionWeb = ShrinkHelper.buildDefaultApp("RestartMissedTimerActionWeb.war", "com.ibm.ws.ejbcontainer.timer.persistent.restart.missed.web.");

        EnterpriseArchive RestartMissedTimerActionApp = ShrinkWrap.create(EnterpriseArchive.class, "RestartMissedTimerActionApp.ear");
        RestartMissedTimerActionApp.addAsModule(RestartMissedTimerActionEJB).addAsModule(RestartMissedTimerActionWeb);

        ShrinkHelper.exportDropinAppToServer(server, RestartMissedTimerActionApp);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(expectedFailures());
        }
    }

    //-----------------------------------------------------
    // --------------RestartMissedTimerActionServlet-------
    //-----------------------------------------------------

    /**
     * Test Persistent Timer missed action default behavior across server restart when failover has not been enabled.
     * The default behavior without failover should be ALL. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will run all expirations despite a server restart that causes several to be missed.
     * <li> Timer.getNextTimeout() will return values in the past for missed expirations.
     * </ol>
     */
    @Test
    public void testMissedTimerActionDefaultNoFailoverRestart() throws Exception {
        // Default when no failover is ALL
        // No action, no failover, don't wait for timer to run before restarting server
        testMissedTimerAction(null, false, false);
    }

    /**
     * Test Persistent Timer missed action default behavior across server restart when failover has been enabled.
     * The default behavior with failover should be ONCE. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will skip expirations missed because of a server restart.
     * <li> Timer.getNextTimeout() will return values in the future; skipping missed expirations.
     * </ol>
     */
    @Test
    public void testMissedTimerActionDefaultWithFailoverRestart() throws Exception {
        // Default when failover is enabled is ONCE
        // No action, failover, don't wait for timer to run before restarting server
        testMissedTimerAction(null, true, false);
    }

    /**
     * Test Persistent Timer missed action "ALL" behavior across server restart when failover has been enabled. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will run all expirations despite a server restart that causes several to be missed.
     * <li> Timer.getNextTimeout() will return values in the past for missed expirations.
     * </ol>
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testMissedTimerActionAllWithFailoverRestart() throws Exception {
        // ALL action, failover, wait for timer to run before restarting server
        testMissedTimerAction("ALL", true, true);
    }

    /**
     * Test Persistent Timer missed action "ONCE" behavior across server restart when failover has not been enabled. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will skip expirations missed because of a server restart.
     * <li> Timer.getNextTimeout() will return values in the future; skipping missed expirations.
     * </ol>
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testMissedTimerActionOnceNoFailoverRestart() throws Exception {
        // ONCE action, no failover, wait for timer to run before restarting server
        testMissedTimerAction("ONCE", false, true);
    }

    private void testMissedTimerAction(String missedTimerAction, boolean failover, boolean waitForExpiration) throws Exception {
        setMissedPersistentTimerActionConfiguration(missedTimerAction, failover);
        server.startServer();

        // getNextTimeout is calculated differently when timer has never run before; so sometimes force at least one run
        String createTimerMethod = (waitForExpiration) ? "createIntervalTimerAndWaitForExpiration" : "createIntervalTimer";

        try {
            FATServletClient.runTest(server, RESTART_MISSED_ACTION_SERVLET, "cancelAllTimers");
            FATServletClient.runTest(server, RESTART_MISSED_ACTION_SERVLET, createTimerMethod);
            server.stopServer(expectedFailures());
            Thread.sleep(1000);
            server.startServer();
            FATServletClient.runTest(server, RESTART_MISSED_ACTION_SERVLET, getTestMethodSimpleName());
            server.stopServer(expectedFailures("CNTR0333W"));
        } catch (Throwable ex) {
            if (server != null && server.isStarted()) {
                try {
                    FATServletClient.runTest(server, RESTART_MISSED_ACTION_SERVLET, "cancelAllTimers");
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
                try {
                    server.stopServer();
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
            }
            throw ex;
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

        PersistentExecutor persistentExecutor = config.getPersistentExecutors().getById("EJBPersistentTimerExecutor");
        if (failover) {
            persistentExecutor.setMissedTaskThreshold("3s");
            persistentExecutor.setRetryInterval(null); // mutually exclusive with missedTaskThreshold
        } else {
            persistentExecutor.setMissedTaskThreshold(null);
            persistentExecutor.setRetryInterval("300s");
        }

        server.updateServerConfiguration(config);
    }
}
