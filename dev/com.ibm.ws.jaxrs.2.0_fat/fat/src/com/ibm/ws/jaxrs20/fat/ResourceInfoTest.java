/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
import componenttest.topology.impl.LibertyServer;

/**
 * Tests {@code ResourceInfo} methods {@code getResourceClass} and {@code getResourceMethod}.
 */
@RunWith(FATRunner.class)
public class ResourceInfoTest extends AbstractTest {

    @Server("com.ibm.ws.jaxrs.fat.resourceinfo")
    public static LibertyServer server;

    private static final String war = "resourceinfo";
    private final String target = war + "/TestServlet";

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, war, "com.ibm.ws.jaxrs.fat.resourceinfo");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            e.printStackTrace();
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
    public void testSimpleResource() throws Exception {
        this.runTestOnServer(target, "testSimpleResource", null, "OK");
    }

    @Test
    public void testAbstractAndSubClassResource() throws Exception {
        this.runTestOnServer(target, "testAbstractAndSubClassResource", null, "OK");
    }

    @Test
    public void testInterfaceAndImplClassResource() throws Exception {
        this.runTestOnServer(target, "testInterfaceAndImplClassResource", null, "OK");
    }

    @Test
    public void testConcreteSuperAndConcreteSubClassResource() throws Exception {
        this.runTestOnServer(target, "testConcreteSuperAndConcreteSubClassResource", null, "OK");
    }
}