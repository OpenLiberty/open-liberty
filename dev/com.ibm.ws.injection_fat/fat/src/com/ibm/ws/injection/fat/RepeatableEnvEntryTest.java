/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
import com.ibm.ws.injection.repeatable.envmix.web.AdvRepeatableEnvMixObjServlet;
import com.ibm.ws.injection.repeatable.envmix.web.AdvRepeatableEnvMixPrimServlet;
import com.ibm.ws.injection.repeatable.envmix.web.BasicRepeatableEnvMixObjServlet;
import com.ibm.ws.injection.repeatable.envmix.web.BasicRepeatableEnvMixPrimServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * This test case ensures that repeated @Resource can be declared and inject an
 * env-entry for all the boxed types (Boolean, Integer, etc.) into the fields and
 * methods of servlets, listeners, and filters. To perform the test, a servlet is
 * invoked in the web module with a listener or filter declared in the web.xml.
 * The expected result is that the servlet, listener, or filter is created and injected
 * with the values specified in the ibm-web-bnd.xml.
 *
 * @author bmdecker
 *
 */
@RunWith(FATRunner.class)
public class RepeatableEnvEntryTest extends FATServletClient {
    @Server("com.ibm.ws.injection.fat.RepeatableEnvEntryServer")
    @TestServlets({ @TestServlet(servlet = BasicRepeatableEnvMixPrimServlet.class, contextRoot = "RepeatableEnvEntryMixWeb"),
                    @TestServlet(servlet = BasicRepeatableEnvMixObjServlet.class, contextRoot = "RepeatableEnvEntryMixWeb"),
                    @TestServlet(servlet = AdvRepeatableEnvMixPrimServlet.class, contextRoot = "RepeatableEnvEntryMixWeb"),
                    @TestServlet(servlet = AdvRepeatableEnvMixObjServlet.class, contextRoot = "RepeatableEnvEntryMixWeb")
    })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the ears
        WebArchive RepeatableEnvEntryMixWeb = ShrinkHelper.buildDefaultApp("RepeatableEnvEntryMixWeb.war", "com.ibm.ws.injection.repeatable.envmix.web.");
        EnterpriseArchive RepeatableEnvEntryMixTest = ShrinkWrap.create(EnterpriseArchive.class, "RepeatableEnvEntryMixTest.ear");
        RepeatableEnvEntryMixTest.addAsModule(RepeatableEnvEntryMixWeb);

        ShrinkHelper.exportDropinAppToServer(server, RepeatableEnvEntryMixTest);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}