/*******************************************************************************
* Copyright (c) 2016, 2021 IBM Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/

package com.ibm.ws.microprofile.config.fat.tests;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.appConfig.classLoaderCache.test.ClassLoaderCacheTestServlet;
import com.ibm.ws.microprofile.config.fat.suite.SharedShrinkWrapApps;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ClassLoaderCacheTest extends FATServletClient {

    public static final String APP_NAME = "classLoaderCache";

    public static final String EARA = APP_NAME + "A";
    public static final String EARB = APP_NAME + "B";
    public static final String EARA_NAME = EARA + ".ear";
    public static final String EARB_NAME = EARB + ".ear";

    public static final String WARA1 = EARA + "1";
    public static final String WARA2 = EARA + "2";
    public static final String WARA1_NAME = WARA1 + ".war";
    public static final String WARA2_NAME = WARA2 + ".war";

    public static final String WARB1 = EARB + "1";
    public static final String WARB2 = EARB + "2";
    public static final String WARB1_NAME = WARB1 + ".war";
    public static final String WARB2_NAME = WARB2 + ".war";

    // Don't repeat against mpConfig > 1.4 since SmallRye Config implementation doesn't have methods for accessing cache for ConfigProviderResolver. e.g. getConfigCacheSize()
    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat("ClassLoaderCacheServer", MicroProfileActions.MP13, MicroProfileActions.MP33);

    @Server("ClassLoaderCacheServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        JavaArchive testAppUtils = SharedShrinkWrapApps.getTestAppUtilsJar();

        WebArchive classLoaderCacheA1_war = ShrinkWrap.create(WebArchive.class, WARA1_NAME).addAsLibrary(testAppUtils)
                                                      .addPackage("com.ibm.ws.microprofile.appConfig.classLoaderCache.test");

        WebArchive classLoaderCacheA2_war = ShrinkWrap.create(WebArchive.class, WARA2_NAME)
                                                      .addAsLibrary(testAppUtils).addPackage("com.ibm.ws.microprofile.appConfig.classLoaderCache.test");

        WebArchive classLoaderCacheB1_war = ShrinkWrap.create(WebArchive.class, WARB1_NAME)
                                                      .addAsLibrary(testAppUtils).addPackage("com.ibm.ws.microprofile.appConfig.classLoaderCache.test");

        WebArchive classLoaderCacheB2_war = ShrinkWrap.create(WebArchive.class, WARB2_NAME)
                                                      .addAsLibrary(testAppUtils).addPackage("com.ibm.ws.microprofile.appConfig.classLoaderCache.test");

        EnterpriseArchive classLoaderCacheA_ear = ShrinkWrap.create(EnterpriseArchive.class, EARA_NAME)
                                                            .addAsManifestResource(new File("test-applications/" + EARA_NAME + "/resources/META-INF/application.xml"),
                                                                                   "application.xml")
                                                            .addAsManifestResource(new File("test-applications/" + EARA_NAME + "/resources/META-INF/permissions.xml"),
                                                                                   "permissions.xml")
                                                            .addAsModule(classLoaderCacheA1_war).addAsModule(classLoaderCacheA2_war);

        EnterpriseArchive classLoaderCacheB_ear = ShrinkWrap.create(EnterpriseArchive.class, EARB_NAME)
                                                            .addAsManifestResource(new File("test-applications/" + EARB_NAME + "/resources/META-INF/application.xml"),
                                                                                   "application.xml")
                                                            .addAsManifestResource(new File("test-applications/" + EARA_NAME + "/resources/META-INF/permissions.xml"), //shares the same permissions file as EARA
                                                                                   "permissions.xml")
                                                            .addAsModule(classLoaderCacheB1_war).addAsModule(classLoaderCacheB2_war);

        ShrinkHelper.exportDropinAppToServer(server, classLoaderCacheA_ear, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, classLoaderCacheB_ear, DeployOptions.SERVER_ONLY);
    }

    @Before
    public void startServer() throws Exception {
        server.startServer();
    }

    @After
    public void stopServer() throws Exception {
        server.stopServer();
    }

    @Test
    public void testClassLoaderCache() throws Exception {
        runConfigTest(WARA1, 0, 2); //initially there are zero configs, the test is expected to load two; one specific to the war and one global one
        runConfigTest(WARA2, 2, 3); //after the previous test there should be two configs, this test is expected to load one new one specific to the war and reuse the global one (total 3)
        assertTrue("EARA could not be restarted", server.restartDropinsApplication(EARA_NAME)); //restarting the app should clear out all three configs
        runConfigTest(WARA1, 0, 2); //so performing the same tests again should yeild the same results
        runConfigTest(WARA2, 2, 3);
    }

    @Test
    public void testMultiApplication() throws Exception {
        runConfigTest(WARA1, 0, 2); //initially there are zero configs, the test is expected to load two; one specific to the war and one global one
        runConfigTest(WARA2, 2, 3); //after the previous test there should be two configs, this test is expected to load one new one specific to the war and reuse the global one (total 3)
        runConfigTest(WARB1, 3, 4); //after the previous test there should be three configs, this test is expected to load one new one specific to the war and reuse the global one (total 4)
        runConfigTest(WARB2, 4, 5); //after the previous test there should be four configs, this test is expected to load one new one specific to the war and reuse the global one (total 5)
        assertTrue("EARB could not be removed", server.removeAndStopDropinsApplications(EARB_NAME)); // removing EARB should clear it's two war specific configs but leave the others (total 3)
        server.removeInstalledAppForValidation(EARB); // must do this otherwise EARB will still be expected to start if server is restarted
        runConfigTest(WARA1, 3, 3); //there should be three configs at this point; one for each war and the global one. they should all be reused so the total remains the same
        runConfigTest(WARA2, 3, 3); //there should be three configs at this point; one for each war and the global one. they should all be reused so the total remains the same
    }

    private void runConfigTest(String path, int before, int after) throws Exception {
        String pathAndQuery = path + "?" + ClassLoaderCacheTestServlet.BEFORE + "=" + before + "&" + ClassLoaderCacheTestServlet.AFTER + "=" + after;
        runTest(server, pathAndQuery, "testClassLoaderCache");
    }

}
