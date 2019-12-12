/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.cache.config.fat.infinispan;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.HttpSessionCache;
import com.ibm.websphere.simplicity.config.Monitor;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class SessionCacheConfigUpdateTest extends FATServletClient {

    private static final String APP_DEFAULT = "sessionCacheConfigApp";
    private static final Set<String> APP_NAMES = Collections.singleton(APP_DEFAULT); // jcacheApp not included because it isn't normally configured
    private static final String[] EMPTY_RECYCLE_LIST = new String[0];
    private static final String SERVLET_NAME = "SessionCacheConfigTestServlet";

    private static String[] cleanupList = EMPTY_RECYCLE_LIST;

    private static ServerConfiguration savedConfig;

    @Server("com.ibm.ws.session.cache.fat.config.infinispan")
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

        // In addition to starting the application, must also wait for asynchronous web module initialization to complete,
        // otherwise tests which attempt a configuration update could end up triggering a deactivate and close of the CachingProvider
        // while the servlet initialization code is still attempting to use the CachingProvider and/or the CacheManager and Caches that it creates.
        List<String> session = new ArrayList<>();
        run("getSessionId", session);
        run("invalidateSession", session);

        System.out.println("server configuration restored");
    }

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_DEFAULT, "session.cache.infinispan.web");

        savedConfig = server.getServerConfiguration().clone();
        String rand = UUID.randomUUID().toString();
        Map<String, String> options = server.getJvmOptionsAsMap();
        options.put("-Dinfinispan.cluster.name", rand);
        server.setJvmOptions(options);
        server.startServer();

        // In addition to starting the application, must also wait for asynchronous web module initialization to complete,
        // otherwise tests which attempt a configuration update could end up triggering a deactivate and close of the CachingProvider
        // while the servlet initialization code is still attempting to use the CachingProvider and/or the CacheManager and Caches that it creates.
        List<String> session = new ArrayList<>();
        run("getSessionId", session);
        run("invalidateSession", session);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // If a config update occurred just prior to this and didn't have subseuqent use of a session,
        // then http sessions code could be initializing Infinispan asynchronously, which, if that is still ongoing,
        // can result in errors if a server stop happens at the same time.  To avoid this, first run an operation
        // that will wait for the intialization to complete.
        try {
            List<String> session = new ArrayList<>();
            run("getSessionId", session);
        } finally {
            server.stopServer();
        }
    }

    /**
     * Enable and disable monitoring for sessions while the server is running.
     */
    @Test
    public void testMonitoring() throws Exception {
        run("testMXBeansNotEnabled", new ArrayList<>());

        // add monitor-1.0 feature
        ServerConfiguration config = server.getServerConfiguration();
        config.getFeatureManager().getFeatures().add("monitor-1.0");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, EMPTY_RECYCLE_LIST);

        run("testMXBeansEnabled", new ArrayList<>());

        // add monitor configuration that doesn't include Session
        Monitor monitor = new Monitor();
        monitor.setFilter("ThreadPool,WebContainer");
        config.getMonitors().add(monitor);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, EMPTY_RECYCLE_LIST);

        run("testMXBeansNotEnabled", new ArrayList<>());

        // switch to monitor configuration that includes Session
        monitor.setFilter("ThreadPool,WebContainer,Session");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, EMPTY_RECYCLE_LIST);

        run("testMXBeansEnabled", new ArrayList<>());

        // remove monitor-1.0 feature (and monitor config)
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(savedConfig);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, EMPTY_RECYCLE_LIST);

        run("testMXBeansNotEnabled", new ArrayList<>());
    }

    @Test
    public void testScheduleInvalidation() throws Exception {
        // Choose hours that are far away from when the test is running so that invalidation doesn't accidentally run during the test.
        int hour = ZonedDateTime.now().getHour();
        int hour1 = (hour + 8) % 24;
        int hour2 = (hour + 16) % 24;

        ServerConfiguration config = server.getServerConfiguration();
        HttpSessionCache httpSessionCache = config.getHttpSessionCaches().get(0);
        httpSessionCache.setScheduleInvalidationFirstHour(Integer.toString(hour1));
        httpSessionCache.setScheduleInvalidationSecondHour(Integer.toString(hour2));
        httpSessionCache.setWriteFrequency("TIME_BASED_WRITE");
        httpSessionCache.setWriteInterval("15s");
        server.setMarkToEndOfLog(); // Only marks messages.log, does not mark the trace file
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, EMPTY_RECYCLE_LIST);
        String messageToCheckFor = "doScheduledInvalidation scheduled hours are " + Integer.toString(hour1) + " and " + Integer.toString(hour2);

        // Monitor trace.log and wait for the message "doScheduledInvalidation scheduled hours are X and Y" before the test should continue
        // This replaces the TimeUnit.SECONDS.sleep(10) used before
        assertNotNull("Could not find message \"" + messageToCheckFor
                      + "\" in the trace.log file.  Has the logging message in com.ibm.ws.session.store.common.BackedHashMap.doScheduledInvalidation() changed?",
                      server.waitForStringInTraceUsingMark(messageToCheckFor));

        ArrayList<String> session = new ArrayList<>();
        String response = run("testSetAttributeWithTimeout&attribute=testScheduleInvalidation&value=si1&maxInactiveInterval=1",
                              session);
        int start = response.indexOf("session id: [") + 13;
        String sessionId = response.substring(start, response.indexOf(']', start));

        // Wait until invalidation would normally have occurred
        TimeUnit.SECONDS.sleep(35);

        // confirm that invalidated data remains in the cache
        run("testCacheContains&attribute=testScheduleInvalidation&value=si1&sessionId=" + sessionId, null);

        // Add another attribute, but don't wait for it to be written
        response = run("testSetAttributeWithTimeout&attribute=testTimeBasedWriteNoSync&value=si2&maxInactiveInterval=60",
                       session);
        start = response.indexOf("session id: [") + 13;
        sessionId = response.substring(start, response.indexOf(']', start));
    }

    /**
     * Update the configured value of the writeContents attribute while the server is running. Confirm the configured behavior.
     */
    @Test
    public void testWriteContents() throws Exception {
        // Verify default behavior: writeContents=ONLY_SET_ATTRIBUTES
        run("testWriteContents_ONLY_SET_ATTRIBUTES", new ArrayList<>());

        // Reconfigure writeContents=GET_AND_SET_ATTRIBUTES
        ServerConfiguration config = server.getServerConfiguration();
        HttpSessionCache httpSessionCache = config.getHttpSessionCaches().get(0);
        httpSessionCache.setWriteContents("GET_AND_SET_ATTRIBUTES");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, EMPTY_RECYCLE_LIST);

        run("testWriteContents_GET_AND_SET_ATTRIBUTES", new ArrayList<>());

        // Reconfigure writeContents=ALL_SESSION_ATTRIBUTES
        httpSessionCache.setWriteContents("ALL_SESSION_ATTRIBUTES");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, EMPTY_RECYCLE_LIST);

        run("testWriteContents_ALL_SESSION_ATTRIBUTES", new ArrayList<>());
    }

    /**
     * Update the configured value of the writeFrequency attribute from default (END_OF_SERVLET_SERVICE) to MANUAL_UPDATE
     * while the server is running. Whether or not the session persists across restart when there is only a single member
     * is implementation-specific and not covered by the test case. After the update, however, a session must must exhibit
     * the MANUAL_UPDATE behavior.
     */
    @Test
    public void testWriteFrequency() throws Exception {
        // Verify default behavior: writeFrequency=END_OF_SERVLET_SERVICE
        List<String> session = new ArrayList<>();
        run("testSetAttribute&attribute=testWriteFrequency&value=1_END_OF_SERVLET_SERVICE", session);
        run("testCacheContains&attribute=testWriteFrequency&value=1_END_OF_SERVLET_SERVICE", session);

        // Reconfigure writeFrequency=MANUAL_UPDATE
        ServerConfiguration config = server.getServerConfiguration();
        HttpSessionCache httpSessionCache = config.getHttpSessionCaches().get(0);
        httpSessionCache.setWriteFrequency("MANUAL_UPDATE");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, EMPTY_RECYCLE_LIST);

        // Set a new attribute value without performing a manual sync, the value in the cache should not be updated
        session = new ArrayList<>();
        run("testSetAttribute&attribute=testWriteFrequency&value=2_MANUAL_UPDATE", session);
        run("testCacheContains&attribute=testWriteFrequency&value=null", session);

        // Perform a manual update within the same servlet request
        run("testManualUpdate&attribute=testWriteFrequency&value=3_MANUAL_UPDATE", session);
        run("invalidateSession", session);
    }

    /**
     * Update the configured value of the writeInterval attribute while the server is running.
     */
    @Test
    public void testWriteInterval() throws Exception {
        // Verify default behavior: writeFrequency=END_OF_SERVLET_SERVICE, writeInterval ignored
        List<String> session = new ArrayList<>();
        run("testSetAttribute&attribute=testWriteInterval&value=0_END_OF_SERVLET_SERVICE", session);
        run("testCacheContains&attribute=testWriteInterval&value=0_END_OF_SERVLET_SERVICE", session);

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
            run("testSetAttribute&attribute=testWriteFrequency&value=" + newValue, session);

            String response = run("getValueFromCache&attribute=testWriteFrequency", session);
            int start = response.indexOf("value from cache: [") + 19;
            String cachedValue = response.substring(start, response.indexOf(']', start));

            if (!previousValue.equals(cachedValue))
                break;

            previousValue = newValue;
        }

        assertFalse("TIME_BASED_WRITE was either not honored, or the test was very unlucky in repeatedly " +
                    "having the time based write align with servlet request completion",
                    previousValue.equals(newValue));

        String response = run("getSessionId", session);
        int start = response.indexOf("session id: [") + 13;
        String sessionId = response.substring(start, response.indexOf(']', start));

        // Due to TIME_BASED_WRITE, the value should be written to cache some time within the next 5 seconds. Poll for it,
        run("testPollCache&attribute=testWriteFrequency&value=" + newValue + "&sessionId=" + sessionId,
            null); // Avoid having the servlet access the session here because this will block 5 cycles of the time based write.

        run("invalidateSession", session);
    }

    private static String run(String testMethod, List<String> session) throws Exception {
        return FATSuite.run(server, APP_DEFAULT + '/' + SERVLET_NAME, testMethod, session);
    }
}
