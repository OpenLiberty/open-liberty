/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.fat;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;

public class JsonbAppClientTest {
    private final String testClientName = "com.ibm.ws.clientcontainer.jsonb.fat.client";
    private final LibertyClient client = LibertyClientFactory.getLibertyClient(testClientName);

    @Rule
    public TestName name = new TestName();

    private void assertClientAppMessage(String msg) {
        assertNotNull("FAIL: Did not receive" + msg + " message", client.waitForStringInCopiedLog(msg));          
    }

	@Test
	public void testJSONBAppClient() throws Exception {
        ShrinkHelper.exportToClient(client, "apps", FATSuite.jsonbAppClientApp);

        client.startClient();

        String found;

        // Check JSON-B provider
        assertNotNull(found = client.waitForStringInCopiedLog("Jsonb implementation is"));
        assertTrue(found, found.contains("org.eclipse.yasson."));

        // Convert Java Object to JSON
        assertNotNull(found = client.waitForStringInCopiedLog("Location written to JSON as"));
        assertTrue(found, found.contains("\"city\""));
        assertTrue(found, found.contains("\"Rochester\""));
        assertTrue(found, found.contains("\"county\""));
        assertTrue(found, found.contains("\"Olmsted\""));
        assertTrue(found, found.contains("\"state\""));
        assertTrue(found, found.contains("\"Minnesota\""));
        assertTrue(found, found.contains("\"zip\""));
        assertTrue(found, found.contains("55902"));

        // Unmarshall from JSON
        assertNotNull(found = client.waitForStringInCopiedLog("Forecast for "));
        assertTrue(found, found.contains("2018-01-16 in 55901"));

        assertNotNull(found = client.waitForStringInCopiedLog("Chance of snow at 1am "));
        assertTrue(found, found.contains("is 29") || found.contains("is 28")); // in case of minor fluctuation due to precision

        assertNotNull(found = client.waitForStringInCopiedLog("Expected wind chill at 10am "));
        assertTrue(found, found.contains("is -13"));

        assertNotNull(found = client.waitForStringInCopiedLog("Expected temperature at 10pm "));
        assertTrue(found, found.contains("is -4"));

        assertNotNull(found = client.waitForStringInCopiedLog("Thursday's forecast in JSON "));
        assertTrue(found, found.contains("\"Thursday\""));
        assertTrue(found, found.contains("55902"));
        assertTrue(found, found.contains("22"));

        assertClientAppMessage("JSON-B Application Client Completed.");
	}
}
