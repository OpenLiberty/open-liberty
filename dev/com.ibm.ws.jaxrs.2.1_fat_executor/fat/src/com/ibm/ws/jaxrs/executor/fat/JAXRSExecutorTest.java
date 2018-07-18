/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.executor.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.jaxrstest.JAXRSExecutorTestServlet;

@RunWith(FATRunner.class)
public class JAXRSExecutorTest extends FATServletClient {
    private static final String SERVLET_PATH = "jaxrsapp/JAXRSExecutorTestServlet";

    @Server("com.ibm.ws.jaxrs.2.1.fat.executor")
    @TestServlet(servlet = JAXRSExecutorTestServlet.class, path = SERVLET_PATH)
    public static LibertyServer server;

    private static final String appName = "jaxrsapp";

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, appName + ".war")
                        .addPackage("web.jaxrstest");
        ShrinkHelper.exportAppToServer(server, app);

        server.addInstalledAppForValidation(appName);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    // Example
    //@Test
    //public void test1() throws Exception {
    //    runTest(server, SERVLET_PATH, testName.getMethodName());
    //}
}
