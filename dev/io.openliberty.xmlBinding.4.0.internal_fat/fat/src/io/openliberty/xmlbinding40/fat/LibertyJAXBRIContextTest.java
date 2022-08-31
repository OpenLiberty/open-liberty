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
package io.openliberty.xmlbinding40.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jaxb.web.JAXBContextTestServlet;

/**
 * This test is intended to use the JAXBContext object to marshall and unmarshall various Java types on the Liberty runtime
 * and test the ability of the runtime to handle more than one JAXB Implementation if Configured.
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11)
public class LibertyJAXBRIContextTest extends FATServletClient {

    private static final String APP_NAME = "jaxbApp";

    @Server("jaxb_fat")
    @TestServlet(servlet = JAXBContextTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "jaxb.web", "jaxb.web.dataobjects", "jaxb.web.utils");

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (JakartaEE10Action.isActive()) {
            // To-Do: get these versions of the message to print:
            // Assert.assertFalse("Expected to find message - CWWKW1404W in logs but it was not found",
            //                   server.findStringsInLogs("CWWKW1404W:*")
            //                                   .isEmpty());

            //Assert.assertFalse("Expected to find message - CWWKW1405W in logs but it was not found",
            //                   server.findStringsInLogs("CWWKW1405W:*")
            //                                  .isEmpty());
        }

        server.stopServer("CWWKW1405W", "CWWKW1404W");
    }

}