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
package com.ibm.ws.jaxrs20.cdi12.fat.test;

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
public class InterceptorTest extends AbstractTest {

    @Server("com.ibm.ws.jaxrs20.cdi12.fat.interceptor")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        appname = "interceptor";
        ShrinkHelper.defaultDropinApp(server, appname, "com.ibm.ws.jaxrs20.cdi12.fat.interceptor");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKW1002W");
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
    public void testCDIInterceptorsInvokedAroundResourceAndProviderMethods() throws Exception {
        runGetMethod("/resource/mxyzptlk", 200, "kltpzyxm", true);
        assertLibertyMessage("Filter filter Entering", 2, "equal");
        assertLibertyMessage("inside request filter method", 1, "equal");
        assertLibertyMessage("inside response filter method", 1, "equal");
        assertLibertyMessage("Filter filter Exiting", 2, "equal");
        assertLibertyMessage("Resource reverse Entering mxyzptlk", 1, "equal");
        assertLibertyMessage("Resource reverse Exiting kltpzyxm", 1, "equal");
    }
}