/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config13.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.config13.duplicateInServerXML.web.DuplicateInServerXMLServlet;
import com.ibm.ws.microprofile.config13.mapEnvVar.web.MapEnvVarServlet;
import com.ibm.ws.microprofile.config13.serverXML.web.ServerXMLServlet;
import com.ibm.ws.microprofile.config13.serverXMLWebApp.web.ServerXMLWebAppServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Example Shrinkwrap FAT project:
 * <li> Application packaging is done in the @BeforeClass, instead of ant scripting.
 * <li> Injects servers via @Server annotation. Annotation value corresponds to the
 * server directory name in 'publish/servers/%annotation_value%' where ports get
 * assigned to the LibertyServer instance when the 'testports.properties' does not
 * get used.
 * <li> Specifies an @RunWith(FATRunner.class) annotation. Traditionally this has been
 * added to bytecode automatically by ant.
 * <li> Uses the @TestServlet annotation to define test servlets. Notice that no @Test
 * methods are defined in this class. All of the @Test methods are defined on the test
 * servlet referenced by the annotation, and will be run whenever this test class runs.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ServerXMLTest extends FATServletClient {

    public static final String SERVER_NAME = "ServerXMLServer";
    public static final String SERVER_XML_APP_NAME = "serverXMLApp";
    public static final String DUPLICATE_IN_SERVER_XML_APP_NAME = "duplicateInServerXMLApp";
    public static final String SERVER_XML_WEB_APP_NAME = "serverXMLWebApp";
    public static final String MAP_ENV_VAR_APP_NAME = "mapEnvVarApp";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = DuplicateInServerXMLServlet.class, contextRoot = DUPLICATE_IN_SERVER_XML_APP_NAME),
                    @TestServlet(servlet = ServerXMLServlet.class, contextRoot = SERVER_XML_APP_NAME),
                    @TestServlet(servlet = ServerXMLWebAppServlet.class, contextRoot = SERVER_XML_WEB_APP_NAME),
                    @TestServlet(servlet = MapEnvVarServlet.class, contextRoot = MAP_ENV_VAR_APP_NAME) })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME, MicroProfileActions.MP50, MicroProfileActions.MP41, MicroProfileActions.MP14);

    @BeforeClass
    public static void setUp() throws Exception {
        DeployOptions[] options = { DeployOptions.SERVER_ONLY };
        ShrinkHelper.defaultApp(server, SERVER_XML_APP_NAME, options, "com.ibm.ws.microprofile.config13.serverXML.*");
        ShrinkHelper.defaultApp(server, DUPLICATE_IN_SERVER_XML_APP_NAME, options, "com.ibm.ws.microprofile.config13.duplicateInServerXML.*");
        ShrinkHelper.defaultApp(server, SERVER_XML_WEB_APP_NAME, options, "com.ibm.ws.microprofile.config13.serverXMLWebApp.*");
        ShrinkHelper.defaultApp(server, MAP_ENV_VAR_APP_NAME, options, "com.ibm.ws.microprofile.config13.mapEnvVar.*");

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
