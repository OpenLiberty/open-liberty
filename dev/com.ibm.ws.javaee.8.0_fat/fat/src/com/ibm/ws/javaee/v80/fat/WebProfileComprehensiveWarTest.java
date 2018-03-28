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
package com.ibm.ws.javaee.v80.fat;

import javax.servlet.http.HttpServlet;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import javaee8.web.WebProfile8TestServlet;

@RunWith(FATRunner.class)
public class WebProfileComprehensiveWarTest extends FATServletClient {

    public static final String SERVER_NAME = "javaee8Web.comprehensiveWar";

    public static final String WAR_NAME = "comprehensiveWar.war";
    public static final String CONTEXT_ROOT = "comprehensiveWar";
    public static final String[] WAR_PACKAGE_NAMES =
        new String[] {
            "javaee8.web",
            "javaee8.web.cdi",
            "javaee8.web.jaxrs",
            "javaee8.web.jpa",
            "javaee8.web.jsonb" };

    @Server(SERVER_NAME)
    @TestServlet(servlet = WebProfile8TestServlet.class, contextRoot = CONTEXT_ROOT)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        FATServerHelper.addWarToServer(
            server, FATServerHelper.DROPINS_DIR,
            WAR_NAME, WAR_PACKAGE_NAMES, FATServerHelper.DO_ADD_RESOURCES); // throws Exception

        server.startServer();

        server.waitForStringInLog("CWWKZ0001I.* " + WAR_NAME, 10000);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
