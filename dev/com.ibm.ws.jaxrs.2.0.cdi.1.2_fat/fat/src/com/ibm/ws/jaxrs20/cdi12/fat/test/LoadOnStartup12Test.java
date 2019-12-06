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
public class LoadOnStartup12Test extends AbstractTest {

    public static final String[] ignore_messages =  new String[] { "CWWKW1002W" , "CWWKE1102W", "CWWKE1106W" , "CWWKE1107W" };

    @Server("com.ibm.ws.jaxrs20.cdi12.fat.loadonstartup")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        appname = "loadonstartup";
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname, "com.ibm.ws.jaxrs20.cdi12.fat.loadonstartup");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(ignore_messages);
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
        runGetMethod("/startup1/resource", 200, "ok", true);
    }

    @Test
    public void testLoadOnStartupResource2() throws Exception {
        runGetMethod("/startup2/resource", 200, "ok", true);
    }

    @Test
    public void testLoadOnStartupResource3() throws Exception {
        runGetMethod("/startup3/resource", 200, "ok", true);
    }

    @Test
    public void testLoadOnStartupResource4() throws Exception {
        runGetMethod("/startup4/resource", 200, "ok", true);
    }

    @Test
    public void testLoadOnStartupResourceMultiple1() throws Exception {
        server.stopServer(ignore_messages);
        server.startServer(true);
        runGetMethod("/startup4/resource", 200, "ok", true);
        runGetMethod("/startup3/resource", 200, "ok", true);
        runGetMethod("/startup2/resource", 200, "ok", true);
        runGetMethod("/startup1/resource", 200, "ok", true);
    }

    @Test
    public void testLoadOnStartupResourceMultiple2() throws Exception {
        server.stopServer(ignore_messages);
        server.startServer(true);
        runGetMethod("/startup3/resource", 200, "ok", true);
        runGetMethod("/startup2/resource", 200, "ok", true);
        runGetMethod("/startup4/resource", 200, "ok", true);
        runGetMethod("/startup1/resource", 200, "ok", true);
    }
}