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
package com.ibm.ws.java11_fat;

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class Java11Test extends FATServletClient {

    private static final String APP_NAME = "java11-app";

    @Server("java11_fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // NOTE: This FAT uses a pre-compiled application because it uses APIs that are new in Java 9 and cannot
        // be built normally since our build still requires Java 8.  Source can be found under src-reference
        server.addInstalledAppForValidation(APP_NAME);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testApplicationModule() throws Exception {
        String appResponse = HttpUtils.getHttpResponseAsString(server, APP_NAME + '/');
        assertContains(appResponse, "<<< EXIT SUCCESSFUL");
        assertContains(appResponse, "This class is in: unnamed module");
    }

    private static void assertContains(String str, String lookFor) {
        assertTrue("Expected to find string '" + lookFor + "' but it was not found in the string: " + str, str.contains(lookFor));
    }

}
