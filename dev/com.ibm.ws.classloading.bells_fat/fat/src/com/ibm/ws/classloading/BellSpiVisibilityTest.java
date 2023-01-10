/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

import static com.ibm.ws.classloading.TestUtils.IS_DROPIN;
import static com.ibm.ws.classloading.TestUtils.buildAndExportBellLibrary;
import static com.ibm.ws.classloading.TestUtils.buildAndExportWebApp;
import static com.ibm.ws.classloading.TestUtils.removeSysProps;
import static com.ibm.ws.classloading.TestUtils.setSysProps;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 * Tests for {@link LibraryServiceExporter}.
 */
@SuppressWarnings("serial")
public class BellSpiVisibilityTest {
    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("bell_spi_server");

    private static final String USER_BUNDLE_NAME = "exporting.metainf.services";
    private static final String USER_FEATURE_NAME = "user.feature.meta.inf.services-1.0";

    @BeforeClass
    public static void setup() throws Throwable {
        buildAndExportBellLibrary(server, "testSpiVisible.jar", "SpiVisible", "SpiVisible$1", "SpiVisibleRESTHandlerImpl");
        buildAndExportWebApp(server, !IS_DROPIN, "SpiVisibility.war", "com.ibm.ws.classloading.bells");
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

    static final String
        // These exceptions will appear as FFDCs at some point. They may appear before or after the server
        // start message, so they may potentially appear as a new FFDC during any test. There isn't a way to
        // reliably wait for all FFDCs to be issued, so every test should include these as allowed FFDCs.
        INSTANTIATION_EXCEPTION = "java.lang.InstantiationException",
        CLASS_NOT_FOUND_EXCEPTION = "java.lang.ClassNotFoundException",
        NO_CLASSDEF_FOUND_EXCEPTION = "java.lang.NoClassDefFoundError",
        EXCEPTION = "java.lang.Exception",

        IBMSPI_CLASS_NAME = "com.ibm.wsspi.rest.handler.RESTHandler", // SPI type="ibm-spi" from restConnector-2.0
        SPI_CLASS_NAME = "com.ibm.wsspi.webcontainer",                // SPI from servlet-3.1
        LIB_CLASS_NAME = "com.ibm.ws.test.SpiVisible",
        LIB_IBMSPI_IMPL_CLASS_NAME = "com.ibm.ws.test.SpiVisibleRESTHandlerImpl",

        // BELL service classloading parameters as jvm system properties
        // TODO: Reconfigure jvm options as BELL properties
        BELL_LOAD_CLASSNAME_JVM_OPTION = "-DclassName",
        BELL_LOAD_OPERATION_JVM_OPTION = "-DloadOp";

    enum Load_Op {
        forName,
        loadClass;
    }

    static final int
        TimeOut = 3000;

    /**
     * Verify BELL SPI visibility enables for the referenced library and that BELL services can see
     * SPI packages.
     */
    @Test
    @AllowedFFDC({
        INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION
    })
    public void testSpiIsVisibleToBell() throws Exception
    {
        Map<String,String> props = new HashMap<String,String>(3){};
        props.put(BELL_LOAD_CLASSNAME_JVM_OPTION, IBMSPI_CLASS_NAME);
        props.put(BELL_LOAD_OPERATION_JVM_OPTION, Load_Op.loadClass.toString());
        try {
            setSysProps(server, props);
            server.startServer();

            assertNotNull("The server should report spi visibility is enabled for library 'testSpiVisible', but did not.",
                    server.waitForStringInLog(".*CWWKL0059I: .*testSpiVisible"));

            assertNotNull("The server should register the 'SpiVisible' service in the 'testSpiVisible' library, but did not.",
                    server.waitForStringInLog(".*CWWKL0050I: .*testSpiVisible.*SpiVisible"));

            assertNotNull("SPI should be visible to the library classloader when spi visibility is enabled, but is not",
                    server.waitForStringInLog(".*" + IBMSPI_CLASS_NAME + " is visible to the BELL library classloader"));

            assertNotNull("The server should instantiate an instance of SpiVisibilityRESTHandlerImpl when spi visibility is enabled, but did not",
                    server.waitForStringInLog(".*" + "TestUser: addingService: impl is there, SPI impl class SpiVisibilityRESTHandlerImpl"));
        } finally {
            removeSysProps(server, props);
        }
    }

    /**
     * Verify BELL services cannot see SPI packages when BELL SPI visibility is not enabled (the default.)
     */
    @Test
    @AllowedFFDC({
        INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION
    })
    public void testSpiIsNotVisibleToBell() throws Exception
    {
        Map<String,String> props = new HashMap<String,String>(3){};
        props.put(BELL_LOAD_CLASSNAME_JVM_OPTION, IBMSPI_CLASS_NAME);
        props.put(BELL_LOAD_OPERATION_JVM_OPTION, Load_Op.loadClass.toString());
        try {
            setSysProps(server, props);
            server.startServer();

            assertNull("The server should not report spi visibility is enabled for library 'testNoSpiVisible', but did.",
                    server.waitForStringInLog(".*CWWKL0059I: .*testNoSpiVisible"));

            assertNotNull("The server should register the 'SpiVisible' service in the 'testNoSpiVisible' library, but did not.",
                    server.waitForStringInLog(".*CWWKL0050I: .*testNoSpiVisible.*SpiVisible"));

            assertNotNull("IBM-SPI packages should not be visible to the library classloader, but are.",
                    server.waitForStringInLog(".*" + IBMSPI_CLASS_NAME + " is not visible to the BELL library classloader"));
        } finally {
            removeSysProps(server, props);
        }
    }

    /**
     * Verify the application cannot see SPI packages whenever the application and BELL reference
     * a common library, and the BELL is enabled for SPI visibility.
     *
     * The test servlet uses Class.forName() to load the classes from the library referenced by a BELL.
     * If the library has SPI visibility, loading a library class that uses SPI will causes the JVM to
     * set the library classloader (libCL) as the initiating class loader of SPI class. On subsequent
     * loads the libCl will immediately find the SPI class via the call to findLoadedClass().
     *
     * The test ensures the library SPI class loader created by the BELL runtime is never installed as
     * a delegate of an application class loader -- that is, the app and BELL never share the same
     * library class loader when BELL SPI visibility is enabled.
     */
    @Test
    @AllowedFFDC({
        INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION
    })
    public void testSpiIsNotVisibleToApp() throws Exception
    {
        Load_Op loadOp = Load_Op.forName;
        Map<String,String> props = new HashMap<String,String>(3){};
        props.put(BELL_LOAD_CLASSNAME_JVM_OPTION, IBMSPI_CLASS_NAME);
        props.put(BELL_LOAD_OPERATION_JVM_OPTION, loadOp.toString());
        try {
            setSysProps(server, props);
            server.startServer();

            server.waitForStringInLog(".*CWWKT0016I: Web application available.*SpiVisibility");

            // Library classes should be visible to application, regardless of BELL SPI visibility
            HttpUtils.findStringInUrl(server, "/SpiVisibility/TestServlet?loadOp=" + loadOp + "&className=" + LIB_CLASS_NAME,
                                      LIB_CLASS_NAME + " is visible to the application classloader");

            // SPI should not be visible to the application, regardless of BELL SPI visibility
            HttpUtils.findStringInUrl(server, "/SpiVisibility/TestServlet?loadOp=" + loadOp + "&className=" + IBMSPI_CLASS_NAME,
                                      IBMSPI_CLASS_NAME + " is not visible to the application classloader");

            // Extra credit: A bell service impl that uses SPI should not be visible to the application
            HttpUtils.findStringInUrl(server, "/SpiVisibility/TestServlet?loadOp=" + loadOp + "&className=" + LIB_IBMSPI_IMPL_CLASS_NAME,
                                      LIB_IBMSPI_IMPL_CLASS_NAME + " is not visible to the application classloader");
        } finally {
            removeSysProps(server, props);
        }
    }

    /**
     * Verify BELL SPI visibility does not enable for the liberty global shared library.
     */
    @Test
    @AllowedFFDC({
        INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION
    })
    public void testSpiVisibilityDisabledForGlobalLib() throws Exception
    {
        Map<String,String> props = new HashMap<String,String>(3){};
        try {
            setSysProps(server, props);
            server.startServer();

            assertNotNull("The server should disable bell spi visibility for the global shared library, but did not",
                    server.waitForStringInLog(".*CWWKL0060E: .*global "));
        } finally {
            removeSysProps(server, props);
        }
    }
}
