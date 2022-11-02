/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import io.openliberty.jcache.internal.fat.docker.InfinispanContainer;

/**
 * See Infinispan hotrod properties here:
 *
 * https://docs.jboss.org/infinispan/12.1/apidocs/org/infinispan/client/hotrod/configuration/package-summary.html
 */
public class InfinispanTestPlugin implements TestPlugin {

    private final String hotrodFile;

    public static InfinispanContainer infinispan = new InfinispanContainer();

    public InfinispanTestPlugin(String hotrodFile) {
        this.hotrodFile = hotrodFile;
    }

    @Override
    public void setupServer1(LibertyServer server, String clusterName, Integer authCacheMaxSize, Integer authCacheTtlSecs) throws Exception {
        Log.info(InfinispanTestPlugin.class, "setupServer1", "Configuring server1");

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
                                           "-Dinfinispan.client.hotrod.uri=" + infinispan.getHotRodUri(),
                                           "-Dinfinispan.hotrod.file=" + hotrodFile));
    }

    @Override
    public void setupServer2(LibertyServer server, String clusterName) throws Exception {
        Log.info(InfinispanTestPlugin.class, "setupServer2", "Configuring server2");

        /*
         * Set JVM options.
         */
        server.setJvmOptions(Arrays.asList("-Dinfinispan.cluster.name=" + clusterName,
                                           "-Dinfinispan.client.hotrod.uri=" + infinispan.getHotRodUri(),
                                           "-Dinfinispan.hotrod.file=" + hotrodFile));
    }

    @Override
    public String getCachingProviderName() {
        return "org.infinispan.jcache.remote.JCachingProvider";
    }

    @Override
    public void beforeTest() throws Exception {
        /*
         * Start the Infinispan docker container before each tests. There were issues with
         * when we deleted Infinispan caches, where the REST and Java APIs said the caches
         * didn't exist, but Liberty was able to find them when it started up. Then sometime
         * during Libery's runtime execution, the cache would disappear and Liberty would
         * encounter errors. So instead of keeping the Infinispan docker container up during
         * the entire suite, we will bring it up before each test so there there should never
         * be any existing caches.
         */
        Log.info(InfinispanTestPlugin.class, "beforeTest", "Starting Infinispan Docker container...");
        infinispan.start();
        Log.info(InfinispanTestPlugin.class, "beforeTest", "Finished starting Infinispan Docker container.");
    }

    @Override
    public void afterTest() throws Exception {
        /*
         * Stop the Infinispan server.
         */
        Log.info(InfinispanTestPlugin.class, "afterTest", "Stopping Infinispan Docker container.");
        infinispan.stop();
        Log.info(InfinispanTestPlugin.class, "afterTest", "Finished stopping Infinispan Docker container.");
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
