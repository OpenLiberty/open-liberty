/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.legacy.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.ejb2x.base.pitt.web.StatefulPassivationServlet;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class SFPassivationTest {

    @Server("com.ibm.ws.ejbcontainer.legacy.server.remote.noTrace")
    @TestServlets({ @TestServlet(servlet = StatefulPassivationServlet.class, contextRoot = "StatefulPassivationWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("com.ibm.ws.ejbcontainer.legacy.server.remote.noTrace")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.legacy.server.remote.noTrace"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the ears

        //StatefulPassivationEJB.jar StatefulPassivationWeb.war

        JavaArchive StatefulPassivationEJB = ShrinkHelper.buildJavaArchive("StatefulPassivationEJB.jar", "com.ibm.ejb2x.base.pitt.ejb.");
        ShrinkHelper.addDirectory(StatefulPassivationEJB, "test-applications/StatefulPassivationEJB.jar/resources");

        WebArchive StatefulPassivationWeb = ShrinkHelper.buildDefaultApp("StatefulPassivationWeb.war", "com.ibm.ejb2x.base.pitt.web.");

        EnterpriseArchive StatefulPassivationApp = ShrinkWrap.create(EnterpriseArchive.class, "StatefulPassivationApp.ear");
        StatefulPassivationApp.addAsModules(StatefulPassivationEJB, StatefulPassivationWeb);
        ShrinkHelper.addDirectory(StatefulPassivationApp, "test-applications/StatefulPassivationApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, StatefulPassivationApp);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}