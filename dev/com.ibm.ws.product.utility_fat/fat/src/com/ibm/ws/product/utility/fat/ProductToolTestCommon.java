/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.product.utility.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import org.junit.AfterClass;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public abstract class ProductToolTestCommon {
    public static final Class<?> c = ProductToolTestCommon.class;
    public static LibertyServer server;
    public static String javaExc;
    public static String installRoot;

    // ETC product extension related variables.
    public static final String PRODUCT_FEATURE_PATH = "producttest/lib/features/";
    public static final String PRODUCT_VERSIONS_PATH = "producttest/lib/versions/";
    public static final String PRODUCT_BUNDLE_PATH = "producttest/lib/";
    public static final String PRODUCT_EXTENSIONS_PATH = "etc/extensions/";
    public static final String PRODUCT_FEATURE_PROPERTIES_FILE = "testproduct.properties";
    public static final String PRODUCT_VERSIONS_PROPERTIES_FILE = "producttest.properties";
    public static final String PRODUCT_FEATURE_PRODTEST_MF = "prodtest-1.0.mf";
    public static final String PRODUCT_FEATURE_PRODTEST_JAR = "com.ibm.ws.prodtest.internal_1.0.jar";
    public static final String PRODUCT_EXT_NAME = "testproduct";

    // USR product extension related properties:
    public static final String USR_PRODUCT_BUNDLE_PATH = "usr/extension/lib/";
    public static final String USR_PRODUCT_FEATURE_PATH = "usr/extension/lib/features/";
    public static final String USR_PRODUCT_VERSIONS_PATH = "usr/extension/lib/versions/";
    public static final String USR_PRODUCT_FEATURE_NAME = "usertest.with.versions.props.file";
    public static final String USR_PRODUCT_FEATURE_MF = "user.ext.version.info.mf";
    public static final String USR_PRODUCT_BUNDLE_JAR = "userProdExt_1.0.0.jar";
    public static final String USR_PRODUCT_VERSIONS_PROPERTIES_FILE = "user.ext.version.info.properties";

    // Product pre-set return codes as set in:
    // com.ibm.ws.kernel.feature.internal.cmdline.ReturnCode,
    // com.ibm.ws.kernel.feature.internal.generator.FeatureListOptions.
    public static final int PRODUCT_EXT_NOT_FOUND = 26;
    public static final int PRODUCT_EXT_NOT_DEFINED = 27;
    public static final int PRODUCT_EXT_NO_FEATURES_FOUND = 28;

    // Other variables.
    public static final String CORE_PRODUCT_NAME = "core";
    public static final String USR_PRODUCT_NAME = "usr";
    public static final int SETUP_PROD_EXT = 1;
    public static final int SETUP_USR_PROD_EXT = 2;
    public static final int SETUP_ALL_PROD_EXTS = 3;
    public static final Collection<String> filesToTidy = new HashSet<String>();

    /**
     * Setup the environment.
     * 
     * @param svr The server instance.
     * 
     * @throws Exception
     */
    public static void setupEnv(LibertyServer svr) throws Exception {
        final String METHOD_NAME = "setup";
        server = svr;
        installRoot = server.getInstallRoot();
        javaExc = System.getProperty("java.home") + "/bin/java";
        Log.entering(c, METHOD_NAME);
        Log.info(c, METHOD_NAME, "java: " + javaExc);
        Log.info(c, METHOD_NAME, "installRoot: " + installRoot);

        // Create a directory to store the output files.
        File toolsOutputDir = new File(installRoot + "/tool.output.dir");
        toolsOutputDir.mkdir();
    }

    /**
     * Setup product extensions.
     * 
     * @param setupOption The option that determines what preset product extension will be installed.
     * 
     * @throws Exception
     */
    public static void setupProductExtensions(int setupOption) throws Exception {
        final String METHOD_NAME = "setupProductExtensions";
        Log.exiting(c, METHOD_NAME);
        boolean setupAll = false;
        switch (setupOption) {
            case SETUP_ALL_PROD_EXTS:
                setupAll = true;
            case SETUP_PROD_EXT:
                // Copy the product's feature manifest.
                server.copyFileToLibertyInstallRoot(PRODUCT_FEATURE_PATH, "productFeatures/" + PRODUCT_FEATURE_PRODTEST_MF);
                assertTrue("product feature: " + PRODUCT_FEATURE_PRODTEST_MF + " should have been copied to: " + PRODUCT_FEATURE_PATH,
                           server.fileExistsInLibertyInstallRoot(PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_PRODTEST_MF));
                // Copy the product's bundle jar.
                server.copyFileToLibertyInstallRoot(PRODUCT_BUNDLE_PATH, "productBundles/" + PRODUCT_FEATURE_PRODTEST_JAR);
                assertTrue("product bundle: " + PRODUCT_FEATURE_PRODTEST_JAR + " should have been copied to: " + PRODUCT_BUNDLE_PATH,
                           server.fileExistsInLibertyInstallRoot(PRODUCT_BUNDLE_PATH + PRODUCT_FEATURE_PRODTEST_JAR));
                // Copy the product's extension properties file.
                server.copyFileToLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH, "productProperties/" + PRODUCT_FEATURE_PROPERTIES_FILE);
                assertTrue("product extension props file: " + PRODUCT_FEATURE_PROPERTIES_FILE + " should have been copied to: " + PRODUCT_EXTENSIONS_PATH,
                           server.fileExistsInLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_PROPERTIES_FILE));
                // Copy the product's version properties file.
                server.copyFileToLibertyInstallRoot(PRODUCT_VERSIONS_PATH, "productVersionProperties/" + PRODUCT_VERSIONS_PROPERTIES_FILE);
                assertTrue("product version props file: " + PRODUCT_VERSIONS_PROPERTIES_FILE + " should have been copied to: " + PRODUCT_VERSIONS_PATH,
                           server.fileExistsInLibertyInstallRoot(PRODUCT_VERSIONS_PATH + PRODUCT_VERSIONS_PROPERTIES_FILE));

                Log.info(c, METHOD_NAME, "Product extension: " + PRODUCT_EXT_NAME + " has been installed.");
                if (!setupAll) {
                    break;
                }
            case SETUP_USR_PROD_EXT:
                // install a (usr) product extension.
                ProgramOutput po = server.installFeature(null, USR_PRODUCT_FEATURE_NAME);
                String stdout = po.getStdout();
                if (!stdout.contains("CWWKF1000I")) {
                    assertEquals("The feature: " + USR_PRODUCT_FEATURE_NAME + " should have been installed. stdout:\r\n" + po.getStdout() + "\r\n" + po.getStderr(), 0,
                                 po.getReturnCode());
                }
                assertTrue("The " + USR_PRODUCT_FEATURE_MF + " feature manifest should exist.",
                           server.fileExistsInLibertyInstallRoot(USR_PRODUCT_FEATURE_PATH + USR_PRODUCT_FEATURE_MF));
                assertTrue("The " + USR_PRODUCT_BUNDLE_JAR + " bundle should exist.", server.fileExistsInLibertyInstallRoot(USR_PRODUCT_BUNDLE_PATH + USR_PRODUCT_BUNDLE_JAR));
                assertTrue("The " + USR_PRODUCT_VERSIONS_PROPERTIES_FILE + " bundle should exist.",
                           server.fileExistsInLibertyInstallRoot(USR_PRODUCT_VERSIONS_PATH + USR_PRODUCT_VERSIONS_PROPERTIES_FILE));

                Log.info(c, METHOD_NAME, "Product extension: " + USR_PRODUCT_FEATURE_NAME + " has been installed in usr");

                break;
            default:
                throw new Exception("Invalid setupOption: " + setupOption);

        }

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Cleans up the installation from any files that may have been left around.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void AfterClassCleanup() throws Exception {
        final String METHOD_NAME = "cleanup";

        Log.entering(c, METHOD_NAME);

        if (server.isStarted())
            server.stopServer();

        server.deleteDirectoryFromLibertyInstallRoot("usr/extension/");
        server.deleteDirectoryFromLibertyInstallRoot("producttest");
        server.deleteDirectoryFromLibertyInstallRoot("etc/extensions");
        server.deleteDirectoryFromLibertyInstallRoot("tool.output.dir");

        for (String filePath : filesToTidy) {
            server.deleteFileFromLibertyInstallRoot(filePath);
        }
        filesToTidy.clear();

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests the command to print product information such as product name and version.
     * 
     * @param cmd The command to execute.
     * @param parms The parameters for the command.
     * @param workDir The working directory where the command is to be issued.
     * 
     * @throws Exception
     */
    public void testPrintProductVersionForAllProductsInstalled(String cmd, String[] parms, String workDir) throws Exception {
        final String METHOD_NAME = "testPrintProductVersionForAllProductsInstalled";
        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.getMachine().execute(cmd, parms, workDir);
        logInfo(po);
        String stdout = po.getStdout();
        assertTrue("The output should contain usr product name: XYZ User Product.", stdout.contains("XYZ User Product"));
        assertTrue("The output should contain usr product version: 1.0.0.", stdout.contains("1.0.0"));
        assertTrue("The output should contain product name: ACMEProductTest.", stdout.contains("ACMEProductTest"));
        assertTrue("The output should contain product version: 9.8.8.9.", stdout.contains("9.8.8.9"));
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests the command to print product information such as product name and version.
     * 
     * @param cmd The command to execute.
     * @param parms The parameters for the command.
     * @param workDir The working directory where the command is to be issued.
     * 
     * @throws Exception
     */
    public void testPrintProductVersionIfixesForAllProductsInstalled(String cmd, String[] parms, String workDir) throws Exception {
        final String METHOD_NAME = "testPrintProductVersionForAllProductsInstalled";
        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.getMachine().execute(cmd, parms, workDir);
        logInfo(po);
        String stdout = po.getStdout();
        assertTrue("The output should contain usr product name: XYZ User Product.", stdout.contains("XYZ User Product"));
        assertTrue("The output should contain usr product version: 1.0.0.", stdout.contains("1.0.0"));
        assertTrue("The output should contain product name: ACMEProductTest.", stdout.contains("ACMEProductTest"));
        assertTrue("The output should contain product version: 9.8.8.9.", stdout.contains("9.8.8.9"));
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests the command that list the installed features for all products installed.
     * 
     * @param cmd The command to execute.
     * @param parms The parameters for the command.
     * @param workDir The working directory where the command is to be issued.
     * 
     * @throws Exception
     */
    public void testPrintFeatureInfoForAllProductsInstalled(String cmd, String[] parms, String workDir) throws Exception {
        final String METHOD_NAME = "testPrintFeatureInfoForAllProductsInstalled";
        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.getMachine().execute(cmd, parms, workDir);
        logInfo(po);
        String stdout = po.getStdout();
        assertTrue("The output should contain usr product feature heading: Product Extension: usr.", stdout.contains("Product Extension: usr"));
        assertTrue("The output should contain usr product feature: user.ext.version.info [1.0.0].", stdout.contains("user.ext.version.info [1.0.0]"));
        assertTrue("The output should contain product feature heading: Product Extension: testproduct.", stdout.contains("Product Extension: testproduct"));
        assertTrue("The output should contain product feature: prodtest-1.0 [1.0.0].", stdout.contains("prodtest-1.0 [1.0.0]"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Prints an extended debug output.
     * 
     * @param po The programOutput
     * @param fileName
     * @throws Exception
     */
    public void logInfo(ProgramOutput po) throws Exception {
        String methodName = "logInfo";
        Log.info(c, methodName, "Return Code: " + po.getReturnCode() + ". STDOUT: " + po.getStdout());

        if (po.getReturnCode() != 0) {
            Log.info(c, methodName, "STDERR: " + po.getStderr());
        }
    }
}
