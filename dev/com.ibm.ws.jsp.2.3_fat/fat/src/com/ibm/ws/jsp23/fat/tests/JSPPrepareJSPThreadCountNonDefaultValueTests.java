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
package com.ibm.ws.jsp23.fat.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * A set of tests to ensure that the prepareJSPThreadCount metatype works
 * when prepareJSPs is enabled.
 *
 * A non default value of 2 is tested.
 */
//No need to run against cdi-2.0 since these tests don't use CDI at all.
@SkipForRepeat("CDI-2.0")
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JSPPrepareJSPThreadCountNonDefaultValueTests {
    private static final String APP_NAME = "SimpleJSPApp";

    @Server("jsp23_prepareJSPThreadCountNonDefaultServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war");

        server.startServer(JSPPrepareJSPThreadCountNonDefaultValueTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test to ensure that using a non default value of 2 in the server configuration works.
     *
     * <jspEngine prepareJSPs="0" prepareJSPThreadCount="2" keepGenerated="true" scratchdir="scratchdir"/>
     *
     * @throws Exception
     */
    @Test
    public void testPrepareJSPThreadCountOfTwo() throws Exception {
        // We need to wait for the server to finish processing before looking for the files to ensure we don't hit intermittent timing issues.
        assertNotNull("The JSPs were not processed during server startup.", server
                        .waitForStringInLog("PrepareJspHelper in group \\[SimpleJSPApp\\]: All 1 jsp files have been processed."));

        // Look for the generated class and java files.
        assertTrue("The HelloWorld.class file was not found.",
                   server.fileExistsInLibertyServerRoot("scratchdir/default_node/SMF_WebContainer/SimpleJSPApp/SimpleJSPApp/_HelloWorld.class"));
        assertTrue("The HelloWorld.java file was not found.",
                   server.fileExistsInLibertyServerRoot("scratchdir/default_node/SMF_WebContainer/SimpleJSPApp/SimpleJSPApp/_HelloWorld.java"));

        // Ensure that there are two threads listed in the trace.
        assertNotNull("The JSP trace did not show that two threads were being used.",
                      server.waitForStringInTraceUsingMark("PrepareJspHelper run PrepareJspHelper: Number of threads: 2"));

        // Delete the scratchdir.
        server.deleteDirectoryFromLibertyServerRoot("scratchdir");
    }
}
