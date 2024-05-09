/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.feature.tests;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class VersionlessEnvVarErrorTest {

    public static final String SERVER_NAME = "ee7toMP";

    @Test
    public void testNoEnvVar() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer(SERVER_NAME);

        server.startServer();
        try {
            String errorMsg = server.waitForStringInLog("CWWKF0048E");
            assertNotNull("Expected error message for no env var", errorMsg);

        } finally {
            server.stopServerAlways("CWWKF0001E", "CWWKF0048E");
        }
    }
    @Test
    public void testNoFeatureInEnvVar() throws Exception {
        String envVar = "MicroProfile-6.0, MicroProfile-5.0, MicroProfile-4.0, MicroProfile-3.0, MicroProfile-2.0, MicroProfile-1.2";

        LibertyServer server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        server.addEnvVar("PREFERRED_PLATFORM_VERSIONS", envVar);

        server.startServer();
        try {
            String errorMsg = server.waitForStringInLog("CWWKF0049E");
            assertNotNull("Expected error message for no mpMetrics in env var", errorMsg);
        } finally {
            server.stopServerAlways("CWWKF0001E", "CWWKF0049E");
        }
    }
}
