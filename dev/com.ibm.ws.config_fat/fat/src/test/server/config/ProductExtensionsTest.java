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
package test.server.config;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 * Schema product extension tests.
 */
public class ProductExtensionsTest {
    public static final Class<?> c = ProductExtensionsTest.class;
    public static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.product.extension");
    public static String javaExc;
    public static String installRoot;

    // Product extension variables.
    public static final String PRODUCT_FEATURE_PATH = "producttest/lib/features/";
    public static final String PRODUCT_BUNDLE_PATH = "producttest/lib/";
    public static final String PRODUCT_EXTENSIONS_PATH = "etc/extensions/";
    public static final String PRODUCT_EXT_NAME = "testproduct";
    public static final String PRODUCT_FEATURE_PROPERTIES_FILE = "productExtensions/propertiesFiles/testproduct.properties";
    public static final String PRODUCT_FEATURE_PRODTEST_MF = "productExtensions/features/prodtest1-1.0.mf";
    public static final String PRODUCT_FEATURE_PRODTEST_JAR = "bundles/test.prod.extensions_1.0.0.jar";
    public static final String PROD_EXT_CONTEXT_ROOT = "/product1-extensions-test";
    public static final String PRODUCT_CONFIG_ALIAS = "prodtest1Config";
    public static final String PRODUCT_SERVER_XML_INVALID = "productExtensions/config/prod.ext.invalid.server.xml";
    public static final String PRODUCT_SERVER_XML_VALID = "productExtensions/config/prod.ext.valid.server.xml";
    public static final String PRODUCT_SERVER_XML_USING_FACTORY_PID = "productExtensions/config/prod.ext.factory.pid.server.xml";

    // User product extension variables.
    public static final String USER_FEATURE_PATH = "usr/extension/lib/features/";
    public static final String USER_BUNDLE_PATH = "usr/extension/lib/";
    public static final String USER_FEATURE_PRODTEST_MF = "productExtensions/features/userProdtest1-1.0.mf";
    public static final String USER_BUNDLE_JAR = "bundles/test.user.prod.extensions_1.0.0.jar";
    public static final String USER_CONFIG_ALIAS = "userProdtest1Config";
    public static final String USER_PRODUCT_SERVER_XML_INVALID = "productExtensions/config/user.prod.ext.invalid.server.xml";
    public static final String USER_PRODUCT_SERVER_XML_VALID = "productExtensions/config/user.prod.ext.valid.server.xml";
    public static final String USER_PRODUCT_SERVER_XML_USING_FACTORY_PID = "productExtensions/config/user.prod.ext.factory.pid.server.xml";
    private static final String USER_PROD_EXT_CONTEXT_ROOT = "/user.product1-extensions-test";

    // Other variables.
    public static final String CORE_PRODUCT_NAME = "core";
    public static final String USR_PRODUCT_NAME = "usr";
    private static final String PASS_STRING = "TEST_PASSED";

    /**
     * Setup the environment.
     * 
     * @param svr The server instance.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setupEnv() throws Exception {
        final String METHOD_NAME = "setup";
        Log.entering(c, METHOD_NAME);
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.product.extension");
        installRoot = server.getInstallRoot();
        javaExc = System.getProperty("java.home") + "/bin/java";
        Log.info(c, METHOD_NAME, "java: " + javaExc);
        Log.info(c, METHOD_NAME, "installRoot: " + installRoot);

        // Install product extensions.
        installProductExtension();
        installUserProductExtension();

        // Create a directory to store the output files.
        File toolsOutputDir = new File(installRoot + "/tool.output.dir");
        toolsOutputDir.mkdir();
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

        tidyup();

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests that the ws-schema.jar tool accounts for product extensions.
     * The test is expected successfully complete and the product names must be tagged appropriately in the
     * serverType section of the generated schema.
     * 
     * @throws Exception
     */
    @Test
    public void testSchemaGeneratorToolOutputForTaggedProdExtEntries() throws Exception {
        final String METHOD_NAME = "testFeatureToolWithProdExtArgument";
        Log.entering(c, METHOD_NAME);

        String[] parms = new String[] { "-jar", installRoot + "/bin/tools/ws-schemagen.jar", installRoot + "/tool.output.dir/schema.xml" };
        ProgramOutput po = server.getMachine().execute(javaExc, parms, installRoot);
        logInfo(po, "tool.output.dir/prodExtFeaturelist.xml");

        RemoteFile rf = server.getFileFromLibertyInstallRoot("tool.output.dir/schema.xml");

        InputStream in = rf.openForReading();
        DataInputStream dis = new DataInputStream(in);
        BufferedReader br = new BufferedReader(new InputStreamReader(dis));
        String line;

        boolean foundUsrConfigElement = false;
        boolean foundProdConfigElement = false;
        int prefixedElementsCount = 0;
        boolean foundServerTypeElement = false;
        while ((line = br.readLine()) != null) {
            if (!foundServerTypeElement && line.contains("name=\"serverType\"")) {
                foundServerTypeElement = true;
            }

            if (foundServerTypeElement) {
                if (line.contains("name=\"" + USR_PRODUCT_NAME + "_" + USER_CONFIG_ALIAS + "\"")) {
                    prefixedElementsCount++;
                    foundUsrConfigElement = true;
                } else if (line.contains("name=\"" + PRODUCT_EXT_NAME + "_" + PRODUCT_CONFIG_ALIAS + "\"")) {
                    prefixedElementsCount++;
                    foundProdConfigElement = true;
                }
            }
        }

        assertTrue("There should have been only 2 elements prefixed with _", (prefixedElementsCount == 2));
        assertTrue("There should have been a " + USR_PRODUCT_NAME + "_" + USER_CONFIG_ALIAS + " element in the schemaOutput.", foundUsrConfigElement);
        assertTrue("There should have been a " + PRODUCT_EXT_NAME + "_" + PRODUCT_CONFIG_ALIAS + " element in the schemaOutput.", foundProdConfigElement);

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Test a product extension installation with metatype configuration.
     * The test is expected to successfully complete.
     * 
     * @throws Exception
     */
    @Test
    public void testProductInstallWithMetatypeConfig() throws Exception {
        String method = "testProductInstallWithMetatypeConfig";
        Log.entering(c, method);

        server.setServerConfigurationFile(PRODUCT_SERVER_XML_VALID);
        server.startServer();
        try {
            server.waitForStringInLog("CWWKT0016I.*" + PROD_EXT_CONTEXT_ROOT);
            HttpUtils.findStringInUrl(server, PROD_EXT_CONTEXT_ROOT + "/test", PASS_STRING);
        } finally {
            server.stopServer();
        }

        Log.exiting(c, method);
    }

    /**
     * Test a product extension installation with the an invalid OCD alias configuration.
     * 
     * @throws Exception
     */
    @Test
    public void testProductInstallWithInvalidAliasConfig() throws Exception {
        String method = "testProductInstallWithInvalidAliasConfig";
        Log.entering(c, method);

        server.setServerConfigurationFile(PRODUCT_SERVER_XML_INVALID);
        server.startServer();
        try {
            server.waitForStringInLog("CWWKT0016I.*" + PROD_EXT_CONTEXT_ROOT);
            HttpUtils.findStringInUrl(server, PROD_EXT_CONTEXT_ROOT + "/test", "TEST_FAILED: getAttribute1 returned: null");
        } finally {
            server.stopServer();
        }

        Log.exiting(c, method);
    }

    /**
     * Test a user product extension installation with metatype configuration.
     * The test is expected to successfully complete.
     * 
     * @throws Exception
     */
    @Test
    public void testUserProductInstallWithMetatypeConfig() throws Exception {
        String method = "testUserProductInstallWithMetatypeConfig";
        Log.entering(c, method);

        server.setServerConfigurationFile(USER_PRODUCT_SERVER_XML_VALID);
        server.startServer();
        try {
            server.waitForStringInLog("CWWKT0016I.*" + USER_PROD_EXT_CONTEXT_ROOT);
            HttpUtils.findStringInUrl(server, USER_PROD_EXT_CONTEXT_ROOT + "/test", PASS_STRING);
        } finally {
            server.stopServer();
        }

        Log.exiting(c, method);
    }

    /**
     * Test a product extension installation with the an invalid OCD alias configuration.
     * 
     * @throws Exception
     */
    @Test
    public void testUserProductInstallWithInvalidAliasConfig() throws Exception {
        String method = "testUserProductInstallWithInvalidAliasConfig";
        Log.entering(c, method);

        server.setServerConfigurationFile(USER_PRODUCT_SERVER_XML_INVALID);
        server.startServer();
        try {
            server.waitForStringInLog("CWWKT0016I.*" + USER_PROD_EXT_CONTEXT_ROOT);
            HttpUtils.findStringInUrl(server, USER_PROD_EXT_CONTEXT_ROOT + "/test", "TEST_FAILED: getAttribute1 returned: null");
        } finally {
            server.stopServer();
        }

        Log.exiting(c, method);
    }

    /**
     * Test a product extension installation with metatype configuration using the factory pid
     * The test is expected to successfully complete.
     * 
     * @throws Exception
     */
    @Test
    public void testProductInstallWithMetatypeConfigUsingFactoryPid() throws Exception {
        String method = "testProductInstallWithMetatypeConfigUsingFactoryPid";
        Log.entering(c, method);

        server.setServerConfigurationFile(PRODUCT_SERVER_XML_USING_FACTORY_PID);
        server.startServer();
        try {
            server.waitForStringInLog("CWWKT0016I.*" + PROD_EXT_CONTEXT_ROOT);
            HttpUtils.findStringInUrl(server, PROD_EXT_CONTEXT_ROOT + "/test", PASS_STRING);
        } finally {
            server.stopServer();
        }

        Log.exiting(c, method);
    }

    /**
     * Test a user product extension installation with metatype configuration using the factory pid.
     * The test is expected to successfully complete.
     * 
     * @throws Exception
     */
    @Test
    public void testUserProductInstallWithMetatypeConfigUsingFactoryPid() throws Exception {
        String method = "testUserProductInstallWithMetatypeConfigUsingFactoryPid";
        Log.entering(c, method);

        server.setServerConfigurationFile(USER_PRODUCT_SERVER_XML_USING_FACTORY_PID);
        server.startServer();
        try {
            server.waitForStringInLog("CWWKT0016I.*" + USER_PROD_EXT_CONTEXT_ROOT);
            HttpUtils.findStringInUrl(server, USER_PROD_EXT_CONTEXT_ROOT + "/test", PASS_STRING);
        } finally {
            server.stopServer();
        }

        Log.exiting(c, method);
    }

    /**
     * Prints an extended debug output.
     * 
     * @param po The programOutput
     * @param fileName
     * @throws Exception
     */
    public void logInfo(ProgramOutput po, String fileName) throws Exception {
        String methodName = "logInfo";
        Log.info(c, methodName, "Return Code: " + po.getReturnCode() + ". STDOUT: " + po.getStdout());

        if (po.getReturnCode() != 0) {
            Log.info(c, methodName, "STDERR: " + po.getStderr());
            RemoteFile rf = server.getFileFromLibertyInstallRoot(fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(rf.openForReading()));
            StringBuffer sb = new StringBuffer();
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            Log.info(c, methodName, "File " + fileName + " content:\n" + sb.toString());
            br.close();
        }
    }

    /**
     * Installs a specific product extension.
     * 
     * @throws Exception
     */
    public static void installProductExtension() throws Exception {
        String method = "installProductExtension";
        Log.entering(c, method, "Intalling product extension: " + PRODUCT_EXT_NAME + ".");

        server.copyFileToLibertyInstallRoot(PRODUCT_FEATURE_PATH, PRODUCT_FEATURE_PRODTEST_MF);
        assertTrue("Product feature: " + PRODUCT_FEATURE_PRODTEST_MF + " should have been copied to: " + PRODUCT_FEATURE_PATH,
                   server.fileExistsInLibertyInstallRoot(PRODUCT_FEATURE_PATH + "prodtest1-1.0.mf"));

        server.copyFileToLibertyInstallRoot(PRODUCT_BUNDLE_PATH, PRODUCT_FEATURE_PRODTEST_JAR);
        assertTrue("Product bundle: " + PRODUCT_FEATURE_PRODTEST_JAR + " should have been copied to: " + PRODUCT_BUNDLE_PATH,
                   server.fileExistsInLibertyInstallRoot(PRODUCT_BUNDLE_PATH + "test.prod.extensions_1.0.0.jar"));

        server.copyFileToLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT_FEATURE_PROPERTIES_FILE);
        assertTrue("Product extension props file: " + PRODUCT_FEATURE_PROPERTIES_FILE + " should have been copied to: " + PRODUCT_EXTENSIONS_PATH,
                   server.fileExistsInLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + "testproduct.properties"));

        Log.exiting(c, method, "Product extension: " + PRODUCT_EXT_NAME + " has been installed.");

    }

    /**
     * Installs a specific product extension if the default USR location.
     * 
     * @throws Exception
     */
    public static void installUserProductExtension() throws Exception {
        String method = "installUserProductExtension";
        Log.entering(c, method, "Intalling user product extension.");

        server.copyFileToLibertyInstallRoot(USER_FEATURE_PATH, USER_FEATURE_PRODTEST_MF);
        assertTrue("User product feature: " + USER_FEATURE_PRODTEST_MF + " should have been copied to: " + USER_FEATURE_PATH,
                   server.fileExistsInLibertyInstallRoot(USER_FEATURE_PATH + "userProdtest1-1.0.mf"));

        server.copyFileToLibertyInstallRoot(USER_BUNDLE_PATH, USER_BUNDLE_JAR);
        assertTrue("User product bundle: " + USER_BUNDLE_JAR + " should have been copied to: " + USER_BUNDLE_PATH,
                   server.fileExistsInLibertyInstallRoot(USER_BUNDLE_PATH + "test.user.prod.extensions_1.0.0.jar"));

        Log.exiting(c, method, "User product extension using feature: " + USER_FEATURE_PRODTEST_MF + " has been installed.");
    }

    /**
     * Cleans up the installation from any dirs/files that this test may have created.
     * 
     * @throws Exception
     */
    public static void tidyup() throws Exception {
        if (server.isStarted())
            server.stopServer();

        server.deleteDirectoryFromLibertyInstallRoot("producttest");
        server.deleteDirectoryFromLibertyInstallRoot("etc/extensions");
        server.deleteDirectoryFromLibertyInstallRoot("usr/extension/");
        server.deleteDirectoryFromLibertyInstallRoot("tool.output.dir");
    }
}
