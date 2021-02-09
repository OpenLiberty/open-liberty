/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.managedbeans.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.managedbeans.fat.mb.ejb.web.ManagedBeanEjbServlet;
import com.ibm.ws.managedbeans.fat.xml.ejb.web.ManagedBeanXmlServlet;

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
public class ManagedBeansEjbTest extends FATServletClient {

    public static final String EJB_WEB_NAME = "ManagedBeanEjbWeb";
    public static final String XML_WEB_NAME = "ManagedBeanXmlWeb";

    @Server("ManagedBeansEjbServer")
    @TestServlets({ @TestServlet(servlet = ManagedBeanEjbServlet.class, contextRoot = EJB_WEB_NAME),
                    @TestServlet(servlet = ManagedBeanXmlServlet.class, contextRoot = XML_WEB_NAME) })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("ManagedBeansEjbServer"))
                    .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().forServers("ManagedBeansEjbServer"))
                    .andWith(new JakartaEE9Action().fullFATOnly().forServers("ManagedBeansEjbServer"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### ManagedBeanEjbApp.ear
        JavaArchive ManagedBeanEJB = ShrinkHelper.buildJavaArchive("ManagedBeanEJB.jar", "com.ibm.ws.managedbeans.fat.mb.ejb.");
        WebArchive ManagedBeanEjbWeb = ShrinkHelper.buildDefaultApp("ManagedBeanEjbWeb.war", "com.ibm.ws.managedbeans.fat.mb.ejb.web");
        EnterpriseArchive ManagedBeanEjbApp = ShrinkWrap.create(EnterpriseArchive.class, "ManagedBeanEjbApp.ear");
        ManagedBeanEjbApp.addAsModule(ManagedBeanEJB).addAsModule(ManagedBeanEjbWeb);

        ShrinkHelper.exportDropinAppToServer(server, ManagedBeanEjbApp);

        //#################### ManagedBeanXmlApp.ear
        JavaArchive ManagedBeanXmlEJB = ShrinkHelper.buildJavaArchive("ManagedBeanXmlEJB.jar", "com.ibm.ws.managedbeans.fat.xml.ejb.");
        ManagedBeanXmlEJB = (JavaArchive) ShrinkHelper.addDirectory(ManagedBeanXmlEJB, "test-applications/ManagedBeanXmlEJB.jar/resources");
        WebArchive ManagedBeanXmlWeb = ShrinkHelper.buildDefaultApp("ManagedBeanXmlWeb.war", "com.ibm.ws.managedbeans.fat.xml.ejb.web");
        EnterpriseArchive ManagedBeanXmlApp = ShrinkWrap.create(EnterpriseArchive.class, "ManagedBeanXmlApp.ear");
        ManagedBeanXmlApp.addAsModule(ManagedBeanXmlEJB).addAsModule(ManagedBeanXmlWeb);

        ShrinkHelper.exportDropinAppToServer(server, ManagedBeanXmlApp);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}
