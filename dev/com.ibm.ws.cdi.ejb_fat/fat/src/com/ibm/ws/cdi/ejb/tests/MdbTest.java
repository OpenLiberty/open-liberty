/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.ejb.apps.mdbTestEar.jar.JarMdb;
import com.ibm.ws.cdi.ejb.apps.mdbTestEar.jarNoBeanDiscovery.JarMdbNotDiscovered;
import com.ibm.ws.cdi.ejb.apps.mdbTestEar.lib.EarTestMessageHolder;
import com.ibm.ws.cdi.ejb.apps.mdbTestEar.war.EarMdbTestServlet;
import com.ibm.ws.cdi.ejb.apps.mdbWar.BasicMdbTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class MdbTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12MdbServer";
    public static final String BASIC_APP = "basicMdb";
    public static final String EAR_APP = "MdbTest";
    public static final String EAR_WAR_NAME = "EarTestWar";

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE10, EERepeatActions.EE9, EERepeatActions.EE7);

    @Server(SERVER_NAME)
    @TestServlets({ @TestServlet(contextRoot = BASIC_APP, servlet = BasicMdbTestServlet.class),
                    @TestServlet(contextRoot = EAR_WAR_NAME, servlet = EarMdbTestServlet.class)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, BASIC_APP + ".war")
                                   .addPackage(BasicMdbTestServlet.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);

        // Regular jar with MDB
        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "EjbJar.jar")
                                       .addClass(JarMdb.class);

        // Jar with MDB with bean-discovery-mode=none
        JavaArchive ejbNoDiscoveryJar = ShrinkWrap.create(JavaArchive.class, "EjbNoDiscoveryJar.jar")
                                                  .addClass(JarMdbNotDiscovered.class)
                                                  .addAsManifestResource(JarMdbNotDiscovered.class.getResource("beans.xml"), "beans.xml");

        // Ear lib, containing a CDI bean
        JavaArchive earLib = ShrinkWrap.create(JavaArchive.class, "EarLib.jar")
                                       .addClass(EarTestMessageHolder.class);

        // War to hold the test servlet
        WebArchive earTestWar = ShrinkWrap.create(WebArchive.class, EAR_WAR_NAME + ".war")
                                          .addClass(EarMdbTestServlet.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_APP + ".ear")
                                          .addAsModules(ejbJar, ejbNoDiscoveryJar, earTestWar)
                                          .addAsLibrary(earLib);

        ShrinkHelper.exportDropinAppToServer(server, ear, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }
}
