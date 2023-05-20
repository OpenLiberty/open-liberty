/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.java.internal.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.web.IllegalAccessTestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class JavaIllegalAccessTest extends FATServletClient {

    public static final String APP_NAME = "illegalaccess";
    public static final String SERVLET_NAME = "IllegalAccessTestServlet";

    @Server("java-illegal-access-exception-server")
    @TestServlet(servlet = IllegalAccessTestServlet.class, path = APP_NAME + '/' + SERVLET_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "test.web");
        server.addInstalledAppForValidation(APP_NAME);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
