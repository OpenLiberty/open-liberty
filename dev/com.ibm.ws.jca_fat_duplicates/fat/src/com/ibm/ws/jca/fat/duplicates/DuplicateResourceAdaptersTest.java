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

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Test of duplicate configurations that try to use the same resource adapter name.
 */
@RunWith(FATRunner.class)
public class DuplicateResourceAdaptersTest {
    private static final String RAR_NAME = "DuplicateRA";
    private static final String RAR_NAME_COPY = "DuplicateRA2";

    @Server("com.ibm.ws.jca.fat.duplicates")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ResourceAdapterArchive original = ShrinkWrap.create(ResourceAdapterArchive.class, RAR_NAME + ".rar");
        ResourceAdapterArchive copy = ShrinkWrap.create(ResourceAdapterArchive.class, RAR_NAME_COPY + ".rar");

        original.addAsManifestResource(new File("test-resourceadapters/DuplicateRA/resources/META-INF/ra.xml"));
        copy.addAsManifestResource(new File("test-resourceadapters/DuplicateRA/resources/META-INF/ra.xml"));

        ShrinkHelper.exportToServer(server, "connectors", original);
        ShrinkHelper.exportToServer(server, "connectors", copy);

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
        // Note in cases where a slow test system takes a while to install a resource adapter this trace string may not
        // be logged and instead we will get "J2CA7022W: Resource adapter {} has not installed in 30.x seconds."
        server.waitForStringInLog("J2CA7001I.*uplicate.*");
    }

    @Test
    public void testDuplicateResourceAdapterNames_OneShouldNotInstall() throws Exception {
        // The other will show up as a duplicate
        assertNotNull("Server should not be able to load resource adapater with the same name",
                      server.waitForStringInLog(".*J2CA8815E.*uplicate.*"));
    }
}
