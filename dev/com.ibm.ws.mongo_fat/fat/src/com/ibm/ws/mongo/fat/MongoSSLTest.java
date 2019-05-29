/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.mongo.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import fat.mongo.web.MongoSSLTestServlet;
import fat.mongo.web.MongoTestServlet;

@RunWith(FATRunner.class)
public class MongoSSLTest extends FATServletClient {
    @Server("mongo.fat.server.ssl")
    @TestServlets({
                    @TestServlet(servlet = MongoTestServlet.class, contextRoot = FATSuite.APP_NAME),
                    @TestServlet(servlet = MongoSSLTestServlet.class, contextRoot = FATSuite.APP_NAME)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        MongoServerSelector.assignMongoServers(server);
        FATSuite.createApp(server);
        server.startServer();
        FATSuite.waitForMongoSSL(server);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        // TODO: CWWKE0701E - Circular reference detected trying to get service
        // {org.osgi.service.cm.ManagedServiceFactory,
        // com.ibm.wsspi.logging.Introspector,
        // com.ibm.ws.runtime.update.RuntimeUpdateListener,
        // com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator}
        server.stopServer("CWKKD0026E:.*mongo-valid-certificate-no-alias-but-reqd.*",
                          "CWKKD0024E:.*mongo-invalid-certauth-no-sslenabled.*",
                          "CWKKD0024E:.*mongo-invalid-certauth-sslenabled-false.*",
                          "CWKKD0023E:.*mongo-invalid-certauth-driver-level.*",
                          "CWPKI0023E:.*wibblewibblewibble.*",
                          "CWWKE0701E",
                          "CWWKG0033W");
    }

    @Before
    public void beforeEach() throws Exception {
        server.setMarkToEndOfLog();
    }

    @Test
    public void testInsertFindInvalidTruststore() throws Exception {
        testInvalidConfig("mongo/testdb-invalid-truststore", "javax.naming.NameNotFoundException");
    }

    @Test
    @ExpectedFFDC({ "java.lang.RuntimeException" })
    public void testCertAuthPasswordCoded() throws Exception {
        testInvalidConfig("mongo/testdb-invalid-certauth-parms-password", "CWWKN0008E");
        // CWKKD0018E: The {0} service encountered an incompatible combination of authentication options.
        // useCertificateAuthentication is incompatible with user and password.
        assertNotNull("Server exception for error CWKKD0018E was not found within the allotted interval", server.waitForStringInLogUsingMark("CWKKD0018E"));
        assertNotNull("Server exception for error CWWKE0701E was not found within the allotted interval", server.waitForStringInLogUsingMark("CWWKE0701E"));
    }

    @Test
    @ExpectedFFDC({ "java.lang.RuntimeException" })
    public void testCertAuthUseridCoded() throws Exception {
        testInvalidConfig("mongo/testdb-invalid-certauth-parms-userid", "CWWKN0008E");
        // CWKKD0018E: The {0} service encountered an incompatible combination of authentication options.
        // useCertificateAuthentication is incompatible with user and password.
        assertNotNull("Server exception for error CWKKD0018E was not found within the allotted interval", server.waitForStringInLogUsingMark("CWKKD0018E"));
        assertNotNull("Server exception for error CWWKE0701E was not found within the allotted interval", server.waitForStringInLogUsingMark("CWWKE0701E"));
    }

    @Test
    @ExpectedFFDC({ "java.security.PrivilegedActionException", "java.lang.IllegalArgumentException" })
    public void testCertAuthAliasNotInKeystore() throws Exception {
        testInvalidConfig("mongo/testdb-valid-certificate-alias-not-in-keystore", "CWWKN0008E");
        // CWPKI0023E: The certificate alias {0} specified by the property com.ibm.ssl.keyStoreClientAlias is not found in KeyStore {1}.
        assertNotNull("Server exception for error CWPKI0023E was not found within the allotted interval", server.waitForStringInLogUsingMark("CWPKI0023E"));
        assertNotNull("Server exception for error CWWKE0701E was not found within the allotted interval", server.waitForStringInLogUsingMark("CWWKE0701E"));
    }

    @Test
    public void testCertAuthAliasInvalid() throws Exception {
        testInvalidConfig("mongo/testdb-valid-certificate-invalid-alias", "CommandFailureException");
        assertNotNull("'Could not find user CN=client_not_known' was not found within the allotted interval",
                      server.waitForStringInLogUsingMark("com.mongodb.CommandFailureException.*Could not find user CN=client_not_known"));
    }

    @Test
    @ExpectedFFDC({ "java.lang.RuntimeException" })
    public void testCertAuthAliasMissing() throws Exception {
        testInvalidConfig("mongo/testdb-valid-certificate-no-alias-but-reqd", "CWWKN0008E");
        // CWKKD0026E: The mongo service with id {0} was unable to extract the client key and certificate from the keystore.
        // Either there are no keys in the keystore, or there are multiple keys and clientKeyAlias was not specified on the ssl element.
        assertNotNull("Server exception for error CWKKD0026E was not found within the allotted interval", server.waitForStringInLogUsingMark("CWKKD0026E"));
        assertNotNull("Server exception for error CWWKE0701E was not found within the allotted interval", server.waitForStringInLogUsingMark("CWWKE0701E"));
    }

    @Test
    @ExpectedFFDC({ "java.lang.RuntimeException" })
    public void testCertAuthSSLEnabledNotSet() throws Exception {
        testInvalidConfig("mongo/testdb-invalid-certauth-no-sslenabled", "CWWKN0008E");
        // CWKKD0024E: The {0} service with id {1} has the sslRef property set in the server.xml but sslEnabled is not set to true.
        assertNotNull("Server exception for error CWKKD0024E was not found within the allotted interval", server.waitForStringInLogUsingMark("CWKKD0024E"));
        assertNotNull("Server exception for error CWWKE0701E was not found within the allotted interval", server.waitForStringInLogUsingMark("CWWKE0701E"));
    }

    @Test
    @ExpectedFFDC({ "java.lang.RuntimeException" })
    public void testCertAuthSSLEnabledFalse() throws Exception {
        testInvalidConfig("mongo/testdb-invalid-certauth-sslenabled-false", "CWWKN0008E");
        // CWKKD0024E: The {0} service with id {1} has the sslRef property set in the server.xml but sslEnabled is not set to true.
        assertNotNull("Server exception for error CWKKD0024E was not found within the allotted interval", server.waitForStringInLogUsingMark("CWKKD0024E"));
        assertNotNull("Server exception for error CWWKE0701E was not found within the allotted interval", server.waitForStringInLogUsingMark("CWWKE0701E"));
    }

    @Test
    @ExpectedFFDC({ "java.lang.RuntimeException" })
    public void testCertAuthOldDriver() throws Exception {
        testInvalidConfig("mongo/testdb-invalid-certauth-driver-level", "CWWKN0008E");
        // CWKKD0023E: The {0} service encountered an incompatible version of the MongoDB driver at shared library {1}.  For certificate authentication
        // a minimum level of {2} is required, but found {3}.
        assertNotNull("Server exception for error CWKKD0023E was not found within the allotted interval", server.waitForStringInLogUsingMark("CWKKD0023E"));
        assertNotNull("Server exception for error CWWKE0701E was not found within the allotted interval", server.waitForStringInLogUsingMark("CWWKE0701E"));
    }

    private void testInvalidConfig(String jndiName, String expectedEx) throws Exception {
        FATServletClient.runTest(server, FATSuite.APP_NAME + "/MongoSSLTestServlet",
                                 "testInvalidConfig&jndiName=" + jndiName + "&expectedEx=" + expectedEx + "&forTest=" + testName.getMethodName());
    }
}
