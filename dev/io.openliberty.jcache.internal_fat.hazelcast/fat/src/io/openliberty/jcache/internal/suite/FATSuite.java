/*******************************************************************************
 * Copyright (c) 2021, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jcache.internal.suite;

import java.util.Locale;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.Machine;

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import io.openliberty.jcache.internal.fat.JCacheAuthCacheFailureTest;
import io.openliberty.jcache.internal.fat.JCacheAuthenticationCacheServerRestartTest;
import io.openliberty.jcache.internal.fat.JCacheAuthenticationCacheTest;
import io.openliberty.jcache.internal.fat.JCacheCustomPrincipalCastingTest;
import io.openliberty.jcache.internal.fat.JCacheDeleteAuthCacheTest;
import io.openliberty.jcache.internal.fat.JCacheDynamicUpdateTest;
import io.openliberty.jcache.internal.fat.JCacheJwtAuthenticationCacheTest;
import io.openliberty.jcache.internal.fat.JCacheJwtLoggedOutCookieCacheTest;
import io.openliberty.jcache.internal.fat.JCacheLtpaLoggedOutCookieCacheServerRestartTest;
import io.openliberty.jcache.internal.fat.JCacheLtpaLoggedOutCookieCacheTest;
import io.openliberty.jcache.internal.fat.JCacheOauth20AuthenticationCacheTest;
import io.openliberty.jcache.internal.fat.JCacheOidcClientAuthenticationCacheTest;
import io.openliberty.jcache.internal.fat.JCacheOidcLoginAuthenticationCacheTest;
import io.openliberty.jcache.internal.fat.JCacheProviderInAppTest;
import io.openliberty.jcache.internal.fat.JCacheSamlAuthenticationCacheTest;
import io.openliberty.jcache.internal.fat.JCacheSpnegoAuthenticationCacheTest;
import io.openliberty.jcache.internal.fat.plugins.HazelcastTestPlugin;
import io.openliberty.jcache.internal.fat.plugins.TestPluginHelper;

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

    public static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");

    @ClassRule
    public static RepeatTests repeat;

    static {
        if (isWindows && !FATRunner.FAT_TEST_LOCALRUN) {
            repeat = RepeatTests.with(FeatureReplacementAction.NO_REPLACEMENT().conditionalFullFATOnly(EmptyAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                            .andWith(FeatureReplacementAction.EE10_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                            .andWith(FeatureReplacementAction.EE11_FEATURES());
        } else {
            repeat = RepeatTests.with(FeatureReplacementAction.NO_REPLACEMENT().fullFATOnly())
                            .andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                            .andWith(FeatureReplacementAction.EE10_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                            .andWith(FeatureReplacementAction.EE11_FEATURES());
        }
    }

    @BeforeClass
    public static void beforeSuite() throws Exception {
        TestPluginHelper.setTestPlugin(new HazelcastTestPlugin());

        /*
         * Delete the Hazelcast jars that might have been left around by previous test buckets.
         */
        LibertyServer server = LibertyServerFactory.getLibertyServer("io.openliberty.jcache.internal.fat.auth.cache.1");
        Machine machine = server.getMachine();
        String installRoot = server.getInstallRoot();
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/hazelcast");
    }
}
