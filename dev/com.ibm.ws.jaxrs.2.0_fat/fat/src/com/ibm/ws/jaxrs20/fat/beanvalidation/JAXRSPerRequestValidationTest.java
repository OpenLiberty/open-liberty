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
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@SkipForRepeat("EE9_FEATURES") // currently broken due to multiple issues
public class JAXRSPerRequestValidationTest extends AbstractTest {

    private static final String bvwar = "beanvalidation";
    private final static String target = bvwar + "/TestServlet";

    @Server("com.ibm.ws.jaxrs.fat." + bvwar)
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
    public void testThatNoValidationConstraintsAreViolatedWhenBookIdIsSet() throws Exception {
        this.runTestOnServer(target, "testThatNoValidationConstraintsAreViolatedWhenBookIdIsSet", null, "OK");
    }

    @Test
    public void testThatValidationConstraintsAreViolatedWhenBookIdIsNotSet() throws Exception {
        this.runTestOnServer(target, "testThatValidationConstraintsAreViolatedWhenBookIdIsNotSet", null, "OK");
    }

    @Test
    public void testThatValidationConstraintsAreViolatedWhenBookDoesNotExist() throws Exception {
        this.runTestOnServer(target, "testThatValidationConstraintsAreViolatedWhenBookDoesNotExist", null, "OK");
    }

    @Test
    public void testThatValidationConstraintsAreViolatedWhenBookDoesNotExistResponse() throws Exception {
        this.runTestOnServer(target, "testThatValidationConstraintsAreViolatedWhenBookDoesNotExistResponse", null, "OK");
    }

    @Test
    public void testThatValidationConstraintsAreViolatedWhenBookNameIsNotSet() throws Exception {
        this.runTestOnServer(target, "testThatValidationConstraintsAreViolatedWhenBookNameIsNotSet", null, "OK");
    }
}