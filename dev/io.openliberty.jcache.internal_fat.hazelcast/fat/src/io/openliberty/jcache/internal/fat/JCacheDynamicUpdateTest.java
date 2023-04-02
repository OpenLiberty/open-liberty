/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.jcache.internal.fat;

import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.cache.AuthCache;
import com.ibm.websphere.simplicity.config.cache.Cache;
import com.ibm.websphere.simplicity.config.cache.CacheManager;
import com.ibm.websphere.simplicity.config.cache.CachingProvider;
import com.ibm.websphere.simplicity.config.dsprops.Properties;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.jcache.internal.fat.plugins.TestPluginHelper;

/**
 * Contains distributed JCache test dynamic server.xml changes
 */
@SuppressWarnings("restriction")
@SkipIfSysProp("skip.tests=true")
@SkipForRepeat({ SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES })
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JCacheDynamicUpdateTest extends BaseTestCase {

    @Server("io.openliberty.jcache.internal.fat.dynamicupdate.1")
    public static LibertyServer server1;

    /**
     * Nearly empty server configuration. Does not contain any cache config.
     */
    private static ServerConfiguration emptyConfiguration = null;

    @Before
    public void setup() throws Exception {
        assumeShouldNotSkipTests();

        String groupName = UUID.randomUUID().toString();

        /*
         * Start server 1.
         */
        startServer1(server1, groupName, null, null);

        emptyConfiguration = server1.getServerConfiguration();
    }

    @After
    public void after() throws Exception {
        /*
         * Stop the servers in the reverse order they were started.
         */
        stopServer(server1, "CWWKG0033W", "CWLJC0004E", "CWWKE0701E", "CWLJC0011W", "CWWKG0058E",
                   "CWLJC0006E", "CWLJC0012W", "CWLJC0013W", "CWWKE1102W", "CWWKE1106W", "CWWKE1107W");
    }

    /**
     * Convenience method to configure the Liberty server with a Cache config.
     *
     * @throws Exception If there was an error configuring the server.
     */
    private static void updateLibertyServer(String cacheManagerRef, String cachingProviderRef, String providerClass, String jCacheLibraryRef, String commonLibraryRef,
                                            String uri) throws Exception {
        ServerConfiguration server = emptyConfiguration.clone();

        Cache cache = new Cache("AuthCache", "AuthCache", cacheManagerRef);
        AuthCache authCache = new AuthCache("AuthCache");
        CacheManager cacheManager = new CacheManager("CacheManager", cachingProviderRef, uri);

        if (TestPluginHelper.isInfinispan()) {
            Properties properties = new Properties();
            properties.setExtraAttribute("infinispan.client.hotrod.uri", "${infinispan.client.hotrod.uri}");
            cacheManager.setProps(properties);
        }

        CachingProvider cachingProvider = new CachingProvider("CachingProvider", jCacheLibraryRef, commonLibraryRef, providerClass);

        /*
         * Remove any duplicates of the Cache, CacheManager or CachingProviders and add in the new ones.
         */
        server.getCaches().removeIf(c -> cache.getId().equals(c.getId()));
        server.getCaches().add(cache);

        server.getCacheManagers().removeIf(cm -> cacheManager.getId().equals(cm.getId()));
        server.getCacheManagers().add(cacheManager);

        server.getCachingProviders().removeIf(cp -> cachingProvider.getId().equals(cp.getId()));
        server.getCachingProviders().add(cachingProvider);

        server.getAuthCaches().clear();
        server.getAuthCaches().add(authCache);

        updateConfigDynamically(server1, server);
    }

    /**
     * Test dynamically adding and removing additional caches.
     *
     * @throws Exception
     */
    @Test
    public void addAndRemoveCaches() throws Exception {
        /*
         * Create a working cache configuration
         */
        if (TestPluginHelper.isInfinispan()) {
            updateLibertyServer("CacheManager", "CachingProvider", "org.infinispan.jcache.remote.JCachingProvider", "InfinispanLib", "CustomLoginLib",
                                "file:///${shared.resource.dir}/infinispan/${infinispan.hotrod.file}");
        } else if (TestPluginHelper.isHazelcast()) {
            updateLibertyServer("CacheManager", "CachingProvider", null, "HazelcastLib", "CustomLoginLib",
                                "file:${shared.resource.dir}/hazelcast/${hazelcast.config.file}");
        }
        ServerConfiguration current = server1.getServerConfiguration();

        /*
         * Add second and third cache
         */
        current.getCaches().add(new Cache("AuthCache2", "AuthCache2", "CacheManager2"));
        current.getCaches().add(new Cache("AuthCache3", "AuthCache3", "CacheManager3"));
        updateConfigDynamically(server1, current);

        /*
         * Wait for caches to start
         */
        waitForCreatedOrExistingJCache(server1, "AuthCache2");
        waitForCreatedOrExistingJCache(server1, "AuthCache3");

        /*
         * Remove the caches
         */
        current.getCaches().removeById("AuthCache2");
        current.getCaches().removeById("AuthCache3");
        updateConfigDynamically(server1, current);

        //There is no unregister message, but could check here if one is added
    }

    /**
     * Test dynamic change to server.xml
     *
     * Start with bad cacheManagerRef, cachingProviderRef, uri, providerClass, and libraryRef:
     *
     * <cache id="AuthCache" name="AuthCache"
     * cacheManagerRef="CacheManagerBad" />
     *
     * <cachingProvider id="CachingProvider"
     * providerClass="org.infinispan.jcache.remote.JCachingProviderBad"
     * jCacheLibraryRef="InfinispanLibBad" commonLibraryRef="CustomLoginLib" />
     *
     * <cacheManager cachingProviderRef="CachingProviderBad" id="CacheManager"
     * uri="file:///${shared.resource.dir}/infinispan/infinispan_hotrod_bad.props">
     *
     * <properties
     * ... />
     * </cacheManager>
     *
     * 1. Start where everything is "bad".
     * 2. Update server to have correct cacheManagerRef, cachingProviderRef, but incorrect jCacheLibraryRef and commonLibraryRef
     * 3. Update server to have correct jCacheLibraryRef and has commonLibraryRef with a library that matches the jCacheLibraryRef.
     * 4. Update server to have commonLibraryRef.
     * 5. Update server to have correct cachingProvider->providerClass.
     * 6. Update uri so it is good. Working server.
     *
     * @throws Exception if the test fails for some unforeseen reason.
     */
    @Test
    @CheckForLeakedPasswords(USER1_PASSWORD)
    @ExpectedFFDC(value = { "javax.cache.CacheException", "java.lang.IllegalStateException" })
    @AllowedFFDC("org.infinispan.client.hotrod.exceptions.HotRodClientException")
    public void dynamicUpdate() throws Exception {

        /*
         * ***********************************************************************
         *
         * 1. Start where everything is "bad".
         *
         * ***********************************************************************
         */
        if (TestPluginHelper.isInfinispan()) {
            updateLibertyServer("CacheManagerBad", "CachingProviderBad", "org.infinispan.jcache.remote.JCachingProviderBad", null, null,
                                "file:///${shared.resource.dir}/infinispan/infinispan_hotrod_bad.props");
        } else if (TestPluginHelper.isHazelcast()) {
            updateLibertyServer("CacheManagerBad", "CachingProviderBad", "com.hazelcast.cache.HazelcastCachingProviderBad", null, null,
                                "file:${shared.resource.dir}/hazelcast/${hazelcast.config.file}.bad");
        }

        /*
         * Check for warning for invalid references for cachingProviderRef, cacheManagerRef. No values for commonLibraryRef and jCacheLibraryRef.
         */
        String error = "CWWKG0033W: The value \\[CachingProviderBad\\] specified for the reference attribute \\[cachingProviderRef\\] was not found in the configuration.";
        assertTrue("Should find '" + error + "' in the logs", !server1.findStringsInLogsAndTraceUsingMark(error).isEmpty());

        error = "CWWKG0033W: The value \\[CacheManagerBad\\] specified for the reference attribute \\[cacheManagerRef\\] was not found in the configuration.";
        assertTrue("Should find '" + error + "' in the logs", !server1.findStringsInLogsAndTraceUsingMark(error).isEmpty());

        error = "CWWKG0058E: The element cachingProvider with the unique identifier CachingProvider is missing the required attribute jCacheLibraryRef.";
        assertTrue("Should find '" + error + "' in the logs", !server1.findStringsInLogsAndTraceUsingMark(error).isEmpty());

        /*
         * ***********************************************************************
         *
         * 2. Update server to have correct cacheManagerRef, cachingProviderRef, but incorrect jCacheLibraryRef and commonLibraryRef
         *
         * Bad: cachingProvider->providerClass and uri
         *
         * ***********************************************************************
         */
        if (TestPluginHelper.isInfinispan()) {
            updateLibertyServer("CacheManager", "CachingProvider", "org.infinispan.jcache.remote.JCachingProviderBad", "InfinispanLibBad", "CustomLoginLibBad",
                                "file:///${shared.resource.dir}/infinispan/infinispan_hotrod_bad.props");
        } else if (TestPluginHelper.isHazelcast()) {
            updateLibertyServer("CacheManager", "CachingProvider", "com.hazelcast.cache.HazelcastCachingProviderBad", "HazelcastLibBad", "CustomLoginLibBad",
                                "file:${shared.resource.dir}/hazelcast/${hazelcast.config.file}.bad");
        }

        /*
         * Check for warning for invalid references for commonLibraryRef and jCacheLibraryRef.
         */
        error = "CWWKG0033W: The value \\[CustomLoginLibBad\\] specified for the reference attribute \\[commonLibraryRef\\] was not found in the configuration.";
        assertTrue("Should find '" + error + "' in the logs", !server1.findStringsInLogsAndTraceUsingMark(error).isEmpty());

        if (TestPluginHelper.isInfinispan()) {
            error = "CWWKG0033W: The value \\[InfinispanLibBad\\] specified for the reference attribute \\[jCacheLibraryRef\\] was not found in the configuration.";
        } else if (TestPluginHelper.isHazelcast()) {
            error = "CWWKG0033W: The value \\[HazelcastLibBad\\] specified for the reference attribute \\[jCacheLibraryRef\\] was not found in the configuration.";
        }
        assertTrue("Should find '" + error + "' in the logs", !server1.findStringsInLogsAndTraceUsingMark(error).isEmpty());

        /*
         * ***********************************************************************
         *
         * 3. Update server to have correct commonLibraryRef with a library that matches the jCacheLibraryRef.
         *
         * Bad: cachingProvider->providerClass and uri
         *
         * ***********************************************************************
         */
        if (TestPluginHelper.isInfinispan()) {
            updateLibertyServer("CacheManager", "CachingProvider", "org.infinispan.jcache.remote.JCachingProviderBad", "InfinispanLib", "CustomLoginLib,InfinispanLib",
                                "file:///${shared.resource.dir}/infinispan/infinispan_hotrod_bad.props");
        } else if (TestPluginHelper.isHazelcast()) {
            updateLibertyServer("CacheManager", "CachingProvider", "com.hazelcast.cache.HazelcastCachingProviderBad", "HazelcastLib", "CustomLoginLib,HazelcastLib",
                                "file:${shared.resource.dir}/hazelcast/${hazelcast.config.file}.bad");
        }

        /*
         * Check for warning that a library is being shared between jCacheLibraryRef and commonLibraryRef.
         */
        if (TestPluginHelper.isInfinispan()) {
            error = "CWLJC0006E: The CachingProvider caching provider referenced the InfinispanLib library from both jCacheLibaryRef and commonLibraryRef";
        } else if (TestPluginHelper.isHazelcast()) {
            error = "CWLJC0006E: The CachingProvider caching provider referenced the HazelcastLib library from both jCacheLibaryRef and commonLibraryRef";
        }

        assertTrue("Should find '" + error + "' in the logs", !server1.findStringsInLogsAndTraceUsingMark(error).isEmpty());

        /*
         * ***********************************************************************
         *
         * 4. Update server to have commonLibraryRef
         *
         * Bad: cachingProvider->providerClass and uri
         *
         * ***********************************************************************
         */
        if (TestPluginHelper.isInfinispan()) {
            updateLibertyServer("CacheManager", "CachingProvider", "org.infinispan.jcache.remote.JCachingProviderBad", "InfinispanLib", "CustomLoginLib",
                                "file:///${shared.resource.dir}/infinispan/infinispan_hotrod_bad.props");
        } else if (TestPluginHelper.isHazelcast()) {
            updateLibertyServer("CacheManager", "CachingProvider", "com.hazelcast.cache.HazelcastCachingProviderBad", "HazelcastLib", "CustomLoginLib",
                                "file:${shared.resource.dir}/hazelcast/${hazelcast.config.file}.bad");
        }

        /*
         * Next issue is bad cachingProvider->providerClass.
         *
         * CWLJC0004E: The io.openliberty.jcache.cache[AuthCache]...
         * javax.cache.CacheException: Failed to load the CachingProvider [org.infinispan.jcache.remote.JCachingProviderBad]
         * Caused by: java.lang.ClassNotFoundException
         */
        if (TestPluginHelper.isInfinispan()) {
            error = "javax.cache.CacheException: Failed to load the CachingProvider \\[org.infinispan.jcache.remote.JCachingProviderBad\\]";
        } else if (TestPluginHelper.isHazelcast()) {
            error = "javax.cache.CacheException: Failed to load the CachingProvider \\[com.hazelcast.cache.HazelcastCachingProviderBad\\]";
        }
        assertTrue("Should find '" + error + "' in the logs", !server1.findStringsInLogsAndTraceUsingMark(error).isEmpty());

        /*
         * ***********************************************************************
         *
         * 5. Update server to have correct cachingProvider->providerClass.
         *
         * Bad: uri.
         *
         * ***********************************************************************
         */
        if (TestPluginHelper.isInfinispan()) {
            updateLibertyServer("CacheManager", "CachingProvider", "org.infinispan.jcache.remote.JCachingProvider", "InfinispanLib", "CustomLoginLib",
                                "file:///${shared.resource.dir}/infinispan/infinispan_hotrod_bad.props");
        } else if (TestPluginHelper.isHazelcast()) {
            updateLibertyServer("CacheManager", "CachingProvider", "com.hazelcast.cache.HazelcastCachingProvider", "HazelcastLib", "CustomLoginLib",
                                "file:${shared.resource.dir}/hazelcast/${hazelcast.config.file}.bad");
        }

        /*
         * The providerClass, jCacheLibraryRef, and commonLibraryRef are fixed. Look for bad uri error.
         *
         * CWLJC0011E: Error encountered while retrieving the AuthCache JCache: javax.cache.CacheException: Could not load configuration
         * Caused by: java.io.FileNotFoundException: /Users/eschr/libertyGit/open-liberty/dev/build.image/wlp/usr/shared/resources/infinispan/infinispan_hotrod_bad.props (No such
         * file or directory)
         */
        if (TestPluginHelper.isInfinispan()) {
            error = "CWLJC0011W: Error encountered while retrieving the AuthCache JCache. Will attempt to create it instead. The error was: javax.cache.CacheException: Could not load configuration";
        } else if (TestPluginHelper.isHazelcast()) {
            error = "CWLJC0011W: Error encountered while retrieving the AuthCache JCache. Will attempt to create it instead. The error was: javax.cache.CacheException: Error opening URI";
        }
        assertTrue("Should find '" + error + "' in the logs", !server1.findStringsInLogsAndTraceUsingMark(error).isEmpty());

        /*
         * ***********************************************************************
         *
         * 6. Update uri so it is good. Working server.
         *
         * ***********************************************************************
         */
        if (TestPluginHelper.isInfinispan()) {
            updateLibertyServer("CacheManager", "CachingProvider", "org.infinispan.jcache.remote.JCachingProvider", "InfinispanLib", "CustomLoginLib",
                                "file:///${shared.resource.dir}/infinispan/${infinispan.hotrod.file}");
        } else if (TestPluginHelper.isHazelcast()) {
            updateLibertyServer("CacheManager", "CachingProvider", "com.hazelcast.cache.HazelcastCachingProvider", "HazelcastLib", "CustomLoginLib",
                                "file:${shared.resource.dir}/hazelcast/${hazelcast.config.file}");
        }

        /*
         * All issues have been fixed, so wait for the auth cache to start.
         */
        waitForCreatedOrExistingJCache(server1, AUTH_CACHE_NAME);
    }
}
