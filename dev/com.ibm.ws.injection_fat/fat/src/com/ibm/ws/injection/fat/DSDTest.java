/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injection.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.injection.dsdann.web.BasicDSDAnnServlet;
import com.ibm.ws.injection.dsdmix.web.BasicDSDMixServlet;
import com.ibm.ws.injection.dsdxml.web.BasicDSDXMLServlet;

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
public class DSDTest extends FATServletClient {
    @Server("com.ibm.ws.injection.fat.DSDServer")
    @TestServlets({ @TestServlet(servlet = BasicDSDAnnServlet.class, contextRoot = "DSDAnnWeb"),
                    @TestServlet(servlet = BasicDSDMixServlet.class, contextRoot = "DSDMixWeb"),
                    @TestServlet(servlet = BasicDSDXMLServlet.class, contextRoot = "DSDXMLWeb")
    })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.injection.fat.DSDServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.injection.fat.DSDServer")).andWith(new JakartaEE9Action().forServers("com.ibm.ws.injection.fat.DSDServer"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the ears
        JavaArchive DSDAnnEJB = ShrinkHelper.buildJavaArchive("DSDAnnEJB.jar", "com.ibm.ws.injection.dsdann.ejb.");
        WebArchive DSDAnnWeb = ShrinkHelper.buildDefaultApp("DSDAnnWeb.war", "com.ibm.ws.injection.dsdann.web.");
        EnterpriseArchive DSDAnnTest = ShrinkWrap.create(EnterpriseArchive.class, "DSDAnnTest.ear");
        DSDAnnTest.addAsModule(DSDAnnEJB).addAsModule(DSDAnnWeb);

        JavaArchive DSDMixEJB = ShrinkHelper.buildJavaArchive("DSDMixEJB.jar", "com.ibm.ws.injection.dsdmix.ejb.");
        WebArchive DSDMixWeb = ShrinkHelper.buildDefaultApp("DSDMixWeb.war", "com.ibm.ws.injection.dsdmix.web.");
        EnterpriseArchive DSDMixTest = ShrinkWrap.create(EnterpriseArchive.class, "DSDMixTest.ear");
        DSDMixTest.addAsModule(DSDMixEJB).addAsModule(DSDMixWeb);

        JavaArchive DSDXMLEJB = ShrinkHelper.buildJavaArchive("DSDXMLEJB.jar", "com.ibm.ws.injection.dsdxml.ejb.");
        WebArchive DSDXMLWeb = ShrinkHelper.buildDefaultApp("DSDXMLWeb.war", "com.ibm.ws.injection.dsdxml.web.");
        EnterpriseArchive DSDXMLTest = ShrinkWrap.create(EnterpriseArchive.class, "DSDXMLTest.ear");
        DSDXMLTest.addAsModule(DSDXMLEJB).addAsModule(DSDXMLWeb);

        ShrinkHelper.exportAppToServer(server, DSDAnnTest);
        ShrinkHelper.exportAppToServer(server, DSDMixTest);
        ShrinkHelper.exportAppToServer(server, DSDXMLTest);

        server.addInstalledAppForValidation("DSDAnnTest");
        server.addInstalledAppForValidation("DSDMixTest");
        server.addInstalledAppForValidation("DSDXMLTest");

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKN0005W");
        }
    }
}