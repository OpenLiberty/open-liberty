/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Tests for {@link LibraryServiceExporter}.
 */
public class BellServicePropertiesTest {

    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("bell_service_properties_server");

    private static final String USER_BUNDLE_NAME = "exporting.metainf.services";
    private static final String USER_FEATURE_NAME = "user.feature.meta.inf.services-1.0";

    @BeforeClass
    public static void setup() throws Throwable {
        buildAndExportBellLibrary(server, "testMultipleValidServices.jar", "MultipleValidServices1", "MultipleValidServices2");
        buildAndExportBellLibrary(server, "testImplClassNotConstructible.jar", "ImplClassNotConstructible");

        server.installUserBundle(USER_BUNDLE_NAME);
        server.installUserFeature(USER_FEATURE_NAME);
    }

    /**
     * Build a Bell library and export it to the "sharedLib" directory of the target server.
     *
     * @param String targetServer The server that will contain the exported archive.
     * @param String archiveName The name of the Bell archive, including the file extension (e.g. ".jar").
     * @param String[] classNames  The short names of classes to package.
     */
    static void buildAndExportBellLibrary(LibertyServer targetServer, String archiveName, String... classNames) throws Throwable {
        JavaArchive bellArchive = ShrinkHelper.buildJavaArchive(
                archiveName,
                new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {
                    @Override public boolean include(ArchivePath ap) {
                        for (String cn : classNames)
                            if (ap.get().endsWith(cn + ".class")) return true;
                        return false;
                    }
                },
                "com.ibm.ws.test");
        ShrinkHelper.exportToServer(targetServer, "sharedLib", bellArchive);
    }

    @AfterClass
    public static void tearDown() throws Throwable {
        stopServer();
        try {
//            server.uninstallUserFeature(USER_FEATURE_NAME);
//            server.uninstallUserBundle(USER_BUNDLE_NAME);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Before
    public void beforeTest() throws Throwable {
        server.startServer();
    }

    @After
    public void afterTest() {
        stopServer();
    };

    static void stopServer() {
        if (server.isStarted()) {
            try {
                server.stopServer();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    };


    // These four exceptions will all appear as FFDCs at some point. They may be issued before
    // or after the server start message, so they could potentially appear as a new FFDC during
    // any of these tests. There isn't really a way to reliably wait for all of the FFDCs to be
    // issued, so every test should include these as allowed FFDCs.
    private static final String INSTANTIATION_EXCEPTION = "java.lang.InstantiationException";
    private static final String CLASS_NOT_FOUND_EXCEPTION = "java.lang.ClassNotFoundException";
    private static final String NO_CLASSDEF_FOUND_EXCEPTION = "java.lang.NoClassDefFoundError";
    private static final String EXCEPTION = "java.lang.Exception";

    /**
     * Verify the server collects and propagates the appropriate service properties
     * to multiple BELL services.
     */
    @Test
    @AllowedFFDC({
        INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION
    })
    public void testProperties() throws Exception {
        String logEntry = server.waitForStringInLog(".*CWWKL0050I: .*testPropertiesLib.*MultipleValidServices1");
        assertNotNull("The server did not register service testPropertiesLib/MultipleValidServices1", logEntry);

        logEntry = server.waitForStringInLog("MultipleValidServices1 has properties \\{TP_.*");
        Set<String> expectedProperties = Stream.of("TP_P0=TP_P0_VAL", "TP_P1=TP_P1_VAL", "TP_P2=TP_P2_VAL").collect(Collectors.toCollection(HashSet::new));
        assertLogSpecifiesProperties(logEntry, "MultipleValidServices1", expectedProperties);

        // Currently both services have the same properties
        logEntry = server.waitForStringInLog(".*CWWKL0050I: .*testPropertiesLib.*MultipleValidServices2");
        assertNotNull("The server failed to register service testPropertiesLib/MultipleValidServices2", logEntry);

        logEntry = server.waitForStringInLog("MultipleValidServices2 has properties \\{TP_.*");
        //expectedProperties = Stream.of("TP_P0=TP_P0_VAL", "TP_P1=TP_P1_VAL", "TP_P2=TP_P2_VAL").collect(Collectors.toCollection(HashSet::new));
        assertLogSpecifiesProperties(logEntry, "MultipleValidServices2", expectedProperties);
    }

    /**
     * Verify the server emits a warning a BELL service class lacks the constructor
     * required to propagate service properties.
     */
    @Test
    @AllowedFFDC({
        INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION
    })
    public void testMissingPropertiesCtor() throws Exception {
        String logEntry = server.waitForStringInLog(".*CWWKL0050I: .*testMissingPropertiesCtor.*testImplClassNotConstructible");
        assertNotNull("The server did not register service testMissingPropertiesCtor/testImplClassNotConstructible", logEntry);

        logEntry = server.waitForStringInLog(".*CWWKL0059W: .*ImplClassNotConstructible");
        assertNotNull("The server did not warn that the ImplClassNotConstructible class is missing a public ctor with parm type java.util.Map", logEntry);

        logEntry = server.waitForStringInLog(".*CWWKL0053W: .*ImplClassNotConstructible");
        assertNotNull("The server did not warn that the ImplClassNotConstructible failed to instantiate", logEntry);

        // ASSERT SERVICE IMPL OTHERWISE CONSTRUCTS WHEN NULLARY CTOR EXISTS
        //   ==> BELL W/ PROPS + SHARED LIB W/ IMPL CLASS THAT DECLARES ONLY NULLARY CTOR
    }

    /**
     * Verify the server emits a warning whenever a BELL service property lacks a
     * name or value.
     */
    @Test
    @AllowedFFDC({
        INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION
    })
    public void testInvalidProperties() throws Exception {
        String logEntry = server.waitForStringInLog(".*CWWKL0050I: .*testInvalidPropertiesLib.*MultipleValidServices1");
        assertNotNull("The server did not register service testInvalidPropertiesLib/MultipleValidServices1", logEntry);

        logEntry = server.waitForStringInLog(".*CWWKL0060W: .*name = \\[\\], value = \\[TIP_P0_VAL\\]");
        assertNotNull("The server did not warn about an empty service property name", logEntry);

        logEntry = server.waitForStringInLog(".*CWWKL0060W: .*name = \\[TIP_P1\\], value = \\[\\]");
        assertNotNull("The server did not warn about an empty service property value", logEntry);

        // TODO INVESTIGATE WHETHER DUPLICATE NAMES ARE POSSIBLE.
        //      IF SO, ASSERT WARING FOR INVALID/IGNORED PROP

        // TODO ASSERT INVALID PROPS DO NOT PROPAGATE TO SERVICE IMPL
    }

    /**
     * Verify the server collects and propagates the appropriate service properties
     * to multiple BELL services after the BELL is updated.
     */
    @Test
    @AllowedFFDC({
        INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION
    })
    public void testUpdatedProperties() throws Exception {
        // TODO
        assertNotNull("TBD", null);
    }

    /**
     * Verify the server resolves Liberty Variables in BELL service property names
     * and values.
     */
    @Test
    @AllowedFFDC({
        INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION
    })
    public void testPropertiesContainLibertyVars() throws Exception {
        String logEntry = server.waitForStringInLog(".*CWWKL0050I: .*testPropertiesContainLibertyVarsLib.*MultipleValidServices1");
        assertNotNull("The server did not register service testPropertiesContainLibertyVarsLib/MultipleValidServices1", logEntry);

        logEntry = server.waitForStringInLog("MultipleValidServices1 has properties \\{TPCLV_.*");
        Set<String> expectedProperties = Stream.of("TPCLV_P0=LV_0_VAL", "TPCLV_P1_LV_1_VAL=TPCLV_P1_LV_1_VAL", "TPCLV_P2=TPCLV_P2_VAL").collect(Collectors.toCollection(HashSet::new));
        assertLogSpecifiesProperties(logEntry, "MultipleValidServices1", expectedProperties);

        // Currently both services share the same properties
        logEntry = server.waitForStringInLog(".*CWWKL0050I: .*testPropertiesContainLibertyVarsLib.*MultipleValidServices2");
        assertNotNull("The server failed to register service testPropertiesContainLibertyVarsLib/MultipleValidServices2", logEntry);

        logEntry = server.waitForStringInLog("MultipleValidServices2 has properties \\{TPCLV_.*");
        //expectedProperties = Stream.of("TP_P0=TP_P0_VAL", "TP_P2=TP_P2_VAL", "TP_P3=TP_P3_VAL").collect(Collectors.toCollection(HashSet::new));
        assertLogSpecifiesProperties(logEntry, "MultipleValidServices2", expectedProperties);
    }


    private void assertLogSpecifiesProperties(final String logEntry, final String service, final Set<String> expectedProps) {
        assertNotNull("Log does not contain TestUser addingService message for " + service,
                      logEntry);
        // Service tracking msg has form "TestUser addingService: <service> has properties {<name>=<val>, <name>=<val>, ...}";
        String propsString = logEntry.substring(logEntry.indexOf("{")+1, logEntry.lastIndexOf("}"));
        String[] propsSplit = propsString.split("\\s*,\\s*");  // trim spaces on elements split over ",".
        Set<String> logEntryProps = Stream.of( propsSplit ).collect(Collectors.toCollection(HashSet::new));
        assertEquals("Log message should specify the expected service properties (order not important)",
                   expectedProps,
                   logEntryProps);
    }

}
