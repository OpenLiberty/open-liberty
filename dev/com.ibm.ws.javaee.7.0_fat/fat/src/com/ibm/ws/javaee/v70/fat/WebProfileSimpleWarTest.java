/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.v70.fat;

import javax.servlet.http.HttpServlet;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

// import testservlet31.web.servlets.SimpleClassesServlet;
import testservlet31.jar.servlets.SimpleFragmentServlet;

/**
 * Test of Servlet 3.1 parsing using the 'webProfile-7.0' feature.
 *
 * This test puts together a server definition which uses feature
 * 'webProfile-7.0' with a simple WAR which uses the Servlet 3.1 schema.
 *
 * The expected test result is that the WAR starts with no errors.
 *
 * The WAR packages a fragment JAR.  Both the WAR and the fragment JAR
 * package a servlet (SimpleClassesServlet and SimpleFragmentServlet),
 * however, the test only configures the fragment servlet.
 */
@RunWith(FATRunner.class)
public class WebProfileSimpleWarTest extends FATServletClient implements FATAppConstants {

    @Server(JAVA7_WEB_SIMPLE_WAR_SERVER_NAME)
    @TestServlet(servlet = SimpleFragmentServlet.class,
                 contextRoot = SIMPLE_WAR_CONTEXT_ROOT)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        FATServerHelper.addWarToServer(
            server, FATServerHelper.DROPINS_DIR,
            SIMPLE_WAR_NAME, SIMPLE_WAR_PACKAGE_NAMES, SIMPLE_WAR_ADD_RESOURCES,
            SIMPLE_JAR_NAME, SIMPLE_JAR_PACKAGE_NAMES, SIMPLE_JAR_ADD_RESOURCES); // throws Exception

        server.startServer();

        server.waitForStringInLog("CWWKZ0001I.* " + SIMPLE_WAR_NAME, 10000);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
