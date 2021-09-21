/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.interceptor.v32.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.web.AroundConstructServlet;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class AroundConstructTest extends FATServletClient {

    @Server("com.ibm.ws.ejbcontainer.interceptor.v32.fat.aroundconstruct")
    @TestServlets({ @TestServlet(servlet = AroundConstructServlet.class, contextRoot = "AroundConstructApp") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("com.ibm.ws.ejbcontainer.interceptor.v32.fat.aroundconstruct")).andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.interceptor.v32.fat.aroundconstruct")).andWith(FeatureReplacementAction.EE9_FEATURES().forServers("com.ibm.ws.ejbcontainer.interceptor.v32.fat.aroundconstruct"));

    @BeforeClass
    public static void setUp() throws Exception {

        // Use ShrinkHelper to build the ears
        JavaArchive AroundConstructEJB = ShrinkHelper.buildJavaArchive("AroundConstructEJB.jar", "com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.ejb.");
        WebArchive AroundConstructWeb = ShrinkHelper.buildDefaultApp("AroundConstructWeb.war", "com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.web.");
        ShrinkHelper.addDirectory(AroundConstructWeb, "test-applications/AroundConstructWeb.war/resources");
        EnterpriseArchive AroundConstructApp = ShrinkWrap.create(EnterpriseArchive.class, "AroundConstructApp.ear");
        AroundConstructApp.addAsModule(AroundConstructEJB).addAsModule(AroundConstructWeb);
        ShrinkHelper.addDirectory(AroundConstructApp, "test-applications/AroundConstructApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, AroundConstructApp, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CNTR0019E", "CNTR0249E", "CNTR4006E", "CNTR4007E", "CNTR5007E");
        }

    }

}
