/*******************************************************************************
* Copyright (c) 2016 IBM Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/

package com.ibm.ws.microprofile.config.fat.tests;

import java.io.File;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;
import com.ibm.ws.microprofile.config.fat.suite.SharedShrinkWrapApps;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

/**
 *
 */
public class DefaultSourcesTest extends AbstractConfigApiTest {

    private final static String testClassName = "DefaultSourcesTest";

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new FeatureReplacementAction("cdi-1.2", "cdi-2.0"));

    @ClassRule
    public static SharedServer SHARED_SERVER = new ShrinkWrapSharedServer("SimpleConfigSourcesServer");

    @BuildShrinkWrap
    public static Archive buildApp() {
        String APP_NAME = "defaultSources";

        JavaArchive testAppUtils = SharedShrinkWrapApps.getTestAppUtilsJar();

        JavaArchive defaultSources_jar = ShrinkWrap.create(JavaArchive.class, APP_NAME + ".jar")
                        .addPackage("com.ibm.ws.microprofile.appConfig.defaultSources.tests")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".jar/resources/META-INF/MANIFEST.MF"), "MANIFEST.MF")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".jar/resources/META-INF/config.properties"), "config.properties")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".jar/resources/META-INF/microprofile-config.json"), "microprofile-config.json")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".jar/resources/META-INF/microprofile-config.properties"),
                                               "microprofile-config.properties")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".jar/resources/META-INF/microprofile-config.xml"), "microprofile-config.xml");

        JavaArchive earlib_jar = ShrinkWrap.create(JavaArchive.class, "earlib.jar")
                        .addAsManifestResource(new File("test-applications/earlib.jar/resources/META-INF/config.properties"), "config.properties")
                        .addAsManifestResource(new File("test-applications/earlib.jar/resources/META-INF/microprofile-config.json"), "microprofile-config.json")
                        .addAsManifestResource(new File("test-applications/earlib.jar/resources/META-INF/microprofile-config.properties"),
                                               "microprofile-config.properties")
                        .addAsManifestResource(new File("test-applications/earlib.jar/resources/META-INF/microprofile-config.xml"), "microprofile-config.xml");

        WebArchive defaultSources_war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibrary(testAppUtils)
                        .addAsLibrary(defaultSources_jar)
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/permissions.xml"), "permissions.xml")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/config.properties"), "config.properties")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/microprofile-config.json"), "microprofile-config.json")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/microprofile-config.properties"),
                                               "microprofile-config.properties")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/microprofile-config.xml"), "microprofile-config.xml")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/web.xml"), "web.xml")
                        .addAsWebInfResource(new File("test-applications/" + APP_NAME + ".war/resources/WEB-INF/web.xml"), "web.xml");

        WebArchive warVisibility_war = ShrinkWrap.create(WebArchive.class, "warVisibility_" + ".war")
                        .addAsLibrary(testAppUtils)
                        .addAsManifestResource(new File("test-applications/warVisibility.war/resources/META-INF/web.xml"), "web.xml")
                        .addAsManifestResource(new File("test-applications/warVisibility.war/resources/META-INF/microprofile-config.properties"),
                                               "microprofile-config.properties")
                        .addAsManifestResource(new File("test-applications/warVisibility.war/resources/META-INF/permissions.xml"), "permissions.xml");

        EnterpriseArchive defaultSources_ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".ear/resources/META-INF/application.xml"), "application.xml")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".ear/resources/META-INF/microprofile-config.properties"),
                                               "microprofile-config.properties")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".ear/resources/META-INF/permissions.xml"), "permissions.xml")
                        .addAsModule(defaultSources_war)
                        .addAsModule(warVisibility_war)
                        .addAsLibrary(earlib_jar);

        return defaultSources_ear;
    }

    public DefaultSourcesTest() {
        super("/defaultSources/");
    }

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Rule
    public TestName testName = new TestName();

    /**
     * Test that a simple config loads from the default locations
     * no provided files so config should just be process environment
     * variables, System.properties and WAS files.
     *
     * @throws Exception
     */
    @Test
    public void defaultsGetConfig() throws Exception {
        test(testName);
    }

    @Test
    public void defaultsGetBuilderWithDefaults() throws Exception {
        test(testName);
    }

    /**
     * Tests that we can get a builder that will not include the default sources
     *
     * @throws Exception
     */
    @Test
    public void defaultsGetEmptyBuilderNoDefaults() throws Exception {
        test(testName);
    }

    /**
     * Tests that a config source can be loaded from within a jar
     */
    @Test
    public void defaultsGetConfigPathJar() throws Exception {
        test(testName);
    }

    /**
     * Tests that a config source can be loaded from within a war
     */
    @Test
    public void defaultsGetConfigPathWar() throws Exception {
        test(testName);
    }

    /**
     * Tests that a config source can be loaded from all
     * valid places within a ear
     */
    @Test
    public void defaultsGetConfigPathEar() throws Exception {
        test(testName);
    }

    /**
     * Test that the microprofile-config.properties files are sourced ok
     *
     * @throws Exception
     */
    @Test
    public void defaultsGetConfigProperties() throws Exception {
        test(testName);
    }

    /**
     * Test that the WAS server level *.xml, *.properties and *.env files are sourced
     *
     * @throws Exception
     */
    @Test
    public void defaultsGetConfigWasSpecific() throws Exception {
        test(testName);
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void defaultsGetConfigPathSysProps() throws Exception {
        test(testName);
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void defaultsGetConfigPathProcEnv() throws Exception {
        test(testName);
    }

}
