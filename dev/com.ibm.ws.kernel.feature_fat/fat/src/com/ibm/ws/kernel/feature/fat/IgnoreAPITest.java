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
package com.ibm.ws.kernel.feature.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * This test ensures that when a user intends to block/ignore an API or SPI package,
 * that user apps are actually prevented from loading classes in it.
 * Note: this bootstrap property that is used to block/ignore API packages is not
 * intended for public use. It is specifically intended for SPSS (stack product)
 * to use until a better answer can be found for the jaxrs (and feature that depend
 * on jaxrs) feature exposing unwanted APIs.
 */
public class IgnoreAPITest {

    @Test
    public void testCannotLoadBlockedAPIClass() throws Throwable {
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.ignoreAPI");
        try {
            server.startServer();
            String appOutput = server.waitForStringInLog("IgnoreAPITest. able to load blocked package. ");
            assertNotNull("No output from startup singleton - app problem?", appOutput);
            assertTrue("App was able to load blocked package", appOutput.endsWith("false"));
        } finally {
            if (server != null) {
                server.stopServer(true);
            }
        }
    }
}
