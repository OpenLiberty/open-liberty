/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
        assertTrue("CDI was not among the loaded features", featuresMessages.get(0).contains("cdi-"));

        assertFalse("Callback handler was not called to provide the username",
                    client.findStringsInCopiedLogs("Name callback: testUser").isEmpty());

        assertFalse("Did not get the name from the injected principal",
                    client.findStringsInCopiedLogs("Injected principal: testUser").isEmpty());

    }
}
