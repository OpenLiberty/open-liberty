/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.internal.tests;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * This test validates the behavior that happens when a user uses their existing user feature
 * that uses the now removed ProviderService API with the Jakarta Authorization 3.0 feature.
 * It requires that they add a tolerates for appAuthorization 3.0 if their current user feature
 * includes dependency on appAuthorization feature.
 */
@RunWith(FATRunner.class)
public class ProviderServiceRemovalTest {

    @Server("ProviderServiceTest")
    public static LibertyServer server;

    @Test
    public void testProviderServiceRemoval() throws Exception {
        // Using a user feature that has tolerates for appAuthorization-3.0 otherwise the feature just won't
        // start due the conflict between 2.1 and 3.0 of the appAuthorization-3.0 features
        server.installUserBundle("com.ibm.ws.security.authorization.jacc.testprovider_2.1");
        server.installUserFeature("jaccTestProvider-2.1");
        server.startServer();
        try {
            RemoteFile messagesLog = server.getDefaultLogFile();
            // The expected exception looks like:
            //
            // CWWKE0702E: Could not resolve module: com.ibm.ws.security.authorization.jacc.testprovider.jakarta [190]
            // Unresolved requirement: Import-Package: com.ibm.wsspi.security.authorization.jacc; version="[10.0.0,20.0.0)"
            //
            // The below validates both lines that are output.  This tells the user that the API is no longer available with
            // Jakarta Authorization 3.0 function in liberty.
            assertNotNull(server.waitForStringInLog("CWWKE0702E.*com.ibm.ws.security.authorization.jacc.testprovider.jakarta", 30000, messagesLog));
            assertNotNull(server.waitForStringInLog("Unresolved requirement: Import-Package: com.ibm.wsspi.security.authorization.jacc", 30000, messagesLog));

        } finally {
            server.stopServer("CWWKE0702E");
            server.uninstallUserBundle("com.ibm.ws.security.authorization.jacc.testprovider_2.1");
            server.uninstallUserFeature("jaccTestProvider-2.1");
        }
    }

}
