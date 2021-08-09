/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsonb.fat;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({
                JSONBTest.class,
                JSONBInAppTest.class,
                JSONBContainerTest.class,
                JSONPContainerTest.class,
                JsonUserFeatureTest.class
})
public class FATSuite {

    // run all tests as-is (e.g. EE8 features) and run all tests again with EE9 features+packages by removing usr:testFeatureUsingJsonb-1.0 feature from com.ibm.ws.jsonb.container.fat
    // and adding usr:testFeatureUsingJsonb-2.0 feature to the server
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action().forServers("com.ibm.ws.jsonb.container.fat")
                                    .removeFeature("usr:testFeatureUsingJsonb-1.0")
                                    .addFeature("usr:testFeatureUsingJsonb-2.0")
                                    .forServers("com.ibm.ws.jsonb.fat", "com.ibm.ws.jsonb.inapp", "com.ibm.ws.jsonp.container.fat"));

    public static final String PROVIDER_YASSON = "org.eclipse.yasson.JsonBindingProvider";
    public static final String PROVIDER_JOHNZON = "org.apache.johnzon.jsonb.JohnzonProvider";
    public static final String PROVIDER_GLASSFISH_JSONP = "org.glassfish.json.JsonProviderImpl";
    public static final String PROVIDER_JOHNZON_JSONP = "org.apache.johnzon.core.JsonProviderImpl";

    public static final String JSONB_APP = "jsonbapp";
    public static final String CDI_APP = "jsonbCDIapp";

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jsonb.fat");

    private static final String BUNDLE_TEST_JSONB = "test.jsonb.bundle";
    private static final String BUNDLE_TEST_JSONP = "test.jsonp.bundle";

    private static final String[] TEST_BUNDLES = { BUNDLE_TEST_JSONB, BUNDLE_TEST_JSONP };

    private static final Class<?> c = FATSuite.class;

    public static void jsonbApp(LibertyServer server) throws Exception {
        ShrinkHelper.defaultApp(server, JSONB_APP, "web.jsonbtest");
    }

    public static void cdiApp(LibertyServer server) throws Exception {
        ShrinkHelper.defaultApp(server, CDI_APP, "jsonb.cdi.web");
    }

    @BeforeClass
    public static void beforeSuite() throws Exception {

        if (JakartaEE9Action.isActive()) {
            // Install bundles for jakartaee user features
            for (String bundle : TEST_BUNDLES) {

                Path bundleFile = Paths.get("lib/LibertyFATTestFiles/bundles", bundle + ".jar");
                Path newBundleFile = Paths.get("lib/LibertyFATTestFiles/bundles", bundle + ".jakarta.jar");
                JakartaEE9Action.transformApp(bundleFile, newBundleFile);
                Log.info(c, "beforeSuite", "Transformed bundle " + bundleFile + " to " + newBundleFile);
                server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/" + bundle + ".jakarta.jar");
            }
            // Install jakartaee user features
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/testFeatureUsingJsonp-2.0.mf");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/testFeatureUsingJsonb-2.0.mf");

        } else {
            // Install bundles for javaee user features
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/test.jsonp.bundle.jar");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/test.jsonb.bundle.jar");

            // Install javaee user features
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/testFeatureUsingJsonp-1.1.mf");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/testFeatureUsingJsonb-1.0.mf");
        }
    }

    @AfterClass
    public static void afterSuite() throws Exception {
        // Remove the user extension added during setup
        server.deleteDirectoryFromLibertyInstallRoot("usr/extension/");
    }

}