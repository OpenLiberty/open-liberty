/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.fat.duplicates;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test of duplicate configurations that try to use the same resource adapter name.
 */
public class DuplicateResourceAdaptersTest {

    private static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jca.fat.duplicates");
        server.startServer();
        server.waitForStringInLog("CWWKE0002I");
        assertNotNull("FeatureManager should report update is complete",
                      server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Server should report it has started",
                      server.waitForStringInLog("CWWKF0011I"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null)
            server.stopServer("J2CA8815E", "J2CA7002E"); // These messages are from the resource adapters having the same unique id
    }

    @Test
    public void testDuplicateResourceAdapterNames_OneShouldInstall() throws Exception {
        // Either "duplicatera" or "DuplicateRA" will start, so only scan for what is common
        server.waitForStringInLog("J2CA7001I.*uplicate.*");
    }

    @Test
    public void testDuplicateResourceAdapterNames_OneShouldNotInstall() throws Exception {
        // The other will show up as a duplicate
        if (null == server.waitForStringInLog(".*J2CA8815E.*uplicate.*"))
            throw new Exception("Did not find error for duplicate resource adapter id");
    }
}
