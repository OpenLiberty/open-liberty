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
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxws.test.soapenv.prefix.SoapEnvelopePrefixTestServlet;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Tests the SOAP Envelope default namespace via a FATServlet.
 * See SoapEnvelopePrefixTestServlet.java for test description
 * Test reuses LoggingServer and the WSR stub classes, Hello Server endpoint.
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11)
public class SoapEnvelopePrefixTest extends FATServletClient {

    private static final String APP_NAME = "soapEnvelopePrefix";

    // Reuse LoggingServer
    @Server("LoggingServer")
    @TestServlet(servlet = SoapEnvelopePrefixTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME, "com.ibm.ws.jaxws.test.wsr.server",
                                                      "com.ibm.ws.jaxws.test.wsr.server.impl",
                                                      "com.ibm.ws.jaxws.test.wsr.server.stub", "com.ibm.ws.jaxws.test.soapenv.prefix");

        ShrinkHelper.exportDropinAppToServer(server, app);

        server.startServer("LoggingServer.log");
        System.out.println("Starting Server");

        assertNotNull("Application hello does not appear to have started.", server.waitForStringInLog("CWWKZ0001I:.*" + APP_NAME));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKW0056W");
        }
    }

}
