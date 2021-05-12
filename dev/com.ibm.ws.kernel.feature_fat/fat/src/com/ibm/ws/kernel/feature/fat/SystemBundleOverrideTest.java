/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.kernel.feature.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class SystemBundleOverrideTest {

    private static final String SYSTEM_PROVIDES_API_FEATURE = "override.systemFeature.providesSwing";
    private static final String SYSTEM_REQUIRES_API_FEATURE = "override.systemFeature.requiresSwing";
    private static final String USER_PROVIDES_API_FEATURE = "override.userFeature.providesSwing";
    private static final List<String> ALLTEST_FEATURES = Arrays.asList(SYSTEM_PROVIDES_API_FEATURE, SYSTEM_REQUIRES_API_FEATURE, USER_PROVIDES_API_FEATURE);

    private final static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.override");
    private final long timeout = 30000;

    private static final Class<?> TEST_CLASS = SystemBundleOverrideTest.class;

    private static final String SYS_FEATURES_SUBDIR = "lib/features/";
    private static final String USR_FEATURES_SUBDIR = "usr/extension/lib/features/";

    @Before
    public void setup() throws Exception {
        Log.entering(TEST_CLASS, "setup");

        // Scrub test feature manifests from all possible install root subdirectories
        if (server.isStarted()) {
            server.stopServer();
        }
        for (String f : ALLTEST_FEATURES) {
            if (server.fileExistsInLibertyInstallRoot(SYS_FEATURES_SUBDIR + f + ".mf")) {
                server.deleteFileFromLibertyInstallRoot(SYS_FEATURES_SUBDIR + f + ".mf");
            }
            if (server.fileExistsInLibertyInstallRoot(USR_FEATURES_SUBDIR + f + ".mf")) {
                server.deleteFileFromLibertyInstallRoot(USR_FEATURES_SUBDIR + f + ".mf");
            }
        }

        // Install test features
        server.installSystemFeature(SYSTEM_PROVIDES_API_FEATURE);
        server.installSystemBundle(SYSTEM_PROVIDES_API_FEATURE);

        server.installSystemFeature(SYSTEM_REQUIRES_API_FEATURE);
        server.installSystemBundle(SYSTEM_REQUIRES_API_FEATURE);

        server.installUserFeature(USER_PROVIDES_API_FEATURE);
        server.installUserBundle(USER_PROVIDES_API_FEATURE);

        server.setServerConfigurationFile("override.server.xml");

        // All features and bundles in place, start the server
        server.startServer("SystemBundleOverrideTest.log");

        Log.exiting(TEST_CLASS, "setup");
    }

    @After
    public void teardown() throws Exception {
        Log.entering(TEST_CLASS, "teardown");

        server.stopServer();

        server.uninstallSystemFeature(SYSTEM_PROVIDES_API_FEATURE);
        server.uninstallSystemBundle(SYSTEM_PROVIDES_API_FEATURE);

        server.uninstallSystemFeature(SYSTEM_REQUIRES_API_FEATURE);
        server.uninstallSystemBundle(SYSTEM_REQUIRES_API_FEATURE);

        server.uninstallUserFeature(USER_PROVIDES_API_FEATURE);
        server.uninstallUserBundle(USER_PROVIDES_API_FEATURE);

        Log.exiting(TEST_CLASS, "teardown");
    }

    private final static String ACTIVATION_START_MESSAGE = "SUCCESS: wire found to -> .*";
    static final String BSN_SYSTEM = "org.eclipse.osgi";
    static final String BSN_FEATURE = "override.systemFeature.providesSwing";

    private void checkActivationStartMessage(String expectedProvider) {
        // The test Bundle activator start method looks for its own wiring for the javax.swing.plaf package
        // and prints out this:
        // SUCCESS: wire found to -> <provider BSN>
        String startMessage = server.waitForStringInLogUsingMark(ACTIVATION_START_MESSAGE, timeout);
        assertNotNull("No activation start message.", startMessage);
        assertTrue("Wrong provider: " + startMessage, startMessage.endsWith(expectedProvider));
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void testSystemBundlePackagesOverrides() throws Exception {
        Log.entering(TEST_CLASS, "testSystemBundlePackagesOverrides");

        try {
            // Case 1
            // A system feature requires javax.swing.plaf, which is provided by base java.
            // Check activator message to make sure it was loaded from the system bundle
            TestUtils.makeConfigUpdateSetMark(server, "override.case1.server.xml");
            checkActivationStartMessage(BSN_SYSTEM);

            // Case 2
            // A system feature requires javax.swing.plaf, which is now provided from a newly installed system feature
            // Check activator message to make sure it was loaded from the feature bundle
            TestUtils.makeConfigUpdateSetMark(server, "override.case2.server.xml");
            checkActivationStartMessage(BSN_FEATURE);

            // Case 3
            // A system feature requires javax.swing.plaf, but the new system feature added in case 2 is removed, so
            // it is again provide by base java.
            // Check activator message to make sure it was loaded from the system bundle once again
            TestUtils.makeConfigUpdateSetMark(server, "override.case1.server.xml");
            checkActivationStartMessage(BSN_SYSTEM);

            // Case 4
            // Switch back to Case 2 such that on the following case, the bundle will refresh and reactivate and
            // display the activator eyecatcher again.
            TestUtils.makeConfigUpdateSetMark(server, "override.case2.server.xml");
            checkActivationStartMessage(BSN_FEATURE);

            // Case 5
            // A system feature requires javax.swing.plaf, which is provided by base java.
            // A user feature is installed that exports/provides javax.swing.plaf, however, this should not
            // override the system bundle export. We don't want user features to be able to override
            // system packages and alter the behavior of the core Liberty features
            TestUtils.makeConfigUpdateSetMark(server, "override.case3.server.xml");
            checkActivationStartMessage(BSN_SYSTEM);
        } catch (Exception e) {
            Log.error(TEST_CLASS, "testSystemBundlePackagesOverrides", e);
            throw e;
        } finally {
            // Revert to the original xml
            TestUtils.makeConfigUpdateSetMark(server, "override.server.xml");
        }

        Log.exiting(TEST_CLASS, "testSystemBundlePackagesOverrides");
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void testSystemBundlePackagesOverridesWarmStarts() throws Exception {
        Log.entering(TEST_CLASS, "testSystemBundlePackagesOverridesWarmStarts");

        try {
            // Case 2
            // A system feature requires javax.swing.plaf, which is now provided from a newly installed system feature
            // Check activator message to make sure it was loaded from the feature bundle
            TestUtils.makeConfigUpdateSetMark(server, "override.case2.server.xml");
            checkActivationStartMessage(BSN_FEATURE);

            // Case 3
            // Make sure that Case 2 still works on a warm start and that no exceptions occur
            server.stopServer();
            server.startServer("SystemOverrideTest2.log", false);
            checkActivationStartMessage(BSN_FEATURE);

            // Case 4
            // Touch the timestamp of the equinox region bundle to force it to be re-installed
            // Make sure we still are wired to the feature bundle
            server.stopServer();
            RemoteFile libDir = server.getFileFromLibertyInstallRoot("lib");
            RemoteFile[] libs = libDir.list(false);
            for (RemoteFile lib : libs) {
                if (lib.getName().contains("equinox.region")) {
                    File libFile = new File(lib.getAbsolutePath());
                    libFile.setLastModified(libFile.lastModified() + 100);
                }
            }
            server.startServer("SystemOverrideTest3.log", false);
            checkActivationStartMessage(BSN_FEATURE);
        } catch (Exception e) {
            Log.error(TEST_CLASS, "testSystemBundlePackagesOverridesWarmStarts", e);
            throw e;
        } finally {
            // Revert to the original xml
            TestUtils.makeConfigUpdateSetMark(server, "override.server.xml");
        }

        Log.exiting(TEST_CLASS, "testSystemBundlePackagesOverridesWarmStarts");
    }
}