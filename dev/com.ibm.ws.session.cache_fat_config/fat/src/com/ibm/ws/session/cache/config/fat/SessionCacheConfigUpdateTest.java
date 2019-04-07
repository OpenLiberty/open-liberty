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
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ClassloaderElement;
import com.ibm.websphere.simplicity.config.HttpSessionCache;
import com.ibm.websphere.simplicity.config.Monitor;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class SessionCacheConfigUpdateTest extends FATServletClient {

    private static final String APP_DEFAULT = "sessionCacheConfigApp";
    private static final String APP_JCACHE = "jcacheApp";
    private static final Set<String> APP_NAMES = Collections.singleton(APP_DEFAULT); // jcacheApp not included because it isn't normally configured
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
        ShrinkHelper.defaultApp(server, APP_DEFAULT, "session.cache.web");
        ShrinkHelper.defaultApp(server, APP_JCACHE, "test.cache.web");
        server.removeInstalledAppForValidation(APP_JCACHE); // This application is available for tests to add but not configured by default.

        String hazelcastConfigFile = "hazelcast-localhost-only.xml";

        if (FATSuite.isMulticastDisabled()) {
            Log.info(SessionCacheConfigUpdateTest.class, "setUp", "Disabling multicast in Hazelcast config.");
            hazelcastConfigFile = "hazelcast-localhost-only-multicastDisabled.xml";
        }

        String configLocation = new File(server.getUserDir() + "/shared/resources/hazelcast/" + hazelcastConfigFile).getAbsolutePath();
        server.setJvmOptions(Arrays.asList("-Dhazelcast.config=" + configLocation,
                                           "-Dhazelcast.config.file=" + hazelcastConfigFile,
                                           "-Dhazelcast.group.name=" + UUID.randomUUID()));

        savedConfig = server.getServerConfiguration().clone();
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
        server.stopServer();
    }

    /**
     * Verify that application usage of a caching provider does not interfere with the sessionCache feature.
     */
    @Test
    public void testApplicationClosesCachingProvider() throws Exception {
        // Add application: jcacheApp
        ServerConfiguration config = server.getServerConfiguration();
        Application jcacheApp = new Application();
        ClassloaderElement jcacheApp_classloader = new ClassloaderElement();
        jcacheApp_classloader.getCommonLibraryRefs().add("HazelcastLib");
        jcacheApp.getClassloaders().add(jcacheApp_classloader);
        jcacheApp.setLocation(APP_JCACHE + ".war");
        config.getApplications().add(jcacheApp);

        Set<String> appNames = new TreeSet<String>(APP_NAMES);
        appNames.add(APP_JCACHE);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, EMPTY_RECYCLE_LIST);

        // Application obtains the CachingProvider for the same configured library and closes the provider
        FATSuite.run(server, APP_JCACHE + "/JCacheConfigTestServlet", "testCloseCachingProvider", null);

        // Access a session - this will only work if sessionCache feature has used a different CachingProvider instance
        List<String> session = new ArrayList<>();
        run("getSessionId", session);
        run("invalidateSession", session);
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
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, EMPTY_RECYCLE_LIST);

        TimeUnit.SECONDS.sleep(10); // Due to invalidation thread delay

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
     * while the server is running. The session must remain valid, and must exhibit the new behavior (MANUAL_UPDATE) after
     * the configuration change.
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
        run("testSetAttribute&attribute=testWriteFrequency&value=2_MANUAL_UPDATE", session);
        run("testCacheContains&attribute=testWriteFrequency&value=1_END_OF_SERVLET_SERVICE", session);

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
