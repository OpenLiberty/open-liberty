/*******************************************************************************
 * Copyright (c) 2010, 2019 IBM Corporation and others.
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

import com.ibm.ejb2x.ejbinwar.web.EJB2xTestServlet;
import com.ibm.ejb2x.ejbinwar.web.InterfaceAndNamespaceTestServlet;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class EJBinWAR2xTest extends FATServletClient {
    @Server("com.ibm.ws.ejbcontainer.legacy.server.remote")
    @TestServlets({ @TestServlet(servlet = EJB2xTestServlet.class, contextRoot = "EJBinWARTest"),
                    @TestServlet(servlet = InterfaceAndNamespaceTestServlet.class, contextRoot = "EJBinWARTest") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.legacy.server.remote")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.legacy.server.remote"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the ears
        JavaArchive EJBinWARIntf = ShrinkHelper.buildJavaArchive("EJBinWARIntf.jar", "com.ibm.ejb2x.ejbinwar.intf.");
        WebArchive EJB2xInWARBean = ShrinkHelper.buildDefaultApp("EJB2xInWARBean.war", "com.ibm.ejb2x.ejbinwar.webejb2x.");
        ShrinkHelper.addDirectory(EJB2xInWARBean, "test-applications/EJB2xInWARBean.war/resources");
        WebArchive EJBinWARTest = ShrinkHelper.buildDefaultApp("EJBinWARTest.war", "com.ibm.ejb2x.ejbinwar.web.");

        EnterpriseArchive EJBinWARTestApp = ShrinkWrap.create(EnterpriseArchive.class, "EJBinWARTestApp.ear");
        EJBinWARTestApp.addAsModules(EJBinWARTest, EJB2xInWARBean);
        EJBinWARTestApp.addAsLibrary(EJBinWARIntf);
        ShrinkHelper.addDirectory(EJBinWARTestApp, "test-applications/EJBinWARTestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, EJBinWARTestApp);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}