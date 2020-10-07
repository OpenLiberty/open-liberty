/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.java11_fat;

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 14)
@MaximumJavaLevel(javaLevel = 14)
public class Java14Test extends FATServletClient {

    private static final String APP_NAME = "java14-app";

    @Server("java11_fat-java14-server")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // NOTE: This FAT uses a pre-compiled application which is compiled at bytecode level
        // of JDK 14, which is higher than what our build systems normally use
        // Source for the app can be found here: https://github.com/aguibert/basic-liberty/tree/java14-app/src/main
        server.addInstalledAppForValidation(APP_NAME);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWJP9991W"); // warnings for JPA's auto drop/create tables
    }

    @Test
    public void testJava14App() throws Exception {
        String appResponse = HttpUtils.getHttpResponseAsString(server, APP_NAME + '/');
        assertContains(appResponse, "<<< EXIT SUCCESSFUL");
    }

    private static void assertContains(String str, String lookFor) {
        assertTrue("Expected to find string '" + lookFor + "' but it was not found in the string: " + str, str.contains(lookFor));
    }

}
