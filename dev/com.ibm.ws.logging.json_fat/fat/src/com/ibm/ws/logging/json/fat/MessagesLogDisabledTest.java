/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.json.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Disable messages.log from being generated via bootstrap.properties
 */

@RunWith(FATRunner.class)
public class MessagesLogDisabledTest {

    protected static final Class<?> c = MessagesLogDisabledTest.class;

    @Server("com.ibm.ws.logging.json.MessagesLogDisabled")
    public static LibertyServer server;

    @BeforeClass
    public static void setUpClass() throws Exception {
        server.startServer();
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        assertNotNull("Server has not started.", server.waitForStringInLog("CWWKF0011I", consoleLogFile));
    }

    @Test
    public void testDisabledMessagesLog() throws Exception {
        assertFalse("The messages.log was created and was not disabled.", server.defaultLogFileExists());
    }

    @AfterClass
    public static void tearDownClass() {
        if ((server != null) && (server.isStarted())) {
            try {
                server.stopServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
