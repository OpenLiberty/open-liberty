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

import static org.junit.Assert.assertNotNull;

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
 * Test of Application 7 and Servlet 3.1 parsing using the 'javaee-7.0'
 * feature.
 *
 * This test puts together a server definition which uses feature
 * 'javaee-7.0' with a simple EAR which uses Application 7 and which
 * contains a Servlet 3.1 WAR.
 *
 * The expected test result is that the EAR and WAR start with no errors.
 *
 * The WAR packages a fragment JAR.  Both the WAR and the fragment JAR
 * package a servlet (SimpleClassesServlet and SimpleFragmentServlet),
 * however, the test only configures the fragment servlet.
 */
@RunWith(FATRunner.class)
public class FullProfileSimpleEarTest extends FATServletClient implements FATAppConstants {

    @Server(JAVA7_FULL_SIMPLE_EAR_SERVER_NAME)
    @TestServlet(servlet = SimpleFragmentServlet.class,
                 contextRoot = SIMPLE_WAR_CONTEXT_ROOT)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        FATServerHelper.addToServer(
            server, FATServerHelper.DROPINS_DIR,
            SIMPLE_EAR_NAME, SIMPLE_EAR_ADD_RESOURCES,
            SIMPLE_WAR_NAME, SIMPLE_WAR_PACKAGE_NAMES, SIMPLE_WAR_ADD_RESOURCES,
            SIMPLE_JAR_NAME, SIMPLE_JAR_PACKAGE_NAMES, SIMPLE_JAR_ADD_RESOURCES);
        // throws Exception

        server.startServer();

        assertNotNull(
            "Message was not detected in the log",
            server.waitForStringInLog("CWWKZ0001I.* " + SIMPLE_WAR_NAME, 10000));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
