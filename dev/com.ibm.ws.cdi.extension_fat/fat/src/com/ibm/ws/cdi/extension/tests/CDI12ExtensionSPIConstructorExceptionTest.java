/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;

import java.util.List;

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
import com.ibm.ws.cdi.extension.spi.test.constructor.exception.MyExtension;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
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
@Mode(TestMode.FULL)
public class CDI12ExtensionSPIConstructorExceptionTest extends FATServletClient {

    public static final String APP_NAME = "ExtensionConstructorExceptionApp";
    public static final String SERVER_NAME = "cdi12SPIConstructorExceptionExtensionServer";

    @Server(SERVER_NAME)
    @TestServlet(servlet = ConstructorExceptionServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = CDIExtensionRepeatActions.repeat(SERVER_NAME,
                                                                   CDIExtensionRepeatActions.EE10_PLUS,
                                                                   CDIExtensionRepeatActions.EE9_PLUS,
                                                                   CDIExtensionRepeatActions.EE7_PLUS);

    @BeforeClass
    public static void setUp() throws Exception {

        CDIExtensionRepeatActions.installUserExtension(server, CDIExtensionRepeatActions.SPI_XTOR_FAIL_EXTENSION_BUNDLE_ID);

        WebArchive extensionConstructorExceptionApp = ShrinkWrap.create(WebArchive.class,
                                                                        APP_NAME + ".war");
        extensionConstructorExceptionApp.addPackage(ConstructorExceptionServlet.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, extensionConstructorExceptionApp, DeployOptions.SERVER_ONLY);
        server.startServer(true);

        // SPI extension exception should not prevent an application starting
        assertNotNull(server.waitForStringInLogUsingMark("CWWKZ0001I.*Application " + APP_NAME + " started"));

        // Check we have the right exception messages
        List<String> errors = server.findStringsInLogsUsingMark("CWOWB1012E", server.getDefaultLogFile());

        // Check that the error message includes:
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0), allOf(containsString(CDIExtensionRepeatActions.SPI_XTOR_FAIL_EXTENSION_BUNDLE_ID), // The bundle containing the offending class
                                        containsString(MyExtension.class.getName()) // The offending class name
        ));
    }

    @AfterClass
    public static void cleanup() throws Exception {
        final String METHOD_NAME = "cleanup";
        Log.info(CDI12ExtensionTest.class, METHOD_NAME, "Stopping the server.");
        server.stopServer("CWOWB1010E");//The error thrown when a SPI extension constructor fails.
        Log.info(CDI12ExtensionTest.class, METHOD_NAME, "Removing cdi extension test user feature files.");
        CDIExtensionRepeatActions.uninstallUserExtension(server, CDIExtensionRepeatActions.SPI_XTOR_FAIL_EXTENSION_BUNDLE_ID);
    }
}
