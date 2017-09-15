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
package com.ibm.ws.jsonb.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class, // so we report at least 1 test on java 7 builds
                JSONBTest.class,
                JSONBInAppTest.class,
                JSONBContainerTest.class,
                JSONPContainerTest.class,
                JsonUserFeatureTest.class
})
public class FATSuite {
    public static final String PROVIDER_YASSON = "org.eclipse.yasson.JsonBindingProvider";
    public static final String PROVIDER_JOHNZON = "org.apache.johnzon.jsonb.JohnzonProvider";
    public static final String PROVIDER_GLASSFISH_JSONP = "org.glassfish.json.JsonProviderImpl";
    public static final String PROVIDER_JOHNZON_JSONP = "org.apache.johnzon.core.JsonProviderImpl";

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jsonb.fat");

    @BeforeClass
    public static void beforeSuite() throws Exception {
        // Install user features
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/testFeatureUsingJsonp-1.1.mf");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/testFeatureUsingJsonb-1.0.mf");

        // Install bundles for user features
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/test.jsonp.bundle.jar");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/test.jsonb.bundle.jar");

        // For some reason the JSON-P 1.1 ref impl doesn't include a META-INF/service/ so we need
        // to manually add one here.  It would be ideal if Glassfish JSON-P could add a service file
        // so the implementation can be discovered in a standard way.  See discussion on this issue:
        // https://github.com/javaee/jsonp/issues/55
//        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "org.glassfish.jsonp-1.1.jar")
//                        .as(ZipImporter.class)
//                        .importFrom(new File("publish/shared/resources/refImpls/javax.json-1.1.jar"))
//                        .as(JavaArchive.class)
//                        .addAsServiceProvider(JsonProvider.class.getName(), "org.glassfish.json.JsonProviderImpl");
//        ShrinkHelper.exportArtifact(jar, "lib/LibertyFATTestFiles/refImpls/");
//        server.copyFileToLibertyInstallRoot("usr/shared/resources/refImpls/", "refImpls" + jar.getName());
    }

    @AfterClass
    public static void afterSuite() throws Exception {
        // Remove the user extension added during setup
        server.deleteDirectoryFromLibertyInstallRoot("usr/extension/");
    }
}