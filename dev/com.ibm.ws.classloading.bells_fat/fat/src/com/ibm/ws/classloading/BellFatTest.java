/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package com.ibm.ws.classloading;

import static com.ibm.ws.classloading.TestUtils.buildAndExportBellLibrary;
import static com.ibm.ws.classloading.TestUtils.removeSysProps;
import static com.ibm.ws.classloading.TestUtils.setSysProps;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Tests for {@link LibraryServiceExporter}.
 */
public class BellFatTest {

    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("bell_fat_server");

    private static final String USER_BUNDLE_NAME = "exporting.metainf.services";
    private static final String USER_FEATURE_NAME = "user.feature.meta.inf.services-1.0";

    @BeforeClass
    public static void setup() throws Throwable {
        buildAndExportBellLibrary(server, "testNoEntry.jar", "NoEntry");
        buildAndExportBellLibrary(server, "testOneValidEntry.jar", "OneValidEntry");
        buildAndExportBellLibrary(server, "testMultipleValidServices.jar", "MultipleValidServices1", "MultipleValidServices2");
        buildAndExportBellLibrary(server, "testMultipleImplsOfSingleService.jar", "MultipleImplsOfSingleService1", "MultipleImplsOfSingleService2");
        buildAndExportBellLibrary(server, "testInterfaceClassNotFound.jar", "InterfaceClassNotFound");
        buildAndExportBellLibrary(server, "testImplClassThrowsException.jar", "ImplClassThrowsException");
        buildAndExportBellLibrary(server, "testImplClassNotFound.jar", "ImplClassNotFound");
        buildAndExportBellLibrary(server, "testImplClassNotConstructible.jar", "ImplClassNotConstructible");
        buildAndExportBellLibrary(server, "testReadingServicesFile.jar", "MyService", "MySecondService");

        server.installUserBundle(USER_BUNDLE_NAME);
        server.installUserFeature(USER_FEATURE_NAME);
    }

    @AfterClass
    public static void tearDown() throws Throwable {
        stopServer();
        try {
            server.uninstallUserFeature(USER_FEATURE_NAME);
            server.uninstallUserBundle(USER_BUNDLE_NAME);
        } catch (final Exception e) {
            e.printStackTrace();
        }
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

    // These four exceptions will all appear as FFDCs at some point. They may be issued before or after the server start message, so they
    // could potentially appear as a new FFDC during any one of these tests. There isn't really a way to reliably wait for all of the FFDCs
    // to be issued, so every test should include these as allowed FFDCs.
    private static final String INSTANTIATION_EXCEPTION = "java.lang.InstantiationException";
    private static final String CLASS_NOT_FOUND_EXCEPTION = "java.lang.ClassNotFoundException";
    private static final String NO_CLASSDEF_FOUND_EXCEPTION = "java.lang.NoClassDefFoundError";
    private static final String EXCEPTION = "java.lang.Exception";

    /**
     * Test no META-INF/services entries — library should behave normally
     * but warn that no services were found to export.
     */
    @Test
    @AllowedFFDC({INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION} )
    @SuppressWarnings("serial")
    public void testNoEntry() throws Exception {

        Map<String,String> sysProps = new HashMap<String,String>(3){};
         try {
            setSysProps(server, sysProps);
            server.startServer();

            assertNotNull("Should warn that no services were found to export from the 'testNoEntry' library.",
                    server.waitForStringInLog(".*CWWKL0055W: .*testNoEntry"));
        } finally {
            removeSysProps(server, sysProps);
        }
    }

    /**
     * Test one valid META-INF/services entry — library should report that it exports the service.
     */
    @Test
    @AllowedFFDC({INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION} )
    public void testOneValidEntry() throws Exception {

        server.startServer();

        final String logEntry = server.waitForStringInLog(".*CWWKL0050I: .*testOneValidEntry");
        assertNotNull("Should load a service from META-INF services in the 'testOneValidEntry' library.", logEntry);
        assertLogSpecifiesJar(logEntry, "testOneValidEntry.jar");
        assertLogSpecifiesMetaInfServicesFile(logEntry, "com.ibm.ws.classloading.exporting.test.TestInterface");
        assertLogSpecifiesImplementationType(logEntry, "com.ibm.ws.test.OneValidEntry");
    }

    /**
     * Test multiple valid services — one report per service.
     */
    @Test
    @AllowedFFDC({INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION} )
    public void testMultipleValidServices() throws Exception {

        server.startServer();

        final String logEntry1 = server.waitForStringInLog(".*CWWKL0050I: .*testAllValidServices.*MultipleValidServices1");
        final String logEntry2 = server.waitForStringInLog(".*CWWKL0050I: .*testAllValidServices.*MultipleValidServices2");
        assertNotNull("Should load a service when more than one are present.", logEntry1);
        assertNotNull("Should load another service when more than one are present.", logEntry2);
    }

    /**
     * Test multiple valid services — but scoped to register only one.
     */
    @Test
    @AllowedFFDC({INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION} )
    public void testScopeValidServices() throws Exception {

        server.startServer();

        final String logEntry1 = server.waitForStringInLog(".*CWWKL0050I: .*testScopeValidServices.*MultipleValidServices2");
        final String logEntry2 = server.waitForStringInLog(".*CWWKL0050I: .*testScopeValidServices.*MultipleValidServices1", 1000);
        assertNotNull("Should load the scoped service when more than one are present.", logEntry1);
        assertNull("Should not load another service.", logEntry2);
    }

    /**
     * Test multiple implementations of a single service.
     */
    @Test
    @AllowedFFDC({INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION} )
    public void testMultipleImplsOfSingleService() throws Exception {

        server.startServer();

        final int numOfLogEntries = server.waitForMultipleStringsInLog(2, ".*CWWKL0050I: .*testMultipleImplsOfSingleService");
        assertEquals("Should load both implementations of the service.",
                2,
                numOfLogEntries);
    }

    /**
     * Test invalid service: interface class not found.
     */
    @Test
    @AllowedFFDC({INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION} )
    public void testInterfaceClassNotFound() throws Exception {

        server.startServer();

        final String logEntry = server.waitForStringInLog(".*CWWKL0052W: .*testInterfaceClassNotFound");
        assertNotNull("Should warn that the 'TestInterface3' interface does not exist.", logEntry);
        assertLogSpecifiesJar(logEntry, "testInterfaceClassNotFound.jar");
        assertLogSpecifiesMetaInfServicesFile(logEntry, "com.ibm.ws.classloading.exporting.test.TestInterface3");
        assertLogSpecifiesImplementationType(logEntry, "com.ibm.ws.test.InterfaceClassNotFound");
        assertLogIncludesException(logEntry, "java.lang.NoClassDefFoundError");
    }

    /**
     * Test invalid service: implementation class not found.
     */
    @Test
    @AllowedFFDC({INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION} )
    public void testImplClassNotFound() throws Exception {

        server.startServer();

        final String logEntry = server.waitForStringInLog(".*CWWKL0051W: .*testImplClassNotFound");
        assertNotNull("Should warn that 'NotARealClass' cannot be found.", logEntry);
        assertLogSpecifiesJar(logEntry, "testImplClassNotFound.jar");
        assertLogSpecifiesMetaInfServicesFile(logEntry, "com.ibm.ws.classloading.exporting.test.TestInterface");
        assertLogSpecifiesImplementationType(logEntry, "com.ibm.ws.test.NotARealClass");
    }

    /**
     * Test invalid service: implementation class not constructible (no-arguments constructor is present).
     */
    @Test
    @AllowedFFDC({INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION} )
    public void testImplClassNotConstructible() throws Exception {

        server.startServer();

        final String logEntry = server.waitForStringInLog(".*CWWKL0053W: .*testImplClassNotConstructible");
        assertNotNull("Should warn that 'ImplClassNotConstructible' cannot be constructed/", logEntry);
        assertLogSpecifiesJar(logEntry, "testImplClassNotConstructible.jar");
        assertLogSpecifiesMetaInfServicesFile(logEntry, "com.ibm.ws.classloading.exporting.test.TestInterface");
        assertLogSpecifiesImplementationType(logEntry, "com.ibm.ws.test.ImplClassNotConstructible");
    }

    /**
     * Test invalid service: implementation class throws error on construction.
     */
    @Test
    @AllowedFFDC({INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION} )
    public void testImplClassThrowsException() throws Exception {

        server.startServer();

        final String logEntry = server.waitForStringInLog(".*CWWKL0057W: .*testImplClassThrowsException");
        assertNotNull("Should warn that an exception occurs when creating 'ImplClassThrowsException'.", logEntry);
        assertLogSpecifiesJar(logEntry, "testImplClassThrowsException.jar");
        assertLogSpecifiesMetaInfServicesFile(logEntry, "com.ibm.ws.classloading.exporting.test.TestInterface");
        assertLogSpecifiesImplementationType(logEntry, "com.ibm.ws.test.ImplClassThrowsException");
        assertLogIncludesException(logEntry, "java.lang.Exception");
    }

    /**
     * Test that we can correctly load the services even when the lines are riddled with comments and whitespace.
     */
    @Test
    @AllowedFFDC({INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION} )
    public void testReadingMessyServicesFile() throws Exception {

        server.startServer();

        final int numberOfServicesCreated = server.waitForMultipleStringsInLog(2, ".*CWWKL0050I: .*testReadingServicesFile");
        assertEquals("Should load both 'MyService' and 'MySecondService'.",
                2,
                numberOfServicesCreated);
    }

    /**
     * Test that we don't try to load lines which begin with '#' (comments).
     */
    @Test
    @AllowedFFDC({INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION} )
    public void testCommentedOutService() throws Exception {

        server.startServer();

        assertThat("Should not try to do anything with 'NotARealService' since it is commented out.",
                server.findStringsInLogs(".*com.ibm.ws.test.NotARealService"),
                equalTo(Collections.EMPTY_LIST));
    }

    /**
     * Test that we don't try to load any lines which should be ignored.
     */
    @Test
    @AllowedFFDC({INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION} )
    public void testReadingServicesFileWithComment() throws Exception {

        server.startServer();

        assertThat("The 'testReadingServicesFile' library should not produce any warnings.",
                server.findStringsInLogs(".*W: .*testReadingServicesFile"),
                equalTo(Collections.EMPTY_LIST));
    }

    private void assertLogSpecifiesImplementationType(final String logEntry, final String implClass) {

        assertThat("Log message should specify the implementation type.",
                logEntry,
                containsString(implClass));
    }

    private void assertLogSpecifiesMetaInfServicesFile(final String logEntry, final String metaInfServicesFile) {

        assertThat("Log message should specify the name of the META-INF/services file.",
                logEntry,
                containsString(metaInfServicesFile));
    }

    private void assertLogSpecifiesJar(final String logEntry, final String jarFile) {

        assertThat("Log message should specify the jar the META-INF/services file is in.",
                logEntry,
                containsString(jarFile));
    }

    private void assertLogIncludesException(final String logEntry, final String exception) {

        assertThat("Log message should specify the jar the META-INF/services file is in.",
                logEntry,
                containsString(exception));
    }
}
