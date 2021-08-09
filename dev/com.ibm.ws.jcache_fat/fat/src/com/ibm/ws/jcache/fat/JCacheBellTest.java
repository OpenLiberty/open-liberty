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
package com.ibm.ws.jcache.fat;

import java.util.Arrays;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jcache.web.JCacheTestServlet;

/**
 * Test the basic functionality of JCache, to ensure the jcacheContainer feature works.
 */
@RunWith(FATRunner.class)
public class JCacheBellTest extends FATServletClient {

    public static final String APP_NAME = "jcache";

    @Server("jcacheContainerServerBell")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "jcache.web");

        server.setJvmOptions(Arrays.asList("-Dhazelcast.group.name=" + UUID.randomUUID()));
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    //TODO: Disabling all tests until the hazelcast config for the bucket is fixed and Annotations are addressed.
    @Test
    public void mustHaveTest() throws Exception {
        FATServletClient.runTest(server, "jcache/JCacheTestServlet", testName.getMethodName());
    }

    //@Test
    public void basicJCacheTest() throws Exception {
        FATServletClient.runTest(server, "jcache/JCacheTestServlet", testName.getMethodName());
    }

    @Test // parts of test are disabled to reflect lack of support for jcache annotations
    public void testAnnotations() throws Exception {
        FATServletClient.runTest(server, "jcache/JCacheTestServlet", testName.getMethodName());
    }

    //@Test
    public void testCloseAndReopen() throws Exception {
        FATServletClient.runTest(server, "jcache/JCacheTestServlet", testName.getMethodName());
    }

    //@Test
    public void testEntryProcessor() throws Exception {
        FATServletClient.runTest(server, "jcache/JCacheTestServlet", testName.getMethodName());
    }
}
