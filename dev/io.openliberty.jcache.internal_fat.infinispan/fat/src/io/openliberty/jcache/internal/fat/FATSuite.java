/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jcache.internal.fat;

import java.util.Random;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.TestContainerSuite;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import io.openliberty.jcache.internal.fat.docker.InfinispanContainer;
import io.openliberty.jcache.internal.fat.plugins.InfinispanTestPlugin;
import io.openliberty.jcache.internal.fat.plugins.TestPluginHelper;

@SuppressWarnings("restriction")
@RunWith(Suite.class)
@SuiteClasses({
                JCacheAuthenticationCacheTest.class,
                JCacheJwtAuthenticationCacheTest.class,
                JCacheJwtLoggedOutCookieCacheTest.class,
                JCacheLtpaLoggedOutCookieCacheTest.class,
                JCacheCustomPrincipalCastingTest.class,
                JCacheDeleteAuthCacheTest.class,
                JCacheAuthCacheFailureTest.class,
                JCacheSamlAuthenticationCacheTest.class,
                JCacheSpnegoAuthenticationCacheTest.class,
                JCacheOauth20AuthenticationCacheTest.class,
                JCacheOidcClientAuthenticationCacheTest.class,
                JCacheOidcLoginAuthenticationCacheTest.class,
                JCacheProviderInAppTest.class,
                JCacheLtpaLoggedOutCookieCacheServerRestartTest.class,
                JCacheDynamicUpdateTest.class,
                JCacheAuthenticationCacheServerRestartTest.class
})
public class FATSuite extends TestContainerSuite {

    /*
     * Run EE9 tests in LITE mode and run all tests in FULL mode.
     */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                    .andWith(new JakartaEE9Action());

    @ClassRule
    public static InfinispanContainer infinispan = new InfinispanContainer();

    private static String hotrodFile;
    private static final String HOTROD_FILE = "infinispan_hotrod.props";
    private static final String HOTROD_NEARCACHE_FILE = "infinispan_hotrod_near_cache.props";

    static {
        /*
         * Choose one of the Hotrod properties files. This will ensure we test with and without nearside cache.
         */
        boolean forceNearCache = Boolean.getBoolean("fat.test.force.nearcache");
        hotrodFile = (forceNearCache || new Random().nextBoolean()) ? HOTROD_NEARCACHE_FILE : HOTROD_FILE;
        Log.info(FATSuite.class, "<clinit>", "Infinispan will use the following Hotrod props file: " + hotrodFile);
    }

    @BeforeClass
    public static void beforeSuite() throws Exception {
        TestPluginHelper.setTestPlugin(new InfinispanTestPlugin(hotrodFile));

        /*
         * Delete the Infinispan jars that might have been left around by previous test buckets.
         */
        LibertyServer server = LibertyServerFactory.getLibertyServer("io.openliberty.jcache.internal.fat.auth.cache.1");
        Machine machine = server.getMachine();
        String installRoot = server.getInstallRoot();
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/infinispan");
    }
}
