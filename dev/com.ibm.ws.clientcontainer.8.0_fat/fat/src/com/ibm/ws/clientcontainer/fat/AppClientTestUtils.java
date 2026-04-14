/*******************************************************************************
 * Copyright 2015, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.clientcontainer.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import componenttest.topology.impl.LibertyClient;

public class AppClientTestUtils {

    public static void assertAppMessage(String msg, LibertyClient client) {
        assertNotNull("Did not find message in client logs: " + msg, client.waitForStringInCopiedLog(msg));
    }

    public static void assertNotAppMessage(String msg, LibertyClient client) throws Exception {
        assertEquals("Should NOT have found the following msg in logs: Detected \"" + msg + "\" message, but did not expect it", 0, client.findStringsInCopiedLogs(msg).size());
    }

}
