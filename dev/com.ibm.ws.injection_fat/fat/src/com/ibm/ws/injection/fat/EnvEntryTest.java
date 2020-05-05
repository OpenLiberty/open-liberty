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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.injection.envann.web.AdvEnvAnnObjServlet;
import com.ibm.ws.injection.envann.web.AdvEnvAnnPrimServlet;
import com.ibm.ws.injection.envann.web.BasicEnvObjAnnServlet;
import com.ibm.ws.injection.envann.web.BasicEnvPrimAnnServlet;
import com.ibm.ws.injection.envmix.web.AdvEnvMixObjServlet;
import com.ibm.ws.injection.envmix.web.AdvEnvMixPrimServlet;
import com.ibm.ws.injection.envmix.web.BasicEnvObjMixServlet;
import com.ibm.ws.injection.envmix.web.BasicEnvPrimMixServlet;
import com.ibm.ws.injection.envxml.web.AdvEnvXMLObjServlet;
import com.ibm.ws.injection.envxml.web.AdvEnvXMLPrimServlet;
import com.ibm.ws.injection.envxml.web.BasicEnvObjXMLServlet;
import com.ibm.ws.injection.envxml.web.BasicEnvPrimXMLServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

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
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("com.ibm.ws.injection.fat.EnvEntryServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.injection.fat.EnvEntryServer")).andWith(new JakartaEE9Action().forServers("com.ibm.ws.injection.fat.EnvEntryServer"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the ears
        WebArchive EnvEntryAnnWeb = ShrinkHelper.buildDefaultApp("EnvEntryAnnWeb.war", "com.ibm.ws.injection.envann.web.");
        EnterpriseArchive EnvEntryAnnTest = ShrinkWrap.create(EnterpriseArchive.class, "EnvEntryAnnTest.ear");
        EnvEntryAnnTest.addAsModule(EnvEntryAnnWeb);

        WebArchive EnvEntryMixWeb = ShrinkHelper.buildDefaultApp("EnvEntryMixWeb.war", "com.ibm.ws.injection.envmix.web.");
        EnterpriseArchive EnvEntryMixTest = ShrinkWrap.create(EnterpriseArchive.class, "EnvEntryMixTest.ear");
        EnvEntryMixTest.addAsModule(EnvEntryMixWeb);

        WebArchive EnvEntryXMLWeb = ShrinkHelper.buildDefaultApp("EnvEntryXMLWeb.war", "com.ibm.ws.injection.envxml.web.");
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