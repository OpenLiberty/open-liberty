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

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
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
public class JCacheTest extends FATServletClient {

    public static final String APP_NAME = "jcache";

    @Server("jcacheContainerServer")
    @TestServlet(servlet = JCacheTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "jcache.web");

        String configLocation = new File(server.getUserDir() + "/shared/resources/hazelcast/hazelcast-localhost-only.xml").getAbsolutePath();
        server.setJvmOptions(Arrays.asList("-Dhazelcast.group.name=" + UUID.randomUUID(),
                                           "-Dhazelcast.config=" + configLocation));

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
