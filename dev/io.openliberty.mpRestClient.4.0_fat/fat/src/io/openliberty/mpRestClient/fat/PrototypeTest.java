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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import io.openliberty.mpRestClient.fat.prototypeClient.BasicClientTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/*
 * The purpose of this test is to provide an empty canvas for rapid/easy test experimentation,
 * as well as providing and example of FAT best practices.
 *
 * This Test should never have any real tests, if you use this Test to create a test that should
 * be added permanently, create a new FAT Test using this test as a template.
 */
@RunWith(FATRunner.class)
public class PrototypeTest extends FATServletClient {

    final static String REMOTE_SERVER_NAME = "io.openliberty.mpRestClient.fat.prototypeRemote";
    final static String SERVER_NAME = "io.openliberty.mpRestClient.fat.prototypeLocal";


    private static final String appName = "prototypeClientApp";

    // Third party libs are copied to ${buildDir}/autoFVT/appLibs/prototype in build.gradle
    private static final String libs = "appLibs/prototype";


    @Server(SERVER_NAME)
    @TestServlet(servlet = BasicClientTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @Server(REMOTE_SERVER_NAME)
    public static LibertyServer remoteAppServer;

    @BeforeClass
    public static void setup() throws Exception {
         ShrinkHelper.defaultDropinApp(remoteAppServer, "prototype", "io.openliberty.mpRestClient.fat.prototype");
        remoteAppServer.startServer();
         ShrinkHelper.defaultDropinApp(server, appName, "io.openliberty.mpRestClient.fat.prototypeClient");
  
        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer("Prototype.log", true);
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
