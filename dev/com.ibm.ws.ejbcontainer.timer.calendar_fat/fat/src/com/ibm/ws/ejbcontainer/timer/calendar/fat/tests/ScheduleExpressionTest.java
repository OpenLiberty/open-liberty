/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.timer.calendar.fat.tests;

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
import com.ibm.ws.ejbcontainer.timer.cal.web.ScheduleExpressionServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class ScheduleExpressionTest extends FATServletClient {
    private static final String SERVLET = "TimerCalTestWeb/ScheduleExpressionServlet";

    @Server("com.ibm.ws.ejbcontainer.timer.cal.fat.NpTimerServer")
    @TestServlets({ @TestServlet(servlet = ScheduleExpressionServlet.class, contextRoot = "TimerCalTestWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.timer.cal.fat.NpTimerServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.timer.cal.fat.NpTimerServer")).andWith(FeatureReplacementAction.EE9_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.timer.cal.fat.NpTimerServer")).andWith(FeatureReplacementAction.EE10_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.timer.cal.fat.NpTimerServer"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### TimerCalTestApp.ear
        JavaArchive TimerCalTestEJB = ShrinkHelper.buildJavaArchive("TimerCalTestEJB.jar", "com.ibm.ws.ejbcontainer.timer.cal.ejb.");
        WebArchive TimerCalTestWeb = ShrinkHelper.buildDefaultApp("TimerCalTestWeb.war", "com.ibm.ws.ejbcontainer.timer.cal.web.");
        EnterpriseArchive TimerCalTestApp = ShrinkWrap.create(EnterpriseArchive.class, "TimerCalTestApp.ear");
        TimerCalTestApp.addAsModule(TimerCalTestEJB).addAsModule(TimerCalTestWeb);

        ShrinkHelper.exportDropinAppToServer(server, TimerCalTestApp, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        try {
            FATServletClient.runTest(server, SERVLET, "cleanup");
        } finally {
            if (server != null && server.isStarted()) {
                server.stopServer();
            }
        }
    }

}
