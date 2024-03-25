/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.fat;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;

/**
 * The purpose of this test is to work item 88030_PM76009_:
 * INVALID UTF-8 middle byte error WITH DANISH CHARACTER SET
 */
@RunWith(FATRunner.class)
public class UTF8Test extends AbstractTest {

    @Server("com.ibm.ws.jaxrs.fat.json")
    public static LibertyServer server;

    private static final String jsonwar = "json";
    private final String target = jsonwar + "/TestServlet";

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, jsonwar, "com.ibm.ws.jaxrs.fat.json");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

    @Before
    public void preTest() {
        serverRef = server;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }

    @Test
    public void testCountriesUpperCase() throws Exception {
        this.runTestOnServer(target, "testCountriesUpperCase", null, "OK");
    }

    @Test
    public void testCountriesLowerCase() throws Exception {
        this.runTestOnServer(target, "testCountriesLowerCase", null, "OK");
    }

    @Test
    public void testInvalidBody() throws Exception {
        // The 3.1 specification introduces a defaultExceptionmapper with the following:
        // "A JAX-RS implementation MUST include a default exception mapping provider that
        // implements ExceptionMapper<Throwable> and which SHOULD set the response status to 500."
        if (JakartaEEAction.isEE10OrLaterActive()) {
            this.runTestOnServer(target, "testInvalidBody", null, "500");
        } else {
            this.runTestOnServer(target, "testInvalidBody", null, "400");

        }
    }
}
