/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsonb.fat;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.test.json.p.FakeProvider;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.JavaInfo;
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

    private static final Class<?> c = FATSuite.class;

    private static final String[] servers = new String[] { "com.ibm.ws.jsonb.container.fat",
                                                           "com.ibm.ws.jsonb.fat",
                                                           "com.ibm.ws.jsonb.inapp",
                                                           "com.ibm.ws.jsonp.container.fat",
                                                           "com.ibm.ws.jsonp.container.userfeature.fat" };

    /**
     * JavaEE8 - without modification - Full Mode - usr:testFeatureUsingJsonb-1.0
     * JakartaEE9 - required modification - Full Mode - usr:testFeatureUsingJsonb-2.0
     * JakartaEE10 - required modification - Lite Mode - usr:testFeatureUsingJsonb-3.0
     */
    @ClassRule
    public static RepeatTests repeat;

    static {
        if (JavaInfo.JAVA_VERSION >= 11) {
            repeat = RepeatTests
                            .withoutModificationInFullMode()
                            .andWith(new JakartaEE9Action().fullFATOnly()
                                            .forServers(servers)
                                            .removeFeatures(new HashSet<>(Arrays.asList("usr:testFeatureUsingJsonb-1.0", "usr:testFeatureUsingJsonb-3.0")))
                                            .addFeature("usr:testFeatureUsingJsonb-2.0")
                                            .removeFeatures(new HashSet<>(Arrays.asList("usr:testFeatureUsingJsonp-1.1", "usr:testFeatureUsingJsonp-2.1")))
                                            .addFeature("usr:testFeatureUsingJsonpa-2.0"))
                            .andWith(new JakartaEE10Action()
                                            .forServers(servers)
                                            .removeFeatures(new HashSet<>(Arrays.asList("usr:testFeatureUsingJsonb-1.0", "usr:testFeatureUsingJsonb-2.0")))
                                            .addFeature("usr:testFeatureUsingJsonb-3.0")
                                            .removeFeatures(new HashSet<>(Arrays.asList("usr:testFeatureUsingJsonp-1.1", "usr:testFeatureUsingJsonp-2.0")))
                                            .addFeature("usr:testFeatureUsingJsonp-2.1"));
        } else {
            // JakartaEE10 requires Java 11, so when we test on Java8,
            // only run the Jakarta EE9 tests, otherwise we get errors
            // related to 0 tests run because the JakartaEE10 tests
            // will all get filtered out on Java8
            repeat = RepeatTests
                            .withoutModificationInFullMode()
                            .andWith(new JakartaEE9Action()
                                            .forServers(servers)
                                            .removeFeatures(new HashSet<>(Arrays.asList("usr:testFeatureUsingJsonb-1.0", "usr:testFeatureUsingJsonb-3.0")))
                                            .addFeature("usr:testFeatureUsingJsonb-2.0")
                                            .removeFeatures(new HashSet<>(Arrays.asList("usr:testFeatureUsingJsonp-1.1", "usr:testFeatureUsingJsonp-2.1")))
                                            .addFeature("usr:testFeatureUsingJsonpa-2.0"));
        }
    }

    //JSONB providers
    public static final String PROVIDER_YASSON = "org.eclipse.yasson.JsonBindingProvider";
    public static final String PROVIDER_JOHNZON_JSONB = "org.apache.johnzon.jsonb.JohnzonProvider";

    //JSONP providers
    public static final String PROVIDER_GLASSFISH_JSONP = "org.glassfish.json.JsonProviderImpl";
    public static final String PROVIDER_JOHNZON_JSONP = "org.apache.johnzon.core.JsonProviderImpl";
    public static final String PROVIDER_PARSSON = "org.eclipse.parsson.JsonProviderImpl";
    public static final String PROVIDER_FAKE = "org.test.json.p.FakeProvider";

    //Application names
    public static final String JSONB_APP = "jsonbapp";
    public static final String CDI_APP = "jsonbCDIapp";

    //Server used for setup
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jsonb.fat");

    //Bundle names
    private static final String BUNDLE_TEST_JSONB = "test.jsonb.bundle";
    private static final String BUNDLE_TEST_JSONP = "test.jsonp.bundle";
    private static final String[] TEST_BUNDLES = { BUNDLE_TEST_JSONB, BUNDLE_TEST_JSONP };

    //Utility methods for installing applications
    public static void jsonbApp(LibertyServer server) throws Exception {
        ShrinkHelper.defaultApp(server, JSONB_APP, "web.jsonbtest");
    }

    public static void cdiApp(LibertyServer server) throws Exception {
        ShrinkHelper.defaultApp(server, CDI_APP, "jsonb.cdi.web");
    }

    @BeforeClass
    public static void beforeSuite() throws Exception {
        //REMINDER - for future EE version remember to update the corresponding transformer file
        //i.e. wlp-jakarta-transformer/jakarta-versions-eeX.properties
        //This is necessary for bundle MANIFEST.MF to be updated with the correct version of jsonp/b spec versions.
        if (JakartaEE10Action.isActive()) {
            Log.info(c, "beforeSuite", "Transforming bundle for Jakarta EE 10");
            // Install bundles for jakartaee user features
            for (String bundle : TEST_BUNDLES) {
                Path bundleFile = Paths.get("lib/LibertyFATTestFiles/bundles", bundle + ".jar");
                Path newBundleFile = Paths.get("lib/LibertyFATTestFiles/bundles", bundle + ".jakarta.jar");
                JakartaEE10Action.transformApp(bundleFile, newBundleFile);
                Log.info(c, "beforeSuite", "Transformed bundle " + bundleFile + " to " + newBundleFile);
                server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/" + bundle + ".jakarta.jar");
            }

            // Install jakartaee user features
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/testFeatureUsingJsonp-2.1.mf");
            server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/testFeatureUsingJsonb-3.0.mf");

            addFakeProvider(server);
        } else if (JakartaEE9Action.isActive()) {
            Log.info(c, "beforeSuite", "Transforming bundle for Jakarta EE 9");
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
            Log.info(c, "beforeSuite", "Transforming bundle for Java EE 8");
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

    /**
     * This method will set environment variables for jsonp/b implementation classes
     * based on the jakarta level being used.
     *
     * @param server - server being used
     */
    public static void configureImpls(LibertyServer server) {

        if (JakartaEE10Action.isActive()) {
            //TODO replace with a third party implementation when one is developed
            server.addEnvVar("JSONP_PATH", "fakeProvider/1.0/jakarta/");
            server.addEnvVar("JSONP_JAR", "fake-json-p.jar");

            //TODO replace with a third party implementation when one is developed
            server.addEnvVar("JSONB_PATH", "yasson/3.0.0/");
            server.addEnvVar("JSONB_JAR", "yasson.jar");
            server.addEnvVar("JSONB_ALT_JAR", "yasson.jar");

            server.addEnvVar("YASSON_PATH", "yasson/3.0.0/");
            server.addEnvVar("YASSON_JAR", "yasson.jar");
        } else if (JakartaEE9Action.isActive()) {
            server.addEnvVar("JSONP_PATH", "johnzon/1.2.18/jakarta/");
            server.addEnvVar("JSONP_JAR", "johnzon-core.jar");

            server.addEnvVar("JSONB_PATH", "johnzon/1.2.18/jakarta/");
            server.addEnvVar("JSONB_JAR", "johnzon-jsonb.jar, johnzon-mapper.jar");
            server.addEnvVar("JSONB_ALT_JAR", "johnzon-*.jar");

            server.addEnvVar("YASSON_PATH", "yasson/2.0.4/");
            server.addEnvVar("YASSON_JAR", "yasson.jar");
        } else {
            server.addEnvVar("JSONP_PATH", "johnzon/1.2.18/javax/");
            server.addEnvVar("JSONP_JAR", "johnzon-core.jar");

            server.addEnvVar("JSONB_PATH", "johnzon/1.2.18/javax/");
            server.addEnvVar("JSONB_JAR", "johnzon-jsonb.jar, johnzon-mapper.jar");
            server.addEnvVar("JSONB_ALT_JAR", "johnzon-*.jar");

            server.addEnvVar("YASSON_PATH", "yasson/1.0.4/");
            server.addEnvVar("YASSON_JAR", "yasson.jar");
        }
    }

    /**
     * Returns the jsonb provider class name that is available to the applications running
     * based on the jakarta level being used.
     *
     * @param isAvailable - provide a false value if you want a provider class name that ISN'T available for negative testing.
     * @return String - classname
     */
    public static String getJsonbProviderClassName(boolean... isAvailable) {
        if (isAvailable.length > 1) {
            throw new RuntimeException("Provide only one value for isAvailable (true/false)");
        }

        //If the length is 1 and set to false, provide the native case
        if (isAvailable.length == 1 && !isAvailable[0]) {
            return JakartaEE10Action.isActive() ? PROVIDER_JOHNZON_JSONB : PROVIDER_YASSON;
        }

        //In all other situations we should return the provider that is available
        return JakartaEE10Action.isActive() ? PROVIDER_YASSON : PROVIDER_JOHNZON_JSONB;
    }

    /**
     * Returns the jsonp provider class name that is available to the applications running
     * based on the jakarta level being used.
     *
     * @param isAvailable - provide a false value if you want a provider class name that ISN'T available for negative testing.
     * @return String - classname
     */
    public static String getJsonpProviderClassName(boolean... isAvailable) {
        if (isAvailable.length > 1) {
            throw new RuntimeException("Provide only one value for isAvailable (true/false)");
        }

        //If the length is 1 and set to false, provide the native case
        if (isAvailable.length == 1 && !isAvailable[0]) {
            return JakartaEE10Action.isActive() ? PROVIDER_JOHNZON_JSONP : PROVIDER_FAKE;
        }

        //In all other situations we should return the provider that is available
        return JakartaEE10Action.isActive() ? PROVIDER_FAKE : PROVIDER_JOHNZON_JSONP;

    }

    /**
     * Bundles and exports fake provider to shared/resources/ dir.
     *
     * @param server - dummy server just to get shared dir
     * @throws Exception
     */
    private static void addFakeProvider(LibertyServer server) throws Exception {
        if (JakartaEE10Action.isActive()) {
            RemoteFile parsson = server.getFileFromLibertySharedDir("resources/parsson/1.1.0/jakarta/parsson.jar");
            Path fakeDestination = Paths.get(server.getServerSharedPath(), "resources", "fakeProvider", "1.0", "jakarta");

            JavaArchive fake_json_p = ShrinkWrap.create(ZipImporter.class, "fake-json-p.jar")
                            .importFrom(new File(parsson.getAbsolutePath()))
                            .as(JavaArchive.class)
                            .addPackage("org.test.json.p")
                            .addAsServiceProvider(jakarta.json.spi.JsonProvider.class, FakeProvider.class);

            ShrinkHelper.exportArtifact(fake_json_p, fakeDestination.toString());
        }
    }
}