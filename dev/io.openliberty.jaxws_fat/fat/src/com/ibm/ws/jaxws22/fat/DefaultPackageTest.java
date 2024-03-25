/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jaxws22.fat;

import static org.junit.Assert.assertNotNull;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import fats.cxf.basic.jaxws.DefaultPackageTestServlet;

/**
 *
 * Migrated from tWAS based com.ibm.ws.jaxws_fat jaxws/defaultpackage/** bucket
 *
 * These are the original test comments and are still applicable to this Liberty migrated test class:
 *
 * @author jtnguyen
 *         This test component ensures the Jaxws22 3.6 Conformance "Overriding JAXB types empty namespace: JAX-WS tools and runtimes MUST override
 *         the default empty namespace for JAXB types and elements to SEI's targetNamespace."
 *
 *         What in the package:
 *         - Service is of type document/literal BARE
 *         - WSDL doesn't have elementFormdefault="qualified" to ensure that it will generate empty targetNamespace for operation in JAXB artifacts
 *         - SEI has targetNamespace not null
 *         - WSDL is packaged in the application. No generated artifacts are in the EAR.
 *
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class DefaultPackageTest extends FATServletClient {

    private static final String APP_NAME1 = "defaultpackage";

    private static Class<DefaultPackageTest> thisClass = DefaultPackageTest.class;

    @Server("com.ibm.ws.jaxws22.defaultpackage_fat")
    @TestServlet(servlet = DefaultPackageTestServlet.class, contextRoot = APP_NAME1)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive app1 = ShrinkHelper.buildDefaultApp(APP_NAME1, "fats.cxf.basic.jaxws", "com.ibm.ws.jaxws22.fat.simpleservice");

        ShrinkHelper.exportDropinAppToServer(server, app1);

        server.startServer();
        System.out.println("Starting Server");

        assertNotNull("Application hello does not appear to have started.", server.waitForStringInLog("CWWKZ0001I:.*" + APP_NAME1));

        return;

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}
