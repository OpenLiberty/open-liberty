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

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jaxrs21sse.jsonb.SseJsonbTestServlet;

/**
 * This test of jsonb SSE function.
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 8)
public class SseJsonbTest extends FATServletClient {
    private static final String SERVLET_PATH = "SseJsonbApp/SseJsonbTestServlet";

    @Server("com.ibm.ws.jaxrs21.sse.jsonb")
    @TestServlet(servlet = SseJsonbTestServlet.class, path = SERVLET_PATH)
    public static LibertyServer server;

    private static final String appName = "SseJsonbApp";

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, appName + ".war")
                        .addPackage("jaxrs21sse.jsonb");
        ShrinkHelper.exportAppToServer(server, app);

        server.addInstalledAppForValidation(appName);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testJsonSse() throws Exception {
        runTest(server, SERVLET_PATH, "testJsonSse");
    }

    @Test
    public void testJsonbSse() throws Exception {
        runTest(server, SERVLET_PATH, "testJsonbSse");
    }
}
