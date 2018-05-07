/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.tests;

import static org.junit.Assert.assertNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * This test class will use the jsf23jpa22Server to ensure that the two features can be
 * configured together.
 *
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF23JPA22Test {

    protected static final Class<?> c = JSF23JPA22Test.class;

    @Rule
    public TestName name = new TestName();

    @Server("jsf23jpa22Server")
    public static LibertyServer jsf23jpa22Server;

    @BeforeClass
    public static void setup() throws Exception {
        // Start the server and use the class name so we can find logs easily.
        jsf23jpa22Server.startServer(c.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsf23jpa22Server != null && jsf23jpa22Server.isStarted()) {
            jsf23jpa22Server.stopServer();
        }
    }

    /**
     * Test to ensure that the jsf-2.3 feature and jpa-2.2 feature can be used together.
     *
     * @throws Exception
     */
    @Test
    public void testJSF23AndJPA22Features() throws Exception {
        // CWWKE0702E: Could not resolve module:
        String msgToSearchFor = "CWWKE0702E";

        // If the features can not be loaded together we'll see one or more module resolution issues.
        // Setting timeout to 10 seconds as we don't want to wait for the default timeout.
        assertNull("The following message was found in the logs and should not have been: " + msgToSearchFor,
                   jsf23jpa22Server.waitForStringInLog(msgToSearchFor, 10 * 1000));
    }
}
