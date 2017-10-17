/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.fat.tests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * See RTC defect 168494. This test checks that the cdi1.2 feature will startup on its own
 * with no errors. As CDI on its own doesn't actually do anything, the test is just a framework
 * to start the server up and check that there are no errors. There is intentionally no test code
 */
@Mode(TestMode.FULL)
public class EmptyCDITest extends LoggingTest {

    @ClassRule
    public static SharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12EmptyServer");

    @Override
    protected SharedServer getSharedServer() {
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
