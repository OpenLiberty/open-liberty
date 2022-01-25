/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.extension.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.cdi.extension.apps.xtorException.ConstructorExceptionServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test the runtime extension to function correctly
 */
@RunWith(FATRunner.class)
public class CDI12ExtensionSPIConstructorExceptionTest extends FATServletClient {

    public static final String APP_NAME = "ExtensionConstructorExceptionApp";
    public static final String SERVER_NAME = "cdi12SPIConstructorExceptionExtensionServer";

    @Server(SERVER_NAME)
    @TestServlet(servlet = ConstructorExceptionServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = CDIExtensionRepeatActions.repeat(SERVER_NAME, CDIExtensionRepeatActions.EE7_PLUS, CDIExtensionRepeatActions.EE9_PLUS,
                                                                   CDIExtensionRepeatActions.EE10_PLUS);

    @BeforeClass
    public static void setUp() throws Exception {

        CDIExtensionRepeatActions.installUserExtension(server, CDIExtensionRepeatActions.SPI_XTOR_FAIL_EXTENSION_BUNDLE_ID);

        WebArchive extensionConstructorExceptionApp = ShrinkWrap.create(WebArchive.class,
                                                                        APP_NAME + ".war");
        extensionConstructorExceptionApp.addPackage(ConstructorExceptionServlet.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, extensionConstructorExceptionApp, DeployOptions.SERVER_ONLY);
        server.startServer(true);
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application " + APP_NAME + " started");
    }

    @AfterClass
    public static void cleanup() throws Exception {
        final String METHOD_NAME = "cleanup";
        Log.info(CDI12ExtensionTest.class, METHOD_NAME, "Stopping the server.");
        if (server.isStarted()) {
            server.stopServer("CWOWB1010E");//The error thrown when a SPI extension constructor fails.
        }
        Log.info(CDI12ExtensionTest.class, METHOD_NAME, "Removing cdi extension test user feature files.");
        CDIExtensionRepeatActions.uninstallUserExtension(server, CDIExtensionRepeatActions.SPI_XTOR_FAIL_EXTENSION_BUNDLE_ID);
    }
}
