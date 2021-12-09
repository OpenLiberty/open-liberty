/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.timer.auto.fat.tests;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.zone.ZoneRules;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.ejbcontainer.timer.auto.npTimer.web.AutoCreatedNPTimerServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class AutoCreatedNPTimerTest extends FATServletClient {
    public static final String AUTO_WAR_NAME = "AutoNPTimersWeb";
    private static final String SERVLET = "AutoNPTimersWeb/AutoCreatedNPTimerServlet";
    private static final Logger logger = Logger.getLogger(AutoCreatedNPTimerTest.class.getCanonicalName());

    private static boolean allowDaylightSavingsSkip = false;
    private static boolean skipTest = false;

    @Server("AutoNPTimerServer")
    @TestServlet(servlet = AutoCreatedNPTimerServlet.class, contextRoot = AUTO_WAR_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("AutoNPTimerServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("AutoNPTimerServer")).andWith(new JakartaEE9Action().fullFATOnly().forServers("AutoNPTimerServer"));

    @BeforeClass
    public static void setup() throws Exception {
        // Custom FATRunner doesn't support skipping tests like this in @BeforeClass; deferred to @Before
        // Assume.assumeTrue(!leavingDaylightSavings());
        if (leavingDaylightSavings()) {
            // Log message and avoid starting server and waiting 5 minutes in setup for timers to run
            logger.info("Leaving Daylight Savings; all tests will be skipped");
            skipTest = true;
            return;
        }

        // cleanup from prior repeat actions
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();

        // Use ShrinkHelper to build the Ears & Wars

        //#################### AutoNPTimersApp.ear
        JavaArchive AutoNPTimersEJB = ShrinkHelper.buildJavaArchive("AutoNPTimersEJB.jar", "com.ibm.ws.ejbcontainer.timer.auto.npTimer.ejb.");
        AutoNPTimersEJB = (JavaArchive) ShrinkHelper.addDirectory(AutoNPTimersEJB, "test-applications/AutoNPTimersEJB.jar/resources");
        WebArchive AutoNPTimersWeb = ShrinkHelper.buildDefaultApp("AutoNPTimersWeb.war", "com.ibm.ws.ejbcontainer.timer.auto.npTimer.web.");

        EnterpriseArchive AutoNPTimersApp = ShrinkWrap.create(EnterpriseArchive.class, "AutoNPTimersApp.ear");
        AutoNPTimersApp.addAsModule(AutoNPTimersEJB).addAsModule(AutoNPTimersWeb);

        ShrinkHelper.exportDropinAppToServer(server, AutoNPTimersApp, DeployOptions.SERVER_ONLY);

        // Finally, start server
        server.startServer();

        FATServletClient.runTest(server, SERVLET, "setup");
    }

    @Before
    public void beforeMethod() throws Exception {
        Assume.assumeTrue(!skipTest);
        // Verify this mechanism to skip tests continues to work
        Assume.assumeTrue(!testName.getMethodName().startsWith("testSkipTestWithAssumeInBefore"));
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (!skipTest) {
            FATServletClient.runTest(server, SERVLET, "cleanup");
            if (server != null && server.isStarted()) {
                server.stopServer();
            }
        }
    }

    public static boolean leavingDaylightSavings() {
        if (!allowDaylightSavingsSkip) {
            return false;
        }

        // Check if leaving daylight savings using local timezone
        ZonedDateTime now = ZonedDateTime.now();
        ZoneRules zoneRules = now.getZone().getRules();
        boolean nowDst = zoneRules.isDaylightSavings(now.toInstant());
        ZonedDateTime end = now.plus(1, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES);
        boolean endDst = zoneRules.isDaylightSavings(end.toInstant());

        // Also check against eastern timezone since test also uses that
        ZonedDateTime eastern_now = ZonedDateTime.now(ZoneId.of("America/New_York"));
        ZoneRules eastern_zoneRules = eastern_now.getZone().getRules();
        boolean eastern_nowDst = eastern_zoneRules.isDaylightSavings(eastern_now.toInstant());
        ZonedDateTime eastern_end = eastern_now.plus(1, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES);
        boolean eastern_endDst = eastern_zoneRules.isDaylightSavings(eastern_end.toInstant());

        return (nowDst && !endDst) || (eastern_nowDst && !eastern_endDst);
    }

    /**
     * Verifies that AssumptionViolatedException from @Before will skip this test.
     */
    @Test
    public void testSkipTestWithAssumeInBefore() throws Exception {
        throw new IllegalStateException("This method should always be skipped");
    }
}
