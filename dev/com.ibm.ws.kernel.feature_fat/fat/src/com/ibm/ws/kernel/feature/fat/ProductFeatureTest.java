/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2012, 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.kernel.feature.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
public class ProductFeatureTest {

    private static final Class<?> c = ProductFeatureTest.class;
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.fat.product");

    private static final String PRODUCT_FEATURE_PRODTEST_MF = "prodtest-1.0.mf";
    private static final String PRODUCT_FEATURE_PRODTEST_PRIVATE_MF = "prodtestprivate-1.0.mf";
    private static final String PRODUCT_FEATURE_PRODTEST_JAR = "com.ibm.ws.prodtest.internal_1.0.jar";
    private static final String PRODUCT_FEATURE_PROPERTIES_FILE = "testproduct.properties";
    private static final String PRODUCT2_FEATURE_PROPERTIES_FILE = "testproduct2.properties";
    private static final String PRODUCT3_FEATURE_PROPERTIES_FILE = "testproduct3.properties";
    private static final String PRODUCT_FEATURE_BAD_PROPERTIES_FILE = "testproductbad.properties";
    private static final String PRODUCT_FEATURE_BAD_WLP_PROPERTIES_FILE = "testproductbadwlp.properties";
    private static final String PRODUCT_FEATURE_CONTAINS_SYMBOL_PROPERTIES_FILE = "testproductcontainssymbol.properties";
    private static final String PRODUCT_FEATURE_BAD_PATH_PROPERTIES_FILE = "testproductbadpath.properties";
    private static final String PRODUCT_FEATURE_USR_PROPERTIES_FILE = "usr.properties";
    private static final String ODD_CHARS_IN_PATH = "(test a&)";

    private static final String installFeatureMsgPrefix = "CWWKF0012I:";
    private static final String uninstallFeatureMsgPrefix = "CWWKF0013I:";
    private static final String productInstallnotInPropertiesFilePrefix = "CWWKF0018E:";
    private static final String productInstallPathPrefix = "CWWKF0017E:";
    private static final String productInstallPathContainsSymbol = "CWWKF0027E:";
    private static final String productInstallPathSameAsWlp = "CWWKF0028E:";
    private static final String serverUpdatedMsgPrefix = "CWWKG0017I:";
    private static final String missingFeatureMsgPrefix = "CWWKF0001E";
    private static final String missingFeatureCoreMsgPrefix = "CWWKF0042E";

    private static final String PRODUCT_FEATURE_PATH = "producttest/lib/features/";
    private static final String PRODUCT_BUNDLE_PATH = "producttest/lib/";
    private static final String PRODUCT_EXTENSIONS_PATH = "etc/extensions/";

    private static final String CACHE_DIRECTORY = "workarea/platform/";
    private static final String FEATURE_BUNDLE_CACHE = CACHE_DIRECTORY + "feature.bundles.cache";

    /**
     * Copy the necessary features and bundles to the liberty server directories
     * 
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        final String METHOD_NAME = "setup";

        Log.entering(c, METHOD_NAME);

        if (server.isStarted())
            server.stopServer();

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * This method removes all the testing artifacts from the server directories.
     * 
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        final String METHOD_NAME = "tearDown";

        Log.entering(c, METHOD_NAME);

        if (server.isStarted())
            server.stopServer();

        server.uninstallProductExtension("productA");
        server.uninstallProductExtension("productB");
        server.uninstallProductExtension("producttest");
        server.uninstallProductExtension("testproduct");
        server.uninstallProductExtension("testproduct2");
        server.uninstallProductExtension("testproduct3");
        server.deleteDirectoryFromLibertyInstallRoot("producttest");

        server.deleteDirectoryFromLibertyInstallRoot(ODD_CHARS_IN_PATH);
        server.deleteFileFromLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_PROPERTIES_FILE);
        server.deleteFileFromLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + PRODUCT2_FEATURE_PROPERTIES_FILE);
        server.deleteFileFromLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + PRODUCT3_FEATURE_PROPERTIES_FILE);
        server.deleteFileFromLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_BAD_PROPERTIES_FILE);
        server.deleteFileFromLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_BAD_WLP_PROPERTIES_FILE);
        server.deleteFileFromLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_USR_PROPERTIES_FILE);
        server.deleteFileFromLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_CONTAINS_SYMBOL_PROPERTIES_FILE);
        server.deleteFileFromLibertyInstallRoot(PRODUCT_BUNDLE_PATH + "../bin/test.bat");

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a product feature is installed when added to server.xml.
     * * com.ibm.websphere.productInstall= ends with a /
     * 
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testProductFeatureInstalled() throws Exception {
        final String METHOD_NAME = "testProductFeatureInstalled";

        Log.entering(c, METHOD_NAME);

        server.copyFileToLibertyInstallRoot(PRODUCT_FEATURE_PATH, PRODUCT_FEATURE_PRODTEST_MF);
        server.copyFileToLibertyInstallRoot(PRODUCT_BUNDLE_PATH, PRODUCT_FEATURE_PRODTEST_JAR);
        server.copyFileToLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT_FEATURE_PROPERTIES_FILE);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(METHOD_NAME + ".log");

        // Now move the server xml with a product feature available
        TestUtils.makeConfigUpdateSetMark(server, "server_product_features.xml");
        // Get the install feature message for the added product feature.
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("prodtest-1.0 product feature was not installed and should have been: " + output, output.contains("prodtest-1.0"));

        // Finally check that removing the product feature from server.xml will uninstall it.
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("product feature prodtest-1.0 was not uninstalled and should have been: " + output, output.contains("prodtest-1.0"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a product feature cannot include private features from core
     * 
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testProductFeatureInstalledWithPrivateContent() throws Exception {
        final String METHOD_NAME = "testProductFeatureInstalledWithPrivateContent";

        Log.entering(c, METHOD_NAME);

        server.copyFileToLibertyInstallRoot(PRODUCT_FEATURE_PATH, PRODUCT_FEATURE_PRODTEST_PRIVATE_MF);
        server.copyFileToLibertyInstallRoot(PRODUCT_BUNDLE_PATH, PRODUCT_FEATURE_PRODTEST_JAR);
        server.copyFileToLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT_FEATURE_PROPERTIES_FILE);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(METHOD_NAME + ".log");

        // Now move the server xml with a product feature available
        TestUtils.makeConfigUpdateSetMark(server, "server_product_private_features.xml");
        // Get the install feature message for the added product feature.
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("prodtestprivate-1.0 product feature was not installed and should have been: " + output, output.contains("prodtestprivate-1.0"));

        // check for missing feature message for internal feature that we don't have access to
        output = server.waitForStringInLogUsingMark(missingFeatureMsgPrefix);
        assertNotNull("We haven't found the " + missingFeatureMsgPrefix + " in the logs.", output);
        assertTrue("wrong missing feature found: " + output, output.contains("com.ibm.websphere.appserver.javax.servlet-3.0"));

        // Finally check that removing the product feature from server.xml will uninstall it.
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("product feature prodtestprivate-1.0 was not uninstalled and should have been: " + output, output.contains("prodtestprivate-1.0"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a product feature is installed when added to server.xml.
     * com.ibm.websphere.productInstall= does not end with a /
     * 
     * @throws Exception
     */
    @Test
    public void testProductFeatureInstalledNoSlashAtEnd() throws Exception {
        final String METHOD_NAME = "testProductFeatureInstalledNoSlashAtEnd";

        Log.entering(c, METHOD_NAME);

        server.copyFileToLibertyInstallRoot(PRODUCT_FEATURE_PATH, PRODUCT_FEATURE_PRODTEST_MF);
        server.copyFileToLibertyInstallRoot(PRODUCT_BUNDLE_PATH, PRODUCT_FEATURE_PRODTEST_JAR);
        server.copyFileToLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT2_FEATURE_PROPERTIES_FILE);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(METHOD_NAME + ".log");

        // Now move the server xml with a product feature available
        TestUtils.makeConfigUpdateSetMark(server, "server_product2_features.xml");
        // Get the install feature message for the added product feature.
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("prodtest-1.0 product feature was not installed and should have been: " + output, output.contains("prodtest-1.0"));

        // Finally check that removing the product feature from server.xml will uninstall it.
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("product feature prodtest-1.0 was not uninstalled and should have been: " + output, output.contains("prodtest-1.0"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a product feature is installed when added to server.xml.
     * com.ibm.websphere.productInstall= has odd characters in it
     * 
     * @throws Exception
     */
    @Test
    public void testProductFeatureInstalledOddCharacters() throws Exception {
        final String METHOD_NAME = "testProductFeatureInstalledOddCharacters";

        Log.entering(c, METHOD_NAME);

        server.copyFileToLibertyInstallRoot(ODD_CHARS_IN_PATH + "/" + PRODUCT_FEATURE_PATH, PRODUCT_FEATURE_PRODTEST_MF);
        server.copyFileToLibertyInstallRoot(ODD_CHARS_IN_PATH + "/" + PRODUCT_BUNDLE_PATH, PRODUCT_FEATURE_PRODTEST_JAR);
        server.copyFileToLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT3_FEATURE_PROPERTIES_FILE);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(METHOD_NAME + ".log");

        // Now move the server xml with a product feature available
        TestUtils.makeConfigUpdateSetMark(server, "server_product3_features.xml");
        // Get the install feature message for the added product feature.
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("prodtest-1.0 product feature was not installed and should have been: " + output, output.contains("prodtest-1.0"));

        // Finally check that removing the product feature from server.xml will uninstall it.
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("product feature prodtest-1.0 was not uninstalled and should have been: " + output, output.contains("prodtest-1.0"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a product feature located as a peer of wlp/ can have
     * it's features installed and uninstalled.
     * com.ibm.websphere.productInstall= does not end with a /
     * 
     * C:\stuff\wlp
     * C:\stuff\productA\lib\features
     * 
     * @throws Exception
     */
    @Test
    public void testProductFeatureInstalledAsPeerToWLPNoSlashAtEnd() throws Exception {
        final String METHOD_NAME = "testProductFeatureInstalledAsPeerToWLPNoSlashAtEnd";

        Log.entering(c, METHOD_NAME);

        server.installProductExtension("productA");
        server.installProductBundle("productA", "com.ibm.ws.prodtest.internal_1.0");
        server.installProductFeature("productA", "prodtest-1.0");

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(METHOD_NAME + ".log");

        // Now move the server xml with a product feature available
        TestUtils.makeConfigUpdateSetMark(server, "server_productA_features.xml");
        // Get the install feature message for the added product feature.
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);
        assertTrue("prodtest-1.0 product feature was not installed and should have been: " + output, output.contains("prodtest-1.0"));

        // Finally check that removing the product feature from server.xml will uninstall it.
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);
        assertTrue("product feature prodtest-1.0 was not uninstalled and should have been: " + output, output.contains("prodtest-1.0"));

        server.stopServer();
        server.uninstallProductBundle("productA", "com.ibm.ws.prodtest.internal_1.0");
        server.uninstallProductFeature("productA", "prodtest-1.0");
        server.uninstallProductExtension("productA");

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a product feature located as a peer of wlp/ can have
     * it's features installed and uninstalled.
     * com.ibm.websphere.productInstall= has a / on the end
     * 
     * C:\stuff\wlp
     * C:\stuff\productA\lib\features
     * 
     * @throws Exception
     */
    @Test
    public void testProductFeatureInstalledAsPeerToWLPSlashAtEnd() throws Exception {
        final String METHOD_NAME = "testProductFeatureInstalledAsPeerToWLPSlashAtEnd";

        Log.entering(c, METHOD_NAME);

        server.installProductExtension("productB");
        server.installProductBundle("productB", "com.ibm.ws.prodtest.internal_1.0");
        server.installProductFeature("productB", "prodtest-1.0");

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(METHOD_NAME + ".log");

        // Now move the server xml with a product feature available
        TestUtils.makeConfigUpdateSetMark(server, "server_productB_features.xml");
        // Get the install feature message for the added product feature.
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);
        assertTrue("prodtest-1.0 product feature was not installed and should have been: " + output, output.contains("prodtest-1.0"));

        // Finally check that removing the product feature from server.xml will uninstall it.
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");
        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);
        assertTrue("product feature prodtest-1.0 was not uninstalled and should have been: " + output, output.contains("prodtest-1.0"));

        server.stopServer();
        server.uninstallProductBundle("productB", "com.ibm.ws.prodtest.internal_1.0");
        server.uninstallProductFeature("productB", "prodtest-1.0");
        server.installProductExtension("productB");

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a product feature is installed when in server.xml at server start.
     * 
     * @throws Exception
     */
    @Test
    public void testProductFeatureInstalledAtStartup() throws Exception {
        final String METHOD_NAME = "testproductFeatureInstalledAtStartup";

        Log.entering(c, METHOD_NAME);

        server.copyFileToLibertyInstallRoot(PRODUCT_FEATURE_PATH, PRODUCT_FEATURE_PRODTEST_MF);
        server.copyFileToLibertyInstallRoot(PRODUCT_BUNDLE_PATH, PRODUCT_FEATURE_PRODTEST_JAR);
        server.copyFileToLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT_FEATURE_PROPERTIES_FILE);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_product_features.xml");
        server.startServer(METHOD_NAME + ".log");

        // Check that product feature was there by uninstalling it.
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");
        String output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);

        assertTrue("product feature prodtest-1.0 was not uninstalled and should have been: " + output, output.contains("prodtest-1.0"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a product feature called usr is not installed when added to server.xml.
     * 
     * @throws Exception
     */
    @Test
    public void testUsrProductFeatureNotInstalled() throws Exception {
        final String METHOD_NAME = "testUsrProductFeatureNotInstalled";

        Log.entering(c, METHOD_NAME);

        server.copyFileToLibertyInstallRoot(PRODUCT_FEATURE_PATH, PRODUCT_FEATURE_PRODTEST_MF);
        server.copyFileToLibertyInstallRoot(PRODUCT_BUNDLE_PATH, PRODUCT_FEATURE_PRODTEST_JAR);
        server.copyFileToLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT_FEATURE_USR_PROPERTIES_FILE);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(METHOD_NAME + ".log");

        // Now move the server xml with a product feature available
        TestUtils.makeConfigUpdateSetMark(server, "server_usr_product_features.xml");
        //CWWKF0001E: A feature definition could not be found for usr:prodtest-1.0
        // Get the install feature message for the added product feature.
        String output = server.waitForStringInLogUsingMark(missingFeatureMsgPrefix);
        assertNotNull("We haven't found the " + missingFeatureMsgPrefix + " in the logs.", output);
        assertTrue("product feature usr:prodtest-1.0 should not have been installed: " + output, output.contains("usr:prodtest-1.0"));

        // Finally check that removing the product feature from server.xml will uninstall it.
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");
        //CWWKG0017I: The server configuration was successfully updated in 0.229 seconds.
        output = server.waitForStringInLogUsingMark(serverUpdatedMsgPrefix);
        assertNotNull("We haven't found the " + serverUpdatedMsgPrefix + " in the logs.", output);

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test issues an error for a product properties file that does not contain com.ibm.websphere.productInstall.
     * 
     * @throws Exception
     */
    @Test
    public void testProductFeatureBadPropertiesFile() throws Exception {
        final String METHOD_NAME = "testProductFeatureBadPropertiesFile";

        Log.entering(c, METHOD_NAME);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.copyFileToLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT_FEATURE_BAD_PROPERTIES_FILE);

        server.startServer(METHOD_NAME + ".log");

        server.deleteFileFromLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_BAD_PROPERTIES_FILE);
        // CWWKF0018E: Property com.ibm.websphere.productInstall not found in product properties file testproductbad.properties.
        // Get the com.ibm.websphere.productInstall not found in product properties file message.
        String output = server.waitForStringInLog(productInstallnotInPropertiesFilePrefix);
        assertNotNull("We haven't found the " + productInstallnotInPropertiesFilePrefix + " in the logs.", output);

        assertTrue(PRODUCT_FEATURE_BAD_PROPERTIES_FILE + " was not flagged with an error and should have been: " + output, output.contains(PRODUCT_FEATURE_BAD_PROPERTIES_FILE));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test issues an error for a product properties file that has a product install
     * path that is the same as ${wlp.install.dir}.
     * 
     * @throws Exception
     */
    @Test
    @ExpectedFFDC("com.ibm.ws.kernel.productinfo.DuplicateProductInfoException")
    public void testProductFeatureInstallSameAsWlpInstallDir() throws Exception {
        final String METHOD_NAME = "testProductFeatureInstallSameAsWlpInstallDir";

        Log.entering(c, METHOD_NAME);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.copyFileToLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT_FEATURE_BAD_WLP_PROPERTIES_FILE);
        server.startServer(METHOD_NAME + ".log");

        server.deleteFileFromLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_BAD_WLP_PROPERTIES_FILE);
        // look for the error message 
        String output = server.waitForStringInLog(productInstallPathSameAsWlp);
        assertNotNull("We haven't found the " + productInstallPathSameAsWlp + " in the logs.", output);

        assertTrue(PRODUCT_FEATURE_BAD_WLP_PROPERTIES_FILE + " was not flagged with an error and should have been: " + output,
                   output.contains(PRODUCT_FEATURE_BAD_WLP_PROPERTIES_FILE));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test issues an error for a product properties file that has a product install
     * path that has a symbol.
     * 
     * @throws Exception
     */
    @Test
    public void testProductFeatureInstallContainsSymbol() throws Exception {
        final String METHOD_NAME = "testProductFeatureInstallContainsSymbol";

        Log.entering(c, METHOD_NAME);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.copyFileToLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT_FEATURE_CONTAINS_SYMBOL_PROPERTIES_FILE);
        server.startServer(METHOD_NAME + ".log");

        server.deleteFileFromLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_CONTAINS_SYMBOL_PROPERTIES_FILE);
        // look for the error message 
        String output = server.waitForStringInLog(productInstallPathContainsSymbol);
        assertNotNull("We haven't found the " + productInstallPathContainsSymbol + " in the logs.", output);

        assertTrue(PRODUCT_FEATURE_CONTAINS_SYMBOL_PROPERTIES_FILE + " was not flagged with an error and should have been: " + output,
                   output.contains(PRODUCT_FEATURE_CONTAINS_SYMBOL_PROPERTIES_FILE));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test issues an error for a product properties file that contains a bad path.
     * 
     * @throws Exception
     */
    @Test
    public void testProductFeatureBadPath() throws Exception {
        final String METHOD_NAME = "testProductFeatureBadPath";

        Log.entering(c, METHOD_NAME);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        // delete extensions dir incase a previous test did not clean up it's .properties files
        // extra .properties file was causing an extra CWWKF0017E with a different filename in it so this test would fail
        server.deleteDirectoryFromLibertyInstallRoot("etc/extensions");
        server.copyFileToLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT_FEATURE_BAD_PATH_PROPERTIES_FILE);
        server.startServer(METHOD_NAME + ".log");

        server.deleteFileFromLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_BAD_PATH_PROPERTIES_FILE);
        // CWWKF0017E: Product install path {0} specified in product properties file {1} could not be found.
        // CWWKF0017E: Product install path lib/features/ specified in product properties file testproductbadpath.properties could not be found.
        // Get the path not found message.
        String output = server.waitForStringInLog(productInstallPathPrefix);
        assertNotNull("We haven't found the " + productInstallPathPrefix + " in the logs.", output);

        assertTrue(PRODUCT_FEATURE_BAD_PATH_PROPERTIES_FILE + " was not flagged with an path error and should have been: " + output,
                   output.contains(PRODUCT_FEATURE_BAD_PATH_PROPERTIES_FILE));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test tests with a product feature that does not have a .mf file.
     * 
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.io.FileNotFoundException")
    public void testProductFeatureNotFound() throws Exception {
        final String METHOD_NAME = "testProductFeatureNotFound";

        Log.entering(c, METHOD_NAME);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(METHOD_NAME + ".log");

        // Now move the server xml with a product feature that will not be found.
        TestUtils.makeConfigUpdateSetMark(server, "server_product_features_not_found.xml");
        // Get the feature definition could not be found message.
        String output = server.waitForStringInLogUsingMark(missingFeatureMsgPrefix);
        assertNotNull("We haven't found the " + missingFeatureMsgPrefix + " in the logs.", output);

        assertTrue("testproduct:producttestnotfound product feature was installed and should have been: " + output, output.contains("testproduct:producttestnotfound"));

        Log.exiting(c, METHOD_NAME);
    }

    @Test
    @AllowedFFDC("java.io.FileNotFoundException")
    public void testCoreFeatureNotFound() throws Exception {
        final String METHOD_NAME = "testCoreFeatureNotFound";

        Log.entering(c, METHOD_NAME);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(METHOD_NAME + ".log");

        // Now move the server xml with a product feature that will not be found.
        TestUtils.makeConfigUpdateSetMark(server, "server_core_features_not_found.xml");
        // Get the feature definition could not be found message.
        String output = server.waitForStringInLogUsingMark(missingFeatureCoreMsgPrefix);
        assertNotNull("We haven't found the " + missingFeatureCoreMsgPrefix + " in the logs.", output);

        assertTrue("coretest-1.0 product feature name was not found in the output: " + output, output.contains("coretest-1.0"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a .mf file added to producttest/lib/features after the server is
     * up and running will be found and installed when the feature is specified in server.xml.
     * The test ensures that this happens during server update.
     * 
     * The feature structure is as follows:
     * 
     * pfeatureA is defined in pfeatureA.mf by IBM-ShortName: pfeatureA-1.0
     * 
     * @throws Exception
     */
    @Test
    public void testProductFeatureManifestFileAddedAfterServerIsAlreadyUp() throws Exception {
        final String METHOD_NAME = "testProductFeatureManifestFileAddedAfterServerIsAlreadyUp";

        Log.entering(c, METHOD_NAME);

        // Start with no features being installed.
        server.setServerConfigurationFile("server_no_features.xml");

        server.deleteDirectoryFromLibertyInstallRoot("producttest");
        server.copyFileToLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT_FEATURE_PROPERTIES_FILE);
        server.copyFileToLibertyInstallRoot(PRODUCT_FEATURE_PATH, "pfeatureA-1.0.mf");
        server.copyFileToLibertyInstallRoot(PRODUCT_BUNDLE_PATH, "pbundle1_1.0.0.jar");

        server.startServer(METHOD_NAME + ".log");

        // Now move the server xml with pfeatureA
        TestUtils.makeConfigUpdateSetMark(server, "server_product_featureA.xml");

        // Get the install feature message for the initial set up of updated features
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);
        assertTrue("pfeatureA-1.0 was not installed and should have been: " + output, output.contains("pfeatureA-1.0"));

        // Now check that we have the expected bundles installed.
        Properties bundleCacheProperties = getCacheProperties(FEATURE_BUNDLE_CACHE);

        assertTrue("testproduct|pbundle1/[1.0.0,2.0.0) was not installed and should have been: " + bundleCacheProperties,
                   bundleCacheProperties.containsKey("testproduct|pbundle1/[1.0.0,2.0.0)"));

        // Finally check that removing the feature will also uninstall the feature.
        TestUtils.makeConfigUpdateSetMark(server, "server_no_features.xml");

        output = server.waitForStringInLogUsingMark(uninstallFeatureMsgPrefix);
        assertNotNull("We haven't found the " + uninstallFeatureMsgPrefix + " in the logs.", output);
        assertTrue("pfeatureA-1.0 was not uninstalled and should have been: " + output, output.contains("pfeatureA-1.0"));

        // Now check that bundles of the features are gone.
        bundleCacheProperties = getCacheProperties(FEATURE_BUNDLE_CACHE);

        assertFalse("testproduct|pbundle1/[1.0.0,2.0.0) was installed and should not have been: " + bundleCacheProperties,
                    bundleCacheProperties.containsKey("testproduct|pbundle1/[1.0.0,2.0.0)"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * This method loads the feature.cache file as properties.
     * 
     * @param cacheFile - The cache file to read.
     * @return - A properties object containing the properties from the feature.cache file.
     * @throws Exception
     */
    private Properties getCacheProperties(String cacheFile) throws Exception {
        Properties cacheProps = new Properties();
        InputStream cacheStream = null;
        try {
            cacheStream = server.getFileFromLibertyServerRoot(cacheFile).openForReading();
            cacheProps.load(cacheStream);
        } finally {
            if (cacheStream != null)
                cacheStream.close();
        }

        return cacheProps;
    }

    /**
     * TestDescription:
     * This test ensures that a product feature can be packaged with minify.
     * Including type=file content.
     * 
     * @throws Exception
     */
    @Test
    public void testProductFeatureMinifiable() throws Exception {
        final String METHOD_NAME = "testProductFeatureMinifiable";

        Log.entering(c, METHOD_NAME);

        server.copyFileToLibertyInstallRoot(PRODUCT_FEATURE_PATH, PRODUCT_FEATURE_PRODTEST_MF);
        server.copyFileToLibertyInstallRoot(PRODUCT_BUNDLE_PATH, PRODUCT_FEATURE_PRODTEST_JAR);
        server.copyFileToLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT_FEATURE_PROPERTIES_FILE);

        //add some the "test.bat" type=file content to the product extension's "bin" directory
        //bin is a peer of lib similar to the wlp.install.dir
        server.copyFileToLibertyInstallRoot(PRODUCT_BUNDLE_PATH + "../bin", "test.bat");

        // Ensure we use a server xml with the testproduct feature product extension available
        server.setServerConfigurationFile("product_ext_for_minify.xml");

        //we want to validate that the content related to our product extension got packaged
        List<String> names = Arrays.asList(new String[] { "wlp/producttest/bin/test.bat",
                                                         "wlp/producttest/lib/com.ibm.ws.prodtest.internal_1.0.jar",
                                                         "wlp/producttest/lib/features/prodtest-1.0.mf",
                                                         "wlp/etc/extensions/testproduct.properties" });
        List<String> namesToValidate = new ArrayList<String>(names.size());
        namesToValidate.addAll(names);

        //minify the server
        MinifiedServerTestUtils minifyUtils = new MinifiedServerTestUtils();
        try {
            //set up to minify
            minifyUtils.setup(this.getClass().getName(),
                              "com.ibm.ws.kernel.feature.fat.product",
                              server);
            //actually minify
            RemoteFile serverPackage = minifyUtils.minifyServer();
            if (serverPackage != null) {
                ZipInputStream minifiedServerZip = null;
                try {
                    minifiedServerZip = new ZipInputStream(serverPackage.openForReading());

                    ZipEntry entry;
                    while ((entry = minifiedServerZip.getNextEntry()) != null) {
                        if (namesToValidate.remove(entry.getName()))
                            Log.info(c, METHOD_NAME, "Found match in minified archive for: " + entry.getName());
                    }
                } finally {
                    //close stream
                    if (minifiedServerZip != null)
                        minifiedServerZip.close();
                }
                for (String name : namesToValidate) {
                    fail("The minified package did not contain the product extension file: " + name);
                }
            } else {
                //if the server package was null we are on z/os
                Log.info(c, METHOD_NAME, "Tests for minify are not active currently on zOS");
            }

        } finally {
            //be sure to clean up
            minifyUtils.tearDown();
        }
        Log.exiting(c, METHOD_NAME);
    }
}
