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

import com.ibm.ejb1x.base.spec.sfr.web.SFRemoteHomeCreateServlet;
import com.ibm.ejb1x.base.spec.sfr.web.SFRemoteHomeMethodServlet;
import com.ibm.ejb1x.base.spec.sfr.web.SFRemoteHomeRemoveServlet;
import com.ibm.ejb1x.base.spec.sfr.web.SFRemoteImplContextServlet;
import com.ibm.ejb1x.base.spec.sfr.web.SFRemoteImplEnvEntryServlet;
import com.ibm.ejb1x.base.spec.sfr.web.SFRemoteImplExceptionServlet;
import com.ibm.ejb1x.base.spec.sfr.web.SFRemoteImplLifecycleMethodServlet;
import com.ibm.ejb1x.base.spec.sfr.web.SFRemoteInterfaceContextServlet;
import com.ibm.ejb1x.base.spec.sfr.web.SFRemoteInterfaceMethodServlet;
import com.ibm.ejb1x.base.spec.sfr.web.SFRemoteInterfaceRemoveServlet;
import com.ibm.ejb1x.base.spec.slr.web.SLRemoteHomeCreateServlet;
import com.ibm.ejb1x.base.spec.slr.web.SLRemoteHomeMethodServlet;
import com.ibm.ejb1x.base.spec.slr.web.SLRemoteHomeRemoveServlet;
import com.ibm.ejb1x.base.spec.slr.web.SLRemoteImplContextServlet;
import com.ibm.ejb1x.base.spec.slr.web.SLRemoteImplEnvEntryServlet;
import com.ibm.ejb1x.base.spec.slr.web.SLRemoteImplExceptionServlet;
import com.ibm.ejb1x.base.spec.slr.web.SLRemoteImplLifecycleMethodServlet;
import com.ibm.ejb1x.base.spec.slr.web.SLRemoteInterfaceContextServlet;
import com.ibm.ejb1x.base.spec.slr.web.SLRemoteInterfaceMethodServlet;
import com.ibm.ejb1x.base.spec.slr.web.SLRemoteInterfaceRemoveServlet;
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
public class EJB1XStatefulTest extends FATServletClient {

    @Server("com.ibm.ws.ejbcontainer.legacy.server.remote")
    @TestServlets({ @TestServlet(servlet = SFRemoteHomeCreateServlet.class, contextRoot = "EJB1XRemoteSpecWeb"),
                    @TestServlet(servlet = SFRemoteHomeMethodServlet.class, contextRoot = "EJB1XRemoteSpecWeb"),
                    @TestServlet(servlet = SFRemoteHomeRemoveServlet.class, contextRoot = "EJB1XRemoteSpecWeb"),
                    @TestServlet(servlet = SFRemoteImplContextServlet.class, contextRoot = "EJB1XRemoteSpecWeb"),
                    @TestServlet(servlet = SFRemoteImplEnvEntryServlet.class, contextRoot = "EJB1XRemoteSpecWeb"),
                    @TestServlet(servlet = SFRemoteImplExceptionServlet.class, contextRoot = "EJB1XRemoteSpecWeb"),
                    @TestServlet(servlet = SFRemoteImplLifecycleMethodServlet.class, contextRoot = "EJB1XRemoteSpecWeb"),
                    @TestServlet(servlet = SFRemoteInterfaceContextServlet.class, contextRoot = "EJB1XRemoteSpecWeb"),
                    @TestServlet(servlet = SFRemoteInterfaceMethodServlet.class, contextRoot = "EJB1XRemoteSpecWeb"),
                    @TestServlet(servlet = SFRemoteInterfaceRemoveServlet.class, contextRoot = "EJB1XRemoteSpecWeb"),
                    @TestServlet(servlet = SLRemoteHomeCreateServlet.class, contextRoot = "EJB1XRemoteSpecWeb"),
                    @TestServlet(servlet = SLRemoteHomeMethodServlet.class, contextRoot = "EJB1XRemoteSpecWeb"),
                    @TestServlet(servlet = SLRemoteHomeRemoveServlet.class, contextRoot = "EJB1XRemoteSpecWeb"),
                    @TestServlet(servlet = SLRemoteImplContextServlet.class, contextRoot = "EJB1XRemoteSpecWeb"),
                    @TestServlet(servlet = SLRemoteImplEnvEntryServlet.class, contextRoot = "EJB1XRemoteSpecWeb"),
                    @TestServlet(servlet = SLRemoteImplExceptionServlet.class, contextRoot = "EJB1XRemoteSpecWeb"),
                    @TestServlet(servlet = SLRemoteImplLifecycleMethodServlet.class, contextRoot = "EJB1XRemoteSpecWeb"),
                    @TestServlet(servlet = SLRemoteInterfaceContextServlet.class, contextRoot = "EJB1XRemoteSpecWeb"),
                    @TestServlet(servlet = SLRemoteInterfaceMethodServlet.class, contextRoot = "EJB1XRemoteSpecWeb"),
                    @TestServlet(servlet = SLRemoteInterfaceRemoveServlet.class, contextRoot = "EJB1XRemoteSpecWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.legacy.server.remote")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.legacy.server.remote"));

    @BeforeClass
    public static void setUp() throws Exception {
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();

        // Use ShrinkHelper to build the ears

        JavaArchive EJB1XSFRemoteSpecEJB = ShrinkHelper.buildJavaArchive("EJB1XSFRemoteSpecEJB.jar", "com.ibm.ejb1x.base.spec.sfr.ejb.");
        ShrinkHelper.addDirectory(EJB1XSFRemoteSpecEJB, "test-applications/EJB1XSFRemoteSpecEJB.jar/resources");

        JavaArchive EJB1XSLRemoteSpecEJB = ShrinkHelper.buildJavaArchive("EJB1XSLRemoteSpecEJB.jar", "com.ibm.ejb1x.base.spec.slr.ejb.");
        ShrinkHelper.addDirectory(EJB1XSLRemoteSpecEJB, "test-applications/EJB1XSLRemoteSpecEJB.jar/resources");

        WebArchive EJB1XRemoteSpecWeb = ShrinkHelper.buildDefaultApp("EJB1XRemoteSpecWeb.war", "com.ibm.ejb1x.base.spec.sfr.web.", "com.ibm.ejb1x.base.spec.slr.web.");

        EnterpriseArchive EJB1XRemoteSpecApp = ShrinkWrap.create(EnterpriseArchive.class, "EJB1XRemoteSpecApp.ear");
        EJB1XRemoteSpecApp.addAsModules(EJB1XSFRemoteSpecEJB, EJB1XSLRemoteSpecEJB, EJB1XRemoteSpecWeb);
        ShrinkHelper.addDirectory(EJB1XRemoteSpecApp, "test-applications/EJB1XRemoteSpecApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, EJB1XRemoteSpecApp);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CNTR0019E", "CNTR0020E", "CWNEN0013W", "CWNEN0014W", "CWNEN0015W", "CWNEN0045W", "WLTC0017E");
        }
    }
}