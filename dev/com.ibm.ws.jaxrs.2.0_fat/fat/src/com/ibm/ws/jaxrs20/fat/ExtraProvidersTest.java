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

import static org.junit.Assert.assertNotNull;

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

@RunWith(FATRunner.class)
public class ExtraProvidersTest extends AbstractTest {

    // Use a copy of the same server as JAXRSWebContainerTest
    @Server("com.ibm.ws.jaxrs.fat.extraproviders")
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
            server.stopServer("CWWKW0100W", "CWWKW1305W");
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
        final String prefix = (JakartaEEAction.isEE9OrLaterActive()) ? "CWWKW1305W" : "CWWKW0100W";
        assertNotNull("No warning logged for provider without a public constructor - expected " + prefix,
                      server.waitForStringInLog(prefix + ".*com.ibm.ws.jaxrs.fat.extraproviders.NoPublicConstructorProvider"));
        assertNotNull("No warning logged for provider (declared via Application classes) without a public constructor - expected " + prefix,
                      server.waitForStringInLog(prefix + ".*com.ibm.ws.jaxrs.fat.extraproviders.NoAnnotationNoPublicConstructorProvider"));
    }
}
