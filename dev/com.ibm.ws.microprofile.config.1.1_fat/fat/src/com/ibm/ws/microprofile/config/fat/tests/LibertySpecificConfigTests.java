/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.appConfig.cdi.libertyTests.LibertyBuiltInConverterTestServlet;
import com.ibm.ws.microprofile.appConfig.cdi.libertyTests.LibertyFieldTestServlet;
import com.ibm.ws.microprofile.appConfig.classLoaders.test.libertyTests.LibertyClassLoadersTestServlet;
import com.ibm.ws.microprofile.appConfig.converters.test.libertyTests.LibertyConvertersTestServlet;
import com.ibm.ws.microprofile.appConfig.defaultSources.tests.libertyTests.LibertyDefaultSourcesTestServlet;
import com.ibm.ws.microprofile.config.fat.suite.SharedShrinkWrapApps;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * These tests test Config functionality specific to Liberty's implementation (mpConfig 1.x) of MicroProfile Config specification.
 *
 * For mpConfig < 2.0, Open Liberty implemented some extra pieces of functionality beyond the MicroProfile Config specification.
 *
 * In addition, some tests check for error messages defined by Liberty, and others check functionality which changed between mpConfig 1.4 -> 2.0.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class LibertySpecificConfigTests extends FATServletClient {

    public static final String SERVER_NAME = "CDILibertyConfigServer";

    public static final String CDI_CONFIG_APP_NAME = "cdiConfig";
    public static final String CONVERTERS_APP_NAME = "converters";
    public static final String CLASS_LOADER_APP_NAME = "classLoaders";
    public static final String DEFAULT_SOURCES_APP_NAME = "defaultSources";

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat("CDILibertyConfigServer", MicroProfileActions.MP12, MicroProfileActions.MP33); // Don't repeat for mpConfig > 1.4

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = LibertyFieldTestServlet.class, contextRoot = CDI_CONFIG_APP_NAME),
                    @TestServlet(servlet = LibertyBuiltInConverterTestServlet.class, contextRoot = CDI_CONFIG_APP_NAME),
                    @TestServlet(servlet = LibertyConvertersTestServlet.class, contextRoot = CONVERTERS_APP_NAME),
                    @TestServlet(servlet = LibertyClassLoadersTestServlet.class, contextRoot = CLASS_LOADER_APP_NAME),
                    @TestServlet(servlet = LibertyDefaultSourcesTestServlet.class, contextRoot = DEFAULT_SOURCES_APP_NAME)

    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive cdiConfigWar = SharedShrinkWrapApps.cdiConfigServerApps()
                                                      .addPackages(true, "com.ibm.ws.microprofile.appConfig.cdi.libertyTests");

        WebArchive convertersWar = ShrinkWrap.create(WebArchive.class, CONVERTERS_APP_NAME + ".war")
                                             .addPackages(true, "com.ibm.ws.microprofile.appConfig.converters.test")
                                             .addAsLibrary(SharedShrinkWrapApps.getTestAppUtilsJar())
                                             .addAsManifestResource(new File("test-applications/" + CONVERTERS_APP_NAME
                                                                             + ".war/resources/META-INF/services/org.eclipse.microprofile.config.spi.Converter"),
                                                                    "services/org.eclipse.microprofile.config.spi.Converter")
                                             .addAsManifestResource(new File("test-applications/" + CONVERTERS_APP_NAME + ".war/resources/META-INF/permissions.xml"),
                                                                    "permissions.xml");

        WebArchive classLoadersWar = ShrinkWrap.create(WebArchive.class, CLASS_LOADER_APP_NAME + ".war")
                                               .addPackages(true, "com.ibm.ws.microprofile.appConfig.classLoaders.test")
                                               .addAsLibrary(SharedShrinkWrapApps.getTestAppUtilsJar())
                                               .add(new FileAsset(new File("test-applications/" + CLASS_LOADER_APP_NAME
                                                                           + ".war/resources/CUSTOM-DIR/META-INF/microprofile-config.properties")),
                                                    "/CUSTOM-DIR/META-INF/microprofile-config.properties")
                                               .addAsManifestResource(new File("test-applications/" + CLASS_LOADER_APP_NAME + ".war/resources/META-INF/permissions.xml"),
                                                                      "permissions.xml");

        WebArchive defaultSourcesWar = ShrinkWrap.create(WebArchive.class, DEFAULT_SOURCES_APP_NAME + ".war")
                                                 .addPackages(true, "com.ibm.ws.microprofile.appConfig.defaultSources.tests")
                                                 .addAsLibrary(SharedShrinkWrapApps.getTestAppUtilsJar())
                                                 .addAsManifestResource(new File("test-applications/" + DEFAULT_SOURCES_APP_NAME + ".war/resources/META-INF/permissions.xml"),
                                                                        "permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, cdiConfigWar, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, convertersWar, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, classLoadersWar, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, defaultSourcesWar, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKE0912W", "CWWKE0921W");
        // CWWKE0912W, CWWKE0921W: Ignore Java 2 Security policy warning about "com.ibm.oti.shared.SharedClassPermission"
        // We expect this access control exception to occur but to be safely caught and handled, so we just need to ignore the warning generated by our FAT infrastructure when running locally.
    }

}
