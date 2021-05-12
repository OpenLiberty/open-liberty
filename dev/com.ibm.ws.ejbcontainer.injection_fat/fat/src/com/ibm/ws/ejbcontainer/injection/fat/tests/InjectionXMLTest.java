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
import com.ibm.ws.ejbcontainer.injection.xml.web.AdvSLEJBInjectionServlet;
import com.ibm.ws.ejbcontainer.injection.xml.web.BasicSFEnvInjectionServlet;
import com.ibm.ws.ejbcontainer.injection.xml.web.BasicSFRemoteEnvInjectionServlet;
import com.ibm.ws.ejbcontainer.injection.xml.web.BasicSLEJBInjectionServlet;
import com.ibm.ws.ejbcontainer.injection.xml.web.BasicSLEnvInjectionServlet;
import com.ibm.ws.ejbcontainer.injection.xml.web.BasicSLRemoteEnvInjectionServlet;
import com.ibm.ws.ejbcontainer.injection.xml.web.MsgDestinationRefServlet;
import com.ibm.ws.ejbcontainer.injection.xml.web.SuperEnvInjectionServlet;
import com.ibm.ws.ejbcontainer.injection.xml.web.TypeCompatibleInjectionServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@RunWith(FATRunner.class)
public class InjectionXMLTest {

    @Server("com.ibm.ws.ejbcontainer.injection.fat.mdbserver")
    @TestServlets({ @TestServlet(servlet = AdvSLEJBInjectionServlet.class, contextRoot = "EJB3INJSXWeb"),
                    @TestServlet(servlet = BasicSFEnvInjectionServlet.class, contextRoot = "EJB3INJSXWeb"),
                    @TestServlet(servlet = BasicSFRemoteEnvInjectionServlet.class, contextRoot = "EJB3INJSXWeb"),
                    @TestServlet(servlet = BasicSLEJBInjectionServlet.class, contextRoot = "EJB3INJSXWeb"),
                    @TestServlet(servlet = BasicSLEnvInjectionServlet.class, contextRoot = "EJB3INJSXWeb"),
                    @TestServlet(servlet = BasicSLRemoteEnvInjectionServlet.class, contextRoot = "EJB3INJSXWeb"),
                    @TestServlet(servlet = MsgDestinationRefServlet.class, contextRoot = "EJB3INJSXWeb"),
                    @TestServlet(servlet = SuperEnvInjectionServlet.class, contextRoot = "EJB3INJSXWeb"),
                    @TestServlet(servlet = TypeCompatibleInjectionServlet.class, contextRoot = "EJB3INJSXWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.injection.fat.mdbserver")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.injection.fat.mdbserver"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Use ShrinkHelper to build the ear

        JavaArchive EJB3INJEJBXBeanJar = ShrinkHelper.buildJavaArchive("EJB3INJEJBXBean.jar", "com.ibm.ws.ejbcontainer.injection.xml.ejbx.");
        EJB3INJEJBXBeanJar = (JavaArchive) ShrinkHelper.addDirectory(EJB3INJEJBXBeanJar, "test-applications/EJB3INJEJBXBean.jar/resources");

        JavaArchive EJB3INJSXBeanJar = ShrinkHelper.buildJavaArchive("EJB3INJSXBean.jar", "com.ibm.ws.ejbcontainer.injection.xml.ejb.");
        EJB3INJSXBeanJar = (JavaArchive) ShrinkHelper.addDirectory(EJB3INJSXBeanJar, "test-applications/EJB3INJSXBean.jar/resources");

        JavaArchive OtherEJBJar = ShrinkHelper.buildJavaArchive("OtherEJB.jar", "com.ibm.ws.ejbcontainer.injection.xml.ejbo.");
        OtherEJBJar = (JavaArchive) ShrinkHelper.addDirectory(OtherEJBJar, "test-applications/OtherEJB.jar/resources");

        WebArchive EJB3INJSXWeb = ShrinkHelper.buildDefaultApp("EJB3INJSXWeb.war", "com.ibm.ws.ejbcontainer.injection.xml.web.");

        EnterpriseArchive EJB3INJSXTestApp = ShrinkWrap.create(EnterpriseArchive.class, "EJB3INJSXTestApp.ear");
        EJB3INJSXTestApp = (EnterpriseArchive) ShrinkHelper.addDirectory(EJB3INJSXTestApp, "test-applications/EJB3INJSXTestApp.ear/resources");
        EJB3INJSXTestApp.addAsModule(EJB3INJEJBXBeanJar).addAsModule(EJB3INJSXBeanJar).addAsModule(OtherEJBJar).addAsModule(EJB3INJSXWeb);

        ShrinkHelper.exportDropinAppToServer(server, EJB3INJSXTestApp);

        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer("CNTR0168W", "CNTR0020E", "CWNEN0061E", "CWNEN0009E", "CNTR4006E", "CNTR0338W");
    }
}
