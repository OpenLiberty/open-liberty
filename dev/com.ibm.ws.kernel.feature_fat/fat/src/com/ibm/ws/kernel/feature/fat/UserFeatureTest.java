/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2019
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.kernel.feature.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
public class UserFeatureTest {

    private static final Class<?> c = UserFeatureTest.class;
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.fat.user");

    private static final String USER_FEATURE_USERTEST_MF = "usertest.mf";
    private static final String USER_FEATURE_PRIVATE_MF = "usertestprivate-1.0.mf";

    private static final String USER_FEATURE_USERTEST_JAR = "usertest_1.0.0.jar";

    private static final String installFeatureMsgPrefix = "CWWKF0012I:";
    private static final String uninstallFeatureMsgPrefix = "CWWKF0013I:";
    private static final String notFoundFeatureMsgPrefix = "CWWKF0001E";
    private static final String notPublicFeatureMsgPrefix = "CWWKF0021E:";

    private static final String USER_FEATURE_PATH = "usr/extension/lib/features/";
    private static final String USER_BUNDLE_PATH = "usr/extension/lib/";

    private static final String CACHE_DIRECTORY = "workarea/platform/";
    private static final String FEATURE_BUNDLE_CACHE = CACHE_DIRECTORY + "feature.bundles.cache";

    /**
     * Copy the necessary features and bundles to the liberty server directories
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setup() throws Exception {
        final String METHOD_NAME = "classSetUp";

        Log.entering(c, METHOD_NAME);

        server.copyFileToLibertyInstallRoot(USER_FEATURE_PATH, USER_FEATURE_USERTEST_MF);
        server.copyFileToLibertyInstallRoot(USER_FEATURE_PATH, USER_FEATURE_PRIVATE_MF);

        server.copyFileToLibertyInstallRoot(USER_FEATURE_PATH, "featureVv-1.0.mf");

        server.copyFileToLibertyInstallRoot(USER_BUNDLE_PATH, USER_FEATURE_USERTEST_JAR);
        server.copyFileToLibertyInstallRoot(USER_BUNDLE_PATH, "bundlev_1.0.0.jar");

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * This method removes all the testing artifacts from the server directories.
     *
     * @throws Exception
     */
    @AfterClass
    public static void cleanup() throws Exception {
        final String METHOD_NAME = "classTearDown";

        Log.entering(c, METHOD_NAME);

        if (server.isStarted())
            server.stopServer();

        server.deleteFileFromLibertyInstallRoot("lib/features/featureWw-1.0.mf");

        server.deleteFileFromLibertyInstallRoot(USER_FEATURE_PATH + USER_FEATURE_USERTEST_MF);
        server.deleteFileFromLibertyInstallRoot(USER_FEATURE_PATH + USER_FEATURE_PRIVATE_MF);
        server.deleteFileFromLibertyInstallRoot(USER_FEATURE_PATH + "ufeature1-1.0.mf");
        server.deleteFileFromLibertyInstallRoot(USER_FEATURE_PATH + "ufeatureA-1.0.mf");
        server.deleteFileFromLibertyInstallRoot(USER_FEATURE_PATH + "featureVv-1.0.mf");
        server.deleteFileFromLibertyInstallRoot(USER_FEATURE_PATH + "featureWw-1.0.mf");

        server.deleteFileFromLibertyInstallRoot(USER_BUNDLE_PATH + USER_FEATURE_USERTEST_JAR);
        server.deleteFileFromLibertyInstallRoot(USER_BUNDLE_PATH + "ubundle1_1.0.0.jar");
        server.deleteFileFromLibertyInstallRoot(USER_BUNDLE_PATH + "bundlev_1.0.0.jar");

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * This method removes all the testing artifacts from the server directories.
     *
     * @throws Exception
     */
    @After
    public void stopServer() throws Exception {
        final String METHOD_NAME = "stopServer";

        Log.entering(c, METHOD_NAME);

        if (server.isStarted())
            server.stopServer();

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a user feature is installed when added to server.xml.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testUserFeatureInstalled() throws Exception {
        final String METHOD_NAME = "testUserFeatureInstalled";

        Log.entering(c, METHOD_NAME);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(METHOD_NAME + ".log");

        // Now move the server xml with a user feature available
        TestUtils.makeConfigUpdateSetMark(server, "server_user_features.xml");
        // Get the install feature message for the added user feature.
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("usertest user feature was not installed and should have been: " + output, output.contains("usertest"));

        // Finally check that removing the user feature from server.xml will uninstall it.
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("user feature usertest was not uninstalled and should have been: " + output, output.contains("usertest"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a message comes out indicating that a non public feature is specified in server.xml.
     *
     * @throws Exception
     */
    @Test
    public void testPrivateUserFeatureInServerXml() throws Exception {
        final String METHOD_NAME = "testPrivateUserFeatureInServerXml";

        Log.entering(c, METHOD_NAME);

        if (server.isStarted())
            server.stopServer();

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(METHOD_NAME + ".log");

        // Now move the server xml with a user feature available
        TestUtils.makeConfigUpdateSetMark(server, "server_user_private_feature.xml");
        // Get the not public feature message for the added user feature.
        String output = server.waitForStringInLogUsingMark(notPublicFeatureMsgPrefix);
        assertNotNull("We haven't found the " + notPublicFeatureMsgPrefix + " in the logs.", output);

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a user feature is installed when added to server.xml.
     *
     * @throws Exception
     */
    @Test
    public void testUserFeatureInstalledAtStartup() throws Exception {
        final String METHOD_NAME = "testUserFeatureInstalledAtStartup";

        Log.entering(c, METHOD_NAME);

        // Start with features installed.
        server.setServerConfigurationFile("server_user_features.xml");
        server.startServer(METHOD_NAME + ".log");

        // Check that user feature was there by uninstalling it.
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");
        String output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("user feature usertest was not uninstalled and should have been: " + output, output.contains("usertest"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a user feature is installed when added to server.xml.
     * And also ensures that the server starts properly when done cached start i.e, without --clean
     *
     * @throws Exception
     */
    @Test
    public void testCachedServerStartWithUserFeature() throws Exception {
        final String METHOD_NAME = "testCachedServerStartWithUserFeature";

        Log.entering(c, METHOD_NAME);

        server.setServerConfigurationFile("server_with_user_feature_only.xml");
        server.startServerAndValidate(true, true, true, false, false);

        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);
        server.stopServer();

        //Restart the server without clean
        server.startServerAndValidate(false, false, true, false, false);
        output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " for server cached start in the logs.", output);
        server.stopServer();

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test tests with a user feature that does not have a .mf file.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.io.FileNotFoundException")
    public void testUserFeatureNotFound() throws Exception {
        final String METHOD_NAME = "testUserFeatureNotFound";

        Log.entering(c, METHOD_NAME);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(METHOD_NAME + ".log");

        // Now move the server xml with a user feature that will not be found.
        TestUtils.makeConfigUpdateSetMark(server, "server_user_features_not_found.xml");
        // Get the feature definition could not be found message.
        String output = server.waitForStringInLogUsingMark(notFoundFeatureMsgPrefix);
        assertNotNull("We haven't found the " + notFoundFeatureMsgPrefix + " in the logs.", output);

        assertTrue("usr:usertestnotfound user feature was installed and should have been: " + output, output.contains("usr:usertestnotfound"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a .mf file added to usr/extension/lib/features after the server is
     * up and running will be found and installed when the feature is specified in server.xml.
     * The test ensures that this happens during server update.
     *
     * The feature structure is as follows:
     *
     * ufeatureA is defined in ufeatureA.mf by IBM-ShortName: ufeatureA-1.0
     *
     * @throws Exception
     */
    @Test
    public void testUsrFeatureManifestFileAddedAfterServerIsAlreadyUp() throws Exception {
        final String METHOD_NAME = "testUsrFeatureManifestFileAddedAfterServerIsAlreadyUp";

        Log.entering(c, METHOD_NAME);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.deleteFileFromLibertyInstallRoot(USER_FEATURE_PATH + "ufeatureA-1.0.mf");
        server.startServer(METHOD_NAME + ".log");

        // copy mf file and bundle jar
        server.copyFileToLibertyInstallRoot(USER_FEATURE_PATH, "ufeatureA-1.0.mf");
        server.copyFileToLibertyInstallRoot(USER_BUNDLE_PATH, "ubundle1_1.0.0.jar");

        // Now move the server xml with ufeatureA
        TestUtils.makeConfigUpdateSetMark(server, "server_user_featureA.xml");
        // Get the install feature message for the initial set up of updated features
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("ufeatureA-1.0 was not installed and should have been: " + output, output.contains("ufeatureA-1.0"));

        // Now check that we have the expected bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("usr|ubundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("usr|ubundle1/[1.0.0,2.0.0)"));

        // Finally check that removing the feature will also uninstall the feature.
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("ufeatureA-1.0 was not uninstalled and should have been: " + output, output.contains("ufeatureA-1.0"));

        // Now check that bundles of the features are gone.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("usr|ubundle1/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("usr|ubundle1/[1.0.0,2.0.0)"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that the features
     * listed under Subsystem-Content of a user feature
     * are not installed because it's manifest file
     * does not exist.
     * The test ensures that this happens during server update.
     *
     * expected messages
     * CWWKF0001E: A feature definition could not be found for com.ibm.websphere.appserver.featureW-1.0
     * CWWKF0012I: The server installed the following features: [usr:featureV-1.0].
     *
     * The feature structure is as follows:
     *
     * featureV is a user feature, featureW is a normal feature.
     *
     * featureV is defined in featureVv.mf by IBM-ShortName: featureV-1.0
     * featureW is defined in featureWw.mf by IBM-ShortName: featureW-1.0
     *
     * featureV is a feature that has featureW listed under Subsystem-Content.
     * featureW is a feature that has no other features listed under Subsystem-Content.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.io.FileNotFoundException")
    public void testUsrSubsystemContentFeatureManifestFileNotAddedAfterServerIsAlreadyUp() throws Exception {
        final String METHOD_NAME = "testUsrSubsystemContentFeatureManifestFileNotAddedAfterServerIsAlreadyUp";

        Log.entering(c, METHOD_NAME);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        // ensure dependent feature manifest is not there when the server starts
        server.deleteFileFromLibertyInstallRoot("usr/extension/lib/features/featureWw-1.0.mf");
        server.deleteFileFromLibertyInstallRoot("lib/features/featureWw-1.0.mf");
        server.startServer(METHOD_NAME + ".log");

        // Now move the server xml with the first set of features available
        TestUtils.makeConfigUpdateSetMark(server, "server_user_featureV.xml");

        String output = server.waitForStringInLogUsingMark(notFoundFeatureMsgPrefix);
        assertNotNull("We haven't found the " + notFoundFeatureMsgPrefix + " in the logs.", output);
        assertTrue("featureW-1.0 was installed and should not have been: " + output, output.contains("featureW-1.0"));

        // Get the install feature message for the initial set up of updated features
        output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("usr:featureV-1.0 was not installed and should have been: " + output, output.contains("usr:featureV-1.0"));
        assertFalse("featureW-1.0 was installed and should not have been: " + output, output.contains("featureW-1.0"));

        // Now check that we have the expected bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("usr|bundlev/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|bundlev/[1.0.0,2.0.0)"));
        assertFalse("bundlew/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundlew/[1.0.0,2.0.0)"));

        // Finally check that removing the feature will also uninstall the dependant features.
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("usr:featureV-1.0 was not uninstalled and should have been: " + output, output.contains("usr:featureV-1.0"));
        assertFalse("featureW-1.0 was uninstalled and should not have been: " + output, output.contains("featureW-1.0"));

        // Now check that bundles of the features are gone.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("usr|bundlev/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|bundlev/[1.0.0,2.0.0)"));
        assertFalse("bundlew/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundlew/[1.0.0,2.0.0)"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a feature manifest file ending in .MF ie. uppercase,
     * will be found and installed when the feature is specified in server.xml.
     * The test ensures that this happens during server update.
     *
     * The feature structure is as follows:
     *
     * ufeature1 is defined in ufeature1.MF by IBM-ShortName: ufeature1-1.0
     *
     * @throws Exception
     */
    @Test
    public void testUsrFeatureManifestFileExtensionUpperCase() throws Exception {
        final String METHOD_NAME = "testUsrFeatureManifestFileExtensionUpperCase";

        Log.entering(c, METHOD_NAME);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        // copy mf file and bundle jar
        server.copyFileToLibertyInstallRoot(USER_FEATURE_PATH, "ufeature1-1.0.MF");
        server.copyFileToLibertyInstallRoot(USER_BUNDLE_PATH, "ubundle1_1.0.0.jar");

        server.startServer(METHOD_NAME + ".log");
        // Now move the server xml with ufeature1
        TestUtils.makeConfigUpdateSetMark(server, "server_user_feature1.xml");
        // Get the install feature message for the initial set up of updated features
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("ufeature1-1.0 was not installed and should have been: " + output, output.contains("ufeature1-1.0"));

        // Now check that we have the expected bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("usr|ubundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle1/[1.0.0,2.0.0)"));

        // Finally check that removing the feature will also uninstall the feature.
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("ufeature1-1.0 was not uninstalled and should have been: " + output, output.contains("ufeature1-1.0"));

        // Now check that bundles of the features are gone.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("usr|ubundle1/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle1/[1.0.0,2.0.0)"));

        Log.exiting(c, METHOD_NAME);
    }
}
