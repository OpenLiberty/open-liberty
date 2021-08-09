/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.beansxml.fat.tests;

import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE7_FULL;
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE8_FULL;
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE9;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatTests;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * See RTC defect 168494. This test checks that the cdi1.2 feature will startup on its own
 * with no errors. As CDI on its own doesn't actually do anything, the test is just a framework
 * to start the server up and check that there are no errors. There is intentionally no test code
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class EmptyCDITest {

    public static final String SERVER_NAME = "cdi12EmptyServer";

    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE9, EE8_FULL, EE7_FULL);

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void test() throws Exception {
        List<String> foundMessages = server.findStringsInLogs("Could not resolve module:");
        StringBuilder errors = new StringBuilder();
        for (String error : foundMessages) {
            errors.append(error + "\n");
        }
        assertEquals("The server should start with no errors about unresolved modules, but found:\n" + errors, 0, foundMessages.size());
    }
}
