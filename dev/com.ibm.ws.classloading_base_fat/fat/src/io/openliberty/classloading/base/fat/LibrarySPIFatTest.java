/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package io.openliberty.classloading.base.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class LibrarySPIFatTest {

    private static final String FEATURE_NAME = "test.libraries-1.0";
    private static final String BUNDLE_NAME = "test.libraries";
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("libraryUserServer");

    @BeforeClass
    public static void setUp() throws Exception {
        // install our user feature (with the MXBeans in it)
        server.installUserBundle(BUNDLE_NAME);
        server.installUserFeature(FEATURE_NAME);

        // start the server
        server.startServer();

        // wait for some necessary events before proceeding
        assertNotNull("FeatureManager should report update is complete",
                      server.waitForStringInLog("CWWKF0008I"));

//        assertNotNull("test bundle should report that it has started",
//                      server.waitForStringInLog(Constants.BUNDLE_START_MSG));

    }

    @Test
    public void testLibraryChangeNoLibrary() throws Exception {

        //testLibraryChangeNoLibrary config states:
        //0) libraryUser with libraryRef referring to non-existent library
        //1) libraryUser referencing library with nested fileset referring to libA.jar
        //2) libraryUser referencing library with nested fileset referring to libB.jar
        //3) libraryUser referencing library with nested fileset referring to libA.jar

        //Waiting for server ready message
        assertNotNull(server.waitForStringInLog("CWWKF0011I"));
        server.copyFileToLibertyServerRoot("", "LibraryUser/LibA/server.xml");
        //wait for config update to complete:
        assertNotNull(server.waitForStringInLogUsingLastOffset("CWWKG0017I"));

        server.copyFileToLibertyServerRoot("", "LibraryUser/LibB/server.xml");
        //wait for config update to complete:
        assertNotNull(server.waitForStringInLogUsingLastOffset("CWWKG0017I"));

        server.copyFileToLibertyServerRoot("", "LibraryUser/LibA/server.xml");
        //wait for config update to complete:
        assertNotNull(server.waitForStringInLogUsingLastOffset("CWWKG0017I"));

        assertEquals(3, server.waitForMultipleStringsInLog(3, "LibraryUser01"));
    }

    @Test
    public void testLibraryChangeNoLibElement() throws Exception {

        //testLibraryChangeNoLibElement config states:
        //0) libraryUser with no libraryRef attribute and no nested library element
        //1) libraryUser referencing library with nested fileset referring to libA.jar
        //2) libraryUser referencing library with nested fileset referring to libB.jar
        //3) libraryUser referencing library with nested fileset referring to libA.jar

        //Preparing the server
        server.copyFileToLibertyServerRoot("", "LibraryUser/testB/server.xml");
        //wait for config update to complete:
        assertNotNull(server.waitForStringInLogUsingLastOffset("CWWKG0017I"));

        server.copyFileToLibertyServerRoot("", "LibraryUser/LibA/server.xml");
        //wait for config update to complete:
        assertNotNull(server.waitForStringInLogUsingLastOffset("CWWKG0017I"));
        server.copyFileToLibertyServerRoot("", "LibraryUser/LibB/server.xml");
        //wait for config update to complete:
        assertNotNull(server.waitForStringInLogUsingLastOffset("CWWKG0017I"));
        server.copyFileToLibertyServerRoot("", "LibraryUser/LibA/server.xml");
        //wait for config update to complete:
        assertNotNull(server.waitForStringInLogUsingLastOffset("CWWKG0017I"));
        assertEquals(3, server.waitForMultipleStringsInLog(3, "LibraryUser01"));
    }

    @AfterClass
    public static void tearDown() throws Exception {

        try {
            if (server != null) {
                if (server.isStarted()) {

                    server.stopServer("CWWKG0014E");
                }
                server.uninstallUserFeature(FEATURE_NAME);
                server.uninstallUserBundle(BUNDLE_NAME);
            }
        } finally {

        }
    }

}
