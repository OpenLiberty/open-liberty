/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
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

public class DefaultKeystoreNonDefaultLocation extends CommonSSLTest {
    private static final Class<?> c = DefaultKeystoreNonDefaultLocation.class;
    private static boolean isOracle6 = false;

    public DefaultKeystoreNonDefaultLocation() {
        super(LibertyServerFactory.getLibertyServer("com.ibm.ws.ssl.fat.pkcs12.nonDefaultLoc"));
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
     * When default keystore configuration specify a keystore in non-default
     * location eg.
     * <keyStore id="defaultKeyStore" location="${server.config.dir}/key.p12" type="PKCS12" password="liberty" />, and
     * the keyfile exists, a key.p12 file should not be created in the default location.
     *
     */
    @Test
    @AllowedFFDC("java.lang.NoClassDefFoundError")
    public void testDefaultKeyStoreNonDefaultLocationPKCS12Type() throws Exception {

        Log.info(c, name.getMethodName(), "Entering " + name.getMethodName());

        server.setServerConfigurationFile(KEYSTORE_DEFAULT_NON_DEFAULT_LOC_PKCS12);
        server.startServer(name.getMethodName() + ".log");

        // Requires info trace
        server.addInstalledAppForValidation("basicauth");

        assertNotNull("SSL TCP Channel did not start in time.",
                      server.waitForStringInLog("CWWKO0219I.*ssl"));
        assertNotNull("Need to wait for 'smarter planet' message (server is ready).",
                      server.waitForStringInLog("CWWKF0011I"));

        //Make sure or existing keystore file is loaded
        assertNotNull("Wrong keystore file loaded",
                      server.findStringsInLogs("Successfully loaded default keystore: " + server.getServerRoot() + "/key.p12"));

        // Hit the servlet on the SSL port and validate it requires SSL
        SSLBasicAuthClient sslClient = null;
        String response = null;
        try {
            sslClient = createSSLClientWithTrust("key.p12", DEFAULT_GENERATED_KEY_PASSWORD, null);
            response = sslClient.accessUnprotectedServlet(SSLBasicAuthClient.UNPROTECTED_NO_SECURITY_CONSTRAINT);
        } catch (Exception e) {
        }
        if (response != null)
            assertTrue("Did not get the expected response",
                       sslClient.verifyUnauthenticatedResponse(response));
        else
            assertTrue("Did not get the expected response", false);

        // Verify the PKCS12 file has not been generated
        assertFalse("The unexpected key file " + "resources/security/key.p12" + " was generated",
                    fileExists(server, "resources/security/key.p12"));

        if (server != null && server.isStarted()) {
            server.stopServer("CWNEN0050W", "SRVE0272W");
        }

        Log.info(c, name.getMethodName(), "Exiting " + name.getMethodName());

    }

    /**
     * When default keystore configuration specify a keystore in non-default
     * location eg.
     * <keyStore id="defaultKeyStore" location="${server.config.dir}/key.jks" type="JKS" password="liberty" />, and
     * the keyfile exists, a key.p12 file should not be created in the default location.
     *
     */
    @Test
    @AllowedFFDC("java.lang.NoClassDefFoundError")
    public void testDefaultKeyStoreNonDefaultLocationJKSType() throws Exception {

        Log.info(c, name.getMethodName(), "Entering " + name.getMethodName());

        server.setServerConfigurationFile(KEYSTORE_DEFAULT_NON_DEFAULT_LOC_JKS);
        server.startServer(name.getMethodName() + ".log");

        // Requires info trace
        server.addInstalledAppForValidation("basicauth");

        assertNotNull("SSL TCP Channel did not start in time.",
                      server.waitForStringInLog("CWWKO0219I.*ssl"));
        assertNotNull("Need to wait for 'smarter planet' message (server is ready).",
                      server.waitForStringInLog("CWWKF0011I"));

        //Make sure or existing keystore file is loaded
        assertNotNull("Wrong keystore file loaded",
                      server.findStringsInLogs("Successfully loaded default keystore: " + server.getServerRoot() + "/key.jks"));

        // Hit the servlet on the SSL port and validate it requires SSL
        SSLBasicAuthClient sslClient = null;
        String response = null;
        try {
            sslClient = createSSLClientWithTrust("key.jks", DEFAULT_GENERATED_KEY_PASSWORD, null);
            response = sslClient.accessUnprotectedServlet(SSLBasicAuthClient.UNPROTECTED_NO_SECURITY_CONSTRAINT);
        } catch (Exception e) {
        }
        if (response != null)
            assertTrue("Did not get the expected response",
                       sslClient.verifyUnauthenticatedResponse(response));
        else
            assertTrue("Did not get the expected response", false);

        // Verify the PKCS12 file has not been generated
        assertFalse("The unexpected key file " + "resources/security/key.p12" + " was generated",
                    fileExists(server, "resources/security/key.p12"));

        if (server != null && server.isStarted()) {
            server.stopServer("CWNEN0050W", "SRVE0272W");
        }

        Log.info(c, name.getMethodName(), "Exiting " + name.getMethodName());

    }
}
