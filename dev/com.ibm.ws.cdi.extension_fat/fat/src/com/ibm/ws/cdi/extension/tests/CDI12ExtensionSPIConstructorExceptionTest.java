/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
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
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.cdi.extension.apps.xtorException.ConstructorExceptionServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test the runtime extension to function correctly
 */
@RunWith(FATRunner.class)
public class CDI12ExtensionSPIConstructorExceptionTest extends FATServletClient {

    public static final String APP_NAME = "ExtensionConstructorExceptionApp";
    public static final String SERVER_NAME = "cdi12SPIConstructorExceptionExtensionServer";
    public static final String INSTALL_USERBUNDLE_JAVAX = "cdi.spi.constructor.fail.extension";
    public static final String INSTALL_USERBUNDLE_JAKARTA = "cdi.spi.constructor.fail.extension-jakarta";
    public static final String INSTALL_USERFEATURE_JAVAX = "cdi.spi.constructor.fail.extension-1.0";
    public static final String INSTALL_USERFEATURE_JAKARTA = "cdi.spi.constructor.fail.extension-3.0";

    @Server(SERVER_NAME)
    @TestServlet(servlet = ConstructorExceptionServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive extensionConstructorExceptionApp = ShrinkWrap.create(WebArchive.class,
                                                                        APP_NAME + ".war");
        extensionConstructorExceptionApp.addPackage(ConstructorExceptionServlet.class.getPackage());

        System.out.println("Install the user feature bundle... cdi.spi.extension");
        if (RepeatTestFilter.isRepeatActionActive("EE9_FEATURES")) {
            server.installUserBundle(INSTALL_USERBUNDLE_JAKARTA);
            server.installUserFeature(INSTALL_USERFEATURE_JAKARTA);
        } else {
            server.installUserBundle(INSTALL_USERBUNDLE_JAVAX);
            server.installUserFeature(INSTALL_USERFEATURE_JAVAX);
        }
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
        if (RepeatTestFilter.isRepeatActionActive("EE9_FEATURES")) {
            server.uninstallUserBundle(INSTALL_USERBUNDLE_JAKARTA);
            server.uninstallUserFeature(INSTALL_USERFEATURE_JAKARTA);
        } else {
            server.uninstallUserBundle(INSTALL_USERBUNDLE_JAVAX);
            server.uninstallUserFeature(INSTALL_USERFEATURE_JAVAX);
        }
    }
}
