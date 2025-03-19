/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
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
package com.ibm.ws.cloudant.fat;

import static com.ibm.ws.cloudant.fat.FATSuite.cloudant;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class CloudantTest extends FATServletClient {

    @Server("com.ibm.ws.cloudant.fat")
    public static LibertyServer server;

    private static final String DB_NAME = "cloudantdb";
    public static final String JEE_APP = "cloudantfat";
    public static final String SERVLET_NAME = "CloudantTestServlet";
    public static String[] expectedFailures = { "CWWKE0701E.*ResourceFactoryTrackerData",
                                                "CWWKS1300E.*missingAuthData",
                                                "CWPKI0022E.*invalid_keystore",
                                                "CWPKI0823E.*",
                                                "CWWKG0033W.*does_not_exist",
                                                "CWWKO0801E.*no cipher suites in common",
                                                "CWPKI0312E.*localhost",
                                                "CWPKI0063W" };

    @BeforeClass
    public static void setUp() throws Exception {
        server.addEnvVar("cloudant_url", cloudant.getURL(false));
        server.addEnvVar("cloudant_url_secure", cloudant.getURL(true));
        server.addEnvVar("cloudant_username", cloudant.getUser());
        server.addEnvVar("cloudant_password", cloudant.getPassword());
        server.addEnvVar("cloudant_databaseName", DB_NAME);

        cloudant.createDb(DB_NAME);

        // TODO extract security files from container prior to server start
        // TODO delete security files from git

//        cloudant.copyFileFromContainer("/etc/couchdb/cert/server.crt", server.getServerRoot() + "/security/server.crt");
//        FATSuite.createKeystore(server.getServerRoot() + "/security/keystore.jks", server.getServerRoot() + "/security/server.crt");

        ShrinkHelper.defaultApp(server, JEE_APP, "cloudant.web");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(expectedFailures);
    }

    private void runTest() throws Exception {
        runTest(server, JEE_APP + '/' + SERVLET_NAME, testName.getMethodName() + "&databaseName=" + DB_NAME);
    }

    @Test
    public void testAuthenticationTypeApplicationUnauthenticated() throws Exception {
        runTest();
    }

    @Test
    public void testBasicSaveAndFind() throws Exception {
        runTest();
    }

    @Test
    public void testCreateFalse() throws Exception {
        runTest();
    }

    @ExpectedFFDC("java.lang.UnsupportedOperationException")
    // direct lookup not supported
    @Test
    public void testDirectLookupOfClientBuilder() throws Exception {
        runTest();
    }

    @ExpectedFFDC("java.lang.ClassNotFoundException")
    @Test
    public void testLoadFromApp() throws Exception {
        runTest();
    }

    @Test
    public void testResourceRefContainerAuthAlias() throws Exception {
        runTest();
    }

    @Test
    public void testResourceRefContainerAuthAliasDB() throws Exception {
        runTest();
    }

    @ExpectedFFDC("javax.security.auth.login.LoginException")
    @AllowedFFDC("java.security.PrivilegedActionException")
    // contains the above LoginException
    @Test
    public void testResourceRefContainerAuthAliasInvalid() throws Exception {
        runTest();
    }

    @ExpectedFFDC("com.cloudant.client.org.lightcouch.CouchDbException")
    @Test
    public void testResourceRefContainerAuthAliasInvalidUser() throws Exception {
        runTest();
    }

    @ExpectedFFDC("com.cloudant.client.org.lightcouch.CouchDbException")
    @Test
    public void testResourceRefContainerAuthAliasNotAppliedtoApplicationAuth() throws Exception {
        runTest();
    }

    @Test
    public void testSSLBasic() throws Exception {
        runTest();
    }

    @Test
    public void testSSLNestedConfig() throws Exception {
        runTest();
    }

    @Test
    public void testNoSSLRef() throws Exception {
        runTest();
    }

    @Test
    @AllowedFFDC({ "java.security.cert.CertPathBuilderException",
                   "sun.security.validator.ValidatorException" })
    public void testInvalidSSL() throws Exception {
        runTest();
    }

    @Test
    public void testSSLAuthDisabled() throws Exception {
        runTest();
    }

}
