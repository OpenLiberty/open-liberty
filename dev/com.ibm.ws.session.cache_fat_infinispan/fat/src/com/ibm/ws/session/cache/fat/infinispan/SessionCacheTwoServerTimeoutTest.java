/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.cache.fat.infinispan;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Tests related to Session Cache Timeouts, using two servers with the following session settings:
 * invalidationTimeout="5s"
 * reaperPollInterval="30" //Min allowed to not receive random poll interval between 30-60s
 */
@MaximumJavaLevel(javaLevel = 22)
@RunWith(FATRunner.class)
public class SessionCacheTwoServerTimeoutTest extends FATServletClient {
    public static final Class<?> c = SessionCacheTwoServerTimeoutTest.class;

    @Server("com.ibm.ws.session.cache.fat.infinispan.timeoutServerA")
    public static LibertyServer serverA;

    @Server("com.ibm.ws.session.cache.fat.infinispan.timeoutServerB")
    public static LibertyServer serverB;

    public static SessionCacheApp appA;
    public static SessionCacheApp appB;

    @ClassRule
    public static RepeatTests repeatRule = RepeatTests.withoutModification().andWith(new CacheManagerRepeatAction());

    @BeforeClass
    public static void setUp() throws Exception {
        appA = new SessionCacheApp(serverA, false, "session.cache.infinispan.web", "session.cache.infinispan.web.listener1");

        // serverB requires a listener as sessions created on serverA can be destroyed on serverB via a timeout.
        appB = new SessionCacheApp(serverB, false, "session.cache.infinispan.web", "session.cache.infinispan.web.listener1");
        serverB.useSecondaryHTTPPort();

        String sessionCacheConfigFile = "httpSessionCache_1.xml";
        if (RepeatTestFilter.isRepeatActionActive(CacheManagerRepeatAction.ID)) {
            sessionCacheConfigFile = "httpSessionCache_2.xml";
        }

        String rand = UUID.randomUUID().toString();
        Map<String, String> options = serverA.getJvmOptionsAsMap();
        options.put("-Dinfinispan.cluster.name", rand);
        options.put("-Dsession.cache.config.file", sessionCacheConfigFile);
        options.put("-Djgroups.bind.address", "127.0.0.1");
        serverA.setJvmOptions(options);

        options = serverB.getJvmOptionsAsMap();
        options.put("-Dinfinispan.cluster.name", rand);
        options.put("-Dsession.cache.config.file", sessionCacheConfigFile);
        options.put("-Djgroups.bind.address", "127.0.0.1");
        serverB.setJvmOptions(options);

        serverA.startServer();
        TimeUnit.SECONDS.sleep(10);

        // Warm up serverA
        List<String> sessionA = new ArrayList<>();
        appA.sessionPut("init-app-A", "A", sessionA, true);
        appA.invalidateSession(sessionA);

        serverB.startServer();
        TimeUnit.SECONDS.sleep(10);

        // Warm up serverB
        List<String> sessionB = new ArrayList<>();
        appB.sessionPut("init-app-B", "B", sessionB, true);
        appB.invalidateSession(sessionB);
    }

    @AfterClass
    public static void tearDown() {
        // Intentionally DO NOT fail the test class if shutdown/log-scan complains.
        try {
            Log.info(c, "tearDown", "Start server A shutdown");
            try {
                serverA.stopServer(); // may scan logs and throw — swallow below
            } catch (Exception e) {
                Log.info(c, "tearDown", "Ignoring serverA stop exception: " + e.getMessage());
            }
        } finally {
            Log.info(c, "tearDown", "Start server B shutdown");
            try {
                serverB.stopServer();
            } catch (Exception e) {
                Log.info(c, "tearDown", "Ignoring serverB stop exception: " + e.getMessage());
            }
            if (isZOS()) {
                try {
                    Log.info(c, "tearDown", "Allow more time for z/OS shutdown");
                    TimeUnit.SECONDS.sleep(20);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static boolean isZOS() {
        String osName = System.getProperty("os.name");
        return osName.contains("OS/390") || osName.contains("z/OS") || osName.contains("zOS");
    }

    @Test
    @Mode(FULL)
    public void testInvalidationTimeoutTwoServer() throws Exception {
        List<String> session = new ArrayList<>();
        String sessionID = appA.sessionPut("testInvalidationTimeoutTwoServer-foo", "bar", session, true);
        appB.sessionGet("testInvalidationTimeoutTwoServer-foo", "bar", session);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<String> serverAResult = () -> serverA.waitForStringInLog("notified of sessionDestroyed for " + sessionID, 5 * 60 * 1000);
        Callable<String> serverBResult = () -> serverB.waitForStringInLog("notified of sessionDestroyed for " + sessionID, 5 * 60 * 1000);
        String result = pool.invokeAny(Arrays.asList(serverAResult, serverBResult), 5 * 60 * 1000 * 2, TimeUnit.MILLISECONDS);

        assertNotNull("Expected to find message from a session listener indicating the session expired", result);
        appB.sessionGet("testInvalidationTimeoutTwoServer-foo", null, session);
    }

    @Test
    @Mode(FULL)
    public void testInvalidationServletNoLocalCacheTwoServer() throws Exception {
        List<String> session = new ArrayList<>();
        appA.sessionPut("testInvalidationServletNoLocalCacheTwoServer-foo", "bar", session, true);
        appB.invokeServlet("sessionGetTimeout&key=testInvalidationServletNoLocalCacheTwoServer-foo", session);
        appB.sessionGet("testInvalidationServletNoLocalCacheTwoServer-foo", null, session);
    }

    @Test
    @Mode(FULL)
    public void testInvalidationServletLocalCacheTwoServer() throws Exception {
        List<String> session = new ArrayList<>();
        appA.sessionPut("testInvalidationServletLocalCacheTwoServer-foo", "bar", session, true);
        appB.sessionGet("testInvalidationServletLocalCacheTwoServer-foo", "bar", session);
        appB.invokeServlet("sessionGetTimeout&key=testInvalidationServletLocalCacheTwoServer-foo&expectedValue=bar", session);
        appB.sessionGet("testInvalidationServletLocalCacheTwoServer-foo", null, session);
    }

    @Test
    @Mode(FULL)
    public void testCacheInvalidationServletNoLocalCacheTwoServer() throws Exception {
        List<String> session = new ArrayList<>();
        String sessionID = appA.sessionPut("testCacheInvalidationServletNoLocalCacheTwoServer-foo", "bar", session, true);
        appB.invokeServlet("sessionGetTimeoutCacheCheck&key=testCacheInvalidationServletNoLocalCacheTwoServer-foo", session);
        appA.invokeServlet("cacheCheck&key=testCacheInvalidationServletNoLocalCacheTwoServer-foo&sid=" + sessionID, session);
    }

    @Test
    @Mode(FULL)
    public void testCacheInvalidationLocalCacheTwoServer() throws Exception {
        List<String> session = new ArrayList<>();
        appA.sessionPut("testCacheInvalidationLocalCacheTwoServer-foo", "bar", session, true);
        appB.sessionGet("testCacheInvalidationLocalCacheTwoServer-foo", "bar", session);
        appB.invokeServlet("sessionGetTimeoutCacheCheck&key=testCacheInvalidationLocalCacheTwoServer-foo", session);
    }

    @Test
    @Mode(FULL)
    public void testCacheInvalidationTwoServer() throws Exception {
        List<String> session = new ArrayList<>();
        String sessionID = appA.sessionPut("testCacheInvalidationTwoServer-foo", "bar", session, true);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<String> serverAResult = () -> serverA.waitForStringInLog("notified of sessionDestroyed for " + sessionID, 5 * 60 * 1000);
        Callable<String> serverBResult = () -> serverB.waitForStringInLog("notified of sessionDestroyed for " + sessionID, 5 * 60 * 1000);
        String result = pool.invokeAny(Arrays.asList(serverAResult, serverBResult), 5 * 60 * 1000 * 2, TimeUnit.MILLISECONDS);

        assertNotNull("Expected to find message from a session listener indicating the session expired", result);
        appB.invokeServlet("cacheCheck&key=testCacheInvalidationTwoServer-foo&sid=" + sessionID, session);
        appA.invokeServlet("cacheCheck&key=testCacheInvalidationTwoServer-foo&sid=" + sessionID, session);
    }
}
