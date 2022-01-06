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
import com.ibm.ws.cdi.extension.apps.spi.CrossWireTestServlet;
import com.ibm.ws.cdi.extension.apps.spi.MisplacedTestServlet;
import com.ibm.ws.cdi.extension.apps.spi.SPIExtensionServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test the runtime extension to function correctly
 */
@RunWith(FATRunner.class)
public class CDI12ExtensionSPITest extends FATServletClient {

    public static final String APP_NAME = "SPIExtension";
    public static final String SERVER_NAME = "cdi12SPIExtensionServer";
    public static final String INSTALL_USERBUNDLE_JAVAX = "cdi.spi.extension";
    public static final String INSTALL_USERBUNDLE_JAKARTA = "cdi.spi.extension-jakarta";
    public static final String INSTALL_MISPLACED_USERBUNDLE_JAVAX = "cdi.spi.misplaced";
    public static final String INSTALL_MISPLACED_USERBUNDLE_JAKARTA = "cdi.spi.misplaced-jakarta";
    public static final String INSTALL_USERFEATURE_JAVAX = "cdi.spi.extension-1.0";
    public static final String INSTALL_USERFEATURE_JAKARTA = "cdi.spi.extension-3.0";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = SPIExtensionServlet.class, contextRoot = APP_NAME),
                    @TestServlet(servlet = CrossWireTestServlet.class, contextRoot = APP_NAME),
                    @TestServlet(servlet = MisplacedTestServlet.class, contextRoot = APP_NAME) })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive classSPIExtension = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        classSPIExtension.addPackage(SPIExtensionServlet.class.getPackage());

        System.out.println("Intall the user feature bundle... cdi.spi.extension");
        if (RepeatTestFilter.isRepeatActionActive("EE9_FEATURES")) {
            server.installUserBundle(INSTALL_USERBUNDLE_JAKARTA);
            server.installUserBundle(INSTALL_MISPLACED_USERBUNDLE_JAKARTA);
            server.installUserFeature(INSTALL_USERFEATURE_JAKARTA);
        } else {
            server.installUserBundle(INSTALL_USERBUNDLE_JAVAX);
            server.installUserBundle(INSTALL_MISPLACED_USERBUNDLE_JAVAX);
            server.installUserFeature(INSTALL_USERFEATURE_JAVAX);
        }
        ShrinkHelper.exportDropinAppToServer(server, classSPIExtension, DeployOptions.SERVER_ONLY);
        server.startServer(true);
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application SPIExtension started");
    }

    @AfterClass
    public static void cleanup() throws Exception {
        final String METHOD_NAME = "cleanup";
        Log.info(CDI12ExtensionTest.class, METHOD_NAME, "Stopping the server.");
        if (server.isStarted()) {
            server.stopServer();
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
