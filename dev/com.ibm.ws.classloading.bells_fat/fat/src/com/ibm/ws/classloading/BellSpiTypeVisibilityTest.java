/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading;

import static com.ibm.ws.classloading.TestUtils.BETA_EDITION_JVM_OPTION;
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
import org.junit.Before;
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
public class BellSpiTypeVisibilityTest {

    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("bell_Spi_server");

    private static final String USER_BUNDLE_NAME = "exporting.metainf.services";
    private static final String USER_FEATURE_NAME = "user.feature.meta.inf.services-1.0";

    @BeforeClass
    public static void setup() throws Throwable {

        buildAndExportBellLibrary(server, "testSpiTypeVisible.jar", "SpiTypeVisible", "SpiTypeVisible$1", "SpiTypeVisibleRESTHandlerImpl");
        buildAndExportWebApp(server, !IS_DROPIN, "SpiTypeVisibility.war", "com.ibm.ws.classloading.bells");
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

    @Before
    public void beforeTest() throws Throwable {
        // nada
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

            IBMSPI_CLASS_NAME = "com.ibm.wsspi.rest.handler.RESTHandler",  // SPI type="ibm-spi" from restConnector-2.0
            SPI_CLASS_NAME = "com.ibm.wsspi.webcontainer",  // SPI from servlet-3.1
            LIB_CLASS_NAME = "com.ibm.ws.test.SpiTypeVisible",
            LIB_IBMSPI_IMPL_CLASS_NAME = "com.ibm.ws.test.SpiTypeVisibleRESTHandlerImpl",

            // BELL service classloading parameters as jvm options
            BELL_LOAD_CLASSNAME_JVM_OPTION = "-DclassName",
            BELL_LOAD_OPERATION_JVM_OPTION = "-DloadOp";

    enum Load_Op { forName, loadClass; }

    /**
     * Verify BELL services can see SPI packages when library spiTypeVisibility is set to "spi".
     */
    @Test
    @AllowedFFDC({INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION} )
    public void testLibSpiIsVisibleToBell() throws Exception
    {
        doTestLibSpiIsVisibleToBell(server, Load_Op.loadClass, Boolean.TRUE);  // beta-edition
        doTestLibSpiIsVisibleToBell(server, Load_Op.loadClass, Boolean.FALSE);
    }

    void doTestLibSpiIsVisibleToBell(LibertyServer server, Load_Op loadOp, Boolean runAsBetaEdition) throws Exception
    {
        stopServer();
        Map<String,String> props = new HashMap<String,String>(3){};
        props.put(BETA_EDITION_JVM_OPTION, runAsBetaEdition.toString());
        props.put(BELL_LOAD_CLASSNAME_JVM_OPTION, IBMSPI_CLASS_NAME);
        props.put(BELL_LOAD_OPERATION_JVM_OPTION, loadOp.toString());

        try {
            setSysProps(server, props);
            server.startServer();

            if (runAsBetaEdition) {
                assertNotNull("The server should report library spi visibility has been invoked in beta images.",
                              server.waitForStringInLog(".*BETA: Library SPI type visibility has been invoked by class"));

                final String logEntry1 = server.waitForStringInLog(".*CWWKL0050I: .*testSpiTypeVisible.*SpiTypeVisible");
                final String logEntry2 = server.waitForStringInLog(".*" + IBMSPI_CLASS_NAME + " is visible to the BELL library classloader");
                final String logEntry3 = server.waitForStringInLog(".*" + "TestUser: addingService: impl is there, SPI impl class SpiTypeVisibilityRESTHandlerImpl");
                // Fyi, the server runtime used Class.forName to load the BELL service impl classes,
                // SpiTypeVisible and SpiTypeVisibleRESTHandlerImpl.

                assertNotNull("The server should load the META-INF service in the 'testSpiTypeVisible' library referenced by the BELL.", logEntry1);
                assertNotNull("SPI should be visible to the BELL service when library spiTypeVisibility is 'spi'", logEntry2);
                assertNotNull("The server should instantiate a BELL service impl that implements/extends SPI when library spiTypeVisibility is 'spi'", logEntry3);
            }
            else {
                // Library SPI visibility must be disabled
                final String logEntry1 = server.waitForStringInLog(".*CWWKL0050I: .*testSpiTypeVisible.*SpiTypeVisible");
                final String logEntry2 = server.waitForStringInLog(".*" + IBMSPI_CLASS_NAME + " is visible to the BELL library classloader");

                assertNotNull("NON-BETA EDITION: The server should load the META-INF service in the 'testSpiTypeVisible' library referenced by the BELL.", logEntry1);
                assertNull("NON-BETA EDITION: IBM-SPI packages should NOT BE VISIBIBLE to the BELL service when library spiTypeVisibility is 'spi'", logEntry2);
            }
        } finally {
            stopServer();
            removeSysProps(server, props);
        }
    }

    /**
     * Verify BELL services cannot see SPI packages when library spiTypeVisibility is not set (i.e. the default.)
     */
    @Test
    @AllowedFFDC({INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION})
    public void testLibSpiIsNotVisibleToBell() throws Exception
    {
        doTestLibSpiIsNotVisibleToBell(server, Load_Op.loadClass, Boolean.TRUE);
        doTestLibSpiIsNotVisibleToBell(server, Load_Op.loadClass, Boolean.FALSE);
    }

    void doTestLibSpiIsNotVisibleToBell(LibertyServer server, Load_Op loadOp, Boolean runAsBetaEdition) throws Exception
    {
        stopServer();
        Map<String,String> props = new HashMap<String,String>(3){};
        props.put(BETA_EDITION_JVM_OPTION, runAsBetaEdition.toString());
        props.put(BELL_LOAD_CLASSNAME_JVM_OPTION, IBMSPI_CLASS_NAME);
        props.put(BELL_LOAD_OPERATION_JVM_OPTION, loadOp.toString());

        try {
            setSysProps(server, props);
            server.startServer();

            final String logEntry1 = server.waitForStringInLog(".*CWWKL0050I: .*testNoSpiTypeVisible.*SpiTypeVisible");
            final String logEntry2 = server.waitForStringInLog(".*" + IBMSPI_CLASS_NAME + " is not visible to the BELL library classloader");

            assertNotNull("The server should load the META-INF service in the 'testNoSpiTypeVisible' library referenced by the BELL.", logEntry1);
            assertNotNull("IBM-SPI packages should not be visible to the BELL service when library spiTypeVisibility is not set", logEntry2);
        } finally {
            stopServer();
            removeSysProps(server, props);
        }
    }

    /**
     * Verify the application cannot see SPI packages when spiTypeVisibility is set on a common library.
     * In this test the shared library is also referenced by a BELL.
     */
    @Test
    @AllowedFFDC({INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION} )
    public void testLibSpiIsNotVisibleToApp() throws Exception
    {
        doTestLibSpiIsNotVisibleToApp(server, Load_Op.forName, Boolean.TRUE);
        doTestLibSpiIsNotVisibleToApp(server, Load_Op.forName, Boolean.FALSE);
    }

    void doTestLibSpiIsNotVisibleToApp(LibertyServer server, Load_Op loadOp, Boolean runAsBetaEdition) throws Exception
    {
        stopServer();
        Map<String,String> props = new HashMap<String,String>(3){};
        props.put(BETA_EDITION_JVM_OPTION, runAsBetaEdition.toString());
        props.put(BELL_LOAD_CLASSNAME_JVM_OPTION, IBMSPI_CLASS_NAME);
        props.put(BELL_LOAD_OPERATION_JVM_OPTION, loadOp.toString());

        try {
            setSysProps(server, props);
            server.startServer();

            if (runAsBetaEdition) {
                // App classloader must not delegate to a common library with SPI visibility
                server.waitForStringInLog(".*CWWKL0019W: Application [SpiTypeVisibility#SpiTypeVisibility.war] is incompatible with the common class loader for library [testSpiTypeVisible]"
                                          + ", because the library is configured with SPI visibility [spi]. Library [testSpiTypeVisible] will be ignored.");
            }

            server.waitForStringInLog(".*CWWKT0016I: Web application available.*SpiTypeVisibility");

            if (runAsBetaEdition) {
                // Library classes must not be visible to the application via library delegation
                HttpUtils.findStringInUrl(server, "/SpiTypeVisibility/TestServlet?loadOp=" + loadOp + "&className=" + LIB_CLASS_NAME,
                                          LIB_CLASS_NAME + " is not visible to the application classloader");
            } else {
                HttpUtils.findStringInUrl(server, "/SpiTypeVisibility/TestServlet?loadOp=" + loadOp + "&className=" + LIB_CLASS_NAME,
                                          LIB_CLASS_NAME + " is visible to the application classloader");
            }

            // SPI must not be visible to the application via library delegation
            HttpUtils.findStringInUrl(server, "/SpiTypeVisibility/TestServlet?loadOp=" + loadOp + "&className=" + IBMSPI_CLASS_NAME,
                                      IBMSPI_CLASS_NAME + " is not visible to the application classloader");

            // Bell service impl (SPI loaded by the jvm during class definition) is not visible to the application via library delegation
            HttpUtils.findStringInUrl(server, "/SpiTypeVisibility/TestServlet?loadOp=" + loadOp + "&className=" + LIB_IBMSPI_IMPL_CLASS_NAME ,
                                      LIB_IBMSPI_IMPL_CLASS_NAME + " is not visible to the application classloader");
        } finally {
            stopServer();
            removeSysProps(server, props);
        }
    }
}
