/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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
package io.openliberty.checkpoint.jcache.fat;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.Machine;

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import io.openliberty.jcache.internal.fat.plugins.HazelcastTestPlugin;
import io.openliberty.jcache.internal.fat.plugins.TestPluginHelper;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                JCacheAuthenticationCacheTest.class,
                JCacheProviderInAppTest.class,
                JCacheJwtAuthenticationCacheTest.class,
                JCacheLtpaLoggedOutCookieCacheTest.class
})
public class FATSuite extends TestContainerSuite {

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

    public static RepeatTests defaultMPRepeat(String[] serverNames) {
        return MicroProfileActions.repeat(serverNames,
                                          MicroProfileActions.MP61, // first test in LITE mode
                                          MicroProfileActions.MP41, // rest are FULL mode
                                          MicroProfileActions.MP50);
    }
}
