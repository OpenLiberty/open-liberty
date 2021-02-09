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

package com.ibm.ws.kernel.feature.fat;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@RunWith(FATRunner.class)
public class MinifiedServerEnforceSingletonTest {
    private static final Class<?> c = MinifiedServerEnforceSingletonTest.class;

    private static final String serverName = "com.ibm.ws.kernel.feature.fat.minify.enforce.singleton";
    private static LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);
    private static MinifiedServerTestUtils minifyUtils = new MinifiedServerTestUtils();

    private static final String WLP_DIR = "wlp/";
    private static final String PRODUCT_EXTENSION_NAME = "minifyEnforceSingleton";
    private static final String PRODUCT_FEATURE_PATH = PRODUCT_EXTENSION_NAME + "/lib/features/";
    private static final String PRODUCT_EXTENSIONS_PATH = "etc/extensions/";

    private static final String PRODUCT_FEATURE_A1 = "minifyEnforceSingletonA-1.0";
    private static final String PRODUCT_FEATURE_B1 = "minifyEnforceSingletonB-1.0";
    private static final String PRODUCT_FEATURE_B2 = "minifyEnforceSingletonB-2.0";
    private static final String PRODUCT_FEATURE_C1 = "minifyEnforceSingletonC-1.0";
    private static final String PRODUCT_FEATURE_C2 = "minifyEnforceSingletonC-2.0";

    private static final String PRODUCT_FEATURE_A1_MF = "minifyEnforceSingletonA-1.0.mf";
    private static final String PRODUCT_FEATURE_B1_MF = "minifyEnforceSingletonB-1.0.mf";
    private static final String PRODUCT_FEATURE_B2_MF = "minifyEnforceSingletonB-2.0.mf";
    private static final String PRODUCT_FEATURE_C1_MF = "minifyEnforceSingletonC-1.0.mf";
    private static final String PRODUCT_FEATURE_C2_MF = "minifyEnforceSingletonC-2.0.mf";
    private static final String PRODUCT_FEATURE_PROPERTIES_FILE = PRODUCT_EXTENSION_NAME + ".properties";

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setup() throws Exception {
        server.copyFileToLibertyInstallRoot(PRODUCT_FEATURE_PATH, PRODUCT_FEATURE_A1_MF);
        server.copyFileToLibertyInstallRoot(PRODUCT_FEATURE_PATH, PRODUCT_FEATURE_B1_MF);
        server.copyFileToLibertyInstallRoot(PRODUCT_FEATURE_PATH, PRODUCT_FEATURE_B2_MF);
        server.copyFileToLibertyInstallRoot(PRODUCT_FEATURE_PATH, PRODUCT_FEATURE_C1_MF);
        server.copyFileToLibertyInstallRoot(PRODUCT_FEATURE_PATH, PRODUCT_FEATURE_C2_MF);
        server.copyFileToLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT_FEATURE_PROPERTIES_FILE);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        minifyUtils.tearDown();

        if (server.isStarted())
            server.stopServer();

        server.uninstallProductExtension(PRODUCT_EXTENSION_NAME);
        server.deleteDirectoryFromLibertyInstallRoot(PRODUCT_EXTENSION_NAME);
        server.deleteFileFromLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_PROPERTIES_FILE);
    }

    @After
    public void afterTest() throws Exception {
        ServerConfiguration serverConfig = server.getServerConfiguration();
        serverConfig.getFeatureManager().getFeatures().clear();
        server.updateServerConfiguration(serverConfig);
        server.setExtraArgs(Collections.emptyList());
        // need to clear out the work area where it records resolution in the utils from the run
        // this cached resolution needs to be cleared so the next test starts fresh
        server.deleteDirectoryFromLibertyServerRoot("workarea");
    }

    @Test
    public void testMinifyEnforceSingleton() throws Exception {
        Log.entering(c, name.getMethodName());

        ServerConfiguration serverConfig = server.getServerConfiguration();
        Set<String> features = serverConfig.getFeatureManager().getFeatures();
        features.add(PRODUCT_EXTENSION_NAME + ':' + PRODUCT_FEATURE_A1);
        features.add(PRODUCT_EXTENSION_NAME + ':' + PRODUCT_FEATURE_C2);
        server.updateServerConfiguration(serverConfig);

        List<String> namesToValidate = new ArrayList<>();
        namesToValidate.add(WLP_DIR + PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_PROPERTIES_FILE);
        namesToValidate.add(WLP_DIR + PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_A1_MF);
        namesToValidate.add(WLP_DIR + PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_B2_MF);
        namesToValidate.add(WLP_DIR + PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_C2_MF);

        List<String> namesToInvalidate = Arrays.asList(WLP_DIR + PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_B1_MF,
                                                       WLP_DIR + PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_C1_MF);

        //set up to minify
        minifyUtils.setup(this.getClass().getName(),
                          serverName,
                          server);
        minifyAndValidate(namesToValidate, namesToInvalidate);

        Log.exiting(c, name.getMethodName());
    }

    @Test
    public void testMinifyIgnoreSingletonNoFallback() throws Exception {
        Log.entering(c, name.getMethodName());

        ServerConfiguration serverConfig = server.getServerConfiguration();
        Set<String> features = serverConfig.getFeatureManager().getFeatures();
        features.add(PRODUCT_EXTENSION_NAME + ':' + PRODUCT_FEATURE_A1);
        features.add(PRODUCT_EXTENSION_NAME + ':' + PRODUCT_FEATURE_C2);
        server.updateServerConfiguration(serverConfig);

        List<String> namesToValidate = new ArrayList<>();
        namesToValidate.add(WLP_DIR + PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_PROPERTIES_FILE);
        namesToValidate.add(WLP_DIR + PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_A1_MF);
        namesToValidate.add(WLP_DIR + PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_B2_MF);
        namesToValidate.add(WLP_DIR + PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_C2_MF);

        List<String> namesToInvalidate = Arrays.asList(WLP_DIR + PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_B1_MF,
                                                       WLP_DIR + PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_C1_MF);

        //set up to minify
        minifyUtils.setup(this.getClass().getName(),
                          serverName,
                          server);

        // minify with fallback to ignore singletons, but the first attempt while
        // enforcing singletons should succeed.
        server.setExtraArgs(Collections.singletonList("-Dinternal.minify.ignore.singleton=true"));
        minifyAndValidate(namesToValidate, namesToInvalidate);

        Log.exiting(c, name.getMethodName());
    }

    @Test
    public void testMinifyIgnoreSingletonWithConflictsFallback() throws Exception {
        Log.entering(c, name.getMethodName());

        ServerConfiguration serverConfig = server.getServerConfiguration();
        Set<String> features = serverConfig.getFeatureManager().getFeatures();
        features.add(PRODUCT_EXTENSION_NAME + ':' + PRODUCT_FEATURE_A1);
        features.add(PRODUCT_EXTENSION_NAME + ':' + PRODUCT_FEATURE_B1);
        features.add(PRODUCT_EXTENSION_NAME + ':' + PRODUCT_FEATURE_C2);
        server.updateServerConfiguration(serverConfig);

        List<String> namesToValidate = new ArrayList<>();
        namesToValidate.add(WLP_DIR + PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_PROPERTIES_FILE);
        namesToValidate.add(WLP_DIR + PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_A1_MF);
        namesToValidate.add(WLP_DIR + PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_B1_MF);
        namesToValidate.add(WLP_DIR + PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_C1_MF);
        namesToValidate.add(WLP_DIR + PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_C2_MF);

        List<String> namesToInvalidate = Arrays.asList(WLP_DIR + PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_B2_MF);

        //set up to minify
        minifyUtils.setup(this.getClass().getName(),
                          serverName,
                          server);

        // minify with fallback to ignore singletons, first attempt while
        // enforcing singletons should fail causing the fallback.
        server.setExtraArgs(Collections.singletonList("-Dinternal.minify.ignore.singleton=true"));
        minifyAndValidate(namesToValidate, namesToInvalidate);

        Log.exiting(c, name.getMethodName());
    }

    @Test
    @AllowedFFDC("java.lang.IllegalArgumentException")
    public void testMinifyEnforceSingletonWithConflicts() throws Exception {
        Log.entering(c, name.getMethodName());

        ServerConfiguration serverConfig = server.getServerConfiguration();
        Set<String> features = serverConfig.getFeatureManager().getFeatures();
        features.add(PRODUCT_EXTENSION_NAME + ':' + PRODUCT_FEATURE_A1);
        features.add(PRODUCT_EXTENSION_NAME + ':' + PRODUCT_FEATURE_B1);
        features.add(PRODUCT_EXTENSION_NAME + ':' + PRODUCT_FEATURE_C2);
        server.updateServerConfiguration(serverConfig);

        List<String> namesToValidate = new ArrayList<>();
        namesToValidate.add(WLP_DIR + PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_PROPERTIES_FILE);
        namesToValidate.add(WLP_DIR + PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_A1_MF);
        namesToValidate.add(WLP_DIR + PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_B1_MF);

        List<String> namesToInvalidate = Arrays.asList(WLP_DIR + PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_B2_MF,
                                                       WLP_DIR + PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_C1_MF,
                                                       WLP_DIR + PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_C2_MF);

        //set up to minify
        minifyUtils.setup(this.getClass().getName(),
                          serverName,
                          server);

        minifyAndValidate(namesToValidate, namesToInvalidate);

        Log.exiting(c, name.getMethodName());
    }

    private void minifyAndValidate(List<String> namesToValidate, List<String> namesToInvalidate) throws Exception {
        RemoteFile serverPackage = minifyUtils.minifyServer();
        if (serverPackage != null) {
            ZipInputStream minifiedServerZip = null;
            try {
                minifiedServerZip = new ZipInputStream(serverPackage.openForReading());

                ZipEntry entry;
                while ((entry = minifiedServerZip.getNextEntry()) != null) {
                    if (namesToValidate.remove(entry.getName()))
                        Log.info(c, name.getMethodName(), "Found match in minified archive for: " + entry.getName());
                    if (namesToInvalidate.contains(entry.getName())) {
                        fail("The minified package contained the product extension file: " + entry.getName());
                    }
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
            Log.info(c, name.getMethodName(), "Tests for minify are not active currently on zOS");
        }
    }
}
