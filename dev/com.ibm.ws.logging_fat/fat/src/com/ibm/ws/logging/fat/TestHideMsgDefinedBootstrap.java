/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class TestHideMsgDefinedBootstrap {

    private static LibertyServer msgServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.logging.hidemsg.bootstrap");
    private static final Class<?> logClass = TestHideMsgDefinedBootstrap.class;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void prepareTest() throws Exception {
        msgServer.startServer();
    }

    @Test
    public void testHiddenMsgIds() throws Exception {
        assertTrue("Hidden Message CWWKZ0058I should not be seen in messages.log",
                   msgServer.findStringsInLogs("CWWKZ0058I:", msgServer.getMatchingLogFile("messages.log")).isEmpty());
        assertTrue("Hidden Message CWWKZ0058I should not be seen in console.log", msgServer.findStringsInLogs("CWWKZ0058I:", msgServer.getMatchingLogFile("console.log")).isEmpty());
        assertFalse("Hidden Message CWWKZ0058I should be seen in trace", msgServer.findStringsInTrace("CWWKZ0058I:").isEmpty());
    }

    @Test
    public void testSuppressedIdsInMsgHeader() throws Exception {
        assertFalse("Suppressed Message Ids logged in header ",
                    msgServer.findStringsInLogs("Suppressed message ids:", msgServer.getMatchingLogFile("messages.log")).isEmpty());

    }

    @AfterClass
    public static void completeTest() throws Exception {
        msgServer.stopServer();
    }

}
