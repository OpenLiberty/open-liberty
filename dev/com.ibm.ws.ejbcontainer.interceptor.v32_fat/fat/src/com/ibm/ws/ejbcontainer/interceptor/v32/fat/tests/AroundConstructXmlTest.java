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

import com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.xml.web.AroundConstructXmlServlet;
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
public class AroundConstructXmlTest extends FATServletClient {

    @Server("com.ibm.ws.ejbcontainer.interceptor.v32.fat.aroundconstruct.xml")
    @TestServlets({ @TestServlet(servlet = AroundConstructXmlServlet.class, contextRoot = "AroundConstructXmlApp") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("com.ibm.ws.ejbcontainer.interceptor.v32.fat.aroundconstruct.xml")).andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.interceptor.v32.fat.aroundconstruct.xml")).andWith(FeatureReplacementAction.EE9_FEATURES().forServers("com.ibm.ws.ejbcontainer.interceptor.v32.fat.aroundconstruct.xml"));

    @BeforeClass
    public static void setUp() throws Exception {

        // Use ShrinkHelper to build the ears
        JavaArchive AroundConstructXmlEJB = ShrinkHelper.buildJavaArchive("AroundConstructXmlEJB.jar", "com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.xml.ejb.");
        ShrinkHelper.addDirectory(AroundConstructXmlEJB, "test-applications/AroundConstructXmlEJB.jar/resources");
        WebArchive AroundConstructXmlWeb = ShrinkHelper.buildDefaultApp("AroundConstructXmlWeb.war", "com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.xml.web.");
        ShrinkHelper.addDirectory(AroundConstructXmlWeb, "test-applications/AroundConstructXmlWeb.war/resources");
        EnterpriseArchive AroundConstructXmlApp = ShrinkWrap.create(EnterpriseArchive.class, "AroundConstructXmlApp.ear");
        AroundConstructXmlApp.addAsModule(AroundConstructXmlEJB).addAsModule(AroundConstructXmlWeb);
        ShrinkHelper.addDirectory(AroundConstructXmlApp, "test-applications/AroundConstructXmlApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, AroundConstructXmlApp, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CNTR0019E");
        }

    }

}
