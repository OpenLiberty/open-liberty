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

import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ClassloaderElement;
import com.ibm.websphere.simplicity.config.MongoElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.mongo.fat.shared.MongoServletAction;

import componenttest.annotation.AllowedFFDC;

// We know that some FFDCs get emitted late because the mongo client starts a background thread to
// check the server status.  So this will stop tests AFTER the test that is issuing the FFDC from failing.
@AllowedFFDC({ "java.security.cert.CertPathBuilderException", "sun.security.validator.ValidatorException" })
public class TestMongoDbSSL extends TestMongoDb {
    private static final Class<?> c = TestMongoDbSSL.class;

    @BeforeClass
    public static void b() throws Exception {
        before("com.ibm.ws.mongo.fat.server.ssl");
    }

    @AfterClass
    public static void a() throws Exception {
        after("CWKKD0017E:.*",
              "CWKKD0013E:.*",
              "SRVE0777E:.*",
              "CWKKD0010E.*mongo-invalid-truststore.*",
              "SRVE0319E:.*[INVALID_TRUSTSTORE].*",
              "CWNEN1006E:.*invalid-truststore.*",
              "CWNEN0030E:.*invalid-truststore.*",
              "CWKKD0026E:.*mongo-valid-certificate-no-alias-but-reqd.*",
              "CWKKD0024E:.*mongo-invalid-certauth-no-sslenabled.*",
              "CWKKD0024E:.*mongo-invalid-certauth-sslenabled-false.*",
              "CWKKD0023E:.*mongo-invalid-certauth-driver-level.*",
              "CWPKI0023E:.*wibblewibblewibble.*",
              "SRVE0315E:.*com.mongodb.CommandFailureException.*client_not_known.*",
              "CWPKI0022E:.*", // SSL HANDSHAKE FAILURE
              "SRVE0315E:.*javax.net.ssl.SSLHandshakeException.*");
    }

    @Test()
    public void testInsertFindNestedSSL() throws Exception {
        final String method = "testInsertFindNestedSSL";
        Log.info(c, method, "entering " + method);
        insertFindScenario(true, MongoServlet.NESTED_SSL);
        Log.info(c, method, "exiting " + method);
    }

    @Test()
    public void testInsertFindSSLEnabledFalse() throws Exception {
        final String method = "testInsertFindSSLEnabledFalse";
        Log.info(c, method, "entering " + method);
        insertFindScenario(true, MongoServlet.SSLENABLED_FALSE);
        Log.info(c, method, "exiting " + method);
    }

    @Test()
    public void testInsertFindSSLEnabledFalseSSLRef() throws Exception {
        final String method = "testInsertFindSSLEnabledFalseSSLRef";
        Log.info(c, method, "entering " + method);
        insertFindScenario(true, MongoServlet.SSLENABLED_FALSE_SSLREF);
        Log.info(c, method, "exiting " + method);
    }

    @Test()
    public void testInsertFindDifferentSSLRef() throws Exception {
        final String method = "testInsertFindDifferentSSLRef";
        Log.info(c, method, "entering " + method);

        // Just because SSL feature is reported as starting doesn't mean it is really ready,
        // nor that Mongo is functional, so wait for SSL to report the keystore has been
        // added, then wait for MongoDBService to activate.
        server.resetLogMarks(); // look from start of logs
        server.waitForStringInTraceUsingMark("Adding keystore: differentTrustStore", 30 * 1000);
        server.waitForStringInTraceUsingMark("MongoDBService * < activate", 30 * 1000);

        insertFindScenario(true, MongoServlet.DIFFERENT_SSLREF);
        Log.info(c, method, "exiting " + method);
    }

    @Test
    @AllowedFFDC({ "com.ibm.websphere.ssl.SSLException", "com.ibm.wsspi.injectionengine.InjectionException", "javax.servlet.UnavailableException",
                   "com.mongodb.MongoTimeoutException", "java.security.PrivilegedActionException", "java.lang.IllegalArgumentException" })
    public void testInsertFindInvalidTruststore() throws Exception {
        final String method = "testInsertFindInvalidTruststore";
        Log.info(c, method, "entering " + method);
        try {
            insertFindScenario(true, MongoServlet.INVALID_TRUSTSTORE);
            fail("Expected Exception when trying to insert into a database using an invalid truststore");
        } catch (Exception e) {
        }
        Log.info(c, method, "exiting " + method);
    }

    @Test
    public void testRemoveAddSSLConfigFromRunningServer() throws Exception {
        final String method = "testRemoveSSLConfigFromRunningServer";
        Log.info(c, method, "entering " + method);
        ServerConfiguration working = config.clone();

        MongoElement mongoElement = working.getMongos().getById("mongo-auth-encrypted");
        mongoElement.setSslEnabled(null);
        mongoElement.setSslRef(null);
        Integer[] authPort = { 27018 };
        mongoElement.setPorts(authPort);
        updateServerConfigWithParams(working, true, true, MongoServlet.JNDI, true, false, "test", "testRemoveSSLConfigFromRunningServer()");
        insertFindScenario(true, MongoServlet.JNDI);

        updateServerConfig(config, true, true, MongoServlet.JNDI, true, true);
        insertFindScenario(true, MongoServlet.JNDI);

        Log.info(c, method, "exiting " + method);
    }

    @Test
    @AllowedFFDC({ "java.lang.RuntimeException", "com.ibm.wsspi.injectionengine.InjectionException" })
    public void testVersionTwoTenSSLMongoFailure() throws Exception {
        final String method = "testVersionTwoTenSSLMongoFailure";
        Log.info(c, method, "entering " + method);
        ServerConfiguration working = config.clone();
        try {
            modifyLib(working, "mongo-lib-210");
            updateServerConfig(working, true, true, MongoServlet.JNDI, true, false);
            // CWWKZ0009I: The application mongo has stopped successfully.
            if (server.waitForStringInLogUsingMark("CWWKZ0009I.* mongo") == null) {
                throw new Exception("Server application stop did not complete within the allotted interval");
            }
            // CWWKZ0001I: Application mongo started in 0.9 seconds.
            // CWWKZ0003I: The application mongo updated in 0.3 seconds.
            if (server.waitForStringInLogUsingMark("CWWKZ000[13]I.* mongo") == null) {
                throw new Exception("Server application restart did not complete within the allotted interval");
            }
            try {
                doRequest(MongoServletAction.DUMP_CONFIG, MongoServlet.JNDI, "key",
                          "testVersionTwoTenSSLMongoFailure");
            } catch (Exception e) {
                // expected
            }
            // check the logs for
            if (server.waitForStringInLogUsingMark("CWKKD0017E") == null) {
                throw new Exception("Server exception for error CWKKD0017E was not found within the allotted interval");
            }

        } finally {
            updateServerConfig(config, true, true, MongoServlet.JNDI, false, true);
        }
        Log.info(c, method, "exiting " + method);
    }

    //************* CERTAUTH TESTS *****************************************

    @AllowedFFDC({ "java.lang.RuntimeException", "com.ibm.wsspi.injectionengine.InjectionException", "javax.servlet.UnavailableException" })
    @Test()
    public void testCertAuthPasswordCoded() throws Exception {
        final String method = "testCertAuthPasswordCoded";
        Log.info(c, method, "entering " + method);
        try {
            insertFindScenario(true, MongoServlet.CERT_PASSWORD_INV);
            fail("Expected Exception when coding password with useCertificateAuthentication");
        } catch (Exception e) {
        }
        // CWKKD0018.ssl.user.pswd.certificate=CWKKD0018E: The {0} service encountered an incompatible combination of authentication options. useCertificateAuthentication is incompatible with user and password.
        if (server.waitForStringInLogUsingMark("CWKKD0018E") == null) {
            throw new Exception("Server exception for error CWKKD0018E was not found within the allotted interval");
        }
        Log.info(c, method, "exiting " + method);
    }

    @AllowedFFDC({ "java.lang.RuntimeException", "com.ibm.wsspi.injectionengine.InjectionException", "javax.servlet.UnavailableException" })
    @Test()
    public void testCertAuthUseridCoded() throws Exception {
        final String method = "testCertAuthUseridCoded";
        Log.info(c, method, "entering " + method);
        try {
            insertFindScenario(true, MongoServlet.CERT_USER_INV);
            fail("Expected Exception when coding userid with useCertificateAuthentication");
        } catch (Exception e) {
        }
        // CWKKD0018.ssl.user.pswd.certificate=CWKKD0018E: The {0} service encountered an incompatible combination of authentication options. useCertificateAuthentication is incompatible with user and password.
        if (server.waitForStringInLogUsingMark("CWKKD0018E") == null) {
            throw new Exception("Server exception for error CWKKD0018E was not found within the allotted interval");
        }
        Log.info(c, method, "exiting " + method);
    }

    @Test()
    public void testCertAuthAliasValid() throws Exception {
        final String method = "testCertAuthAliasValid";
        Log.info(c, method, "entering " + method);
        insertFindScenario(true, MongoServlet.CERT_ALIAS_VALID);
        Log.info(c, method, "exiting " + method);
    }

    @AllowedFFDC({ "java.security.PrivilegedActionException",
                   "com.ibm.wsspi.injectionengine.InjectionException",
                   "javax.servlet.UnavailableException",
                   "java.lang.IllegalArgumentException" })
    @Test()
    public void testCertAuthAliasNotInKeystore() throws Exception {
        final String method = "testCertAuthAliasInvalid";
        Log.info(c, method, "entering " + method);
        try {
            server.setMarkToEndOfLog();
            insertFindScenario(true, MongoServlet.CERT_ALIAS_NOT_IN_KEYSTORE);
            fail("Expected Exception when specifying an invalid alias");
        } catch (Exception e) {
        }
        //CWPKI0023E: The certificate alias [alias] specified by the property com.ibm.ssl.keyStoreClientAlias is not found in KeyStore C:/java/eclipse/WDTMarsLatestWdt27032017/ws-WDT/MyServers/servers/mongoServer/client1x.jks.
        if (server.waitForStringInLogUsingMark("CWPKI0023E") == null) {
            throw new Exception("Server exception for error CWPKI0023E was not found within the allotted interval");
        }
        Log.info(c, method, "exiting " + method);
    }

    @AllowedFFDC({ "com.mongodb.CommandFailureException" })
    @Test()
    public void testCertAuthAliasInvalid() throws Exception {
        final String method = "testCertAuthAliasInvalid";
        Log.info(c, method, "entering " + method);
        try {
            server.setMarkToEndOfLog();
            insertFindScenario(true, MongoServlet.CERT_ALIAS_INV);
            fail("Expected Exception when specifying an invalid alias");
        } catch (Exception e) {
        }
        //CWPKI0023E: The certificate alias [alias] specified by the property com.ibm.ssl.keyStoreClientAlias is not found in KeyStore C:/java/eclipse/WDTMarsLatestWdt27032017/ws-WDT/MyServers/servers/mongoServer/client1x.jks.
        if (server.waitForStringInLogUsingMark("com.mongodb.CommandFailureException.*Could not find user CN=client_not_known") == null) {
            throw new Exception("'Could not find user CN=client_not_known' was not found within the allotted interval");
        }
        Log.info(c, method, "exiting " + method);
    }

    @AllowedFFDC({ "java.lang.RuntimeException", "com.ibm.wsspi.injectionengine.InjectionException", "javax.servlet.UnavailableException" })
    @Test()
    public void testCertAuthAliasMissing() throws Exception {
        final String method = "testCertAuthAliasMissing";
        Log.info(c, method, "entering " + method);
        try {
            server.setMarkToEndOfLog();
            insertFindScenario(true, MongoServlet.CERT_ALIAS_MISSING);
            fail("Expected Exception when an alias is not supplied but required");
        } catch (Exception e) {
        }
        // CWNEN1006E: The server was unable to obtain an object for the mongo/sampledb binding with the
        //   com.mongodb.DB type. The exception message was: java.lang.IllegalArgumentException:
        //   username can not be null
        if (server.waitForStringInLogUsingMark("CWNEN1006E") == null) {
            throw new Exception("Server exception for error CWNEN1006E was not found within the allotted interval");
        }
        Log.info(c, method, "exiting " + method);
    }

    @Test()
    public void testCertAuthAliasNotReqd() throws Exception {
        final String method = "testCertAuthAliasNotReqd";
        Log.info(c, method, "entering " + method);
        server.setMarkToEndOfLog();
        insertFindScenario(true, MongoServlet.CERT_ALIAS_NOT_REQD);
        Log.info(c, method, "exiting " + method);
    }

    @AllowedFFDC({ "com.mongodb.MongoTimeoutException" })
    @Test()
    // Both of the following exceptions are added to the class @AllowedFFDC as they can be returned late
    // and fail other tests.  The error messages produced are the same (both produce CWPKI0022E).
    // IBM JDK: java.security.cert.CertPathBuilderException: PKIXCertPathBuilderImpl could not build a valid CertPath.
    // Sun JDK: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
    public void testCertAuthInvalidTrust() throws Exception {
        final String method = "testCertAuthInvalidTrust";
        Log.info(c, method, "entering " + method);
        try {
            server.setMarkToEndOfLog();
            insertFindScenario(true, MongoServlet.CERT_TRUST_INV);
            fail("Expected Exception when trust store not valid");
        } catch (Exception e) {
        }
        // CWPKI0022E: SSL HANDSHAKE FAILURE:  A signer with SubjectDN [snip] was sent from the target host.  The signer might need to be added to local trust store [snip]
        if (server.waitForStringInLogUsingMark("CWPKI0022E") == null) {
            throw new Exception("Server exception for error CWPKI0022E was not found within the allotted interval");
        }
        Log.info(c, method, "exiting " + method);
    }

    @AllowedFFDC({ "java.lang.RuntimeException", "com.ibm.wsspi.injectionengine.InjectionException", "javax.servlet.UnavailableException" })
    @Test()
    public void testCertAuthSSLEnabledNotSet() throws Exception {
        final String method = "testCertAuthSSLEnabledNotSet";
        Log.info(c, method, "entering " + method);
        try {
            server.setMarkToEndOfLog();
            insertFindScenario(true, MongoServlet.CERT_SSLENABLED_NOT_SET);
            fail("Expected Exception sslEnabled not set");
        } catch (Exception e) {
        }
        // CWKKD0024E: The {0} service with id {1} has the sslRef property set in the server.xml but sslEnabled is not set to true.
        if (server.waitForStringInLogUsingMark("CWKKD0024E") == null) {
            throw new Exception("Server exception for error CWKKD0024E was not found within the allotted interval");
        }
        Log.info(c, method, "exiting " + method);
    }

    @AllowedFFDC({ "java.lang.RuntimeException", "com.ibm.wsspi.injectionengine.InjectionException", "javax.servlet.UnavailableException" })
    @Test()
    public void testCertAuthSSLEnabledFalse() throws Exception {
        final String method = "testCertAuthSSLEnabledFalse";
        Log.info(c, method, "entering " + method);
        try {
            server.setMarkToEndOfLog();
            insertFindScenario(true, MongoServlet.CERT_SSLENABLED_FALSE);
            fail("Expected Exception when an sslEnabled set to false");
        } catch (Exception e) {
        }
        // CWKKD0024E: The {0} service with id {1} has the sslRef property set in the server.xml but sslEnabled is not set to true.
        if (server.waitForStringInLogUsingMark("CWKKD0024") == null) {
            throw new Exception("Server exception for error CWKKD0024 was not found within the allotted interval");
        }
        Log.info(c, method, "exiting " + method);
    }

    @AllowedFFDC({ "java.lang.RuntimeException", "com.ibm.wsspi.injectionengine.InjectionException", "javax.servlet.UnavailableException" })
    @Test()
    public void testCertAuthOldDriver() throws Exception {
        final String method = "testCertAuthOldDriver";
        Log.info(c, method, "entering " + method);
        try {
            server.setMarkToEndOfLog();
            insertFindScenario(true, MongoServlet.CERT_INVALID_DRIVER_LEVEL);
            fail("Expected Exception when using a driver that does not support certificate authentication");
        } catch (Exception e) {
        }
        // CWKKD0023.ssl.certauth.incompatible.driver=CWKKD0023E: The {0} service encountered an incompatible version of the MongoDB driver at shared library {1}. For certificate authentication a minimum level of {2} is required, but found {3}.
        if (server.waitForStringInLogUsingMark("CWKKD0023E") == null) {
            throw new Exception("Server exception for error CWKKD0023E was not found within the allotted interval");
        }
        Log.info(c, method, "exiting " + method);
    }
    //************* CERTAUTH TESTS END *****************************************

    @Override
    protected void updateApplication(ServerConfiguration sc, String libName) {
        for (Application app : sc.getApplications()) {
            if ("mongo.war".equals(app.getLocation())) {
                ClassloaderElement cl = app.getClassloader();
                Set<String> refs = cl.getCommonLibraryRefs();
                refs.clear();
                refs.add(libName);
            }
        }
    }
}
