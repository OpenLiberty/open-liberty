/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * See RTC defect 168494. This test checks that the cdi1.2 feature will startup on its own
 * with no errors. As CDI on its own doesn't actually do anything, the test is just a framework
 * to start the server up and check that there are no errors. There is intentionally no test code
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class EmptyCDITest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12EmptyServer");

    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void test() throws Exception {
        List<String> foundMessages = SHARED_SERVER.getLibertyServer().findStringsInLogs("Could not resolve module:");
        StringBuilder errors = new StringBuilder();
        for (String error : foundMessages) {
            errors.append(error + "\n");
        }
        assertEquals("The server should start with no errors about unresolved modules, but found:\n" + errors, 0, foundMessages.size());
    }
}
