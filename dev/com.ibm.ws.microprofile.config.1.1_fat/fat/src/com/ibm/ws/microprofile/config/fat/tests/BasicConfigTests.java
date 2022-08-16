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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.LocalFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.appConfig.cdi.web.BuiltInConverterTestServlet;
import com.ibm.ws.microprofile.appConfig.cdi.web.ConfigPropertyTestServlet;
import com.ibm.ws.microprofile.appConfig.cdi.web.FieldTestServlet;
import com.ibm.ws.microprofile.appConfig.cdi.web.SimpleScopeServlet;
import com.ibm.ws.microprofile.appConfig.cdi.web.XtorTestServletNamed;
import com.ibm.ws.microprofile.appConfig.converters.test.ConvertersTestServlet;
import com.ibm.ws.microprofile.appConfig.customSources.test.CustomSourcesTestServlet;
import com.ibm.ws.microprofile.appConfig.types.test.TypesTestServlet;
import com.ibm.ws.microprofile.config.fat.suite.SharedShrinkWrapApps;
import com.ibm.ws.microprofile.config11.converter.priority.web.ConverterPriorityServlet;

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

    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME, MicroProfileActions.MP50, MicroProfileActions.MP12, MicroProfileActions.MP13, MicroProfileActions.MP14,
                                                             MicroProfileActions.MP33,
                                                             MicroProfileActions.MP41);

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
                    @TestServlet(servlet = TypesTestServlet.class, contextRoot = TYPES_APP_NAME)
    })
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

        ShrinkHelper.exportDropinAppToServer(server, customSourcesWar, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, types_war, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, cdiConfigWar, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, convertersWar, DeployOptions.SERVER_ONLY);
        DeployOptions[] options = { DeployOptions.SERVER_ONLY };
        ShrinkHelper.defaultDropinApp(server, CONVERTER_PRIORITY_APP_NAME, options, "com.ibm.ws.microprofile.config11.converter.*");

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Verify that the MP Config introspection is included in a server dump and includes expected config vars
     */
    @Test
    @Mode(TestMode.FULL)
    public void introspectionTest() throws Exception {
        LocalFile file = server.dumpServer("introspectionTestDump");
        Map<String, String> introspectionParts;
        try (ZipFile zip = new ZipFile(file.getAbsolutePath())) {
            Optional<? extends ZipEntry> entry = zip.stream().filter(e -> e.getName().matches(".*MicroProfileConfig\\.txt")).findFirst();
            assertTrue("MP Config introspection missing from dump", entry.isPresent());
            introspectionParts = readIntrospectionSections(zip.getInputStream(entry.get()));
        }

        for (Entry<String, String> entry : introspectionParts.entrySet()) {
            assertThat(entry.getValue(), containsString("introspectorTest = env")); // from server.env
            assertThat(entry.getValue(), containsString("introspectorTest = sysprops")); // from bootstrap.properties

            // Test removal of sensitive values
            assertThat(entry.getValue(), containsString("introspectorTestPassword = *****"));
            assertThat(entry.getValue(), containsString("introspectorTestApiKey = *****"));
            assertThat(entry.getValue(), containsString("introspectorTestEncoded = *****"));
            assertThat(entry.getValue(), containsString("INTROSPECTOR_TEST_PASS = *****"));
            assertThat(entry.getValue(), not(containsString("myTestSecret")));

            if (entry.getKey().equals(CONVERTERS_APP_NAME)) {
                assertThat(entry.getValue(), containsString("introspectorTest = appprops")); // from microprofile-config.properties in converters app
            } else {
                assertThat(entry.getValue(), not(containsString("introspectorTest = appprops"))); // not present in other apps
            }
        }
    }

    /**
     * Read the introspection and split it into sections for each app
     *
     * @param in InputStream for introspection file
     * @return map from app name to corresponding section of the introspection
     */
    private static Map<String, String> readIntrospectionSections(InputStream in) throws IOException {
        Map<String, String> result = new HashMap<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line = reader.readLine();
        StringBuilder currentBuilder = new StringBuilder();
        String currentApp = null;
        Pattern appStartPattern = Pattern.compile("Config for (.+)");
        while (line != null) {
            Matcher m = appStartPattern.matcher(line);
            if (m.matches()) {
                if (currentApp != null) {
                    result.put(currentApp, currentBuilder.toString());
                }
                currentApp = m.group(1);
                currentBuilder = new StringBuilder();
            }
            currentBuilder.append(line);
            currentBuilder.append("\n");
            line = reader.readLine();
        }
        if (currentApp != null) {
            result.put(currentApp, currentBuilder.toString());
        }
        return result;
    }

}
