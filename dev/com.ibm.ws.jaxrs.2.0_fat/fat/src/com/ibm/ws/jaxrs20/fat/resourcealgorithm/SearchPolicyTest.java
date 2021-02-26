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
package com.ibm.ws.jaxrs20.fat.resourcealgorithm;

import static org.junit.Assert.assertNotNull;

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
public class SearchPolicyTest extends AbstractTest {

    @Server("com.ibm.ws.jaxrs.fat.searchpolicy")
    public static LibertyServer server;

    private static final String ra_war = "resourcealgorithm";
    private final String target = ra_war + "/TestServlet";

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, ra_war, "com.ibm.ws.jaxrs.fat.resourcealgorithm");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
            assertNotNull("The server did not start", server.waitForStringInLog("CWWKF0011I"));
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

    /**
     * Tests that a regular GET is okay. Technically on MyRootResource but
     * MyOtherRootResource should have the same path value.
     */
    @Test
    public void testDefaultGETIsOK() throws Exception {
        this.runTestOnServer(target, "testDefaultGETIsOK", null, "OK");
    }

    /**
     * Tests that a regular POST is okay. Technically on MyOtherRootResource but
     * MyRootResource should have the same path value.
     */
    @Test
    public void testDefaultPOSTIsOK() throws Exception {
        this.runTestOnServer(target, "testDefaultPOSTIsOK", null, "OK");
    }

    /**
     * Tests that a subresource method can be reached with the same path as a
     * subresource locator.
     */
    @Test
    public void testSubresourceMethodGET() throws Exception {
        this.runTestOnServer(target, "testSubresourceMethodGET", null, "OK");
    }

    /**
     * Tests that a subresource locator can be reached with the same path as a
     * subresource method.
     */
    @Test
    public void testSubresourceLocatorPOST() throws Exception {
        this.runTestOnServer(target, "testSubresourceLocatorPOST", null, "OK");
    }

    /**
     * Tests that a regular GET is okay. Technically on MyRootResource but
     * MyOtherRootResource should have the same path value.
     */
    @Test
    public void testNormalSearch() throws Exception {
        this.runTestOnServer(target, "testNormalSearch", null, "OK");
    }
}
