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
package io.openliberty.jcache.internal.fat.plugins;

import java.util.Arrays;

import componenttest.topology.impl.LibertyServer;
import io.openliberty.jcache.internal.fat.FATSuite;

/**
 * See Infinispan hotrod properties here:
 *
 * https://docs.jboss.org/infinispan/12.1/apidocs/org/infinispan/client/hotrod/configuration/package-summary.html
 */
public class InfinispanTestPlugin implements TestPlugin {

    private final String hotrodFile;

    public InfinispanTestPlugin(String hotrodFile) {
        this.hotrodFile = hotrodFile;
    }

    @Override
    public void setupServer1(LibertyServer server, String clusterName, Integer authCacheMaxSize, Integer authCacheTtlSecs) throws Exception {
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
         * Set JVM options.
         */
        server.setJvmOptions(Arrays.asList("-Dinfinispan.cluster.name=" + clusterName,
                                           "-Dauthcache.max.size=" + authCacheMaxSize,
                                           "-Dauthcache.entry.ttl=" + (1000 * authCacheTtlSecs),
                                           "-Dinfinispan.client.hotrod.uri=" + FATSuite.infinispan.getHotRodUri(),
                                           "-Dinfinispan.hotrod.file=" + hotrodFile));
    }

    @Override
    public void setupServer2(LibertyServer server, String clusterName) throws Exception {
        /*
         * Set JVM options.
         */
        server.setJvmOptions(Arrays.asList("-Dinfinispan.cluster.name=" + clusterName,
                                           "-Dinfinispan.client.hotrod.uri=" + FATSuite.infinispan.getHotRodUri(),
                                           "-Dinfinispan.hotrod.file=" + hotrodFile));
    }

    @Override
    public String getCachingProviderName() {
        return "org.infinispan.jcache.remote.JCachingProvider";
    }

    @Override
    public void beforeTest() throws Exception {
        FATSuite.infinispan.deleteAllCaches();
    }

    @Override
    public void afterTest() throws Exception {
        // Nothing to do.
    }

    @Override
    public boolean skipTtlTest() {
        /*
         * Disable b/c tests were failing due to the JCache provider not evicting in a short / timely fashion.
         * Besides, we are only testing whether the JCache provider is evicting, which isn't really our
         * functionality.
         */
        return true;
    }
}
