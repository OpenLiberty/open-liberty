/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.logging.json.fat;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests if all the messages logged by Liberty (TR or JUL) contains the LogRecordContext extensions that are registered by the Liberty Kernel (e.g. ext_thread).
 * This test uses a kernel-only Liberty server, so less messages are logged in the messages.log, and thus, will be easier to check each log line.
 */
@RunWith(FATRunner.class)
public class ExtensionsMessagesLogTest {

    protected static final Class<?> c = ExtensionsMessagesLogTest.class;

    public static final String KERNEL_SERVER_NAME = "com.ibm.ws.logging.json.KernelOnlyExtServer";

    @Server(KERNEL_SERVER_NAME)
    public static LibertyServer server; // Server with no applications or features installed, just the Liberty kernel.

    @Before
    public void setUp() throws Exception {
        if (server != null && !server.isStarted()) {
            server.startServer();
        }
    }

    @After
    public void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /*
     * This test verifies if all the liberty JSON log messages contain the LogRecordContext extension (e.g. ext_thread).
     * Most of the Liberty initialization messages at start up are logged using Tr, whereas later messages are logged using JUL.
     * Hence, the LRC extensions should be included, regardless of the logging mechanism that is used to log the message.
     */
    @Test
    public void testKernelLibertyMessagesContainLRCExtensions() throws Exception {
        final String method = "testKernelLibertyMessagesContainLRCExtentions";
        List<String> lines = server.findStringsInLogs("\\{.*\"type\":\"liberty_message\".*\\}");

        // Check if the ext_thread LogRecordContext extensions exists in each log line
        for (String line : lines) {
            if (line.contains("product = ")) {
                // Skip the header JSON entry, as it will not contain LogRecordContext Extensions.
                Log.info(c, method, "Skipping Liberty Log Header Line : " + line);
                continue;
            }
            Log.info(c, method, "Liberty Log Line : " + line);
            assertTrue("LogRecordContext Extensions was not found for line : " + line, line.contains("ext_thread"));
        }
    }

}
