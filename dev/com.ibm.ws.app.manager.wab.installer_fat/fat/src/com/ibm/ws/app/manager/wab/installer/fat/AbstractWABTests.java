/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.wab.installer.fat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
public abstract class AbstractWABTests {

    private static final String BUNDLE_TEST_WAB1 = "test.wab1";
    private static final String BUNDLE_TEST_WAB2 = "test.wab2";
    private static final String BUNDLE_TEST_WAB3 = "test.wab3";
    private static final String[] PRODUCT_BUNDLES = { BUNDLE_TEST_WAB1, BUNDLE_TEST_WAB2, BUNDLE_TEST_WAB3 };

    protected static final String PRODUCT1 = "product1";
    protected static final String PRODUCT2 = "product2";
    protected static final String[] PRODUCTS = { PRODUCT1, PRODUCT2 };

    protected static final String CONFIGS = "configs/";
    protected static final String CONFIG_DEFAULT = CONFIGS + "testDefault.xml";

    protected static LibertyServer server = null;

    private static final Class<?> c = AbstractWABTests.class;

    private static boolean isJakarta9;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        if (JakartaEE9Action.isActive()) {
            /* transform any wab bundles with javax.servlet classes to jakartaee-9 equivalents */
            Log.info(c, "setUp", "Transforming product bundles to Jakarta-EE-9: ");
            for (String bundle : PRODUCT_BUNDLES) {

                Path bundleFile = Paths.get("publish", "productbundles", bundle + ".jar");
                Path newBundleFile = Paths.get("publish", "productbundles", bundle + ".jakarta.jar");
                JakartaEE9Action.transformApp(bundleFile, newBundleFile);
                Log.info(c, "setUp", "Transformed bundle " + bundleFile + " to " + newBundleFile);
            }
            isJakarta9 = true;
        } else {
            isJakarta9 = false;
        }

        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.app.manager.wab.installer");
        server.setServerConfigurationFile(CONFIG_DEFAULT);

        for (String product : PRODUCTS) {
            Log.info(c, "setUp", "Installing product properties file for: " + product);
            server.installProductExtension(product);

            String feature = product + (isJakarta9 ? "_jakarta" : "");
            Log.info(c, "setUp", "Installing product feature: " + feature);
            server.installProductFeature(product, feature);

            Log.info(c, "setUp", "Installing product bundles: " + product);

            String extensionIfJakarta = (isJakarta9 ? ".jakarta" : "");
            server.installProductBundle(product, product);
            server.installProductBundle(product, BUNDLE_TEST_WAB1 + extensionIfJakarta);
            server.installProductBundle(product, BUNDLE_TEST_WAB2 + extensionIfJakarta);
            server.installProductBundle(product, BUNDLE_TEST_WAB3 + extensionIfJakarta);
        }
    }

    @Before
    public void beforeTest() throws Exception {
        Log.info(c, name.getMethodName(), "===== Starting test " + name.getMethodName() + " =====");
    }

    protected void setConfiguration(String config) throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
    }

    @After
    public void afterTest() throws Exception {
        Log.info(c, name.getMethodName(), "===== Stopping test " + name.getMethodName() + " =====");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "teardown", "===== Enter Class teardown =====");
        if (server != null) {
            for (String product : PRODUCTS) {
                server.uninstallProductBundle(product, product + "_1.0.0");
                server.uninstallProductBundle(product, BUNDLE_TEST_WAB1);
                server.uninstallProductBundle(product, BUNDLE_TEST_WAB2);
                server.uninstallProductBundle(product, BUNDLE_TEST_WAB3);
                server.uninstallProductFeature(product, product);
                server.uninstallProductExtension(product);
            }
        }
    }

    protected void checkWAB(String path, String... expected) throws Exception {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        HttpUtils.findStringInUrl(server, path, expected);
    }

}
