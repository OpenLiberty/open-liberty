/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.fat.beanvalidation;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxrs20.fat.AbstractTest;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class JAXRSValidationDisabledTest extends AbstractTest {

    private static final String bvwar = "beanvalidation";
    private final static String target = bvwar + "/TestServlet";

    @Server("com.ibm.ws.jaxrs.fat." + bvwar + "Disabled")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, bvwar, "com.ibm.ws.jaxrs.fat.beanvalidation");

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
    public void testThatNoValidationConstraintsAreViolatedWhenBookIdIsSet_Disabled() throws Exception {
        this.runTestOnServer(target, "testThatNoValidationConstraintsAreViolatedWhenBookIdIsSet", null, "OK");
    }

    @Test
    public void testThatValidationConstraintsAreViolatedWhenBookIdIsNotSet_Disabled() throws Exception {
        this.runTestOnServer(target, "testThatValidationConstraintsAreViolatedWhenBookIdIsNotSet_Disabled", null, "OK");
    }

    @Test
    public void testThatValidationConstraintsAreViolatedWhenBookDoesNotExist_Disabled() throws Exception {
        this.runTestOnServer(target, "testThatValidationConstraintsAreViolatedWhenBookDoesNotExist_Disabled", null, "OK");
    }

    @Test
    public void testThatValidationConstraintsAreViolatedWhenBookDoesNotExistResponse_Disabled() throws Exception {
        this.runTestOnServer(target, "testThatValidationConstraintsAreViolatedWhenBookDoesNotExistResponse_Disabled", null, "OK");
    }

    @Test
    public void testThatValidationConstraintsAreViolatedWhenBookNameIsNotSet_Disabled() throws Exception {
        this.runTestOnServer(target, "testThatValidationConstraintsAreViolatedWhenBookDoesNotExistResponse_Disabled", null, "OK");
    }

    @Test
    public void testThatPatternValidationFails_Disabled() throws Exception {
        this.runTestOnServer(target, "testThatPatternValidationFails_Disabled", null, "OK");
    }
}