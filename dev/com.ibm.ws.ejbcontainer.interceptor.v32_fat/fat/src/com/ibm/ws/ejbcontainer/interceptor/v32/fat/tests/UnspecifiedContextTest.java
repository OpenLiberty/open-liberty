/*******************************************************************************
 * Copyright (c) 2014, 2023 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.interceptor.v32.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.ejblite.interceptor.v32.mix.web.UnspecifiedContextMixServlet;
import com.ibm.ejblite.interceptor.v32.xml.web.UnspecifiedContextXmlServlet;
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
public class UnspecifiedContextTest extends FATServletClient {

    @Server("com.ibm.ws.ejbcontainer.interceptor.v32.fat.basic")
    @TestServlets({ @TestServlet(servlet = UnspecifiedContextMixServlet.class, contextRoot = "EJB3INTMTestApp"),
                    @TestServlet(servlet = UnspecifiedContextXmlServlet.class, contextRoot = "EJB3INTXTestApp") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("com.ibm.ws.ejbcontainer.interceptor.v32.fat.basic")).andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.interceptor.v32.fat.basic")).andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11).forServers("com.ibm.ws.ejbcontainer.interceptor.v32.fat.basic")).andWith(FeatureReplacementAction.EE10_FEATURES().forServers("com.ibm.ws.ejbcontainer.interceptor.v32.fat.basic"));

    @BeforeClass
    public static void setUp() throws Exception {

        // Use ShrinkHelper to build the ears
        JavaArchive EJB3INTMBean = ShrinkHelper.buildJavaArchive("EJB3INTMBean.jar", "com.ibm.ejblite.interceptor.v32.mix.ejb.");
        ShrinkHelper.addDirectory(EJB3INTMBean, "test-applications/EJB3INTMBean.jar/resources");
        WebArchive EJB3INTMWeb = ShrinkHelper.buildDefaultApp("EJB3INTMWeb.war", "com.ibm.ejblite.interceptor.v32.mix.web.");
        ShrinkHelper.addDirectory(EJB3INTMWeb, "test-applications/EJB3INTMWeb.war/resources");
        EnterpriseArchive EJB3INTMTestApp = ShrinkWrap.create(EnterpriseArchive.class, "EJB3INTMTestApp.ear");
        EJB3INTMTestApp.addAsModule(EJB3INTMBean).addAsModule(EJB3INTMWeb);
        ShrinkHelper.addDirectory(EJB3INTMTestApp, "test-applications/EJB3INTMTestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, EJB3INTMTestApp, DeployOptions.SERVER_ONLY);

        JavaArchive EJB3INTXBean = ShrinkHelper.buildJavaArchive("EJB3INTXBean.jar", "com.ibm.ejblite.interceptor.v32.xml.ejb.");
        ShrinkHelper.addDirectory(EJB3INTXBean, "test-applications/EJB3INTXBean.jar/resources");
        WebArchive EJB3INTXWeb = ShrinkHelper.buildDefaultApp("EJB3INTXWeb.war", "com.ibm.ejblite.interceptor.v32.xml.web.");
        ShrinkHelper.addDirectory(EJB3INTXWeb, "test-applications/EJB3INTXWeb.war/resources");
        EnterpriseArchive EJB3INTXTestApp = ShrinkWrap.create(EnterpriseArchive.class, "EJB3INTXTestApp.ear");
        EJB3INTXTestApp.addAsModule(EJB3INTXBean).addAsModule(EJB3INTXWeb);
        ShrinkHelper.addDirectory(EJB3INTXTestApp, "test-applications/EJB3INTXTestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, EJB3INTXTestApp, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CNTR0020E");
        }
    }

}
