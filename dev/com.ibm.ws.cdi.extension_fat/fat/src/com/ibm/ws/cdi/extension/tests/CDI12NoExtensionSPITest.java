/*******************************************************************************
 * Copyright (c) 2014, 2023 IBM Corporation and others.
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
import com.ibm.ws.cdi.extension.apps.spinoext.SPIExtensionServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test the runtime extension to function correctly
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class CDI12NoExtensionSPITest extends FATServletClient {

    public static final String APP_NAME = "SPIExtension";
    public static final String SERVER_NAME = "cdi12SPINoExtensionServer";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = SPIExtensionServlet.class, contextRoot = APP_NAME) })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = CDIExtensionRepeatActions.defaultRepeat(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        System.out.println("Install the user feature bundles...");
        CDIExtensionRepeatActions.installUserExtension(server, CDIExtensionRepeatActions.CDI_SPI_WITH_NO_EXTENSION_BUNDLE_ID);

        WebArchive classSPIExtension = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        classSPIExtension.addPackage(SPIExtensionServlet.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, classSPIExtension, DeployOptions.SERVER_ONLY);
        server.startServer(true);
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application SPIExtension started");
    }

    @AfterClass
    public static void cleanup() throws Exception {
        try {
            final String METHOD_NAME = "cleanup";
            Log.info(CDI12ExtensionTest.class, METHOD_NAME, "Stopping the server.");
            if (server.isStarted()) {
                server.stopServer();
            }
            Log.info(CDI12ExtensionTest.class, METHOD_NAME, "Removing spi extension test user feature files.");
        } finally {
            CDIExtensionRepeatActions.uninstallUserExtension(server, CDIExtensionRepeatActions.CDI_SPI_WITH_NO_EXTENSION_BUNDLE_ID);
        }
    }
}
