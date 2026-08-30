/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package io.openliberty.classloading.base.fat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Tests for LibraryServiceExporter.
 * Migrated from WS-CD-Open com.ibm.ws.classloading.LibraryExporterFatTest.
 */
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class LibraryExporterFatTest {

    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("libraryPackagesServer");

    private static final String TEST_LIBRARY_ACCESS_BUNDLE = "test.library.packages";
    private static final String TEST_LIBRARY_PACKAGE_IMPORTER1 = "test.library.packages.importer1";
    private static final String TEST_LIBRARY_PACKAGE_IMPORTER2 = "test.library.packages.importer2";
    private static final String TEST_SYSTEM_FEATURE_NAME = "test.library.packages-1.0";
    private static final String[] TEST_SYSTEM_BUNDLE_NAMES =  {TEST_LIBRARY_ACCESS_BUNDLE, TEST_LIBRARY_PACKAGE_IMPORTER1};
    private static final String TEST_USER_FEATURE_NAME = "user.test.library.packages-1.0";
    private static final String[] TEST_USER_BUNDLE_NAMES = {TEST_LIBRARY_PACKAGE_IMPORTER2};
    private static final String PACKAGE1 = "test.library.packages.exporter1";
    private static final String PACKAGE2 = "test.library.packages.exporter2";
    private static final String CLASS_NAME = ".TestLibraryClass";
    private static final String TAG_PRESET = "PRE_SET_PACKAGES: ";
    private static final String TAG_POSTSET = "POST_SET_PACKAGES: ";
    private static final String REGION_KERNEL = "org.eclipse.equinox.region.kernel";
    private static final String REGION_USR = "liberty.extension.usr";

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Deploy the exporter jar to the server's lib/ directory (referenced by fileset in server.xml)
        JavaArchive exporterJar = ShrinkHelper.buildJavaArchive("test.library.packages.exporter.jar",
                "test.library.packages.exporter1",
                "test.library.packages.exporter2");
        ShrinkHelper.exportToServer(server, "lib", exporterJar);

        for (String bundleName : TEST_SYSTEM_BUNDLE_NAMES) {
            server.installSystemBundle(bundleName);
        }
        server.installSystemFeature(TEST_SYSTEM_FEATURE_NAME);
        for (String bundleName : TEST_USER_BUNDLE_NAMES) {
            server.installUserBundle(bundleName);
        }
        server.installUserFeature(TEST_USER_FEATURE_NAME);
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
        server.uninstallSystemFeature(TEST_SYSTEM_FEATURE_NAME);
        for (String bundleName : TEST_SYSTEM_BUNDLE_NAMES) {
            server.uninstallSystemBundle(bundleName);
        }
        server.uninstallUserFeature(TEST_USER_FEATURE_NAME);
        for (String bundleName : TEST_USER_BUNDLE_NAMES) {
            server.uninstallUserBundle(bundleName);
        }
    }

    @After
    public void resetConfiguration() {
        try {
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("libraryPackagesServer/serverPackageNoConfig.xml");
            server.waitForConfigUpdateInLogUsingMark(Collections.<String>emptySet());
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testConfigure1and2Packages() throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("libraryPackagesServer/serverPackage1and2Exported.xml");
        server.waitForConfigUpdateInLogUsingMark(Collections.<String>emptySet());
        checkPackageAccess(Arrays.asList(PACKAGE1, PACKAGE2), Collections.<String>emptyList());
    }

    @Test
    public void testConfigure1Packages() throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("libraryPackagesServer/serverPackage1Exported.xml");
        server.waitForConfigUpdateInLogUsingMark(Collections.<String>emptySet());
        checkPackageAccess(Collections.singletonList(PACKAGE1), Collections.singletonList(PACKAGE2));
    }

    @Test
    public void testConfigureNoPackages() throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("libraryPackagesServer/serverPackageNoneExported.xml");
        server.waitForConfigUpdateInLogUsingMark(Collections.<String>emptySet());
        checkPackageAccess(Collections.<String>emptyList(), Arrays.asList(PACKAGE1, PACKAGE2));
    }

    @Test
    public void testRestart() throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("libraryPackagesServer/serverPackage1and2Exported.xml");
        server.waitForConfigUpdateInLogUsingMark(Collections.<String>emptySet());
        server.stopServer();
        server.startServer(false);
        checkPackageAccess(Arrays.asList(PACKAGE1, PACKAGE2), Collections.<String>emptyList());
    }

    private void checkPackageAccess(Collection<String> passes, Collection<String> fails) {
        String prelogEntryKernel = server.waitForStringInLog(TAG_PRESET + REGION_KERNEL + ":.*", 500);
        // we always fail all packages in the pre log
        checkFails(prelogEntryKernel, Arrays.asList(PACKAGE1, PACKAGE2));
        String prelogEntryUsr = server.waitForStringInLog(TAG_PRESET + REGION_USR + ":.*", 500);
        checkFails(prelogEntryUsr, Arrays.asList(PACKAGE1, PACKAGE2));

        String postlogEntryKernel = server.waitForStringInLog(TAG_POSTSET + REGION_KERNEL + ":.*", 500);
        checkFails(postlogEntryKernel, fails);
        chechPasses(postlogEntryKernel, passes);
        // we always expect usr bundles to fail on post set
        String postlogEntryUsr = server.waitForStringInLog(TAG_POSTSET + REGION_USR + ":.*", 500);
        checkFails(postlogEntryUsr, Arrays.asList(PACKAGE1, PACKAGE2));
    }

    private void checkFails(String logEntry, Collection<String> fails) {
        for (String expectedFailurePackage : fails) {
            assertThat("Log message should specify a failure for the package: " + expectedFailurePackage,
                       logEntry,
                       containsString("FAILED." + expectedFailurePackage + ";"));
        }
    }

    private void chechPasses(String logEntry, Collection<String> passes) {
        for (String expectedPassPackage : passes) {
            assertThat("Log message should specify a pass for the package: " + expectedPassPackage,
                       logEntry,
                       containsString("PASSED." + expectedPassPackage + CLASS_NAME + ";"));
        }
    }
}
