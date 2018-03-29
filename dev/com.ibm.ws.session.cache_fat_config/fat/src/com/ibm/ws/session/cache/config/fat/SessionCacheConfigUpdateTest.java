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

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.HttpSessionCache;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class SessionCacheConfigUpdateTest extends FATServletClient {

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
     * After running each test, restore to the original configuration.
     */
    @After
    public void cleanUpPerTest() throws Exception {
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(savedConfig);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, cleanupList);
        cleanupList = EMPTY_RECYCLE_LIST;
        System.out.println("server configuration restored");
    }

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "session.cache.web");

        String configLocation = new File(server.getUserDir() + "/shared/resources/hazelcast/hazelcast-localhost-only.xml").getAbsolutePath();
        server.setJvmOptions(Arrays.asList("-Dhazelcast.config=" + configLocation,
                                           "-Dhazelcast.group.name=" + UUID.randomUUID()));

        savedConfig = server.getServerConfiguration().clone();
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("SRVE0297E.*IllegalStateException"); // TODO remove this temporarily allowed error once OSGi dependencies in session manager code are fixed
    }

    /**
     * Update the configured value of the writeContents attribute while the server is running. Confirm the configured behavior.
     */
    @Test
    public void testWriteContents() throws Exception {
        // Verify default behavior: writeContents=ONLY_SET_ATTRIBUTES
        FATSuite.run(server, APP_NAME + '/' + SERVLET_NAME, "testWriteContents_ONLY_SET_ATTRIBUTES", new ArrayList<>());

        // Reconfigure writeContents=GET_AND_SET_ATTRIBUTES
        ServerConfiguration config = server.getServerConfiguration();
        HttpSessionCache httpSessionCache = config.getHttpSessionCaches().get(0);
        httpSessionCache.setWriteContents("GET_AND_SET_ATTRIBUTES");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, EMPTY_RECYCLE_LIST);

        FATSuite.run(server, APP_NAME + '/' + SERVLET_NAME, "testWriteContents_GET_AND_SET_ATTRIBUTES", new ArrayList<>());

        // Reconfigure writeContents=ALL_SESSION_ATTRIBUTES
        httpSessionCache.setWriteContents("ALL_SESSION_ATTRIBUTES");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, EMPTY_RECYCLE_LIST);

        FATSuite.run(server, APP_NAME + '/' + SERVLET_NAME, "testWriteContents_ALL_SESSION_ATTRIBUTES", new ArrayList<>());
    }

    /**
     * Update the configured value of the writeFrequency attribute from default (END_OF_SERVLET_SERVICE) to MANUAL_UPDATE
     * while the server is running. The session must remain valid, and must exhibit the new behavior (MANUAL_UPDATE) after
     * the configuration change.
     */
    @Test
    public void testWriteFrequency() throws Exception {
        // Verify default behavior: writeFrequency=END_OF_SERVLET_SERVICE
        List<String> session = new ArrayList<>();
        FATSuite.run(server, APP_NAME + '/' + SERVLET_NAME, "testSetAttribute&attribute=testWriteFrequency&value=1_END_OF_SERVLET_SERVICE", session);
        FATSuite.run(server, APP_NAME + '/' + SERVLET_NAME, "testCacheContains&attribute=testWriteFrequency&value=1_END_OF_SERVLET_SERVICE", session);

        // Reconfigure writeFrequency=MANUAL_UPDATE
        ServerConfiguration config = server.getServerConfiguration();
        HttpSessionCache httpSessionCache = config.getHttpSessionCaches().get(0);
        httpSessionCache.setWriteFrequency("MANUAL_UPDATE");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, EMPTY_RECYCLE_LIST);

        // Set a new attribute value without performing a manual sync, the value in the cache should not be updated
        FATSuite.run(server, APP_NAME + '/' + SERVLET_NAME, "testSetAttribute&attribute=testWriteFrequency&value=2_MANUAL_UPDATE", session);
        FATSuite.run(server, APP_NAME + '/' + SERVLET_NAME, "testCacheContains&attribute=testWriteFrequency&value=1_END_OF_SERVLET_SERVICE", session);

        // TODO enable if manual sync is supported across same session spanning multiple servlet requests:
        // Perform a manual sync under a subsequent servlet request and verify the previously set value is updated
        // FATSuite.run(server, APP_NAME + '/' + SERVLET_NAME, "testManualSync&attribute=testWriteFrequency&value=2_MANUAL_UPDATE", session);

        // Perform a manual update within the same servlet request
        FATSuite.run(server, APP_NAME + '/' + SERVLET_NAME, "testManualUpdate&attribute=testWriteFrequency&value=3_MANUAL_UPDATE", session);
        FATSuite.run(server, APP_NAME + '/' + SERVLET_NAME, "invalidateSession", session);
    }

    /**
     * Update the configured value of the writeInterval attribute while the server is running.
     */
    @AllowedFFDC("java.lang.IllegalStateException") // TODO remove this temporarily allowed error once OSGi dependencies in session manager code are fixed
    @Test
    public void testWriteInterval() throws Exception {
        // Verify default behavior: writeFrequency=END_OF_SERVLET_SERVICE, writeInterval ignored
        List<String> session = new ArrayList<>();
        FATSuite.run(server, APP_NAME + '/' + SERVLET_NAME, "testSetAttribute&attribute=testWriteInterval&value=0_END_OF_SERVLET_SERVICE", session);
        FATSuite.run(server, APP_NAME + '/' + SERVLET_NAME, "testCacheContains&attribute=testWriteInterval&value=0_END_OF_SERVLET_SERVICE", session);

        // Reconfigure writeFrequency=TIME_BASED_WRITE and writeInterval=5s
        ServerConfiguration config = server.getServerConfiguration();
        HttpSessionCache httpSessionCache = config.getHttpSessionCaches().get(0);
        httpSessionCache.setWriteFrequency("TIME_BASED_WRITE");
        httpSessionCache.setWriteInterval("5s");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, EMPTY_RECYCLE_LIST);

        // Set a new attribute value and verify that it does not get persisted upon the end of the servlet request.
        // This might require retries because periodic timed based write could happen right as the servlet request ends.
        String previousValue = "0_END_OF_SERVLET_SERVICE";
        String newValue = null;
        for (int numAttempts = 1; numAttempts < 20; numAttempts++) {
            newValue = numAttempts + "_TIME_BASED_WRITE";
            FATSuite.run(server, APP_NAME + '/' + SERVLET_NAME, "testSetAttribute&attribute=testWriteFrequency&value=" + newValue, session);

            String response = FATSuite.run(server, APP_NAME + '/' + SERVLET_NAME, "getValueFromCache&attribute=testWriteFrequency", session);
            int start = response.indexOf("value from cache: [") + 19;
            String cachedValue = response.substring(start, response.indexOf(']', start));

            if (!previousValue.equals(cachedValue))
                break;

            previousValue = newValue;
        }

        assertFalse("TIME_BASED_WRITE was either not honored, or the test was very unlucky in repeatedly " +
                    "having the time based write align with servlet request completion",
                    previousValue.equals(newValue));

        String response = FATSuite.run(server, APP_NAME + '/' + SERVLET_NAME, "getSessionId", session);
        int start = response.indexOf("session id: [") + 13;
        String sessionId = response.substring(start, response.indexOf(']', start));

        // Due to TIME_BASED_WRITE, the value should be written to cache some time within the next 5 seconds. Poll for it,
        FATSuite.run(server, APP_NAME + '/' + SERVLET_NAME,
                     "testPollCache&attribute=testWriteFrequency&value=" + newValue + "&sessionId=" + sessionId,
                     null); // Avoid having the servlet access the session here because this will block 5 cycles of the time based write.

        FATSuite.run(server, APP_NAME + '/' + SERVLET_NAME, "invalidateSession", session);
    }
}
