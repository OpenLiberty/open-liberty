/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertNull;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;

/**
 *  Misc Test Class
 */
@RunWith(FATRunner.class)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES) 
@Mode(TestMode.FULL)
public class WCServerMiscTest {

    private static final Logger LOG = Logger.getLogger(WCServerMiscTest.class.getName());

    @Server("servlet40_monitor")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer(WCServerMiscTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");

        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test for "SRVE8501E: The servlet container did not load with an acceptable version." 
     * Find more at OLGH #17950
     * Occurs when com.ibm.ws.webcontianer is still activating but getServletContainerSpecLevel() call occurs in addGlobalListener
       
     * @throws Exception
     */
    @Test
    public void ensureServletSpecLevelIsLoaded() throws Exception {
       assertNull("Log should not contain SRVE8501E: The servlet container did not load with an acceptable version.", server.verifyStringNotInLogUsingMark("SRVE8501E.*", 1000));
    }

}