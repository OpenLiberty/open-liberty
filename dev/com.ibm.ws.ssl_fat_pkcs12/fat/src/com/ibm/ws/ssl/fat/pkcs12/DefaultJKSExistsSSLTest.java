/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ssl.fat.pkcs12;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.SSLBasicAuthClient;

import componenttest.annotation.AllowedFFDC;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.vulnerability.LeakedPasswordChecker;

public class DefaultJKSExistsSSLTest extends CommonSSLTest {
    private static final Class<?> c = DefaultJKSExistsSSLTest.class;

    public DefaultJKSExistsSSLTest() {
        super(LibertyServerFactory.getLibertyServer("com.ibm.ws.ssl.fat.pkcs12.withkeyjks"));
    }

    @Rule
    public TestRule passwordChecker = new LeakedPasswordChecker(server);

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        server.removeInstalledAppForValidation("basicauth");
    }

    /**
     * Validste that with the SSL feature but no SSL config specified in server.xml,
     * and an existing default key.jks file exists, use the default key.jks keystore
     * and do not create a pkcs12 default keystore.
     *
     */
    @Test
    @AllowedFFDC("java.lang.NoClassDefFoundError")
    public void testSSLFeatureNoSSLConfigExistingJKSUsesJKSKeyStore() throws Exception {

        Log.info(c, name.getMethodName(), "Entering " + name.getMethodName());
        String protocol = TLSV11_PROTOCOL;

        server.setServerConfigurationFile(NO_SSL_CONFIG_BUT_DOES_INCLUDE_SSL_FEATURE);
        server.startServer(name.getMethodName() + ".log");

        // Requires info trace
        server.addInstalledAppForValidation("basicauth");

        assertNotNull("SSL TCP Channel did not start in time.",
                      server.waitForStringInLog("CWWKO0219I.*ssl"));
        assertNotNull("Need to wait for 'smarter planet' message (server is ready).",
                      server.waitForStringInLog("CWWKF0011I"));

        // Check to see if the file has not been generated, but exists already
        assertTrue("The expected key file " + "resources/security/key.jks" + " was not generated",
                   fileExists(server, "resources/security/key.jks"));

        // Need to verify the certificate
        assertTrue("Default certificate not created correctly",
                   verifyDefaultCert(server, "resources/security/key.jks"));

        // Hit the servlet on the SSL port and validate it requires SSL
        SSLBasicAuthClient sslClient = null;
        String response = null;
        try {
            sslClient = createSSLClientWithTrust("resources/security/key.jks", DEFAULT_GENERATED_KEY_PASSWORD, protocol);
            response = sslClient.accessUnprotectedServlet(SSLBasicAuthClient.UNPROTECTED_NO_SECURITY_CONSTRAINT);
        } catch (Exception e) {
        }
        if (response != null)
            assertTrue("Did not get the expected response",
                       sslClient.verifyUnauthenticatedResponse(response));
        else
            assertTrue("Did not get the expected response", false);

        // Check to see if the pkcs12 file has NOT been generated

        assertFalse("The expected key file " + DEFAULT_GENERATED_KEY_PATH + " was generated",
                    fileExists(server, DEFAULT_GENERATED_KEY_PATH));

        if (server != null && server.isStarted()) {
            server.stopServer("CWNEN0050W", "SRVE0272W");
        }
        deleteFileIfExists(server, DEFAULT_GENERATED_KEY_PATH);
        deleteFileIfExists(server, ALTERNATE_GENERATED_KEY_PATH);

        Log.info(c, name.getMethodName(), "Exiting " + name.getMethodName());

    }

    /**
     * Using a minimal SSL default configuration, if the default SSL key.jks file does exist,
     * do not generate a new PKCS12 key.p12 file, but use the
     * existing key.jks as the default keystore (do not generate a new key.jks)
     */
    @Test
    @AllowedFFDC("java.lang.NoClassDefFoundError")
    public void testDefaultMinimalSSLCertificateFileKeyJKSFileDoesExist() throws Exception {

        Log.info(c, name.getMethodName(), "Entering " + name.getMethodName());
        String protocol = TLSV11_PROTOCOL;

        server.setServerConfigurationFile(DEFAULT_MINIMAL_SSL_CONFIG);
        server.startServer(name.getMethodName() + ".log");

        // Requires info trace
        server.addInstalledAppForValidation("basicauth");
        assertNotNull("SSL TCP Channel did not start in time.",
                      server.waitForStringInLog("CWWKO0219I.*ssl"));
        assertNotNull("Need to wait for 'smarter planet' message (server is ready).",
                      server.waitForStringInLog("CWWKF0011I"));

        // Check to see if the file has been generated; it should already exist and not be generated
        assertTrue("The expected key file " + "resources/security/key.jks" + " was not generated",
                   fileExists(server, "resources/security/key.jks"));

        // Need to verify the certificate
        assertTrue("Default certificate not created correctly",
                   verifyDefaultCert(server, "resources/security/key.jks"));

        // Hit the servlet on the SSL port and validate it requires SSL
        SSLBasicAuthClient sslClient = null;
        String response = null;
        try {
            sslClient = createSSLClientWithTrust("resources/security/key.jks", DEFAULT_GENERATED_KEY_PASSWORD, protocol);
            response = sslClient.accessUnprotectedServlet(SSLBasicAuthClient.UNPROTECTED_NO_SECURITY_CONSTRAINT);
        } catch (Exception e) {
        }
        if (response != null)
            assertTrue("Did not get the expected response",
                       sslClient.verifyUnauthenticatedResponse(response));
        else
            assertTrue("Did not get the expected response", false);

        // Check to see if the pkcs12 file has NOT been generated

        assertFalse("The expected key file " + DEFAULT_GENERATED_KEY_PATH + " was generated",
                    fileExists(server, DEFAULT_GENERATED_KEY_PATH));

        Log.info(c, name.getMethodName(), "Exiting " + name.getMethodName());

    }
}
