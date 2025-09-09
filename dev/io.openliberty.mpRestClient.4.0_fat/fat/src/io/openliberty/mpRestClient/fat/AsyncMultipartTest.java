/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mpRestClient.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import io.openliberty.mpRestClient.fat.multipartClient.AsyncMultipartClientTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test for asynchronous multipart form data with MP Rest Client
 * This test is designed to reproduce the customer issue with asynchronous multipart requests
 */
@RunWith(FATRunner.class)
public class AsyncMultipartTest extends FATServletClient {

    final static String REMOTE_SERVER_NAME = "io.openliberty.mpRestClient.fat.multipartRemote";
    final static String SERVER_NAME = "io.openliberty.mpRestClient.fat.asyncMultipartLocal";

    private static final String appName = "multipartClientApp";

    @Server(SERVER_NAME)
    @TestServlet(servlet = AsyncMultipartClientTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @Server(REMOTE_SERVER_NAME)
    public static LibertyServer remoteAppServer;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(remoteAppServer, "multipart", "io.openliberty.mpRestClient.fat.multipart");
        remoteAppServer.startServer();
        ShrinkHelper.defaultDropinApp(server, appName, "io.openliberty.mpRestClient.fat.multipartClient");
  
        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer("AsyncMultipart.log", true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void teardown() throws Exception {
        if (server != null) {
            server.stopServer();  //ignore server quiesce timeouts due to slow test machines
        }
        if (remoteAppServer != null) {
            remoteAppServer.stopServer();  //ignore server quiesce timeouts due to slow test machines
        }
    }
}