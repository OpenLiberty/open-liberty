/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
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
import com.ibm.ws.injection.repeatable.dsdann.web.BasicRepeatableDSDAnnServlet;
import com.ibm.ws.injection.repeatable.dsdmix.web.BasicRepeatableDSDMixServlet;
import com.ibm.ws.injection.repeatable.dsdxml.web.BasicRepeatableDSDXMLServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class RepeatableDSDTest extends FATServletClient {
    @Server("com.ibm.ws.injection.fat.RepeatableDSDServer")
    @TestServlets({ @TestServlet(servlet = BasicRepeatableDSDAnnServlet.class, contextRoot = "RepeatableDSDAnnWeb"),
                    @TestServlet(servlet = BasicRepeatableDSDMixServlet.class, contextRoot = "RepeatableDSDMixWeb"),
                    @TestServlet(servlet = BasicRepeatableDSDXMLServlet.class, contextRoot = "RepeatableDSDXMLWeb")
    })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the ears
        JavaArchive RepeatableDSDAnnEJB = ShrinkHelper.buildJavaArchive("RepeatableDSDAnnEJB.jar", "com.ibm.ws.injection.repeatable.dsdann.ejb.");
        WebArchive RepeatableDSDAnnWeb = ShrinkHelper.buildDefaultApp("RepeatableDSDAnnWeb.war", "com.ibm.ws.injection.repeatable.dsdann.web.");
        EnterpriseArchive RepeatableDSDAnnTest = ShrinkWrap.create(EnterpriseArchive.class, "RepeatableDSDAnnTest.ear");
        RepeatableDSDAnnTest.addAsModule(RepeatableDSDAnnEJB).addAsModule(RepeatableDSDAnnWeb);

        JavaArchive RepeatableDSDMixEJB = ShrinkHelper.buildJavaArchive("RepeatableDSDMixEJB.jar", "com.ibm.ws.injection.repeatable.dsdmix.ejb.");
        WebArchive RepeatableDSDMixWeb = ShrinkHelper.buildDefaultApp("RepeatableDSDMixWeb.war", "com.ibm.ws.injection.repeatable.dsdmix.web.");
        EnterpriseArchive RepeatableDSDMixTest = ShrinkWrap.create(EnterpriseArchive.class, "RepeatableDSDMixTest.ear");
        RepeatableDSDMixTest.addAsModule(RepeatableDSDMixEJB).addAsModule(RepeatableDSDMixWeb);

        JavaArchive RepeatableDSDXMLEJB = ShrinkHelper.buildJavaArchive("RepeatableDSDXMLEJB.jar", "com.ibm.ws.injection.repeatable.dsdxml.ejb.");
        WebArchive RepeatableDSDXMLWeb = ShrinkHelper.buildDefaultApp("RepeatableDSDXMLWeb.war", "com.ibm.ws.injection.repeatable.dsdxml.web.");
        EnterpriseArchive RepeatableDSDXMLTest = ShrinkWrap.create(EnterpriseArchive.class, "RepeatableDSDXMLTest.ear");
        RepeatableDSDXMLTest.addAsModule(RepeatableDSDXMLEJB).addAsModule(RepeatableDSDXMLWeb);

        ShrinkHelper.exportAppToServer(server, RepeatableDSDAnnTest);
        ShrinkHelper.exportAppToServer(server, RepeatableDSDMixTest);
        ShrinkHelper.exportAppToServer(server, RepeatableDSDXMLTest);

        server.addInstalledAppForValidation("RepeatableDSDAnnTest");
        server.addInstalledAppForValidation("RepeatableDSDMixTest");
        server.addInstalledAppForValidation("RepeatableDSDXMLTest");

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}