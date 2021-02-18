/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

import componenttest.topology.impl.LibertyServer;

/**
 * Test the runtime extension to function correctly
 */
public class CDI12ExtensionSPIConstructorExceptionTest extends LoggingTest {

    public static SharedServer EXTENSION_SERVER = new SharedServer("cdi12SPIConstructorExceptionExtensionServer");
    public static String INSTALL_USERBUNDLE = "cdi.spi.constructor.fail.extension";
    public static String INSTALL_USERFEATURE_JAVAX = "cdi.spi.constructor.fail.extension-1.0";
    public static String INSTALL_USERFEATURE_JAKARTA = "cdi.spi.constructor.fail.extension-3.0";
    private static LibertyServer server;

    @Override
    protected SharedServer getSharedServer() {
        return EXTENSION_SERVER;
    }

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive extensionConstructorExceptionApp = ShrinkWrap.create(WebArchive.class,
                                                                        "ExtensionConstructorExceptionApp.war").addPackage("com.ibm.ws.cdi.extension.spi.test.constructor");

        server = EXTENSION_SERVER.getLibertyServer();
        System.out.println("Intall the user feature bundle... cdi.spi.extension");
        server.installUserBundle(INSTALL_USERBUNDLE);
        server.installUserFeature(INSTALL_USERFEATURE_JAVAX);
        server.installUserFeature(INSTALL_USERFEATURE_JAKARTA);
        ShrinkHelper.exportDropinAppToServer(server, extensionConstructorExceptionApp);
        server.startServer(true);
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application ExtensionConstructorExceptionApp started");
    }

    @Test
    public void testConstructorExceptionViaExtensionSPI() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();

        verifyResponse(browser, "/ExtensionConstructorExceptionApp/",
                       new String[] { "getBeans registered bean was injected", "Could not find unregistered bean" });
    }

    @AfterClass
    public static void cleanup() throws Exception {
        final String METHOD_NAME = "cleanup";
        Log.info(CDI12ExtensionTest.class, METHOD_NAME, "Stopping the server.");
        if (server.isStarted()) {
            server.stopServer("CWOWB1010E");//The error thrown when a SPI extension constructor fails. 
        }
        Log.info(CDI12ExtensionTest.class, METHOD_NAME, "Removing cdi extension test user feature files.");
        server.uninstallUserBundle(INSTALL_USERBUNDLE);
        server.uninstallUserFeature(INSTALL_USERFEATURE_JAVAX);
        server.uninstallUserFeature(INSTALL_USERFEATURE_JAKARTA);
    }
}
