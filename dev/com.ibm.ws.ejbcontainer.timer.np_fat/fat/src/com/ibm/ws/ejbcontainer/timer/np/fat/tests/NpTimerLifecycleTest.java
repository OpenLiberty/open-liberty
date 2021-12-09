/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.fat.tests;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.logging.Logger;

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
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test that non-persistent timers respect application lifecycle.
 */
@RunWith(FATRunner.class)
public class NpTimerLifecycleTest extends FATServletClient {
    private static final String servlet = "NpTimerLifecycleWeb/LifecycleServlet";
    private static final Logger logger = Logger.getLogger(NpTimerLifecycleTest.class.getName());

    @Server("com.ibm.ws.ejbcontainer.timer.np.fat.TimerLifecycleServer")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.timer.np.fat.TimerLifecycleServer")).andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.timer.np.fat.TimerLifecycleServer")).andWith(FeatureReplacementAction.EE9_FEATURES().forServers("com.ibm.ws.ejbcontainer.timer.np.fat.TimerLifecycleServer"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### NpTimerShared.jar (lib/global shared resource)
        JavaArchive NpTimerShared = ShrinkHelper.buildJavaArchive("NpTimerShared.jar", "com.ibm.ws.ejbcontainer.timer.np.shared.");

        ShrinkHelper.exportToServer(server, "lib/global", NpTimerShared, DeployOptions.SERVER_ONLY);

        //#################### NpTimerLifecycleApp.ear
        JavaArchive NpTimerLifecycleEJB = ShrinkHelper.buildJavaArchive("NpTimerLifecycleEJB.jar", "com.ibm.ws.ejbcontainer.timer.np.lifecycle.ejb.");
        EnterpriseArchive NpTimerLifecycleApp = ShrinkWrap.create(EnterpriseArchive.class, "NpTimerLifecycleApp.ear");
        NpTimerLifecycleApp.addAsModule(NpTimerLifecycleEJB);

        ShrinkHelper.exportAppToServer(server, NpTimerLifecycleApp, DeployOptions.SERVER_ONLY);

        //#################### NpTimerLifecycleWeb.war
        WebArchive NpTimerLifecycleWeb = ShrinkHelper.buildDefaultApp("NpTimerLifecycleWeb.war", "com.ibm.ws.ejbcontainer.timer.np.lifecycle.web.");

        ShrinkHelper.exportDropinAppToServer(server, NpTimerLifecycleWeb, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Verify that a timer can be created during singleton PostConstruct, that
     * the timer is returned as part of getTimers, but that it does not fire
     * until after PostConstruct has returned (the application is fully
     * started).
     */
    @Test
    public void testPostConstructWaits() throws Exception {
        //CWWKZ0001I: Application NpTimerLifecycleApp started in # seconds.
        server.waitForStringInLog("CWWKZ0001I.*NpTimerLifecycleApp");
        runTest(server, servlet, getTestMethodSimpleName());
    }

    /**
     * Verify that existing timers are cancelled when an application is stopped
     * and that new timers cannot be created during PreDestroy.
     */
    @Test
    public void testApplicationStopCancelsTimers() throws Exception {
        server.getServerConfiguration(); // do this in advance to speed up the application remove below.
        FATServletClient.runTest(server, servlet, "prepareForTestApplicationStopCancelsTimers");
        removeServerApplication("NpTimerLifecycleApp");
        runTest(server, servlet, getTestMethodSimpleName());
    }

    /**
     * Removes the specified application element from server.xml.
     */
    private void removeServerApplication(String application) throws Exception {

        ServerConfiguration config = server.getServerConfiguration();
        List<Application> removedApps = config.removeApplicationsByName(application);

        if (removedApps.size() >= 0) {
            for (Application removedApp : removedApps) {
                logger.info("removed application : " + removedApp.getName());
            }
        } else {
            logger.info("Did NOT remove application : " + application);
        }

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        //CWWKZ0009I: The application NpTimerLifecycleApp has stopped successfully.
        assertNotNull(server.waitForConfigUpdateInLogUsingMark(null, "CWWKZ0009I.*NpTimerLifecycleApp"));
    }

}
