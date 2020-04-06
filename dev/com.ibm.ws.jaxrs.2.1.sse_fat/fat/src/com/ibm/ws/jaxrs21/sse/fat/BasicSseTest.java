/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.sse.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jaxrs21sse.basic.BasicSseTestServlet;

/**
 * This test of basic SSE function.
 */
@RunWith(FATRunner.class)
public class BasicSseTest extends FATServletClient {
    private static final String SERVLET_PATH = "BasicSseApp/BasicSseTestServlet";

    @Server("com.ibm.ws.jaxrs21.sse.basic")
    @TestServlet(servlet = BasicSseTestServlet.class, path = SERVLET_PATH)
    public static LibertyServer server;

    private static final String appName = "BasicSseApp";

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, appName + ".war")
                        .addPackage("jaxrs21sse.basic");
        ShrinkHelper.exportAppToServer(server, app);

        server.addInstalledAppForValidation(appName);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer( "CWWKE1102W", "CWWKE1106W" , "CWWKE1107W");
    }

    @Test
    public void testSimpleDirectTextPlainSse() throws Exception {
        runTest(server, SERVLET_PATH, "testSimpleDirectTextPlainSse");
    }

    @Test
    public void testIntegerSse() throws Exception {
        runTest(server, SERVLET_PATH, "testIntegerSse");
    }

    @Test
    public void testJaxbSse() throws Exception {
        runTest(server, SERVLET_PATH, "testJaxbSse");
    }

    @Test
    public void testJsonSse() throws Exception {
        runTest(server, SERVLET_PATH, "testJsonSse");
    }

    @Test
    public void testSseWithRX() throws Exception {
        runTest(server, SERVLET_PATH, "testSseWithRX");
    }

    @Test
    public void testErrorSse() throws Exception {
        runTest(server, SERVLET_PATH, "testErrorSse");
    }
}
