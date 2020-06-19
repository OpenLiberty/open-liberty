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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.appConfig.cdi.web.BuiltInConverterTestServlet;
import com.ibm.ws.microprofile.appConfig.cdi.web.ConfigPropertyTestServlet;
import com.ibm.ws.microprofile.appConfig.cdi.web.FieldTestServlet;
import com.ibm.ws.microprofile.appConfig.cdi.web.SimpleScopeServlet;
import com.ibm.ws.microprofile.appConfig.cdi.web.XtorTestServletNamed;
import com.ibm.ws.microprofile.appConfig.converters.test.ConvertersTestServlet;
import com.ibm.ws.microprofile.appConfig.customSources.test.CustomSourcesTestServlet;
import com.ibm.ws.microprofile.appConfig.types.test.TypesTestServlet;
import com.ibm.ws.microprofile.config.fat.repeat.RepeatConfigActions;
import com.ibm.ws.microprofile.config.fat.suite.SharedShrinkWrapApps;
import com.ibm.ws.microprofile.config11.converter.priority.web.ConverterPriorityServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class BasicConfigTests extends FATServletClient {

    public static final String SERVER_NAME = "CDIConfigServer";

    public static final String CDI_CONFIG_APP_NAME = "cdiConfig";
    public static final String CONVERTERS_APP_NAME = "converters";
    public static final String CONVERTER_PRIORITY_APP_NAME = "converterApp";
    public static final String CUSTOM_SOURCES_APP_NAME = "customSources";
    public static final String TYPES_APP_NAME = "types";

    @ClassRule
    public static RepeatTests r = RepeatConfigActions.repeatAllConfigVersionsEE8(SERVER_NAME);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = BuiltInConverterTestServlet.class, contextRoot = CDI_CONFIG_APP_NAME),
                    @TestServlet(servlet = ConfigPropertyTestServlet.class, contextRoot = CDI_CONFIG_APP_NAME),
                    @TestServlet(servlet = FieldTestServlet.class, contextRoot = CDI_CONFIG_APP_NAME),
                    @TestServlet(servlet = XtorTestServletNamed.class, contextRoot = CDI_CONFIG_APP_NAME),
                    @TestServlet(servlet = SimpleScopeServlet.class, contextRoot = CDI_CONFIG_APP_NAME),
                    @TestServlet(servlet = ConverterPriorityServlet.class, contextRoot = CONVERTER_PRIORITY_APP_NAME),
                    @TestServlet(servlet = ConvertersTestServlet.class, contextRoot = CONVERTERS_APP_NAME),
                    @TestServlet(servlet = CustomSourcesTestServlet.class, contextRoot = CUSTOM_SOURCES_APP_NAME),
                    @TestServlet(servlet = TypesTestServlet.class, contextRoot = TYPES_APP_NAME) })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive cdiConfigWar = SharedShrinkWrapApps.cdiConfigServerApps();

        WebArchive convertersWar = ShrinkWrap.create(WebArchive.class, CONVERTERS_APP_NAME + ".war")
                                             .addPackages(true, "com.ibm.ws.microprofile.appConfig.converters.test")
                                             .addAsLibrary(SharedShrinkWrapApps.getTestAppUtilsJar())
                                             .addAsManifestResource(new File("test-applications/" + CONVERTERS_APP_NAME + ".war/resources/META-INF/permissions.xml"),
                                                                    "permissions.xml")
                                             .addAsManifestResource(new File("test-applications/" + CONVERTERS_APP_NAME + ".war/resources/META-INF/microprofile-config.properties"),
                                                                    "microprofile-config.properties")
                                             .addAsManifestResource(new File("test-applications/" + CONVERTERS_APP_NAME
                                                                             + ".war/resources/META-INF/services/org.eclipse.microprofile.config.spi.Converter"),
                                                                    "services/org.eclipse.microprofile.config.spi.Converter");

        WebArchive types_war = ShrinkWrap.create(WebArchive.class, TYPES_APP_NAME + ".war")
                                         .addPackages(true, "com.ibm.ws.microprofile.appConfig.types.test")
                                         .addAsLibrary(SharedShrinkWrapApps.getTestAppUtilsJar())
                                         .addAsManifestResource(new File("test-applications/" + TYPES_APP_NAME + ".war/resources/META-INF/permissions.xml"), "permissions.xml");

        WebArchive customSourcesWar = ShrinkWrap.create(WebArchive.class, CUSTOM_SOURCES_APP_NAME + ".war")
                                                .addPackages(true, "com.ibm.ws.microprofile.appConfig.customSources.test")
                                                .addAsLibrary(SharedShrinkWrapApps.getTestAppUtilsJar())
                                                .addAsManifestResource(new File("test-applications/" + CUSTOM_SOURCES_APP_NAME + ".war/resources/META-INF/permissions.xml"),
                                                                       "permissions.xml")
                                                .addAsManifestResource(new File("test-applications/" + CUSTOM_SOURCES_APP_NAME
                                                                                + ".war/resources/META-INF/services/org.eclipse.microprofile.config.spi.ConfigSourceProvider"),
                                                                       "services/org.eclipse.microprofile.config.spi.ConfigSourceProvider")
                                                .addAsManifestResource(new File("test-applications/" + CUSTOM_SOURCES_APP_NAME
                                                                                + ".war/resources/META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource"),
                                                                       "services/org.eclipse.microprofile.config.spi.ConfigSource");

        ShrinkHelper.exportDropinAppToServer(server, customSourcesWar);
        ShrinkHelper.exportDropinAppToServer(server, types_war);
        ShrinkHelper.exportDropinAppToServer(server, cdiConfigWar);
        ShrinkHelper.exportDropinAppToServer(server, convertersWar);
        ShrinkHelper.defaultDropinApp(server, CONVERTER_PRIORITY_APP_NAME, "com.ibm.ws.microprofile.config11.converter.*");

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
