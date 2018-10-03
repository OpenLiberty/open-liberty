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
package com.ibm.ws.mongo.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import fat.mongo.web.MongoSSLTestServlet;
import fat.mongo.web.MongoTestServlet;

// We know that some FFDCs get emitted late because the mongo client starts a background thread to
// check the server status.  So this will stop tests AFTER the test that is issuing the FFDC from failing.
@AllowedFFDC({ "java.security.cert.CertPathBuilderException", "sun.security.validator.ValidatorException" })
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
        server.stopServer("CWKKD0017E:.*", "CWKKD0013E:.*", "SRVE0777E:.*", "CWKKD0010E.*mongo-invalid-truststore.*",
                          "SRVE0319E:.*[INVALID_TRUSTSTORE].*", "CWNEN1006E:.*invalid-truststore.*",
                          "CWNEN0030E:.*invalid-truststore.*", "CWKKD0026E:.*mongo-valid-certificate-no-alias-but-reqd.*",
                          "CWKKD0024E:.*mongo-invalid-certauth-no-sslenabled.*",
                          "CWKKD0024E:.*mongo-invalid-certauth-sslenabled-false.*",
                          "CWKKD0023E:.*mongo-invalid-certauth-driver-level.*", "CWPKI0023E:.*wibblewibblewibble.*",
                          "SRVE0315E:.*com.mongodb.CommandFailureException.*client_not_known.*", "CWPKI0022E:.*", // SSL HANDSHAKE
                          // FAILURE
                          "SRVE0315E:.*javax.net.ssl.SSLHandshakeException.*", "CWWKE0701E", "CWWKG0033W" // TODO: Circular reference detected
        // trying to get service
        // {org.osgi.service.cm.ManagedServiceFactory,
        // com.ibm.wsspi.logging.Introspector,
        // com.ibm.ws.runtime.update.RuntimeUpdateListener,
        // com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator}
        );
    }

    @After
    public void afterEach() throws Exception {
        server.setMarkToEndOfLog();
    }

    @Test
    @AllowedFFDC({ "com.ibm.websphere.ssl.SSLException", "com.ibm.wsspi.injectionengine.InjectionException",
                   "javax.servlet.UnavailableException", "com.mongodb.MongoTimeoutException",
                   "java.security.PrivilegedActionException", "java.lang.IllegalArgumentException" })
    public void testInsertFindInvalidTruststore() throws Exception {
        testInvalidConfig("mongo/testdb-invalid-truststore");
    }

    @Test
    @AllowedFFDC({ "java.lang.RuntimeException", "com.ibm.wsspi.injectionengine.InjectionException", "javax.servlet.UnavailableException" })
    public void testCertAuthPasswordCoded() throws Exception {
        testInvalidConfig("mongo/testdb-invalid-certauth-parms-password");
        // CWKKD0018.ssl.user.pswd.certificate=CWKKD0018E: The {0} service encountered
        // an incompatible combination of authentication options.
        // useCertificateAuthentication is incompatible with user and password.
        assertNotNull("Server exception for error CWKKD0018E was not found within the allotted interval", server.waitForStringInLogUsingMark("CWKKD0018E"));
    }

    @Test
    @AllowedFFDC({ "java.lang.RuntimeException", "com.ibm.wsspi.injectionengine.InjectionException", "javax.servlet.UnavailableException" })
    public void testCertAuthUseridCoded() throws Exception {
        testInvalidConfig("mongo/testdb-invalid-certauth-parms-userid");
        // CWKKD0018.ssl.user.pswd.certificate=CWKKD0018E: The {0} service encountered
        // an incompatible combination of authentication options.
        // useCertificateAuthentication is incompatible with user and password.
        assertNotNull("Server exception for error CWKKD0018E was not found within the allotted interval", server.waitForStringInLogUsingMark("CWKKD0018E"));
    }

    @Test
    @AllowedFFDC({ "java.security.PrivilegedActionException", "com.ibm.wsspi.injectionengine.InjectionException",
                   "javax.servlet.UnavailableException", "java.lang.IllegalArgumentException" })
    public void testCertAuthAliasNotInKeystore() throws Exception {
        testInvalidConfig("mongo/testdb-valid-certificate-alias-not-in-keystore");
        // CWPKI0023E: The certificate alias [alias] specified by the property
        // com.ibm.ssl.keyStoreClientAlias is not found in KeyStore
        // C:/java/eclipse/WDTMarsLatestWdt27032017/ws-WDT/MyServers/servers/mongoServer/client1x.jks.
        assertNotNull("Server exception for error CWPKI0023E was not found within the allotted interval", server.waitForStringInLogUsingMark("CWPKI0023E"));
    }

    @Test
    @AllowedFFDC({ "com.mongodb.CommandFailureException" })
    public void testCertAuthAliasInvalid() throws Exception {
        testInvalidConfig("mongo/testdb-valid-certificate-invalid-alias");
        // CWPKI0023E: The certificate alias [alias] specified by the property
        // com.ibm.ssl.keyStoreClientAlias is not found in KeyStore
        // C:/java/eclipse/WDTMarsLatestWdt27032017/ws-WDT/MyServers/servers/mongoServer/client1x.jks.
        assertNotNull("'Could not find user CN=client_not_known' was not found within the allotted interval",
                      server.waitForStringInLogUsingMark("com.mongodb.CommandFailureException.*Could not find user CN=client_not_known"));
    }

    @Test
    @AllowedFFDC({ "java.lang.RuntimeException", "com.ibm.wsspi.injectionengine.InjectionException",
                   "javax.servlet.UnavailableException" })
    public void testCertAuthAliasMissing() throws Exception {
        testInvalidConfig("mongo/testdb-valid-certificate-no-alias-but-reqd");
        // CWNEN1006E: The server was unable to obtain an object for the mongo/sampledb
        // binding with the
        // com.mongodb.DB type. The exception message was:
        // java.lang.IllegalArgumentException:
        // username can not be null
        assertNotNull("Server exception for error CWNEN1006E was not found within the allotted interval", server.waitForStringInLogUsingMark("CWKKD0026E"));
    }

    @Test
    @Mode(TestMode.FULL)
    @AllowedFFDC({ "com.mongodb.MongoTimeoutException" })
    // Both of the following exceptions are added to the class @AllowedFFDC as they
    // can be returned late
    // and fail other tests. The error messages produced are the same (both produce
    // CWPKI0022E).
    // IBM JDK: java.security.cert.CertPathBuilderException: PKIXCertPathBuilderImpl
    // could not build a valid CertPath.
    // Sun JDK: sun.security.validator.ValidatorException: PKIX path building
    // failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to
    // find valid certification path to requested target
    public void testCertAuthInvalidTrust() throws Exception {
        testInvalidConfig("mongo/testdb-invalid-certificate-trust");
        // CWPKI0022E: SSL HANDSHAKE FAILURE: A signer with SubjectDN [snip] was sent
        // from the target host. The signer might need to be added to local trust store
        assertNotNull("Server exception for error CWPKI0022E was not found within the allotted interval", server.waitForStringInLogUsingMark("CWPKI0022E"));
    }

    @Test
    @AllowedFFDC({ "java.lang.RuntimeException", "com.ibm.wsspi.injectionengine.InjectionException",
                   "javax.servlet.UnavailableException" })
    public void testCertAuthSSLEnabledNotSet() throws Exception {
        testInvalidConfig("mongo/testdb-invalid-certauth-no-sslenabled");
        // CWKKD0024E: The {0} service with id {1} has the sslRef property set in the
        // server.xml but sslEnabled is not set to true.
        assertNotNull("Server exception for error CWKKD0024E was not found within the allotted interval", server.waitForStringInLogUsingMark("CWKKD0024E"));
    }

    @Test
    @AllowedFFDC({ "java.lang.RuntimeException", "com.ibm.wsspi.injectionengine.InjectionException",
                   "javax.servlet.UnavailableException" })
    public void testCertAuthSSLEnabledFalse() throws Exception {
        testInvalidConfig("mongo/testdb-invalid-certauth-sslenabled-false");
        // CWKKD0024E: The {0} service with id {1} has the sslRef property set in the
        // server.xml but sslEnabled is not set to true.
        assertNotNull("Server exception for error CWKKD0024 was not found within the allotted interval", server.waitForStringInLogUsingMark("CWKKD0024"));
    }

    @Test
    @AllowedFFDC({ "java.lang.RuntimeException", "com.ibm.wsspi.injectionengine.InjectionException", "javax.servlet.UnavailableException" })
    public void testCertAuthOldDriver() throws Exception {
        testInvalidConfig("mongo/testdb-invalid-certauth-driver-level");
        // CWKKD0023.ssl.certauth.incompatible.driver=CWKKD0023E: The {0} service
        // encountered an incompatible version of the MongoDB driver at shared library
        // {1}. For certificate authentication a minimum level of {2} is required, but
        // found {3}.
        assertNotNull("Server exception for error CWKKD0023E was not found within the allotted interval", server.waitForStringInLogUsingMark("CWKKD0023E"));
    }

    private void testInvalidConfig(String jndiName) throws Exception {
        FATServletClient.runTest(server, FATSuite.APP_NAME + "/MongoSSLTestServlet", "testInvalidConfig&jndiName=" + jndiName + "&forTest=" + testName.getMethodName());
    }
}
