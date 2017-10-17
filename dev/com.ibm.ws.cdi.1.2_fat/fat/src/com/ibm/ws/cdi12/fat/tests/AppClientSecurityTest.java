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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;

/**
 * Test that our integration with security works in the client container.
 */

public class AppClientSecurityTest {
    private final String testClientName = "cdiClientSecurity";
    private final LibertyClient client = LibertyClientFactory.getLibertyClient(testClientName);

    @Test
    public void testCallbackHandlerInjection() throws Exception {
        client.startClient();

        List<String> featuresMessages = client.findStringsInCopiedLogs("CWWKF0034I");
        assertFalse("Did not receive features loaded message", featuresMessages.isEmpty());
        assertTrue("cdi-1.2 was not among the loaded features", featuresMessages.get(0).contains("cdi-1.2"));

        assertFalse("Callback handler was not called to provide the username",
                    client.findStringsInCopiedLogs("Name callback: testUser").isEmpty());

        assertFalse("Did not get the name from the injected principal",
                    client.findStringsInCopiedLogs("Injected principal: testUser").isEmpty());

    }
}
