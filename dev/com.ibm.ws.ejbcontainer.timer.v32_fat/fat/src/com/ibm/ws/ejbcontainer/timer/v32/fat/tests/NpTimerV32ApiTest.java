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
package com.ibm.ws.ejbcontainer.timer.v32.fat.tests;

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
import com.ibm.ws.ejbcontainer.timer.np.v32.web.NpTimerV32ApiServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test the behavior of the TimerService.getTimers() API for non-persistent
 * timers. <p>
 *
 * The following scenarios are covered for each session bean type:
 *
 * <ul>
 * <li>Automatic timers declared in multiple modules, including a war module.
 * <li>Transactional nature of timer creation for programmatic timers created
 * in multiple modules, including a war module.
 * <li>Transactional nature of timer cancellation, both automatic and programmatic
 * in multiple modules, including a war module.
 * <li>SingleAction timers that expire are no longer returned by getAllTimers(),
 * across multiple modules, including a war module.
 * <li>Interval timers that expire will be returned by getAllTimers() until cancelled,
 * across multiple modules, including a war module.
 * </ul>
 */
@RunWith(FATRunner.class)
public class NpTimerV32ApiTest extends FATServletClient {
    @Server("com.ibm.ws.ejbcontainer.timer.v32.fat.NpTimerServer")
    @TestServlets({ @TestServlet(servlet = NpTimerV32ApiServlet.class, contextRoot = "NpTimerV32ApiWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.timer.v32.fat.NpTimerServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.timer.v32.fat.NpTimerServer")).andWith(FeatureReplacementAction.EE9_FEATURES().forServers("com.ibm.ws.ejbcontainer.timer.v32.fat.NpTimerServer"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### NpTimerV32ApiApp.ear
        JavaArchive NpTimerV32ApiEJB = ShrinkHelper.buildJavaArchive("NpTimerV32ApiEJB.jar", "com.ibm.ws.ejbcontainer.timer.np.v32.ejb.");
        JavaArchive NpTimerV32ApiOtherEJB = ShrinkHelper.buildJavaArchive("NpTimerV32ApiOtherEJB.jar", "com.ibm.ws.ejbcontainer.timer.np.v32.otherejb.");
        JavaArchive NpTimerV32ApiShared = ShrinkHelper.buildJavaArchive("NpTimerV32ApiShared.jar", "com.ibm.ws.ejbcontainer.timer.np.v32.shared.");
        WebArchive NpTimerV32ApiWeb = ShrinkHelper.buildDefaultApp("NpTimerV32ApiWeb.war", "com.ibm.ws.ejbcontainer.timer.np.v32.web.*");
        EnterpriseArchive NpTimerV32ApiApp = ShrinkWrap.create(EnterpriseArchive.class, "NpTimerV32ApiApp.ear");
        NpTimerV32ApiApp.addAsModule(NpTimerV32ApiEJB).addAsModule(NpTimerV32ApiOtherEJB).addAsModule(NpTimerV32ApiWeb).addAsLibrary(NpTimerV32ApiShared);

        ShrinkHelper.exportDropinAppToServer(server, NpTimerV32ApiApp, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

}
