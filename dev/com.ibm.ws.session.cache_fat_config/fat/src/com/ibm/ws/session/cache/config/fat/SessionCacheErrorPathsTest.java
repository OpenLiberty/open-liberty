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
import static org.junit.Assert.assertTrue;

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
import com.ibm.websphere.simplicity.config.File;
import com.ibm.websphere.simplicity.config.Library;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class SessionCacheErrorPathsTest extends FATServletClient {

    private static final String APP_NAME = "sessionCacheConfigApp";
    private static final Set<String> APP_NAMES = Collections.singleton(APP_NAME);
    private static final String[] EMPTY_RECYCLE_LIST = new String[0];
    private static final String SERVLET_NAME = "SessionCacheConfigTestServlet";

    private static ServerConfiguration savedConfig;

    @Server("sessionCacheServer")
    public static LibertyServer server;

    /**
     * After running each test, ensure the server is stopped and restore the original configuration.
     */
    @After
    public void cleanUpPerTest() throws Exception {
        try {
            if (server.isStarted())
                server.stopServer();
        } finally {
            server.updateServerConfiguration(savedConfig);
        }
        System.out.println("server configuration restored");
    }

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "session.cache.web");

        String configLocation = new java.io.File(server.getUserDir() + "/shared/resources/hazelcast/hazelcast-localhost-only.xml").getAbsolutePath();
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
        server.startServer(testName.getMethodName() + ".log");

        assertEquals("Session manager should warn user that sessions will be stored in memory",
                     1, server.findStringsInLogs("SESN8501I").size());

        List<String> session = new ArrayList<>();
        run("testSetAttribute&attribute=testAddFeature1&value=AF1", session);

        // Add the sessionCache-1.0 feature
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(savedConfig);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, EMPTY_RECYCLE_LIST);

        run("testSetAttribute&attribute=testAddFeature2&value=AF2", session);

        // second value should be written to the cache
        run("testCacheContains&attribute=testAddFeature2&value=AF2", session);

        // first value should not be written to the cache
        run("testCacheContains&attribute=testAddFeature1&value=null", session);

        run("invalidateSession", session);
    }

    /**
     * Start the server with an invalid Hazelcast uri configured on httpSessionCache.
     * Verify that after correcting the uri, session data is persisted.
     */
    @AllowedFFDC(value = { "javax.cache.CacheException", // for invalid uri
                           "java.lang.NullPointerException", // TODO fails when WebApp.destroy/SessionContext.stop invoked after deactivate
                           "java.net.MalformedURLException" // TODO possible bug
    })
    @Test
    public void testInvalidURI() throws Exception {
        // Start the server with invalid httpSessionCache uri
        String invalidHazelcastURI = "file:" + new java.io.File(server.getUserDir() + "/servers/sessionCacheServer/server.xml").getAbsolutePath();
        ServerConfiguration config = savedConfig.clone();
        config.getHttpSessionCaches().get(0).setUri(invalidHazelcastURI);
        server.updateServerConfiguration(config);
        server.startServer(testName.getMethodName() + ".log");

        try {
            List<String> session = new ArrayList<>();
            run("testSessionCacheNotAvailable", session);

            assertTrue("Expected: CacheException: Error opening URI ...",
                       server.findStringsInLogs("CacheException.*URI").size() > 1);

            // Correct the URI
            String validHazelcastURI = "file:" + new java.io.File(server.getUserDir() + "/shared/resources/hazelcast/hazelcast-localhost-only.xml").getAbsolutePath();
            config.getHttpSessionCaches().get(0).setUri(validHazelcastURI);
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(APP_NAMES, EMPTY_RECYCLE_LIST);

            run("testSetAttribute&attribute=testInvalidURI2&value=IU2", session);

            // second value should be written to the cache
            run("testCacheContains&useURI=true&attribute=testInvalidURI2&value=IU2", session);

            // first value should not be written to the cache
            run("testCacheContains&useURI=true&attribute=testInvalidURI1&value=null", session);

            // Remove the URI and let it default to the system property
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(savedConfig);
            server.waitForConfigUpdateInLogUsingMark(APP_NAMES, EMPTY_RECYCLE_LIST);

            run("testSetAttribute&attribute=testInvalidURI3&value=IU3", session);

            // third value should be written to the cache
            run("testCacheContains&useURI=false&attribute=testInvalidURI3&value=IU3", session);

            // whether or not the second value is written depends on JCache provider's behavior when same URI specified a different way

            // first value should not be written to the cache
            run("testCacheContains&useURI=false&attribute=testInvalidURI1&value=null", session);

            run("invalidateSession", session);
        } finally {
            server.stopServer("CWWKE0701E.*CacheException.*URI", // Expected: CWWKE0701E: ... CacheException: Error opening URI ...
                              "SRVE0297E.*NullPointerException" // TODO fails when WebApp.destroy/SessionContext.stop invoked after deactivate
            );
        }
    }

    /**
     * Configure httpSessionCache pointing at a library that lacks a valid JCache provider. Access a session before and after,
     * verifying that a session attribute added afterward is persisted, whereas a session attribute added before is not.
     */
    @AllowedFFDC("java.net.MalformedURLException") // TODO possible bug
    @Test
    public void testLibraryWithoutJCacheProvider() throws Exception {
        // Start the server with libraryRef missing
        ServerConfiguration config = savedConfig.clone();
        Library libraryWithoutJCacheProvider = new Library();
        libraryWithoutJCacheProvider.setId("libraryWithoutJCacheProvider");
        File libraryWithoutJCacheProvider_file = new File();
        // specify a binary already produced by the FAT bucket that won't have a JCache provider in it
        libraryWithoutJCacheProvider_file.setName("${server.config.dir}/apps/sessionCacheConfigApp.war");
        libraryWithoutJCacheProvider.setFile(libraryWithoutJCacheProvider_file);
        config.getLibraries().add(libraryWithoutJCacheProvider);
        config.getHttpSessionCaches().get(0).setLibraryRef("libraryWithoutJCacheProvider");
        server.updateServerConfiguration(config);
        server.startServer(testName.getMethodName() + ".log");
        try {
            List<String> session = new ArrayList<>();
            run("testSessionCacheNotAvailable", session);
            assertEquals("Expecting javax.cache.CacheException: No CachingProviders have been configured",
                         1, server.findStringsInLogs("CacheException").size());

            // Correct the libraryRef to point at Hazelcast JCache provider
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(savedConfig);
            server.waitForConfigUpdateInLogUsingMark(APP_NAMES, EMPTY_RECYCLE_LIST);

            run("testSetAttribute&attribute=testLibraryWithoutJCacheProvider2&value=LWJCP2", session);

            // second value should be written to the cache
            run("testCacheContains&attribute=testLibraryWithoutJCacheProvider2&value=LWJCP2", session);

            // first value should not be written to the cache
            run("testCacheContains&attribute=testLibraryWithoutJCacheProvider1&value=null", session);

            run("invalidateSession", session);
        } finally {
            server.stopServer("SRVE8059E", "CWWKE0701E.*CacheException"); // Expecting javax.cache.CacheException: No CachingProviders have been configured
        }
    }

    @Test
    @AllowedFFDC("java.net.MalformedURLException") // TODO possible bug
    public void testModifyFileset() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        com.ibm.websphere.simplicity.config.File hazelcastFile = config.getLibraries().getById("HazelcastLib").getNestedFile();
        String originalName = hazelcastFile.getName();
        server.startServer(testName.getMethodName() + ".log");

        // Use sessionCache with original (good) config
        List<String> session = new ArrayList<>();
        run("testSetAttribute&attribute=testModifyFileset&value=0", session);
        run("testCacheContains&attribute=testModifyFileset&value=0", session);

        // Change the fileset to reference a bogus file
        hazelcastFile.setName("${shared.resource.dir}/hazelcast/bogus.jar");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES);

        // Restore the original config and expect the new cache to be usable
        hazelcastFile.setName(originalName);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
        run("testSetAttribute&attribute=testModifyFileset&value=1", session);
        run("testCacheContains&attribute=testModifyFileset&value=1", session);

        server.stopServer("CWWKL0012W.*bogus", "SRVE8059E", "CWWKE0701E.*CacheException");
    }

    /**
     * Configure httpSessionCache lacking a libraryRef (or bell). Access a session before and after,
     * verifying that a session attribute added afterward is persisted, whereas a session attribute added before is not
     * (the OSGi service backing httpSessionCache config will be unable to activate in the absence of libraryRef).
     */
    @AllowedFFDC("java.net.MalformedURLException") // TODO possible bug
    @Test
    public void testMissingLibraryRef() throws Exception {
        // Start the server with libraryRef missing
        ServerConfiguration config = savedConfig.clone();
        config.getHttpSessionCaches().get(0).setLibraryRef(null);
        server.updateServerConfiguration(config);
        server.startServer(testName.getMethodName() + ".log");

        assertEquals("Session manager should warn user that sessions will be stored in memory", 1, server.findStringsInLogs("SESN8501I").size());

        List<String> session = new ArrayList<>();
        run("testSetAttribute&attribute=testMissingLibraryRef1&value=MLF1", session);

        // Add the libraryRef
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(savedConfig);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, EMPTY_RECYCLE_LIST);

        run("testSetAttribute&attribute=testMissingLibraryRef2&value=MLF2", session);

        // second value should be written to the cache
        run("testCacheContains&attribute=testMissingLibraryRef2&value=MLF2", session);

        // first value should not be written to the cache
        run("testCacheContains&attribute=testMissingLibraryRef1&value=null", session);

        run("invalidateSession", session);
    }

    private static String run(String testName, List<String> session) throws Exception {
        return FATSuite.run(server, APP_NAME + '/' + SERVLET_NAME, testName, session);
    }
}
