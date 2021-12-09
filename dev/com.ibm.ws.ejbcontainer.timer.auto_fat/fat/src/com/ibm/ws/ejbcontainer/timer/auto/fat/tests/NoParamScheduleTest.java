/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.timer.auto.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.ejbcontainer.timer.auto.noparam.web.NoParamScheduleServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * This test is equivalent to NoParamTimeoutTest, except that the Schedule annotation
 * is used instead of the Timeout annotation, and a timer/timeout-method is
 * used in XML instead of a timeout-method. In all cases, the scheduled timers
 * are non-persistent and are scheduled to fire every second.
 *
 * Migrated from suite.r80.base.timer.noparam.ScheduleTest.
 */
@RunWith(FATRunner.class)
public class NoParamScheduleTest extends FATServletClient {
    public static final String NOPARAM_WAR_NAME = "NoParamTimerWeb";
    private static final String SERVLET = "NoParamTimerWeb/NoParamScheduleServlet";

    @Server("AutoNPTimerNoParamServer")
    @TestServlet(servlet = NoParamScheduleServlet.class, contextRoot = NOPARAM_WAR_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("AutoNPTimerNoParamServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("AutoNPTimerNoParamServer")).andWith(new JakartaEE9Action().fullFATOnly().forServers("AutoNPTimerNoParamServer"));

    @BeforeClass
    public static void setUp() throws Exception {
        // cleanup from prior repeat actions
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();

        // Use ShrinkHelper to build the Ears & Wars

        //#################### NoParamTimerApp.ear
        JavaArchive NoParamTimerEJB = ShrinkHelper.buildJavaArchive("NoParamTimerEJB.jar", "com.ibm.ws.ejbcontainer.timer.auto.noparam.ejb.");
        NoParamTimerEJB = (JavaArchive) ShrinkHelper.addDirectory(NoParamTimerEJB, "test-applications/NoParamTimerEJB.jar/resources");
        WebArchive NoParamTimerWeb = ShrinkHelper.buildDefaultApp("NoParamTimerWeb.war", "com.ibm.ws.ejbcontainer.timer.auto.noparam.web.");

        EnterpriseArchive NoParamTimerApp = ShrinkWrap.create(EnterpriseArchive.class, "NoParamTimerApp.ear");
        NoParamTimerApp.addAsModule(NoParamTimerEJB).addAsModule(NoParamTimerWeb);

        ShrinkHelper.exportDropinAppToServer(server, NoParamTimerApp, DeployOptions.SERVER_ONLY);

        // Finally, start server
        server.startServer();

        FATServletClient.runTest(server, SERVLET, "setup");
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        FATServletClient.runTest(server, SERVLET, "cleanup");
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}
