/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.injection.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.ejbcontainer.injection.ann.web.SFEnvInjectionServlet;
import com.ibm.ws.ejbcontainer.injection.ann.web.SFRemoteEnvInjectionServlet;
import com.ibm.ws.ejbcontainer.injection.ann.web.SLEnvInjectionServlet;
import com.ibm.ws.ejbcontainer.injection.ann.web.SLRmtServEnvInjectionServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class RemoteInjectionTest {
    @Server("com.ibm.ws.ejbcontainer.injection.fat.server")
    @TestServlets({ @TestServlet(servlet = SFEnvInjectionServlet.class, contextRoot = "EJB3INJSAWeb"),
                    @TestServlet(servlet = SFRemoteEnvInjectionServlet.class, contextRoot = "EJB3INJSAWeb"),
                    @TestServlet(servlet = SLEnvInjectionServlet.class, contextRoot = "EJB3INJSAWeb"),
                    @TestServlet(servlet = SLRmtServEnvInjectionServlet.class, contextRoot = "EJB3INJSAWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("com.ibm.ws.ejbcontainer.injection.fat.server")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.injection.fat.server"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Use ShrinkHelper to build the ear

        JavaArchive EJB3INJSABeanJar = ShrinkHelper.buildJavaArchive("EJB3INJSABean.jar", "com.ibm.ws.ejbcontainer.injection.ann.ejb.");
        WebArchive EJB3INJSAWeb = ShrinkHelper.buildDefaultApp("EJB3INJSAWeb.war", "com.ibm.ws.ejbcontainer.injection.ann.web.");

        EnterpriseArchive EJB3INJSATestApp = ShrinkWrap.create(EnterpriseArchive.class, "EJB3INJSATestApp.ear");
        EJB3INJSATestApp.addAsModule(EJB3INJSABeanJar).addAsModule(EJB3INJSAWeb);
        EJB3INJSATestApp = (EnterpriseArchive) ShrinkHelper.addDirectory(EJB3INJSATestApp, "test-applications/EJB3INJSATestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, EJB3INJSATestApp);

        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer("CNTR0020E");
    }
}
