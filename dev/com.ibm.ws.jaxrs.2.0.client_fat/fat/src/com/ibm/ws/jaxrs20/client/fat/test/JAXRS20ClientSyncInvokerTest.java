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
package com.ibm.ws.jaxrs20.client.fat.test;

import java.util.HashMap;
import java.util.Map;

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
public class JAXRS20ClientSyncInvokerTest extends AbstractTest {

    @Server("jaxrs20.client.JAXRS20ClientSyncInvokerTest")
    public static LibertyServer server;

    private final static String appname = "bookstore";
    private final static String syncInvokerTarget = appname + "/SyncInvokerTestServlet";

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname, "com.ibm.ws.jaxrs20.fat.bookstore");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
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
    public void testSyncInvoker_get1() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(syncInvokerTarget, "testSyncInvoker_get1", p, "Good book");
    }

    @Test
    public void testSyncInvoker_get2() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(syncInvokerTarget, "testSyncInvoker_get2", p, "Good book");
    }

    //@Test
    public void testSyncInvoker_get3() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(syncInvokerTarget, "testSyncInvoker_get3", p, "true");
    }

    @Test
    public void testSyncInvoker_post1() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(syncInvokerTarget, "testSyncInvoker_post1", p, "Test book");
    }

    @Test
    public void testSyncInvoker_post2() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(syncInvokerTarget, "testSyncInvoker_post2", p, "Test book2");
    }

    // @Test
    public void testSyncInvoker_post3() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(syncInvokerTarget, "testSyncInvoker_post3", p, "Test book3");
    }
}
