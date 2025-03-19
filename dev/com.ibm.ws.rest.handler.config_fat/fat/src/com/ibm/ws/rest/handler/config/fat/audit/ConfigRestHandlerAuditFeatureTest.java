/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.rest.handler.config.fat.audit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.rest.handler.config.fat.FATSuite;
import com.ibm.ws.security.audit.fat.common.tooling.AuditMessageConstants;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class ConfigRestHandlerAuditFeatureTest extends FATServletClient {
    private static final Class<?> c = ConfigRESTHandlerAuditTest.class;

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat("com.ibm.ws.rest.handler.config.audit.feature.fat",
                                                         EERepeatActions.EE10, // EE10
                                                         EERepeatActions.EE9, // EE9
                                                         EERepeatActions.EE8);

    @Server("com.ibm.ws.rest.handler.config.audit.feature.fat")
    public static LibertyServer server;

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer("CWWKE0701E",
                              "CWWKS1300E",
                              "WTRN0112E");
        } finally {
            // Remove the user extension added during setup
            server.deleteDirectoryFromLibertyInstallRoot("usr/extension/");
        }
    }

    // Configure the server with only audit-2.0.
    // This test should not see the /ibm/api uri in the logs.
    @Test
    public void testAudit20FeatureOnly() throws Exception {

        // Install user features
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/nestedFlat-1.0.mf");

        // Install bundles for user features
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/test.config.nested.flat.jar");

        FATSuite.setupServerSideAnnotations(server);

        server.startServer();

        // Wait for the API to become available
        assertNotNull(server.waitForStringInLog("CWWKS0008I")); // CWWKS0008I: The security service is ready.
        assertNotNull("FeatureManager did not report the audit feature was installed",
                      server.waitForStringInLogUsingMark(AuditMessageConstants.CWWKF0012I_AUDIT_FEATURE_INSTALLED));
        assertNotNull("Audit service did not report it was ready",
                      server.waitForStringInLogUsingMark(AuditMessageConstants.CWWKS5851I_AUDIT_SERVICE_READY));
        assertNotNull("Audit file handler service did not report it was ready",
                      server.waitForStringInLogUsingMark(AuditMessageConstants.CWWKS5805I_AUDIT_FILEHANDLER_SERVICE_READY));
        assertNotNull(server.waitForStringInLog("CWWKS4105I")); // CWWKS4105I: LTPA configuration is ready after # seconds.
        assertNotNull(server.waitForStringInLog("CWPKI0803A")); // CWPKI0803A: SSL certificate created in # seconds. SSL key file: ...
        assertNotNull(server.waitForStringInLog("CWWKO0219I")); // CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl has been started and is now listening for requests on host *  (IPv6) port 8020.

        //Audit-2.0 should not enable ibm/api
        assertTrue((server.waitForStringInLog("CWWKT0016I")) == null); // CWWKT0016I: Web application available (default_host): http://9.10.111.222:8010/ibm/api/
    }
}
