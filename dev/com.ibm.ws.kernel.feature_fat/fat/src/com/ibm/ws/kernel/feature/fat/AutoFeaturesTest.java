/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2012
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.kernel.feature.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

@Mode(TestMode.FULL)
public class AutoFeaturesTest {

    private static final Class<?> c = AutoFeaturesTest.class;
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.autofeature");
    private static final Logger logger = Logger.getLogger(AutoFeaturesTest.class.getName());
    private static final String FEATURE_PROVISIONER_CONTEXT_ROOT = "/feature/provisioner";

    private static final String USR_FEATURE_A_MF = "ufeatureA-1.0.mf";
    private static final String USR_FEATURE_B_MF = "ufeatureB-1.0.mf";
    private static final String USR_FEATURE_C_MF = "ufeatureC-1.0.mf";
    private static final String USR_FEATURE_D_MF = "ufeatureD-1.0.mf";

    private static final String USR_FEATURE_H_MF = "ufeatureH-1.0.mf";
    private static final String USR_FEATURE_I_MF = "ufeatureI-1.0.mf";

    private static final String USR_AUTO_FEATURE_E_MF = "ufeatureE-1.0.mf";
    private static final String USR_AUTO_FEATURE_F_MF = "ufeatureF-1.0.mf";
    private static final String USR_AUTO_FEATURE_G_MF = "ufeatureG-1.0.mf";
    private static final String USR_AUTO_FEATURE_J_MF = "ufeatureJ-1.0.mf";
    private static final String USR_AUTO_FEATURE_L_MF = "ufeatureL-1.0.mf";
    private static final String USR_AUTO_FEATURE_M_MF = "ufeatureM-1.0.mf";
    private static final String USR_AUTO_FEATURE_N_MF = "ufeatureN-1.0.mf";

    private static final String[] userFeatures = { USR_FEATURE_A_MF, USR_FEATURE_B_MF, USR_FEATURE_C_MF, USR_FEATURE_D_MF,
                                                  USR_FEATURE_H_MF, USR_FEATURE_I_MF, USR_AUTO_FEATURE_E_MF,
                                                  USR_AUTO_FEATURE_F_MF, USR_AUTO_FEATURE_G_MF, USR_AUTO_FEATURE_J_MF,
                                                  USR_AUTO_FEATURE_L_MF, USR_AUTO_FEATURE_M_MF, USR_AUTO_FEATURE_N_MF };
    private static final String USR_FEATURE_PATH = "usr/extension/lib/features/";
    private static final String USR_BUNDLE_PATH = "usr/extension/lib/";

    private static final String PRODUCT_FEATURE_A_MF = "pfeatureA-1.0.mf";
    private static final String PRODUCT_FEATURE_B_MF = "pfeatureB-1.0.mf";
    private static final String PRODUCT_FEATURE_C_MF = "pfeatureC-1.0.mf";
    private static final String PRODUCT_FEATURE_D_MF = "pfeatureD-1.0.mf";

    private static final String PRODUCT_FEATURE_H_MF = "pfeatureH-1.0.mf";
    private static final String PRODUCT_FEATURE_I_MF = "pfeatureI-1.0.mf";

    private static final String PRODUCT_AUTO_FEATURE_E_MF = "pfeatureE-1.0.mf";
    private static final String PRODUCT_AUTO_FEATURE_F_MF = "pfeatureF-1.0.mf";
    private static final String PRODUCT_AUTO_FEATURE_G_MF = "pfeatureG-1.0.mf";
    private static final String PRODUCT_AUTO_FEATURE_J_MF = "pfeatureJ-1.0.mf";
    private static final String PRODUCT_AUTO_FEATURE_L_MF = "pfeatureL-1.0.mf";
    private static final String PRODUCT_AUTO_FEATURE_M_MF = "pfeatureM-1.0.mf";
    private static final String PRODUCT_AUTO_FEATURE_N_MF = "pfeatureN-1.0.mf";

    private static final String[] productFeatures = { PRODUCT_FEATURE_A_MF, PRODUCT_FEATURE_B_MF, PRODUCT_FEATURE_C_MF, PRODUCT_FEATURE_D_MF,
                                                     PRODUCT_FEATURE_H_MF, PRODUCT_FEATURE_I_MF, PRODUCT_AUTO_FEATURE_E_MF,
                                                     PRODUCT_AUTO_FEATURE_F_MF, PRODUCT_AUTO_FEATURE_G_MF, PRODUCT_AUTO_FEATURE_J_MF,
                                                     PRODUCT_AUTO_FEATURE_L_MF, PRODUCT_AUTO_FEATURE_M_MF, PRODUCT_AUTO_FEATURE_N_MF };
    private static final String PRODUCT_EXTENSIONS_PATH = "etc/extensions/";
    private static final String PRODUCT_PATH = "productAuto";
    private static final String PRODUCT_FEATURE_PATH = PRODUCT_PATH + "/lib/features/";
    private static final String PRODUCT_BUNDLE_PATH = PRODUCT_PATH + "/lib/";
    private static final String PRODUCT_FEATURE_PROPERTIES_FILE = "productAuto.properties";

    private static final String PRODUCT2_PATH = "product2Auto";
    private static final String PRODUCT2_FEATURE_PATH = PRODUCT2_PATH + "/lib/features/";
    private static final String PRODUCT2_BUNDLE_PATH = PRODUCT2_PATH + "/lib/";
    private static final String PRODUCT2_FEATURE_PROPERTIES_FILE = "product2Auto.properties";

    private static final String PRODUCT2_FEATURE_A_MF = "p2featureA-1.0.mf";
    private static final String PRODUCT2_FEATURE_B_MF = "p2featureB-1.0.mf";
    private static final String PRODUCT2_FEATURE_D_MF = "p2featureD-1.0.mf";

    private static final String PRODUCT2_AUTO_FEATURE_E_MF = "p2featureE-1.0.mf";
    private static final String PRODUCT2_AUTO_FEATURE_G_MF = "p2featureG-1.0.mf";
    private static final String[] product2Features = { PRODUCT2_FEATURE_A_MF, PRODUCT2_FEATURE_B_MF, PRODUCT2_FEATURE_D_MF,
                                                      PRODUCT2_AUTO_FEATURE_E_MF, PRODUCT2_AUTO_FEATURE_G_MF };

    private static final String FEATURE_A_MF = "featureA-1.0.mf";
    private static final String FEATURE_B_MF = "featureB-1.0.mf";
    private static final String FEATURE_C_MF = "featureC-1.0.mf";
    private static final String FEATURE_D_MF = "featureD-1.0.mf";

    private static final String FEATURE_H_MF = "featureH-1.0.mf";
    private static final String FEATURE_I_MF = "featureI-1.0.mf";

    private static final String AUTO_FEATURE_E_MF = "featureE-1.0.mf";
    private static final String AUTO_FEATURE_F_MF = "featureF-1.0.mf";
    private static final String AUTO_FEATURE_G_MF = "featureG-1.0.mf";

    private static final String AUTO_FEATURE_J_MF = "featureJ-1.0.mf";

    private static final String AUTO_FEATURE_INVALID_HEADER_MF = "invalid_header_feature.mf";

    private static final String FEATURE_X_MF = "featureXx-1.0.mf";
    private static final String FEATURE_Y_MF = "featureYy-1.0.mf";
    private static final String FEATURE_Z_MF = "featureZz-1.0.mf";

    private static final String[] features = { FEATURE_A_MF, FEATURE_B_MF, FEATURE_C_MF, FEATURE_D_MF, FEATURE_H_MF, FEATURE_I_MF,
                                              AUTO_FEATURE_E_MF, AUTO_FEATURE_F_MF, AUTO_FEATURE_G_MF, AUTO_FEATURE_J_MF, FEATURE_X_MF, FEATURE_Y_MF, FEATURE_Z_MF };

    private static final String installFeatureMsgPrefix = "CWWKF0012I: The server installed the following features: \\[";
    private static final String uninstallFeatureMsgPrefix = "CWWKF0013I: The server removed the following features: \\[";
    private static final String invalidHeaderMsgPrefix = "CWWKF0016I: The filter .*? in the invalid_header_feature feature manifest header is incorrect:";

    // the number of server cycles to perform before counting any
    private static final int PRESTART_IGNORES = 5;
    private static final int SAMPLE_SIZE = 25;

    private static final String FEATURE_PATH = "lib/features/";
    private static final String TEST_FEATURE = "skeletonTestFeature";
    private static final String TEST_FEATURE_VERSION = "-1.0.mf";
    private static final String TEST_MF = TEST_FEATURE + TEST_FEATURE_VERSION;

    private static final String CACHE_DIRECTORY = "workarea/platform/";
    private static final String FEATURE_CACHE = CACHE_DIRECTORY + "feature.cache";
    private static final String FEATURE_BUNDLE_CACHE = CACHE_DIRECTORY + "feature.bundles.cache";

    /**
     * Copy the necessary features and bundles to the liberty server directories
     * 
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @BeforeClass
    public static void setup() throws Exception {
        final String METHOD_NAME = "classSetUp";

        Log.entering(c, METHOD_NAME);

        for (String feature : features) {
            Log.info(c, METHOD_NAME, "Copying " + feature + " to " + FEATURE_PATH + ".");
            server.copyFileToLibertyInstallRoot(FEATURE_PATH, feature);
        }

        for (int i = 1; i < 8; i++) {
            Log.info(c, METHOD_NAME, "Copying bundle" + i + "_1.0.0.jar to lib.");
            server.copyFileToLibertyInstallRoot("lib", "bundle" + i + "_1.0.0.jar");
            Log.info(c, METHOD_NAME, "Copying ubundle" + i + "_1.0.0.jar to " + USR_BUNDLE_PATH);
            server.copyFileToLibertyInstallRoot(USR_BUNDLE_PATH, "ubundle" + i + "_1.0.0.jar");
            Log.info(c, METHOD_NAME, "Copying pbundle" + i + "_1.0.0.jar to " + PRODUCT_BUNDLE_PATH);
            server.copyFileToLibertyInstallRoot(PRODUCT_BUNDLE_PATH, "pbundle" + i + "_1.0.0.jar");
            Log.info(c, METHOD_NAME, "Copying pbundle" + i + "_1.0.0.jar to " + PRODUCT2_BUNDLE_PATH);
            server.copyFileToLibertyInstallRoot(PRODUCT2_BUNDLE_PATH, "p2bundle" + i + "_1.0.0.jar");
        }

        Log.info(c, METHOD_NAME, "Copying bundlex_1.0.0.jar to lib.");
        server.copyFileToLibertyInstallRoot("lib", "bundlex_1.0.0.jar");
        Log.info(c, METHOD_NAME, "Copying bundley_1.0.0.jar to lib.");
        server.copyFileToLibertyInstallRoot("lib", "bundley_1.0.0.jar");
        Log.info(c, METHOD_NAME, "Copying bundlez_1.0.0.jar to lib.");
        server.copyFileToLibertyInstallRoot("lib", "bundlez_1.0.0.jar");

        for (String feature : userFeatures) {
            Log.info(c, METHOD_NAME, "Copying " + feature + " to " + USR_FEATURE_PATH + ".");
            server.copyFileToLibertyInstallRoot(USR_FEATURE_PATH, feature);
        }

        for (String feature : productFeatures) {
            Log.info(c, METHOD_NAME, "Copying " + feature + " to " + PRODUCT_FEATURE_PATH + ".");
            server.copyFileToLibertyInstallRoot(PRODUCT_FEATURE_PATH, feature);
        }

        for (String feature : product2Features) {
            Log.info(c, METHOD_NAME, "Copying " + feature + " to " + PRODUCT2_FEATURE_PATH + ".");
            server.copyFileToLibertyInstallRoot(PRODUCT2_FEATURE_PATH, feature);
        }

        server.copyFileToLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT_FEATURE_PROPERTIES_FILE);
        server.copyFileToLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT2_FEATURE_PROPERTIES_FILE);

        deleteSkeletons();

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * This method removes all the testing artifacts from the server directories.
     * 
     * @throws Exception
     */
    @Mode(TestMode.LITE)
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

        // Clean up the invalid header. Leaving this in the features dir will cause tests to fail.
        server.deleteFileFromLibertyInstallRoot(FEATURE_PATH + AUTO_FEATURE_INVALID_HEADER_MF);

        for (int i = 1; i < 8; i++) {
            server.deleteFileFromLibertyInstallRoot("lib/bundle" + i + "_1.0.0.jar");
            server.deleteFileFromLibertyInstallRoot(USR_BUNDLE_PATH + "ubundle" + i + "_1.0.0.jar");
        }
        server.deleteFileFromLibertyInstallRoot("lib/bundlex_1.0.0.jar");
        server.deleteFileFromLibertyInstallRoot("lib/bundley_1.0.0.jar");
        server.deleteFileFromLibertyInstallRoot("lib/bundlez_1.0.0.jar");

        // Remove the refreshFeatures servlet.
        server.deleteFileFromLibertyInstallRoot("lib/test.feature.provisioner_1.0.0.jar");
        server.deleteFileFromLibertyInstallRoot(FEATURE_PATH + "test.featureprovisioner_1.0.mf");

        for (String feature : userFeatures) {
            Log.info(c, METHOD_NAME, USR_FEATURE_PATH + feature + " will be deleted.");
            server.deleteFileFromLibertyInstallRoot(USR_FEATURE_PATH + feature);
        }

        server.deleteDirectoryFromLibertyInstallRoot(PRODUCT_PATH);
        server.deleteFileFromLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_PROPERTIES_FILE);
        server.deleteDirectoryFromLibertyInstallRoot(PRODUCT2_PATH);
        server.deleteFileFromLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + PRODUCT2_FEATURE_PROPERTIES_FILE);

        deleteSkeletons();

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * This method removes the skeleton features that are created as part of the performance test.
     * 
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    private static void deleteSkeletons() throws Exception {
        if (server.fileExistsInLibertyInstallRoot(FEATURE_PATH)) {
            for (String oldAutoFeature : server.listLibertyInstallRoot(FEATURE_PATH, TEST_FEATURE)) {
                server.deleteFileFromLibertyInstallRoot(FEATURE_PATH + oldAutoFeature);
            }
        }
    }

    @Rule
    public TestName testName = new TestName();

    @Mode(TestMode.LITE)
    @After
    public void stopServer() throws Exception {
        if (server.isStarted())
            server.stopServer();
    }

    /**
     * TestDescription:
     * This test ensures that auto features are automatically installed when their capability features are installed. The test ensures
     * that this happens successfully during server start.
     * 
     * The feature structure is as follows:
     * 
     * featureA, featureB, featureC and featureD are all normal features.
     * 
     * featureE is an auto feature that depends on features A and B.
     * featureF is an auto feature that depends on features B and C.
     * featureG is an auto feature that depends on auto feature E and normal feature D.
     * 
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAutoFeaturesInstallDuringServerStartup() throws Exception {
        Log.entering(c, testName.getMethodName());

        // Now move the server xml with the all features available
        server.setServerConfigurationFile("server_all_features.xml");
        server.startServer(testName.getMethodName() + ".log");

        // When the features are installed during a server start we don't get messages issued to the logs about which features are 
        // installed, so we need to look in the feature.cache file to ensure the correct features have been installed.

        String installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        assertNotNull("There is no installed features property in the feature.cache file",
                      installedFeatures);

        assertTrue("featureA-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureA-1.0"));
        assertTrue("featureB-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureB-1.0"));
        assertTrue("featureC-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureC-1.0"));
        assertTrue("featureD-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureD-1.0"));
        assertTrue("featureE-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureE-1.0"));
        assertTrue("featureF-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureF-1.0"));
        assertTrue("featureG-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureG-1.0"));

        // Now check that we have the expected bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("bundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));
        assertTrue("bundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle2/[1.0.0,2.0.0)"));
        assertTrue("bundle3/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle3/[1.0.0,2.0.0)"));
        assertTrue("bundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle4/[1.0.0,2.0.0)"));
        assertTrue("bundle5/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle5/[1.0.0,2.0.0)"));
        assertTrue("bundle6/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle6/[1.0.0,2.0.0)"));
        assertTrue("bundle7/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle7/[1.0.0,2.0.0)"));

        Log.exiting(c, testName.getMethodName());
    }

    /**
     * TestDescription:
     * This test ensures that auto features are automatically installed when their capability features are installed. The test
     * ensures that this happens during server update.
     * 
     * The feature structure is as follows:
     * 
     * featureA, featureB, featureC and featureD are all normal features.
     * 
     * featureE is an auto feature that depends on features A and B.
     * featureF is an auto feature that depends on features B and C.
     * featureG is an auto feature that depends on auto feature E and normal feature D.
     * 
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAutoFeaturesInstallDuringServerUpdate() throws Exception {
        Log.entering(c, testName.getMethodName());

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(testName.getMethodName() + ".log");

        // Now move the server xml with the first set of features available
        TestUtils.makeConfigUpdateSetMark(server, "server_initial_features.xml");
        // Get the install feature message for the initial set up of updated features
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("featureA-1.0 was not installed and should have been: " + output, output.contains("featureA-1.0"));
        assertTrue("featureB-1.0 was not installed and should have been: " + output, output.contains("featureB-1.0"));
        assertTrue("featureD-1.0 was not installed and should have been: " + output, output.contains("featureD-1.0"));
        assertTrue("featureE-1.0 was not installed and should have been: " + output, output.contains("featureE-1.0"));
        assertTrue("featureG-1.0 was not installed and should have been: " + output, output.contains("featureG-1.0"));

        // Ensure that an auto feature that doesn't have a type of osgi.subsystem.feature isn't provisioned.
        assertFalse("invalidSubsystemTypeFeature-1.0 was installed and should not have been: " + output, output.contains("invalidSubsystemTypeFeature-1.0"));

        // Now check that we have only the expected bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("bundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));
        assertTrue("bundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle2/[1.0.0,2.0.0)"));
        assertFalse("bundle3/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle3/[1.0.0,2.0.0)"));
        assertTrue("bundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle4/[1.0.0,2.0.0)"));
        assertTrue("bundle5/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle5/[1.0.0,2.0.0)"));
        assertFalse("bundle6/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle6/[1.0.0,2.0.0)"));
        assertTrue("bundle7/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle7/[1.0.0,2.0.0)"));

        // Now add a new feature which should trigger an auto install as well.
        TestUtils.makeConfigUpdateSetMark(server, "server_add_featureC.xml");
        output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix + ".*featureC.*");
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " for feature C and F in the logs.", output);

        assertTrue("featureC-1.0 was not installed and should have been: " + output, output.contains("featureC-1.0"));
        assertTrue("featureF-1.0 was not installed and should have been: " + output, output.contains("featureF-1.0"));

        // Now check that bundles of the features have also been installed.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("bundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));
        assertTrue("bundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle2/[1.0.0,2.0.0)"));
        assertTrue("bundle3/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle3/[1.0.0,2.0.0)"));
        assertTrue("bundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle4/[1.0.0,2.0.0)"));
        assertTrue("bundle5/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle5/[1.0.0,2.0.0)"));
        assertTrue("bundle6/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle6/[1.0.0,2.0.0)"));
        assertTrue("bundle7/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle7/[1.0.0,2.0.0)"));

        // Finally check that removing a feature will also uninstall some auto features.
        TestUtils.makeConfigUpdateSetMark(server, "server_remove_featureA.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("featureA-1.0 was not uninstalled and should have been: " + output, output.contains("featureA-1.0"));
        assertTrue("featureE-1.0 was not uninstalled and should have been: " + output, output.contains("featureE-1.0"));
        assertTrue("featureG-1.0 was not uninstalled and should have been: " + output, output.contains("featureG-1.0"));

        // Now check that bundles of the features we've installed have been added.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("bundle1/[1.0.0,2.0.0) was installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));
        assertTrue("bundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle2/[1.0.0,2.0.0)"));
        assertTrue("bundle3/[1.0.0,2.0.0) was not installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle3/[1.0.0,2.0.0)"));
        assertTrue("bundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle4/[1.0.0,2.0.0)"));
        assertFalse("bundle5/[1.0.0,2.0.0) was installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle5/[1.0.0,2.0.0)"));
        assertTrue("bundle6/[1.0.0,2.0.0) was not installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle6/[1.0.0,2.0.0)"));
        assertFalse("bundle7/[1.0.0,2.0.0) was installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle7/[1.0.0,2.0.0)"));

        Log.exiting(c, testName.getMethodName());
    }

    /**
     * TestDescription:
     * This test ensures that you are able to use "OR" filters, so that an auto feature will be auto provisioned if one feature or another
     * feature, or both features are configured. The test uses the feature update method to check things have been provisioned correctly.
     * 
     * The feature structure is as follows:
     * 
     * featureH and featureI are all normal features.
     * 
     * featureJ is an auto feature that depends on feature H or feature I.
     * 
     * @throws Exception
     */
    @Test
    public void testCapabilityHeaderRequiringOneFeatureOrAnotherFeature() throws Exception {
        Log.entering(c, testName.getMethodName());

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(testName.getMethodName() + ".log");

        // Now move the server xml with the first features available
        TestUtils.makeConfigUpdateSetMark(server, "server_add_featureH.xml");
        // Get the install feature message for the initial set up of updated features
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("featureH-1.0 was not installed and should have been: " + output, output.contains("featureH-1.0"));
        assertTrue("featureJ-1.0 was not installed and should have been: " + output, output.contains("featureJ-1.0"));

        // Now check that we have the correct bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("bundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));
        assertFalse("bundle2/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle2/[1.0.0,2.0.0)"));
        assertTrue("bundle3/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle3/[1.0.0,2.0.0)"));

        // Now move the server xml with the second feature available
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");

        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);
        assertTrue("featureH-1.0 was not uninstalled and should have been: " + output, output.contains("featureH-1.0"));
        assertTrue("featureJ-1.0 was not uninstalled and should have been: " + output, output.contains("featureJ-1.0"));

        // Now check that we have uninstalled the bundles.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("bundle1/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));
        assertFalse("bundle2/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle2/[1.0.0,2.0.0)"));
        assertFalse("bundle3/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle3/[1.0.0,2.0.0)"));

        server.stopServer();
        server.startServer(testName.getMethodName() + "-2.log"); // clean start

        TestUtils.makeConfigUpdateSetMark(server, "server_add_featureI.xml");
        // Get the install feature message for the initial set up of updated features
        output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + "in the logs.", output);

        assertTrue("featureI-1.0 was not installed and should have been: " + output, output.contains("featureI-1.0"));
        assertTrue("featureJ-1.0 was not installed and should have been: " + output, output.contains("featureJ-1.0"));

        // Now check that we have the correct bundles installed.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("bundle1/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));
        assertTrue("bundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle2/[1.0.0,2.0.0)"));
        assertTrue("bundle3/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle3/[1.0.0,2.0.0)"));

        // Now move the server xml with the second feature available
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");

        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);
        assertTrue("featureI-1.0 was not uninstalled and should have been: " + output, output.contains("featureI-1.0"));
        assertTrue("featureJ-1.0 was not uninstalled and should have been: " + output, output.contains("featureJ-1.0"));

        // Now check that we have uninstalled the bundles.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("bundle1/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));
        assertFalse("bundle2/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle2/[1.0.0,2.0.0)"));
        assertFalse("bundle3/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle3/[1.0.0,2.0.0)"));

        server.stopServer();
        server.startServer(testName.getMethodName() + "-3.log"); // clean start

        TestUtils.makeConfigUpdateSetMark(server, "server_add_featureH_and_featureI.xml");
        // Get the install feature message for the initial set up of updated features
        output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + "in the logs.", output);

        assertTrue("featureH-1.0 was not installed and should have been: " + output, output.contains("featureH-1.0"));
        assertTrue("featureI-1.0 was not installed and should have been: " + output, output.contains("featureI-1.0"));
        assertTrue("featureJ-1.0 was not installed and should have been: " + output, output.contains("featureJ-1.0"));

        // Now check that we have installed all of the bundles.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("bundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));
        assertTrue("bundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle2/[1.0.0,2.0.0)"));
        assertTrue("bundle3/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle3/[1.0.0,2.0.0)"));

        //Now clear down the server and try just configuring the other feature.
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("featureH-1.0 was not uninstalled and should have been: " + output, output.contains("featureH-1.0"));
        assertTrue("featureI-1.0 was not uninstalled and should have been: " + output, output.contains("featureI-1.0"));
        assertTrue("featureJ-1.0 was not uninstalled and should have been: " + output, output.contains("featureJ-1.0"));

        // Now check that we have the uninstalled the bundles.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("bundle1/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));
        assertFalse("bundle2/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle2/[1.0.0,2.0.0)"));
        assertFalse("bundle3/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle3/[1.0.0,2.0.0)"));

        Log.exiting(c, testName.getMethodName());
    }

    /**
     * TestDescription:
     * This test ensures that auto features are added to the feature directory between a server stop and start, will get provisioned
     * if they are satisfied, even if the configured features aren't changed between server starts.
     * 
     * The feature structure is as follows:
     * 
     * featureA, featureB
     * 
     * featureE is an auto feature that depends on features A and B.
     * featureG is an auto feature that depends on auto feature E and normal feature D.
     * 
     * @throws Exception
     */
    @Test
    public void testNewAutoFeaturesInstallAfterRestart() throws Exception {
        Log.entering(c, testName.getMethodName());

        // Remove auto features, E, F and G
        server.deleteFileFromLibertyInstallRoot("lib/features/" + AUTO_FEATURE_E_MF);
        server.deleteFileFromLibertyInstallRoot("lib/features/" + AUTO_FEATURE_F_MF);
        server.deleteFileFromLibertyInstallRoot("lib/features/" + AUTO_FEATURE_G_MF);

        // Now move the server xml with the all features available
        server.setServerConfigurationFile("server_all_features.xml");
        server.startServer(testName.getMethodName() + ".log");

        // When the features are installed during a server start we don't get messages issued to the logs about which features are 
        // installed, so we need to look in the feature.cache file to ensure the correct features have been installed.
        String installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        assertNotNull("There is no installed features property in the feature.cache file",
                      installedFeatures);

        assertTrue("featureA-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureA-1.0"));
        assertTrue("featureB-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureB-1.0"));
        assertTrue("featureC-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureC-1.0"));
        assertTrue("featureD-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureD-1.0"));
        assertFalse("featureE-1.0 was installed and should not have been: " + installedFeatures, installedFeatures.contains("featureE-1.0"));
        assertFalse("featureF-1.0 was installed and should not have been: " + installedFeatures, installedFeatures.contains("featureF-1.0"));
        assertFalse("featureG-1.0 was installed and should not have been: " + installedFeatures, installedFeatures.contains("featureG-1.0"));

        // Now check that we have only the expected bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("bundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));
        assertTrue("bundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle2/[1.0.0,2.0.0)"));
        assertTrue("bundle3/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle3/[1.0.0,2.0.0)"));
        assertTrue("bundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle4/[1.0.0,2.0.0)"));
        assertFalse("bundle5/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle5/[1.0.0,2.0.0)"));
        assertFalse("bundle6/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle6/[1.0.0,2.0.0)"));
        assertFalse("bundle7/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle7/[1.0.0,2.0.0)"));

        if (server.isStarted())
            server.stopServer();

        // Now add auto features, E, F and G
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, AUTO_FEATURE_E_MF);
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, AUTO_FEATURE_F_MF);
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, AUTO_FEATURE_G_MF);
        // restart the server
        server.startServer(testName.getMethodName() + "-2.log"); // clean start

        // When the features are installed during a server start we don't get messages issued to the logs about which features are 
        // installed, so we need to look in the feature.cache file to ensure the correct features have been installed.
        installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        assertNotNull("There is no installed features property in the feature.cache file",
                      installedFeatures);

        // feature names are stored in the cache as lower case.
        assertTrue("featureA-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureA-1.0"));
        assertTrue("featureB-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureB-1.0"));
        assertTrue("featureC-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureC-1.0"));
        assertTrue("featureD-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureD-1.0"));
        assertTrue("featureE-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureE-1.0"));
        assertTrue("featureF-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureF-1.0"));
        assertTrue("featureG-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureG-1.0"));

        // Now check that bundles of the features are all installed.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("bundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));
        assertTrue("bundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle2/[1.0.0,2.0.0)"));
        assertTrue("bundle3/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle3/[1.0.0,2.0.0)"));
        assertTrue("bundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle4/[1.0.0,2.0.0)"));
        assertTrue("bundle5/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle5/[1.0.0,2.0.0)"));
        assertTrue("bundle6/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle6/[1.0.0,2.0.0)"));
        assertTrue("bundle7/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle7/[1.0.0,2.0.0)"));
        Log.exiting(c, testName.getMethodName());
    }

    /**
     * TestDescription:
     * This test ensures that auto user features are automatically installed when their capability features are installed.
     * The test ensures that this happens successfully during server start.
     * 
     * The feature structure is as follows:
     * 
     * ufeatureA, ufeatureB, ufeatureC and ufeatureD are all normal user features.
     * 
     * ufeatureE is an auto user feature that depends on normal user features A and B.
     * ufeatureF is an auto user feature that depends on normal user features B and C.
     * ufeatureG is an auto user feature that depends on auto user feature E and normal user feature D.
     * 
     * @throws Exception
     */
    @Test
    public void testUsrAutoFeaturesInstallDuringServerStartup() throws Exception {
        Log.entering(c, testName.getMethodName());

        // Now move the server xml with the all features available
        server.setServerConfigurationFile("server_usr_all_features.xml");
        server.startServer(testName.getMethodName() + ".log");

        // When the features are installed during a server start we don't get messages issued to the logs about which features are 
        // installed, so we need to look in the feature.cache file to ensure the correct features have been installed.

        String installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        assertNotNull("There is no installed features property in the feature.cache file",
                      installedFeatures);

        assertTrue("ufeatureA-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("ufeatureA-1.0"));
        assertTrue("ufeatureB-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("ufeatureB-1.0"));
        assertTrue("ufeatureC-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("ufeatureC-1.0"));
        assertTrue("ufeatureD-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("ufeatureD-1.0"));
        assertTrue("ufeatureE-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("ufeatureE-1.0"));
        assertTrue("ufeatureF-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("ufeatureF-1.0"));
        assertTrue("ufeatureG-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("ufeatureG-1.0"));

        // Now check that we have the expected bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("usr|ubundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle1/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle2/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle3/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle3/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle4/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle5/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle5/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle6/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle6/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle7/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle7/[1.0.0,2.0.0)"));

        Log.exiting(c, testName.getMethodName());
    }

    /**
     * TestDescription:
     * This test ensures that auto user features are automatically installed when their capability user features are installed.
     * The test ensures that this happens during server update.
     * 
     * The feature structure is as follows:
     * 
     * ufeatureA, ufeatureB, ufeatureC and ufeatureD are all normal user features.
     * 
     * ufeatureE is an auto user feature that depends on normal user features A and B.
     * ufeatureF is an auto user feature that depends on normal user features B and C.
     * ufeatureG is an auto user feature that depends on auto user feature E and normal user feature D.
     * 
     * @throws Exception
     */
    @Test
    public void testUsrAutoFeaturesInstallDuringServerUpdate() throws Exception {
        Log.entering(c, testName.getMethodName());

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(testName.getMethodName() + ".log");

        // Now move the server xml with the first set of features available
        TestUtils.makeConfigUpdateSetMark(server, "server_initial_usr_features.xml");
        // Get the install feature message for the initial set up of updated features
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("ufeatureA-1.0 was not installed and should have been: " + output, output.contains("ufeatureA-1.0"));
        assertTrue("ufeatureB-1.0 was not installed and should have been: " + output, output.contains("ufeatureB-1.0"));
        assertTrue("ufeatureD-1.0 was not installed and should have been: " + output, output.contains("ufeatureD-1.0"));
        assertTrue("ufeatureE-1.0 was not installed and should have been: " + output, output.contains("ufeatureE-1.0"));
        assertTrue("ufeatureG-1.0 was not installed and should have been: " + output, output.contains("ufeatureG-1.0"));

        // Ensure that an auto feature that doesn't have a type of osgi.subsystem.feature isn't provisioned.
        assertFalse("invalidSubsystemTypeFeature-1.0 was installed and should not have been: " + output, output.contains("invalidSubsystemTypeFeature-1.0"));

        // Now check that we have only the expected bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("usr|ubundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle1/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle2/[1.0.0,2.0.0)"));
        assertFalse("usr|ubundle3/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle3/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle4/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle5/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle5/[1.0.0,2.0.0)"));
        assertFalse("usr|ubundle6/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle6/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle7/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle7/[1.0.0,2.0.0)"));

        // Now add a new feature which should trigger an auto install as well.
        TestUtils.makeConfigUpdateSetMark(server, "server_add_usr_featureC.xml");
        output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix + ".*ufeatureC.*");
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " for user feature C and F in the logs.", output);

        assertTrue("ufeatureC-1.0 was not installed and should have been: " + output, output.contains("ufeatureC-1.0"));
        assertTrue("ufeatureF-1.0 was not installed and should have been: " + output, output.contains("ufeatureF-1.0"));

        // Now check that bundles of the features have also been installed.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("usr|ubundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle1/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle2/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle3/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle3/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle4/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle5/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle5/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle6/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle6/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle7/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle7/[1.0.0,2.0.0)"));

        // Finally check that removing a feature will also uninstall some auto features.
        TestUtils.makeConfigUpdateSetMark(server, "server_remove_usr_featureA.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("ufeatureA-1.0 was not uninstalled and should have been: " + output, output.contains("ufeatureA-1.0"));
        assertTrue("ufeatureE-1.0 was not uninstalled and should have been: " + output, output.contains("ufeatureE-1.0"));
        assertTrue("ufeatureG-1.0 was not uninstalled and should have been: " + output, output.contains("ufeatureG-1.0"));

        // Now check that bundles of the features we've installed have been added.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("usr|ubundle1/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle1/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle2/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle3/[1.0.0,2.0.0) was not installed and should not have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("usr|ubundle3/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle4/[1.0.0,2.0.0)"));
        assertFalse("usr|ubundle5/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle5/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle6/[1.0.0,2.0.0) was not installed and should not have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("usr|ubundle6/[1.0.0,2.0.0)"));
        assertFalse("usr|ubundle7/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle7/[1.0.0,2.0.0)"));

        Log.exiting(c, testName.getMethodName());
    }

    /**
     * TestDescription:
     * This test ensures that auto user features are automatically installed when their capability features are installed.
     * The capability features are a combination of user and non user features.
     * The test ensures that this happens during server update.
     * 
     * The feature structure is as follows:
     * 
     * featureA, featureB, featureC and featureD are all normal non user features.
     * 
     * ufeatureL is an auto user feature that depends on normal non user features A and B.
     * ufeatureM is an auto user feature that depends on normal non user features B and C.
     * ufeatureN is an auto user feature that depends on auto user feature L and normal non user feature D.
     * 
     * @throws Exception
     */
    @Test
    public void testUsrAutoFeaturesDependsUserAndNonUserFeatures() throws Exception {
        Log.entering(c, testName.getMethodName());

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(testName.getMethodName() + ".log");

        // Now move the server xml with the first set of features available
        TestUtils.makeConfigUpdateSetMark(server, "server_initial_features.xml");
        // Get the install feature message for the initial set up of updated features
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("featureA-1.0 was not installed and should have been: " + output, output.contains("featureA-1.0"));
        assertTrue("featureB-1.0 was not installed and should have been: " + output, output.contains("featureB-1.0"));
        assertTrue("featureD-1.0 was not installed and should have been: " + output, output.contains("featureD-1.0"));
        assertTrue("ufeatureL-1.0 was not installed and should have been: " + output, output.contains("ufeatureL-1.0"));
        assertTrue("ufeatureN-1.0 was not installed and should have been: " + output, output.contains("ufeatureN-1.0"));

        // Ensure that an auto feature that doesn't have a type of osgi.subsystem.feature isn't provisioned.
        assertFalse("invalidSubsystemTypeFeature-1.0 was installed and should not have been: " + output, output.contains("invalidSubsystemTypeFeature-1.0"));

        // Now check that we have only the expected bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("bundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));
        assertTrue("bundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle2/[1.0.0,2.0.0)"));
        assertFalse("bundle3/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle3/[1.0.0,2.0.0)"));
        assertTrue("bundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle4/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle5/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle5/[1.0.0,2.0.0)"));
        assertFalse("usr|ubundle6/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle6/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle7/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle7/[1.0.0,2.0.0)"));

        // Now add a new feature which should trigger an auto install as well.
        TestUtils.makeConfigUpdateSetMark(server, "server_add_featureC.xml");
        output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix + ".*featureC.*");
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " for feature C and user feature M in the logs.", output);

        assertTrue("featureC-1.0 was not installed and should have been: " + output, output.contains("featureC-1.0"));
        assertTrue("ufeatureM-1.0 was not installed and should have been: " + output, output.contains("ufeatureM-1.0"));

        // Now check that bundles of the features have also been installed.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("bundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));
        assertTrue("bundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle2/[1.0.0,2.0.0)"));
        assertTrue("bundle3/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle3/[1.0.0,2.0.0)"));
        assertTrue("bundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle4/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle5/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle5/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle6/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle6/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle7/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle7/[1.0.0,2.0.0)"));

        // Finally check that removing a feature will also uninstall some auto features.
        TestUtils.makeConfigUpdateSetMark(server, "server_remove_featureA.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("featureA-1.0 was not uninstalled and should have been: " + output, output.contains("featureA-1.0"));
        assertTrue("ufeatureL-1.0 was not uninstalled and should have been: " + output, output.contains("ufeatureL-1.0"));
        assertTrue("ufeatureN-1.0 was not uninstalled and should have been: " + output, output.contains("ufeatureN-1.0"));

        // Now check that bundles of the features we've installed have been added.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("bundle1/[1.0.0,2.0.0) was installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));
        assertTrue("bundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle2/[1.0.0,2.0.0)"));
        assertTrue("bundle3/[1.0.0,2.0.0) was not installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle3/[1.0.0,2.0.0)"));
        assertTrue("bundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle4/[1.0.0,2.0.0)"));
        assertFalse("usr|ubundle5/[1.0.0,2.0.0) was installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle5/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle6/[1.0.0,2.0.0) was not installed and should not have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("usr|ubundle6/[1.0.0,2.0.0)"));
        assertFalse("usr|ubundle7/[1.0.0,2.0.0) was installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle7/[1.0.0,2.0.0)"));

        Log.exiting(c, testName.getMethodName());
    }

    /**
     * TestDescription:
     * This test ensures that you are able to use "OR" filters, so that an auto user feature will be auto provisioned if one user feature or another
     * user feature, or both user features are configured. The test uses the feature update method to check things have been provisioned correctly.
     * 
     * The feature structure is as follows:
     * 
     * ufeatureH and ufeatureI are all normal user features.
     * 
     * ufeatureJ is an auto user feature that depends on user feature H or user feature I.
     * 
     * @throws Exception
     */
    @Test
    public void testUsrCapabilityHeaderRequiringOneFeatureOrAnotherFeature() throws Exception {
        Log.entering(c, testName.getMethodName());

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(testName.getMethodName() + ".log");

        // Now move the server xml with the first features available
        TestUtils.makeConfigUpdateSetMark(server, "server_add_usr_featureH.xml");
        // Get the install feature message for the initial set up of updated features
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("ufeatureH-1.0 was not installed and should have been: " + output, output.contains("ufeatureH-1.0"));
        assertTrue("ufeatureJ-1.0 was not installed and should have been: " + output, output.contains("ufeatureJ-1.0"));

        // Now check that we have the correct bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("usr|ubundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle1/[1.0.0,2.0.0)"));
        assertFalse("usr|ubundle2/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle2/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle3/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle3/[1.0.0,2.0.0)"));

        // Now move the server xml with the second feature available
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");

        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);
        assertTrue("ufeatureH-1.0 was not uninstalled and should have been: " + output, output.contains("ufeatureH-1.0"));
        assertTrue("ufeatureJ-1.0 was not uninstalled and should have been: " + output, output.contains("ufeatureJ-1.0"));

        // Now check that we have uninstalled the bundles.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("usr|ubundle1/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle1/[1.0.0,2.0.0)"));
        assertFalse("usr|ubundle2/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle2/[1.0.0,2.0.0)"));
        assertFalse("usr|ubundle3/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle3/[1.0.0,2.0.0)"));

        server.stopServer();
        server.startServer(testName.getMethodName() + "-2.log"); // clean start

        TestUtils.makeConfigUpdateSetMark(server, "server_add_usr_featureI.xml");
        // Get the install feature message for the initial set up of updated features
        output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + "in the logs.", output);

        assertTrue("ufeatureI-1.0 was not installed and should have been: " + output, output.contains("ufeatureI-1.0"));
        assertTrue("ufeatureJ-1.0 was not installed and should have been: " + output, output.contains("ufeatureJ-1.0"));

        // Now check that we have the correct bundles installed.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("usr|ubundle1/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle1/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle2/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle3/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle3/[1.0.0,2.0.0)"));

        // Now move the server xml with the second feature available
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");

        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);
        assertTrue("ufeatureI-1.0 was not uninstalled and should have been: " + output, output.contains("ufeatureI-1.0"));
        assertTrue("ufeatureJ-1.0 was not uninstalled and should have been: " + output, output.contains("ufeatureJ-1.0"));

        // Now check that we have uninstalled the bundles.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("usr|ubundle1/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle1/[1.0.0,2.0.0)"));
        assertFalse("usr|ubundle2/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle2/[1.0.0,2.0.0)"));
        assertFalse("usr|ubundle3/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle3/[1.0.0,2.0.0)"));

        server.stopServer();
        server.startServer(testName.getMethodName() + "-3.log"); // clean start

        TestUtils.makeConfigUpdateSetMark(server, "server_add_usr_featureH_and_featureI.xml");
        // Get the install feature message for the initial set up of updated features
        output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + "in the logs.", output);

        assertTrue("ufeatureH-1.0 was not installed and should have been: " + output, output.contains("ufeatureH-1.0"));
        assertTrue("ufeatureI-1.0 was not installed and should have been: " + output, output.contains("ufeatureI-1.0"));
        assertTrue("ufeatureJ-1.0 was not installed and should have been: " + output, output.contains("ufeatureJ-1.0"));

        // Now check that we have installed all of the bundles.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("usr|ubundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle1/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle2/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle3/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle3/[1.0.0,2.0.0)"));

        //Now clear down the server and try just configuring the other feature.
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("ufeatureH-1.0 was not uninstalled and should have been: " + output, output.contains("ufeatureH-1.0"));
        assertTrue("ufeatureI-1.0 was not uninstalled and should have been: " + output, output.contains("ufeatureI-1.0"));
        assertTrue("ufeatureJ-1.0 was not uninstalled and should have been: " + output, output.contains("ufeatureJ-1.0"));

        // Now check that we have the uninstalled the bundles.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("usr|ubundle1/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle1/[1.0.0,2.0.0)"));
        assertFalse("usr|ubundle2/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle2/[1.0.0,2.0.0)"));
        assertFalse("usr|ubundle3/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle3/[1.0.0,2.0.0)"));

        Log.exiting(c, testName.getMethodName());
    }

    /**
     * TestDescription:
     * This test ensures that auto user features are added to the feature directory between a server stop and start, will get provisioned
     * if they are satisfied, even if the configured features aren't changed between server starts.
     * 
     * The feature structure is as follows:
     * 
     * ufeatureA, ufeatureB are normal user features
     * 
     * ufeatureE is an auto user feature that depends on normal user features A and B.
     * ufeatureG is an auto user feature that depends on auto user feature E and normal user feature D.
     * 
     * @throws Exception
     */
    @Test
    public void testUsrNewAutoFeaturesInstallAfterRestart() throws Exception {
        Log.entering(c, testName.getMethodName());

        // Remove user auto features, E, F and G
        server.deleteFileFromLibertyInstallRoot(USR_FEATURE_PATH + USR_AUTO_FEATURE_E_MF);
        server.deleteFileFromLibertyInstallRoot(USR_FEATURE_PATH + USR_AUTO_FEATURE_F_MF);
        server.deleteFileFromLibertyInstallRoot(USR_FEATURE_PATH + USR_AUTO_FEATURE_G_MF);

        // Now move the server xml with the all features available
        server.setServerConfigurationFile("server_usr_all_features.xml");
        server.startServer(testName.getMethodName() + ".log");

        // When the features are installed during a server start we don't get messages issued to the logs about which features are 
        // installed, so we need to look in the feature.cache file to ensure the correct features have been installed.
        String installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        assertNotNull("There is no installed features property in the feature.cache file",
                      installedFeatures);

        // feature names are stored in the cache as lower case.
        assertTrue("ufeatureA-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("ufeatureA-1.0"));
        assertTrue("ufeatureB-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("ufeatureB-1.0"));
        assertTrue("ufeatureC-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("ufeatureC-1.0"));
        assertTrue("ufeatureD-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("ufeatureD-1.0"));
        assertFalse("ufeatureE-1.0 was installed and should not have been: " + installedFeatures, installedFeatures.contains("ufeatureE-1.0"));
        assertFalse("ufeatureF-1.0 was installed and should not have been: " + installedFeatures, installedFeatures.contains("ufeatureF-1.0"));
        assertFalse("ufeatureG-1.0 was installed and should not have been: " + installedFeatures, installedFeatures.contains("ufeatureG-1.0"));

        // Now check that we have only the expected bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("usr|ubundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle1/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle2/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle3/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle3/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle4/[1.0.0,2.0.0)"));
        assertFalse("usr|ubundle5/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle5/[1.0.0,2.0.0)"));
        assertFalse("usr|ubundle6/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle6/[1.0.0,2.0.0)"));
        assertFalse("usr|ubundle7/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle7/[1.0.0,2.0.0)"));

        if (server.isStarted())
            server.stopServer();

        // Now add user auto features, E, F and G
        server.copyFileToLibertyInstallRoot(USR_FEATURE_PATH, USR_AUTO_FEATURE_E_MF);
        server.copyFileToLibertyInstallRoot(USR_FEATURE_PATH, USR_AUTO_FEATURE_F_MF);
        server.copyFileToLibertyInstallRoot(USR_FEATURE_PATH, USR_AUTO_FEATURE_G_MF);
        // restart the server
        server.startServer(testName.getMethodName() + "-2.log"); // clean start

        // When the features are installed during a server start we don't get messages issued to the logs about which features are 
        // installed, so we need to look in the feature.cache file to ensure the correct features have been installed.
        installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        assertNotNull("There is no installed features property in the feature.cache file",
                      installedFeatures);

        assertTrue("ufeatureA-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("ufeatureA-1.0"));
        assertTrue("ufeatureB-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("ufeatureB-1.0"));
        assertTrue("ufeatureC-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("ufeatureC-1.0"));
        assertTrue("ufeatureD-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("ufeatureD-1.0"));
        assertTrue("ufeatureE-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("ufeatureE-1.0"));
        assertTrue("ufeatureF-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("ufeatureF-1.0"));
        assertTrue("ufeatureG-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("ufeatureG-1.0"));

        // Now check that bundles of the features are all installed.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("usr|ubundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle1/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle2/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle3/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle3/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle4/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle5/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle5/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle6/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle6/[1.0.0,2.0.0)"));
        assertTrue("usr|ubundle7/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("usr|ubundle7/[1.0.0,2.0.0)"));
        Log.exiting(c, testName.getMethodName());
    }

    /**
     * TestDescription:
     * This test ensures that auto product features are automatically installed when their capability features are installed.
     * The test ensures that this happens successfully during server start.
     * 
     * The feature structure is as follows:
     * 
     * pfeatureA, pfeatureB, pfeatureC and pfeatureD are all normal product features.
     * 
     * pfeatureE is an auto product feature that depends on normal product features A and B.
     * pfeatureF is an auto product feature that depends on normal product features B and C.
     * pfeatureG is an auto product feature that depends on auto product feature E and normal product feature D.
     * 
     * @throws Exception
     */
    @Test
    public void testProductAutoFeaturesInstallDuringServerStartup() throws Exception {
        Log.entering(c, testName.getMethodName());

        // Now move the server xml with the all features available
        server.setServerConfigurationFile("server_product_all_features.xml");
        server.startServer(testName.getMethodName() + ".log");

        // When the features are installed during a server start we don't get messages issued to the logs about which features are 
        // installed, so we need to look in the feature.cache file to ensure the correct features have been installed.

        String installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        assertNotNull("There is no installed features property in the feature.cache file",
                      installedFeatures);

        // feature names are stored in the cache as lower case.
        assertTrue("pfeatureA-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("pfeatureA-1.0"));
        assertTrue("pfeatureB-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("pfeatureB-1.0"));
        assertTrue("pfeatureC-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("pfeatureC-1.0"));
        assertTrue("pfeatureD-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("pfeatureD-1.0"));
        assertTrue("pfeatureE-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("pfeatureE-1.0"));
        assertTrue("pfeatureF-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("pfeatureF-1.0"));
        assertTrue("pfeatureG-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("pfeatureG-1.0"));

        // Now check that we have the expected bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("productAuto|pbundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle1/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle2/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle3/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle3/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle4/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle5/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle5/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle6/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle6/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle7/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle7/[1.0.0,2.0.0)"));

        Log.exiting(c, testName.getMethodName());
    }

    /**
     * TestDescription:
     * This test ensures that auto product features are automatically installed when their capability product features are installed.
     * The test ensures that this happens during server update.
     * 
     * The feature structure is as follows:
     * 
     * pfeatureA, pfeatureB, pfeatureC and pfeatureD are all normal product features.
     * 
     * pfeatureE is an auto product feature that depends on normal product features A and B.
     * pfeatureF is an auto product feature that depends on normal product features B and C.
     * pfeatureG is an auto product feature that depends on auto product feature E and normal product feature D.
     * 
     * @throws Exception
     */
    @Test
    public void testProductAutoFeaturesInstallDuringServerUpdate() throws Exception {
        Log.entering(c, testName.getMethodName());

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(testName.getMethodName() + ".log");

        // Now move the server xml with the first set of features available
        TestUtils.makeConfigUpdateSetMark(server, "server_initial_product_features.xml");
        // Get the install feature message for the initial set up of updated features
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("pfeatureA-1.0 was not installed and should have been: " + output, output.contains("pfeatureA-1.0"));
        assertTrue("pfeatureB-1.0 was not installed and should have been: " + output, output.contains("pfeatureB-1.0"));
        assertTrue("pfeatureD-1.0 was not installed and should have been: " + output, output.contains("pfeatureD-1.0"));
        assertTrue("pfeatureE-1.0 was not installed and should have been: " + output, output.contains("pfeatureE-1.0"));
        assertTrue("pfeatureG-1.0 was not installed and should have been: " + output, output.contains("pfeatureG-1.0"));

        // Ensure that an auto feature that doesn't have a type of osgi.subsystem.feature isn't provisioned.
        assertFalse("invalidSubsystemTypeFeature-1.0 was installed and should not have been: " + output, output.contains("invalidSubsystemTypeFeature-1.0"));

        // Now check that we have only the expected bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("productAuto|pbundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle1/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle2/[1.0.0,2.0.0)"));
        assertFalse("productAuto|pbundle3/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle3/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle4/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle5/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle5/[1.0.0,2.0.0)"));
        assertFalse("productAuto|pbundle6/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle6/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle7/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle7/[1.0.0,2.0.0)"));

        // Now add a new feature which should trigger an auto install as well.
        TestUtils.makeConfigUpdateSetMark(server, "server_add_product_featureC.xml");
        output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix + ".*pfeatureC.*");
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " for product feature C and F in the logs.", output);

        assertTrue("pfeatureC-1.0 was not installed and should have been: " + output, output.contains("pfeatureC-1.0"));
        assertTrue("pfeatureF-1.0 was not installed and should have been: " + output, output.contains("pfeatureF-1.0"));

        // Now check that bundles of the features have also been installed.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("productAuto|pbundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle1/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle2/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle3/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle3/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle4/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle5/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle5/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle6/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle6/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle7/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle7/[1.0.0,2.0.0)"));

        // Finally check that removing a feature will also uninstall some auto features.
        TestUtils.makeConfigUpdateSetMark(server, "server_remove_product_featureA.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("pfeatureA-1.0 was not uninstalled and should have been: " + output, output.contains("pfeatureA-1.0"));
        assertTrue("pfeatureE-1.0 was not uninstalled and should have been: " + output, output.contains("pfeatureE-1.0"));
        assertTrue("pfeatureG-1.0 was not uninstalled and should have been: " + output, output.contains("pfeatureG-1.0"));

        // Now check that bundles of the features we've installed have been added.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("productAuto|pbundle1/[1.0.0,2.0.0) was installed and should have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle1/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle2/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle3/[1.0.0,2.0.0) was not installed and should not have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle3/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle4/[1.0.0,2.0.0)"));
        assertFalse("productAuto|pbundle5/[1.0.0,2.0.0) was installed and should have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle5/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle6/[1.0.0,2.0.0) was not installed and should not have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle6/[1.0.0,2.0.0)"));
        assertFalse("productAuto|pbundle7/[1.0.0,2.0.0) was installed and should have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle7/[1.0.0,2.0.0)"));

        Log.exiting(c, testName.getMethodName());
    }

    /**
     * TestDescription:
     * This test ensures that when multiple product features are configured, the
     * auto product features are automatically installed when their capability product features are installed.
     * The test ensures that this happens during server update.
     * 
     * The feature structure is as follows:
     * 
     * pfeatureA, pfeatureB, and pfeatureD are all normal product features for product productAuto.
     * p2featureA p2featureB and p2featureD are all normal product features for product product2Auto.
     * 
     * pfeatureE is an auto product feature that depends on normal product features pfeatureA and pfeatureB.
     * pfeatureG is an auto product feature that depends on auto product feature pfeatureE and normal product feature pfeatureD.
     * p2featureE is an auto product feature that depends on normal product features p2featureA and p2featureB.
     * p2featureG is an auto product feature that depends on auto product feature p2featureE and normal product feature p2featureD.
     * 
     * @throws Exception
     */
    @Test
    public void testMultipleProductsWithAutoFeaturesInstallDuringServerUpdate() throws Exception {
        Log.entering(c, testName.getMethodName());

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(testName.getMethodName() + ".log");

        // Now move the server xml with the first set of features available
        TestUtils.makeConfigUpdateSetMark(server, "server_initial_product2_features.xml");
        // Get the install feature message for the initial set up of updated features
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("pfeatureA-1.0 was not installed and should have been: " + output, output.contains("pfeatureA-1.0"));
        assertTrue("pfeatureB-1.0 was not installed and should have been: " + output, output.contains("pfeatureB-1.0"));
        assertTrue("pfeatureD-1.0 was not installed and should have been: " + output, output.contains("pfeatureD-1.0"));
        assertTrue("pfeatureE-1.0 was not installed and should have been: " + output, output.contains("pfeatureE-1.0"));
        assertTrue("pfeatureG-1.0 was not installed and should have been: " + output, output.contains("pfeatureG-1.0"));

        assertTrue("p2featureA-1.0 was not installed and should have been: " + output, output.contains("p2featureA-1.0"));
        assertTrue("p2featureB-1.0 was not installed and should have been: " + output, output.contains("p2featureB-1.0"));
        assertTrue("p2featureD-1.0 was not installed and should have been: " + output, output.contains("p2featureD-1.0"));
        assertTrue("p2featureE-1.0 was not installed and should have been: " + output, output.contains("p2featureE-1.0"));
        assertTrue("p2featureG-1.0 was not installed and should have been: " + output, output.contains("p2featureG-1.0"));

        // Ensure that an auto feature that doesn't have a type of osgi.subsystem.feature isn't provisioned.
        assertFalse("invalidSubsystemTypeFeature-1.0 was installed and should not have been: " + output, output.contains("invalidSubsystemTypeFeature-1.0"));

        // Now check that we have only the expected bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("productAuto|pbundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle1/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle2/[1.0.0,2.0.0)"));
        assertFalse("productAuto|pbundle3/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle3/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle4/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle5/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle5/[1.0.0,2.0.0)"));
        assertFalse("productAuto|pbundle6/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle6/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle7/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle7/[1.0.0,2.0.0)"));

        assertTrue("product2Auto|p2bundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("product2Auto|p2bundle1/[1.0.0,2.0.0)"));
        assertTrue("product2Auto|p2bundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("product2Auto|p2bundle2/[1.0.0,2.0.0)"));
        assertFalse("product2Auto|p2bundle3/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("product2Auto|p2bundle3/[1.0.0,2.0.0)"));
        assertTrue("product2Auto|p2bundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("product2Auto|p2bundle4/[1.0.0,2.0.0)"));
        assertTrue("product2Auto|p2bundle5/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("product2Auto|p2bundle5/[1.0.0,2.0.0)"));
        assertFalse("product2Auto|p2bundle6/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("product2Auto|p2bundle6/[1.0.0,2.0.0)"));
        assertTrue("product2Auto|p2bundle7/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("product2Auto|p2bundle7/[1.0.0,2.0.0)"));

        // Finally check that removing a feature will also uninstall some auto features.
        // Removing D should cause G to get unstalled as well
        TestUtils.makeConfigUpdateSetMark(server, "server_remove_product2_featureD.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("p2featureD-1.0 was not uninstalled and should have been: " + output, output.contains("p2featureD-1.0"));
        assertTrue("p2featureG-1.0 was not uninstalled and should have been: " + output, output.contains("p2featureG-1.0"));

        // Now check that bundles of the features we've installed have been added.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("productAuto|pbundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle1/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle2/[1.0.0,2.0.0)"));
        assertFalse("productAuto|pbundle3/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle3/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle4/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle5/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle5/[1.0.0,2.0.0)"));
        assertFalse("productAuto|pbundle6/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle6/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle7/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle7/[1.0.0,2.0.0)"));

        assertTrue("product2Auto|p2bundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("product2Auto|p2bundle1/[1.0.0,2.0.0)"));
        assertTrue("product2Auto|p2bundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("product2Auto|p2bundle2/[1.0.0,2.0.0)"));
        assertFalse("product2Auto|p2bundle3/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("product2Auto|p2bundle3/[1.0.0,2.0.0)"));
        assertFalse("product2Auto|p2bundle4/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("product2Auto|p2bundle4/[1.0.0,2.0.0)"));
        assertTrue("product2Auto|p2bundle5/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("product2Auto|p2bundle5/[1.0.0,2.0.0)"));
        assertFalse("product2Auto|p2bundle6/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("product2Auto|p2bundle6/[1.0.0,2.0.0)"));
        assertFalse("product2Auto|p2bundle7/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("product2Auto|p2bundle7/[1.0.0,2.0.0)"));

        Log.exiting(c, testName.getMethodName());
    }

    /**
     * TestDescription:
     * This test ensures that auto product features are automatically installed when their capability features are installed.
     * The capability features are a combination of product and non product features.
     * The test ensures that this happens during server update.
     * 
     * The feature structure is as follows:
     * 
     * featureA, featureB, featureC and featureD are all normal non product features.
     * 
     * pfeatureL is an auto product feature that depends on normal non product features A and B.
     * pfeatureM is an auto product feature that depends on normal non product features B and C.
     * pfeatureN is an auto product feature that depends on auto product feature L and normal non product feature D.
     * 
     * @throws Exception
     */
    @Test
    public void testProductAutoFeaturesDependsUserAndNonUserFeatures() throws Exception {
        Log.entering(c, testName.getMethodName());

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(testName.getMethodName() + ".log");

        // Now move the server xml with the first set of features available
        TestUtils.makeConfigUpdateSetMark(server, "server_initial_features.xml");
        // Get the install feature message for the initial set up of updated features
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("featureA-1.0 was not installed and should have been: " + output, output.contains("featureA-1.0"));
        assertTrue("featureB-1.0 was not installed and should have been: " + output, output.contains("featureB-1.0"));
        assertTrue("featureD-1.0 was not installed and should have been: " + output, output.contains("featureD-1.0"));
        assertTrue("pfeatureL-1.0 was not installed and should have been: " + output, output.contains("pfeatureL-1.0"));
        assertTrue("pfeatureN-1.0 was not installed and should have been: " + output, output.contains("pfeatureN-1.0"));

        // Ensure that an auto feature that doesn't have a type of osgi.subsystem.feature isn't provisioned.
        assertFalse("invalidSubsystemTypeFeature-1.0 was installed and should not have been: " + output, output.contains("invalidSubsystemTypeFeature-1.0"));

        // Now check that we have only the expected bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("bundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));
        assertTrue("bundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle2/[1.0.0,2.0.0)"));
        assertFalse("bundle3/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle3/[1.0.0,2.0.0)"));
        assertTrue("bundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle4/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle5/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle5/[1.0.0,2.0.0)"));
        assertFalse("productAuto|pbundle6/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle6/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle7/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle7/[1.0.0,2.0.0)"));

        // Now add a new feature which should trigger an auto install as well.
        TestUtils.makeConfigUpdateSetMark(server, "server_add_featureC.xml");
        output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix + ".*featureC.*");
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " for feature C and product feature M in the logs.", output);

        assertTrue("featureC-1.0 was not installed and should have been: " + output, output.contains("featureC-1.0"));
        assertTrue("pfeatureM-1.0 was not installed and should have been: " + output, output.contains("pfeatureM-1.0"));

        // Now check that bundles of the features have also been installed.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("bundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));
        assertTrue("bundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle2/[1.0.0,2.0.0)"));
        assertTrue("bundle3/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle3/[1.0.0,2.0.0)"));
        assertTrue("bundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle4/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle5/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle5/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle6/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle6/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle7/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle7/[1.0.0,2.0.0)"));

        // Finally check that removing a feature will also uninstall some auto features.
        TestUtils.makeConfigUpdateSetMark(server, "server_remove_featureA.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("featureA-1.0 was not uninstalled and should have been: " + output, output.contains("featureA-1.0"));
        assertTrue("pfeatureL-1.0 was not uninstalled and should have been: " + output, output.contains("pfeatureL-1.0"));
        assertTrue("pfeatureN-1.0 was not uninstalled and should have been: " + output, output.contains("pfeatureN-1.0"));

        // Now check that bundles of the features we've installed have been added.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("bundle1/[1.0.0,2.0.0) was installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle1/[1.0.0,2.0.0)"));
        assertTrue("bundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle2/[1.0.0,2.0.0)"));
        assertTrue("bundle3/[1.0.0,2.0.0) was not installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle3/[1.0.0,2.0.0)"));
        assertTrue("bundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle4/[1.0.0,2.0.0)"));
        assertFalse("productAuto|pbundle5/[1.0.0,2.0.0) was installed and should have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle5/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle6/[1.0.0,2.0.0) was not installed and should not have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle6/[1.0.0,2.0.0)"));
        assertFalse("productAuto|pbundle7/[1.0.0,2.0.0) was installed and should have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle7/[1.0.0,2.0.0)"));

        Log.exiting(c, testName.getMethodName());
    }

    /**
     * TestDescription:
     * This test ensures that you are able to use "OR" filters, so that an auto product feature will be auto provisioned if one product feature or another
     * product feature, or both product features are configured. The test uses the feature update method to check things have been provisioned correctly.
     * 
     * The feature structure is as follows:
     * 
     * pfeatureH and pfeatureI are all normal product features.
     * 
     * pfeatureJ is an auto product feature that depends on product feature H or product feature I.
     * 
     * @throws Exception
     */
    @Test
    public void testProductCapabilityHeaderRequiringOneFeatureOrAnotherFeature() throws Exception {
        Log.entering(c, testName.getMethodName());

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(testName.getMethodName() + ".log");

        // Now move the server xml with the first features available
        TestUtils.makeConfigUpdateSetMark(server, "server_add_product_featureH.xml");
        // Get the install feature message for the initial set up of updated features
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("pfeatureH-1.0 was not installed and should have been: " + output, output.contains("pfeatureH-1.0"));
        assertTrue("pfeatureJ-1.0 was not installed and should have been: " + output, output.contains("pfeatureJ-1.0"));

        // Now check that we have the correct bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("pundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle1/[1.0.0,2.0.0)"));
        assertFalse("productAuto|pbundle2/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle2/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle3/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle3/[1.0.0,2.0.0)"));

        // Now move the server xml with the second feature available
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");

        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);
        assertTrue("pfeatureH-1.0 was not uninstalled and should have been: " + output, output.contains("pfeatureH-1.0"));
        assertTrue("pfeatureJ-1.0 was not uninstalled and should have been: " + output, output.contains("pfeatureJ-1.0"));

        // Now check that we have uninstalled the bundles.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("productAuto|pbundle1/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle1/[1.0.0,2.0.0)"));
        assertFalse("productAuto|pbundle2/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle2/[1.0.0,2.0.0)"));
        assertFalse("productAuto|pbundle3/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle3/[1.0.0,2.0.0)"));

        server.stopServer();
        // This is still a clean start
        server.startServer(testName.getMethodName() + "-2.log");

        TestUtils.makeConfigUpdateSetMark(server, "server_add_product_featureI.xml");
        // Get the install feature message for the initial set up of updated features
        output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + "in the logs.", output);

        assertTrue("pfeatureI-1.0 was not installed and should have been: " + output, output.contains("pfeatureI-1.0"));
        assertTrue("pfeatureJ-1.0 was not installed and should have been: " + output, output.contains("pfeatureJ-1.0"));

        // Now check that we have the correct bundles installed.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("productAuto|pbundle1/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle1/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle2/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle3/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle3/[1.0.0,2.0.0)"));

        // Now move the server xml with the second feature available
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");

        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);
        assertTrue("pfeatureI-1.0 was not uninstalled and should have been: " + output, output.contains("pfeatureI-1.0"));
        assertTrue("pfeatureJ-1.0 was not uninstalled and should have been: " + output, output.contains("pfeatureJ-1.0"));

        // Now check that we have uninstalled the bundles.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("productAuto|pbundle1/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle1/[1.0.0,2.0.0)"));
        assertFalse("productAuto|pbundle2/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle2/[1.0.0,2.0.0)"));
        assertFalse("productAuto|pbundle3/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle3/[1.0.0,2.0.0)"));

        server.stopServer();
        // This is still a clean start
        server.startServer(testName.getMethodName() + "-3.log");

        TestUtils.makeConfigUpdateSetMark(server, "server_add_product_featureH_and_featureI.xml");
        // Get the install feature message for the initial set up of updated features
        output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + "in the logs.", output);

        assertTrue("pfeatureH-1.0 was not installed and should have been: " + output, output.contains("pfeatureH-1.0"));
        assertTrue("pfeatureI-1.0 was not installed and should have been: " + output, output.contains("pfeatureI-1.0"));
        assertTrue("pfeatureJ-1.0 was not installed and should have been: " + output, output.contains("pfeatureJ-1.0"));

        // Now check that we have installed all of the bundles.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("productAuto|pbundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle1/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle2/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle3/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle3/[1.0.0,2.0.0)"));

        //Now clear down the server and try just configuring the other feature.
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("pfeatureH-1.0 was not uninstalled and should have been: " + output, output.contains("pfeatureH-1.0"));
        assertTrue("pfeatureI-1.0 was not uninstalled and should have been: " + output, output.contains("pfeatureI-1.0"));
        assertTrue("pfeatureJ-1.0 was not uninstalled and should have been: " + output, output.contains("pfeatureJ-1.0"));

        // Now check that we have the uninstalled the bundles.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertFalse("productAuto|pbundle1/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle1/[1.0.0,2.0.0)"));
        assertFalse("productAuto|pbundle2/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle2/[1.0.0,2.0.0)"));
        assertFalse("productAuto|pbundle3/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle3/[1.0.0,2.0.0)"));

        Log.exiting(c, testName.getMethodName());
    }

    /**
     * TestDescription:
     * This test ensures that auto product features are added to the feature directory between a server stop and start, will get provisioned
     * if they are satisfied, even if the configured features aren't changed between server starts.
     * 
     * The feature structure is as follows:
     * 
     * pfeatureA, pfeatureB are normal product features
     * 
     * pfeatureE is an auto product feature that depends on normal product features A and B.
     * pfeatureG is an auto product feature that depends on auto product feature E and normal product feature D.
     * 
     * @throws Exception
     */
    @Test
    public void testProductNewAutoFeaturesInstallAfterRestart() throws Exception {
        Log.entering(c, testName.getMethodName());

        // Remove product auto features, E, F and G
        server.deleteFileFromLibertyInstallRoot(PRODUCT_FEATURE_PATH + PRODUCT_AUTO_FEATURE_E_MF);
        server.deleteFileFromLibertyInstallRoot(PRODUCT_FEATURE_PATH + PRODUCT_AUTO_FEATURE_F_MF);
        server.deleteFileFromLibertyInstallRoot(PRODUCT_FEATURE_PATH + PRODUCT_AUTO_FEATURE_G_MF);

        // Now move the server xml with the all features available
        server.setServerConfigurationFile("server_product_all_features.xml");
        server.startServer(testName.getMethodName() + ".log");

        // When the features are installed during a server start we don't get messages issued to the logs about which features are 
        // installed, so we need to look in the feature.cache file to ensure the correct features have been installed.
        String installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        assertNotNull("There is no installed features property in the feature.cache file",
                      installedFeatures);

        // feature names are stored in the cache as lower case.
        assertTrue("pfeatureA-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("pfeatureA-1.0"));
        assertTrue("pfeatureB-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("pfeatureB-1.0"));
        assertTrue("pfeatureC-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("pfeatureC-1.0"));
        assertTrue("pfeatureD-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("pfeatureD-1.0"));
        assertFalse("pfeatureE-1.0 was installed and should not have been: " + installedFeatures, installedFeatures.contains("pfeatureE-1.0"));
        assertFalse("pfeatureF-1.0 was installed and should not have been: " + installedFeatures, installedFeatures.contains("pfeatureF-1.0"));
        assertFalse("pfeatureG-1.0 was installed and should not have been: " + installedFeatures, installedFeatures.contains("pfeatureG-1.0"));

        // Now check that we have only the expected bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("productAuto|pbundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle1/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle2/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle3/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle3/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle4/[1.0.0,2.0.0)"));
        assertFalse("productAuto|pbundle5/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle5/[1.0.0,2.0.0)"));
        assertFalse("productAuto|pbundle6/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle6/[1.0.0,2.0.0)"));
        assertFalse("productAuto|pbundle7/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("productAuto|pbundle7/[1.0.0,2.0.0)"));

        if (server.isStarted())
            server.stopServer();

        // Now add product auto features, E, F and G
        server.copyFileToLibertyInstallRoot(PRODUCT_FEATURE_PATH, PRODUCT_AUTO_FEATURE_E_MF);
        server.copyFileToLibertyInstallRoot(PRODUCT_FEATURE_PATH, PRODUCT_AUTO_FEATURE_F_MF);
        server.copyFileToLibertyInstallRoot(PRODUCT_FEATURE_PATH, PRODUCT_AUTO_FEATURE_G_MF);
        // restart the server
        server.startServer(testName.getMethodName() + "-2.log"); // clean start

        // When the features are installed during a server start we don't get messages issued to the logs about which features are 
        // installed, so we need to look in the feature.cache file to ensure the correct features have been installed.
        installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        assertNotNull("There is no installed features property in the feature.cache file",
                      installedFeatures);

        // feature names are stored in the cache as lower case.
        assertTrue("pfeatureA-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("pfeatureA-1.0"));
        assertTrue("pfeatureB-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("pfeatureB-1.0"));
        assertTrue("pfeatureC-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("pfeatureC-1.0"));
        assertTrue("pfeatureD-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("pfeatureD-1.0"));
        assertTrue("pfeatureE-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("pfeatureE-1.0"));
        assertTrue("pfeatureF-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("pfeatureF-1.0"));
        assertTrue("pfeatureG-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("pfeatureG-1.0"));

        // Now check that bundles of the features are all installed.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);

        assertTrue("productAuto|pbundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle1/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle2/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle2/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle3/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle3/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle4/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle4/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle5/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle5/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle6/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle6/[1.0.0,2.0.0)"));
        assertTrue("productAuto|pbundle7/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("productAuto|pbundle7/[1.0.0,2.0.0)"));
        Log.exiting(c, testName.getMethodName());
    }

    /**
     * TestDescription:
     * This test ensures that an invalid filter in the capability header throws the expected exception.
     * 
     * @throws Exception
     */

    @Test
    @ExpectedFFDC({ "org.osgi.framework.InvalidSyntaxException" })
    public void testInvalidFilterInCapabilityHeader() throws Exception {
        Log.entering(c, testName.getMethodName());

        // Copy the invalid feature header to the server features dir and set the server xml to the invalid header feature xml file.
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, AUTO_FEATURE_INVALID_HEADER_MF);
        server.setServerConfigurationFile("server_invalid_header.xml");
        server.startServer(testName.getMethodName() + ".log");

        String output = server.waitForStringInLog(invalidHeaderMsgPrefix);

        server.deleteFileFromLibertyInstallRoot(FEATURE_PATH + AUTO_FEATURE_INVALID_HEADER_MF);

        assertNotNull("We haven't found the " + invalidHeaderMsgPrefix + " in the logs.", output);

        Log.exiting(c, testName.getMethodName());
    }

    @Test
    @Ignore
    public void testAutoFeatureScanTime() throws Exception {
        Log.entering(c, testName.getMethodName());

        //work out the initial performance
        double[] initialStats = performStatsSeries(testName.getMethodName());

        //now add some autoFeatures (say 10)
        int someAutoFeatures = 10;
        createAutoFeatures(someAutoFeatures);
        //get the stats for 10 features
        double[] someStats = performStatsSeries(testName.getMethodName() + "-some");

        //now add a ridiculous number of autoFeatures (say 500)
        int ridiculousAutoFeatures = 500;
        createAutoFeatures(ridiculousAutoFeatures);
        //get the stats for 500 features
        double[] ridiculousStats = performStatsSeries(testName.getMethodName() + "-lots");

        logger.info("Statistics (no auto feature files): mean= " + initialStats[0] + " sample standard deviation= " + initialStats[1]);
        logger.info("Statistics (" + someAutoFeatures + " auto feature files): mean= " + someStats[0] + " sample standard deviation= " + someStats[1]);
        logger.info("Statistics (" + ridiculousAutoFeatures + " auto feature files): mean= " + ridiculousStats[0] + " sample standard deviation= " + ridiculousStats[1]);

        double change1 = performanceDegradation(initialStats, someStats);
        assertTrue("Performance was more than 1% worse (" + change1 + "% worse) when reading " + someAutoFeatures + " auto feature files", change1 <= 1d);

        double change2 = performanceDegradation(initialStats, ridiculousStats);
        assertTrue("Performance was more than 1% worse (" + change2 + "% worse) when reading " + ridiculousAutoFeatures + " auto feature files", change2 <= 1d);

        Log.exiting(c, testName.getMethodName());
    }

    /**
     * TestDescription:
     * This test ensures that auto product features that are added to the feature directory after a server start, will get provisioned
     * if they are satisfied, even if the configured features aren't changed between server starts.
     * 
     * The feature structure is as follows:
     * 
     * featureA, featureB, featureC and feature D are normal features
     * 
     * featureE is an auto feature that depends on normal features A and B.
     * featureG is an auto feature that depends on auto feature E and normal feature D.
     * 
     * @throws Exception
     */

    @Test
    public void testAutoFeatureInstallFromFeatureManagerRefresh() throws Exception {
        Log.entering(c, testName.getMethodName());

        //Ensure autofeatures A, B, C and D are in the runtime.
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, FEATURE_A_MF);
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, FEATURE_B_MF);
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, FEATURE_C_MF);
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, FEATURE_D_MF);
        // Remove auto features, E, F and G
        server.deleteFileFromLibertyInstallRoot(FEATURE_PATH + AUTO_FEATURE_E_MF);
        server.deleteFileFromLibertyInstallRoot(FEATURE_PATH + AUTO_FEATURE_F_MF);
        server.deleteFileFromLibertyInstallRoot(FEATURE_PATH + AUTO_FEATURE_G_MF);

        // Copy the test feature  bundle, and the test featureProvisioner manifest,
        // which allows us to use a servlet to refresh the autofeatures.
        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getInstallRoot() + "/lib", "publish/bundles/test.feature.provisioner_1.0.0.jar");
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, "test.featureprovisioner-1.0.mf");

        // Now move the server xml with the all features available
        server.setServerConfigurationFile("server_refresh_autofeatures.xml");
        server.startServer(testName.getMethodName() + ".log");

        // Wait for the Feature Refresh servlet to start.
        String message = server.waitForStringInLog("CWWKT0016I.*http://.*/feature");

        // When the features are installed during a server start we don't get messages issued to the logs about which features are 
        // installed, so we need to look in the feature.cache file to ensure the correct features have been installed.

        String installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        assertNotNull("There is no installed features property in the feature.cache file",
                      installedFeatures);

        assertTrue("featureA-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureA-1.0"));
        assertTrue("featureB-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureB-1.0"));
        assertTrue("featureC-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureC-1.0"));
        assertTrue("featureD-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureD-1.0"));
        assertFalse("featureE-1.0 was installed and should not have been: " + installedFeatures, installedFeatures.contains("featureE-1.0"));
        assertFalse("featureF-1.0 was installed and should not have been: " + installedFeatures, installedFeatures.contains("featureF-1.0"));
        assertFalse("featureG-1.0 was installed and should not have been: " + installedFeatures, installedFeatures.contains("featureG-1.0"));

        // Now check that we have the expected bundles installed.
        Properties bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);
        assertFalse("bundle5/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle5/[1.0.0,2.0.0)"));
        assertFalse("bundle7/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle7/[1.0.0,2.0.0)"));

        // Add product auto feature E to the feature lib dir.
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, AUTO_FEATURE_E_MF);

        // Trigger the refresh of the auto features, and ensure we get the message saying feature E has been provisioned.
        HttpUtils.findStringInUrl(server, FEATURE_PROVISIONER_CONTEXT_ROOT, "FeatureProvisioner: features refreshed.");

        // Because the refreshFeatures method is asynchronous, we need to wait for the completion message in the console logs.
        server.setMarkToEndOfLog();
        server.waitForStringInLogUsingMark("CWWKF0008I");

        installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);

        assertTrue("featureE-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureE-1.0"));

        // Now check that we have the expected bundles installed.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);
        assertTrue("bundle5/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle5/[1.0.0,2.0.0)"));

        // Add product auto feature G to the feature lib dir.
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, AUTO_FEATURE_G_MF);

        // Trigger the refresh of the auto features, and ensure we get the message saying feature G has been provisioned.
        HttpUtils.findStringInUrl(server, FEATURE_PROVISIONER_CONTEXT_ROOT, "FeatureProvisioner: features refreshed.");
        // Because the refreshFeatures method is asynchronous, we need to wait for the completion message in the console logs.
        server.setMarkToEndOfLog();
        server.waitForStringInLogUsingMark("CWWKF0008I");

        installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        assertTrue("featureG-1.0 was not installed and should have been: " + installedFeatures, installedFeatures.contains("featureG-1.0"));

        // Now check that we have the expected bundles installed.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);
        assertTrue("bundle7/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle7/[1.0.0,2.0.0)"));

        // Remove Feature E and G manifests from the lib features dir, and refresh the auto features, and ensure that the 
        // both feature E and G have been removed. You have to Feature G as well as E, otherwise we get an exception because G
        // is currently provisioned and the system recognises that it can't 
        server.deleteFileFromLibertyInstallRoot(FEATURE_PATH + AUTO_FEATURE_E_MF);

        HttpUtils.findStringInUrl(server, FEATURE_PROVISIONER_CONTEXT_ROOT, "FeatureProvisioner: features refreshed.");
        // Because the refreshFeatures method is asynchronous, we need to wait for the completion message in the console logs.
        server.setMarkToEndOfLog();
        server.waitForStringInLogUsingMark("CWWKF0008I");

        installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        assertFalse("featureE-1.0 was installed and should not have been: " + installedFeatures, installedFeatures.contains("featureE-1.0"));
        assertFalse("featureG-1.0 was installed and should not have been: " + installedFeatures, installedFeatures.contains("featureG-1.0"));

        // Now check that we have the expected bundles installed.
        bundleCacheProperties = TestUtils.getCacheProperties(server, FEATURE_BUNDLE_CACHE);
        assertFalse("bundle5/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle5/[1.0.0,2.0.0)"));
        assertFalse("bundle7/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties, bundleCacheProperties.containsKey("bundle7/[1.0.0,2.0.0)"));

        // Add product auto feature E and F back to the feature lib dir for future tests.
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, AUTO_FEATURE_E_MF);
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, AUTO_FEATURE_F_MF);

        Log.exiting(c, testName.getMethodName());
    }

    /**
     * Work out if the performance of stats2 was worse than stats1
     * 
     * @param stats1
     * @param stats2
     */
    private double performanceDegradation(double[] stats1, double[] stats2) {
        double mean1 = stats1[0];
        double stddev1 = stats1[1];
        double mean2 = stats2[0];
        double stddev2 = stats2[1];

        double meanDifference = mean2 - mean1;

        if (meanDifference > 0 && meanDifference > (stddev1 + stddev2)) {
            //the performance was worse by more than one standard deviation overlap
            return (meanDifference / mean1) * 100;
        } else {
            return 0d;
        }
    }

    private double[] performStatsSeries(String filePrefix) throws Exception {
        //work out the initial performance
        List<Long> results = serverCycles(SAMPLE_SIZE, filePrefix);
        double[] stats = calculateStats(results);

        return stats;
    }

    private double[] calculateStats(List<Long> results) {
        Long sum = 0l;
        Integer count = results.size();
        for (Long result : results) {
            sum += result;
        }
        logger.info("Sum= " + sum);
        double mean = sum.floatValue() / count.floatValue();

        //std deviation (use the n-1 sample standard deviation)
        double sumOfSquares = 0d;
        for (Long result : results) {
            sumOfSquares += Math.pow(result.floatValue() - mean, 2);
        }
        double stdDev = Math.sqrt(sumOfSquares / (count.floatValue() - 1f));
        return new double[] { mean, stdDev };
    }

    private List<Long> serverCycles(int count, String filePrefix) throws Exception {
        List<Long> results = new ArrayList<Long>(count);
        for (int i = 1; i <= PRESTART_IGNORES; i++) {
            serverCycle(filePrefix + "-prestart", i);
        }
        for (int i = 1; i <= count; i++) {
            results.add(serverCycle(filePrefix, i));
        }
        return results;
    }

    private long serverCycle(String filePrefix, int i) throws Exception {
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        server.startServer(filePrefix + "-" + i + ".log");
        long duration = getStartDurartion();
        logger.log(Level.INFO, "Start duration was: " + getStartDurartion() + "ms");
        server.stopServer();
        return duration;
    }

    private long getStartDurartion() throws Exception {
        String launchingMessage = server.waitForStringInLogUsingMark("CWWKE0001I");
        String launchedMessage = server.waitForStringInLogUsingMark("CWWKF0011I");
        long startTime = extractTimeFromMessage(launchingMessage);
        long endTime = extractTimeFromMessage(launchedMessage);
        //calculate the duration of start
        return endTime - startTime;
    }

    static final Pattern tsPattern = Pattern.compile("^\\[(.*)\\].*");
    static final DateFormat tsFormat = DateFormatProvider.getDateFormat();

    //new SimpleDateFormat("dd/MM/yy HH:MM:ss:SSS zzz");

    private long extractTimeFromMessage(String message) throws Exception {
        Matcher m = tsPattern.matcher(message);
        if (m.matches() && m.groupCount() > 0) {
            logger.log(Level.INFO, m.group(1));
            return tsFormat.parse(m.group(1)).getTime();
        } else {
            return 0;
        }
    }

    private void createAutoFeatures(int numberToCreate) throws Exception {
        server.copyFileToLibertyInstallRoot(FEATURE_PATH, "skeletonTestFeature-1.0.mf");
        RemoteFile templateFile = server.getFileFromLibertyInstallRoot(FEATURE_PATH + TEST_MF);
        RemoteFile autoFeatureRoot = server.getFileFromLibertyInstallRoot(FEATURE_PATH);
        for (int i = 1; i < numberToCreate; i++) {
            RemoteFile generatedAutoFile = new RemoteFile(autoFeatureRoot, TEST_FEATURE + i + TEST_FEATURE_VERSION);
            templateFile.copyToDest(generatedAutoFile);
        }
    }

    /*
     * This is really frustrating, but we can't use a Liberty call here, but we need to use the same
     * date and time format as the logs in any particular locale, so have a nested copy of the
     * DateFormatProvider, hopefully it won't get out of sync with the logging code very easily
     * or to an extent that impacts its use in this test (which is to calculate time differences
     * between log messages)
     */

    public static class DateFormatProvider {

        /**
         * Return a format string that will produce a reasonable standard way for
         * formatting time (but still using the current locale)
         * 
         * @return The format string
         */
        public static DateFormat getDateFormat() {
            String pattern;
            int patternLength;
            int endOfSecsIndex;
            // Retrieve a standard Java DateFormat object with desired format.
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
            if (formatter instanceof SimpleDateFormat) {
                // Retrieve the pattern from the formatter, since we will need to
                // modify it.
                SimpleDateFormat sdFormatter = (SimpleDateFormat) formatter;
                pattern = sdFormatter.toPattern();
                // Append milliseconds and timezone after seconds
                patternLength = pattern.length();
                endOfSecsIndex = pattern.lastIndexOf('s') + 1;
                String newPattern = pattern.substring(0, endOfSecsIndex) + ":SSS z";
                if (endOfSecsIndex < patternLength)
                    newPattern += pattern.substring(endOfSecsIndex, patternLength);
                // 0-23 hour clock (get rid of any other clock formats and am/pm)
                newPattern = newPattern.replace('h', 'H');
                newPattern = newPattern.replace('K', 'H');
                newPattern = newPattern.replace('k', 'H');
                newPattern = newPattern.replace('a', ' ');
                newPattern = newPattern.trim();
                sdFormatter.applyPattern(newPattern);
                formatter = sdFormatter;
            } else {
                formatter = new SimpleDateFormat("yy.MM.dd HH:mm:ss:SSS z");
            }
            return formatter;
        }
    }
}
