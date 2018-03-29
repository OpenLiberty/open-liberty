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
package com.ibm.ws.session.cache.config.fat;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class SessionCacheErrorPathsTest extends FATServletClient {

    private static final String APP_NAME = "sessionCacheConfigApp";
    private static final Set<String> APP_NAMES = Collections.singleton(APP_NAME);
    private static final String[] APP_RECYCLE_LIST = new String[] { "CWWKZ0009I.*" + APP_NAME, "CWWKZ000[13]I.*" + APP_NAME };
    private static final String[] EMPTY_RECYCLE_LIST = new String[0];
    private static final String SERVLET_NAME = "SessionCacheConfigTestServlet";

    private static String[] cleanupList = EMPTY_RECYCLE_LIST;

    private static ServerConfiguration savedConfig;

    @Server("sessionCacheServer")
    public static LibertyServer server;

    /**
     * After running each test, ensure the server is stopped and restore the original configuration.
     */
    @After
    public void cleanUpPerTest() throws Exception {
        if (server.isStarted())
            try {
                server.stopServer();
            } finally {
                server.updateServerConfiguration(savedConfig);
            }
        System.out.println("server configuration restored");
    }

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "session.cache.web");

        String configLocation = new File(server.getUserDir() + "/shared/resources/hazelcast/hazelcast-localhost-only.xml").getAbsolutePath();
        server.setJvmOptions(Arrays.asList("-Dhazelcast.config=" + configLocation,
                                           "-Dhazelcast.group.name=" + UUID.randomUUID()));

        savedConfig = server.getServerConfiguration().clone();
    }

    /**
     * Add the sessionCache-1.0 feature while the server is running. Access a session before and after,
     * verifying that a session attribute added afterward is persisted, whereas a session attribute added before
     * (in absence of sessionCache-1.0 feature) is not.
     */
    @AllowedFFDC("java.net.MalformedURLException") // TODO possible bug
    @Test
    public void testAddFeature() throws Exception {
        // Start the server with sessionCache-1.0 disabled
        ServerConfiguration config = savedConfig.clone();
        config.getFeatureManager().getFeatures().remove("sessionCache-1.0");
        server.updateServerConfiguration(config);
        server.startServer();

        // Session manager should warn user that sessions will be stored in memory
        assertEquals(1, server.findStringsInLogs("SESN8501I").size());

        List<String> session = new ArrayList<>();
        FATSuite.run(server, APP_NAME + '/' + SERVLET_NAME, "testSetAttribute&attribute=testAddFeature1&value=AF1", session);

        // Add the sessionCache-1.0 feature
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(savedConfig);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, EMPTY_RECYCLE_LIST);

        FATSuite.run(server, APP_NAME + '/' + SERVLET_NAME, "testSetAttribute&attribute=testAddFeature2&value=AF2", session);

        // second value should be written to the cache
        FATSuite.run(server, APP_NAME + '/' + SERVLET_NAME, "testCacheContains&attribute=testAddFeature2&value=AF2", session);

        // first value should not be written to the cache
        FATSuite.run(server, APP_NAME + '/' + SERVLET_NAME, "testCacheContains&attribute=testAddFeature1&value=null", session);

        FATSuite.run(server, APP_NAME + '/' + SERVLET_NAME, "invalidateSession", session);
    }
}
