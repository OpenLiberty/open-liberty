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

public class NonDefaultJKSSSLTest extends CommonSSLTest {
    private static final Class<?> c = NonDefaultJKSSSLTest.class;

    public NonDefaultJKSSSLTest() {
        super(LibertyServerFactory.getLibertyServer("com.ibm.ws.ssl.fat.pkcs12.nondefaultjks"));
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
     * Validste that with the SSL feature and a non-default SSL config specified in server.xml,
     * where the location specifies a JKS keystore file, and no type is specified, the JKS keystore
     * file is used.
     *
     */
    @Test
    @AllowedFFDC("java.lang.NoClassDefFoundError")
    public void testNonDefaultSSLConfigUsingJKS() throws Exception {

        Log.info(c, name.getMethodName(), "Entering " + name.getMethodName());
        String protocol = TLSV11_PROTOCOL;

        server.setServerConfigurationFile(NON_DEFAULT_KEYSTORE_LOCATION_USING_JKS_NO_TYPE_SPECIFIED);
        server.startServer(name.getMethodName() + ".log");

        // Requires info trace
        server.addInstalledAppForValidation("basicauth");

        assertNotNull("We need to wait for the SSL certificate to be generated at the default path, but we did not receive the message",
                      server.waitForStringInLog("CWPKI0803A:.*" + DEFAULT_GENERATED_KEY_PATH));

        assertNotNull("SSL TCP Channel did not start in time.",
                      server.waitForStringInLog("CWWKO0219I.*ssl"));
        assertNotNull("Need to wait for 'smarter planet' message (server is ready).",
                      server.waitForStringInLog("CWWKF0011I"));

        assertTrue("The expected key file " + DEFAULT_GENERATED_KEY_PATH + " was generated",
                   fileExists(server, DEFAULT_GENERATED_KEY_PATH));

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

        // Check to see if the pkcs12 file has NOT been generated

        if (server != null && server.isStarted()) {
            server.stopServer("CWNEN0050W", "SRVE0272W");
        }
        Log.info(c, name.getMethodName(), "Exiting " + name.getMethodName());

    }
}
