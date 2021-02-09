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

import com.ibm.ejb2x.base.spec.sfl.web.SFLocalHomeCreateServlet;
import com.ibm.ejb2x.base.spec.sfl.web.SFLocalHomeRemoveServlet;
import com.ibm.ejb2x.base.spec.sfl.web.SFLocalImplContextServlet;
import com.ibm.ejb2x.base.spec.sfl.web.SFLocalImplEnvEntryServlet;
import com.ibm.ejb2x.base.spec.sfl.web.SFLocalImplExceptionServlet;
import com.ibm.ejb2x.base.spec.sfl.web.SFLocalImplLifecycleMethodServlet;
import com.ibm.ejb2x.base.spec.sfl.web.SFLocalInterfaceContextServlet;
import com.ibm.ejb2x.base.spec.sfl.web.SFLocalInterfaceMethodServlet;
import com.ibm.ejb2x.base.spec.sfl.web.SFLocalInterfaceRemoveServlet;
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
public class SFLocalTest extends FATServletClient {

    @Server("com.ibm.ws.ejbcontainer.legacy.server")
    @TestServlets({ @TestServlet(servlet = SFLocalHomeCreateServlet.class, contextRoot = "EJB2XLocalSpecWeb"),
                    @TestServlet(servlet = SFLocalHomeRemoveServlet.class, contextRoot = "EJB2XLocalSpecWeb"),
                    @TestServlet(servlet = SFLocalImplContextServlet.class, contextRoot = "EJB2XLocalSpecWeb"),
                    @TestServlet(servlet = SFLocalImplEnvEntryServlet.class, contextRoot = "EJB2XLocalSpecWeb"),
                    @TestServlet(servlet = SFLocalImplExceptionServlet.class, contextRoot = "EJB2XLocalSpecWeb"),
                    @TestServlet(servlet = SFLocalImplLifecycleMethodServlet.class, contextRoot = "EJB2XLocalSpecWeb"),
                    @TestServlet(servlet = SFLocalInterfaceContextServlet.class, contextRoot = "EJB2XLocalSpecWeb"),
                    @TestServlet(servlet = SFLocalInterfaceMethodServlet.class, contextRoot = "EJB2XLocalSpecWeb"),
                    @TestServlet(servlet = SFLocalInterfaceRemoveServlet.class, contextRoot = "EJB2XLocalSpecWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.legacy.server")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.legacy.server")).andWith(new JakartaEE9Action().forServers("com.ibm.ws.ejbcontainer.legacy.server"));

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