/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jee.internal.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.testservlet.internal.jar.servlets.SimpleFragmentServlet;

/**
 * Test of Servlet 3.1 parsing using the 'jeeTestFeature-1.0' feature.
 *
 * This test puts together a server definition which uses feature
 * 'jeeTestFeature-1.0' with a simple WAR which uses the Servlet 3.1
 * schemas.
 *
 * The expected test result is that the WAR starts with no errors.
 *
 * The WAR packages a fragment JAR. Both the WAR and the fragment JAR
 * package a servlet (SimpleClassesServlet and SimpleFragmentServlet),
 * however, the test only configures the fragment servlet.
 */
@RunWith(FATRunner.class)
public class SimpleWarTest extends FATServletClient implements FATAppConstants {

    @Server(SIMPLE_WAR_SERVER_NAME)
    @TestServlet(servlet = SimpleFragmentServlet.class,
                 contextRoot = SIMPLE_WAR_CONTEXT_ROOT)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        FATServerHelper.installUserFeature(server);
        FATServerHelper.addWarToServer(
                                       server, FATServerHelper.DROPINS_DIR,
                                       SIMPLE_WAR_NAME, SIMPLE_WAR_PACKAGE_NAMES, SIMPLE_WAR_ADD_RESOURCES,
                                       SIMPLE_JAR_NAME, SIMPLE_JAR_PACKAGE_NAMES, SIMPLE_JAR_ADD_RESOURCES);

        server.startServer();

        server.waitForStringInLog("CWWKZ0001I.* " + SIMPLE_WAR_NAME, 10000);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
        FATServerHelper.uninstallUserFeature(server);
    }
}
