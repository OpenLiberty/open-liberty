/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.kernel.feature.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class SystemBundleOverrideTest {

    private final static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.override");
    private final String bundleActivatorEyecatcher = "override.systemFeature.requiresJavaxRMI.Activator.start: javax.rmi.PortableRemoteObject loaded from";

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
        if (server.fileExistsInLibertyInstallRoot(SYS_FEATURES_SUBDIR + "override.systemFeature.requiresJavaxRMI.mf")) {
            server.deleteFileFromLibertyInstallRoot(SYS_FEATURES_SUBDIR + "override.systemFeature.requiresJavaxRMI.mf");
        }
        if (server.fileExistsInLibertyInstallRoot(SYS_FEATURES_SUBDIR + "override.systemFeature.providesJavaxRMI.mf")) {
            server.deleteFileFromLibertyInstallRoot(SYS_FEATURES_SUBDIR + "override.systemFeature.providesJavaxRMI.mf");
        }
        if (server.fileExistsInLibertyInstallRoot(SYS_FEATURES_SUBDIR + "override.userFeature.providesJavaxRMI.mf")) {
            server.deleteFileFromLibertyInstallRoot(SYS_FEATURES_SUBDIR + "override.userFeature.providesJavaxRMI.mf");
        }
        if (server.fileExistsInLibertyInstallRoot(USR_FEATURES_SUBDIR + "override.systemFeature.requiresJavaxRMI.mf")) {
            server.deleteFileFromLibertyInstallRoot(USR_FEATURES_SUBDIR + "override.systemFeature.requiresJavaxRMI.mf");
        }
        if (server.fileExistsInLibertyInstallRoot(USR_FEATURES_SUBDIR + "override.systemFeature.providesJavaxRMI.mf")) {
            server.deleteFileFromLibertyInstallRoot(USR_FEATURES_SUBDIR + "override.systemFeature.providesJavaxRMI.mf");
        }
        if (server.fileExistsInLibertyInstallRoot(USR_FEATURES_SUBDIR + "override.userFeature.providesJavaxRMI.mf")) {
            server.deleteFileFromLibertyInstallRoot(USR_FEATURES_SUBDIR + "override.userFeature.providesJavaxRMI.mf");
        }

        // Install test features
        server.installSystemFeature("override.systemFeature.requiresJavaxRMI");
        server.installSystemBundle("override.systemFeature.requiresJavaxRMI_1.0.0");

        server.installSystemFeature("override.systemFeature.providesJavaxRMI");
        server.installSystemBundle("override.systemFeature.providesJavaxRMI_1.0.0");

        server.installUserFeature("override.userFeature.providesJavaxRMI");
        server.installUserBundle("override.userFeature.providesJavaxRMI_1.0.0");

        server.setServerConfigurationFile("override.server.xml");

        // All features and bundles in place, start the server
        server.startServer("SystemBundleOverrideTest.log");

        Log.exiting(TEST_CLASS, "setup");
    }

    @After
    public void teardown() throws Exception {
        Log.entering(TEST_CLASS, "teardown");

        server.stopServer();

        server.uninstallSystemFeature("override.systemFeature.requiresJavaxRMI");
        server.uninstallSystemBundle("override.systemFeature.requiresJavaxRMI_1.0.0");

        server.uninstallSystemFeature("override.systemFeature.providesJavaxRMI");
        server.uninstallSystemBundle("override.systemFeature.providesJavaxRMI_1.0.0");

        server.uninstallUserFeature("override.userFeature.providesJavaxRMI");
        server.uninstallUserBundle("override.userFeature.providesJavaxRMI_1.0.0");

        Log.exiting(TEST_CLASS, "teardown");
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void testSystemBundlePackagesOverrides() throws Exception {
        Log.entering(TEST_CLASS, "testSystemBundlePackagesOverrides");

        if (JavaInfo.forServer(server).majorVersion() >= 11) {
            // javax.rmi is no longer available in JDK 11
            Log.info(TEST_CLASS, "testSystemBundlePackagesOverrides", "Skipping test on Java >= 11");
            return;
        }

        try {
            // The override.systemFeature.requiresJavaxRMI_1.0.0 Bundle activator start method looks like this:

            // Class clazz = PortableRemoteObject.class;
            // URL location = clazz.getResource('/' + clazz.getName().replace('.', '/') + ".class");
            // System.out.println("override.systemFeature.requiresJavaxRMI.Activator.start: javax.rmi.PortableRemoteObject loaded from: " + location.getPath());

            // This will give the URL to the location where the PortableRemoteObject.class  was loaded from. I felt it was a better test
            // to avoid using osgi/equinox interfaces to verify where the class was loading from

            String JAVA_RUNTIME_ID = JavaInfo.forServer(server).majorVersion() >= 9 ? ".*jrt.*" : ".*rt.jar.*";

            // Case 1
            // A system feature requires javax.rmi, which is provided by base java.
            // Check activator message to make sure it was loaded from the rt.jar
            TestUtils.makeConfigUpdateSetMark(server, "override.case1.server.xml");
            assertNotNull(server.waitForStringInLogUsingMark(bundleActivatorEyecatcher + JAVA_RUNTIME_ID, timeout));

            // Case 2
            // A system feature requires javax.rmi, which is now provided from a newly installed system feature
            // Check activator message to make sure it was loaded from a bundleresource and not rt.jar
            TestUtils.makeConfigUpdateSetMark(server, "override.case2.server.xml");
            assertNotNull(server.waitForStringInLogUsingMark(bundleActivatorEyecatcher + ".*bundleresource.*", timeout));

            // Case 3
            // A system feature requires javax.rmi, but the new system feature added in case 2 is removed, so
            // it is again provide by base java.
            // Check activator message to make sure it was loaded from rt.jar once again
            TestUtils.makeConfigUpdateSetMark(server, "override.case1.server.xml");
            assertNotNull(server.waitForStringInLogUsingMark(bundleActivatorEyecatcher + JAVA_RUNTIME_ID, timeout));

            // Case 4
            // Switch back to Case 2 such that on the following case, the bundle will refresh and reactivate and
            // display the activator eyecatcher again.
            TestUtils.makeConfigUpdateSetMark(server, "override.case2.server.xml");
            assertNotNull(server.waitForStringInLogUsingMark(bundleActivatorEyecatcher + ".*bundleresource.*", timeout));

            // Case 5
            // A system feature requires javax.rmi, which is provided by base java.
            // A user feature is installed that exports/provides javax.rmi, however, this should not
            // override the system bundle export. We don't want user features to be able to override
            // system packages and alter the behavior of the core Liberty features
            TestUtils.makeConfigUpdateSetMark(server, "override.case3.server.xml");
            assertNotNull(server.waitForStringInLogUsingMark(bundleActivatorEyecatcher + JAVA_RUNTIME_ID, timeout));
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

        if (JavaInfo.forServer(server).majorVersion() >= 11) {
            // javax.rmi is no longer available in JDK 11
            Log.info(TEST_CLASS, "testSystemBundlePackagesOverridesWarmStarts", "Skipping test on Java >= 11");
            return;
        }

        try {
            // The override.systemFeature.requiresJavaxRMI_1.0.0 Bundle activator start method looks like this:

            // Class clazz = PortableRemoteObject.class;
            // URL location = clazz.getResource('/' + clazz.getName().replace('.', '/') + ".class");
            // System.out.println("override.systemFeature.requiresJavaxRMI.Activator.start: javax.rmi.PortableRemoteObject loaded from: " + location.getPath());

            // This will give the URL to the location where the PortableRemoteObject.class  was loaded from. I felt it was a better test
            // to avoid using osgi/equinox interfaces to verify where the class was loading from

            String JAVA_RUNTIME_ID = JavaInfo.forServer(server).majorVersion() >= 9 ? ".*jrt.*" : ".*rt.jar.*";

            // Case 1
            // A system feature requires javax.rmi, which is provided by base java.
            // Check activator message to make sure it was loaded from the rt.jar
            TestUtils.makeConfigUpdateSetMark(server, "override.case1.server.xml");
            assertNotNull(server.waitForStringInLogUsingMark(bundleActivatorEyecatcher + JAVA_RUNTIME_ID, timeout));

            // Case 2
            // A system feature requires javax.rmi, which is now provided from a newly installed system feature
            // Check activator message to make sure it was loaded from a bundleresource and not rt.jar
            TestUtils.makeConfigUpdateSetMark(server, "override.case2.server.xml");
            assertNotNull(server.waitForStringInLogUsingMark(bundleActivatorEyecatcher + ".*bundleresource.*", timeout));

            // Case 3
            // Make sure that Case 2 still works on a warm start and that no exceptions occur
            server.stopServer();
            server.startServer("SystemOverrideTest2.log");
            assertNotNull(server.waitForStringInLogUsingMark(bundleActivatorEyecatcher + ".*bundleresource.*", timeout));
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