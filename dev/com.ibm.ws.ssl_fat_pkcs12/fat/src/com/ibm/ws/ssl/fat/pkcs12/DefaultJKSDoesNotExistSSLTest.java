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

public class DefaultJKSDoesNotExistSSLTest extends CommonSSLTest {
    private static final Class<?> c = DefaultJKSDoesNotExistSSLTest.class;

    public DefaultJKSDoesNotExistSSLTest() {
        super(LibertyServerFactory.getLibertyServer("com.ibm.ws.ssl.fat.pkcs12.nokeyjks"));
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
     * When the SSL feature is specified, but no SSL config exists in the server.xml,
     * and no key.jks exists in the default path, a PKCS12 keystore is created.
     *
     */
    @Test
    @AllowedFFDC("java.lang.NoClassDefFoundError")
    public void testSSLFeatureNoSSLConfigNoExistingJKSGensPKCS12KeyStore() throws Exception {

        Log.info(c, name.getMethodName(), "Entering " + name.getMethodName());
        String protocol = TLSV11_PROTOCOL;

        server.setServerConfigurationFile(NO_SSL_CONFIG_BUT_DOES_INCLUDE_SSL_FEATURE);
        server.startServer(name.getMethodName() + ".log");

        // Requires info trace
        server.addInstalledAppForValidation("basicauth");

        // Verify no key.jks exists in the default path
        assertFalse("The key file " + "resources/security/key.jks" + " exists",
                    fileExists(server, "resources/security/key.jks"));

        assertNotNull("We need to wait for the SSL certificate to be generated at the default path, but we did not receive the message",
                      server.waitForStringInLog("CWPKI0803A:.*" + DEFAULT_GENERATED_KEY_PATH));

        assertNotNull("SSL TCP Channel did not start in time.",
                      server.waitForStringInLog("CWWKO0219I.*ssl"));
        assertNotNull("Need to wait for 'smarter planet' message (server is ready).",
                      server.waitForStringInLog("CWWKF0011I"));

        // Hit the servlet on the SSL port and validate it requires SSL
        SSLBasicAuthClient sslClient = null;
        String response = null;
        try {
            sslClient = createSSLClientWithTrust(DEFAULT_GENERATED_KEY_PATH, DEFAULT_GENERATED_KEY_PASSWORD, protocol);
            response = sslClient.accessUnprotectedServlet(SSLBasicAuthClient.UNPROTECTED_NO_SECURITY_CONSTRAINT);
        } catch (Exception e) {
        }
        if (response != null)
            assertTrue("Did not get the expected response",
                       sslClient.verifyUnauthenticatedResponse(response));
        else
            assertTrue("Did not get the expected response", false);

        // Check to see if the file has been generated

        assertTrue("The expected key file " + DEFAULT_GENERATED_KEY_PATH + " was not generated",
                   fileExists(server, DEFAULT_GENERATED_KEY_PATH));

        // Verify no key.jks has been generated
        assertFalse("The key file " + "resources/security/key.jks" + " was generated",
                    fileExists(server, "resources/security/key.jks"));

        if (server != null && server.isStarted()) {
            server.stopServer("CWNEN0050W", "SRVE0272W");
        }
        deleteFileIfExists(server, DEFAULT_GENERATED_KEY_PATH);
        deleteFileIfExists(server, ALTERNATE_GENERATED_KEY_PATH);

        Log.info(c, name.getMethodName(), "Exiting " + name.getMethodName());

    }

    /**
     * When the SSL feature is specified, a minimal SSL configuration is
     * specified in server.xml which includes the type,
     * <keyStore id="defaultKeyStore" type="JKS" password="Liberty" />, and
     * no key.jks exists in the default path, a new key.jks file will be
     * generated.
     *
     */
    @Test
    @AllowedFFDC("java.lang.NoClassDefFoundError")
    public void testDefaultMinimalSSLConfigWithJKSType() throws Exception {

        Log.info(c, name.getMethodName(), "Entering " + name.getMethodName());
        String protocol = TLSV11_PROTOCOL;

        // Verify no key.jks exists in the default path
        assertFalse("The key file " + "resources/security/key.jks" + " exists",
                    fileExists(server, "resources/security/key.jks"));

        server.setServerConfigurationFile(DEFAULT_MINIMAL_SSL_CONFIG_WITH_JKS_TYPE);
        server.startServer(name.getMethodName() + ".log");

        // Requires info trace
        server.addInstalledAppForValidation("basicauth");

        assertNotNull("SSL TCP Channel did not start in time.",
                      server.waitForStringInLog("CWWKO0219I.*ssl"));
        assertNotNull("Need to wait for 'smarter planet' message (server is ready).",
                      server.waitForStringInLog("CWWKF0011I"));

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

        // Verify the PKCS12 file has not been generated

        assertFalse("The expected key file " + DEFAULT_GENERATED_KEY_PATH + " was generated",
                    fileExists(server, DEFAULT_GENERATED_KEY_PATH));

        // Verify key.jks has been generated
        assertTrue("The key file " + "resources/security/key.jks" + " was generated",
                   fileExists(server, "resources/security/key.jks"));

        if (server != null && server.isStarted()) {
            server.stopServer("CWNEN0050W", "SRVE0272W");
        }

        deleteFileIfExists(server, "resources/security/key.jks");
        deleteFileIfExists(server, DEFAULT_GENERATED_KEY_PATH);
        deleteFileIfExists(server, ALTERNATE_GENERATED_KEY_PATH);

        Log.info(c, name.getMethodName(), "Exiting " + name.getMethodName());

    }
}
