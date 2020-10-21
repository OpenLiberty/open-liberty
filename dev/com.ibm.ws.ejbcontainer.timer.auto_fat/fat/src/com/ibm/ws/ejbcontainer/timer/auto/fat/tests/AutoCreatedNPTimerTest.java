/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
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

    @Server("AutoNPTimerServer")
    @TestServlet(servlet = AutoCreatedNPTimerServlet.class, contextRoot = AUTO_WAR_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("AutoNPTimerServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("AutoNPTimerServer")).andWith(new JakartaEE9Action().fullFATOnly().forServers("AutoNPTimerServer"));

    @BeforeClass
    public static void setup() throws Exception {
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

        ShrinkHelper.exportDropinAppToServer(server, AutoNPTimersApp);

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
