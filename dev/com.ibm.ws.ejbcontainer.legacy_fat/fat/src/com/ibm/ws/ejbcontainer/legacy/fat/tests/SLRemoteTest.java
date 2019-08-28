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

import com.ibm.ejb2x.base.spec.slr.web.SLRemoteHomeCreateServlet;
import com.ibm.ejb2x.base.spec.slr.web.SLRemoteHomeMethodServlet;
import com.ibm.ejb2x.base.spec.slr.web.SLRemoteHomeRemoveServlet;
import com.ibm.ejb2x.base.spec.slr.web.SLRemoteImplContextServlet;
import com.ibm.ejb2x.base.spec.slr.web.SLRemoteImplEnvEntryServlet;
import com.ibm.ejb2x.base.spec.slr.web.SLRemoteImplExceptionServlet;
import com.ibm.ejb2x.base.spec.slr.web.SLRemoteImplLifecycleMethodServlet;
import com.ibm.ejb2x.base.spec.slr.web.SLRemoteInterfaceContextServlet;
import com.ibm.ejb2x.base.spec.slr.web.SLRemoteInterfaceMethodServlet;
import com.ibm.ejb2x.base.spec.slr.web.SLRemoteInterfaceRemoveServlet;
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
public class SLRemoteTest extends FATServletClient {

    @Server("com.ibm.ws.ejbcontainer.legacy.server.remote")
    @TestServlets({ @TestServlet(servlet = SLRemoteHomeCreateServlet.class, contextRoot = "EJB2XRemoteSpecWeb"),
                    @TestServlet(servlet = SLRemoteHomeMethodServlet.class, contextRoot = "EJB2XRemoteSpecWeb"),
                    @TestServlet(servlet = SLRemoteHomeRemoveServlet.class, contextRoot = "EJB2XRemoteSpecWeb"),
                    @TestServlet(servlet = SLRemoteImplContextServlet.class, contextRoot = "EJB2XRemoteSpecWeb"),
                    @TestServlet(servlet = SLRemoteImplEnvEntryServlet.class, contextRoot = "EJB2XRemoteSpecWeb"),
                    @TestServlet(servlet = SLRemoteImplExceptionServlet.class, contextRoot = "EJB2XRemoteSpecWeb"),
                    @TestServlet(servlet = SLRemoteImplLifecycleMethodServlet.class, contextRoot = "EJB2XRemoteSpecWeb"),
                    @TestServlet(servlet = SLRemoteInterfaceContextServlet.class, contextRoot = "EJB2XRemoteSpecWeb"),
                    @TestServlet(servlet = SLRemoteInterfaceMethodServlet.class, contextRoot = "EJB2XRemoteSpecWeb"),
                    @TestServlet(servlet = SLRemoteInterfaceRemoveServlet.class, contextRoot = "EJB2XRemoteSpecWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.legacy.server.remote")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.legacy.server.remote"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the ears

        //EJB2XSFRemoteSpecEJB.jar EJB2XSLRemoteSpecEJB.jar EJB2XRemoteSpecWeb.war

        JavaArchive EJB2XSFRemoteSpecEJB = ShrinkHelper.buildJavaArchive("EJB2XSFRemoteSpecEJB.jar", "com.ibm.ejb2x.base.spec.sfr.ejb.");
        ShrinkHelper.addDirectory(EJB2XSFRemoteSpecEJB, "test-applications/EJB2XSFRemoteSpecEJB.jar/resources");

        JavaArchive EJB2XSLRemoteSpecEJB = ShrinkHelper.buildJavaArchive("EJB2XSLRemoteSpecEJB.jar", "com.ibm.ejb2x.base.spec.slr.ejb.");
        ShrinkHelper.addDirectory(EJB2XSLRemoteSpecEJB, "test-applications/EJB2XSLRemoteSpecEJB.jar/resources");

        WebArchive EJB2XRemoteSpecWeb = ShrinkHelper.buildDefaultApp("EJB2XRemoteSpecWeb.war", "com.ibm.ejb2x.base.spec.sfr.web.", "com.ibm.ejb2x.base.spec.slr.web.");

        EnterpriseArchive EJB2XRemoteSpecApp = ShrinkWrap.create(EnterpriseArchive.class, "EJB2XRemoteSpecApp.ear");
        EJB2XRemoteSpecApp.addAsModules(EJB2XSFRemoteSpecEJB, EJB2XSLRemoteSpecEJB, EJB2XRemoteSpecWeb);
        ShrinkHelper.addDirectory(EJB2XRemoteSpecApp, "test-applications/EJB2XRemoteSpecApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, EJB2XRemoteSpecApp);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CNTR0020E", "CWNEN0013W", "CWNEN0014W", "CWNEN0015W", "CWNEN0045W", "WLTC0017E");
        }
    }
}