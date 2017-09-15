/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.embeddable;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test the merging of added ProductExtensions. There are currently 3 sources of ProductExtensions:
 * 1) Embedder SPI
 * 2) WLP_PRODUCT_EXT_DIR environment variable
 * 3) etc/extensions install directory.
 */
public class EmbeddedServerMergeProductExtensionTest {

    private static final Class<?> c = EmbeddedServerMergeProductExtensionTest.class;

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.merge.product.extension.fat");

    private static final String PRODA_ETC = "prodA.properties";
    private static final String PRODB_ETC = "prodB.properties";
    private static final String SERVER_ETC_PATH = "mergeProductExtensions/etc/extensions/";

    @BeforeClass
    static public void setupForTests() throws Exception {

        // Prime etc/extensions
        server.copyFileToLibertyInstallRoot("etc/extensions", SERVER_ETC_PATH + PRODA_ETC);
        server.copyFileToLibertyInstallRoot("etc/extensions", SERVER_ETC_PATH + PRODB_ETC);

        // Note: properties file locations are using relative paths which are resolved using
        // the parent of the install root (i.e. they need to be at the same directory level as
        // "wlp").

        // Copy test Products to the install root for the server
        server.copyFileToLibertyInstallRoot("../products", "mergeProductExtensions/products/prodA.properties");
        server.copyFileToLibertyInstallRoot("../products", "mergeProductExtensions/products/prodB.properties");

        server.copyFileToLibertyInstallRoot("../products/productA/lib", "mergeProductExtensions/products/productA/lib/com.ibm.ws.prodtest.internal_1.0.jar");
        server.copyFileToLibertyInstallRoot("../products/productA/lib/features", "mergeProductExtensions/products/productA/lib/features/prodA-1.0.mf");
        server.copyFileToLibertyInstallRoot("../products/productA/lib/versions", "mergeProductExtensions/products/productA/lib/versions/prodA.properties");

        server.copyFileToLibertyInstallRoot("../products/productB/lib", "mergeProductExtensions/products/productB/lib/com.ibm.ws.prodtest.internal_1.0.jar");
        server.copyFileToLibertyInstallRoot("../products/productB/lib/features", "mergeProductExtensions/products/productB/lib/features/prodB-1.0.mf");
        server.copyFileToLibertyInstallRoot("../products/productB/lib/versions", "mergeProductExtensions/products/productB/lib/versions/prodB.properties");

        // Copy test Products2 to the install root for the server
        server.copyFileToLibertyInstallRoot("../products2", "mergeProductExtensions/products2/prodA.properties");

        server.copyFileToLibertyInstallRoot("../products2/productA/lib", "mergeProductExtensions/products2/productA/lib/com.ibm.ws.prodtest.internal_1.0.jar");
        server.copyFileToLibertyInstallRoot("../products2/productA/lib/features", "mergeProductExtensions/products2/productA/lib/features/prodA-1.0.mf");
        server.copyFileToLibertyInstallRoot("../products2/productA/lib/versions", "mergeProductExtensions/products2/productA/lib/versions/prodA1.properties");

        // Copy test Products3 to the install root for the server
        server.copyFileToLibertyInstallRoot("../products3", "mergeProductExtensions/products3/prodA.properties");

        server.copyFileToLibertyInstallRoot("../products3/productA/lib", "mergeProductExtensions/products3/productA/lib/com.ibm.ws.prodtest.internal_1.0.jar");
        server.copyFileToLibertyInstallRoot("../products3/productA/lib/features", "mergeProductExtensions/products3/productA/lib/features/prodA-1.0.mf");
        server.copyFileToLibertyInstallRoot("../products3/productA/lib/versions", "mergeProductExtensions/products3/productA/lib/versions/prodA3.properties");

        // Make an product extension target with no .properties files
        server.copyFileToLibertyInstallRoot("../products0", "mergeProductExtensions/products/productA/lib/features/prodA-1.0.mf");
    }

    @AfterClass
    static public void tearDown() throws Exception {
        server.stopServer();

        // Cleanup the stuff added for Product Extensions
        server.deleteDirectoryFromLibertyInstallRoot("etc/extensions");
        server.deleteDirectoryFromLibertyInstallRoot("../products");
        server.deleteDirectoryFromLibertyInstallRoot("../products2");
        server.deleteDirectoryFromLibertyInstallRoot("../products3");
        server.deleteDirectoryFromLibertyInstallRoot("../products0");

    }

    @After
    public void testCleanup() throws Exception {
        // Stop the server
        server.stopServer();

        // Delete any env product setup
        server.deleteDirectoryFromLibertyInstallRoot("../wlp_ext_products");

        // Restore the previous bootstrap.properties (if applicable)
        server.renameLibertyServerRootFile("bootstrap.properties.restore", "bootstrap.properties");
    }

    @Test
    public void testProductExtensionMergeEnvOverEtc() throws Exception {
        final String METHOD_NAME = "testProductExtensionMergeEnvOverEtc";
        Log.entering(c, METHOD_NAME);

        // Setup for server start

        // Setup directory were WLP_PRODUCT_EXT_DIR points to have a version 2 for "prodA"
        String prodAV2 = "mergeProductExtensions/products2/prodA.properties";
        server.copyFileToLibertyInstallRoot("../wlp_ext_products", prodAV2);

        // Start the server (wait for server started message)
        server.startServer();

        // Verify that "prodA" is the version 17.2.0.2 from the Env and not from etc/extensions
        // "product = Test Product A  17.2.0.2, Test Product B  17.2.0.1, WebSphere Application Server 17.0.0.2 (wlp-1.0.
        // 17.201705011120)".
        assertTrue("Products Extensions were not applied", !!!server.findStringsInLogs("product.*Test Product A  17.2.0.2, Test Product B  17.2.0.1").isEmpty());

        // Check for Product Extensions added via the ENV
        // CWWKE0940I: The prodA product extension has a product identifier of com.ibm.producta and a product installation location of products2/productA. ..."
        assertTrue("Products Extensions were not added via env", !!!server.findStringsInLogs("CWWKE0940I:.*prodA.*com.ibm.producta.*products2/productA").isEmpty());

        // Verify that both prodA and prodB have their features added
        // "CWWKF0012I: The server installed the following features: [servlet-3.0, jsp-2.2, timedexit-1.0, prodA:prodA-1.0, prodB:prodB-1.0]."
        assertTrue("Products Extensions features not added", !!!server.findStringsInLogs("CWWKF0012I:.*prodA:prodA-1.0.*prodB:prodB-1.0").isEmpty());

        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testProductExtensionEnvNullDirOverEtc() throws Exception {
        final String METHOD_NAME = "testProductExtensionEnvNullDirOverEtc";
        Log.entering(c, METHOD_NAME);

        // Setup for server start

        // Prime directory were WLP_PRODUCT_EXT_DIR points to -- without a ".properties" file
        String prodAV2 = "mergeProductExtensions/product0/nothingToSeeHere.txt";
        server.copyFileToLibertyInstallRoot("../wlp_ext_products", prodAV2);

        // Start the server (wait for server started message)
        server.startServer();

        // Verify that "prodA" is the version 17.2.0.1. from from etc/extensions
        // "product = Test Product A  17.2.0.1, Test Product B  17.2.0.1, WebSphere Application Server 17.0.0.2 (wlp-1.0.
        // 17.201705011120)".
        assertTrue("Products Extensions were not applied", !!!server.findStringsInLogs("product.*Test Product A  17.2.0.1, Test Product B  17.2.0.1").isEmpty());

        // Check that Product Extensions were not added via the ENV
        // CWWKE0940I: The prodA product extension has a product identifier of com.ibm.producta and a product installation location of products2/productA. ..."
        assertTrue("Products Extensions were added via env", server.findStringsInLogs("CWWKE0940I:.*").isEmpty());

        // Check that Product Extensions were not added via the embedder SPI
        // "CWWKE0108I: The product extension prodA was programmatically enabled. The product identifier of the product extension is com.ibm.producta. The product install
        //  location of the product extension is products3/productA."
        assertTrue("Products Extensions were added via SPI", server.findStringsInLogs("CWWKE0108I:.*").isEmpty());

        // Verify that both prodA and prodB have their features added
        // "CWWKF0012I: The server installed the following features: [servlet-3.0, jsp-2.2, timedexit-1.0, prodA:prodA-1.0, prodB:prodB-1.0]."
        assertTrue("Products Extensions features not added", !!!server.findStringsInLogs("CWWKF0012I:.*prodA:prodA-1.0.*prodB:prodB-1.0").isEmpty());

        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testProductExtensionMergeSPIOverEnvOverEtc() throws Exception {
        final String METHOD_NAME = "testProductExtensionMergeSPIOverEnvOverEtc";
        Log.entering(c, METHOD_NAME);

        // Setup for server start

        // Setup directory were WLP_PRODUCT_EXT_DIR points to have a version 2 for "prodA"
        String prodAV2 = "mergeProductExtensions/products2/prodA.properties";
        server.copyFileToLibertyInstallRoot("../wlp_ext_products", prodAV2);

        // Prime ProdA V3 from SPI
        // Define a Product Extension from the Embedder SPI to override both prodA from Env and etc/extensions.
        server.renameLibertyServerRootFile("bootstrap.properties", "bootstrap.properties.restore");
        server.copyFileToLibertyServerRoot("mergeProductExtensions/bootstrap.spienv_properties");
        server.renameLibertyServerRootFile("bootstrap.spienv_properties", "bootstrap.properties");

        // Start the server (wait for server started message)

        server.startServer();

        // Verify that "prodA" is the version 17.3.0.2 from the Embedder SPI and not from Env or etc/extensions
        // "product = Test Product A  17.3.0.2, Test Product B  17.2.0.1, WebSphere Application Server 17.0.0.2 (wlp-1.0.
        // 17.201705011120)".
        assertTrue("Products Extensions were not applied", !!!server.findStringsInLogs("product.*Test Product A  17.3.0.2, Test Product B  17.2.0.1").isEmpty());

        // Make sure that prodA was not defined for Product Extensions via the ENV
        assertTrue("Products Extensions were added via env", server.findStringsInLogs("CWWKE0940I:.*prodA.*com.ibm.producta.*products3/productA").isEmpty());

        // prodA should have been added by the embedder SPI
        // "CWWKE0108I: The product extension prodA was programmatically enabled. The product identifier of the product extension is com.ibm.producta. The product install
        //  location of the product extension is products3/productA."
        assertTrue("Products Extensions were not added via SPI", !!!server.findStringsInLogs("CWWKE0108I:.*prodA.*com.ibm.producta.*products3/productA").isEmpty());

        // Verify that both prodA and prodB have their features added
        // "CWWKF0012I: The server installed the following features: [servlet-3.0, jsp-2.2, timedexit-1.0, prodA:prodA-1.0, prodB:prodB-1.0]."
        assertTrue("Products Extensions features not added", !!!server.findStringsInLogs("CWWKF0012I:.*prodA:prodA-1.0.*prodB:prodB-1.0").isEmpty());

        Log.exiting(c, METHOD_NAME);
    }
}
