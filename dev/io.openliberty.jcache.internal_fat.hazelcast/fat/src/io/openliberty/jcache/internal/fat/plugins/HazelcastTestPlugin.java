/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jcache.internal.fat.plugins;

import java.util.Arrays;
import java.util.Locale;

import componenttest.topology.impl.LibertyServer;

/**
 * Test plugin for Hazelcast.
 */
public class HazelcastTestPlugin implements TestPlugin {

    private static final String HAZELCAST_GROUP_PASSWORD = "groupPassword";

    @Override
    public void setupServer1(LibertyServer server, String hazelcastGroupName, Integer authCacheMaxSize, Integer authCacheTtlSecs) throws Exception {
        /*
         * Default the cache size and TTL to the default values for authCache->maxSize and authCache->timeout
         * from server.xml.
         */
        if (authCacheMaxSize == null) {
            authCacheMaxSize = 25000;
        }
        if (authCacheTtlSecs == null) {
            authCacheTtlSecs = 600;
        }

        /*
         * Determine whether we need to disable multicast in the configuration.
         */
        String hazecastConfigFile = "hazelcast-localhost-only.xml";
        if (isMulticastDisabled()) {
            hazecastConfigFile = "hazelcast-localhost-only-multicastDisabled.xml";
        }

        /*
         * Set JVM options.
         */
        server.setJvmOptions(Arrays.asList("-Dhazelcast.group.name=" + hazelcastGroupName,
                                           "-Dhazelcast.group.password=" + HAZELCAST_GROUP_PASSWORD,
                                           "-Dhazelcast.authcache.max.size=" + authCacheMaxSize,
                                           "-Dhazelcast.authcache.entry.ttl=" + authCacheTtlSecs,
                                           "-Dhazelcast.config.file=" + hazecastConfigFile,
                                           "-Dhazelcast.jcache.provider.type=server",
                                           "-Dcom.ibm.ws.beta.edition=true")); // TODO Remove when GA'd
    }

    @Override
    public void setupServer2(LibertyServer server, String hazelcastGroupName) throws Exception {
        String hazecastConfigFile = "hazelcast-client-localhost-only.xml";

        /*
         * Set JVM options.
         */
        server.setJvmOptions(Arrays.asList("-Dhazelcast.group.name=" + hazelcastGroupName,
                                           "-Dhazelcast.group.password=" + HAZELCAST_GROUP_PASSWORD,
                                           "-Dhazelcast.config.file=" + hazecastConfigFile,
                                           "-Dhazelcast.jcache.provider.type=client",
                                           "-Dcom.ibm.ws.beta.edition=true")); // TODO Remove when GA'd
    }

    @Override
    public String getCachingProviderName() {
        return "HazelcastCachingProvider";
    }

    /**
     * Checks if multicast should be disabled in Hazelcast. We want to disable multicase on z/OS,
     * and when the environment variable disable_multicast_in_fats=true.
     *
     * If you are seeing a lot of NPE errors while running this FAT bucket you might need to set
     * disable_multicast_in_fats to true. This has been needed on some personal Linux systems, as
     * well as when running through a VPN.
     *
     * @return true if multicast should be disabled.
     */
    public static boolean isMulticastDisabled() {
        boolean multicastDisabledProp = Boolean.parseBoolean(System.getenv("disable_multicast_in_fats"));
        String osName = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);

        return (multicastDisabledProp || osName.contains("z/os"));
    }

    @Override
    public void beforeTest() {
        // Do nothing.
    }

    @Override
    public boolean cacheShouldExistBeforeTest() {
        /*
         * This occurs b/c our hazelcast configuration files contain the templates for the caches. Apparently,
         * Hazelcast returns caches that have been defined in configuration without having to call create on
         * them first.
         */
        return true;
    }

    @Override
    public boolean skipTtlTest() {
        return false;
    }
}
