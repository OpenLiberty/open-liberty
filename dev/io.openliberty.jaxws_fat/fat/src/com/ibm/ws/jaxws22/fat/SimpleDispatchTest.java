/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws22.fat;

import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxws22.fat.simpleservice.SimpleDispatchTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * The purpose of this test is simple use of Holders<T> as Web Service parameters.
 * Here Holders<T> are used as both a regular parameter and as a header as annotated on the SEI:
 *
 * @WebParam(name = "Address", targetNamespace = "", mode = WebParam.Mode.INOUT) Holder<Address> address,
 * @WebParam(name = "Header", targetNamespace = "", header = true, mode = WebParam.Mode.INOUT) Holder<Header> header);
 *
 *                The tests check client instantiated Holder values against the values returned by the Web Services Implementation.
 *                For simplicities sake, the same hello.Address type value is always expected between tests as it makes for easy compares, since what
 *                we really want to test is the proper marshalling and unmarshalling of the Holder<T> types.
 *
 *                TODO: Add code in HeaderAddressHandler, then tests to check contents of Holder<Header> in the SOAPHeader itself
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class SimpleDispatchTest extends FATServletClient {

    private static final String APP_NAME = "simpleservice";

    @Server("com.ibm.ws.jaxws22.dispatch_fat")
    @TestServlet(servlet = SimpleDispatchTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    private final Class<?> thisClass = SimpleDispatchTest.class;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME, "com.ibm.ws.jaxws22.fat.simpleservice",
                                                      "fats.cxf.basic.jaxws");

        ShrinkHelper.exportDropinAppToServer(server, app);

        server.startServer("HolderServer.log");
        System.out.println("Starting Server");

        assertNotNull("Application hello does not appear to have started.", server.waitForStringInLog("CWWKZ0001I:.*" + APP_NAME));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            // Ignore a warning that the servlet for SimpleSvcClient isn't mapped when test pasess anyway
            server.stopServer("SRVE0274W");
        }
    }

}
