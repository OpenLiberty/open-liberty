/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013, 2014
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
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class FeatureTest {

    private static final Class<?> c = FeatureTest.class;
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature");
    //private static final Logger logger = Logger.getLogger(FeatureTest.class.getName());

    private static final String FEATURE_V_MF = "featureVv-1.0.mf";
    private static final String FEATURE_X_MF = "featureXx-1.0.mf";
    private static final String FEATURE_Y_MF = "featureYy-1.0.mf";
    private static final String FEATURE_Z_MF = "featureZz-1.0.mf";

    private static final String[] features = { FEATURE_V_MF, FEATURE_X_MF, FEATURE_Y_MF, FEATURE_Z_MF };

    private static final String installFeatureMsgPrefix = "CWWKF0012I: The server installed the following features: \\[";
    private static final String uninstallFeatureMsgPrefix = "CWWKF0013I: The server removed the following features: \\[";
    private static final String notFoundFeatureMsgPrefix = "CWWKF0001E:";
    private static final String wrongProcessTypeMsgPrefix = "CWWKF0038E:";
    private static final String missingJavaDependency1 = "CWWKF0032E: The featureJavaEight-1.0 feature requires a minimum Java runtime environment version of JavaSE 1.888";
    private static final String missingJavaDependency2 = "CWWKF0032E: The featureJavaNine-1.0 feature requires a minimum Java runtime environment version of JavaSE 1.9";

    private static final String FEATURE_PATH = "lib/features/";

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

        for (String feature : features) {
            Log.info(c, METHOD_NAME, "Copying " + feature + " to " + FEATURE_PATH + ".");
            server.copyFileToLibertyInstallRoot(FEATURE_PATH, feature);
        }

        Log.info(c, METHOD_NAME, "Copying bundlev_1.0.0.jar to lib.");
        server.copyFileToLibertyInstallRoot("lib", "bundlev_1.0.0.jar");
        Log.info(c, METHOD_NAME, "Copying bundlex_1.0.0.jar to lib.");
        server.copyFileToLibertyInstallRoot("lib", "bundlex_1.0.0.jar");
        Log.info(c, METHOD_NAME, "Copying bundley_1.0.0.jar to lib.");
        server.copyFileToLibertyInstallRoot("lib", "bundley_1.0.0.jar");
        Log.info(c, METHOD_NAME, "Copying bundlez_1.0.0.jar to lib.");
        server.copyFileToLibertyInstallRoot("lib", "bundlez_1.0.0.jar");

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

        for (String feature : features) {
            Log.info(c, METHOD_NAME, FEATURE_PATH + feature + " will be deleted.");
            server.deleteFileFromLibertyInstallRoot("lib/features/" + feature);
        }
        server.deleteFileFromLibertyInstallRoot("lib/features/featureA-1.0.mf");
        server.deleteFileFromLibertyInstallRoot("lib/features/featureWw-1.0.mf");

        server.deleteFileFromLibertyInstallRoot("lib/bundlev_1.0.0.jar");
        server.deleteFileFromLibertyInstallRoot("lib/bundlew_1.0.0.jar");
        server.deleteFileFromLibertyInstallRoot("lib/bundlex_1.0.0.jar");
        server.deleteFileFromLibertyInstallRoot("lib/bundley_1.0.0.jar");
        server.deleteFileFromLibertyInstallRoot("lib/bundlez_1.0.0.jar");
        server.deleteFileFromLibertyInstallRoot("lib/bundle1_1.0.0.jar");

        server.deleteFileFromLibertyInstallRoot("lib/features/featureJavaEight-1.0.mf");
        server.deleteFileFromLibertyInstallRoot("lib/features/featureJavaNine-1.0.mf");
        server.deleteFileFromLibertyInstallRoot("lib/bundleJavaEight_1.0.0.jar");
        server.deleteFileFromLibertyInstallRoot("lib/bundleJavaNine_1.0.0.jar");

        Log.exiting(c, METHOD_NAME);
    }

    @After
    public void stopServer() throws Exception {
        if (server.isStarted())
            server.stopServer();
    }

    /**
     * TestDescription:
     * This test ensures that the name of the .mf file does not matter and that the features
     * listed under Subsystem-Content are also installed.
     * The test ensures that this happens during server update.
     *
     * The feature structure is as follows:
     *
     * featureX, featureY, featureZ are all normal features.
     *
     * featureX is defined in featureXx.mf by IBM-ShortName: featureX-1.0
     * featureY is defined in featureYy.mf by IBM-ShortName: featureY-1.0
     * featureZ is defined in featureZz.mf by IBM-ShortName: featureZ-1.0
     *
     * featureX is a feature that has featureY listed under Subsystem-Content.
     * featureY is a feature that has featureZ listed under Subsystem-Content.
     * featureZ is a feature that has no other features listed under Subsystem-Content.
     *
     * @throws Exception
     */
    @Test
    public void testSubsystemContentFeaturesInstallDuringServerUpdate() throws Exception {
        final String METHOD_NAME = "testSubsystemContentFeaturesInstallDuringServerUpdate";

        Log.entering(c, METHOD_NAME);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(METHOD_NAME + ".log");

        // Now move the server xml with the first set of features available (also resets mark)
        TestUtils.makeConfigUpdateSetMark(server, "server_add_featureX.xml");

        // Get the install feature message for the initial set up of updated features
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("featureX-1.0 was not installed and should have been: " + output, output.contains("featureX-1.0"));
        assertTrue("featureY-1.0 was not installed and should have been: " + output, output.contains("featureY-1.0"));
        assertTrue("featureZ-1.0 was not installed and should have been: " + output, output.contains("featureZ-1.0"));

        // Now check that we have the expected bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("bundlex/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundlex/[1.0.0,2.0.0)"));
        assertTrue("bundley/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundley/[1.0.0,2.0.0)"));
        assertTrue("bundlez/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundlez/[1.0.0,2.0.0)"));

        // Finally check that removing the feature will also uninstall the dependant features.
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);

        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("featureX-1.0 was not uninstalled and should have been: " + output, output.contains("featureX-1.0"));
        assertTrue("featureY-1.0 was not uninstalled and should have been: " + output, output.contains("featureY-1.0"));
        assertTrue("featureZ-1.0 was not uninstalled and should have been: " + output, output.contains("featureZ-1.0"));

        // Now check that bundles of the features are gone.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("bundlex/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundlex/[1.0.0,2.0.0)"));
        assertFalse("bundley/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundley/[1.0.0,2.0.0)"));
        assertFalse("bundlez/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundlez/[1.0.0,2.0.0)"));

        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testFeatureJavaVersionDependencyCheck() throws Exception {
        final String METHOD_NAME = "testFeatureJavaVersionDependencyCheck";

        if (JavaInfo.forServer(server).majorVersion() > 8)
            return;

        Log.entering(c, METHOD_NAME);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(METHOD_NAME + ".log");

        // copy mf file and bundle jar
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, "featureJavaEight-1.0.mf");
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, "featureJavaNine-1.0.mf");
        server.copyFileToLibertyInstallRoot("lib", "bundleJavaEight_1.0.0.jar");
        server.copyFileToLibertyInstallRoot("lib", "bundleJavaNine_1.0.0.jar");

        // Now move the server xml with the first set of features available
        server.setServerConfigurationFile("server_add_featureJavaEight.xml");

        // Should find feature needing Java 1.888
        String output = server.waitForStringInLogUsingMark(missingJavaDependency1);
        assertNotNull("No output message found: " + missingJavaDependency1, output);
        assertTrue("Missing java version check failure message javaEight : " + output, output.contains(missingJavaDependency1));

        // And one for (dependent feature needing Java 1.9)
        output = server.waitForStringInLogUsingMark(missingJavaDependency2);
        assertTrue("Missing java version check failure message javaNine : " + output, output.contains(missingJavaDependency2));

        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testClientFeatureInServerTest() throws Exception {
        final String METHOD_NAME = "testClientFeatureInServerTest";

        Log.entering(c, METHOD_NAME);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(METHOD_NAME + ".log");

        // copy mf file and bundle jar
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, "includeClientFeature-1.0.mf");

        // Now move the server xml with the first set of features available
        server.setServerConfigurationFile("server_with_client.xml");

        // Should find feature is not found
        String output = server.waitForStringInLogUsingMark(wrongProcessTypeMsgPrefix);
        assertNotNull("We haven't found the " + wrongProcessTypeMsgPrefix + " in the logs.", output);
        assertTrue("Client was installed and should not have been: " + output, output.contains("javaeeClient"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that the name of the .mf file does not matter and that the features
     * listed under Subsystem-Content are also installed even if the
     * Subsystem-Content feature's .mf file is not in lib/features when the
     * server is started and added later before server.xml is updated.
     * The test ensures that this happens during server update.
     *
     * The feature structure is as follows:
     *
     * featureV, featureW are normal features.
     *
     * featureV is defined in featureVv.mf by IBM-ShortName: featureV-1.0
     * featureW is defined in featureWw.mf by IBM-ShortName: featureW-1.0
     *
     * featureV is a feature that has featureW listed under Subsystem-Content.
     * featureW is a feature that has no other features listed under Subsystem-Content.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testSubsystemContentFeatureManifestFileAddedAfterServerIsAlreadyUp() throws Exception {
        final String METHOD_NAME = "testSubsystemContentFeatureManifestFileAddedAfterServerIsAlreadyUp";

        Log.entering(c, METHOD_NAME);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");

        // ensure dependent feature manifest is not there when the server starts
        server.deleteFileFromLibertyInstallRoot("lib/features/featureWw-1.0.mf");
        server.startServer(METHOD_NAME + ".log");

        // copy mf file and bundle jar in after server start
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, "featureWw-1.0.mf");
        server.copyFileToLibertyInstallRoot("lib", "bundlew_1.0.0.jar");

        // Now move the server xml with the first set of features available (and reset mark)
        TestUtils.makeConfigUpdateSetMark(server, "server_add_featureV.xml");
        // Get the install feature message for the initial set up of updated features
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("featureV-1.0 was not installed and should have been: " + output, output.contains("featureV-1.0"));
        assertTrue("featureW-1.0 was not installed and should have been: " + output, output.contains("featureW-1.0"));

        // Now check that we have the expected bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("bundlev/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundlev/[1.0.0,2.0.0)"));
        assertTrue("bundlew/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundlew/[1.0.0,2.0.0)"));

        // Finally check that removing the feature will also uninstall the dependant features. (and reset mark)
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("featureV-1.0 was not uninstalled and should have been: " + output, output.contains("featureV-1.0"));
        assertTrue("featureW-1.0 was not uninstalled and should have been: " + output, output.contains("featureW-1.0"));

        // Now check that bundles of the features are gone.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("bundlev/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundlev/[1.0.0,2.0.0)"));
        assertFalse("bundlew/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundlew/[1.0.0,2.0.0)"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that the name of the .mf file does not matter and that the features
     * listed under Subsystem-Content are not installed because it's manifest file
     * does not exist in lib/features.
     * The test ensures that this happens during server update.
     *
     * expected messages
     * CWWKF0001E: A feature definition could not be found for com.ibm.websphere.appserver.featureW-1.0
     * CWWKF0012I: The server installed the following features: [featureV-1.0].
     *
     * The feature structure is as follows:
     *
     * featureV, featureW are normal features.
     *
     * featureV is defined in featureVv.mf by IBM-ShortName: featureV-1.0
     * featureW is defined in featureWw.mf by IBM-ShortName: featureW-1.0
     *
     * featureV is a feature that has featureW listed under Subsystem-Content.
     * featureW is a feature that has no other features listed under Subsystem-Content.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    @AllowedFFDC("java.io.FileNotFoundException")
    public void testSubsystemContentFeatureManifestFileNotAddedAfterServerIsAlreadyUp() throws Exception {
        final String METHOD_NAME = "testSubsystemContentFeatureManifestFileNotAddedAfterServerIsAlreadyUp";

        Log.entering(c, METHOD_NAME);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        // ensure dependent feature manifest is not there when the server starts
        server.deleteFileFromLibertyInstallRoot("lib/features/featureWw-1.0.mf");
        server.startServer(METHOD_NAME + ".log");

        // Now move the server xml with the first set of features available (and update mark)
        TestUtils.makeConfigUpdateSetMark(server, "server_add_featureV.xml");

        String output = server.waitForStringInLogUsingMark(notFoundFeatureMsgPrefix);
        assertNotNull("We haven't found the " + notFoundFeatureMsgPrefix + " in the logs.", output);
        assertTrue("featureW-1.0 was installed and should not have been: " + output, output.contains("featureW-1.0"));

        // Get the install feature message for the initial set up of updated features
        output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("featureV-1.0 was not installed and should have been: " + output, output.contains("featureV-1.0"));
        assertFalse("featureW-1.0 was installed and should not have been: " + output, output.contains("featureW-1.0"));

        // Now check that we have the expected bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("bundlev/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundlev/[1.0.0,2.0.0)"));
        assertFalse("bundlew/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundlew/[1.0.0,2.0.0)"));

        // Finally check that removing the feature will also uninstall the dependant features. (update mark)
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("featureV-1.0 was not uninstalled and should have been: " + output, output.contains("featureV-1.0"));
        assertFalse("featureW-1.0 was uninstalled and should not have been: " + output, output.contains("featureW-1.0"));

        // Now check that bundles of the features are gone.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("bundlev/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundlev/[1.0.0,2.0.0)"));
        assertFalse("bundlew/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundlew/[1.0.0,2.0.0)"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a .mf file added to lib/features after the server is
     * up and running will be found and installed when the feature is specified in server.xml.
     * The test ensures that this happens during server update.
     *
     * The feature structure is as follows:
     *
     * featureA is defined in featureA.mf by IBM-ShortName: featureA-1.0
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testFeatureManifestFileAddedAfterServerIsAlreadyUp() throws Exception {
        final String METHOD_NAME = "testFeatureManifestFileAddedAfterServerIsAlreadyUp";

        Log.entering(c, METHOD_NAME);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.deleteFileFromLibertyInstallRoot("lib/features/featureA-1.0.mf");
        server.startServer(METHOD_NAME + ".log");

        // copy mf file and bundle jar
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, "featureA-1.0.mf");
        server.copyFileToLibertyInstallRoot("lib", "bundle1_1.0.0.jar");

        // Now move the server xml with featureA
        TestUtils.makeConfigUpdateSetMark(server, "server_add_featureA.xml");
        // Get the install feature message for the initial set up of updated features
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("featureA-1.0 was not installed and should have been: " + output, output.contains("featureA-1.0"));

        // Now check that we have the expected bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("bundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));

        // Finally check that removing the feature will also uninstall the feature.
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("featureA-1.0 was not uninstalled and should have been: " + output, output.contains("featureA-1.0"));

        // Now check that bundles of the features are gone.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("bundle1/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that feature defined in it's .mf file as mixed case
     * will be found and installed when the feature is specified in server.xml
     * as FEATUREA-1.0. 850 was not case specific. 855 needs to also
     * not be case specific.
     *
     * The feature structure is as follows:
     *
     * featureA is defined in featureA.mf by IBM-ShortName: featureA-1.0
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testFeatureNameInServerXmlAllUpperCaseManifestFileMixedCase() throws Exception {
        final String METHOD_NAME = "testFeatureNameInServerXmlAllUpperCaseManifestFileMixedCase";

        Log.entering(c, METHOD_NAME);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.deleteFileFromLibertyInstallRoot("lib/features/featureA-1.0.mf");
        // copy mf file and bundle jar
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, "featureA-1.0.mf");
        server.copyFileToLibertyInstallRoot("lib", "bundle1_1.0.0.jar");
        server.startServer(METHOD_NAME + ".log");

        // Now move the server xml with FEATUREA-1.0
        // 850 was not case specific. 855 needs to be that way as well.
        TestUtils.makeConfigUpdateSetMark(server, "server_add_feature_all_upper_a.xml");

        // Get the install feature message for the initial set up of updated features
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("featureA-1.0 was not installed and should have been: " + output, output.contains("featureA-1.0"));

        // Now check that we have the expected bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("bundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));

        // Finally check that removing the feature will also uninstall the feature.
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("featureA-1.0 was not uninstalled and should have been: " + output, output.contains("featureA-1.0"));

        // Now check that bundles of the features are gone.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("bundle1/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that feature defined in it's .mf file as mixed case
     * will be found and installed when the feature is specified in server.xml
     * as featurea-1.0. 850 was not case specific. 855 needs to also
     * not be case specific.
     *
     * The feature structure is as follows:
     *
     * featureA is defined in featureA.mf by IBM-ShortName: featureA-1.0
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testFeatureNameInServerXmlAllLowerCaseManifestFileMixedCase() throws Exception {
        final String METHOD_NAME = "testFeatureNameInServerXmlAllLowerCaseManifestFileMixedCase";

        Log.entering(c, METHOD_NAME);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.deleteFileFromLibertyInstallRoot("lib/features/featureA-1.0.mf");
        // copy mf file and bundle jar
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, "featureA-1.0.mf");
        server.copyFileToLibertyInstallRoot("lib", "bundle1_1.0.0.jar");
        server.startServer(METHOD_NAME + ".log");

        // Now move the server xml with featurea-1.0
        // 850 was not case specific. 855 needs to be that way as well.
        TestUtils.makeConfigUpdateSetMark(server, "server_add_feature_all_lower_a.xml");
        // Get the install feature message for the initial set up of updated features
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("featureA-1.0 was not installed and should have been: " + output, output.contains("featureA-1.0"));

        // Now check that we have the expected bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("bundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));

        // Finally check that removing the feature will also uninstall the feature.
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("featureA-1.0 was not uninstalled and should have been: " + output, output.contains("featureA-1.0"));

        // Now check that bundles of the features are gone.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("bundle1/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that the CWWKF0012I message is present in initial provisioning
     *
     * The feature structure is as follows:
     *
     * featureX, featureY, featureZ are all normal features.
     *
     * featureX is defined in featureXx.mf by IBM-ShortName: featureX-1.0
     * featureY is defined in featureYy.mf by IBM-ShortName: featureY-1.0
     * featureZ is defined in featureZz.mf by IBM-ShortName: featureZ-1.0
     *
     * featureX is a feature that has featureY listed under Subsystem-Content.
     * featureY is a feature that has featureZ listed under Subsystem-Content.
     * featureZ is a feature that has no other features listed under Subsystem-Content.
     *
     * @throws Exception
     */
    @Test
    public void testInitialFeaturesAddedMessage() throws Exception {
        final String METHOD_NAME = "testInitialFeaturesAddedMessage";

        Log.entering(c, METHOD_NAME);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_add_featureX.xml");
        server.startServer(METHOD_NAME + ".log");

        // Get the install feature message for the initial set up of updated features
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("featureX-1.0 was not installed and should have been: " + output, output.contains("featureX-1.0"));
        assertTrue("featureY-1.0 was not installed and should have been: " + output, output.contains("featureY-1.0"));
        assertTrue("featureZ-1.0 was not installed and should have been: " + output, output.contains("featureZ-1.0"));

        // now test with warm start
        server.stopServer();

        server.startServer(METHOD_NAME + "-2.log");
        output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);
        assertTrue("featureX-1.0 was not installed and should have been: " + output, output.contains("featureX-1.0"));
        assertTrue("featureY-1.0 was not installed and should have been: " + output, output.contains("featureY-1.0"));
        assertTrue("featureZ-1.0 was not installed and should have been: " + output, output.contains("featureZ-1.0"));

        Log.exiting(c, METHOD_NAME);
    }
}
