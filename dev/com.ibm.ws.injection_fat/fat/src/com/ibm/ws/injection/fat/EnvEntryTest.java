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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import suite.r85.base.injection.envann.web.AdvEnvAnnObjServlet;
import suite.r85.base.injection.envann.web.AdvEnvAnnPrimServlet;
import suite.r85.base.injection.envann.web.BasicEnvObjAnnServlet;
import suite.r85.base.injection.envann.web.BasicEnvPrimAnnServlet;
import suite.r85.base.injection.envmix.web.AdvEnvMixObjServlet;
import suite.r85.base.injection.envmix.web.AdvEnvMixPrimServlet;
import suite.r85.base.injection.envmix.web.BasicEnvObjMixServlet;
import suite.r85.base.injection.envmix.web.BasicEnvPrimMixServlet;
import suite.r85.base.injection.envxml.web.AdvEnvXMLObjServlet;
import suite.r85.base.injection.envxml.web.AdvEnvXMLPrimServlet;
import suite.r85.base.injection.envxml.web.BasicEnvObjXMLServlet;
import suite.r85.base.injection.envxml.web.BasicEnvPrimXMLServlet;

/**
 * This test case ensures that @Resource can be declared and inject an env-entry
 * for all the boxed types (Boolean, Integer, etc.) into the fields and methods
 * of servlets, listeners, and filters. To perform the test, a servlet is invoked
 * in the web module with a listener or filter declared in the web.xml. The
 * expected result is that the servlet, listener, or filter is created and injected
 * with the values specified in the ibm-web-bnd.xml.
 *
 * @author jnowosa
 *
 */
@RunWith(FATRunner.class)
public class EnvEntryTest extends FATServletClient {
    @Server("com.ibm.ws.injection.fat.EnvEntryServer")
    @TestServlets({ @TestServlet(servlet = BasicEnvPrimAnnServlet.class, contextRoot = "EnvEntryAnnWeb"),
                    @TestServlet(servlet = BasicEnvObjAnnServlet.class, contextRoot = "EnvEntryAnnWeb"),
                    @TestServlet(servlet = AdvEnvAnnPrimServlet.class, contextRoot = "EnvEntryAnnWeb"),
                    @TestServlet(servlet = AdvEnvAnnObjServlet.class, contextRoot = "EnvEntryAnnWeb"),
                    @TestServlet(servlet = BasicEnvPrimMixServlet.class, contextRoot = "EnvEntryMixWeb"),
                    @TestServlet(servlet = BasicEnvObjMixServlet.class, contextRoot = "EnvEntryMixWeb"),
                    @TestServlet(servlet = AdvEnvMixPrimServlet.class, contextRoot = "EnvEntryMixWeb"),
                    @TestServlet(servlet = AdvEnvMixObjServlet.class, contextRoot = "EnvEntryMixWeb"),
                    @TestServlet(servlet = BasicEnvPrimXMLServlet.class, contextRoot = "EnvEntryXMLWeb"),
                    @TestServlet(servlet = BasicEnvObjXMLServlet.class, contextRoot = "EnvEntryXMLWeb"),
                    @TestServlet(servlet = AdvEnvXMLPrimServlet.class, contextRoot = "EnvEntryXMLWeb"),
                    @TestServlet(servlet = AdvEnvXMLObjServlet.class, contextRoot = "EnvEntryXMLWeb")
    })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification().andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.injection.fat.EnvEntryServer"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the ears
        WebArchive EnvEntryAnnWeb = ShrinkHelper.buildDefaultApp("EnvEntryAnnWeb.war", "suite.r85.base.injection.envann.web.");
        EnterpriseArchive EnvEntryAnnTest = ShrinkWrap.create(EnterpriseArchive.class, "EnvEntryAnnTest.ear");
        EnvEntryAnnTest.addAsModule(EnvEntryAnnWeb);

        WebArchive EnvEntryMixWeb = ShrinkHelper.buildDefaultApp("EnvEntryMixWeb.war", "suite.r85.base.injection.envmix.web.");
        EnterpriseArchive EnvEntryMixTest = ShrinkWrap.create(EnterpriseArchive.class, "EnvEntryMixTest.ear");
        EnvEntryMixTest.addAsModule(EnvEntryMixWeb);

        WebArchive EnvEntryXMLWeb = ShrinkHelper.buildDefaultApp("EnvEntryXMLWeb.war", "suite.r85.base.injection.envxml.web.");
        EnterpriseArchive EnvEntryXMLTest = ShrinkWrap.create(EnterpriseArchive.class, "EnvEntryXMLTest.ear");
        EnvEntryXMLTest.addAsModule(EnvEntryXMLWeb);

        ShrinkHelper.exportDropinAppToServer(server, EnvEntryAnnTest);
        ShrinkHelper.exportDropinAppToServer(server, EnvEntryMixTest);
        ShrinkHelper.exportDropinAppToServer(server, EnvEntryXMLTest);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}