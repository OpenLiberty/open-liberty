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
package com.ibm.ws.springboot.support.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class SimpleTest15 {

    protected static final Class<?> c = SimpleTest15.class;

    @Server("com.ibm.ws.springboot.support.fat.SimpleTest15")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {}

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKE0700W");
    }

    @Test
    public void testBasicSpringBootApplication() throws Exception {
        File f = new File(server.getServerRoot() + "/dropins/spr/");
        assertTrue("file does not exist", f.exists());
        server.startServer(true, false);
        assertNotNull("The application was not installed", server
                        .waitForStringInLog("CWWKZ0001I:.*"));
        // NOTE we set the port to the expected port according to the test application.properties
        server.setHttpDefaultPort(8081);
        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");
    }

}
