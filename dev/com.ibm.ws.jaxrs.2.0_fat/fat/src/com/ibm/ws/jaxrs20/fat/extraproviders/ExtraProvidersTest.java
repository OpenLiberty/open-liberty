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
package com.ibm.ws.jaxrs20.fat.extraproviders;

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
public class ExtraProvidersTest extends AbstractTest {

    // Use the same server as JAXRSWebContainerTest
    @Server("com.ibm.ws.jaxrs.fat.webcontainer")
    public static LibertyServer server;

    private static final String epwar = "extraproviders";
    private final static String target = epwar + "/TestServlet";

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, epwar, "com.ibm.ws.jaxrs.fat.extraprovider.jaxb.bean",
                                      "com.ibm.ws.jaxrs.fat.extraproviders",
                                      "com.ibm.ws.jaxrs.fat.extraproviders.jaxb",
                                      "com.ibm.ws.jaxrs.fat.extraproviders.jaxb.book",
                                      "com.ibm.ws.jaxrs.fat.extraproviders.jaxb.person");

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
            server.stopServer("CWWKW0100W");
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

    /*
     * All the rest from JAXBCollectionTest
     */
    //@Test
    public void testXMLRootWithObjectFactoryList() throws Exception {
        this.runTestOnServer(target, "testXMLRootWithObjectFactoryList", null, "OK");
    }

    // @Test
    public void testXMLRootWithObjectFactoryListResponse() throws Exception {
        this.runTestOnServer(target, "testXMLRootWithObjectFactoryListResponse", null, "OK");
    }

    @Test
    public void testXMLRootWithObjectFactoryJAXBElement() throws Exception {
        this.runTestOnServer(target, "testXMLRootWithObjectFactoryJAXBElement", null, "OK");
    }

    //@Test
    public void testXMLRootNoObjectFactoryList() throws Exception {
        this.runTestOnServer(target, "testXMLRootNoObjectFactoryList", null, "OK");
    }

    //@Test
    public void testXMLRootNoObjectFactoryArray() throws Exception {
        this.runTestOnServer(target, "testXMLRootNoObjectFactoryArray", null, "OK");
    }

    //@Test
    public void testXMLRootNoObjectFactoryListResponse() throws Exception {
        this.runTestOnServer(target, "testXMLRootNoObjectFactoryListResponse", null, "OK");
    }

    @Test
    public void testXMLRootNoObjectFactoryJAXBElement() throws Exception {
        this.runTestOnServer(target, "testXMLRootNoObjectFactoryJAXBElement", null, "OK");
    }

    /**
     * Section 4.1.2 of the spec states that a provider class must have a
     * public constructor. This test verifies that Liberty will log a
     * warning message if the customer provides a provider class that does
     * not contain a public constructor.
     */
    @Test
    public void testNoPublicConstructorProvider() {
        assertNotNull("No warning logged for provider without a public constructor - expected CWWKW0100W",
                      server.waitForStringInLog("CWWKW0100W.*com.ibm.ws.jaxrs.fat.extraproviders.NoPublicConstructorProvider"));
        assertNotNull("No warning logged for provider (declared via Application classes) without a public constructor - expected CWWKW0100W",
                      server.waitForStringInLog("CWWKW0100W.*com.ibm.ws.jaxrs.fat.extraproviders.NoAnnotationNoPublicConstructorProvider"));
    }
}
