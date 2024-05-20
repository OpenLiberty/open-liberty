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

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.rules.RuleChain;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.rest.handler.config.fat.FATSuite;
import com.ibm.ws.security.audit.fat.common.tooling.AuditAsserts;
import com.ibm.ws.security.audit.fat.common.tooling.AuditMessageConstants;
import com.ibm.ws.security.audit.fat.common.tooling.RecentAuditFileStream;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpsRequest;

@RunWith(FATRunner.class)
public class ConfigRESTHandlerAuditTest extends FATServletClient {
    private static final Class<?> c = ConfigRESTHandlerAuditTest.class;
    public static final String REST_EVENT = AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_REST_HANDLER_AUTHZ;
    protected static String auditFileLogPath = null;
    protected static Boolean auditLogFilePathExists = false;

    protected String auditLogFilePath = null;
    public static final String DEFAULT_AUDIT_LOG = "audit.log";

    public static RepeatTests r = EERepeatActions.repeat("com.ibm.ws.rest.handler.config.audit.fat",
                                                         EERepeatActions.EE10, // EE10
                                                         EERepeatActions.EE9, // EE9
                                                         EERepeatActions.EE8);

    /**
     * Need the first repeat to make sure that audit-2.0 from a previous repeat gets put back to audit-1.0
     */
    public static RepeatTests auditRepeat = RepeatTests.with(new FeatureReplacementAction("audit-2.0", "audit-1.0").forServers("com.ibm.ws.rest.handler.config.audit.fat").fullFATOnly())
                    .andWith(new FeatureReplacementAction("audit-1.0", "audit-2.0").forServers("com.ibm.ws.rest.handler.config.audit.fat"));

    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(r).around(auditRepeat);

    @Server("com.ibm.ws.rest.handler.config.audit.fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
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
        assertNotNull(server.waitForStringInLog("CWWKT0016I")); // CWWKT0016I: Web application available (default_host): http://9.10.111.222:8010/ibm/api/

        // TODO remove once transactions code is fixed to use container auth for the recovery log dataSource
        // Lacking this fix, transaction manager will experience an auth failure and log FFDC for it.
        // The following line causes an XA-capable data source to be used for the first time outside of a test method execution,
        // so that the FFDC is not considered a test failure.
        new HttpsRequest(server, "/ibm/api/validation/dataSource/DefaultDataSource").run(JsonObject.class);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer("CWWKE0701E", // TODO remove once transaction manager fixes its circular reference bug
                              "CWWKS1300E", // auth alias doesn't exist
                              "WTRN0112E" // TODO remove once transactions code is fixed to use container auth for the recovery log dataSource
            );
        } finally {
            // Remove the user extension added during setup
            server.deleteDirectoryFromLibertyInstallRoot("usr/extension/");
        }
    }

    // Invoke /ibm/api/config REST API to display information for all configured instances.
    @Test
    public void testAuditPERestAuthorization() throws Exception {

        auditFileLogPath = server.getLogsRoot();
        RecentAuditFileStream recent = new RecentAuditFileStream(auditFileLogPath + "/audit.log");
        Log.info(c, testName.getMethodName(), "Looking for audit log at " + auditFileLogPath);
        AuditAsserts asserts = new AuditAsserts(c, recent);

        JsonArray json = new HttpsRequest(server, "/ibm/api/config").run(JsonArray.class);
        String err = "unexpected response: " + json;
        int count = json.size();
        assertTrue(err, count > 10);

        asserts.assertFoundInData(
                                  asserts.asJson(REST_EVENT,
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS,
                                                 AuditEvent.REASON_CODE + "=200",
                                                 AuditEvent.TARGET_APPNAME + "=RESTProxyServlet",
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=adminuser",
                                                 AuditEvent.TARGET_REALM + "=BasicRegistry",
                                                 AuditEvent.TARGET_NAME + "=" + "/ibm/api/config",
                                                 AuditEvent.TARGET_METHOD + "=" + "GET"));
    }
}
