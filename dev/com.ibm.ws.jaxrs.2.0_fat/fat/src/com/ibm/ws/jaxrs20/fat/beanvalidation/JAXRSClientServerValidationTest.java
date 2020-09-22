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
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@SkipForRepeat("EE9_FEATURES") // currently broken due to multiple issues
public class JAXRSClientServerValidationTest extends AbstractTest {

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
    public void testThatPatternValidationFails() throws Exception {
        this.runTestOnServer(target, "testThatPatternValidationFails", null, "OK");
    }

    @Test
    public void testThatNotNullValidationFails() throws Exception {
        this.runTestOnServer(target, "testThatNotNullValidationFails", null, "OK");
    }

    @Test
    public void testThatNotNullValidationSkipped() throws Exception {
        this.runTestOnServer(target, "testThatNotNullValidationSkipped", null, "OK");
    }

    @Test
    public void testThatNotNullValidationNotSkipped() throws Exception {
        this.runTestOnServer(target, "testThatNotNullValidationNotSkipped", null, "OK");
    }

    @Test
    public void testThatSizeValidationFails() throws Exception {
        this.runTestOnServer(target, "testThatSizeValidationFails", null, "OK");
    }

    @Test
    public void testThatMinValidationFails() throws Exception {
        this.runTestOnServer(target, "testThatMinValidationFails", null, "OK");
    }

    // Comment these two non-beanvalidation tests because they are failed in sun jdk some times
//    @Test
    public void testThatNoValidationConstraintsAreViolated() throws Exception {
        this.runTestOnServer(target, "testThatNoValidationConstraintsAreViolated", null, "OK");
    }

//    @Test
    public void testThatNoValidationConstraintsAreViolatedWithDefaultValue() throws Exception {
        this.runTestOnServer(target, "testThatNoValidationConstraintsAreViolatedWithDefaultValue", null, "OK");
    }

    @Test
    @Mode(TestMode.QUARANTINE)
    //TODO: Investigate why CXF JAXRS client is getting response code of 415 (UnsupportMediaType) rather than 201 (Created)
    public void testThatNoValidationConstraintsAreViolatedWithBook() throws Exception {
        this.runTestOnServer(target, "testThatNoValidationConstraintsAreViolatedWithBook", null, "OK");
    }

    @Test
    @Mode(TestMode.QUARANTINE)
    //TODO: Investigate why CXF JAXRS client is getting response code of 415 (UnsupportMediaType) rather than 400 (BadRequest)
    public void testThatValidationConstraintsAreViolatedWithBook() throws Exception {
        this.runTestOnServer(target, "testThatValidationConstraintsAreViolatedWithBook", null, "OK");
    }

    @Test
    @Mode(TestMode.QUARANTINE)
    //TODO: Investigate why CXF JAXRS client is getting response code of 400 (BadRequest) rather than 415 (UnsupportMediaType)
    public void testThatValidationConstraintsAreViolatedWithBooks() throws Exception {
        this.runTestOnServer(target, "testThatValidationConstraintsAreViolatedWithBooks", null, "OK");
    }

    @Test
    public void testThatResponseValidationForOneBookFails() throws Exception {
        this.runTestOnServer(target, "testThatResponseValidationForOneBookFails", null, "OK");
    }

    @Test
    public void testThatResponseValidationForOneBookNotFails() throws Exception {
        this.runTestOnServer(target, "testThatResponseValidationForOneBookNotFails", null, "OK");
    }

    @Test
    public void testThatResponseValidationForNullBookFails() throws Exception {
        this.runTestOnServer(target, "testThatResponseValidationForNullBookFails", null, "OK");
    }

    @Test
    public void testThatResponseValidationForOneResponseBookFails() throws Exception {
        this.runTestOnServer(target, "testThatResponseValidationForOneResponseBookFails", null, "OK");
    }

    @Test
    public void testThatResponseValidationForBookPassesWhenNoConstraintsAreDefined() throws Exception {
        this.runTestOnServer(target, "testThatResponseValidationForBookPassesWhenNoConstraintsAreDefined", null, "OK");
    }

    @Test
    public void testThatResponseValidationForAllBooksFails() throws Exception {
        this.runTestOnServer(target, "testThatResponseValidationForAllBooksFails", null, "OK");
    }

    @Test
    public void testThatResponseValidationIsNotTriggeredForUnacceptableMediaType() throws Exception {
        this.runTestOnServer(target, "testThatResponseValidationIsNotTriggeredForUnacceptableMediaType", null, "OK");
    }

}