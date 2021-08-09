/*******************************************************************************
 * Copyright (c) 2002, 2020 IBM Corporation and others.
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

import com.ibm.ejb2x.base.spec.sll.web.SLLocalHomeCreateServlet;
import com.ibm.ejb2x.base.spec.sll.web.SLLocalHomeRemoveServlet;
import com.ibm.ejb2x.base.spec.sll.web.SLLocalImplContextServlet;
import com.ibm.ejb2x.base.spec.sll.web.SLLocalImplEnvEntryServlet;
import com.ibm.ejb2x.base.spec.sll.web.SLLocalImplExceptionServlet;
import com.ibm.ejb2x.base.spec.sll.web.SLLocalImplLifecycleMethodServlet;
import com.ibm.ejb2x.base.spec.sll.web.SLLocalInterfaceContextServlet;
import com.ibm.ejb2x.base.spec.sll.web.SLLocalInterfaceMethodServlet;
import com.ibm.ejb2x.base.spec.sll.web.SLLocalInterfaceRemoveServlet;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class SLLocalTest extends FATServletClient {

    @Server("com.ibm.ws.ejbcontainer.legacy.server.sll")
    @TestServlets({ @TestServlet(servlet = SLLocalHomeCreateServlet.class, contextRoot = "EJB2XLocalSpecWeb"),
                    @TestServlet(servlet = SLLocalHomeRemoveServlet.class, contextRoot = "EJB2XLocalSpecWeb"),
                    @TestServlet(servlet = SLLocalImplContextServlet.class, contextRoot = "EJB2XLocalSpecWeb"),
                    @TestServlet(servlet = SLLocalImplEnvEntryServlet.class, contextRoot = "EJB2XLocalSpecWeb"),
                    @TestServlet(servlet = SLLocalImplExceptionServlet.class, contextRoot = "EJB2XLocalSpecWeb"),
                    @TestServlet(servlet = SLLocalImplLifecycleMethodServlet.class, contextRoot = "EJB2XLocalSpecWeb"),
                    @TestServlet(servlet = SLLocalInterfaceContextServlet.class, contextRoot = "EJB2XLocalSpecWeb"),
                    @TestServlet(servlet = SLLocalInterfaceMethodServlet.class, contextRoot = "EJB2XLocalSpecWeb"),
                    @TestServlet(servlet = SLLocalInterfaceRemoveServlet.class, contextRoot = "EJB2XLocalSpecWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.legacy.server.sll")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.legacy.server.sll")).andWith(new JakartaEE9Action().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.legacy.server.sll"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the ears

        //EJB2XSFLocalSpecEJB.jar EJB2XSLLocalSpecEJB.jar EJB2XLocalSpecWeb.war

        JavaArchive EJB2XSFLocalSpecEJB = ShrinkHelper.buildJavaArchive("EJB2XSFLocalSpecEJB.jar", "com.ibm.ejb2x.base.spec.sfl.ejb.");
        ShrinkHelper.addDirectory(EJB2XSFLocalSpecEJB, "test-applications/EJB2XSFLocalSpecEJB.jar/resources");

        JavaArchive EJB2XSLLocalSpecEJB = ShrinkHelper.buildJavaArchive("EJB2XSLLocalSpecEJB.jar", "com.ibm.ejb2x.base.spec.sll.ejb.");
        ShrinkHelper.addDirectory(EJB2XSLLocalSpecEJB, "test-applications/EJB2XSLLocalSpecEJB.jar/resources");

        WebArchive EJB2XLocalSpecWeb = ShrinkHelper.buildDefaultApp("EJB2XLocalSpecWeb.war", "com.ibm.ejb2x.base.spec.sfl.web.", "com.ibm.ejb2x.base.spec.sll.web.");

        EnterpriseArchive EJB2XLocalSpecApp = ShrinkWrap.create(EnterpriseArchive.class, "EJB2XLocalSpecApp.ear");
        EJB2XLocalSpecApp.addAsModules(EJB2XSFLocalSpecEJB, EJB2XSLLocalSpecEJB, EJB2XLocalSpecWeb);
        //ShrinkHelper.addDirectory(EJB2XLocalSpecApp, "test-applications/EJB2XLocalSpecApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, EJB2XLocalSpecApp);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CNTR0020E", "CWNEN0013W", "CWNEN0014W", "CWNEN0015W", "CWNEN0045W", "WLTC0017E");
        }
    }
}