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
package com.ibm.ws.jaxrs20.cdi12.fat.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class ResourceInfoAtStartupTest extends AbstractTest {

    public static final String ignore_message = "CWWKW1002W";

    @Server("com.ibm.ws.jaxrs20.cdi12.fat.resourceInfoAtStartup")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        appname = "resourceInfoAtStartup";
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname, "io.openliberty.resourceInfoAtStartup.test");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(ignore_message);
    }

    @Before
    public void preTest() {
        serverRef = server;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }

    @Test
    public void testLoadOnStartupResource1() throws Exception {
        String line = serverRef.waitForStringInLog("All Clients Finished", 300000 /* 5 minutes */);
        assertNotNull("Initial clients did not finish", line);
        assertEquals("Null return from ResourceInfo.getResourceClass in request filter",
                     0, serverRef.findStringsInLogs("Filter1(request) - resourceClass=null").size());
        assertEquals("Null return from ResourceInfo.getResourceClass in response filter",
                     0, serverRef.findStringsInLogs("Filter1(response) - resourceClass=null").size());
        //assertTrue("Failures detected in client runs", line.contains("Successful clients: 50"));
    }

}