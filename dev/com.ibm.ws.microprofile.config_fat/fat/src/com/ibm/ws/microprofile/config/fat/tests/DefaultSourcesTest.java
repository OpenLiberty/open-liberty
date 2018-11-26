/*******************************************************************************
* Copyright (c) 2016, 2018 IBM Corporation and others.
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
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.appConfig.defaultSources.tests.DefaultSourcesTestServlet;
import com.ibm.ws.microprofile.config.fat.repeat.RepeatConfig11EE7;
import com.ibm.ws.microprofile.config.fat.repeat.RepeatConfig12EE8;
import com.ibm.ws.microprofile.config.fat.repeat.RepeatConfig14EE8;
import com.ibm.ws.microprofile.config.fat.suite.SharedShrinkWrapApps;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class DefaultSourcesTest extends FATServletClient {

    public static final String APP_NAME = "defaultSources";

    @Server("SimpleConfigSourcesServer")
    @TestServlet(servlet = DefaultSourcesTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
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

        ShrinkHelper.exportDropinAppToServer(server, defaultSources_ear);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @ClassRule
    public static RepeatTests r = RepeatTests
                    .with(new RepeatConfig11EE7("SimpleConfigSourcesServer"))
                    .andWith(new RepeatConfig12EE8("SimpleConfigSourcesServer"))
                    .andWith(new RepeatConfig14EE8("SimpleConfigSourcesServer"));

}
