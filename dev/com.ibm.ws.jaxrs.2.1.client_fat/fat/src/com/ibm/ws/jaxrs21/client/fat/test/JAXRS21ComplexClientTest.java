/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.client.fat.test;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class JAXRS21ComplexClientTest extends JAXRS21AbstractTest {
    @Server("jaxrs21.client.JAXRS21ComplexClientTest")
    public static LibertyServer server;

    private static final String complexclientwar = "jaxrs21complexclient";

    private final static String target = "jaxrs21complexclient/JAXRS21ClientTestServlet";

    @BeforeClass
    public static void setup() throws Exception {

        ShrinkHelper.defaultDropinApp(server, complexclientwar,
                                      "com.ibm.ws.jaxrs21.client.JAXRS21ComplexClientTest.service",
                                      "com.ibm.ws.jaxrs21.client.JAXRS21ComplexClientTest.client");

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
        server.stopServer();
    }

    @Before
    public void preTest() {
        serverRef = server;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }

    /**
     * Test 14 Client Filter Test
     *
     */

    @Test
    public void testNew2WebTargetsRequestFilterForRx1() throws Exception {
        this.runTestOnServer(target, "testNew2WebTargetsRequestFilterForRx1", null, "{filter1=GET},{filter1=GET, filter2=*/*}");
    }

    @Test
    public void testNew2WebTargetsRequestFilterForRx2() throws Exception {
        this.runTestOnServer(target, "testNew2WebTargetsRequestFilterForRx2", null, "{filter1=GET},{filter1=GET, filter2=*/*}");
    }

    @Test
    public void testNew2ResponseFilterForRx() throws Exception {
        this.runTestOnServer(target, "testNew2ResponseFilterForRx", null, "222,223");
    }

    @Test
    public void testNew2MixFilterForRx() throws Exception {
        this.runTestOnServer(target, "testNew2MixFilterForRx", null, "222,{filter1=GET},223,{filter2=null}");
    }

    /**
     * Test: Test the new Reactive client by simply changing the use of async to rx
     *
     * Expected Results: Exception is thrown
     */

    @Test
    public void testThrowsExceptionForRx() throws Exception {
        this.runTestOnServer(target, "testThrowsExceptionForRx", null, true + "");
    }
}